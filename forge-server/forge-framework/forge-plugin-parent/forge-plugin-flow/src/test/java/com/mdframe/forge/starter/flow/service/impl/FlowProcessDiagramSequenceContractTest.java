package com.mdframe.forge.starter.flow.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowProcessDiagramSequenceContractTest {

    @Test
    void diagramResponseMustExposeSequenceFlowStatusAndDegradedReason() throws IOException {
        String dto = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/dto/ProcessDiagramInfo.java"));
        String sequenceDto = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/dto/ProcessSequenceFlowInfo.java"));
        String service = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowTaskServiceImpl.java"));
        assertTrue(dto.contains("sequenceFlows"));
        assertTrue(dto.contains("sequenceFlowStatusAvailable"));
        assertTrue(dto.contains("sequenceFlowStatusMessage"));
        assertTrue(sequenceDto.contains("sourceRef"));
        assertTrue(sequenceDto.contains("targetRef"));
        assertTrue(service.contains("findFlowElementsOfType(SequenceFlow.class)"));
        assertTrue(service.contains("getActivityType()"));
        assertTrue(service.contains("sequenceFlow 活动，连线状态不可用"));
    }
}
