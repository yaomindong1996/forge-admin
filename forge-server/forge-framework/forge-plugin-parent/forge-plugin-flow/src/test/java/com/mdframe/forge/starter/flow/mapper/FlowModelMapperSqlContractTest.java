package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowModelMapperSqlContractTest {

    @Test
    void modelKeyQueriesMustExplicitlyEnforceTenantAndLogicalDeleteBoundaries() throws IOException {
        String xml = resource("mapper/FlowModelMapper.xml");

        assertTenantBoundary(statement(xml, "selectByModelKeyAndTenantId"));
        assertTenantBoundary(statement(xml, "countByModelKeyAndTenantId"));
        assertTenantBoundary(statement(xml, "selectModelPage"));
        assertTenantBoundary(statement(xml, "selectStatusStatistics"));
        assertTenantBoundary(statement(xml, "selectByIdAndTenant"));
    }

    private static void assertTenantBoundary(String statement) {
        assertTrue(statement.contains("m.tenant_id = #{tenantId}"),
                "model key query must use the trusted tenant id");
        assertTrue(statement.contains("m.del_flag = 0"),
                "model key query must exclude logically deleted models");
    }

    private static String resource(String name) throws IOException {
        try (InputStream input = FlowModelMapperSqlContractTest.class.getClassLoader()
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
