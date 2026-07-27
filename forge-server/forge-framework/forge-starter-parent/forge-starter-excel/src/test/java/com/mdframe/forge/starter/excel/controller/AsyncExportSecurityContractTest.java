package com.mdframe.forge.starter.excel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.excel.core.DynamicExportEngine;
import com.mdframe.forge.starter.excel.model.AsyncExportTask;
import com.mdframe.forge.starter.excel.service.AsyncExportService;
import com.mdframe.forge.starter.excel.service.impl.AsyncExportServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.io.OutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncExportSecurityContractTest {

    @Test
    void failedTaskShouldStoreAndReturnOnlyStablePublicMessage() {
        String internalMessage = "jdbc:mysql://db.internal/forge?password=secret";
        DynamicExportEngine failingEngine = new DynamicExportEngine(null) {
            @Override
            public void exportToStream(OutputStream outputStream, String configKey,
                                       Map<String, Object> queryParams) {
                throw new IllegalStateException(internalMessage);
            }
        };
        AsyncExportServiceImpl service = new AsyncExportServiceImpl(failingEngine);

        String taskId = service.submitExportTask("users", Map.of(), "users.xlsx");
        AsyncExportTask storedTask = service.getTaskStatus(taskId);

        assertThat(storedTask.getStatus()).isEqualTo(2);
        assertThat(storedTask.getErrorMessage())
                .isEqualTo(AsyncExportTask.PUBLIC_FAILURE_MESSAGE)
                .doesNotContain(internalMessage);

        ExcelEnhancedController controller = new ExcelEnhancedController(null, service);
        ResponseEntity<AsyncExportTask> statusResponse = controller.getExportTaskStatus(taskId);
        ResponseEntity<Map<String, Object>> resultResponse = controller.getExportResult(taskId);

        assertThat(statusResponse.getBody()).isNotNull();
        assertThat(statusResponse.getBody().getErrorMessage()).isEqualTo(AsyncExportTask.PUBLIC_FAILURE_MESSAGE);
        assertThat(statusResponse.getBody().getFilePath()).isNull();
        assertThat(resultResponse.getBody())
                .containsEntry("errorMessage", AsyncExportTask.PUBLIC_FAILURE_MESSAGE)
                .doesNotContainValue(internalMessage);
    }

    @Test
    void publicStatusShouldReturnCopyAndSerializationShouldHideInternalPath() throws Exception {
        String internalPath = "/private/export/users.xlsx";
        AsyncExportTask internalTask = new AsyncExportTask();
        internalTask.setTaskId("task-1");
        internalTask.setStatus(1);
        internalTask.setFilePath(internalPath);

        AsyncExportService service = new AsyncExportService() {
            @Override
            public String submitExportTask(String configKey, Map<String, Object> queryParams, String fileName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public AsyncExportTask getTaskStatus(String taskId) {
                return internalTask;
            }

            @Override
            public byte[] downloadFile(String taskId) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void cleanupExpiredTasks() {
                throw new UnsupportedOperationException();
            }
        };
        ExcelEnhancedController controller = new ExcelEnhancedController(null, service);

        AsyncExportTask publicTask = controller.getExportTaskStatus("task-1").getBody();

        assertThat(publicTask).isNotNull().isNotSameAs(internalTask);
        assertThat(publicTask.getFilePath()).isNull();
        assertThat(new ObjectMapper().writeValueAsString(internalTask))
                .doesNotContain("filePath", internalPath);
    }
}
