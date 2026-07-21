package com.mdframe.forge.plugin.job.controller;

import com.mdframe.forge.plugin.job.vo.JobLogDetailVO;
import com.mdframe.forge.plugin.job.vo.JobLogExportVO;
import com.mdframe.forge.plugin.job.vo.JobLogVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobObservabilityApiContractTest {

    @Test
    void shouldExposeOverviewSummaryAndUnencryptedExportEndpoints() throws NoSuchMethodException {
        Method overview = JobConfigController.class.getDeclaredMethod("overview", Long.class);
        Method summary = JobMonitorController.class.getDeclaredMethod("summary");
        Method export = JobLogController.class.getDeclaredMethod(
                "export", Map.class, HttpServletResponse.class);

        assertTrue(Arrays.asList(overview.getAnnotation(GetMapping.class).value())
                .contains("/{id}/overview"));
        assertTrue(Arrays.asList(summary.getAnnotation(GetMapping.class).value())
                .contains("/summary"));
        assertTrue(Arrays.asList(export.getAnnotation(PostMapping.class).value())
                .contains("/export"));
        assertFalse(JobLogController.class.isAnnotationPresent(ApiEncrypt.class));
        assertFalse(export.isAnnotationPresent(ApiEncrypt.class));
        assertNotNull(JobLogController.class.getDeclaredMethod("page",
                com.mdframe.forge.starter.core.domain.PageQuery.class,
                com.mdframe.forge.plugin.job.dto.JobLogQuery.class).getAnnotation(ApiEncrypt.class));
    }

    @Test
    void shouldKeepSensitiveFieldsOutOfPublicAndExportViews() {
        Set<String> prohibited = Set.of("jobParam", "result", "exceptionMsg");

        assertFalse(hasAnyField(JobLogVO.class, prohibited));
        assertFalse(hasAnyField(JobLogDetailVO.class, prohibited));
        assertFalse(hasAnyField(JobLogExportVO.class, prohibited));
    }

    private boolean hasAnyField(Class<?> type, Set<String> prohibited) {
        Set<String> fields = Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        return fields.stream().anyMatch(prohibited::contains);
    }
}
