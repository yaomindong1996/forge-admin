package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.codegen.VelocityCodegenStrategy;
import com.mdframe.forge.plugin.generator.domain.entity.GenTable;
import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.util.VelocityUtils;
import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("业务应用完整代码包契约")
class BusinessApplicationCodegenContractTest {

    @Test
    @DisplayName("应用代码包复用统一对象生成器并支持批量组装")
    void applicationPackageReusesObjectCodegen() throws Exception {
        String source = readSource("service/businessapp/BusinessApplicationCodegenService.java");

        assertTrue(source.contains("objectDesignerService.prepareRuntimeDraft(object.getObjectId())"));
        assertTrue(source.contains("codegenService.generateFiles(prepared)"));
        assertTrue(source.contains("resolveAggregatedObjectIds(primaryConfig, selectedObjects)"));
        assertTrue(source.contains("target.putIfAbsent(entry.getKey(), entry.getValue())"));
        assertTrue(source.contains("config/application-manifest.json"));
        assertTrue(source.contains("request.getObjectIds()"));
    }

    @Test
    @DisplayName("最新模型和页面协议覆盖旧派生运行配置")
    void latestSchemaOverridesStoredRuntimeProjection() throws Exception {
        String source = readSource("service/lowcode/LowcodeCodegenService.java");

        assertTrue(source.contains("config.setSearchSchema(runtimeConfig.getSearchSchema())"));
        assertTrue(source.contains("config.setColumnsSchema(runtimeConfig.getColumnsSchema())"));
        assertTrue(source.contains("config.setEditSchema(runtimeConfig.getEditSchema())"));
        assertTrue(source.contains("StringUtils.defaultIfBlank(runtimeConfig.getLayoutType(), config.getLayoutType())"));
        assertTrue(source.contains("merged.putAll(readOptions(runtimeOptionsJson))"));
        assertTrue(source.contains("request.getBusinessApiBase()"));
        assertFalse(source.contains("if (StringUtils.isBlank(config.getSearchSchema()))"));
        assertFalse(source.contains("StringUtils.defaultIfBlank(config.getLayoutType(), runtimeConfig.getLayoutType())"));
    }

    @Test
    @DisplayName("现有 Velocity 模板生成树形主子页面和 Mapper XML 查询")
    void velocityTemplatesCoverApplicationLayouts() throws Exception {
        String strategy = readSource("codegen/VelocityCodegenStrategy.java");
        String serviceTemplate = readResource("templates/vm/serviceImpl.java.vm");
        String mapperTemplate = readResource("templates/vm/mapper.xml.vm");
        String queryTemplate = readResource("templates/vm/query.java.vm");
        String extensionTemplate = readResource("templates/vm/serviceExtension.java.vm");

        assertTrue(strategy.contains("isLeftTreeLayout"));
        assertTrue(strategy.contains("isMasterDetailLayout"));
        assertTrue(strategy.contains("masterDetailChildren"));
        assertTrue(strategy.contains("appendPageSchemaMasterDetailChildren"));
        assertTrue(strategy.contains("未解析到有效子表关系"));
        assertTrue(strategy.contains("pageSchema == null ? null : pageSchema.getLayoutType()"));
        assertTrue(strategy.contains("public static class RelatedTableMeta"));
        assertTrue(strategy.contains("service/I\" + meta.getClassName() + \"Service.java"));
        assertTrue(strategy.contains("UNRESOLVED_VELOCITY_REFERENCE"));
        assertTrue(serviceTemplate.contains("selectGeneratedPage"));
        assertTrue(serviceTemplate.contains("select${child.className}Rows"));
        assertFalse(serviceTemplate.contains("LambdaQueryWrapper"));
        assertTrue(mapperTemplate.contains("<select id=\"selectGeneratedPage\""));
        assertTrue(mapperTemplate.contains("<select id=\"selectTreeRows\""));
        assertTrue(mapperTemplate.contains("clear${child.className}Rows"));
        assertTrue(mapperTemplate.contains("GeneratedQueryWhere"));
        assertTrue(mapperTemplate.contains("$column.queryType == 'BETWEEN'"));
        assertTrue(mapperTemplate.contains("$column.queryType == 'IN'"));
        assertTrue(queryTemplate.contains("java.util.List<$column.javaType>"));
        assertTrue(serviceTemplate.contains("extends ServiceImpl<${className}Mapper, ${className}>"));
        assertTrue(serviceTemplate.contains("invokeExtensions"));
        assertTrue(serviceTemplate.contains("extensionProvider.orderedStream().toList()"));
        assertTrue(extensionTemplate.contains("operation.proceed()"));
        assertTrue(extensionTemplate.contains("aroundPage"));
        assertTrue(extensionTemplate.contains("aroundInsert"));
    }

