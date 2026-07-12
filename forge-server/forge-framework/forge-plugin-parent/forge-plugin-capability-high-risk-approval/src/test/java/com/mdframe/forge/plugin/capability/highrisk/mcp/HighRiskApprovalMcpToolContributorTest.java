package com.mdframe.forge.plugin.capability.highrisk.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.mcp.adapter.McpToolSchemaProjector;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class HighRiskApprovalMcpToolContributorTest {

    @Test
    void shouldContributeOneFixedReadOnlyApprovalTool() {
        var contributor = new HighRiskApprovalMcpToolContributor(
                mock(HighRiskApprovalQueryService.class), new ObjectMapper());

        var tools = contributor.contribute(mock(McpToolSchemaProjector.class));

        assertThat(tools).hasSize(1);
        var tool = tools.iterator().next().tool();
        assertThat(tool.name()).isEqualTo("capability.approval.get");
        assertThat(tool.annotations().readOnlyHint()).isTrue();
        assertThat(tool.annotations().destructiveHint()).isFalse();
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) tool.outputSchema().get("properties");
        assertThat(properties).containsKeys("approvalRequestId", "status", "resultCode", "message")
                .doesNotContainKeys("payloadCiphertext", "wrappedDek", "keyId", "processInstanceId");
    }
}
