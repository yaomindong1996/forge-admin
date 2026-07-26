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

    private static final String ROOT_NODE_ID = "__application_menu_root__";
    private static final String RUNTIME_COMPONENT = "app-center/application-runtime.[applicationCode]";

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
        String rootPerms = permission(applicationCode, "root");
        List<BusinessApplicationPageMenuDTO> menus = new ArrayList<>();
        BusinessApplicationPageMenuDTO root = menu(ROOT_NODE_ID, null,
                StringUtils.defaultIfBlank(string(application.get("applicationName")), applicationCode),
                "/app-center/application/" + applicationCode + "/runtime", null, rootPerms,
                string(application.get("icon")), 0, true, true, true, List.of());
        menus.add(root);
        for (Map<String, Object> node : nodes) {
            if (!systemMenuVisible(node)) {
                continue;
            }
            String nodeId = StringUtils.trimToNull(string(node.get("id")));
            if (nodeId == null) {
                continue;
            }
            boolean directory = "group".equalsIgnoreCase(string(node.get("type")));
            String parentNodeId = StringUtils.defaultIfBlank(StringUtils.trimToNull(string(node.get("parentId"))), ROOT_NODE_ID);
            String title = StringUtils.defaultIfBlank(string(node.get("title")), directory ? "页面组" : "未命名页面");
            Map<String, Object> access = map(firstNonNull(node.get("access"), map(node.get("settings")).get("access")));
            boolean inherit = !"roles".equalsIgnoreCase(string(access.get("mode")));
            List<Long> roleIds = longList(access.get("roleIds"));
            menus.add(menu(nodeId, parentNodeId, title,
                    directory ? "/app-center/application/" + applicationCode + "/runtime"
                            : "/app-center/application/" + applicationCode + "/runtime?pageId=" + nodeId,
                    directory ? null : RUNTIME_COMPONENT, permission(applicationCode, nodeId),
                    string(node.get("icon")), integer(node.get("sort")), directory, true, inherit, roleIds));
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
        for (Map<String, Object> node : nodes) {
            if (!systemMenuVisible(node)) {
                continue;
            }
            Map<String, Object> access = map(firstNonNull(node.get("access"), map(node.get("settings")).get("access")));
            if ("roles".equalsIgnoreCase(string(access.get("mode"))) && longList(access.get("roleIds")).isEmpty()) {
                errors.add("系统菜单页面未配置访问角色: " + StringUtils.defaultIfBlank(string(node.get("title")), "未命名页面"));
            }
        }
        return errors;
    }

    private BusinessApplicationPageMenuDTO menu(String nodeId, String parentNodeId, String title, String path,
                                                String component, String perms, String icon, Integer sort,
                                                boolean directory, boolean visible, boolean inherit, List<Long> roleIds) {
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
        item.setInheritRuntimeRoles(inherit);
        item.setRoleIds(roleIds);
        return item;
    }

    private boolean systemMenuVisible(Map<String, Object> node) {
        Object value = firstNonNull(node.get("systemMenuVisible"), map(node.get("settings")).get("systemMenuVisible"));
        return !(value instanceof Boolean flag) || flag;
    }

    private String permission(String applicationCode, String nodeId) {
        return "ai:business:application:" + applicationCode + ":page:" + nodeId;
    }

    private Object firstNonNull(Object left, Object right) { return left != null ? left : right; }
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) { return value instanceof Map<?, ?> raw ? new LinkedHashMap<>((Map<String, Object>) raw) : Map.of(); }
    private String string(Object value) { return value == null ? null : String.valueOf(value); }
    private Integer integer(Object value) { try { return value == null ? 0 : Integer.valueOf(String.valueOf(value)); } catch (Exception ignored) { return 0; } }
    private List<Map<String, Object>> maps(Object value) { if (!(value instanceof List<?> list)) return List.of(); return list.stream().filter(Map.class::isInstance).map(this::map).toList(); }
    private List<Long> longList(Object value) { if (!(value instanceof List<?> list)) return List.of(); return list.stream().map(this::string).map(StringUtils::trimToNull).filter(java.util.Objects::nonNull).map(item -> { try { return Long.valueOf(item); } catch (Exception ignored) { return null; } }).filter(java.util.Objects::nonNull).toList(); }
}
