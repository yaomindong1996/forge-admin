package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowStartPreviewContractTest {

    @Test
    void startConfigMustExposeNextApprovalStrategiesFromBpmn() throws IOException {
        String support = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/InitiatorSelectedApproverSupport.java"));
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowModelServiceImpl.java"));
        String dto = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/dto/FlowStartConfig.java"));

        assertTrue(support.contains("discoverNextNodes"));
        assertTrue(support.contains("findFlowElementsOfType(StartEvent.class)"));
        assertTrue(support.contains("instanceof UserTask"));
        assertTrue(support.contains("candidateUsers"));
        assertTrue(support.contains("candidateGroups"));
        assertTrue(service.contains("config.setNextNodes"));
        assertTrue(service.contains("preflightNextApprovers"));
        assertTrue(service.contains("appendCandidateGroupDiagnostics"));
        assertTrue(service.contains("getUserIdsByGroupCode(group)"));
        assertTrue(service.contains("当前未解析到启用成员"));
        assertTrue(service.contains("config.setPreflightPassed"));
        assertTrue(dto.contains("private List<ApproverNode> nextNodes"));
        assertTrue(dto.contains("private Boolean preflightPassed"));
        assertTrue(dto.contains("private List<String> diagnostics"));
    }

    @Test
    void modelPageMustCapRequestedPageSize() throws IOException {
        String controller = Files.readString(Path.of(
                "../../../forge-flow/forge-flow-server/src/main/java/com/mdframe/forge/flow/controller/FlowModelController.java"));
        assertTrue(controller.contains("Math.min(pageSize, 100)"));
    }
}
