package com.mdframe.forge.starter.flow.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowBusinessStatsMapperSqlContractTest {

    @Test
    void processStatsMustAggregateByTenantAndDefinitionWithoutLoadingRows() throws IOException {
        String statement = loadSelectStatement("selectProcessInstanceStats");
        assertTrue(statement.contains("SUM(CASE WHEN status IN ('running', 'active')"));
        assertTrue(statement.contains("SUM(CASE WHEN end_time IS NOT NULL"));
        assertTrue(statement.contains("AVG(CASE WHEN end_time IS NOT NULL AND duration IS NOT NULL"));
        assertTrue(statement.contains("tenant_id = #{tenantId}"));
        assertTrue(statement.contains("process_def_key = #{processDefinitionKey}"));
    }

    @Test
    void monitorStatisticsMustNotFilterSysFlowTaskByUnsupportedDelFlag() throws IOException {
        String statement = loadSelectStatement("selectMonitorStatistics");
        assertTrue(statement.contains("FROM sys_flow_task overdue_task"));
        assertTrue(statement.contains("overdue_task.status IN (0, 1)"));
        assertFalse(statement.contains("del_flag"),
                "sys_flow_task 是运行态快照表，无 del_flag，监控统计禁止引用该列");
    }

    private static String loadSelectStatement(String selectId) throws IOException {
        try (InputStream input = FlowBusinessStatsMapperSqlContractTest.class.getClassLoader()
                .getResourceAsStream("mapper/FlowBusinessMapper.xml")) {
            if (input == null) {
                throw new IOException("Missing FlowBusinessMapper.xml");
            }
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            int start = xml.indexOf("<select id=\"" + selectId + "\"");
            int end = xml.indexOf("</select>", start);
            assertTrue(start >= 0 && end > start, "Missing select id=" + selectId);
            return xml.substring(start, end);
        }
    }
}
