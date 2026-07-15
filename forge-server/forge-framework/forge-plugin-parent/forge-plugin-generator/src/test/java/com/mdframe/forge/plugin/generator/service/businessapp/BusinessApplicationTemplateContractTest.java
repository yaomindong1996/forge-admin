package com.mdframe.forge.plugin.generator.service.businessapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("业务应用模板初始化契约")
class BusinessApplicationTemplateContractTest {

    @Test
    @DisplayName("three templates reuse one transactional schema initialization service")
    void templatesReuseTransactionalSchemaService() throws Exception {
        String source = readSource("service/businessapp/BusinessApplicationTemplateService.java");

        assertTrue(source.contains("@Transactional(rollbackFor = Exception.class)"));
        assertTrue(source.contains("SINGLE_CRUD"));
        assertTrue(source.contains("TREE_TABLE"));
        assertTrue(source.contains("MASTER_DETAIL"));
        assertTrue(source.contains("context.setPageSchema(null)"));
        assertTrue(source.contains("\"tree-crud\""));
        assertTrue(source.contains("\"master-detail-crud\""));
        assertTrue(source.contains("\"REFERENCE\""));
        assertTrue(source.contains("\"CHILD_LIST\""));
        assertTrue(source.contains("SOURCE_DATABASE_TABLE"));
        assertTrue(source.contains("SOURCE_EXISTING_OBJECT"));
        assertTrue(source.contains("requireField("));
        assertTrue(source.contains("applicationObjectService.replace"));
    }

    @Test
    @DisplayName("database table source exposes selectable columns")
    void databaseTableSourceExposesColumns() throws Exception {
        String source = readSource("controller/GenDatasourceController.java");

        assertTrue(source.contains("/{datasourceId}/tables/{tableName}/columns"));
        assertTrue(source.contains("selectDbTableColumnsByName(datasourceId, tableName)"));
    }

    @Test
    @DisplayName("controller exposes an application scoped template endpoint")
    void controllerExposesTemplateEndpoint() throws Exception {
        String source = readSource("controller/BusinessApplicationController.java");

        assertTrue(source.contains("@PostMapping(\"/{id}/initialize-template\")"));
        assertTrue(source.contains("BusinessApplicationTemplateInitializeDTO"));
        assertTrue(source.contains("templateService.initialize(id, dto)"));
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
