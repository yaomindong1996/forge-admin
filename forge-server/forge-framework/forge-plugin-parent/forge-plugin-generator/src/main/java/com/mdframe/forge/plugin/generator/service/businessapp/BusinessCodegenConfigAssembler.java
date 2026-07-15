package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeCodegenRequest;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeCodegenService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeProtocolSnapshotBuilder;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将统一低代码运行配置装配为可独立部署的业务代码生成配置。
 */
@Component
@RequiredArgsConstructor
public class BusinessCodegenConfigAssembler {

    private static final int MAX_GENERATED_CONFIG_KEY_LENGTH = 120;

    private final LowcodeCodegenService lowcodeCodegenService;

    public AiCrudConfig prepare(AiCrudConfig config,
                                LowcodeCodegenRequest request,
                                JSONObject codegenOptions,
                                String requestedApiBase) {
        if (config == null) {
            throw new BusinessException("代码生成配置不存在");
        }
        String apiBase = normalizeBusinessApiBase(requestedApiBase);
        String sourceConfigKey = config.getConfigKey();
        JSONObject codegen = new JSONObject();
        if (codegenOptions != null) {
            codegen.putAll(codegenOptions);
        }
        codegen.put("businessApiBase", apiBase);

        JSONObject options = readOptions(config.getOptions());
        options.put("codegen", codegen);
        mirrorCodegenOptions(options, codegen);
        config.setOptions(writeOptions(options));

        AiCrudConfig prepared = lowcodeCodegenService.prepareConfigForCodegen(config, request);
        String generatedConfigKey = buildGeneratedConfigKey(apiBase, sourceConfigKey);
        String frontendRoute = "/" + generatedConfigKey.replace("_", "/");
        prepared.setConfigKey(generatedConfigKey);
        prepared.setStatus("0");
        prepared.setMode("CONFIG");
        prepared.setBuildMode("LOWCODE");
        prepared.setPublishStatus("PUBLISHED");
        prepared.setApiConfig(buildBusinessApiConfig(apiBase, prepared).toJSONString());

        JSONObject preparedOptions = readOptions(prepared.getOptions());
        JSONObject mergedCodegen = readCodegen(preparedOptions);
        mergedCodegen.putAll(codegen);
        mergedCodegen.put("businessApiBase", apiBase);
        mergedCodegen.put("sourceConfigKey", sourceConfigKey);
        mergedCodegen.put("runtimeConfigKey", generatedConfigKey);
        mergedCodegen.put("runtimeContract", LowcodeProtocolSnapshotBuilder.RUNTIME_CONTRACT);
        mergedCodegen.put("backendMode", "STATIC_MYBATIS_PLUS");
        preparedOptions.put("codegen", mergedCodegen);
        mirrorCodegenOptions(preparedOptions, mergedCodegen);
        rewriteGenericRuntimeLinks(preparedOptions, apiBase, frontendRoute, sourceConfigKey);
        prepared.setOptions(writeOptions(preparedOptions));
        prepared.setModelSchema(rewriteGenericRuntimeJson(
                prepared.getModelSchema(), apiBase, frontendRoute, sourceConfigKey));
        prepared.setPageSchema(rewriteGenericRuntimeJson(
                prepared.getPageSchema(), apiBase, frontendRoute, sourceConfigKey));
        prepared.setSearchSchema(rewriteGenericRuntimeJson(
                prepared.getSearchSchema(), apiBase, frontendRoute, sourceConfigKey));
        prepared.setColumnsSchema(rewriteGenericRuntimeJson(
                prepared.getColumnsSchema(), apiBase, frontendRoute, sourceConfigKey));
        prepared.setEditSchema(rewriteGenericRuntimeJson(
                prepared.getEditSchema(), apiBase, frontendRoute, sourceConfigKey));
        prepared.setDictConfig(rewriteGenericRuntimeJson(
                prepared.getDictConfig(), apiBase, frontendRoute, sourceConfigKey));
        prepared.setDesensitizeConfig(rewriteGenericRuntimeJson(
                prepared.getDesensitizeConfig(), apiBase, frontendRoute, sourceConfigKey));
        prepared.setEncryptConfig(rewriteGenericRuntimeJson(
                prepared.getEncryptConfig(), apiBase, frontendRoute, sourceConfigKey));
        prepared.setTransConfig(rewriteGenericRuntimeJson(
                prepared.getTransConfig(), apiBase, frontendRoute, sourceConfigKey));
        return prepared;
    }

