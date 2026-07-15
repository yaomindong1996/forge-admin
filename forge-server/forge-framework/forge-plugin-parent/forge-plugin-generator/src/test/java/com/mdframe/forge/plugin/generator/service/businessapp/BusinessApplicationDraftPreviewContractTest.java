package com.mdframe.forge.plugin.generator.service.businessapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("业务应用草稿预览与字段配置分层契约")
class BusinessApplicationDraftPreviewContractTest {

    @Test
    @DisplayName("设计预览强制使用最新草稿图编译运行配置")
    void designPreviewCompilesLatestDraftGraph() throws Exception {
        String serviceSource = readSource("service/AiCrudConfigService.java");
        String controllerSource = readSource("controller/AiCrudConfigController.java");

        assertTrue(serviceSource.contains("return buildDraftRenderConfig(config);"));
        assertTrue(serviceSource.contains("!forceDraftCompile && hasStoredRuntimeConfig(config)"));
        assertTrue(controllerSource.contains("businessObjectDesignerService.prepareRuntimeDraft(businessObject.getId())"));
        assertTrue(controllerSource.contains("crudConfigService.getRenderConfig(configKey, designPreview)"));
    }

    @Test
    @DisplayName("应用发布前只刷新主对象聚合草稿图")
    void applicationPublishPreparesPrimaryObjectDraft() throws Exception {
        String source = readSource("service/businessapp/BusinessApplicationPublishService.java");

        assertTrue(source.contains("preparePrimaryObjectDraft(applicationId);"));
        assertTrue(source.contains("\"PRIMARY\".equalsIgnoreCase(item.getObjectRole())"));
        assertTrue(source.contains("ifPresent(objectDesignerService::prepareRuntimeDraft)"));
    }

    @Test
    @DisplayName("页面设计不再无条件反写字段资产")
    void pageDesignDoesNotOverwriteFieldAssets() throws Exception {
        String source = readSource("service/businessapp/BusinessObjectDesignerService.java");

        assertTrue(source.contains(
                "context.setModelSchema(rebuildModelFields(context.getModelSchema(), dto.getFields()))"));
        assertFalse(source.contains("normalizeDesignerFieldPayloads(dto.getFields()"));
        assertFalse(source.contains("applyFormDesignerSchemaToModel(modelSchema, formSchema);"));
        assertTrue(source.contains("applyFormDesignerSchemaToEditZone(pageSchema, modelSchema, formSchema)"));
    }

    private String readSource(String relativePath) throws Exception {
        Path moduleRoot = Path.of("").toAbsolutePath();
        Path source = moduleRoot.resolve("src/main/java/com/mdframe/forge/plugin/generator").resolve(relativePath);
        if (!Files.exists(source)) {
            source = moduleRoot.resolve(
                    "forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator")
                    .resolve(relativePath);
        }
        if (!Files.exists(source)) {
            source = moduleRoot.resolve(
                    "forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator")
                    .resolve(relativePath);
        }
        return Files.readString(source);
    }
}
