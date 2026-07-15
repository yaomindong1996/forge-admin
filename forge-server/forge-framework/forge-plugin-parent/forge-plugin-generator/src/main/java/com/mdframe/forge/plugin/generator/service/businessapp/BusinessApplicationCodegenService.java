package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeCodegenRequest;
import com.mdframe.forge.plugin.generator.service.AiCrudCodegenService;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeProtocolSnapshotBuilder;
import com.mdframe.forge.plugin.generator.util.LowcodeCodegenOptionUtils;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeCodePreviewVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 业务应用维度的完整代码预览与批量下载服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationCodegenService {

    private static final String SOURCE_DRAFT = "DRAFT";
    private static final String SOURCE_PUBLISHED = "PUBLISHED";
    private static final Set<String> SUPPORTED_SOURCE_TYPES = Set.of(SOURCE_DRAFT, SOURCE_PUBLISHED);
    private static final Pattern JAVA_PACKAGE_PATTERN = Pattern.compile(
            "^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*$");
    private static final Pattern JAVA_MODULE_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessObjectDesignerService objectDesignerService;
    private final AiCrudConfigService crudConfigService;
    private final AiCrudCodegenService codegenService;
    private final BusinessCodegenConfigAssembler configAssembler;

    public Map<String, Object> getOptions(Long applicationId) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        List<BusinessApplicationObjectVO> objects = applicationObjectService.list(applicationId);
        JSONObject result = buildCodegenOptions(application, readOptions(application.getOptions()), null);
        JSONArray objectOptions = new JSONArray();
        for (BusinessApplicationObjectVO object : objects) {
            objectOptions.add(buildObjectOption(object));
        }
        result.put("objects", objectOptions);
        result.put("objectIds", objects.stream().map(BusinessApplicationObjectVO::getObjectId).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveOptions(Long applicationId, LowcodeCodegenRequest request) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        JSONObject options = readOptions(application.getOptions());
        options.put("codegen", buildCodegenOptions(application, options, request));
        application.setOptions(writeOptions(options));
        applicationService.updateById(application);
    }

    public LowcodeCodePreviewVO previewCode(Long applicationId, LowcodeCodegenRequest request) {
        GeneratedApplicationPackage generated = generateApplicationPackage(applicationId, request);
        LowcodeCodePreviewVO result = new LowcodeCodePreviewVO();
        result.setAppId(generated.application().getId());
        result.setConfigKey(generated.application().getApplicationCode());
        result.setSourceType(resolveSourceType(request));
        result.setFiles(generated.files());
        result.setFileCount(generated.files().size());
        return result;
    }

    public byte[] downloadCode(Long applicationId, LowcodeCodegenRequest request) {
        return codegenService.toZip(generateApplicationPackage(applicationId, request).files());
    }

    public String resolveDownloadFilename(Long applicationId) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        return configAssembler.toPathSegment(StringUtils.firstNonBlank(
                application.getApplicationCode(), String.valueOf(application.getId()))) + "-source.zip";
    }

    private GeneratedApplicationPackage generateApplicationPackage(
            Long applicationId, LowcodeCodegenRequest request) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        String sourceType = resolveSourceType(request);
        List<BusinessApplicationObjectVO> applicationObjects = applicationObjectService.list(applicationId);
        List<BusinessApplicationObjectVO> selectedObjects = selectObjects(applicationObjects, request);
        if (selectedObjects.isEmpty()) {
            throw new BusinessException("请至少选择一个需要生成代码的数据对象");
        }

        JSONObject codegenOptions = buildCodegenOptions(application, readOptions(application.getOptions()), request);
        Map<String, String> files = new LinkedHashMap<>();
        List<Map<String, Object>> generatedObjects = new ArrayList<>();
        Set<Long> consumedObjectIds = new LinkedHashSet<>();

        BusinessApplicationObjectVO primary = selectedObjects.stream()
                .filter(item -> BusinessApplicationObjectRole.PRIMARY.equals(item.getObjectRole()))
                .findFirst()
                .orElse(null);
        if (primary != null) {
            AiCrudConfig primaryConfig = resolveObjectConfig(primary, sourceType);
            generateObjectFiles(primary, primaryConfig, request, codegenOptions,
                    files, generatedObjects);
            consumedObjectIds.add(primary.getObjectId());
            consumedObjectIds.addAll(resolveAggregatedObjectIds(primaryConfig, selectedObjects));
        }

        for (BusinessApplicationObjectVO object : selectedObjects) {
            if (consumedObjectIds.contains(object.getObjectId())) {
                continue;
            }
            AiCrudConfig config = resolveObjectConfig(object, sourceType);
            generateObjectFiles(object, config, request, codegenOptions,
                    files, generatedObjects);
            consumedObjectIds.add(object.getObjectId());
        }

        files.remove("README.md");
        files.put("config/application-manifest.json", buildApplicationManifest(
                application, sourceType, selectedObjects, generatedObjects, codegenOptions));
        files.put("README.md", buildApplicationReadme(
                application, sourceType, selectedObjects, generatedObjects, codegenOptions));
        return new GeneratedApplicationPackage(application, files);
    }

    private AiCrudConfig resolveObjectConfig(BusinessApplicationObjectVO object, String sourceType) {
        if (StringUtils.isBlank(object.getConfigKey())) {
            throw new BusinessException("对象「" + displayName(object) + "」还没有可生成的页面配置");
        }
        if (SOURCE_DRAFT.equals(sourceType)) {
            return objectDesignerService.prepareRuntimeDraft(object.getObjectId());
        }
        AiCrudConfig config = crudConfigService.getByConfigKey(object.getConfigKey());
        if (config == null) {
            throw new BusinessException("对象「" + displayName(object) + "」的代码生成配置不存在");
        }
        if (!"PUBLISHED".equals(config.getPublishStatus())) {
            throw new BusinessException("对象「" + displayName(object) + "」尚未发布，不能按发布版本生成代码");
        }
        return config;
    }

    private void generateObjectFiles(BusinessApplicationObjectVO object,
                                     AiCrudConfig sourceConfig,
                                     LowcodeCodegenRequest request,
                                     JSONObject codegenOptions,
                                     Map<String, String> targetFiles,
                                     List<Map<String, Object>> generatedObjects) {
        String apiBase = buildObjectApiBase(codegenOptions.getString("moduleName"), object.getObjectCode());
        String sourceConfigKey = sourceConfig.getConfigKey();
        AiCrudConfig prepared = configAssembler.prepare(
                sourceConfig, request, codegenOptions, apiBase);
        Map<String, String> objectFiles = codegenService.generateFiles(prepared);
        configAssembler.assertNoGenericRuntimeApi(objectFiles);
        objectFiles.remove("README.md");
        mergeFiles(targetFiles, objectFiles, object);

        Map<String, Object> generatedObject = new LinkedHashMap<>();
        generatedObject.put("objectId", object.getObjectId());
        generatedObject.put("objectCode", object.getObjectCode());
        generatedObject.put("objectName", object.getObjectName());
        generatedObject.put("objectRole", object.getObjectRole());
        generatedObject.put("sourceConfigKey", sourceConfigKey);
        generatedObject.put("configKey", prepared.getConfigKey());
        generatedObject.put("layoutType", prepared.getLayoutType());
        generatedObject.put("tableName", prepared.getTableName());
        generatedObject.put("apiBase", apiBase);
        generatedObject.put("runtimeContract", LowcodeProtocolSnapshotBuilder.RUNTIME_CONTRACT);
        generatedObject.put("protocolPath", "config/" + prepared.getConfigKey() + "-protocol.json");
        generatedObject.put("coveragePath", "config/" + prepared.getConfigKey() + "-coverage.json");
        generatedObject.put("ownershipPath", "config/" + prepared.getConfigKey() + "-ownership.json");
        generatedObjects.add(generatedObject);
    }

    private void mergeFiles(Map<String, String> target,
                            Map<String, String> source,
                            BusinessApplicationObjectVO object) {
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String existing = target.putIfAbsent(entry.getKey(), entry.getValue());
            if (existing != null && !StringUtils.equals(existing, entry.getValue())) {
                throw new BusinessException("应用代码包文件冲突: " + entry.getKey()
                        + "，请检查对象「" + displayName(object) + "」的表名或类名配置");
            }
        }
    }

    private Set<Long> resolveAggregatedObjectIds(AiCrudConfig primaryConfig,
                                                  List<BusinessApplicationObjectVO> selectedObjects) {
        Set<String> relatedModelCodes = new LinkedHashSet<>();
        Set<String> relatedTables = new LinkedHashSet<>();
        try {
            JSONObject pageSchema = JSON.parseObject(primaryConfig.getPageSchema());
            JSONArray refs = pageSchema == null ? null : pageSchema.getJSONArray("modelRefs");
            if (refs != null) {
                for (Object item : refs) {
                    JSONObject ref = item instanceof JSONObject json ? json : JSON.parseObject(JSON.toJSONString(item));
                    if (ref == null || ref.getBooleanValue("primary")) {
                        continue;
                    }
                    addIfNotBlank(relatedModelCodes, ref.getString("modelCode"));
                    addIfNotBlank(relatedTables, ref.getString("tableName"));
                }
            }
        } catch (Exception ignored) {
            return Set.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (BusinessApplicationObjectVO object : selectedObjects) {
            if (BusinessApplicationObjectRole.PRIMARY.equals(object.getObjectRole())) {
                continue;
            }
            if (relatedTables.contains(object.getTableName()) || relatedModelCodes.stream()
                    .anyMatch(modelCode -> matchesObjectCode(modelCode, object.getObjectCode()))) {
                result.add(object.getObjectId());
            }
        }
        return result;
    }

    private boolean matchesObjectCode(String modelCode, String objectCode) {
        if (StringUtils.isBlank(modelCode) || StringUtils.isBlank(objectCode)) {
            return false;
        }
        String normalizedModel = modelCode.toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
        String normalizedObject = objectCode.toLowerCase(Locale.ROOT).replace('-', '_').replace('.', '_');
        return normalizedModel.equals(normalizedObject) || normalizedModel.endsWith("_" + normalizedObject);
    }

    private List<BusinessApplicationObjectVO> selectObjects(
            List<BusinessApplicationObjectVO> objects, LowcodeCodegenRequest request) {
        if (objects == null || objects.isEmpty()) {
            throw new BusinessException("应用还没有数据对象，不能生成代码");
        }
        List<Long> requestedIds = request == null ? null : request.getObjectIds();
        if (requestedIds == null || requestedIds.isEmpty()) {
            return new ArrayList<>(objects);
        }
        Set<Long> selectedIds = new LinkedHashSet<>(requestedIds);
        List<BusinessApplicationObjectVO> selected = objects.stream()
                .filter(item -> selectedIds.contains(item.getObjectId()))
                .toList();
        Set<Long> knownIds = selected.stream()
                .map(BusinessApplicationObjectVO::getObjectId)
                .collect(java.util.stream.Collectors.toSet());
        selectedIds.removeAll(knownIds);
        if (!selectedIds.isEmpty()) {
            throw new BusinessException("所选数据对象不属于当前应用: " + selectedIds.iterator().next());
        }
        return selected;
    }

    private JSONObject buildCodegenOptions(AiBusinessApplication application,
                                           JSONObject applicationOptions,
                                           LowcodeCodegenRequest request) {
        JSONObject existing = readCodegen(applicationOptions);
        JSONObject result = new JSONObject();
        result.put("sourceType", resolveSourceType(request, existing.getString("sourceType")));
        String defaultModule = defaultModuleName(application);
        String moduleName = StringUtils.firstNonBlank(
                request == null ? null : request.getModuleName(),
                existing.getString("moduleName"),
                defaultModule);
        validateJavaModule(moduleName);
        result.put("moduleName", moduleName);

        String domainPackage = StringUtils.firstNonBlank(
                request == null ? null : request.getDomainPackage(),
                existing.getString("domainPackage"),
                existing.getString("packageName"),
                "com.mdframe.forge");
        validateJavaPackage(domainPackage, "Java 基础包名");
        result.put("domainPackage", domainPackage);

        String groupId = StringUtils.firstNonBlank(
                request == null ? null : request.getGroupId(),
                existing.getString("groupId"),
                domainPackage);
        validateJavaPackage(groupId, "Maven groupId");
        result.put("groupId", groupId);
        result.put("author", StringUtils.firstNonBlank(
                request == null ? null : request.getAuthor(),
                existing.getString("author"),
                "Forge Generator"));
        String requestedEntityPrefix = request == null ? null : request.getEntityPrefix();
        result.put("entityPrefix", LowcodeCodegenOptionUtils.normalizeEntityPrefix(
                requestedEntityPrefix != null ? requestedEntityPrefix : existing.getString("entityPrefix")));
        result.put("stripTablePrefixes", LowcodeCodegenOptionUtils.resolveStripTablePrefixes(
                request == null ? null : request.getStripTablePrefixes(),
                existing.get("stripTablePrefixes"), LowcodeCodegenOptionUtils.DEFAULT_STRIP_TABLE_PREFIXES));
        result.put("backendBasePath", LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getBackendBasePath(), existing.getString("backendBasePath")),
                LowcodeCodegenOptionUtils.DEFAULT_BACKEND_BASE_PATH, "后端 Java 输出路径"));
        result.put("mapperXmlBasePath", LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getMapperXmlBasePath(), existing.getString("mapperXmlBasePath")),
                LowcodeCodegenOptionUtils.DEFAULT_MAPPER_XML_BASE_PATH, "Mapper XML 输出路径"));
        result.put("frontendBasePath", LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                request == null ? null : request.getFrontendBasePath(),
                existing.getString("frontendBasePath")),
                LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_BASE_PATH, "前端页面输出路径"));
        result.put("frontendApiBasePath", LowcodeCodegenOptionUtils.normalizeOutputPath(StringUtils.firstNonBlank(
                        request == null ? null : request.getFrontendApiBasePath(), existing.getString("frontendApiBasePath")),
                LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_API_BASE_PATH, "前端 API 输出路径"));
        result.put("includeBackend", resolveBoolean(request == null ? null : request.getIncludeBackend(),
                existing.get("includeBackend"), true));
        result.put("includeFrontend", resolveBoolean(request == null ? null : request.getIncludeFrontend(),
                existing.get("includeFrontend"), true));
        result.put("includeSql", resolveBoolean(request == null ? null : request.getIncludeSql(),
                existing.get("includeSql"), true));
        result.put("includeMenuSql", resolveBoolean(request == null ? null : request.getIncludeMenuSql(),
                existing.get("includeMenuSql"), true));
        result.put("includeDictSql", resolveBoolean(request == null ? null : request.getIncludeDictSql(),
                existing.get("includeDictSql"), true));
        result.put("includeExcelSql", resolveBoolean(request == null ? null : request.getIncludeExcelSql(),
                existing.get("includeExcelSql"), true));
        return result;
    }

    private JSONObject buildObjectOption(BusinessApplicationObjectVO object) {
        JSONObject result = new JSONObject();
        result.put("objectId", object.getObjectId());
        result.put("objectCode", object.getObjectCode());
        result.put("objectName", object.getObjectName());
        result.put("objectRole", object.getObjectRole());
        result.put("configKey", object.getConfigKey());
        result.put("designStatus", object.getDesignStatus());
        result.put("tableName", object.getTableName());
        result.put("layoutType", object.getLayoutType());
        return result;
    }

    private String buildApplicationManifest(AiBusinessApplication application,
                                            String sourceType,
                                            List<BusinessApplicationObjectVO> selectedObjects,
                                            List<Map<String, Object>> generatedObjects,
                                            JSONObject codegenOptions) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("applicationId", application.getId());
        manifest.put("applicationCode", application.getApplicationCode());
        manifest.put("applicationName", application.getApplicationName());
        manifest.put("suiteCode", application.getSuiteCode());
        manifest.put("sourceType", sourceType);
        manifest.put("runtimeContract", LowcodeProtocolSnapshotBuilder.RUNTIME_CONTRACT);
        manifest.put("autoAdaptationStrategy", "SHARED_FRONTEND_STATIC_BACKEND_COMPILER");
        manifest.put("backendMode", "STATIC_MYBATIS_PLUS");
        List<String> deliveryScope = new ArrayList<>();
        if (codegenOptions.getBooleanValue("includeBackend")) {
            deliveryScope.add("DATA_OBJECT_BACKEND");
            deliveryScope.add("MAPPER_XML");
        }
        if (codegenOptions.getBooleanValue("includeFrontend")) {
            deliveryScope.add("DATA_OBJECT_FRONTEND");
        }
        if (codegenOptions.getBooleanValue("includeSql")) {
            deliveryScope.add("SQL");
        }
        deliveryScope.add("PROTOCOL_SNAPSHOT");
        manifest.put("deliveryScope", deliveryScope);
        manifest.put("codegenOptions", codegenOptions);
        manifest.put("managedAssetsNotMaterialized", List.of("WORKFLOW", "EXTENSION", "EXTERNAL_INTEGRATION"));
        manifest.put("selectedObjectIds", selectedObjects.stream()
                .map(BusinessApplicationObjectVO::getObjectId).toList());
        manifest.put("generatedObjects", generatedObjects);
        return JSON.toJSONString(manifest, JSONWriter.Feature.PrettyFormat);
    }

    private String buildApplicationReadme(AiBusinessApplication application,
                                          String sourceType,
                                          List<BusinessApplicationObjectVO> selectedObjects,
                                          List<Map<String, Object>> generatedObjects,
                                          JSONObject codegenOptions) {
        StringBuilder content = new StringBuilder();
        content.append("# ").append(application.getApplicationName()).append(" 应用代码包\n\n")
                .append("- 应用编码：`").append(application.getApplicationCode()).append("`\n")
                .append("- 业务域：`").append(application.getSuiteCode()).append("`\n")
                .append("- 生成来源：`").append(sourceType).append("`\n")
                .append("- 选择对象：").append(selectedObjects.size()).append(" 个\n\n")
                .append("## 已生成页面\n\n");
        for (Map<String, Object> object : generatedObjects) {
            content.append("- ").append(object.get("objectName"))
                    .append("（`").append(object.get("objectCode")).append("`，")
                    .append(object.get("layoutType")).append("），接口 `")
                    .append(object.get("apiBase")).append("`\n");
        }
        content.append("\n## 合并方式\n\n");
        if (codegenOptions.getBooleanValue("includeBackend")) {
            content.append("- `").append(codegenOptions.getString("backendBasePath"))
                    .append("` 下的 Java 文件与 `").append(codegenOptions.getString("mapperXmlBasePath"))
                    .append("` 下的 Mapper XML 合并到后端业务模块。\n");
        }
        if (codegenOptions.getBooleanValue("includeFrontend")) {
            content.append("- `").append(codegenOptions.getString("frontendBasePath"))
                    .append("` 下的页面与 `").append(codegenOptions.getString("frontendApiBasePath"))
                    .append("` 下的 API 合并到管理端工程。\n");
        }
        if (codegenOptions.getBooleanValue("includeSql")) {
            content.append("- `sql` 中脚本需人工审查后通过 Flyway 纳入正式版本，禁止直接在线执行。\n");
        }
        content.append("- `config/application-manifest.json` 记录本次应用对象、角色、布局、接口前缀和生成设置。\n");
        if (codegenOptions.getBooleanValue("includeFrontend")) {
            content.append("- 生成页面使用共享低代码解释器，并消费同目录完整运行配置。\n");
        }
        if (codegenOptions.getBooleanValue("includeBackend")) {
            content.append("- 业务 Controller 调用生成的 MyBatis-Plus Service，运行契约为 `")
                    .append(LowcodeProtocolSnapshotBuilder.RUNTIME_CONTRACT)
                    .append("`。\n");
        }
        content.append("- 每个对象的 `*-protocol.json` 保存完整协议，`*-coverage.json` 区分静态编译能力与需业务扩展项。\n")
                .append("- `*-ownership.json` 标记生成文件和首次复制示例；用户正式扩展实现不进入生成文件集合。\n")
                .append("- 流程、扩展与外部集成仍由 Forge 治理运行，不生成绕过治理的任意 Java 或 SQL。\n");
        return content.toString();
    }

    private String buildObjectApiBase(String moduleName, String objectCode) {
        return configAssembler.normalizeBusinessApiBase("/"
                + configAssembler.toPathSegment(moduleName) + "/"
                + configAssembler.toPathSegment(objectCode));
    }

    private String defaultModuleName(AiBusinessApplication application) {
        String source = StringUtils.firstNonBlank(application.getSuiteCode(), application.getApplicationCode(), "app");
        String normalized = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_{2,}", "_").replaceAll("^_|_$", "");
        if (StringUtils.isBlank(normalized)) {
            return "app";
        }
        return Character.isDigit(normalized.charAt(0)) ? "app_" + normalized : normalized;
    }

    private String resolveSourceType(LowcodeCodegenRequest request) {
        return resolveSourceType(request, null);
    }

    private String resolveSourceType(LowcodeCodegenRequest request, String existing) {
        String sourceType = StringUtils.firstNonBlank(
                request == null ? null : request.getSourceType(), existing, SOURCE_DRAFT)
                .toUpperCase(Locale.ROOT);
        if (!SUPPORTED_SOURCE_TYPES.contains(sourceType)) {
            throw new BusinessException("应用代码包只支持当前草稿或已发布版本");
        }
        return sourceType;
    }

    private void validateJavaModule(String value) {
        if (StringUtils.isBlank(value) || !JAVA_MODULE_PATTERN.matcher(value).matches()) {
            throw new BusinessException("模块名必须是合法 Java 标识符");
        }
    }

    private void validateJavaPackage(String value, String label) {
        if (StringUtils.isBlank(value) || !JAVA_PACKAGE_PATTERN.matcher(value).matches()) {
            throw new BusinessException(label + "格式不正确");
        }
    }

    private void addIfNotBlank(Collection<String> target, String value) {
        if (StringUtils.isNotBlank(value)) {
            target.add(value);
        }
    }

    private String displayName(BusinessApplicationObjectVO object) {
        return StringUtils.firstNonBlank(object.getObjectName(), object.getObjectCode(), String.valueOf(object.getObjectId()));
    }

    private JSONObject readOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(options);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private JSONObject readCodegen(JSONObject options) {
        JSONObject codegen = options == null ? null : options.getJSONObject("codegen");
        return codegen == null ? new JSONObject() : codegen;
    }

    private String writeOptions(JSONObject options) {
        return options == null || options.isEmpty() ? null : options.toJSONString();
    }

    private boolean resolveBoolean(Boolean requestValue, Object existingValue, boolean fallback) {
        if (requestValue != null) {
            return requestValue;
        }
        if (existingValue instanceof Boolean bool) {
            return bool;
        }
        return existingValue == null ? fallback : Boolean.parseBoolean(String.valueOf(existingValue));
    }

    private record GeneratedApplicationPackage(AiBusinessApplication application, Map<String, String> files) {
    }
}
