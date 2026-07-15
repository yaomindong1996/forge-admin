package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAssetSelectionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 生成白名单化应用发布快照并在提交前补齐实际发布版本引用。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationSnapshotService {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "token", "access_token", "authorization", "cookie", "password", "secret",
            "client_secret", "clientsecret", "webhook_secret", "webhooksecret", "api_key", "apikey",
            "ak", "sk", "key_hash", "keyhash", "private_key", "privatekey"
    );

    private final ObjectMapper objectMapper;
    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessAppService businessAppService;
    private final BusinessBindingMapper bindingMapper;
    private final BusinessExtensionMapper extensionMapper;
    private final BusinessExtensionVersionMapper extensionVersionMapper;
    private final BusinessPermissionService permissionService;

    public SnapshotBundle prepare(Long applicationId, BusinessApplicationAssetSelectionVO selection) {
        return prepare(applicationId, selection, null, null, null, null);
    }

    SnapshotBundle prepare(
            Long applicationId,
            BusinessApplicationVO application,
            BusinessApplicationAssetSelectionService.ResolvedSelection resolved,
            List<BusinessPermissionSummaryVO> permissionSummaries,
            List<AiBusinessBinding> bindings) {
        return prepare(applicationId, resolved.selection(), application, resolved, permissionSummaries, bindings);
    }

    private SnapshotBundle prepare(
            Long applicationId,
            BusinessApplicationAssetSelectionVO selection,
            BusinessApplicationVO application,
            BusinessApplicationAssetSelectionService.ResolvedSelection resolved,
            List<BusinessPermissionSummaryVO> permissionSummaries,
            List<AiBusinessBinding> bindings) {
        AiBusinessApplication applicationEntity = application == null
                ? applicationService.requireEntity(applicationId) : null;
        List<BusinessApplicationObjectVO> availableObjects = resolved == null
                ? applicationObjectService.list(applicationId) : resolved.objects();
        List<AiBusinessApp> availableEntries = resolved == null
                ? businessAppService.listByApplicationId(applicationId) : resolved.entries();
        List<AiBusinessExtension> availableExtensions = resolved == null
                ? extensionMapper.selectByApplicationId(resolveTenantId(), applicationId) : resolved.extensions();
        Map<Long, BusinessApplicationObjectVO> objects = availableObjects.stream()
                .collect(Collectors.toMap(BusinessApplicationObjectVO::getObjectId, Function.identity()));
        Map<Long, AiBusinessApp> entries = availableEntries.stream()
                .collect(Collectors.toMap(AiBusinessApp::getId, Function.identity()));
        Map<Long, AiBusinessExtension> extensions = availableExtensions.stream()
                .collect(Collectors.toMap(AiBusinessExtension::getId, Function.identity()));
        List<BusinessApplicationObjectVO> selectedObjects = selection.getObjectIds().stream()
                .map(objects::get).filter(java.util.Objects::nonNull).toList();
        List<AiBusinessExtension> selectedExtensions = selection.getExtensionIds().stream()
                .map(extensions::get).filter(java.util.Objects::nonNull).toList();
        Map<Long, Integer> releaseVersions = selectedExtensions.stream()
                .filter(extension -> releaseVersion(extension) != null)
                .collect(Collectors.toMap(AiBusinessExtension::getId, this::releaseVersion,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, AiBusinessExtensionVersion> extensionVersions = releaseVersions.isEmpty()
                ? Map.of()
                : extensionVersionMapper.selectReleaseVersions(resolveTenantId(), releaseVersions).stream()
                .collect(Collectors.toMap(AiBusinessExtensionVersion::getExtensionId, Function.identity()));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("schemaVersion", 1);
        snapshot.put("application", application == null
                ? applicationSnapshot(applicationEntity) : applicationSnapshot(application));
        snapshot.put("selection", objectMapper.convertValue(selection, new TypeReference<Map<String, Object>>() { }));
        snapshot.put("objects", selectedObjects.stream().map(this::objectSnapshot).toList());
        snapshot.put("entries", selection.getEntryIds().stream()
                .map(entries::get).filter(java.util.Objects::nonNull).map(this::entrySnapshot).toList());
        List<AiBusinessBinding> availableBindings = bindings == null
                ? bindingMapper.selectByApplication(resolveTenantId(), applicationId) : bindings;
        snapshot.put("bindings", availableBindings.stream()
                .map(this::bindingSnapshot).toList());
        snapshot.put("extensions", selectedExtensions.stream()
                .map(extension -> extensionSnapshot(extension, extensionVersions.get(extension.getId()))).toList());
        List<BusinessPermissionSummaryVO> selectedPermissionSummaries = permissionSummaries == null
                ? permissionService.documentActionSummaries(selectedObjects) : permissionSummaries;
        snapshot.put("permissions", selectedPermissionSummaries.stream()
                .map(this::permissionSnapshot).toList());
        snapshot.put("publishedObjectVersions", new ArrayList<>());
        return bundle(snapshot);
    }

    public SnapshotBundle finalizePublished(String candidateJson,
                                            Map<Long, Long> objectVersionIds,
                                            BusinessApplicationAssetSelectionVO selection,
                                            Integer versionNo,
                                            String operationType) {
        Map<String, Object> snapshot = parse(candidateJson);
        Map<String, Object> application = map(snapshot.get("application"));
        application.put("designStatus", "PUBLISHED");
        application.put("publishedVersion", versionNo);
        snapshot.put("application", application);
        snapshot.put("operationType", operationType);

        List<Map<String, Object>> publishedObjects = new ArrayList<>();
        for (Map.Entry<Long, Long> item : objectVersionIds.entrySet()) {
            Map<String, Object> version = new LinkedHashMap<>();
            version.put("objectId", String.valueOf(item.getKey()));
            version.put("designVersionId", String.valueOf(item.getValue()));
            publishedObjects.add(version);
        }
        snapshot.put("publishedObjectVersions", publishedObjects);
        Set<Long> selectedExtensions = Set.copyOf(selection.getExtensionIds());
        List<Map<String, Object>> extensionSnapshots = listOfMap(snapshot.get("extensions"));
        for (Map<String, Object> extension : extensionSnapshots) {
            Long extensionId = longValue(extension.get("id"));
            if (selectedExtensions.contains(extensionId) && "TESTED".equals(extension.get("status"))) {
                extension.put("status", "ENABLED");
                extension.put("enabledVersion", extension.get("releaseVersion"));
            }
        }
        snapshot.put("extensions", extensionSnapshots);
        return bundle(snapshot);
    }

    public Map<String, Object> parse(String json) {
        if (StringUtils.isBlank(json)) {
            throw new BusinessException("应用发布快照为空");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (Exception e) {
            throw new BusinessException("应用发布快照格式不正确");
        }
    }

    public SnapshotBundle bundle(Map<String, Object> snapshot) {
        Object sanitized = sanitize(snapshot);
        try {
            String json = objectMapper.writeValueAsString(sanitized);
            return new SnapshotBundle(json, sha256(json), parse(json));
        } catch (Exception e) {
            throw new BusinessException("应用发布快照生成失败");
        }
    }

    private Map<String, Object> applicationSnapshot(AiBusinessApplication application) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(application.getId()));
        item.put("applicationCode", application.getApplicationCode());
        item.put("applicationName", application.getApplicationName());
        item.put("suiteCode", application.getSuiteCode());
        item.put("icon", application.getIcon());
        item.put("description", application.getDescription());
        item.put("status", application.getStatus());
        item.put("designStatus", application.getDesignStatus());
        item.put("options", parseOptionalJson(application.getOptions()));
        return item;
    }

    private Map<String, Object> applicationSnapshot(BusinessApplicationVO application) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(application.getId()));
        item.put("applicationCode", application.getApplicationCode());
        item.put("applicationName", application.getApplicationName());
        item.put("suiteCode", application.getSuiteCode());
        item.put("icon", application.getIcon());
        item.put("description", application.getDescription());
        item.put("status", application.getStatus());
        item.put("designStatus", application.getDesignStatus());
        item.put("options", parseOptionalJson(application.getOptions()));
        return item;
    }

    private Map<String, Object> objectSnapshot(BusinessApplicationObjectVO object) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("objectId", String.valueOf(object.getObjectId()));
        item.put("objectCode", object.getObjectCode());
        item.put("objectName", object.getObjectName());
        item.put("objectRole", object.getObjectRole());
        item.put("designStatus", object.getDesignStatus());
        item.put("designVersion", object.getDesignVersion());
        item.put("configKey", object.getConfigKey());
        item.put("datasourceCode", object.getDatasourceCode());
        item.put("tableName", object.getTableName());
        item.put("syncStatus", object.getSyncStatus());
        item.put("sharedApplicationCount", object.getSharedApplicationCount());
        return item;
    }

    private Map<String, Object> entrySnapshot(AiBusinessApp entry) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(entry.getId()));
        item.put("appCode", entry.getAppCode());
        item.put("appName", entry.getAppName());
        item.put("appType", entry.getAppType());
        item.put("suiteCode", entry.getSuiteCode());
        item.put("objectCode", entry.getObjectCode());
        item.put("entryMode", entry.getEntryMode());
        item.put("entryUrl", entry.getEntryUrl());
        item.put("configKey", entry.getConfigKey());
        item.put("icon", entry.getIcon());
        item.put("description", entry.getDescription());
        item.put("status", entry.getStatus());
        item.put("sortOrder", entry.getSortOrder());
        item.put("options", parseOptionalJson(entry.getOptions()));
        return item;
    }

    private Map<String, Object> bindingSnapshot(AiBusinessBinding binding) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(binding.getId()));
        item.put("bindingType", binding.getBindingType());
        item.put("bindingKey", binding.getBindingKey());
        item.put("bindingName", binding.getBindingName());
        item.put("bindingConfig", parseOptionalJson(binding.getBindingConfig()));
        item.put("status", binding.getStatus());
        item.put("sortOrder", binding.getSortOrder());
        return item;
    }

    private Map<String, Object> extensionSnapshot(AiBusinessExtension extension,
                                                  AiBusinessExtensionVersion version) {
        Integer releaseVersion = releaseVersion(extension);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", String.valueOf(extension.getId()));
        item.put("extensionCode", extension.getExtensionCode());
        item.put("extensionName", extension.getExtensionName());
        item.put("extensionType", extension.getExtensionType());
        item.put("hookCode", extension.getHookCode());
        item.put("objectId", stringValue(extension.getObjectId()));
        item.put("entryId", stringValue(extension.getEntryId()));
        item.put("status", extension.getStatus());
        item.put("draftVersion", extension.getDraftVersion());
        item.put("enabledVersion", extension.getEnabledVersion());
        item.put("releaseVersion", releaseVersion);
        item.put("contentHash", version == null ? null : version.getContentHash());
        return item;
    }

    private Integer releaseVersion(AiBusinessExtension extension) {
        return "TESTED".equals(extension.getStatus())
                ? extension.getDraftVersion() : extension.getEnabledVersion();
    }

    private Map<String, Object> permissionSnapshot(BusinessPermissionSummaryVO summary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("objectId", stringValue(summary.getObjectId()));
        item.put("objectCode", summary.getObjectCode());
        item.put("allRequiredConfigured", summary.getAllRequiredConfigured());
        item.put("actions", summary.getActionPermissions().stream().map(action -> Map.of(
                "actionCode", action.getActionCode(),
                "required", Boolean.TRUE.equals(action.getRequired()),
                "configured", Boolean.TRUE.equals(action.getConfigured())
        )).toList());
        return item;
    }

    private Object parseOptionalJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Object sanitize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                String name = String.valueOf(key);
                String normalized = name.replace('-', '_').toLowerCase(Locale.ROOT);
                if (!SENSITIVE_KEYS.contains(normalized)) {
                    result.put(name, sanitize(item));
                }
            });
            return result;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::sanitize).toList();
        }
        if (value instanceof JsonNode node) {
            return sanitize(objectMapper.convertValue(node, Object.class));
        }
        return value;
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        return list.stream().filter(Map.class::isInstance)
                .map(this::map).collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException("应用发布快照摘要生成失败");
        }
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }

    public record SnapshotBundle(String json, String hash, Map<String, Object> snapshot) {
    }
}
