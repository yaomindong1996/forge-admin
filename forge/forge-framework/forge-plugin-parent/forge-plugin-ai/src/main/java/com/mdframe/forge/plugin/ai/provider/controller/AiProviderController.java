package com.mdframe.forge.plugin.ai.provider.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.provider.domain.AiProvider;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ai/provider")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderService providerService;
    private final AiModelService modelService;
    private final ObjectMapper objectMapper;

    /**
     * 内置供应商预设模板列表（纯代码查表，不查数据库）
     */
    @GetMapping("/templates")
    public RespInfo<List<Map<String, String>>> templates() {
        return RespInfo.success(List.of(
                template("alibaba", "阿里百炼", "https://dashscope.aliyuncs.com/compatible-mode", "qwen-plus"),
                template("openai", "OpenAI", "https://api.openai.com", "gpt-4o-mini"),
                template("zhipu", "智谱 AI", "https://open.bigmodel.cn/api/paas/v4", "glm-4"),
                template("moonshot", "Moonshot", "https://api.moonshot.cn/v1", "moonshot-v1-8k"),
                template("deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat"),
                template("ollama", "Ollama（本地）", "http://localhost:11434", "llama3"),
                template("custom", "自定义", "", "")
        ));
    }

    private Map<String, String> template(String key, String name, String baseUrl, String defaultModel) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("templateKey", key);
        m.put("name", name);
        m.put("baseUrl", baseUrl);
        m.put("defaultModel", defaultModel);
        return m;
    }

    /**
     * 分页查询供应商列表
     */
    @GetMapping("/page")
    public RespInfo<Page<AiProvider>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<AiProvider> page = providerService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<AiProvider>().orderByDesc(AiProvider::getCreateTime));
        // 聚合填充 models 和 defaultModel，脱敏 apiKey
        page.getRecords().forEach(this::fillAndMaskProvider);
        return RespInfo.success(page);
    }

    /**
     * 查询供应商详情
     */
    @GetMapping("/{id}")
    public RespInfo<AiProvider> getById(@PathVariable Long id) {
        AiProvider provider = providerService.getById(id);
        if (provider != null) {
            fillAndMaskProvider(provider);
        }
        return RespInfo.success(provider);
    }

    /**
     * 创建供应商
     */
    @PostMapping
    public RespInfo<Void> create(@RequestBody AiProvider provider) {
        providerService.save(provider);
        return RespInfo.success();
    }

    /**
     * 更新供应商
     */
    @PutMapping
    public RespInfo<Void> update(@RequestBody AiProvider provider) {
        providerService.updateById(provider);
        // 双写同步：更新供应商后重新聚合 models
        syncModelsToProvider(provider.getId());
        return RespInfo.success();
    }

    /**
     * 删除供应商（校验关联模型）
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        long modelCount = modelService.countByProviderId(id);
        if (modelCount > 0) {
            throw new BusinessException("该供应商下存在 " + modelCount + " 个关联模型，请先删除关联模型");
        }
        providerService.deleteProvider(id);
        return RespInfo.success();
    }

    /**
     * 测试供应商连接
     */
    @PostMapping("/test")
    public RespInfo<String> test(@RequestBody AiProvider provider) {
        try {
            providerService.testConnection(provider);
            return RespInfo.success("连接成功！");
        } catch (Exception e) {
            log.warn("[AI供应商测试失败] {}", e.getMessage());
            return RespInfo.error(e.getMessage());
        }
    }

    /**
     * 设为默认供应商
     */
    @PutMapping("/{id}/default")
    public RespInfo<Void> setDefault(@PathVariable Long id) {
        providerService.setDefault(id);
        return RespInfo.success();
    }

    /**
     * 聚合填充 models/defaultModel 并脱敏 apiKey
     */
    private void fillAndMaskProvider(AiProvider provider) {
        fillModelsFromAiModel(provider);
        maskApiKey(provider);
    }

    /**
     * 从 ai_model 表聚合填充供应商的 models 和 defaultModel 字段
     * 确保旧接口响应格式兼容
     */
    private void fillModelsFromAiModel(AiProvider provider) {
        List<AiModel> models = modelService.listByProviderId(provider.getId());
        List<String> modelIdList = models.stream()
                .map(AiModel::getModelId)
                .collect(Collectors.toList());
        provider.setModels(modelIdList.isEmpty() ? "[]" : toJsonArray(modelIdList));
        String defaultModel = models.stream()
                .filter(m -> AiConstants.IS_DEFAULT_YES.equals(m.getIsDefault()))
                .map(AiModel::getModelId)
                .findFirst()
                .orElse(provider.getDefaultModel());
        provider.setDefaultModel(defaultModel);
    }

    /**
     * API Key 脱敏：保留前4后4位，中间用 **** 替代
     */
    private void maskApiKey(AiProvider provider) {
        String apiKey = provider.getApiKey();
        if (apiKey != null && apiKey.length() > 8) {
            provider.setApiKey(apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4));
        } else if (apiKey != null && !apiKey.isEmpty()) {
            provider.setApiKey("****");
        }
    }

    /**
     * 双写同步：将 ai_model 表数据聚合回写至 ai_provider.models 和 ai_provider.default_model
     */
    private void syncModelsToProvider(Long providerId) {
        List<String> modelIdList = modelService.getModelIdListByProviderId(providerId);
        String defaultModel = modelService.getDefaultModelId(providerId);

        String modelsJson;
        try {
            modelsJson = objectMapper.writeValueAsString(modelIdList);
        } catch (JsonProcessingException e) {
            log.error("[AI模型同步] JSON序列化失败, providerId={}", providerId, e);
            modelsJson = "[]";
        }

        providerService.update(new LambdaUpdateWrapper<AiProvider>()
                .set(AiProvider::getModels, modelsJson)
                .set(AiProvider::getDefaultModel, defaultModel)
                .eq(AiProvider::getId, providerId));

        log.info("[AI模型同步] 已同步, providerId={}, modelCount={}, defaultModel={}",
                providerId, modelIdList.size(), defaultModel);
    }

    private String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
