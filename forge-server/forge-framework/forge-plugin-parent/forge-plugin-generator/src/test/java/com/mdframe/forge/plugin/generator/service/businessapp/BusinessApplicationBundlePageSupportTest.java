package com.mdframe.forge.plugin.generator.service.businessapp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplication debug bundle page closure")
class BusinessApplicationBundlePageSupportTest {

    @Test
    @DisplayName("collects object codes from selected pages and form assets")
    void collectObjectCodesFromSelectedPages() {
        Map<String, Object> builder = builder(
                List.of(
                        pageNode("page_order", "object", Map.of("objectCode", "order")),
                        pageNode("page_intro", "content", null)
                ),
                Map.of(
                        "page_order", Map.of("items", List.of(
                                Map.of("blockType", "AiCrudPage", "props", Map.of("formAssetId", "form_order")))),
                        "page_intro", Map.of("items", List.of())
                ),
                List.of(Map.of(
                        "id", "form_order",
                        "objectRef", Map.of("objectCode", "order_item")
                ))
        );

        Set<String> codes = BusinessApplicationBundlePageSupport.collectObjectCodesFromPages(
                builder, Set.of("page_order"));

        assertEquals(Set.of("order", "order_item"), codes);
    }

    @Test
    @DisplayName("relation adjacency expands master-detail closure")
    void expandRelationClosure() {
        List<Map<String, Object>> snapshots = List.of(
                object("order", List.of(relation("order", "order_item"))),
                object("order_item", List.of(relation("order", "order_item"))),
                object("customer", List.of())
        );
        Map<String, Set<String>> adjacency = BusinessApplicationBundlePageSupport.buildRelationAdjacency(snapshots);
        Set<String> closure = BusinessApplicationBundlePageSupport.expandObjectClosure(Set.of("order"), adjacency);

        assertTrue(closure.contains("order"));
        assertTrue(closure.contains("order_item"));
        assertFalse(closure.contains("customer"));
    }

    @Test
    @DisplayName("filterBuilder keeps ancestors and drops unselected pages")
    @SuppressWarnings("unchecked")
    void filterBuilderKeepsAncestors() {
        Map<String, Object> builder = new LinkedHashMap<>();
        builder.put("homePageId", "page_b");
        builder.put("nodes", List.of(
                Map.of("id", "group_1", "type", "group", "title", "销售"),
                Map.of("id", "page_a", "type", "page", "parentId", "group_1", "title", "A"),
                Map.of("id", "page_b", "type", "page", "parentId", "group_1", "title", "B")
        ));
        builder.put("pages", Map.of(
                "page_a", Map.of("items", List.of()),
                "page_b", Map.of("items", List.of(Map.of("blockType", "AiCrudPage", "props", Map.of("formAssetId", "fa1"))))
        ));
        builder.put("formAssets", List.of(
                Map.of("id", "fa1", "name", "表单B"),
                Map.of("id", "fa2", "name", "表单A")
        ));

        Map<String, Object> filtered = BusinessApplicationBundlePageSupport.filterBuilder(builder, Set.of("page_b"));
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) filtered.get("nodes");
        Set<String> nodeIds = nodes.stream().map(node -> String.valueOf(node.get("id"))).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("group_1", "page_b"), nodeIds);
        assertEquals(Set.of("page_b"), ((Map<?, ?>) filtered.get("pages")).keySet());
        assertEquals("page_b", filtered.get("homePageId"));
        assertEquals(1, ((List<?>) filtered.get("formAssets")).size());
        assertEquals("fa1", ((Map<?, ?>) ((List<?>) filtered.get("formAssets")).get(0)).get("id"));
    }

    private Map<String, Object> builder(List<Map<String, Object>> nodes,
                                        Map<String, Object> pages,
                                        List<Map<String, Object>> formAssets) {
        Map<String, Object> builder = new LinkedHashMap<>();
        builder.put("nodes", nodes);
        builder.put("pages", pages);
        builder.put("formAssets", formAssets);
        return builder;
    }

    private Map<String, Object> pageNode(String id, String pageType, Map<String, Object> objectRef) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("type", "page");
        node.put("pageType", pageType);
        node.put("title", id);
        if (objectRef != null) {
            node.put("objectRef", objectRef);
        }
        return node;
    }

    private Map<String, Object> object(String code, List<Map<String, Object>> relations) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("objectCode", code);
        snapshot.put("relations", relations);
        snapshot.put("fields", List.of());
        return snapshot;
    }

    private Map<String, Object> relation(String source, String target) {
        return Map.of("sourceObjectCode", source, "targetObjectCode", target);
    }
}
