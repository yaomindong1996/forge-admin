package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPageMenuDTO;
import com.mdframe.forge.plugin.generator.service.MenuRegisterAdapter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 发布或回滚时把已发布页面树投影为系统菜单资源。 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationPageMenuPublishService {

    private static final String PORTAL_COMPONENT = "app-center/application-portal";

    private final MenuRegisterAdapter menuRegisterAdapter;

    public Map<String, Long> sync(Map<String, Object> snapshot) {
        Map<String, Object> application = map(snapshot.get("application"));
        String applicationCode = StringUtils.trimToNull(string(application.get("applicationCode")));
        if (applicationCode == null) {
            return Map.of();
        }
        Map<String, Object> builder = map(map(application.get("options")).get("inAppBuilder"));
        List<Map<String, Object>> nodes = maps(builder.get("nodes"));
        if (nodes.isEmpty()) {
            return menuRegisterAdapter.syncApplicationPageMenus(applicationCode, List.of());
        }
        List<Map<String, Object>> publishedObjects = maps(snapshot.get("objects"));
        String portalIdentifier = resolvePortalIdentifier(application, applicationCode);
        String applicationName = StringUtils.defaultIfBlank(
                string(application.get("applicationName")), applicationCode);
        List<BusinessApplicationPageMenuDTO> menus = new ArrayList<>();
        // 门户 RBAC：每个页面/页面组都注册可授权资源（默认隐藏，不进系统侧栏）。
        // 仅「挂载系统菜单 + 已选父级」时才写成可见系统菜单；未选父级的假挂载降级为隐藏入口，避免发布失败。
        java.util.LinkedHashSet<String> clients = new java.util.LinkedHashSet<>();
        for (Map<String, Object> node : nodes) {
            String nodeId = StringUtils.trimToNull(string(node.get("id")));
            if (nodeId == null) {
                continue;
            }
            boolean directory = isGroupNode(node);
            if (!directory && !"page".equalsIgnoreCase(StringUtils.defaultString(string(node.get("type"))).trim())) {
                continue;
            }
            String title = resolveMenuTitle(node, directory);
            Integer sort = resolveMenuSort(node);
            for (String clientCode : resolveClientCodes(node)) {
                clients.add(clientCode);
                String externalMenuParentId = resolveExternalMenuParentId(node, clientCode);
                boolean mounted = !directory && systemMenuVisible(node) && externalMenuParentId != null;
                String menuPath = resolveMenuPath(application, portalIdentifier,
                        node, nodeId, directory, clientCode, publishedObjects);
                String menuComponent = "h5".equalsIgnoreCase(clientCode)
                        ? menuPath
                        : resolveDesktopMenuComponent(menuPath);
                BusinessApplicationPageMenuDTO item = menu(
                        nodeId,
                        mounted ? externalMenuParentId : null,
                        title,
                        menuPath,
                        menuComponent,
                        permission(applicationCode, nodeId),
                        string(node.get("icon")),
                        sort,
                        directory,
                        mounted,
                        clientCode);
                item.setExternalParent(mounted);
                menus.add(item);
            }
        }
        for (String clientCode : clients) {
            BusinessApplicationPageMenuDTO root = menu(
                    "root",
                    null,
                    applicationName + "门户",
                    "/app/" + portalIdentifier,
                    PORTAL_COMPONENT,
                    permission(applicationCode, "root"),
                    null,
                    0,
                    true,
                    false,
                    clientCode);
            menus.add(0, root);
        }
        Map<String, Long> bindings = menuRegisterAdapter.syncApplicationPageMenus(applicationCode, menus);
        snapshot.put("pageMenu", bindings);
        return bindings;
    }

    public List<String> validate(Map<String, Object> snapshot) {
        Map<String, Object> application = map(snapshot.get("application"));
        Map<String, Object> builder = map(map(application.get("options")).get("inAppBuilder"));
        List<Map<String, Object>> nodes = maps(builder.get("nodes"));
        List<String> errors = new ArrayList<>();
        if (!nodes.isEmpty()) {
            String homePageId = StringUtils.trimToNull(string(builder.get("homePageId")));
            if (homePageId == null || nodes.stream().noneMatch(node -> homePageId.equals(string(node.get("id"))))) {
                errors.add("应用页面未设置有效默认首页");
            }
        }
        // 未选父级的「假挂载」不阻塞发布；真正挂载需开关 + 父级齐全（见 sync）
        return errors;
    }

    private BusinessApplicationPageMenuDTO menu(String nodeId, String parentNodeId, String title, String path,
                                                String component, String perms, String icon, Integer sort,
                                                boolean directory, boolean visible, String clientCode) {
        BusinessApplicationPageMenuDTO item = new BusinessApplicationPageMenuDTO();
        item.setNodeId(nodeId);
        item.setParentNodeId(parentNodeId);
        item.setMenuName(title);
        item.setPath(path);
        item.setComponent(component);
        item.setPerms(perms);
        item.setIcon(icon);
        item.setSort(sort == null ? 0 : sort);
        item.setDirectory(directory);
        item.setVisible(visible);
        item.setClientCode(clientCode);
        return item;
    }

    private boolean systemMenuVisible(Map<String, Object> node) {
        Object value = node.get("systemMenuVisible");
        if (value == null) {
            value = map(node.get("settings")).get("systemMenuVisible");
        }
        return Boolean.TRUE.equals(value);
    }

    /**
     * 读取页面节点上的菜单名称：优先使用 menuName，其次 settings.menuName，兜底用 title。
     */
    private String resolveMenuTitle(Map<String, Object> node, boolean directory) {
        String menuName = StringUtils.trimToNull(string(node.get("menuName")));
        if (menuName == null) {
            menuName = StringUtils.trimToNull(string(map(node.get("settings")).get("menuName")));
        }
        String title = StringUtils.defaultIfBlank(string(node.get("title")), directory ? "页面组" : "未命名页面");
        return StringUtils.defaultIfBlank(menuName, title);
    }

    /**
     * 解析页面节点的挂载位置对应的 clientCode。
     * <p>优先读取节点自身的 mountTarget，其次读取 settings.mountTarget，
     * 默认 ADMIN（pc）。MOBILE 映射为 h5，其他均映射为 pc。</p>
     */
    private List<String> resolveClientCodes(Map<String, Object> node) {
        String mountTarget = StringUtils.trimToNull(string(node.get("mountTarget")));
        if (mountTarget == null) {
            mountTarget = StringUtils.trimToNull(string(map(node.get("settings")).get("mountTarget")));
        }
        if ("MOBILE".equalsIgnoreCase(mountTarget)) {
            return List.of("h5");
        }
        if ("BOTH".equalsIgnoreCase(mountTarget)) {
            return List.of("pc", "h5");
        }
        return List.of("pc");
    }

    private boolean isGroupNode(Map<String, Object> node) {
        String type = StringUtils.defaultString(string(node.get("type"))).trim().toLowerCase();
        return "group".equals(type) || "page-group".equals(type) || "page_group".equals(type)
                || "pagegroup".equals(type) || "menu-group".equals(type) || "menu_group".equals(type)
                || "directory".equals(type) || "folder".equals(type);
    }

    /**
     * 解析页面节点的外部门级 ID（sys_resource.id）。
     * <p>当页面配置了 menuParentId 时，该 ID 指向系统菜单树中的某个目录/菜单，
     * 页面将直接挂载到该目录下。返回 null 表示未配置外部门级，发布时跳过。</p>
     */
    private String resolveExternalMenuParentId(Map<String, Object> node, String clientCode) {
        String parentKey = "h5".equalsIgnoreCase(clientCode) ? "mobileMenuParentId" : "menuParentId";
        String menuParentId = StringUtils.trimToNull(string(node.get(parentKey)));
        if (menuParentId == null) {
            menuParentId = StringUtils.trimToNull(string(map(node.get("settings")).get(parentKey)));
        }
        return menuParentId;
    }

    /**
     * 解析页面节点的菜单排序：优先读取 menuSort，其次 settings.menuSort，兑底用 sort。
     */
    private Integer resolveMenuSort(Map<String, Object> node) {
        Object menuSort = node.get("menuSort");
        if (menuSort == null) {
            menuSort = map(node.get("settings")).get("menuSort");
        }
        if (menuSort != null) {
            try {
                return Integer.valueOf(String.valueOf(menuSort));
            } catch (Exception ignored) {
                // fall through to default sort
            }
        }
        return integer(node.get("sort"));
    }

    private String permission(String applicationCode, String nodeId) {
        return "ai:business:application:" + applicationCode + ":page:" + nodeId;
    }

    private String resolvePortalIdentifier(Map<String, Object> application, String applicationCode) {
        return StringUtils.defaultIfBlank(string(application.get("portalSlug")), applicationCode);
    }

    private String resolveMenuPath(Map<String, Object> application,
                                   String portalIdentifier, Map<String, Object> node, String nodeId,
                                   boolean directory, String clientCode, List<Map<String, Object>> publishedObjects) {
        if (!"h5".equalsIgnoreCase(clientCode)) {
            if (directory) {
                return "/app/" + portalIdentifier;
            }
            String desktopPagePath = resolveDesktopPagePath(application, node, publishedObjects);
            return StringUtils.defaultIfBlank(desktopPagePath,
                    "/app/" + portalIdentifier + "?pageId=" + nodeId);
        }
        if (directory) {
            return "/app/" + portalIdentifier;
        }
        Map<String, Object> objectRef = map(node.get("objectRef"));
        String configKey = resolveObjectConfigKey(node, objectRef, publishedObjects);
        if (StringUtils.isBlank(configKey)) {
            return "/pages/app-entry";
        }
        String applicationId = StringUtils.trimToNull(string(application.get("id")));
        StringBuilder path = new StringBuilder("/pages/lowcode-runtime?configKey=")
                .append(configKey);
        if (applicationId != null) {
            path.append("&appId=").append(applicationId);
        }
        return path.toString();
    }

    private String resolveDesktopPagePath(Map<String, Object> application, Map<String, Object> node,
                                          List<Map<String, Object>> publishedObjects) {
        Map<String, Object> objectRef = map(node.get("objectRef"));
        String configKey = resolveObjectConfigKey(node, objectRef, publishedObjects);
        if (StringUtils.isBlank(configKey)) {
            return null;
        }
        String applicationId = StringUtils.trimToNull(string(application.get("id")));
        String pageKey = StringUtils.defaultIfBlank(string(node.get("pageKey")),
                string(objectRef.get("pageKey")));
        pageKey = StringUtils.defaultIfBlank(pageKey, "list");
        String pageMode = StringUtils.defaultIfBlank(string(node.get("pageMode")),
                string(objectRef.get("pageMode")));
        pageMode = StringUtils.defaultIfBlank(pageMode, "crud");
        StringBuilder path = new StringBuilder("/ai/crud-page/").append(configKey)
                .append("?pageKey=").append(pageKey);
        if (applicationId != null) {
            path.append("&appId=").append(applicationId);
        }
        String formKey = StringUtils.defaultIfBlank(string(node.get("formKey")),
                string(objectRef.get("formKey")));
        if (StringUtils.isNotBlank(formKey)) {
            path.append("&formKey=").append(formKey);
        }
        if ("form".equalsIgnoreCase(pageMode)) {
            path.append("&runtimeOpenMode=CREATE_FORM&mode=create");
        }
        return path.toString();
    }

    private String resolveObjectConfigKey(Map<String, Object> node, Map<String, Object> objectRef,
                                          List<Map<String, Object>> publishedObjects) {
        String configKey = StringUtils.defaultIfBlank(string(node.get("configKey")),
                string(objectRef.get("configKey")));
        if (StringUtils.isNotBlank(configKey)) {
            return configKey;
        }
        String objectId = StringUtils.trimToNull(string(objectRef.get("objectId")));
        String objectCode = StringUtils.trimToNull(string(objectRef.get("objectCode")));
        return publishedObjects.stream()
                .filter(object -> (objectId != null && objectId.equals(string(object.get("objectId"))))
                        || (objectCode != null && objectCode.equals(string(object.get("objectCode")))))
                .map(object -> StringUtils.trimToNull(string(object.get("configKey"))))
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private String resolveDesktopMenuComponent(String menuPath) {
        return StringUtils.isNotBlank(menuPath) && menuPath.startsWith("/ai/crud-page/")
                ? "ai/crud-page" : PORTAL_COMPONENT;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) { return value instanceof Map<?, ?> raw ? new LinkedHashMap<>((Map<String, Object>) raw) : Map.of(); }
    private String string(Object value) { return value == null ? null : String.valueOf(value); }
    private Integer integer(Object value) { try { return value == null ? 0 : Integer.valueOf(String.valueOf(value)); } catch (Exception ignored) { return 0; } }
    private List<Map<String, Object>> maps(Object value) { if (!(value instanceof List<?> list)) return List.of(); return list.stream().filter(Map.class::isInstance).map(this::map).toList(); }
}
