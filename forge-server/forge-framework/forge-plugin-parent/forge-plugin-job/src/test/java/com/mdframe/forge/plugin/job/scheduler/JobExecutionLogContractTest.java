package com.mdframe.forge.plugin.job.scheduler;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobExecutionLogContractTest {

    @Test
    void shouldCarryJobConfigIdAndTriggerTypeIntoExecutionLogs() throws IOException {
        String scheduler = readMainJava("scheduler/JobScheduler.java");
        String executor = readMainJava("scheduler/QuartzJobExecutor.java");
        String lifecycle = readMainJava("service/JobExecutionLifecycleService.java");

        assertTrue(scheduler.contains("jobConfigId"));
        assertTrue(scheduler.contains("MANUAL"));
        assertTrue(executor.contains("getMergedJobDataMap"));
        assertTrue(executor.contains("SCHEDULED"));
        assertTrue(lifecycle.contains("setJobConfigId"));
        assertTrue(lifecycle.contains("setTriggerType"));
        assertTrue(executor.contains("markSuccess"));
        assertTrue(executor.contains("markFailed"));
    }

    @Test
    void shouldNotWriteRawExecutionResultToApplicationLog() throws IOException {
        String executor = readMainJava("scheduler/QuartzJobExecutor.java");

        assertFalse(executor.contains("结果: {}"));
    }

    @Test
    void shouldKeepTaskLevelRetryAsTheSingleRetryOwner() throws IOException {
        String remoteRouter = readMainJava("executor/impl/RemoteJobExecutorRouter.java");

        assertFalse(remoteRouter.contains("getRetryCount()"));
        assertFalse(remoteRouter.contains("for (int i = 0; i <= retryCount; i++)"));
    }

    private String readMainJava(String relativePath) throws IOException {
        return Files.readString(resolveModulePath(
                "src/main/java/com/mdframe/forge/plugin/job/" + relativePath));
    }

    private Path resolveModulePath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path direct = current.resolve(relativePath);
            if (Files.exists(direct)) {
                return direct;
            }
            Path nested = current.resolve(
                    "forge-server/forge-framework/forge-plugin-parent/forge-plugin-job")
                    .resolve(relativePath);
            if (Files.exists(nested)) {
                return nested;
            }
            current = current.getParent();
        }
        return Path.of(relativePath);
    }
}
