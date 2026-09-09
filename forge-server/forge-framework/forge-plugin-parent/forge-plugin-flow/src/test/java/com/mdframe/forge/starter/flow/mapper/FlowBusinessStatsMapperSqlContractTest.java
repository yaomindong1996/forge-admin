package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowBusinessStatsMapperSqlContractTest {

    @Test
    void processStatsMustAggregateByTenantAndDefinitionWithoutLoadingRows() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("mapper/FlowBusinessMapper.xml")) {
            if (input == null) {
                throw new IOException("Missing FlowBusinessMapper.xml");
            }
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            int start = xml.indexOf("<select id=\"selectProcessInstanceStats\"");
            int end = xml.indexOf("</select>", start);
            assertTrue(start >= 0 && end > start);
            String statement = xml.substring(start, end);
            assertTrue(statement.contains("SUM(CASE WHEN status IN ('running', 'active')"));
            assertTrue(statement.contains("SUM(CASE WHEN end_time IS NOT NULL"));
            assertTrue(statement.contains("AVG(CASE WHEN end_time IS NOT NULL AND duration IS NOT NULL"));
            assertTrue(statement.contains("tenant_id = #{tenantId}"));
            assertTrue(statement.contains("process_def_key = #{processDefinitionKey}"));
        }
    }
}
