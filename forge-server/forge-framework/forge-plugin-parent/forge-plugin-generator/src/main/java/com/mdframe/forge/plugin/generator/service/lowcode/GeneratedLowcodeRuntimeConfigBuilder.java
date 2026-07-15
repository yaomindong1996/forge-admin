package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将任意代码生成入口传入的配置规范化为独立、可重放的下载协议配置。
 *
 * <p>该步骤位于公共代码生成策略之前，避免应用级入口已经适配而旧配置键入口仍然生成
 * 数据库依赖或 {@code /ai/crud/*} 依赖。前端继续通过共享解释器消费完整 JSON；后端由
 * VelocityCodegenStrategy 静态编译为 MyBatis-Plus Service 与 Mapper XML。</p>
 */
@Component
@RequiredArgsConstructor
public class GeneratedLowcodeRuntimeConfigBuilder {

    private static final int MAX_GENERATED_CONFIG_KEY_LENGTH = 120;
    private static final String GENERATED_CONFIG_PREFIX = "generated_";

    private final ObjectMapper objectMapper;

    public AiCrudConfig build(AiCrudConfig source, String requestedApiBase) {
        if (source == null || StringUtils.isBlank(source.getConfigKey())) {
            throw new BusinessException("代码生成配置缺少 configKey");
        }
        String apiBase = normalizeBusinessApiBase(requestedApiBase);
        Map<String, Object> options = readJsonObject(source.getOptions(), "options");
        String sourceConfigKey = resolveSourceConfigKey(source.getConfigKey(), options);
        String runtimeConfigKey = isGeneratedConfigKey(source.getConfigKey())
                ? source.getConfigKey()
                : buildGeneratedConfigKey(apiBase, sourceConfigKey);
        String frontendRoute = "/" + source.getConfigKey().replace("_", "/");

        AiCrudConfig target;
        try {
            target = objectMapper.convertValue(source, AiCrudConfig.class);
        } catch (Exception e) {
            throw new BusinessException("复制低代码生成配置失败", e);
        }

        rewriteGenericRuntimeLinks(options, apiBase, frontendRoute, sourceConfigKey);
        Map<String, Object> codegen = mutableNestedObject(options, "codegen");
        codegen.put("businessApiBase", apiBase);
        codegen.put("sourceConfigKey", sourceConfigKey);
        codegen.put("runtimeConfigKey", runtimeConfigKey);
        codegen.put("runtimeContract", LowcodeProtocolSnapshotBuilder.RUNTIME_CONTRACT);
        codegen.put("backendMode", "STATIC_MYBATIS_PLUS");
        if (booleanValue(options.get("enableCustomQuery"))) {
            codegen.put("sourceEnableCustomQuery", true);
            codegen.put("staticRuntimeOverrides", Map.of(
                    "enableCustomQuery", false,
                    "reason", "复杂组合查询需在下载模块中通过 Mapper XML 和 ServiceExtension 实现"
            ));
            options.put("enableCustomQuery", false);
        }
        options.put("codegen", codegen);

        target.setConfigKey(runtimeConfigKey);
        target.setStatus("0");
        target.setMode("CONFIG");
        target.setBuildMode("LOWCODE");
        target.setPublishStatus("PUBLISHED");
        target.setOptions(writeJson(options, "低代码 options"));
        target.setApiConfig(writeJson(buildBusinessApiConfig(
                source.getApiConfig(), apiBase, frontendRoute, sourceConfigKey,
                hasTreeCapability(source, options)), "低代码 apiConfig"));
        target.setModelSchema(rewriteJson(
                source.getModelSchema(), apiBase, frontendRoute, sourceConfigKey, "modelSchema"));
        target.setPageSchema(rewriteJson(
                source.getPageSchema(), apiBase, frontendRoute, sourceConfigKey, "pageSchema"));
        target.setSearchSchema(rewriteJson(
                source.getSearchSchema(), apiBase, frontendRoute, sourceConfigKey, "searchSchema"));
        target.setColumnsSchema(rewriteJson(
                source.getColumnsSchema(), apiBase, frontendRoute, sourceConfigKey, "columnsSchema"));
        target.setEditSchema(rewriteJson(
                source.getEditSchema(), apiBase, frontendRoute, sourceConfigKey, "editSchema"));
        target.setDictConfig(rewriteJson(
                source.getDictConfig(), apiBase, frontendRoute, sourceConfigKey, "dictConfig"));
        target.setDesensitizeConfig(rewriteJson(
                source.getDesensitizeConfig(), apiBase, frontendRoute, sourceConfigKey, "desensitizeConfig"));
        target.setEncryptConfig(rewriteJson(
                source.getEncryptConfig(), apiBase, frontendRoute, sourceConfigKey, "encryptConfig"));
        target.setTransConfig(rewriteJson(
                source.getTransConfig(), apiBase, frontendRoute, sourceConfigKey, "transConfig"));
        assertNoGenericRuntimeApi(target);
        return target;
    }

    public String buildGeneratedConfigKey(String apiBase, String sourceConfigKey) {
        String source = StringUtils.firstNonBlank(apiBase, sourceConfigKey, "app");
        String normalized = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
        String result = GENERATED_CONFIG_PREFIX
                + StringUtils.defaultIfBlank(normalized, "app");
        return StringUtils.left(result, MAX_GENERATED_CONFIG_KEY_LENGTH);
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
        String lower = apiBase.toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(apiBase) || "/".equals(apiBase)
                || lower.startsWith("/ai/crud/")
                || lower.startsWith("/ai/crud-config")
                || lower.startsWith("/ai/lowcode/")
                || lower.startsWith("/rest/")
                || lower.contains("{configkey}")) {
            throw new BusinessException("下载代码必须使用独立业务接口前缀");
        }
        return apiBase;
    }

    private Map<String, Object> buildBusinessApiConfig(String sourceJson,
                                                       String apiBase,
                                                       String frontendRoute,
                                                       String sourceConfigKey,
                                                       boolean treeEnabled) {
        Map<String, Object> apiConfig = readJsonObject(sourceJson, "apiConfig");
        rewriteGenericRuntimeLinks(apiConfig, apiBase, frontendRoute, sourceConfigKey);
        apiConfig.put("list", "get@" + apiBase + "/page");
        if (treeEnabled) {
            apiConfig.put("tree", "get@" + apiBase + "/tree");
        } else {
            apiConfig.remove("tree");
        }
        apiConfig.put("detail", "post@" + apiBase + "/getById");
        apiConfig.put("add", "post@" + apiBase + "/add");
        apiConfig.put("create", "post@" + apiBase + "/add");
        apiConfig.put("update", "post@" + apiBase + "/edit");
        apiConfig.put("delete", "post@" + apiBase + "/remove/:id");
        apiConfig.put("importTemplate", "get@" + apiBase + "/import-template");
        apiConfig.put("import", "post@" + apiBase + "/import");
        apiConfig.put("export", "post@" + apiBase + "/export");
        apiConfig.remove("exportTasks");
        apiConfig.remove("exportTask");
        return apiConfig;
    }

    private boolean hasTreeCapability(AiCrudConfig source, Map<String, Object> options) {
        if ("tree-crud".equalsIgnoreCase(source.getLayoutType())) {
            return true;
        }
        Object treeConfig = options.get("treeConfig");
        return treeConfig instanceof Map<?, ?> tree && !tree.isEmpty();
    }

    private boolean isGeneratedConfigKey(String configKey) {
        return StringUtils.startsWith(configKey, GENERATED_CONFIG_PREFIX);
    }

    private boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private String resolveSourceConfigKey(String configKey, Map<String, Object> options) {
        Object codegenValue = options.get("codegen");
        if (codegenValue instanceof Map<?, ?> codegen) {
            Object value = codegen.get("sourceConfigKey");
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return configKey;
    }

    private Map<String, Object> mutableNestedObject(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new BusinessException("低代码协议 " + key + " 必须是 JSON 对象");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((itemKey, itemValue) -> result.put(String.valueOf(itemKey), itemValue));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonObject(String json, String label) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        Object value;
        try {
            value = objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new BusinessException("低代码协议 " + label + " 不是合法 JSON", e);
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new BusinessException("低代码协议 " + label + " 必须是 JSON 对象");
        }
        return new LinkedHashMap<>((Map<String, Object>) map);
    }

    private String rewriteJson(String json,
                               String apiBase,
                               String frontendRoute,
                               String sourceConfigKey,
                               String label) {
        if (StringUtils.isBlank(json)) {
            return json;
        }
        Object document;
        try {
            document = objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new BusinessException("低代码协议 " + label + " 不是合法 JSON", e);
        }
        rewriteGenericRuntimeLinks(document, apiBase, frontendRoute, sourceConfigKey);
        return writeJson(document, "低代码 " + label);
    }

    @SuppressWarnings("unchecked")
    private void rewriteGenericRuntimeLinks(Object value,
                                            String apiBase,
                                            String frontendRoute,
                                            String sourceConfigKey) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object child = entry.getValue();
                if (child instanceof String text) {
                    ((Map<Object, Object>) map).put(entry.getKey(),
                            rewriteGenericRuntimeText(text, apiBase, frontendRoute, sourceConfigKey));
                } else {
                    rewriteGenericRuntimeLinks(child, apiBase, frontendRoute, sourceConfigKey);
                }
            }
        } else if (value instanceof java.util.List<?> list) {
            for (int index = 0; index < list.size(); index++) {
                Object child = list.get(index);
                if (child instanceof String text) {
                    ((java.util.List<Object>) list).set(index,
                            rewriteGenericRuntimeText(text, apiBase, frontendRoute, sourceConfigKey));
                } else {
                    rewriteGenericRuntimeLinks(child, apiBase, frontendRoute, sourceConfigKey);
                }
            }
        }
    }

    private String rewriteGenericRuntimeText(String text,
                                             String apiBase,
                                             String frontendRoute,
                                             String sourceConfigKey) {
        if (StringUtils.isBlank(sourceConfigKey)) {
            return text;
        }
        String result = replaceBoundedPath(text, "/ai/crud/" + sourceConfigKey, apiBase);
        return replaceBoundedPath(result, "/ai/crud-page/" + sourceConfigKey, frontendRoute);
    }

    private String replaceBoundedPath(String text, String sourcePath, String targetPath) {
        return text.replaceAll(
                Pattern.quote(sourcePath) + "(?=$|[/\\?#&])",
                Matcher.quoteReplacement(targetPath));
    }

    private void assertNoGenericRuntimeApi(AiCrudConfig config) {
        for (String json : new String[]{
                config.getModelSchema(), config.getPageSchema(), config.getSearchSchema(),
                config.getColumnsSchema(), config.getEditSchema(), config.getApiConfig(),
                config.getOptions(), config.getDictConfig(), config.getDesensitizeConfig(),
                config.getEncryptConfig(), config.getTransConfig()
        }) {
            if (StringUtils.contains(json, "/ai/crud/")) {
                throw new BusinessException("低代码协议仍包含平台通用运行接口，无法生成独立代码包");
            }
        }
    }

    private String writeJson(Object value, String label) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(label + "生成失败", e);
        }
    }
}
