package com.mdframe.forge.plugin.job.registry;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobStartupRecoveryContractTest {

    @Test
    void shouldUseSingleStartupRecoveryEntry() throws Exception {
        Path module = resolveModule();

        assertFalse(Files.exists(module.resolve(
                "src/main/java/com/mdframe/forge/plugin/job/loader/JobConfigLoader.java")));
        String reconciler = Files.readString(module.resolve(
                "src/main/java/com/mdframe/forge/plugin/job/scheduler/JobStartupReconciler.java"));
        assertTrue(reconciler.contains("scheduleCoordinator.reconcileOnStartup()"));
    }

    @Test
    void shouldNotScheduleDirectlyFromAnnotationRegistrar() throws Exception {
        String registrar = Files.readString(resolveModule().resolve(
                "src/main/java/com/mdframe/forge/plugin/job/registry/JobAutoRegistrar.java"));

        assertTrue(registrar.contains("selectByJobKey(jobName, jobGroup)"));
        assertTrue(registrar.contains("JobScheduleCoordinator.SYNC_PENDING"));
        assertFalse(registrar.contains("JobScheduler"));
        assertFalse(registrar.contains("LambdaQueryWrapper"));
    }

    private Path resolveModule() {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            if (Files.exists(current.resolve("src/main/java/com/mdframe/forge/plugin/job"))) {
                return current;
            }
            Path nested = current.resolve(
                    "forge-server/forge-framework/forge-plugin-parent/forge-plugin-job");
            if (Files.exists(nested.resolve("src/main/java/com/mdframe/forge/plugin/job"))) {
                return nested;
            }
            current = current.getParent();
        }
        return Path.of("");
    }
}
