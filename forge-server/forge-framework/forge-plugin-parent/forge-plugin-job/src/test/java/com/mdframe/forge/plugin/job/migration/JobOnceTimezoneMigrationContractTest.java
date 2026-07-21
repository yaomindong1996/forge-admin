package com.mdframe.forge.plugin.job.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobOnceTimezoneMigrationContractTest {

    @Test
    void shouldAddScheduleFieldsAndAllowNullableCron() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("COLUMN_NAME = 'schedule_type'"));
        assertTrue(sql.contains("schedule_type varchar(20) NOT NULL DEFAULT ''CRON''"));
        assertTrue(sql.contains("COLUMN_NAME = 'fire_once_time'"));
        assertTrue(sql.contains("fire_once_time datetime NULL"));
        assertTrue(sql.contains("COLUMN_NAME = 'timezone'"));
        assertTrue(sql.contains("timezone varchar(64) NOT NULL DEFAULT ''Asia/Shanghai''"));
        assertTrue(sql.contains("cron_expression varchar(100) NULL"));
        assertTrue(sql.contains("IS_NULLABLE = 'NO'"));
    }

    @Test
    void shouldBackfillExistingJobsWithCompatibilityValues() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("schedule_type = ''CRON''"));
        assertTrue(sql.contains("timezone = ''Asia/Shanghai''"));
        assertTrue(sql.contains("WHERE schedule_type IS NULL OR TRIM(schedule_type) = ''''"));
        assertTrue(sql.contains("WHERE timezone IS NULL OR TRIM(timezone) = ''''"));
    }

    @Test
    void shouldSeedScheduleTypeAndCompletedStatusDictionaries() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("'sys_job_schedule_type'"));
        assertTrue(sql.contains("'CRON'"));
        assertTrue(sql.contains("'ONCE'"));
        assertTrue(sql.contains("'sys_job_status'"));
        assertTrue(sql.contains("'已结束'"));
        assertTrue(sql.contains("'2'"));
        assertTrue(sql.contains("tenant_id, dict_name, dict_type"));
        assertFalse(sql.contains("tenant_id = 0"));
    }

    @Test
    void shouldUseIdempotentGuards() throws IOException {
        String sql = Files.readString(resolveMigration());

        assertTrue(sql.contains("information_schema.TABLES"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("WHERE NOT EXISTS"));
    }

    private Path resolveMigration() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("db/migration/V1.0.42__add_job_once_schedule_and_timezone.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位定时任务一次性与时区迁移脚本");
    }
}