    @Test
    @DisplayName("生成页面复用在线低代码解释器且携带完整协议资产")
    void generatedPageUsesSharedRuntimeInterpreter() throws Exception {
        String strategy = readSource("codegen/VelocityCodegenStrategy.java");
        String pageTemplate = readResource("templates/vm/ai-crud/index.vue.vm");
        String runtimeView = readRepositoryFile("forge-admin-ui/src/views/ai/crud-page.vue");
        String runtimeComponent = readRepositoryFile(
                "forge-admin-ui/src/components/lowcode-runtime/LowcodeRuntimePage.vue");

        assertTrue(pageTemplate.contains("LowcodeRuntimePage"));
        assertTrue(pageTemplate.contains("./runtime-config.json"));
        assertFalse(pageTemplate.contains("transformColumns"));
        assertFalse(pageTemplate.contains("transformFields"));
        assertFalse(pageTemplate.contains("preloadDicts"));
        assertTrue(strategy.contains("artifacts.frontendRuntimeConfig()"));
        assertTrue(strategy.contains("generatedRuntimeConfigBuilder.build(config, apiBase)"));
        assertTrue(strategy.contains("AiCrudConfig codegenConfig = generatedRuntimeConfigBuilder.build(config, apiBase)"));
        assertTrue(strategy.contains("contributor.supports(codegenConfig)"));
        assertFalse(strategy.contains("config = generatedRuntimeConfigBuilder.build(config, apiBase)"));
        assertFalse(strategy.contains("META-INF/forge-lowcode/"));
        assertTrue(strategy.contains("-protocol.json"));
        assertTrue(strategy.contains("-coverage.json"));
        assertTrue(strategy.contains("-ownership.json"));
        assertTrue(strategy.contains("LowcodeStaticCodegenContributor"));
        assertTrue(strategy.contains("contributeStaticCodegenFiles"));
        assertTrue(runtimeView.contains("runtimeConfig:"));
        assertTrue(runtimeView.contains("embeddedRuntime"));
        assertTrue(runtimeView.contains("props.runtimeConfig"));
        assertTrue(runtimeComponent.contains("@/views/ai/crud-page.vue"));
    }

    @Test
    @DisplayName("生成业务Controller调用静态MyBatis-Plus Service")
    void generatedControllerUsesStaticMybatisPlusService() throws Exception {
        String controllerTemplate = readResource("templates/vm/controller.java.vm");
        String serviceTemplate = readResource("templates/vm/serviceImpl.java.vm");
        String mapperTemplate = readResource("templates/vm/mapper.xml.vm");
        String assembler = readSource("service/businessapp/BusinessCodegenConfigAssembler.java");

        assertTrue(controllerTemplate.contains("private final I${className}Service ${classname}Service"));
        assertTrue(controllerTemplate.contains("${classname}Service.select${className}Page"));
        assertTrue(controllerTemplate.contains("${classname}Service.insert${className}"));
        assertTrue(controllerTemplate.contains("${classname}Service.update${className}"));
        assertFalse(controllerTemplate.contains("DynamicCrudService"));
        assertFalse(controllerTemplate.contains("DynamicCrudExcelService"));
        assertTrue(controllerTemplate.contains("DynamicExportEngine excelExportEngine"));
        assertTrue(controllerTemplate.contains("excelImportService.importData"));
        assertTrue(controllerTemplate.contains("${classname}Service.import${className}"));
        assertTrue(controllerTemplate.contains("@PostMapping(\"/getById\")"));
        assertTrue(controllerTemplate.contains("@PostMapping(\"/edit\")"));
        assertTrue(controllerTemplate.contains("@PostMapping(\"/remove/{${pkColumn.javaField}}\")"));
        assertFalse(controllerTemplate.contains("/ai/crud/"));
        assertTrue(serviceTemplate.contains("ServiceImpl<${className}Mapper, ${className}>"));
        assertFalse(serviceTemplate.contains("LambdaQueryWrapper"));
        assertTrue(mapperTemplate.contains("selectGeneratedPage"));
        assertTrue(assembler.contains("generated_"));
        assertTrue(assembler.contains("sourceConfigKey"));
        assertTrue(assembler.contains("LowcodeProtocolSnapshotBuilder.RUNTIME_CONTRACT"));
        assertTrue(assembler.contains("STATIC_MYBATIS_PLUS"));
        assertTrue(assembler.contains("apiConfig.put(\"tree\""));
        assertFalse(assembler.contains("apiConfig.put(\"exportTasks\""));
    }

