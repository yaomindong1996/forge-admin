package com.mdframe.forge.plugin.generator.service.lowcode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将低代码配置构建为可重放的前端配置、完整协议和静态后端编译覆盖报告。
 *
 * <p>前端嵌套协议整体交给共享解释器；后端覆盖报告只把真正生成到
 * MyBatis-Plus Service/Mapper XML 的能力标记为已编译，避免把“保存了 JSON”误报为已实现。</p>
 */
@Component
@RequiredArgsConstructor
public class LowcodeProtocolSnapshotBuilder {

    public static final String RUNTIME_CONTRACT = "forge-lowcode-static-source-v2";
    private static final String PROTOCOL_VERSION = "forge-lowcode-protocol-snapshot-v2";
    private static final String COVERAGE_VERSION = "forge-lowcode-static-coverage-v2";

    private final ObjectMapper objectMapper;

    public ProtocolArtifacts build(AiCrudConfig config) {
        if (config == null || StringUtils.isBlank(config.getConfigKey())) {
            throw new BusinessException("低代码协议快照缺少配置标识");
        }
        boolean lowcode = "LOWCODE".equalsIgnoreCase(config.getBuildMode());
        if (lowcode && (StringUtils.isBlank(config.getModelSchema())
                || StringUtils.isBlank(config.getPageSchema()))) {
            throw new BusinessException("低代码协议快照缺少 modelSchema 或 pageSchema: " + config.getConfigKey());
        }

        Map<String, Object> snapshotConfig = objectMapper.convertValue(
                config, new TypeReference<LinkedHashMap<String, Object>>() { });
        normalizeGeneratedSnapshotState(snapshotConfig, config);

        Map<String, Object> frontendConfig = new LinkedHashMap<>(snapshotConfig);
        replaceJson(frontendConfig, "modelSchema", config.getModelSchema(), JsonShape.OBJECT, lowcode);
        replaceJson(frontendConfig, "pageSchema", config.getPageSchema(), JsonShape.OBJECT, lowcode);
        replaceJson(frontendConfig, "searchSchema", config.getSearchSchema(), JsonShape.ARRAY, false);
        replaceJson(frontendConfig, "columnsSchema", config.getColumnsSchema(), JsonShape.ARRAY, false);
        replaceJson(frontendConfig, "editSchema", config.getEditSchema(), JsonShape.ARRAY, false);
        replaceJson(frontendConfig, "apiConfig", config.getApiConfig(), JsonShape.OBJECT, true);
        replaceJson(frontendConfig, "options", config.getOptions(), JsonShape.OBJECT, false);
        replaceJson(frontendConfig, "dictConfig", config.getDictConfig(), JsonShape.ANY, false);
        replaceJson(frontendConfig, "desensitizeConfig", config.getDesensitizeConfig(), JsonShape.OBJECT, false);
        replaceJson(frontendConfig, "encryptConfig", config.getEncryptConfig(), JsonShape.OBJECT, false);
        replaceJson(frontendConfig, "transConfig", config.getTransConfig(), JsonShape.OBJECT, false);
        frontendConfig.put("rowKey", StringUtils.defaultIfBlank(config.getPrimaryKeyField(), "id"));
        frontendConfig.put("runtimeContract", RUNTIME_CONTRACT);

        Map<String, Object> protocol = new LinkedHashMap<>();
        protocol.put("protocolVersion", PROTOCOL_VERSION);
        protocol.put("runtimeContract", RUNTIME_CONTRACT);
        protocol.put("configKey", config.getConfigKey());
        protocol.put("sourceConfigKey", sourceConfigKey(frontendConfig));
        protocol.put("autoAdaptation", Map.of(
                "strategy", "shared-frontend-static-backend-compiler",
                "frontendNestedProtocolFields", "passthrough",
                "backendUnknownFieldPolicy", "preserve-and-report",
                "downloadEntryCompiler", "VelocityCodegenStrategy",
                "templateFieldAllowlist", false
        ));
        protocol.put("runtimeConfig", frontendConfig);

        Map<String, Object> coverage = buildCoverage(config, frontendConfig);
        return new ProtocolArtifacts(
                writeJson(frontendConfig, "前端运行配置"),
                writeJson(protocol, "低代码协议快照"),
                writeJson(coverage, "低代码协议覆盖报告")
        );
    }

    private void normalizeGeneratedSnapshotState(Map<String, Object> target, AiCrudConfig config) {
        target.put("configKey", config.getConfigKey());
        target.put("status", "0");
        target.put("mode", "CONFIG");
        target.put("buildMode", "LOWCODE");
        target.put("publishStatus", "PUBLISHED");
        target.put("layoutType", StringUtils.defaultIfBlank(config.getLayoutType(), "simple-crud"));
    }

