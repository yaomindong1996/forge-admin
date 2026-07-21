package com.mdframe.forge.plugin.job.controller;

import com.mdframe.forge.plugin.job.service.ISysJobConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobSyncApiContractTest {

    @Test
    void shouldExposeManualSynchronizationEndpoint() throws NoSuchMethodException {
        Method controllerMethod = JobConfigController.class.getDeclaredMethod("sync", Long.class);
        PostMapping mapping = controllerMethod.getAnnotation(PostMapping.class);

        assertTrue(Arrays.asList(mapping.value()).contains("/{id}/sync"));
        ISysJobConfigService.class.getDeclaredMethod("retrySynchronization", Long.class);
    }

    @Test
    void shouldPresentUnderstandableSyncStateAndRetryAction() throws IOException {
        String page = Files.readString(resolveProjectPath(
                "forge-admin-ui/src/views/system/job-config.vue"));
        String workbench = Files.readString(resolveProjectPath(
                "forge-admin-ui/src/views/system/job-config/components/JobConfigWorkbench.vue"));
        String basicSection = Files.readString(resolveProjectPath(
                "forge-admin-ui/src/views/system/job-config/components/JobBasicSection.vue"));

        assertTrue(page.contains("sys_job_sync_status"));
        assertTrue(page.contains("调度同步"));
        assertTrue(page.contains("重新同步"));
        assertTrue(page.contains("/job/config/${row.id}/sync"));
        assertTrue(page.contains("/system/job-config/editor/${row.id}"));
        assertTrue(basicSection.contains(":disabled=\"editing\""));
        assertTrue(workbench.contains("配置已保存，调度同步失败"));
        assertTrue(workbench.contains("partialSaved"));
    }

    @Test
    void shouldUseFlatFileRoutesForWorkbench() {
        assertTrue(Files.exists(resolveProjectPath(
                "forge-admin-ui/src/views/system/job-config.editor.vue")));
        assertTrue(Files.exists(resolveProjectPath(
                "forge-admin-ui/src/views/system/job-config.editor.[id].vue")));
        assertFalse(Files.exists(resolveProjectPath(
                "forge-admin-ui/src/views/system/job-config/editor.vue")));
        assertFalse(Files.exists(resolveProjectPath(
                "forge-admin-ui/src/views/system/job-config/editor.[id].vue")));
    }

    private Path resolveProjectPath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of(relativePath);
    }
}
