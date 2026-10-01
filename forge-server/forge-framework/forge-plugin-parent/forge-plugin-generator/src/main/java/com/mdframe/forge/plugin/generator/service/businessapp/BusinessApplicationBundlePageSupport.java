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