    public String buildGeneratedConfigKey(String apiBase, String sourceConfigKey) {
        String source = StringUtils.firstNonBlank(apiBase, sourceConfigKey, "app");
        String normalized = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
        String result = "generated_" + StringUtils.defaultIfBlank(normalized, "app");
        return StringUtils.left(result, MAX_GENERATED_CONFIG_KEY_LENGTH);
    }

    public void assertNoGenericRuntimeApi(Map<String, String> files) {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            if (StringUtils.contains(entry.getValue(), "/ai/crud/")) {
                throw new BusinessException("功能代码仍包含平台通用运行接口，请检查代码包设置: " + entry.getKey());
            }
        }
    }

    public String normalizeBusinessApiBase(String value) {
        String apiBase = StringUtils.trimToEmpty(value).replace("\\", "/");
        if (StringUtils.isBlank(apiBase)) {
            throw new BusinessException("业务接口前缀不能为空");
        }
        if (!apiBase.startsWith("/")) {
            apiBase = "/" + apiBase;
        }
        apiBase = apiBase.replaceAll("/{2,}", "/").replaceAll("/+$", "");
        if (StringUtils.isBlank(apiBase) || "/".equals(apiBase)) {
            throw new BusinessException("业务接口前缀不能为空");
        }
        String lower = apiBase.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/ai/crud/")
                || lower.startsWith("/ai/crud-config")
                || lower.startsWith("/ai/lowcode/")
                || lower.startsWith("/rest/")) {
            throw new BusinessException("下载代码模式不能使用平台通用运行接口");
        }
        if (lower.contains("{configkey}")) {
            throw new BusinessException("业务接口前缀不能包含旧配置标识");
        }
        return apiBase;
    }

    public String toPathSegment(String value) {
        String text = StringUtils.defaultString(value, "app").trim();
        text = text.replaceAll("([a-z0-9])([A-Z])", "$1-$2");
        text = text.replace('_', '-');
        text = text.replaceAll("[^A-Za-z0-9\\-]+", "-");
        text = text.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return StringUtils.defaultIfBlank(text, "app").toLowerCase(Locale.ROOT);
    }

    private JSONObject buildBusinessApiConfig(String apiBase, AiCrudConfig config) {
        JSONObject apiConfig = new JSONObject();
        apiConfig.put("list", "get@" + apiBase + "/page");
        JSONObject options = readOptions(config.getOptions());
        JSONObject treeConfig = options.getJSONObject("treeConfig");
        if ("tree-crud".equalsIgnoreCase(config.getLayoutType())
                || (treeConfig != null && !treeConfig.isEmpty())) {
            apiConfig.put("tree", "get@" + apiBase + "/tree");
        }
        apiConfig.put("detail", "post@" + apiBase + "/getById");
        apiConfig.put("add", "post@" + apiBase + "/add");
        apiConfig.put("create", "post@" + apiBase + "/add");
        apiConfig.put("update", "post@" + apiBase + "/edit");
        apiConfig.put("delete", "post@" + apiBase + "/remove/:id");
        apiConfig.put("importTemplate", "get@" + apiBase + "/import-template");
        apiConfig.put("import", "post@" + apiBase + "/import");
        apiConfig.put("export", "post@" + apiBase + "/export");
        return apiConfig;
    }

    private void mirrorCodegenOptions(JSONObject options, JSONObject codegen) {
        copyIfPresent(options, codegen, "domainPackage", "packageName");
        copyIfPresent(options, codegen, "moduleName", "moduleName");
        copyIfPresent(options, codegen, "author", "author");
        copyIfDefined(options, codegen, "entityPrefix", "entityPrefix");
        copyIfPresent(options, codegen, "stripTablePrefixes", "stripTablePrefixes");
        copyIfPresent(options, codegen, "backendBasePath", "backendBasePath");
        copyIfPresent(options, codegen, "mapperXmlBasePath", "mapperXmlBasePath");
        copyIfPresent(options, codegen, "frontendBasePath", "frontendBasePath");
        copyIfPresent(options, codegen, "frontendApiBasePath", "frontendApiBasePath");
        copyIfPresent(options, codegen, "includeBackend", "includeBackend");
        copyIfPresent(options, codegen, "includeFrontend", "includeFrontend");
        copyIfPresent(options, codegen, "includeSql", "includeSql");
        copyIfPresent(options, codegen, "includeMenuSql", "includeMenuSql");
        copyIfPresent(options, codegen, "includeDictSql", "includeDictSql");
        copyIfPresent(options, codegen, "includeExcelSql", "includeExcelSql");
    }

    private void copyIfPresent(JSONObject target, JSONObject source, String sourceKey, String targetKey) {
        Object value = source.get(sourceKey);
        if (value != null && !(value instanceof String text && StringUtils.isBlank(text))) {
            target.put(targetKey, value);
        }
    }

    private void copyIfDefined(JSONObject target, JSONObject source, String sourceKey, String targetKey) {
        if (source.containsKey(sourceKey)) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    private void rewriteGenericRuntimeLinks(Object value,
                                            String apiBase,
                                            String frontendRoute,
                                            String configKey) {
        if (value instanceof JSONObject object) {
            for (String key : object.keySet()) {
                Object child = object.get(key);
                if (child instanceof String text) {
                    object.put(key, rewriteGenericRuntimeText(text, apiBase, frontendRoute, configKey));
                } else {
                    rewriteGenericRuntimeLinks(child, apiBase, frontendRoute, configKey);
                }
            }
        } else if (value instanceof JSONArray array) {
            for (int index = 0; index < array.size(); index++) {
                Object child = array.get(index);
                if (child instanceof String text) {
                    array.set(index, rewriteGenericRuntimeText(text, apiBase, frontendRoute, configKey));
                } else {
                    rewriteGenericRuntimeLinks(child, apiBase, frontendRoute, configKey);
                }
            }
        }
    }

    private String rewriteGenericRuntimeText(String text,
                                             String apiBase,
                                             String frontendRoute,
                                             String configKey) {
        String result = text;
        if (StringUtils.isNotBlank(configKey)) {
            result = replaceBoundedPath(result, "/ai/crud/" + configKey, apiBase);
            result = replaceBoundedPath(result, "/ai/crud-page/" + configKey, frontendRoute);
        }
        return result;
    }

    private String replaceBoundedPath(String text, String sourcePath, String targetPath) {
        return text.replaceAll(
                Pattern.quote(sourcePath) + "(?=$|[/\\?#&])",
                Matcher.quoteReplacement(targetPath));
    }

    private String rewriteGenericRuntimeJson(String json,
                                             String apiBase,
                                             String frontendRoute,
                                             String configKey) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        try {
            Object document = JSON.parse(json);
            rewriteGenericRuntimeLinks(document, apiBase, frontendRoute, configKey);
            return JSON.toJSONString(document);
        } catch (Exception e) {
            throw new BusinessException("低代码协议 JSON 解析失败，不能生成完整代码包", e);
        }
    }

    private JSONObject readOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(options);
        } catch (Exception e) {
            throw new BusinessException("低代码 options 不是合法 JSON 对象", e);
        }
    }

    private JSONObject readCodegen(JSONObject options) {
        JSONObject codegen = options == null ? null : options.getJSONObject("codegen");
        return codegen == null ? new JSONObject() : codegen;
    }

    private String writeOptions(JSONObject options) {
        return options == null || options.isEmpty() ? null : options.toJSONString();
    }
}