    @Test
    @DisplayName("用户业务扩展示例不进入生成源码目录")
    void userExtensionExampleIsCreateOnceAndOwnedByUserAfterCopy() throws Exception {
        String strategy = readSource("codegen/VelocityCodegenStrategy.java");
        String example = readResource("templates/vm/businessExtension.java.vm.example");

        assertTrue(strategy.contains("CREATE_ONCE_SAMPLE"));
        assertTrue(strategy.contains("NEVER_GENERATED_NEVER_OVERWRITTEN"));
        assertTrue(strategy.contains("service/extension/custom/**/*.java"));
        assertTrue(example.contains("operation.proceed()"));
        assertTrue(example.contains("@Order(100)"));
    }

    @Test
    @DisplayName("无真实脱敏策略时实体不生成注解或导入")
    void entityTemplateOmitsNoopDesensitizeAnnotation() {
        GenTable table = table("tf_customer", "Customer", "客户");
        table.getPkColumn().setDesensitizeType("NONE");
        VelocityUtils.initVelocity();
        VelocityContext context = VelocityUtils.prepareContext(table);
        context.put("hasDesensitize", false);
        context.put("hasEntityTreeFields", false);

        String generated = VelocityUtils.renderTemplate("templates/vm/entity.java.vm", context);

        assertFalse(generated.contains("@Desensitize"));
        assertFalse(generated.contains("DesensitizeType"));
    }

    @Test
    @DisplayName("下载命名路径和范围设置经过全部公共入口")
    void downloadSettingsUseSharedCodegenProtocol() throws Exception {
        String request = readSource("dto/lowcode/LowcodeCodegenRequest.java");
        String applicationService = readSource("service/businessapp/BusinessApplicationCodegenService.java");
        String entryService = readSource("service/businessapp/BusinessAppCodegenService.java");
        String lowcodeService = readSource("service/lowcode/LowcodeCodegenService.java");
        String assembler = readSource("service/businessapp/BusinessCodegenConfigAssembler.java");
        String strategy = readSource("codegen/VelocityCodegenStrategy.java");
        String panel = readRepositoryFile(
                "forge-admin-ui/src/views/app-center/components/AppCodePanel.vue");
        String applicationApi = readRepositoryFile("forge-admin-ui/src/api/business-application.js");
        String entryApi = readRepositoryFile("forge-admin-ui/src/api/business-app.js");

        for (String source : List.of(request, applicationService, entryService, lowcodeService, assembler, strategy, panel)) {
            assertTrue(source.contains("entityPrefix"));
            assertTrue(source.contains("stripTablePrefixes"));
            assertTrue(source.contains("backendBasePath"));
            assertTrue(source.contains("mapperXmlBasePath"));
            assertTrue(source.contains("frontendApiBasePath"));
            assertTrue(source.contains("includeBackend"));
            assertTrue(source.contains("includeFrontend"));
            assertTrue(source.contains("includeExcelSql"));
        }
        assertTrue(strategy.contains("LowcodeCodegenOptionUtils.buildClassName"));
        assertTrue(strategy.contains("if (includeBackend)"));
        assertTrue(strategy.contains("if (includeFrontend)"));
        assertTrue(strategy.contains("includeSql && includeExcelSql"));
        assertTrue(strategy.contains("mapperXmlRoot + className + \"Mapper.xml\""));
        assertTrue(assembler.contains("copyIfDefined(options, codegen, \"entityPrefix\""));
        assertTrue(applicationApi.contains("key === 'stripTablePrefixes'"));
        assertTrue(applicationApi.contains("key === 'entityPrefix'"));
        assertTrue(entryApi.contains("key === 'stripTablePrefixes'"));
        assertTrue(entryApi.contains("key === 'entityPrefix'"));
    }

