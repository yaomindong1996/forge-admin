package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/** 从不可变应用版本构建当前用户的正式运行配置。 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationRuntimeService {

    private static final Pattern LEGACY_PAGE_ID_UNSAFE = Pattern.compile("[^a-z0-9_]+");
    /** render overlay / print 同请求或短时连点会重复 runtimeById；按发布版本短缓存。 */
    private static final long RUNTIME_CACHE_TTL_MS = 30_000L;
    private static final int RUNTIME_CACHE_MAX = 64;

    private final BusinessApplicationService applicationService;
    private final BusinessApplicationVersionService versionService;
    private final BusinessApplicationSnapshotService snapshotService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CachedRuntime> runtimeCache = new ConcurrentHashMap<>();
    /** applicationId → 发布版本缓存，避免同请求 overlay/print 各打一次 detail。 */
    private final ConcurrentHashMap<String, CachedRuntime> runtimeByAppIdCache = new ConcurrentHashMap<>();

    public BusinessApplicationRuntimeVO runtimeByCode(String applicationCode) {
        BusinessApplicationVO current = applicationService.detailByCode(applicationCode);
        return runtimeCached(current);
    }

    public BusinessApplicationRuntimeVO runtimeByCodeOrSlug(String identifier) {
        return runtimeCached(applicationService.detailByPublishedCodeOrSlug(identifier));
    }

    /**
     * 使用正式门户的同一发布快照、应用可见范围和页面权限链路构建工作台入口。
     * 分发开关读应用表实时 portalConfig（保存即生效），页面与 slug 仍取发布快照。
     */
    public List<BusinessApplicationVO> workbenchApplications() {
        List<BusinessApplicationVO> result = new ArrayList<>();
        for (BusinessApplicationVO candidate : applicationService.workbenchDistributionCandidates()) {
            try {
                if (!applicationService.isCurrentUserDistributedToWorkbench(candidate.getPortalConfig())) {
                    continue;
                }
                BusinessApplicationRuntimeVO runtime = runtimeCached(candidate);
                if (hasReachableHomePage(runtime)) {
                    result.add(runtime.getApplication());
                }
            } catch (BusinessException ignored) {
                // 单个应用无权访问、快照失效或没有可达页面时不影响其它工作台入口。
            }
        }
        return List.copyOf(result);
    }

    /**
     * 按应用主键读取正式运行快照，供带应用入口上下文的低代码运行配置叠加使用。
     */
    public BusinessApplicationRuntimeVO runtimeById(Long applicationId) {
        if (applicationId == null) {
            return runtime(null);
        }
        long now = System.currentTimeMillis();
        String appKey = resolveTenantId() + ":id:" + applicationId + ":" + resolveCacheUserKey();
        CachedRuntime softHit = runtimeByAppIdCache.get(appKey);
        if (softHit != null && softHit.expiresAtMs > now) {
            return softHit.value;
        }
        return runtimeCached(applicationService.detail(applicationId));
    }

    /** 发布后主动失效，避免短 TTL 内仍读到旧快照。 */
    public void invalidateRuntimeCache(Long applicationId) {
        if (applicationId == null) {
            return;
        }
        Long tenantId = resolveTenantId();
        String prefix = tenantId + ":" + applicationId + ":";
        runtimeCache.keySet().removeIf(key -> key.startsWith(prefix));
        String appPrefix = tenantId + ":id:" + applicationId + ":";
        runtimeByAppIdCache.keySet().removeIf(key -> key.startsWith(appPrefix));
    }

    private BusinessApplicationRuntimeVO runtimeCached(BusinessApplicationVO current) {
        if (current == null || current.getId() == null) {
            return runtime(current);
        }
        Integer versionNo = current.getLastPublishVersion();
        if (versionNo == null) {
            return runtime(current);
        }
        // 运行结果已按当前用户权限过滤页面树，缓存键必须带 userId，否则会串权或「改完授权仍看不见菜单」
        String userKey = resolveCacheUserKey();
        String cacheKey = resolveTenantId() + ":" + current.getId() + ":" + versionNo + ":" + userKey;
        long now = System.currentTimeMillis();
        CachedRuntime hit = runtimeCache.get(cacheKey);
        if (hit != null && hit.expiresAtMs > now) {
            runtimeByAppIdCache.put(resolveTenantId() + ":id:" + current.getId() + ":" + userKey, hit);
            return hit.value;
        }
        BusinessApplicationRuntimeVO value = runtime(current);
        CachedRuntime cached = new CachedRuntime(value, now + RUNTIME_CACHE_TTL_MS);
        runtimeCache.put(cacheKey, cached);
        runtimeByAppIdCache.put(resolveTenantId() + ":id:" + current.getId() + ":" + userKey, cached);
        trimRuntimeCache(now);
        return value;
    }

    private void trimRuntimeCache(long now) {
        if (runtimeCache.size() <= RUNTIME_CACHE_MAX) {
            runtimeCache.entrySet().removeIf(entry -> entry.getValue().expiresAtMs <= now);
            return;
        }
        runtimeCache.entrySet().removeIf(entry -> entry.getValue().expiresAtMs <= now);
        if (runtimeCache.size() <= RUNTIME_CACHE_MAX) {
            return;
        }
        runtimeCache.keySet().stream()
                .limit(Math.max(1, runtimeCache.size() - RUNTIME_CACHE_MAX))
                .toList()
                .forEach(runtimeCache::remove);
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception ignored) {
            return 1L;
        }
    }

    private String resolveCacheUserKey() {
        try {
            Long userId = SessionHelper.getUserId();
            return userId == null ? "anon" : String.valueOf(userId);
        } catch (Exception ignored) {
            return "anon";
        }
    }

    private record CachedRuntime(BusinessApplicationRuntimeVO value, long expiresAtMs) {
    }

    private BusinessApplicationRuntimeVO runtime(BusinessApplicationVO current) {
        if (!EnableStatus.ENABLED.matches(current.getStatus())) {
            throw new BusinessException("应用已停用，暂时无法访问");
        }
        if (current.getLastPublishVersion() == null) {
            throw new BusinessException("应用尚未发布，请先完成发布后再访问正式运行页");
        }
        AiBusinessApplicationVersion version = versionService.requireVersion(
                current.getId(), current.getLastPublishVersion());
        Map<String, Object> snapshot = snapshotService.parse(version.getSnapshotJson());
        Map<String, Object> applicationSnapshot = map(snapshot.get("application"));
        if (!applicationService.canCurrentUserAccessPortal(JSON.toJSONString(
                applicationSnapshot.get("portalConfig")))) {
            throw new BusinessException("暂无访问该应用门户的权限");
        }
        Map<String, Object> options = map(applicationSnapshot.get("options"));
        List<Map<String, Object>> objectSnapshots = maps(snapshot.get("objects"));
        Map<String, Object> restoredBuilder = restoreLegacyPrimaryObjectPage(
                map(options.get("inAppBuilder")), options, applicationSnapshot, objectSnapshots);
        options.put("inAppBuilder", filterBuilder(
                restoredBuilder, current.getApplicationCode(),
                resolvePermissionCodes(), applicationService.currentUserIsApplicationAdministrator(
                        JSON.toJSONString(applicationSnapshot.get("portalConfig")))));
        applicationSnapshot.put("options", options);

        BusinessApplicationRuntimeVO runtime = new BusinessApplicationRuntimeVO();
        runtime.setVersionNo(version.getVersionNo());
        runtime.setApplication(toApplication(applicationSnapshot, current, version.getVersionNo()));
        runtime.setObjects(objectSnapshots.stream().map(this::toObject).toList());
        runtime.setEntries(maps(snapshot.get("entries")).stream().map(this::toEntry).toList());
        runtime.setExtensions(maps(snapshot.get("extensions")).stream()
                .filter(this::isRuntimeExtension)
                .map(this::runtimeExtension)
                .toList());
        return runtime;
    }

    /**
     * 改版前的对象型应用直接由主对象进入 CRUD 运行页，没有独立页面树。
     * 当不可变发布快照仍是这种结构时，按 primaryObjectCode 恢复一个新版对象页；
     * 一旦新版草稿保存迁移标记，后续用户主动清空页面不会再被自动补回。
     */
    private Map<String, Object> restoreLegacyPrimaryObjectPage(
            Map<String, Object> builder,
            Map<String, Object> options,
            Map<String, Object> application,
            List<Map<String, Object>> objects) {
        if (Boolean.TRUE.equals(builder.get("legacyObjectPageMigrated"))
                || !maps(builder.get("nodes")).isEmpty()
                || !map(builder.get("pages")).isEmpty()) {
            return builder;
        }
        String primaryObjectCode = StringUtils.trimToNull(string(options.get("primaryObjectCode")));
        if (primaryObjectCode == null) {
            return builder;
        }
        Map<String, Object> primaryObject = objects.stream()
                .filter(object -> primaryObjectCode.equals(string(object.get("objectCode"))))
                .filter(object -> "PRIMARY".equalsIgnoreCase(StringUtils.defaultIfBlank(
                        string(object.get("objectRole")), "PRIMARY")))
                .findFirst().orElse(null);
        String configKey = primaryObject == null
                ? null : StringUtils.trimToNull(string(primaryObject.get("configKey")));
        if (primaryObject == null || configKey == null) {
            return builder;
        }

        String objectCode = StringUtils.defaultIfBlank(
                string(primaryObject.get("objectCode")), primaryObjectCode);
        String objectName = StringUtils.defaultIfBlank(
                string(primaryObject.get("objectName")),
                StringUtils.defaultIfBlank(string(application.get("applicationName")), objectCode));
        String pageId = "page_" + legacyPageToken(objectCode);
        Map<String, Object> objectOptions = primaryObject.get("options") instanceof String text
                ? readJsonObject(text) : map(primaryObject.get("options"));

        Map<String, Object> objectRef = new LinkedHashMap<>();
        objectRef.put("objectId", primaryObject.get("objectId"));
        objectRef.put("objectCode", objectCode);
        objectRef.put("objectName", objectName);
        objectRef.put("configKey", configKey);
        objectRef.put("pageKey", StringUtils.defaultIfBlank(
                string(objectOptions.get("pageKey")), "list"));
        objectRef.put("pageMode", "crud");
        objectRef.put("hasBusinessData", true);
        objectRef.put("valid", true);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", pageId);
        node.put("type", "page");
        node.put("title", objectName);
        node.put("icon", StringUtils.defaultString(string(application.get("icon"))));
        node.put("parentId", null);
        node.put("sort", 0);
        node.put("pageType", "object");
        node.put("pageTemplate", "master-detail-crud".equalsIgnoreCase(
                string(primaryObject.get("layoutType"))) ? "master-detail" : "crud");
        node.put("objectRef", objectRef);
        node.put("mountTarget", "BOTH");
        node.put("systemMenuVisible", false);
        node.put("navigationVisible", true);
        node.put("access", Map.of("mode", "inherit", "roleIds", List.of()));
        node.put("legacyObjectPage", true);

        Map<String, Object> pageLayout = new LinkedHashMap<>();
        pageLayout.put("items", List.of());
        pageLayout.put("pageTitleComponentInitialized", true);
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("title", objectName);
        page.put("description", "");
        page.put("layout", pageLayout);

        Map<String, Object> restored = new LinkedHashMap<>(builder);
        restored.put("schemaVersion", 2);
        restored.put("legacyObjectPageMigrated", true);
        restored.put("homePageId", pageId);
        restored.put("nodes", List.of(node));
        restored.put("pages", Map.of(pageId, page));
        restored.putIfAbsent("formAssets", List.of());
        return restored;
    }

    private String legacyPageToken(String objectCode) {
        String normalized = LEGACY_PAGE_ID_UNSAFE.matcher(
                StringUtils.lowerCase(StringUtils.trimToEmpty(objectCode))).replaceAll("_");
        normalized = StringUtils.strip(normalized, "_");
        return StringUtils.defaultIfBlank(normalized, "legacy_object");
    }

    private boolean isRuntimeExtension(Map<String, Object> extension) {
        return "ENABLED".equalsIgnoreCase(string(extension.get("status")))
                && integer(extension.get("enabledVersion")) > 0;
    }

    private Map<String, Object> runtimeExtension(Map<String, Object> extension) {
        Map<String, Object> runtime = new LinkedHashMap<>(extension);
        if ("SERVER_BINDING".equalsIgnoreCase(string(runtime.get("extensionType")))) {
            runtime.remove("content");
            runtime.remove("processedContent");
            runtime.remove("configJson");
        }
        runtime.remove("draftVersion");
        runtime.remove("releaseVersion");
        runtime.remove("contentHash");
        return runtime;
    }

    Map<String, Object> filterBuilder(Map<String, Object> builder,
                                      String applicationCode,
                                      Set<String> permissionCodes) {
        return filterBuilder(builder, applicationCode, permissionCodes, false);
    }

    Map<String, Object> filterBuilder(Map<String, Object> builder,
                                      String applicationCode,
                                      Set<String> permissionCodes,
                                      boolean bypassRoleAccess) {
        if (builder.isEmpty()) {
            return builder;
        }
        List<Map<String, Object>> nodes = maps(builder.get("nodes"));
        Map<String, Map<String, Object>> nodesById = new LinkedHashMap<>();
        nodes.forEach(node -> {
            String id = StringUtils.trimToNull(string(node.get("id")));
            if (id != null) {
                nodesById.put(id, node);
            }
        });

        Set<String> accessiblePages = new LinkedHashSet<>();
        nodes.stream()
                .filter(node -> "page".equalsIgnoreCase(string(node.get("type"))))
                .filter(node -> accessAllowed(node, applicationCode, permissionCodes, bypassRoleAccess))
                .filter(node -> ancestorsAllow(node, nodesById, applicationCode, permissionCodes, bypassRoleAccess))
                .map(node -> string(node.get("id")))
                .filter(StringUtils::isNotBlank)
                .forEach(accessiblePages::add);

        Set<String> includedNodes = new LinkedHashSet<>(accessiblePages);
        accessiblePages.forEach(pageId -> includeAncestors(pageId, nodesById, includedNodes));
        List<Map<String, Object>> filteredNodes = new ArrayList<>();
        nodes.stream()
                .filter(node -> includedNodes.contains(string(node.get("id"))))
                .forEach(node -> filteredNodes.add(new LinkedHashMap<>(node)));

        Map<String, Object> pages = map(builder.get("pages"));
        Map<String, Object> filteredPages = new LinkedHashMap<>();
        accessiblePages.forEach(pageId -> {
            if (pages.containsKey(pageId)) {
                filteredPages.put(pageId, pages.get(pageId));
            }
        });

        String homePageId = StringUtils.trimToNull(string(builder.get("homePageId")));
        if (!accessiblePages.contains(homePageId)) {
            homePageId = filteredNodes.stream()
                    .filter(node -> accessiblePages.contains(string(node.get("id"))))
                    .sorted(Comparator.comparingInt(node -> integer(node.get("sort"))))
                    .map(node -> string(node.get("id")))
                    .findFirst().orElse(null);
        }

        Map<String, Object> filtered = new LinkedHashMap<>(builder);
        filtered.put("homePageId", homePageId);
        filtered.put("nodes", filteredNodes);
        filtered.put("pages", filteredPages);
        return filtered;
    }

    private boolean ancestorsAllow(Map<String, Object> node,
                                   Map<String, Map<String, Object>> nodesById,
                                   String applicationCode,
                                   Set<String> permissionCodes,
                                   boolean bypassRoleAccess) {
        Set<String> visited = new HashSet<>();
        String parentId = StringUtils.trimToNull(string(node.get("parentId")));
        while (parentId != null && visited.add(parentId)) {
            Map<String, Object> parent = nodesById.get(parentId);
            if (parent == null || !accessAllowed(parent, applicationCode, permissionCodes, bypassRoleAccess)) {
                return false;
            }
            parentId = StringUtils.trimToNull(string(parent.get("parentId")));
        }
        return parentId == null;
    }

    private void includeAncestors(String nodeId,
                                  Map<String, Map<String, Object>> nodesById,
                                  Set<String> includedNodes) {
        Set<String> visited = new HashSet<>();
        Map<String, Object> node = nodesById.get(nodeId);
        String parentId = node == null ? null : StringUtils.trimToNull(string(node.get("parentId")));
        while (parentId != null && visited.add(parentId)) {
            includedNodes.add(parentId);
            Map<String, Object> parent = nodesById.get(parentId);
            parentId = parent == null ? null : StringUtils.trimToNull(string(parent.get("parentId")));
        }
    }

    private boolean accessAllowed(Map<String, Object> node,
                                  String applicationCode,
                                  Set<String> permissionCodes,
                                  boolean bypassRoleAccess) {
        if (bypassRoleAccess) {
            return true;
        }
        // 存量对象页没有页面级 sys_resource；它继承应用门户和对象接口权限。
        if (Boolean.TRUE.equals(node.get("legacyObjectPage"))) {
            return true;
        }
        // 页面组只承担导航结构，入口授权落在子页面上（与权限工作台「结构组隐式」一致）。
        String type = StringUtils.defaultString(string(node.get("type"))).trim().toLowerCase();
        if ("group".equals(type) || "page-group".equals(type) || "page_group".equals(type)
                || "pagegroup".equals(type) || "menu-group".equals(type)
                || "menu_group".equals(type) || "directory".equals(type) || "folder".equals(type)) {
            return true;
        }
        String nodeId = StringUtils.trimToNull(string(node.get("id")));
        String normalizedApplicationCode = StringUtils.trimToNull(applicationCode);
        if (nodeId == null || normalizedApplicationCode == null) {
            return false;
        }
        String permission = "ai:business:application:" + normalizedApplicationCode + ":page:" + nodeId;
        return permissionCodes.contains(permission)
                || permissionCodes.contains("*:*:*")
                || permissionCodes.contains("**");
    }

    private BusinessApplicationVO toApplication(Map<String, Object> snapshot,
                                                BusinessApplicationVO current,
                                                Integer versionNo) {
        BusinessApplicationVO application = new BusinessApplicationVO();
        application.setId(longValue(snapshot.get("id")));
        application.setApplicationCode(StringUtils.defaultIfBlank(
                string(snapshot.get("applicationCode")), current.getApplicationCode()));
        application.setPortalSlug(StringUtils.defaultIfBlank(
                string(snapshot.get("portalSlug")), application.getApplicationCode()));
        application.setApplicationName(string(snapshot.get("applicationName")));
        application.setSuiteCode(string(snapshot.get("suiteCode")));
        application.setSuiteName(current.getSuiteName());
        application.setIcon(string(snapshot.get("icon")));
        application.setDescription(string(snapshot.get("description")));
        application.setStatus(integer(snapshot.get("status")));
        application.setDesignStatus(StringUtils.defaultIfBlank(
                string(snapshot.get("designStatus")), "PUBLISHED"));
        application.setLastPublishVersion(versionNo);
        application.setOptions(writeJson(snapshot.get("options")));
        application.setPortalConfig(writeJson(snapshot.get("portalConfig")));
        application.setAiAssistantConfig(writeJson(snapshot.get("aiAssistantConfig")));
        return application;
    }

    private boolean hasReachableHomePage(BusinessApplicationRuntimeVO runtime) {
        if (runtime == null || runtime.getApplication() == null) {
            return false;
        }
        Map<String, Object> options = readJsonObject(runtime.getApplication().getOptions());
        Map<String, Object> builder = map(options.get("inAppBuilder"));
        String homePageId = StringUtils.trimToNull(string(builder.get("homePageId")));
        return homePageId != null && map(builder.get("pages")).containsKey(homePageId);
    }

    private BusinessApplicationObjectVO toObject(Map<String, Object> snapshot) {
        BusinessApplicationObjectVO object = objectMapper.convertValue(
                snapshot, BusinessApplicationObjectVO.class);
        object.setObjectId(longValue(snapshot.get("objectId")));
        return object;
    }

    private BusinessAppVO toEntry(Map<String, Object> snapshot) {
        Map<String, Object> normalized = new LinkedHashMap<>(snapshot);
        Object options = normalized.remove("options");
        BusinessAppVO entry = objectMapper.convertValue(normalized, BusinessAppVO.class);
        entry.setOptions(writeJson(options));
        return entry;
    }

    private Set<String> resolvePermissionCodes() {
        try {
            var loginUser = SessionHelper.getLoginUser();
            return loginUser == null || loginUser.getPermissions() == null
                    ? Set.of() : Set.copyOf(loginUser.getPermissions());
        } catch (Exception ignored) {
            return Set.of();
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("应用运行配置序列化失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJsonObject(String value) {
        if (StringUtils.isBlank(value)) {
            return new LinkedHashMap<>();
        }
        try {
            return map(objectMapper.readValue(value, Map.class));
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> raw
                ? new LinkedHashMap<>((Map<String, Object>) raw) : new LinkedHashMap<>();
    }

    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        collection.stream().filter(Map.class::isInstance).map(this::map).forEach(result::add);
        return result;
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer integer(Object value) {
        try {
            return value == null ? 0 : Integer.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Long longValue(Object value) {
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }
}
