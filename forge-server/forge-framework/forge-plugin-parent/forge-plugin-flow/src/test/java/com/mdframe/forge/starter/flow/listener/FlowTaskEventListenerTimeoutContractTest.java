package com.mdframe.forge.starter.flow.listener;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTaskEventListenerTimeoutContractTest {

    @Test
    void taskCreationMustPersistConfiguredDueDate() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/listener/FlowTaskEventListener.java"));
        assertTrue(source.contains("applyConfiguredDueDate(task)"));
        assertTrue(source.contains("getTimeoutMillis(config.getId())"));
        assertTrue(source.contains("taskService.setDueDate(task.getId(), dueDate)"));
        assertTrue(source.contains("forge.flow.timeout.time-zone"));
        assertTrue(source.contains("resolveTimeoutZone"));
    }
}