    private Map<String, Object> buildCoverage(AiCrudConfig config, Map<String, Object> frontendConfig) {
        List<Map<String, Object>> capabilities = new ArrayList<>();
        addCoverage(capabilities, "/modelSchema", "PROTOCOL_SNAPSHOT", "PRESERVED");
        addCoverage(capabilities, "/modelSchema/fields", "BACKEND_STATIC_COMPILER", "ENTITY_DTO_QUERY_COMPILED");
        addCoverage(capabilities, "/modelSchema/relations", "BACKEND_STATIC_COMPILER", "MAPPER_XML_COMPILED");
        addCoverage(capabilities, "/pageSchema", "FRONTEND_SHARED_RUNTIME", "PROTOCOL_PASSTHROUGH");
        addCoverage(capabilities, "/pageSchema/modelRefs", "BACKEND_STATIC_COMPILER", "RELATION_METADATA_COMPILED");
        addCoverage(capabilities, "/options/formDesignerSchema", "FRONTEND_SHARED_RUNTIME", "PROTOCOL_PASSTHROUGH");
        addCoverage(capabilities, "/options/viewSchema", "FRONTEND_SHARED_RUNTIME", "PROTOCOL_PASSTHROUGH");
        addCoverage(capabilities, "/options/linkageSchema", "FRONTEND_SHARED_RUNTIME", "PROTOCOL_PASSTHROUGH");
        addCoverage(capabilities, "/options/treeConfig", "FRONTEND_AND_BACKEND_STATIC", "TREE_MAPPER_XML_COMPILED");
        addCoverage(capabilities, "/options/masterDetailConfig", "FRONTEND_AND_BACKEND_STATIC", "TRANSACTION_SERVICE_COMPILED");
        addCoverage(capabilities, "/searchSchema", "FRONTEND_AND_BACKEND_STATIC", "QUERY_DTO_AND_XML_COMPILED");
        addCoverage(capabilities, "/columnsSchema", "FRONTEND_SHARED_RUNTIME", "COMPILED_RUNTIME");
        addCoverage(capabilities, "/editSchema", "FRONTEND_SHARED_RUNTIME", "COMPILED_RUNTIME");
        addCoverage(capabilities, "/apiConfig", "STATIC_BUSINESS_CONTROLLER", "BUSINESS_API_REWRITE");
        addCoverage(capabilities, "/dictConfig", "FRONTEND_AND_STATIC_ANNOTATIONS", "COMPILED");
        addCoverage(capabilities, "/desensitizeConfig", "STATIC_ENTITY_ANNOTATIONS", "COMPILED");
        addCoverage(capabilities, "/encryptConfig", "STATIC_API_ANNOTATIONS", "COMPILED");
        addCoverage(capabilities, "/transConfig", "FRONTEND_SHARED_RUNTIME", "PROTOCOL_PASSTHROUGH");
        addCoverage(capabilities, "/backend/businessExtension", "USER_OWNED_EXTENSION_CHAIN", "AVAILABLE");

        List<Map<String, Object>> requiresExtension = detectExtensionRequirements(frontendConfig);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("coverageVersion", COVERAGE_VERSION);
        report.put("runtimeContract", RUNTIME_CONTRACT);
        report.put("configKey", config.getConfigKey());
        report.put("layoutType", config.getLayoutType());
        report.put("mode", "STATIC_MYBATIS_PLUS");
        report.put("status", requiresExtension.isEmpty() ? "STATIC_COMPILED" : "STATIC_COMPILED_WITH_EXTENSIONS");
        report.put("futureCompatibility", Map.of(
                "frontendNestedFields", "AUTO_PASSTHROUGH",
                "backendNestedFields", "PRESERVED_REQUIRES_COMPILER_COVERAGE",
                "downloadEntryChangeRequired", false,
                "backendCompilerUpdateRequiredForNewSemantics", true,
                "requiresSharedFrontendRuntimeUpgrade", true
        ));
        report.put("capabilities", capabilities);
        report.put("requiresExtension", requiresExtension);
        report.put("unsupported", List.of());
        report.put("protocolRoots", new ArrayList<>(frontendConfig.keySet()));
        return report;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> detectExtensionRequirements(Map<String, Object> frontendConfig) {
        List<Map<String, Object>> result = new ArrayList<>();
        Object optionsValue = frontendConfig.get("options");
        if (optionsValue instanceof Map<?, ?> options) {
            Object codegenValue = options.get("codegen");
            boolean sourceCustomQuery = codegenValue instanceof Map<?, ?> codegen
                    && Boolean.TRUE.equals(codegen.get("sourceEnableCustomQuery"));
            if (Boolean.TRUE.equals(options.get("enableCustomQuery")) || sourceCustomQuery) {
                result.add(extensionRequirement(
                        "/options/enableCustomQuery",
                        "用户保存的任意组合查询需要自定义 Mapper XML 和业务端点"));
            }
        }
        Object modelValue = frontendConfig.get("modelSchema");
        if (modelValue instanceof Map<?, ?> model && model.get("fields") instanceof List<?> fields) {
            List<Map<String, Object>> fieldMaps = fields.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
            boolean formula = fieldMaps.stream()
                    .anyMatch(field -> field.get("formulaConfig") instanceof Map<?, ?> formulaConfig
                            && !formulaConfig.isEmpty());
            if (formula) {
                result.add(extensionRequirement(
                        "/modelSchema/fields/*/formulaConfig",
                        "服务端持久化公式需在 ServiceExtension 或 Manager 中实现"));
            }
            boolean unique = fieldMaps.stream().anyMatch(this::hasUniqueRule);
            if (unique) {
                result.add(extensionRequirement(
                        "/modelSchema/fields/*/advancedProps/unique",
                        "跨记录唯一校验需新增 Mapper XML 查询并在写入扩展中调用"));
            }
            boolean generation = fieldMaps.stream().anyMatch(this::hasGenerationRule);
            if (generation) {
                result.add(extensionRequirement(
                        "/modelSchema/fields/*/advancedProps/generation",
                        "自动编号需在写入扩展中接入目标项目的编码规则服务"));
            }
        }
        return result;
    }

    private boolean hasUniqueRule(Map<String, Object> field) {
        return nestedBoolean(field.get("basicProps"), "unique")
                || nestedBoolean(field.get("advancedProps"), "unique")
                || nestedBoolean(field.get("advancedProps"), "uniqueCheck");
    }

    private boolean hasGenerationRule(Map<String, Object> field) {
        return nestedNonEmptyMap(field.get("basicProps"), "generation")
                || nestedNonEmptyMap(field.get("advancedProps"), "generation");
    }

    private boolean nestedBoolean(Object value, String key) {
        if (!(value instanceof Map<?, ?> map)) {
            return false;
        }
        Object nested = map.get(key);
        return Boolean.TRUE.equals(nested) || "true".equalsIgnoreCase(String.valueOf(nested));
    }

    private boolean nestedNonEmptyMap(Object value, String key) {
        return value instanceof Map<?, ?> map
                && map.get(key) instanceof Map<?, ?> nested
                && !nested.isEmpty();
    }

    private Map<String, Object> extensionRequirement(String path, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("path", path);
        item.put("status", "REQUIRES_EXTENSION");
        item.put("reason", reason);
        return item;
    }

    private void addCoverage(List<Map<String, Object>> target, String path, String owner, String status) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("path", path);
        item.put("owner", owner);
        item.put("status", status);
        target.add(item);
    }

