package com.mdframe.forge.flow.controller;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowNodeConfigControllerContractTest {

    @Test
    void approverCalculationMustUseTypedRequestWithDynamicVariablesField() throws IOException {
        String controller = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/flow/controller/FlowNodeConfigController.java"));
        String dto = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/dto/FlowApproverCalculationDTO.java"));
        String service = Files.readString(Path.of(
                "../../forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/"
                        + "com/mdframe/forge/starter/flow/service/impl/FlowNodeConfigServiceImpl.java"));

        assertThat(controller).contains("@RequestBody FlowApproverCalculationDTO request",
                "request.resolveVariables()").doesNotContain("@RequestBody Map<String, Object> variables");
        assertThat(dto).contains("Map<String, Object> variables", "resolveVariables()");
        assertThat(service).doesNotContain("variables={}").doesNotContain("userIds={}");
    }
}
