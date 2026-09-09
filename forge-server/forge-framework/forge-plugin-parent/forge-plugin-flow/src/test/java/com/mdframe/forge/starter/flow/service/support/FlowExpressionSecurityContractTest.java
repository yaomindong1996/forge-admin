package com.mdframe.forge.starter.flow.service.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FlowExpressionSecurityContractTest {

    @Test
    void configurableFlowExpressionsMustUseTheRestrictedEvaluator() throws IOException {
        String nodeConfig = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowNodeConfigServiceImpl.java"));
        String conditionRule = Files.readString(Path.of(
                "src/main/java/com/mdframe/forge/starter/flow/service/impl/FlowConditionRuleServiceImpl.java"));
        assertThat(nodeConfig)
                .contains("FlowSafeExpressionEvaluator")
                .doesNotContain("StandardEvaluationContext");
        assertThat(conditionRule)
                .contains("FlowSafeExpressionEvaluator")
                .doesNotContain("StandardEvaluationContext");
    }
}
