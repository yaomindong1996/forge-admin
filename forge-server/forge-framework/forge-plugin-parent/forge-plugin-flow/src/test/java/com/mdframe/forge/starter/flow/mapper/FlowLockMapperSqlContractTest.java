package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks are parsed by the tenant interceptor before reaching MySQL.  The
 * parser rewrites a LIMIT clause placed before FOR UPDATE to FOR UPDATE LIMIT,
 * which MySQL rejects, so row-lock queries must rely on their primary-key or
 * engine-identity predicates instead of appending LIMIT 1.
 */
class FlowLockMapperSqlContractTest {

    private static final Map<String, String> LOCK_STATEMENTS = Map.of(
            "mapper/FlowTaskMapper.xml", "selectByTaskIdForUpdateAndTenant",
            "mapper/FlowErrorLogMapper.xml", "selectByIdAndTenantIdForUpdate",
            "mapper/FlowFillBatchItemMapper.xml", "selectByIdForUpdate",
            "mapper/FlowBusinessMapper.xml", "selectByProcessInstanceIdAndTenantIdForUpdate"
    );

    @Test
    void lockQueriesMustEndWithForUpdateWithoutLimitClause() throws IOException {
        LOCK_STATEMENTS.forEach((resource, statementId) -> {
            try {
                String xml = resource(resource);
                String statement = statement(xml, statementId);
                assertTrue(statement.contains("FOR UPDATE"),
                        () -> resource + "#" + statementId + " must lock the selected row");
                assertFalse(statement.matches("(?s).*\\bLIMIT\\b.*\\bFOR\\s+UPDATE\\b.*"),
                        () -> resource + "#" + statementId + " must not place LIMIT before FOR UPDATE");
                assertFalse(statement.matches("(?s).*\\bFOR\\s+UPDATE\\b.*\\bLIMIT\\b.*"),
                        () -> resource + "#" + statementId + " must not place LIMIT after FOR UPDATE");
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to read " + resource, exception);
            }
        });
    }

    private static String resource(String name) throws IOException {
        try (InputStream input = FlowLockMapperSqlContractTest.class.getClassLoader()
                .getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException("Missing classpath resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String statement(String xml, String id) {
        String startToken = "<select id=\"" + id + "\"";
        int start = xml.indexOf(startToken);
        assertTrue(start >= 0, () -> "Missing mapper statement: " + id);
        int end = xml.indexOf("</select>", start);
        assertTrue(end > start, () -> "Unclosed mapper statement: " + id);
        return xml.substring(start, end);
    }
}
