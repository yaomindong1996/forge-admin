package com.mdframe.forge.starter.flow.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicFormArrayPermissionValidatorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SCHEMA = """
            [{
              "type":"group",
              "field":"expenseItems",
              "title":"费用明细",
              "props":{
                "min":1,
                "max":3,
                "rule":[
                  {"type":"input","field":"name","title":"名称","validate":[{"required":true}]},
                  {"type":"inputNumber","field":"amount","title":"金额"}
                ]
              }
            }]
            """;

    @Test
    void rejectsUnauthorizedCreate() {
        String permissions = permissions(false, true, false);
        Map<String, Object> current = Map.of("expenseItems", List.of(row("差旅", 100)));
        Map<String, Object> submitted = Map.of("expenseItems", List.of(row("差旅", 100), row("住宿", 200)));

        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current, submitted));
    }

    @Test
    void allowsWritableItemFieldButRejectsReadonlyItemField() {
        String permissions = """
                {"version":3,"fields":[
                  {"scope":"main","field":"expenseItems","readable":true,"writable":true},
                  {"scope":"array","arrayKey":"expenseItems","itemField":"name","field":"name","readable":true,"writable":false},
                  {"scope":"array","arrayKey":"expenseItems","itemField":"amount","field":"amount","readable":true,"writable":true}
                ],"arrays":[{"arrayKey":"expenseItems","allowCreate":false,"allowUpdate":true,"allowDelete":false}]}
                """;
        Map<String, Object> current = Map.of("expenseItems", List.of(row("差旅", 100)));

        assertDoesNotThrow(() -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of(row("差旅", 200)))));
        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of(row("住宿", 100)))));
    }

    @Test
    void supportsAuthorizedMiddleDeleteWithProtectedFields() {
        String permissions = """
                {"version":3,"fields":[
                  {"scope":"main","field":"expenseItems","readable":true,"writable":true},
                  {"scope":"array","arrayKey":"expenseItems","itemField":"name","field":"name","readable":true,"writable":false},
                  {"scope":"array","arrayKey":"expenseItems","itemField":"amount","field":"amount","readable":true,"writable":true}
                ],"arrays":[{"arrayKey":"expenseItems","allowCreate":false,"allowUpdate":true,"allowDelete":true}]}
                """;
        Map<String, Object> current = Map.of("expenseItems", List.of(
                row("差旅", 100),
                row("住宿", 200),
                row("餐费", 300)));
        Map<String, Object> submitted = Map.of("expenseItems", List.of(
                row("差旅", 150),
                row("餐费", 350)));

        assertDoesNotThrow(() -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current, submitted));
    }

    @Test
    void rejectsNonObjectRowsAndSchemaLimitViolations() {
        String permissions = permissions(true, true, true);
        Map<String, Object> current = Map.of("expenseItems", List.of(row("差旅", 100)));

        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of("invalid"))));
        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of())));
    }

    @Test
    void rejectsCreatedRowsMissingRequiredFields() {
        // v3 节点权限覆盖设计态 schema，必填场景必须显式携带节点 required=true。
        String permissions = permissions(true, true, false);
        Map<String, Object> current = Map.of("expenseItems", List.of(row("差旅", 100)));
        Map<String, Object> invalidRow = Map.of("amount", 200);

        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of(row("差旅", 100), invalidRow))));
    }

    @Test
    void preservesSchemaRequiredValidationWithoutNodePermissions() {
        Map<String, Object> current = Map.of("expenseItems", List.of(row("差旅", 100)));
        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, "{}", current,
                Map.of("expenseItems", List.of(row("差旅", 100), Map.of("amount", 200)))));
    }

    @Test
    void honorsNodePermissionThatExplicitlyMakesSchemaRequiredFieldOptional() {
        String permissions = permissions(true, true, false).replace("\"required\":true", "\"required\":false");
        Map<String, Object> current = Map.of("expenseItems", List.of(row("差旅", 100)));
        assertDoesNotThrow(() -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of(row("差旅", 100), Map.of("amount", 200)))));
    }

    @Test
    void rejectsChangesWhenArrayIsUnreadableOrItemPermissionIsMissing() {
        Map<String, Object> current = Map.of("expenseItems", List.of(row("差旅", 100)));
        String unreadable = """
                {"version":3,"fields":[
                  {"scope":"main","field":"expenseItems","readable":true,"writable":true},
                  {"scope":"array","arrayKey":"expenseItems","itemField":"amount","field":"amount","readable":true,"writable":true}
                ],"arrays":[{"arrayKey":"expenseItems","readable":false,"allowCreate":true,"allowUpdate":true,"allowDelete":true}]}
                """;
        String missingItemPermission = """
                {"version":3,"fields":[
                  {"scope":"main","field":"expenseItems","readable":true,"writable":true},
                  {"scope":"array","arrayKey":"expenseItems","itemField":"amount","field":"amount","readable":true,"writable":true}
                ],"arrays":[{"arrayKey":"expenseItems","readable":true,"allowCreate":false,"allowUpdate":true,"allowDelete":false}]}
                """;

        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, unreadable, current,
                Map.of("expenseItems", List.of(row("差旅", 200)))));
        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, missingItemPermission, current,
                Map.of("expenseItems", List.of(row("住宿", 100)))));
    }

    @Test
    void preservesUnknownLegacyFieldsButRejectsTamperingAndNewUnknownFields() {
        String permissions = permissions(true, true, false);
        Map<String, Object> legacyRow = new LinkedHashMap<>(row("差旅", 100));
        legacyRow.put("legacyCode", "A1");
        Map<String, Object> current = Map.of("expenseItems", List.of(legacyRow));

        assertDoesNotThrow(() -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of(legacyRow))));

        Map<String, Object> tampered = new LinkedHashMap<>(legacyRow);
        tampered.put("legacyCode", "B2");
        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of(tampered))));

        Map<String, Object> created = new LinkedHashMap<>(row("住宿", 200));
        created.put("unknown", "forged");
        assertThrows(IllegalArgumentException.class, () -> DynamicFormArrayPermissionValidator.validate(
                OBJECT_MAPPER, SCHEMA, permissions, current,
                Map.of("expenseItems", List.of(legacyRow, created))));
    }

    private String permissions(boolean allowCreate, boolean allowUpdate, boolean allowDelete) {
        return """
                {"version":3,"fields":[
                  {"scope":"main","field":"expenseItems","readable":true,"writable":true},
                  {"scope":"array","arrayKey":"expenseItems","itemField":"name","field":"name",
                   "readable":true,"writable":true,"required":true},
                  {"scope":"array","arrayKey":"expenseItems","itemField":"amount","field":"amount","readable":true,"writable":true}
                ],"arrays":[{"arrayKey":"expenseItems","allowCreate":%s,"allowUpdate":%s,"allowDelete":%s}]}
                """.formatted(allowCreate, allowUpdate, allowDelete);
    }

    private Map<String, Object> row(String name, int amount) {
        return Map.of("name", name, "amount", amount);
    }
}
