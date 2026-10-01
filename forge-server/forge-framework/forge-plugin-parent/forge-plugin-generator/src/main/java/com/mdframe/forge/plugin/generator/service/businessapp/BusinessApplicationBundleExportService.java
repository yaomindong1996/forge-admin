package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationBundlePageVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectDesignerVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 导出低代码应用设计态调试包（不含业务数据与密钥）。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationBundleExportService {

    public static final String FORMAT = "forge-application-bundle";
    public static final int SCHEMA_VERSION = 2;

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "token", "access_token", "authorization", "cookie", "password", "secret",
            "client_secret", "clientsecret", "webhook_secret", "webhooksecret", "api_key", "apikey",
            "ak", "sk", "key_hash", "keyhash", "private_key", "privatekey", "jdbcUrl", "jdbcurl",
            "username", "password_hash"
    );

    private final ObjectMapper objectMapper;
    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessObjectDesignerService designerService;
    private final BusinessApplicationBundleCompanionCollector companionCollector;

    public List<BusinessApplicationBundlePageVO> listExportPages(Long applicationId) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        Map<String, Object> options = readJsonMap(application.getOptions());
        Map<String, Object> builder = map(options.get("inAppBuilder"));
        Map<String, Object> pages = map(builder.get("pages"));
        List<BusinessApplicationBundlePageVO> result = new ArrayList<>();
        for (Map<String, Object> node : BusinessApplicationBundlePageSupport.listPageNodes(builder)) {
            BusinessApplicationBundlePageVO item = new BusinessApplicationBundlePageVO();
            String pageId = text(node.get("id"));
            item.setPageId(pageId);
            item.setTitle(StringUtils.defaultIfBlank(text(node.get("title")), "未命名页面"));
            item.setPageType(StringUtils.defaultIfBlank(text(node.get("pageType")), "content"));
            Map<String, Object> objectRef = map(node.get("objectRef"));
            item.setObjectCode(text(objectRef.get("objectCode")));
            item.setObjectName(text(objectRef.get("objectName")));
            Set<String> codes = new LinkedHashSet<>();
            BusinessApplicationBundlePageSupport.collectObjectCodesDeep(node.get("objectRef"), codes);
            BusinessApplicationBundlePageSupport.collectObjectCodesDeep(pages.get(pageId), codes);
            item.setDataBound("object".equalsIgnoreCase(item.getPageType()) || !codes.isEmpty());
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> exportBundle(Long applicationId) {
        return exportBundle(applicationId, null);
    }

    public Map<String, Object> exportBundle(Long applicationId, Collection<String> pageIds) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        List<BusinessApplicationObjectVO> associations = applicationObjectService.list(applicationId);

        List<Map<String, Object>> allObjectSnapshots = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        Map<Long, String> objectIdToCode = new LinkedHashMap<>();
        for (BusinessApplicationObjectVO association : associations) {
            if (association == null || association.getObjectId() == null || !seen.add(association.getObjectId())) {
                continue;
            }
            Map<String, Object> snapshot = objectSnapshot(association);
            allObjectSnapshots.add(snapshot);
            String code = text(snapshot.get("objectCode"));
            if (code != null) {
                objectIdToCode.put(association.getObjectId(), code);
            }
        }

        Map<String, Object> applicationSnapshot = applicationSnapshot(application);
        Set<String> selectedPageIds = normalizePageIds(pageIds);
        boolean pageMode = !selectedPageIds.isEmpty();
        Set<String> includedObjectCodes;
        if (pageMode) {
            Map<String, Object> options = map(applicationSnapshot.get("options"));
            Map<String, Object> builder = map(options.get("inAppBuilder"));
            validateSelectedPages(builder, selectedPageIds);
            Set<String> seedCodes = BusinessApplicationBundlePageSupport
                    .collectObjectCodesFromPages(builder, selectedPageIds);
            if (seedCodes.isEmpty()) {
                seedCodes.addAll(fallbackObjectCodes(builder, selectedPageIds, allObjectSnapshots));
            }
            Map<String, Set<String>> adjacency = BusinessApplicationBundlePageSupport
                    .buildRelationAdjacency(allObjectSnapshots);
            includedObjectCodes = BusinessApplicationBundlePageSupport.expandObjectClosure(seedCodes, adjacency);
            Map<String, Object> filteredBuilder = BusinessApplicationBundlePageSupport
                    .filterBuilder(builder, selectedPageIds);
            options.put("inAppBuilder", filteredBuilder);
            applicationSnapshot.put("options", options);
            applicationSnapshot.put("portalConfig", BusinessApplicationBundlePageSupport
                    .filterPortalConfig(map(applicationSnapshot.get("portalConfig")), selectedPageIds));
        }
        else {
            includedObjectCodes = allObjectSnapshots.stream()
                    .map(item -> text(item.get("objectCode")))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            selectedPageIds = BusinessApplicationBundlePageSupport
                    .listPageNodes(map(map(applicationSnapshot.get("options")).get("inAppBuilder")))
                    .stream()
                    .map(node -> text(node.get("id")))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        List<Map<String, Object>> objects = new ArrayList<>();
        for (Map<String, Object> snapshot : allObjectSnapshots) {
            String code = text(snapshot.get("objectCode"));
            if (code == null || !includedObjectCodes.contains(code)) {
                continue;
            }
            Map<String, Object> copy = new LinkedHashMap<>(snapshot);
            copy.put("relations", BusinessApplicationBundlePageSupport.filterRelations(
                    maps(snapshot.get("relations")), includedObjectCodes));
            objects.add(sanitize(copy));
        }

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("format", FORMAT);
        bundle.put("schemaVersion", SCHEMA_VERSION);
        bundle.put("exportedAt", Instant.now().toString());
        bundle.put("source", Map.of(
                "applicationCode", StringUtils.defaultString(application.getApplicationCode()),
                "applicationName", StringUtils.defaultString(application.getApplicationName()),
                "suiteCode", StringUtils.defaultString(application.getSuiteCode())
        ));
        Map<String, Object> selection = new LinkedHashMap<>();
        selection.put("mode", pageMode ? "PAGES" : "FULL");
        selection.put("pageIds", new ArrayList<>(selectedPageIds));
        selection.put("objectCodes", new ArrayList<>(includedObjectCodes));
        bundle.put("selection", selection);
        bundle.put("application", sanitize(applicationSnapshot));
        bundle.put("objects", objects);
        Map<String, Object> companions = companionCollector.collect(
                applicationId, includedObjectCodes, objectIdToCode);
        bundle.put("entries", companions.get("entries"));
        bundle.put("bindings", companions.get("bindings"));
        bundle.put("processes", companions.get("processes"));
        bundle.put("extensions", companions.get("extensions"));
        bundle.put("triggers", companions.get("triggers"));
        bundle.put("printing", sanitize(companions.get("printing")));
        return bundle;
    }

    public byte[] exportBundleBytes(Long applicationId) {
        return exportBundleBytes(applicationId, null);
    }

    public byte[] exportBundleBytes(Long applicationId, Collection<String> pageIds) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(exportBundle(applicationId, pageIds));
        }
        catch (Exception e) {
            throw new BusinessException("导出应用调试包失败: " + e.getMessage(), e);
        }
    }

    public String resolveFileName(Long applicationId) {
        AiBusinessApplication application = applicationService.requireEntity(applicationId);
        String code = StringUtils.defaultIfBlank(application.getApplicationCode(), "application")
                .replaceAll("[^A-Za-z0-9_-]", "_");
        return code + ".forge-app.json";
    }

    private Set<String> fallbackObjectCodes(Map<String, Object> builder,
                                            Set<String> pageIds,
                                            List<Map<String, Object>> objectSnapshots) {
        boolean needsData = false;
        Map<String, Object> pages = map(builder.get("pages"));
        for (Map<String, Object> node : BusinessApplicationBundlePageSupport.listPageNodes(builder)) {
            String pageId = text(node.get("id"));
            if (!pageIds.contains(pageId)) {
                continue;
            }
            if ("object".equalsIgnoreCase(text(node.get("pageType")))) {
                needsData = true;
                break;
            }
            if (containsCrudBlock(pages.get(pageId))) {
                needsData = true;
                break;
            }
        }
        if (!needsData || objectSnapshots.isEmpty()) {
            return Set.of();
        }
        List<String> primary = objectSnapshots.stream()
                .filter(item -> BusinessApplicationObjectRole.PRIMARY
                        .equalsIgnoreCase(text(item.get("role"))))
                .map(item -> text(item.get("objectCode")))
                .filter(StringUtils::isNotBlank)
                .toList();
        if (primary.size() == 1) {
            return Set.of(primary.get(0));
        }
        if (objectSnapshots.size() == 1) {
            String code = text(objectSnapshots.get(0).get("objectCode"));
            return code == null ? Set.of() : Set.of(code);
        }
        return Set.of();
    }

    private boolean containsCrudBlock(Object node) {
        if (node instanceof Map<?, ?> map) {
            if ("AiCrudPage".equalsIgnoreCase(text(map.get("blockType")))) {
                return true;
            }
            for (Object value : map.values()) {
                if (containsCrudBlock(value)) {
                    return true;
                }
            }
            return false;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                if (containsCrudBlock(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void validateSelectedPages(Map<String, Object> builder, Set<String> pageIds) {
        Set<String> available = BusinessApplicationBundlePageSupport.listPageNodes(builder).stream()
                .map(node -> text(node.get("id")))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missing = pageIds.stream().filter(id -> !available.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException("导出页面不存在: " + String.join(", ", missing));
        }
    }

    private Set<String> normalizePageIds(Collection<String> pageIds) {
        if (pageIds == null || pageIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String pageId : pageIds) {
            if (StringUtils.isNotBlank(pageId)) {
                result.add(pageId.trim());
            }
        }
        return result;
    }

    private Map<String, Object> applicationSnapshot(AiBusinessApplication application) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("applicationName", application.getApplicationName());
        snapshot.put("suiteCode", application.getSuiteCode());
        snapshot.put("icon", application.getIcon());
        snapshot.put("description", application.getDescription());
        snapshot.put("status", application.getStatus());
        snapshot.put("options", readJsonMap(application.getOptions()));
        snapshot.put("portalConfig", readJsonMap(application.getPortalConfig()));
        snapshot.put("aiAssistantConfig", readJsonMap(application.getAiAssistantConfig()));
        return snapshot;
    }

    private Map<String, Object> objectSnapshot(BusinessApplicationObjectVO association) {
        BusinessObjectDesignerVO designer = designerService.getDesigner(association.getObjectId());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("objectCode", designer.getObjectCode());
        snapshot.put("objectName", designer.getObjectName());
        snapshot.put("objectType", designer.getObjectType());
        snapshot.put("displayField", designer.getDisplayField());
        snapshot.put("description", designer.getDescription());
        snapshot.put("icon", designer.getIcon());
        snapshot.put("modelCode", designer.getModelSchema() == null || designer.getModelSchema().getObject() == null
                ? designer.getObjectCode()
                : designer.getModelSchema().getObject().getCode());
        snapshot.put("tableName", designer.getModelSchema() == null ? null : designer.getModelSchema().getTableName());
        snapshot.put("role", StringUtils.defaultIfBlank(association.getObjectRole(),
                BusinessApplicationObjectRole.SHARED));
        snapshot.put("sortOrder", association.getSortOrder());
        snapshot.put("fields", toFieldMaps(designer.getFields()));
        snapshot.put("formDesignerSchema", designer.getFormDesignerSchema());
        snapshot.put("pageSchema", designer.getPageSchema());
        snapshot.put("viewSchema", designer.getViewSchema());
        snapshot.put("linkageSchema", designer.getLinkageSchema());
        snapshot.put("designerOptions", designer.getDesignerOptions());
        snapshot.put("modelSchema", stripRuntimeDatasourceForPortability(designer.getModelSchema()));
        snapshot.put("relations", toRelationMaps(designer.getRelations()));
        snapshot.put("documentConfig", companionCollector.documentConfigSnapshot(association.getObjectId()));
        return snapshot;
    }

    private List<Map<String, Object>> toFieldMaps(List<?> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        return fields.stream()
                .map(field -> objectMapper.convertValue(field, new TypeReference<Map<String, Object>>() { }))
                .toList();
    }

    private List<Map<String, Object>> toRelationMaps(List<BusinessObjectRelationVO> relations) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (BusinessObjectRelationVO relation : relations) {
            if (relation == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sourceObjectCode", relation.getSourceObjectCode());
            item.put("targetObjectCode", relation.getTargetObjectCode());
            item.put("relationType", relation.getRelationType());
            item.put("relationName", relation.getRelationName());
            item.put("sourceFieldCode", relation.getSourceFieldCode());
            item.put("targetFieldCode", relation.getTargetFieldCode());
            item.put("relationConfig", readJsonMap(relation.getRelationConfig()));
            item.put("description", relation.getDescription());
            item.put("status", relation.getStatus());
            item.put("sortOrder", relation.getSortOrder());
            result.add(item);
        }
        return result;
    }

    private LowcodeModelSchema stripRuntimeDatasourceForPortability(LowcodeModelSchema modelSchema) {
        if (modelSchema == null) {
            return null;
        }
        LowcodeModelSchema copy = objectMapper.convertValue(modelSchema, LowcodeModelSchema.class);
        // 跨环境导入时改用目标环境默认数据源，避免带上源环境 datasourceId
        copy.setRuntimeDatasource(null);
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitize(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        Object converted = objectMapper.convertValue(value, Object.class);
        return (Map<String, Object>) sanitizeNode(converted);
    }

    @SuppressWarnings("unchecked")
    private Object sanitizeNode(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> cleaned = new LinkedHashMap<>();
            map.forEach((rawKey, rawValue) -> {
                String key = String.valueOf(rawKey);
                if (isSensitiveKey(key)) {
                    return;
                }
                cleaned.put(key, sanitizeNode(rawValue));
            });
            return cleaned;
        }
        if (value instanceof List<?> list) {
            List<Object> cleaned = new ArrayList<>(list.size());
            for (Object item : list) {
                cleaned.add(sanitizeNode(item));
            }
            return cleaned;
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        String normalized = StringUtils.defaultString(key).trim().toLowerCase(Locale.ROOT)
                .replace("-", "_");
        if (SENSITIVE_KEYS.contains(normalized)) {
            return true;
        }
        return normalized.endsWith("_password")
                || normalized.endsWith("_secret")
                || normalized.endsWith("_token")
                || normalized.endsWith("_apikey");
    }

    private Map<String, Object> readJsonMap(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        }
        catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return new LinkedHashMap<>((Map<String, Object>) raw);
        }
        return new LinkedHashMap<>();
    }

    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?>) {
                result.add(map(item));
            }
        }
        return result;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }
}
