package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowTaskStatusTransitionContractTest {

    @Test
    void processTerminationMustRepairAllDeletedActiveTasksToTerminated() throws IOException {
        String mapper = Files.readString(Path.of(
                "src/main/resources/mapper/FlowTaskMapper.xml"));
        String taskService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        String instanceService = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowInstanceServiceImpl.java"));

        assertTrue(mapper.contains("id=\"updateProcessTaskStatusByTaskIds\""));
        assertTrue(mapper.contains("task_id IN"));
        assertTrue(mapper.contains("status IN (0, 1, 5)"));
        assertTrue(!mapper.contains("t.del_flag") && !mapper.contains("update_time = #{completeTime}"));
        assertTrue(taskService.contains("baseMapper.updateProcessTaskStatusByTaskIds"));
        assertTrue(taskService.contains("FlowTaskStatus.TERMINATED.getCode()"));
        assertTrue(taskService.contains("FlowTaskStatus.WITHDRAWN.getCode()"));
        assertTrue(instanceService.contains("flowTaskMapper.updateProcessTaskStatusByTaskIds"));
        assertTrue(instanceService.contains("FlowTaskStatus.TERMINATED.getCode()"));
    }
}
