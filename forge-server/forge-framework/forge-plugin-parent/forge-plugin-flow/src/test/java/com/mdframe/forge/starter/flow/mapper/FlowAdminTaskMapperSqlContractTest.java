package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowAdminTaskMapperSqlContractTest {

    @Test
    void adminInstanceTasksMustBeTenantScopedPagedWithoutUnsupportedRuntimeDeleteColumn() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/FlowTaskMapper.xml")) {
            if (input == null) {
                throw new IOException("Missing FlowTaskMapper.xml");
            }
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            int start = xml.indexOf("<select id=\"selectAdminTasksByProcessInstance\"");
            int end = xml.indexOf("</select>", start);
            assertTrue(start >= 0 && end > start);
            String statement = xml.substring(start, end);
            assertTrue(statement.contains("t.process_instance_id = #{processInstanceId}"));
            assertTrue(statement.contains("t.tenant_id = #{tenantId}"));
            assertTrue(!statement.contains("del_flag"));
            assertTrue(statement.contains("ORDER BY t.create_time"));
        }
    }
}
