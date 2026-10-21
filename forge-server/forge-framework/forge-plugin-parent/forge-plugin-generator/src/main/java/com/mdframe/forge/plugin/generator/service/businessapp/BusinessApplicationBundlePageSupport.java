package com.mdframe.forge.plugin.generator.service.businessapp;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * 调试包按页导出：从选中页面收集对象依赖，并按关系做闭包；过滤 inAppBuilder。
 * 纯函数工具，便于单测。
 */
final class BusinessApplicationBundlePageSupport {

    private static final Set<String> OBJECT_CODE_KEYS = Set.of(
            "objectcode", "referenceobjectcode", "relatedobjectcode",
            "sourceobjectcode", "targetobjectcode", "childobjectcode", "parentobjectcode"
    );

    private static final Set<String> OBJECT_REF_KEYS = Set.of(
            "objectref", "businessobjectref", "targetobjectref", "sourceobjectref"
    );

    private BusinessApplicationBundlePageSupport() {
    }

    static List<Map<String, Object>> listPageNodes(Map<String, Object> builder) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> node : maps(builder.get("nodes"))) {
            if ("page".equalsIgnoreCase(text(node.get("type")))) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * 市场包/紧凑调试包常只带 pages+nav，没有 nodes。
     * 导入前展开成正式 inAppBuilder（nodes + pages.layout + formAssets），否则 pageCount=0、运行壳只有系统菜单。
     *
     * @param objectSnapshots 包内 objects（已 remap objectCode），用于补 formAssets / AiCrudPage
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> normalizeCompactInAppBuilder(Map<String, Object> builder,
                                                            List<Map<String, Object>> objectSnapshots) {
        Map<String, Object> next = builder == null ? new LinkedHashMap<>() : new LinkedHashMap<>(builder);
        List<Map<String, Object>> existingNodes = maps(next.get("nodes"));
        if (!existingNodes.isEmpty()) {
            return next;
        }
        Map<String, Object> pages = map(next.get("pages"));
        List<Map<String, Object>> nav = maps(next.get("nav"));
        List<String> pageIds = new ArrayList<>();
        if (!nav.isEmpty()) {
            for (Map<String, Object> item : nav) {
                String id = text(item.get("id"));
                if (id != null && !pageIds.contains(id)) {
                    pageIds.add(id);
                }
            }
        }
        if (pageIds.isEmpty()) {
            pageIds.addAll(pages.keySet());
        }
        if (pageIds.isEmpty()) {
            return next;
        }

        Map<String, Map<String, Object>> objectsByCode = indexObjectsByCode(objectSnapshots);
        Map<String, Object> primaryObject = resolvePrimaryObject(objectSnapshots);
        List<Map<String, Object>> nodes = new ArrayList<>();
        Map<String, Object> normalizedPages = new LinkedHashMap<>();
        List<Map<String, Object>> formAssets = new ArrayList<>(maps(next.get("formAssets")));
        Set<String> formAssetIds = new LinkedHashSet<>();
        for (Map<String, Object> asset : formAssets) {
            String id = text(asset.get("id"));
            if (id != null) {
                formAssetIds.add(id);
            }
        }

        int sort = 0;
        for (String pageId : pageIds) {
            Map<String, Object> pageMeta = map(pages.get(pageId));
            Map<String, Object> navItem = nav.stream()
                    .filter(item -> pageId.equals(text(item.get("id"))))
                    .findFirst()
                    .orElse(Map.of());
            String title = StringUtils.defaultIfBlank(text(navItem.get("title")),
                    StringUtils.defaultIfBlank(text(pageMeta.get("title")), pageId));
            String pageType = StringUtils.defaultIfBlank(text(pageMeta.get("pageType")), "object");
            Map<String, Object> objectRef = map(pageMeta.get("objectRef"));
            if (text(objectRef.get("objectCode")) == null && primaryObject != null) {
                objectRef = new LinkedHashMap<>();
                objectRef.put("objectCode", text(primaryObject.get("objectCode")));
                objectRef.put("objectName", StringUtils.defaultIfBlank(
                        text(primaryObject.get("objectName")), text(primaryObject.get("objectCode"))));
            }
            String objectCode = text(objectRef.get("objectCode"));
            Map<String, Object> objectSnapshot = objectCode == null ? null : objectsByCode.get(objectCode);

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", pageId);
            node.put("type", "page");
            node.put("title", title);
            node.put("pageType", pageType);
            node.put("pageShape", StringUtils.defaultIfBlank(text(pageMeta.get("pageShape")),
                    "object".equalsIgnoreCase(pageType) ? "form" : "custom"));
            node.put("pageTemplate", StringUtils.defaultIfBlank(text(pageMeta.get("pageTemplate")),
                    text(node.get("pageShape"))));
            node.put("parentId", null);
            node.put("sort", sort * 10);
            node.put("navigationVisible", true);
            node.put("systemMenuVisible", false);
            if (!objectRef.isEmpty()) {
                if (StringUtils.isBlank(text(objectRef.get("pageMode")))) {
                    objectRef.put("pageMode", "form");
                }
                if (StringUtils.isBlank(text(objectRef.get("pageKey")))) {
                    objectRef.put("pageKey", "list");
                }
                node.put("objectRef", objectRef);
            }
            nodes.add(node);

            String formAssetId = ensureFormAsset(formAssets, formAssetIds, pageId, title, objectSnapshot, objectRef);
            Map<String, Object> layout = map(pageMeta.get("layout"));
            Map<String, Object> gridLayout = map(layout.get("gridLayout"));
            List<?> items = gridLayout.get("items") instanceof List<?> list ? list : List.of();
            if (items.isEmpty() && objectCode != null) {
                gridLayout = buildFormCrudGrid(title, formAssetId, objectRef);
                layout.put("gridLayout", gridLayout);
            }
            Map<String, Object> page = new LinkedHashMap<>();
            page.put("title", title);
            page.put("description", StringUtils.defaultString(text(pageMeta.get("description"))));
            page.put("layout", layout.isEmpty()
                    ? Map.of("gridLayout", buildFormCrudGrid(title, formAssetId, objectRef))
                    : layout);
            normalizedPages.put(pageId, page);
            sort++;
        }

        next.put("schemaVersion", next.get("schemaVersion") == null ? 2 : next.get("schemaVersion"));
        next.put("nodes", nodes);
        next.put("pages", normalizedPages);
        next.put("formAssets", formAssets);
        if (text(next.get("homePageId")) == null && !pageIds.isEmpty()) {
            next.put("homePageId", pageIds.get(0));
        }
        next.remove("nav");
        return next;
    }

    private static Map<String, Map<String, Object>> indexObjectsByCode(List<Map<String, Object>> objectSnapshots) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (objectSnapshots == null) {
            return result;
        }
        for (Map<String, Object> snapshot : objectSnapshots) {
            String code = text(snapshot == null ? null : snapshot.get("objectCode"));
            if (code != null) {
                result.put(code, snapshot);
            }
        }
        return result;
    }

    private static Map<String, Object> resolvePrimaryObject(List<Map<String, Object>> objectSnapshots) {
        if (objectSnapshots == null || objectSnapshots.isEmpty()) {
            return null;
        }
        for (Map<String, Object> snapshot : objectSnapshots) {
            if (snapshot == null) {
                continue;
            }
            String role = StringUtils.upperCase(StringUtils.defaultString(text(snapshot.get("role"))));
            if ("PRIMARY".equals(role) && text(snapshot.get("objectCode")) != null) {
                return snapshot;
            }
        }
        for (Map<String, Object> snapshot : objectSnapshots) {
            if (snapshot != null && text(snapshot.get("objectCode")) != null) {
                return snapshot;
            }
        }
        return null;
    }

    private static String ensureFormAsset(List<Map<String, Object>> formAssets,
                                          Set<String> formAssetIds,
                                          String pageId,
                                          String title,
                                          Map<String, Object> objectSnapshot,
                                          Map<String, Object> objectRef) {
        String preferredId = "form_" + StringUtils.defaultIfBlank(text(objectRef.get("objectCode")), pageId);
        if (formAssetIds.contains(preferredId)) {
            return preferredId;
        }
        Object schema = objectSnapshot == null ? null : objectSnapshot.get("formDesignerSchema");
        if (schema == null) {
            return preferredId;
        }
        Map<String, Object> asset = new LinkedHashMap<>();
        asset.put("id", preferredId);
        asset.put("formKey", preferredId);
        asset.put("name", title);
        asset.put("formDesignerSchema", schema);
        formAssets.add(asset);
        formAssetIds.add(preferredId);
        return preferredId;
    }

    private static Map<String, Object> buildFormCrudGrid(String title,
                                                         String formAssetId,
                                                         Map<String, Object> objectRef) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("title", title);
        props.put("formAssetId", formAssetId);
        props.put("formAssetFieldsInitialized", true);
        props.put("formOnly", true);
        props.put("hideToolbar", true);
        props.put("hideBatchDelete", true);
        props.put("showSearch", false);
        props.put("formOpenMode", "flat");
        props.put("modalType", "flat");
        props.put("layoutType", "form");
        if (objectRef != null && !objectRef.isEmpty()) {
            props.put("objectRef", new LinkedHashMap<>(objectRef));
        }
        String objectCode = objectRef == null ? null : text(objectRef.get("objectCode"));
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("id", "blk_" + StringUtils.defaultIfBlank(objectCode, "page") + "_crud");
        block.put("blockType", "AiCrudPage");
        block.put("label", title);
        block.put("gridX", 0);
        block.put("gridY", 0);
        block.put("gridW", 12);
        block.put("gridH", 18);
        block.put("props", props);
        Map<String, Object> grid = new LinkedHashMap<>();
        grid.put("cols", 12);
        grid.put("rowHeight", 32);
        grid.put("gap", 8);
        grid.put("designWidth", 1366);
        grid.put("layoutType", "form");
        grid.put("items", List.of(block));
        return grid;
    }

    static Set<String> collectObjectCodesFromPages(Map<String, Object> builder, Set<String> pageIds) {
        Set<String> codes = new LinkedHashSet<>();
        if (pageIds == null || pageIds.isEmpty()) {
            return codes;
        }
        Map<String, Object> pages = map(builder.get("pages"));
        for (Map<String, Object> node : listPageNodes(builder)) {
            String pageId = text(node.get("id"));
            if (!pageIds.contains(pageId)) {
                continue;
            }
            collectObjectCodesDeep(node.get("objectRef"), codes);
            collectObjectCodesDeep(pages.get(pageId), codes);
        }
        for (Map<String, Object> asset : maps(builder.get("formAssets"))) {
            String assetId = text(asset.get("id"));
            if (assetId == null || !formAssetReferenced(builder, pageIds, assetId)) {
                continue;
            }
            collectObjectCodesDeep(asset, codes);
        }
        return codes;
    }

    static Set<String> expandObjectClosure(Set<String> seedCodes,
                                           Map<String, Set<String>> adjacency) {
        Set<String> closure = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        for (String code : seedCodes) {
            if (StringUtils.isNotBlank(code) && closure.add(code)) {
                queue.add(code);
            }
        }
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String next : adjacency.getOrDefault(current, Set.of())) {
                if (closure.add(next)) {
                    queue.add(next);
                }
            }
        }
        return closure;
    }

    static Map<String, Set<String>> buildRelationAdjacency(List<Map<String, Object>> objectSnapshots) {
        Map<String, Set<String>> adjacency = new LinkedHashMap<>();
        if (objectSnapshots == null) {
            return adjacency;
        }
        for (Map<String, Object> snapshot : objectSnapshots) {
            String self = text(snapshot.get("objectCode"));
            if (self == null) {
                continue;
            }
            adjacency.computeIfAbsent(self, key -> new LinkedHashSet<>());
            collectObjectCodesDeep(snapshot.get("fields"), adjacency.computeIfAbsent(self, key -> new LinkedHashSet<>()));
            for (Map<String, Object> relation : maps(snapshot.get("relations"))) {
                String source = text(relation.get("sourceObjectCode"));
                String target = text(relation.get("targetObjectCode"));
                link(adjacency, source, target);
                link(adjacency, target, source);
                link(adjacency, self, source);
                link(adjacency, self, target);
            }
        }
        // fields may have added codes as neighbors of self; ensure mutual presence in graph keys
        for (Map.Entry<String, Set<String>> entry : new ArrayList<>(adjacency.entrySet())) {
            for (String neighbor : new ArrayList<>(entry.getValue())) {
                if (StringUtils.isBlank(neighbor)) {
                    continue;
                }
                adjacency.computeIfAbsent(neighbor, key -> new LinkedHashSet<>()).add(entry.getKey());
            }
        }
        return adjacency;
    }

    static Map<String, Object> filterBuilder(Map<String, Object> builder, Set<String> pageIds) {
        Map<String, Object> source = map(builder);
        if (pageIds == null || pageIds.isEmpty()) {
            return source;
        }
        Set<String> keepNodeIds = new LinkedHashSet<>(pageIds);
        Map<String, Map<String, Object>> nodeById = new LinkedHashMap<>();
        for (Map<String, Object> node : maps(source.get("nodes"))) {
            String id = text(node.get("id"));
            if (id != null) {
                nodeById.put(id, node);
            }
        }
        for (String pageId : pageIds) {
            String cursor = pageId;
            while (cursor != null && nodeById.containsKey(cursor)) {
                keepNodeIds.add(cursor);
                cursor = text(nodeById.get(cursor).get("parentId"));
            }
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map<String, Object> node : maps(source.get("nodes"))) {
            String id = text(node.get("id"));
            if (id != null && keepNodeIds.contains(id)) {
                nodes.add(new LinkedHashMap<>(node));
            }
        }
        Map<String, Object> pages = new LinkedHashMap<>();
        Map<String, Object> sourcePages = map(source.get("pages"));
        for (String pageId : pageIds) {
            if (sourcePages.containsKey(pageId)) {
                pages.put(pageId, sourcePages.get(pageId));
            }
        }
        List<Map<String, Object>> formAssets = new ArrayList<>();
        Set<String> usedAssetIds = new LinkedHashSet<>();
        for (String pageId : pageIds) {
            collectFormAssetIds(sourcePages.get(pageId), usedAssetIds);
            Map<String, Object> node = nodeById.get(pageId);
            if (node != null) {
                collectFormAssetIds(node, usedAssetIds);
            }
        }
        for (Map<String, Object> asset : maps(source.get("formAssets"))) {
            String id = text(asset.get("id"));
            if (id != null && usedAssetIds.contains(id)) {
                formAssets.add(new LinkedHashMap<>(asset));
            }
        }
        String homePageId = text(source.get("homePageId"));
        if (homePageId == null || !pageIds.contains(homePageId)) {
            homePageId = pageIds.stream().findFirst().orElse(null);
        }
        Map<String, Object> filtered = new LinkedHashMap<>(source);
        filtered.put("nodes", nodes);
        filtered.put("pages", pages);
        filtered.put("formAssets", formAssets);
        filtered.put("homePageId", homePageId);
        return filtered;
    }

    static Map<String, Object> filterPortalConfig(Map<String, Object> portalConfig, Set<String> pageIds) {
        Map<String, Object> portal = map(portalConfig);
        if (pageIds == null || pageIds.isEmpty() || portal.isEmpty()) {
            return portal;
        }
        Map<String, Object> navigation = map(portal.get("navigation"));
        if (navigation.isEmpty()) {
            return portal;
        }
        List<Object> pageOrder = new ArrayList<>();
        Object rawOrder = navigation.get("pageOrder");
        if (rawOrder instanceof List<?> list) {
            for (Object item : list) {
                String pageId = text(item);
                if (pageId != null && pageIds.contains(pageId)) {
                    pageOrder.add(pageId);
                }
            }
        }
        Map<String, Object> nextNavigation = new LinkedHashMap<>(navigation);
        nextNavigation.put("pageOrder", pageOrder);
        Map<String, Object> next = new LinkedHashMap<>(portal);
        next.put("navigation", nextNavigation);
        return next;
    }

    static List<Map<String, Object>> filterRelations(List<Map<String, Object>> relations, Set<String> objectCodes) {
        if (relations == null || relations.isEmpty() || objectCodes == null || objectCodes.isEmpty()) {
            return relations == null ? List.of() : relations;
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> relation : relations) {
            String source = text(relation.get("sourceObjectCode"));
            String target = text(relation.get("targetObjectCode"));
            if ((source == null || objectCodes.contains(source))
                    && (target == null || objectCodes.contains(target))) {
                result.add(relation);
            }
        }
        return result;
    }

    private static boolean formAssetReferenced(Map<String, Object> builder, Set<String> pageIds, String assetId) {
        Map<String, Object> pages = map(builder.get("pages"));
        for (String pageId : pageIds) {
            Set<String> ids = new LinkedHashSet<>();
            collectFormAssetIds(pages.get(pageId), ids);
            Map<String, Object> node = listPageNodes(builder).stream()
                    .filter(item -> pageId.equals(text(item.get("id"))))
                    .findFirst()
                    .orElse(null);
            collectFormAssetIds(node, ids);
            if (ids.contains(assetId)) {
                return true;
            }
        }
        return false;
    }

    private static void collectFormAssetIds(Object node, Set<String> ids) {
        if (node instanceof Map<?, ?> map) {
            Object assetId = map.get("formAssetId");
            if (assetId == null && map.get("props") instanceof Map<?, ?> props) {
                assetId = props.get("formAssetId");
            }
            String text = text(assetId);
            if (text != null) {
                ids.add(text);
            }
            for (Object value : map.values()) {
                collectFormAssetIds(value, ids);
            }
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                collectFormAssetIds(item, ids);
            }
        }
    }

    static void collectObjectCodesDeep(Object node, Set<String> codes) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT)
                        .replace("-", "").replace("_", "");
                Object value = entry.getValue();
                if (OBJECT_REF_KEYS.contains(key)) {
                    collectObjectCodesDeep(value, codes);
                    continue;
                }
                if (OBJECT_CODE_KEYS.contains(key) && value instanceof String text && StringUtils.isNotBlank(text)) {
                    codes.add(text.trim());
                    continue;
                }
                collectObjectCodesDeep(value, codes);
            }
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                collectObjectCodesDeep(item, codes);
            }
        }
    }

    private static void link(Map<String, Set<String>> adjacency, String left, String right) {
        if (StringUtils.isBlank(left) || StringUtils.isBlank(right) || Objects.equals(left, right)) {
            return;
        }
        adjacency.computeIfAbsent(left, key -> new LinkedHashSet<>()).add(right);
        adjacency.computeIfAbsent(right, key -> new LinkedHashSet<>()).add(left);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return new LinkedHashMap<>((Map<String, Object>) raw);
        }
        return new LinkedHashMap<>();
    }

    private static List<Map<String, Object>> maps(Object value) {
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

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }
}
