package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Runtime database dialect default values")
class RuntimeDatabaseDialectDefaultValueTest {

    private final RuntimeDatabaseDialect dialect = new MySqlRuntimeDatabaseDialect();

    @Test
    @DisplayName("boolean switch default becomes numeric 1 for tinyint")
    void booleanTrueDefaultBecomesNumericOne() {
        String definition = dialect.columnDefinition(new RuntimeDatabaseDialect.DdlColumn(
                "field_switch",
                "tinyint",
                true,
                true,
                null,
                "开关",
                false
        ));
        assertTrue(definition.contains("DEFAULT 1"), definition);
        assertFalse(definition.contains("DEFAULT 'true'"), definition);
        assertFalse(definition.contains("DEFAULT true"), definition);
    }

    @Test
    @DisplayName("string false switch default becomes numeric 0 for tinyint")
    void stringFalseDefaultBecomesNumericZero() {
        String definition = dialect.columnDefinition(new RuntimeDatabaseDialect.DdlColumn(
                "field_switch",
                "tinyint",
                true,
                "false",
                null,
                "开关",
                false
        ));
        assertTrue(definition.contains("DEFAULT 0"), definition);
        assertFalse(definition.contains("DEFAULT 'false'"), definition);
    }
}
