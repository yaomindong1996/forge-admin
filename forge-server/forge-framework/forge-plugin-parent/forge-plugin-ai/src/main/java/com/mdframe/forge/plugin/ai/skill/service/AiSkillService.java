package com.mdframe.forge.plugin.ai.skill.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterRegistry;
import com.mdframe.forge.plugin.ai.provider.adapter.AiModelRuntimeOptions;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.plugin.ai.skill.domain.AiAgentSkill;
import com.mdframe.forge.plugin.ai.skill.domain.AiSkill;
import com.mdframe.forge.plugin.ai.skill.domain.AiSkillFile;
import com.mdframe.forge.plugin.ai.skill.mapper.AiAgentSkillMapper;
import com.mdframe.forge.plugin.ai.skill.mapper.AiSkillFileMapper;
import com.mdframe.forge.plugin.ai.skill.mapper.AiSkillMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 技能包 CRUD 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiSkillService extends ServiceImpl<AiSkillMapper, AiSkill> {

    private final AiSkillFileMapper skillFileMapper;
    private final AiAgentSkillMapper agentSkillMapper;
    private final AiProviderService providerService;
    private final AiProviderAdapterRegistry providerAdapterRegistry;
    private final AiSecretCrypto aiSecretCrypto;

    /**
     * 分页查询技能
     */
    public Page<AiSkill> selectSkillPage(Integer pageNum, Integer pageSize,
                                          String keyword, String status) {
        return baseMapper.selectSkillPage(new Page<>(pageNum, pageSize), keyword, status);
    }

    /**
     * 根据编码获取启用的技能
     */
    public AiSkill getByCode(String skillCode) {
        return baseMapper.selectEnabledByCode(skillCode);
    }

    /**
     * 获取技能文件列表
     */
    public List<AiSkillFile> getSkillFiles(Long skillId) {
        return skillFileMapper.selectBySkillId(skillId);
    }

    /**
     * 获取 Agent 绑定的技能列表
     */
    public List<AiAgentSkill> getAgentSkills(Long agentId) {
        return agentSkillMapper.selectByAgentId(agentId);
    }

    /**
     * 为 Agent 绑定技能
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindAgentSkill(Long agentId, Long skillId) {
        AiSkill skill = getById(skillId);
        if (skill == null) {
            throw new BusinessException("技能不存在");
        }
        // 检查是否已绑定
        List<AiAgentSkill> existing = agentSkillMapper.selectByAgentId(agentId);
        boolean alreadyBound = existing.stream().anyMatch(s -> s.getSkillId().equals(skillId));
        if (alreadyBound) {
            return; // 已绑定，幂等
        }
        AiAgentSkill binding = new AiAgentSkill();
        binding.setAgentId(agentId);
        binding.setSkillId(skillId);
        agentSkillMapper.insert(binding);
    }

    /**
     * 解除 Agent 技能绑定
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbindAgentSkill(Long agentId, Long skillId) {
        List<AiAgentSkill> bindings = agentSkillMapper.selectByAgentId(agentId);
        for (AiAgentSkill binding : bindings) {
            if (binding.getSkillId().equals(skillId)) {
                agentSkillMapper.deleteById(binding.getId());
                return;
            }
        }
    }

    /**
     * 新增技能
     */
    @Transactional(rollbackFor = Exception.class)
    public void createSkill(AiSkill skill) {
        validateSkillCode(skill.getSkillCode(), null);
        if (!save(skill)) {
            throw new BusinessException("技能新增失败");
        }
    }

    /**
     * 更新技能
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSkill(AiSkill skill) {
        if (skill == null || skill.getId() == null) {
            throw new BusinessException("技能ID不能为空");
        }
        validateSkillCode(skill.getSkillCode(), skill.getId());
        if (!updateById(skill)) {
            throw new BusinessException("技能更新失败");
        }
    }

    /**
     * 删除技能（级联删除文件和绑定）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSkill(Long id) {
        AiSkill skill = getById(id);
        if (skill == null) {
            throw new BusinessException("技能不存在");
        }
        // 删除技能文件
        List<AiSkillFile> files = skillFileMapper.selectBySkillId(id);
        for (AiSkillFile file : files) {
            skillFileMapper.deleteById(file.getId());
        }
        // 删除 Agent 绑定
        List<AiAgentSkill> bindings = agentSkillMapper.selectBySkillId(id);
        for (AiAgentSkill binding : bindings) {
            agentSkillMapper.deleteById(binding.getId());
        }
        removeById(id);
    }

    /**
     * 上传 ZIP 技能包
     * 解压 ZIP，解析 SKILL.md frontmatter（name/description/version），保存所有文件到 ai_skill_file
     */
    @Transactional(rollbackFor = Exception.class)
    public AiSkill uploadZip(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("ZIP文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new BusinessException("仅支持ZIP格式文件");
        }

        // 解压并解析
        Map<String, String> fileContents;
        Map<String, String> frontmatter;
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            fileContents = new java.util.LinkedHashMap<>();
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String entryName = entry.getName();
                // 跳过隐藏文件和 macOS 资源文件
                if (entryName.startsWith(".") || entryName.contains("/__MACOSX") || entryName.contains("/.")) continue;

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                fileContents.put(entryName, sb.toString());
                zis.closeEntry();
            }

            // 解析 SKILL.md frontmatter
            String skillMd = fileContents.entrySet().stream()
                    .filter(e -> e.getKey().equals("SKILL.md") || e.getKey().endsWith("/SKILL.md"))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("ZIP中未找到SKILL.md文件"));

            frontmatter = parseFrontmatter(skillMd);
        } catch (IOException e) {
            throw new BusinessException("ZIP解压失败: " + e.getMessage());
        }

        // 创建技能记录
        String skillName = frontmatter.getOrDefault("name", "未命名技能");
        String skillCode = frontmatter.getOrDefault("code", generateCodeFromName(skillName));
        String description = frontmatter.getOrDefault("description", "");
        String version = frontmatter.getOrDefault("version", "1.0.0");

        AiSkill skill = new AiSkill();
        skill.setSkillName(skillName);
        skill.setSkillCode(skillCode);
        skill.setDescription(description);
        skill.setVersion(version);
        skill.setStatus(EnableStatus.DISABLED.codeAsString());

        validateSkillCode(skillCode, null);
        if (!save(skill)) {
            throw new BusinessException("技能新增失败");
        }

        // 保存技能文件
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            AiSkillFile skillFile = new AiSkillFile();
            skillFile.setSkillId(skill.getId());
            skillFile.setFilePath(entry.getKey());
            skillFile.setFileContent(entry.getValue());
            skillFile.setEncoding("utf-8");
            skillFileMapper.insert(skillFile);
        }

        return skill;
    }

    /**
     * AI 生成 SKILL.md 内容
     */
    public String aiGenerate(String description) {
        if (description == null || description.isBlank()) {
            throw new BusinessException("技能描述不能为空");
        }

        AiProvider provider = providerService.requireEnabledDefaultProvider();
        String apiKey = aiSecretCrypto.isEncrypted(provider.getApiKey())
                ? aiSecretCrypto.decrypt(provider.getApiKey()) : provider.getApiKey();

        AiModelRuntimeOptions options = new AiModelRuntimeOptions(
                provider.getDefaultModel(), 0.7, 2048);
        ChatModel chatModel = providerAdapterRegistry.createChatModel(provider, options);

        String systemPrompt = """
                你是一个AI技能包设计专家。根据用户描述，生成一个标准的SKILL.md文件内容。
                格式要求：
                ---
                name: 技能名称
                code: 技能编码(英文蛇形)
                description: 技能描述
                version: 1.0.0
                ---
                # 技能名称
                ## 描述
                详细描述技能的功能和使用场景。
                ## 使用指南
                具体的使用步骤和注意事项。
                ## 示例
                提供使用示例。
                """;

        Prompt prompt = new Prompt(List.of(
                new org.springframework.ai.chat.messages.SystemMessage(systemPrompt),
                new UserMessage("请为以下需求生成SKILL.md：\n" + description)));

        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }

    /**
     * AI 优化现有 SKILL.md
     */
    public String aiOptimize(Long skillId, String instruction) {
        AiSkill skill = getById(skillId);
        if (skill == null) {
            throw new BusinessException("技能不存在");
        }

        List<AiSkillFile> files = skillFileMapper.selectBySkillId(skillId);
        String currentSkillMd = files.stream()
                .filter(f -> "SKILL.md".equals(f.getFilePath()) || f.getFilePath().endsWith("/SKILL.md"))
                .map(AiSkillFile::getFileContent)
                .findFirst()
                .orElseThrow(() -> new BusinessException("技能未包含SKILL.md文件"));

        AiProvider provider = providerService.requireEnabledDefaultProvider();
        String apiKey = aiSecretCrypto.isEncrypted(provider.getApiKey())
                ? aiSecretCrypto.decrypt(provider.getApiKey()) : provider.getApiKey();

        AiModelRuntimeOptions options = new AiModelRuntimeOptions(
                provider.getDefaultModel(), 0.7, 2048);
        ChatModel chatModel = providerAdapterRegistry.createChatModel(provider, options);

        String systemPrompt = """
                你是一个AI技能包优化专家。根据用户指令，优化现有的SKILL.md内容。
                保持frontmatter格式不变，只优化正文内容。
                """;

        String userMsg = "当前SKILL.md内容：\n```\n" + currentSkillMd + "\n```\n\n优化指令：" + instruction;

        Prompt prompt = new Prompt(List.of(
                new org.springframework.ai.chat.messages.SystemMessage(systemPrompt),
                new UserMessage(userMsg)));

        ChatResponse response = chatModel.call(prompt);
        String optimized = response.getResult().getOutput().getText();

        // 更新 SKILL.md 文件
        for (AiSkillFile f : files) {
            if ("SKILL.md".equals(f.getFilePath()) || f.getFilePath().endsWith("/SKILL.md")) {
                f.setFileContent(optimized);
                skillFileMapper.updateById(f);
                break;
            }
        }

        return optimized;
    }

    /**
     * 解析 SKILL.md frontmatter（--- 包围的 YAML 头部）
     */
    private Map<String, String> parseFrontmatter(String content) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        if (content == null || !content.startsWith("---")) {
            return result;
        }
        String[] parts = content.split("---", 3);
        if (parts.length < 3) {
            return result;
        }
        String yaml = parts[1].trim();
        for (String line : yaml.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int colonIdx = line.indexOf(':');
            if (colonIdx > 0) {
                String key = line.substring(0, colonIdx).trim();
                String value = line.substring(colonIdx + 1).trim();
                // 去除引号
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(key, value);
            }
        }
        return result;
    }

    private String generateCodeFromName(String name) {
        if (name == null || name.isBlank()) return "unnamed_skill";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "_")
                .replaceAll("^_+|_+$", "")
                .replaceAll("_+", "_");
    }

    private void validateSkillCode(String skillCode, Long excludeId) {
        if (skillCode == null || skillCode.isBlank()) {
            throw new BusinessException("技能编码不能为空");
        }
        int count = baseMapper.countByCode(skillCode, excludeId);
        if (count > 0) {
            throw new BusinessException("技能编码已存在: " + skillCode);
        }
    }
}
