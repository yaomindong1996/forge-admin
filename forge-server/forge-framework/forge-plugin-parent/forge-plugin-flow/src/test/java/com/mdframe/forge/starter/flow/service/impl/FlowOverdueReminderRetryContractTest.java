package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowOverdueReminderRetryContractTest {

    @Test
    void failedReminderRecordsMustUseBoundedBackoffAndAtomicClaim() throws IOException {
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowOverdueReminderServiceImpl.java"));
        String mapper = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/mapper/FlowOverdueReminderRecordMapper.java"));
        String xml = Files.readString(Path.of(
                "src/main/resources/mapper/FlowOverdueReminderRecordMapper.xml"));

        assertTrue(service.contains("MAX_RETRY_COUNT = 5"));
        assertTrue(service.contains("RETRY_BASE_MINUTES = 5"));
        assertTrue(service.contains("claimRetry"));
        assertTrue(service.contains("getNextRetryTime"));
        assertTrue(mapper.contains("selectByUniqueKey"));
        assertTrue(mapper.contains("claimRetry"));
        assertTrue(xml.contains("send_status = 2"));
        assertTrue(xml.contains("next_retry_time IS NULL"));
        assertTrue(xml.contains("retry_count, next_retry_time"));
        assertTrue(xml.contains("AND send_status = 1"));
        assertTrue(service.contains("latest.getNextRetryTime()"));
        assertTrue(service.contains("return task == null ? null : task.getTenantId()"));
        assertTrue(service.contains("FlowTaskStatus.isActionable"));
    }

    @Test
    void overdueReminderMigrationMustProvideIdempotencyAndRetryColumns() throws IOException {
        String migration = Files.readString(Path.of(
                "../../../db/migration/V1.0.144__add_flow_overdue_reminder_runtime.sql"));
        assertTrue(migration.contains("UNIQUE KEY uk_flow_overdue_reminder"));
        assertTrue(migration.contains("retry_count"));
        assertTrue(migration.contains("next_retry_time"));
        assertTrue(migration.contains("tenant_id = 1"));
        assertTrue(!migration.matches("(?s).*\\$\\{[^}]+}.*"));
    }

    @Test
    void timeoutCursorMigrationMustBeRepeatableAndIndexed() throws IOException {
        String migration = Files.readString(Path.of(
                "../../../db/migration/V1.0.145__add_flow_timeout_cursor_index.sql"));
        assertTrue(migration.contains("information_schema.STATISTICS"));
        assertTrue(migration.contains("idx_flow_task_timeout_cursor"));
        assertTrue(migration.contains("(due_date, id, status, tenant_id)"));
        assertTrue(!migration.matches("(?s).*\\$\\{[^}]+}.*"));
    }
}
