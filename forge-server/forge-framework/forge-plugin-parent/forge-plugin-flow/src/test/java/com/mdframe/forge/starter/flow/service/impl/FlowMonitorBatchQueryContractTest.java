package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowMonitorBatchQueryContractTest {

    @Test
    void monitorPageMustLoadTaskSummariesInBatch() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowMonitorServiceImpl.java"));
        int start = source.indexOf("public FlowMonitorProcessInstancePageVO getAdminProcessInstances");
        int end = source.indexOf("public FlowMonitorProcessInstanceDetailVO getAdminProcessInstanceDetail", start);
        assertTrue(start >= 0 && end > start);
        String method = source.substring(start, end);
        assertTrue(method.contains("loadActiveTaskSummaries"));
        assertFalse(method.contains("createTaskQuery"),
                "monitor page must not issue one Flowable task query per process instance");
    }
}
