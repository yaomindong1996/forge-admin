package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.plugin.generator.codegen.CodegenStrategy;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiPageTemplate;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * AI CRUD 配置代码生成服务（入口）
 * <p>
 * 采用策略模式路由：
 * - TEMPLATE（默认）：走 VelocityCodegenStrategy，生成完整前后端代码
 * - AI：走 AiCodegenStrategy，适用于主从表、看板、工作流等复杂组件（预留）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCrudCodegenService {

    private final AiCrudConfigService crudConfigService;
    private final AiPageTemplateService pageTemplateService;
    private final List<CodegenStrategy> strategies;

    /**
     * 根据 configKey 生成代码 zip 包
     */
    public byte[] generateZip(String configKey) {
        AiCrudConfig config = crudConfigService.getByConfigKey(configKey);
        if (config == null) {
            throw new BusinessException("配置不存在: " + configKey);
        }
        try {
            // 加载页面模板，获取 codegen_type
            AiPageTemplate template = null;
            String codegenType = "TEMPLATE"; // 默认
            if (StringUtils.isNotBlank(config.getLayoutType())) {
                template = pageTemplateService.getByTemplateKey(config.getLayoutType());
                if (template != null && StringUtils.isNotBlank(template.getCodegenType())) {
                    codegenType = template.getCodegenType();
                }
            }

            // 路由到匹配的策略
            final String finalCodegenType = codegenType;
            CodegenStrategy strategy = strategies.stream()
                    .filter(s -> s.supports(finalCodegenType))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("no codegen strategy for type: " + finalCodegenType));

            log.info("[AiCrudCodegenService] configKey={}, codegenType={}, strategy={}",
                    configKey, codegenType, strategy.getClass().getSimpleName());

            Map<String, String> files = strategy.generate(config, template);
            return toZip(files);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AiCrudCodegenService] 生成代码失败, configKey={}", configKey, e);
            throw new BusinessException("代码生成失败: " + e.getMessage());
        }
    }

    private byte[] toZip(Map<String, String> files) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
                zipEntry.setSize(bytes.length);
                zos.putNextEntry(zipEntry);
                zos.write(bytes);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
