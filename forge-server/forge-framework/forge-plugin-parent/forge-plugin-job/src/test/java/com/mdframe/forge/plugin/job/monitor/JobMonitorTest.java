package com.mdframe.forge.plugin.job.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.model.JobLog;
import com.mdframe.forge.plugin.job.spi.IJobLogStorage;
import com.mdframe.forge.plugin.job.support.JobLogSanitizer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JobMonitorTest {

    @Test
    void shouldStoreSanitizedLogWithStableJobReference() {
        CapturingLogStorage storage = new CapturingLogStorage();
        JobMonitor monitor = new JobMonitor(storage, new JobLogSanitizer(new ObjectMapper()));
        LocalDateTime startTime = LocalDateTime.of(2026, 7, 19, 20, 0);
        LocalDateTime endTime = startTime.plusSeconds(2);

        monitor.recordLog(7L, "MANUAL", "inventoryClose", "BUSINESS", "inventoryHandler",
                "{\"token\":\"secret-token\",\"mobile\":\"13800138000\"}",
                Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant()),
                startTime, endTime, 0, "Authorization: Bearer result-token",
                new IllegalStateException("password=raw-password"));

        JobLog saved = storage.saved;
        assertNotNull(saved);
        assertEquals(7L, saved.getJobConfigId());
        assertEquals("MANUAL", saved.getTriggerType());
        assertEquals(2000L, saved.getDuration());
        assertFalse(saved.getJobParam().contains("secret-token"));
        assertFalse(saved.getJobParam().contains("13800138000"));
        assertFalse(saved.getResult().contains("result-token"));
        assertFalse(saved.getExceptionMsg().contains("raw-password"));
    }

    private static final class CapturingLogStorage implements IJobLogStorage {

        private JobLog saved;

        @Override
        public void saveLog(JobLog log) {
            this.saved = log;
        }

        @Override
        public JobLog getLog(Long logId) {
            return saved;
        }
    }
}