    private String sourceConfigKey(Map<String, Object> frontendConfig) {
        Object options = frontendConfig.get("options");
        if (!(options instanceof Map<?, ?> optionsMap)) {
            return null;
        }
        Object codegen = optionsMap.get("codegen");
        if (!(codegen instanceof Map<?, ?> codegenMap)) {
            return null;
        }
        Object value = codegenMap.get("sourceConfigKey");
        return value == null ? null : String.valueOf(value);
    }

    private void replaceJson(Map<String, Object> target,
                             String key,
                             String json,
                             JsonShape shape,
                             boolean required) {
        if (StringUtils.isBlank(json)) {
            if (required) {
                throw new BusinessException("低代码协议缺少 " + key);
            }
            target.put(key, shape == JsonShape.ARRAY ? List.of() : Map.of());
            return;
        }
        Object value;
        try {
            value = objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new BusinessException("低代码协议 " + key + " 不是合法 JSON", e);
        }
        if (shape == JsonShape.OBJECT && !(value instanceof Map<?, ?>)) {
            throw new BusinessException("低代码协议 " + key + " 必须是 JSON 对象");
        }
        if (shape == JsonShape.ARRAY && !(value instanceof List<?>)) {
            throw new BusinessException("低代码协议 " + key + " 必须是 JSON 数组");
        }
        target.put(key, value);
    }

    private String writeJson(Object value, String label) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(label + "生成失败", e);
        }
    }

    private enum JsonShape {
        OBJECT,
        ARRAY,
        ANY
    }

    public record ProtocolArtifacts(String frontendRuntimeConfig,
                                    String protocolSnapshot,
                                    String coverageReport) {
    }
}
