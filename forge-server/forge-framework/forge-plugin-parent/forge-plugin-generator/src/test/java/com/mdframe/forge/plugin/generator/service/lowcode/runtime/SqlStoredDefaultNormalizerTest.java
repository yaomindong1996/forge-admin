package com.mdframe.forge.plugin.generator.service.lowcode.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SqlStoredDefaultNormalizer")
class SqlStoredDefaultNormalizerTest {

    @Test
    @DisplayName("strips PostgreSQL cast defaults")
    void stripsPgCast() {
        assertEquals("DRAFT", SqlStoredDefaultNormalizer.stripDecorators("'DRAFT'::character varying"));
        assertEquals("DRAFT", SqlStoredDefaultNormalizer.stripDecorators("'DRAFT'::text"));
        assertEquals("0", SqlStoredDefaultNormalizer.stripDecorators("'0'::bpchar"));
    }

    @Test
    @DisplayName("strips already polluted triple-quoted defaults back to semantic value")
    void stripsPollutedTripleQuotes() {
        assertEquals("DRAFT", SqlStoredDefaultNormalizer.stripDecorators("'''DRAFT''::character varying"));
        assertEquals("DRAFT", SqlStoredDefaultNormalizer.normalizeForCompare("'''DRAFT''::character varying"));
    }

    @Test
    @DisplayName("compare ignores PG cast vs plain field default")
    void compareIgnoresCast() {
        assertTrue(SqlStoredDefaultNormalizer.equalsNormalized("DRAFT", "'DRAFT'::character varying"));
        assertTrue(SqlStoredDefaultNormalizer.equalsNormalized("draft", "'draft'::character varying"));
        assertFalse(SqlStoredDefaultNormalizer.equalsNormalized("DRAFT", "'PUBLISHED'::character varying"));
    }

    @Test
    @DisplayName("prepareForDdl never re-wraps PG cast literals")
    void prepareForDdlStripsCast() {
        assertEquals("DRAFT", SqlStoredDefaultNormalizer.prepareForDdl("'DRAFT'::character varying", "varchar"));
        assertEquals(1, SqlStoredDefaultNormalizer.prepareForDdl(true, "tinyint"));
        assertEquals("CURRENT_TIMESTAMP", SqlStoredDefaultNormalizer.prepareForDdl("current_timestamp", "datetime"));
        assertNull(SqlStoredDefaultNormalizer.prepareForDdl("$forge:now", "datetime"));
    }

    @Test
    @DisplayName("type aliases unify varchar/character varying and int/integer")
    void typeAliases() {
        assertTrue(SqlStoredDefaultNormalizer.sameTypeAlias("varchar", "character varying"));
        assertTrue(SqlStoredDefaultNormalizer.sameTypeAlias("int", "integer"));
        assertTrue(SqlStoredDefaultNormalizer.sameTypeAlias("decimal", "numeric(18,2)"));
        assertEquals("varchar", SqlStoredDefaultNormalizer.normalizeTypeAlias("character varying(64)"));
    }
}
