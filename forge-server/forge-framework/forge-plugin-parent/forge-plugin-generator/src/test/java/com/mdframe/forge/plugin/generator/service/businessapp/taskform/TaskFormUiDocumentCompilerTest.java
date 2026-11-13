package com.mdframe.forge.plugin.generator.service.businessapp.taskform;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskFormUiDocumentCompilerTest {

    @Test
    void compilesPageSectionsAndAppliesFieldPermissions() {
        JSONObject schema = JSONObject.parseObject("""
                {
                  "formKey": "safety_form",
                  "pageSections": [
                    {
                      "sectionId": "base",
                      "sectionType": "card",
                      "title": "基本信息",
                      "fields": ["status", "wtno"]
                    }
                  ],
                  "components": [
                    {
                      "id": "c1",
                      "componentKey": "input",
                      "label": "状态",
                      "fieldBinding": { "fieldCode": "status" },
                      "validation": { "required": true }
                    },
                    {
                      "id": "c2",
                      "componentKey": "input",
                      "label": "流水号",
                      "fieldBinding": { "fieldCode": "wtno" }
                    }
                  ]
                }
                """);

        Map<String, Object> doc = TaskFormUiDocumentCompiler.compile(
                schema,
                "safety_form",
                List.of(
                        Map.of("field", "status", "label", "状态"),
                        Map.of("field", "wtno", "label", "流水号")
                ),
                List.of(
                        Map.of("field", "status", "readable", true, "writable", false),
                        Map.of("field", "wtno", "readable", true, "writable", true)
                )
        );

        assertEquals("1", doc.get("version"));
        assertEquals("business-object", doc.get("uiType"));
        assertEquals("safety_form", doc.get("formKey"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) doc.get("sections");
        assertEquals(1, sections.size());
        assertEquals("base", sections.get(0).get("sectionId"));
        assertEquals(List.of("status", "wtno"), sections.get(0).get("fields"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) doc.get("components");
        assertEquals(2, components.size());
        assertEquals("status", components.get(0).get("field"));
        assertTrue(Boolean.TRUE.equals(components.get(0).get("required")));
        assertFalse(Boolean.TRUE.equals(components.get(0).get("editable")));
        assertEquals("wtno", components.get(1).get("field"));
        assertTrue(Boolean.TRUE.equals(components.get(1).get("editable")));
    }

    @Test
    void appendsFieldsMissingFromStalePageSections() {
        JSONObject schema = JSONObject.parseObject("""
                {
                  "pageSections": [
                    { "sectionId": "section_default", "sectionType": "card", "title": "基本信息", "fields": ["fieldInput"] }
                  ],
                  "components": [
                    { "id": "c1", "componentKey": "input", "label": "报销事由", "fieldBinding": { "fieldCode": "reason" } },
                    { "id": "c2", "componentKey": "money", "label": "报销金额", "fieldBinding": { "fieldCode": "amount" } }
                  ]
                }
                """);

        Map<String, Object> doc = TaskFormUiDocumentCompiler.compile(
                schema,
                "reimburse_form",
                List.of(Map.of("field", "reason", "label", "报销事由"), Map.of("field", "amount", "label", "报销金额")),
                List.of());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) doc.get("sections");
        assertEquals(1, sections.size());
        assertEquals("section_default", sections.get(0).get("sectionId"));
        assertEquals(List.of("fieldInput", "reason", "amount"), sections.get(0).get("fields"));
    }

    @Test
    void buildsDefaultSectionWhenPageSectionsMissing() {
        JSONObject schema = JSONObject.parseObject("""
                {
                  "components": [
                    {
                      "componentKey": "input",
                      "fieldBinding": { "fieldCode": "title" },
                      "label": "标题"
                    }
                  ]
                }
                """);
        Map<String, Object> doc = TaskFormUiDocumentCompiler.compile(
                schema,
                "demo",
                List.of(Map.of("field", "title", "label", "标题")),
                List.of()
        );
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) doc.get("sections");
        assertEquals("main", sections.get(0).get("sectionId"));
        assertEquals(List.of("title"), sections.get(0).get("fields"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) doc.get("components");
        assertFalse(Boolean.TRUE.equals(components.get(0).get("editable")));
    }

    @Test
    void skipsSubTableComponentsFromMainUiDocument() {
        JSONObject schema = JSONObject.parseObject("""
                {
                  "components": [
                    {
                      "componentKey": "input",
                      "fieldBinding": { "fieldCode": "title" },
                      "label": "标题"
                    },
                    {
                      "componentKey": "subTable",
                      "label": "打卡",
                      "props": { "modelCode": "business_object_0eq3", "columns": [{ "field": "fieldSwitch" }] }
                    }
                  ]
                }
                """);
        Map<String, Object> doc = TaskFormUiDocumentCompiler.compile(
                schema,
                "demo",
                List.of(Map.of("field", "title", "label", "标题")),
                List.of()
        );
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) doc.get("components");
        assertEquals(1, components.size());
        assertEquals("title", components.get(0).get("field"));
    }
}