    @Test
    @DisplayName("主子表模板实际渲染子表类型和聚合数据逻辑")
    void masterDetailTemplateResolvesRelatedTableMetadata() {
        GenTable mainTable = table("tf_f_order", "TfFOrder", "订单基本信息");
        GenTable childTable = table("tf_f_order_detail", "TfFOrderDetail", "订单明细");
        GenTableColumn childId = childTable.getPkColumn();

        VelocityCodegenStrategy.RelatedTableMeta child = new VelocityCodegenStrategy.RelatedTableMeta();
        child.setKey("tf_f_order_detail");
        child.setChildKey("tf_f_order_detail");
        child.setClassName("TfFOrderDetail");
        child.setVariableName("tfFOrderDetail");
        child.setMapperVarName("tfFOrderDetailMapper");
        child.setTableName("tf_f_order_detail");
        child.setTable(childTable);
        child.setColumns(childTable.getColumns());
        child.setPkColumn(childId);
        child.setChildFkField("orderId");
        child.setChildFkFieldCap("OrderId");
        child.setChildFkColumn("order_id");
        child.setMainField("id");
        child.setMainFieldCap("Id");
        child.setMainColumn("id");
        child.setMasterDetailChild(true);

        VelocityUtils.initVelocity();
        VelocityContext context = VelocityUtils.prepareContext(mainTable);
        context.put("isMasterDetailLayout", true);
        context.put("hasTreeConfig", false);
        context.put("injectedRelatedTables", List.of(child));
        context.put("masterDetailChildren", List.of(child));

        String generated = VelocityUtils.renderTemplate("templates/vm/serviceImpl.java.vm", context);

        assertTrue(generated.contains("private final TfFOrderDetailMapper tfFOrderDetailMapper;"));
        assertTrue(generated.contains("selectTfFOrderDetailRows"));
        assertTrue(generated.contains("saveTfFOrderDetailRows"));
        assertTrue(generated.contains("clearTfFOrderDetailRows"));
        assertFalse(generated.contains("${table."));
        assertFalse(generated.contains("${child."));
    }

    @Test
    @DisplayName("应用聚合控制器暴露设置预览和下载入口")
    void controllerExposesApplicationCodeEndpoints() throws Exception {
        String source = readSource("controller/BusinessApplicationController.java");

        assertTrue(source.contains("@GetMapping(\"/{id}/code/options\")"));
        assertTrue(source.contains("@GetMapping(\"/{id}/code/preview\")"));
        assertTrue(source.contains("@GetMapping(\"/{id}/code/download\")"));
        assertTrue(source.contains("BusinessApplicationCodegenService"));
    }

    private String readSource(String relativePath) throws Exception {
        return Files.readString(resolveModuleRoot().resolve("src/main/java/com/mdframe/forge/plugin/generator")
                .resolve(relativePath));
    }

    private String readResource(String relativePath) throws Exception {
        return Files.readString(resolveModuleRoot().resolve("src/main/resources").resolve(relativePath));
    }

    private String readRepositoryFile(String relativePath) throws Exception {
        return Files.readString(resolveRepositoryRoot().resolve(relativePath));
    }

    private GenTable table(String tableName, String className, String functionName) {
        GenTableColumn id = new GenTableColumn();
        id.setColumnName("id");
        id.setColumnComment("主键");
        id.setColumnType("bigint");
        id.setJavaType("Long");
        id.setJavaField("id");
        id.setIsPk(1);
        id.setIsIncrement(1);
        id.setIsInsert(0);
        id.setIsEdit(0);
        id.setIsList(1);
        id.setIsQuery(0);

        GenTable table = new GenTable();
        table.setTableName(tableName);
        table.setTableComment(functionName);
        table.setClassName(className);
        table.setFunctionName(functionName);
        table.setBusinessName("order");
        table.setModuleName("procurement");
        table.setPackageName("com.mdframe.forge");
        table.setAuthor("Forge Generator");
        table.setColumns(List.of(id));
        table.setPkColumn(id);
        return table;
    }

    private Path resolveModuleRoot() {
        Path current = Path.of("").toAbsolutePath();
        for (Path candidate : new Path[]{
                current,
                current.resolve("forge-framework/forge-plugin-parent/forge-plugin-generator"),
                current.resolve("forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator")
        }) {
            if (Files.exists(candidate.resolve("src/main/java/com/mdframe/forge/plugin/generator"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("找不到 forge-plugin-generator 模块");
    }

    private Path resolveRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("forge-admin-ui"))
                    && Files.exists(current.resolve("forge-server"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("找不到 Forge 仓库根目录");
    }
}
