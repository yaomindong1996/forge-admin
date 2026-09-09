package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTimeoutServiceQueryContractTest {

    @Test
    void timeoutScanMustUseStableLocalCursorAndTenantContext() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTimeoutServiceImpl.java"));
        assertTrue(source.contains("selectTimeoutCandidates"));
        assertTrue(source.contains("cursorDueDate"));
        assertTrue(source.contains("cursorId"));
        assertTrue(source.contains("TenantContextHolder.executeWithTenant"));
        assertTrue(source.contains("taskId(localTask.getTaskId()).active()"));
        assertTrue(source.contains("backfillMissingDueDates(scanTime)"));
        assertTrue(source.contains("updateDueDateByTaskIdAndTenant"));
        assertTrue(source.contains("forge.flow.timeout.time-zone"));
        assertTrue(source.contains("StringRedisTemplate"));
        assertTrue(source.contains("TIMEOUT_SCAN_LOCK_KEY"));
        assertTrue(source.contains("setIfAbsent(TIMEOUT_SCAN_LOCK_KEY"));
        assertTrue(source.contains("localScanLock.tryLock()"));
        assertTrue(source.contains("RELEASE_SCAN_LOCK_SCRIPT"));
        assertTrue(source.contains("RENEW_SCAN_LOCK_SCRIPT"));
        assertTrue(source.contains("renewScanLock(lease)"));
        assertFalse(source.contains("opsForValue().get(TIMEOUT_SCAN_LOCK_KEY)"));
        assertFalse(source.contains("stringRedisTemplate.delete(TIMEOUT_SCAN_LOCK_KEY)"));
    }

    @Test
    void timeoutCandidateMapperMustOrderByDueDateAndId() throws IOException {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/FlowTaskMapper.xml"));
        int start = xml.indexOf("<select id=\"selectTimeoutCandidates\"");
        int end = xml.indexOf("</select>", start);
        assertTrue(start >= 0 && end > start);
        String statement = xml.substring(start, end);
        assertTrue(statement.contains("t.due_date &lt;= #{now}"));
        assertTrue(statement.contains("t.due_date &gt; #{cursorDueDate}"));
        assertTrue(statement.contains("t.id &gt; #{cursorId}"));
        assertTrue(statement.contains("ORDER BY t.due_date ASC, t.id ASC"));
        assertTrue(statement.contains("LIMIT #{limit}"));
    }

    @Test
    void dueDateBackfillMapperMustUseCreateTimeCursor() throws IOException {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/FlowTaskMapper.xml"));
        int start = xml.indexOf("<select id=\"selectDueDateBackfillCandidates\"");
        int end = xml.indexOf("</select>", start);
        assertTrue(start >= 0 && end > start);
        String statement = xml.substring(start, end);
        assertTrue(statement.contains("t.due_date IS NULL"));
        assertTrue(statement.contains("t.create_time &gt; #{cursorCreateTime}"));
        assertTrue(statement.contains("ORDER BY t.create_time ASC, t.id ASC"));
        assertTrue(statement.contains("LIMIT #{limit}"));
    }

    @Test
    void upcomingTimeoutQueryMustUseDueDateWindow() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTimeoutServiceImpl.java"));
        assertTrue(source.contains(".taskDueAfter(now)"));
        assertTrue(source.contains(".taskDueBefore(deadline)"));
        assertTrue(source.contains("safeAdvanceMinutes"));
    }

    @Test
    void candidateTasksMustUseMapperPaging() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        int start = source.indexOf("public IPage<FlowTask> candidateTasks");
        int end = source.indexOf("private IPage<FlowTask> enrichTaskPage", start);
        assertTrue(start >= 0 && end > start);
        String method = source.substring(start, end);
        assertTrue(method.contains("selectCandidateTasks"));
        assertFalse(method.contains(".list()"));
        assertFalse(method.contains("createTaskQuery"));
    }

    @Test
    void overdueScanMustUseTheRuntimeTaskSchema() throws IOException {
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/FlowTaskMapper.xml"));
        int start = xml.indexOf("<select id=\"selectOverduePendingTasks\"");
        int end = xml.indexOf("</select>", start);
        assertTrue(start >= 0 && end > start);
        String statement = xml.substring(start, end);
        assertFalse(statement.contains("del_flag"));
        assertTrue(statement.contains("due_date IS NOT NULL"));
    }
}
