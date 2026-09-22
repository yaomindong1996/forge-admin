package com.mdframe.forge.plugin.generator.service.businessapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("业务应用草稿预览与字段配置分层契约")
class BusinessApplicationDraftPreviewContractTest {

    @Test
    @DisplayName("设计预览强制使用最新草稿图编译运行配置")
    void designPreviewCompilesLatestDraftGraph() throws Exception {
        String serviceSource = readSource("service/AiCrudConfigService.java");
        String controllerSource = readSource("controller/AiCrudConfigController.java");
        String designerSource = readSource("service/businessapp/BusinessObjectDesignerService.java");

        assertTrue(serviceSource.contains("return buildDraftRenderConfig(config);"));
        assertTrue(serviceSource.contains("!forceDraftCompile && hasStoredRuntimeConfig(config)"));
        assertTrue(controllerSource.contains("businessObjectDesignerService.prepareRuntimeDraftForPreview(businessObject.getId())"));
        assertFalse(controllerSource.contains("businessObjectDesignerService.prepareRuntimeDraft(businessObject.getId())"));
        assertTrue(designerSource.contains("prepareRuntimeDraftForPreview"));
        assertTrue(designerSource.contains("prepareRuntimeDraft(objectId, false)"));
        assertTrue(designerSource.contains("if (persistChildRelations)"));
        assertTrue(controllerSource.contains("crudConfigService.getRenderConfig(configKey, designPreview)"));
        assertTrue(designerSource.contains("saveDraft(context, currentStatus, false)"));
        assertTrue(designerSource.contains("if (markApplicationChanged)"));
        assertFalse(designerSource.contains(
                "return saveDraft(context, BusinessObjectDesignStatus.CHANGED.getCode()).getConfig();"));
    }

    @Test
    @DisplayName("发布检查和最终发布都先同步托管数据表")
    void applicationPublishSynchronizesManagedDatabasesBeforeReadiness() throws Exception {
        String source = readSource("service/businessapp/BusinessApplicationPublishService.java");

        assertEquals(2, countOccurrences(source, "prepareApplicationObjectDrafts(applicationId);"));
        assertEquals(2, countOccurrences(
                source, "formDataService.synchronizeManagedDatabases(applicationId);"));
        assertTrue(source.contains(".forEach(objectDesignerService::prepareRuntimeDraft)"));
        assertTrue(source.contains("verifyPublishedObjects(run.getApplicationId(), selection.getObjectIds(), result)"));
    }

    @Test
    @DisplayName("应用发布内部异常返回步骤、错误码和诊断编号而不返回原始异常")
    void applicationPublishReturnsSafeActionableDiagnostics() throws Exception {
        String serviceSource = readSource("service/businessapp/BusinessApplicationPublishService.java");
        String resultSource = readSource("vo/businessapp/BusinessApplicationPublishResultVO.java");

        assertTrue(serviceSource.contains("safeInternalMessage(step, diagnosticRef)"));
        assertTrue(serviceSource.contains("诊断编号：%s"));
        assertTrue(serviceSource.contains("result.setErrorCode(run.getErrorCode())"));
        assertTrue(serviceSource.contains("log.error(\"[业务应用发布] unexpected step failure"));
        assertFalse(serviceSource.contains(
                "return fail(run, step, \"PUBLISH_INTERNAL_ERROR\", \"发布步骤发生内部异常，详细信息已脱敏\")"));
        assertTrue(resultSource.contains("private String errorCode;"));
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

    @Test
    @DisplayName("application coordinated publishing disables legacy lowcode menu generation")
    void applicationPublishingDoesNotCreateLegacyLowcodeMenus() throws Exception {
        String applicationPublishSource = readSource(
                "service/businessapp/BusinessApplicationPublishService.java");
        String objectPublishSource = readSource(
                "service/businessapp/BusinessObjectPublishService.java");
        String lowcodePublishSource = readSource("service/lowcode/LowcodePublishService.java");

        assertTrue(applicationPublishSource.contains("objectDto.setSyncMenu(false);"));
        assertTrue(objectPublishSource.contains("publishDTO.setSyncMenu(dto == null ? null : dto.getSyncMenu());"));
        assertTrue(objectPublishSource.contains("return rollbackInternal(objectId, versionId, false);"));
        assertTrue(objectPublishSource.contains(
                "lowcodePublishService.rollback(version.getConfigId(), version.getCrudConfigVersionId(), syncMenu)"));
        assertTrue(lowcodePublishSource.contains("if (shouldSyncMenu(dto))"));
        assertTrue(lowcodePublishSource.contains("disablePublishedMenu(config);"));
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

    private int countOccurrences(String source, String target) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = source.indexOf(target, fromIndex)) >= 0) {
            count++;
            fromIndex += target.length();
        }
        return count;
    }
}
