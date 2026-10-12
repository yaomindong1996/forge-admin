package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentHttpToolTest {

    private final SecureOutboundClient client = mock(SecureOutboundClient.class);
    private final HttpTool tool = new HttpTool(client);

    @Test
    void should_useGuardedSceneAndDeadline_when_validRequestIsExecuted() {
        when(client.execute(any())).thenReturn(new OutboundResponse(200, Map.of(),
                "fixture-data".getBytes(StandardCharsets.UTF_8)));

        var result = tool.execute(Map.of("url", "https://fixture.invalid/api", "method", "POST",
                "headers", Map.of("X-Fixture", "value"), "body", "{}"), null);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<OutboundRequest> request = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(client).execute(request.capture());
        assertThat(request.getValue().getScene()).isEqualTo(OutboundScenes.EXTERNAL_CONNECTOR);
        assertThat(request.getValue().getCallTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(request.getValue().getHeaders()).containsExactlyEntriesOf(Map.of("X-Fixture", "value"));
        assertThat(request.getValue().getBody()).isEqualTo("{}".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void should_notReturnRawErrorOrResponse_when_guardOrUpstreamFails() {
        when(client.execute(any())).thenThrow(new IllegalStateException("fixture-secret-diagnostic"));
        assertThat(tool.execute(Map.of("url", "https://fixture.invalid/api"), null).getError())
                .doesNotContain("fixture-secret-diagnostic");
        doReturn(new OutboundResponse(500, Map.of(), "fixture-sensitive-body".getBytes(StandardCharsets.UTF_8)))
                .when(client).execute(any());
        var failed = tool.execute(Map.of("url", "https://fixture.invalid/api"), null);
        assertThat(failed.isSuccess()).isFalse();
        assertThat(failed.getError()).doesNotContain("fixture-sensitive-body");
    }

    @Test
    void should_failBeforeOutboundAccess_when_methodOrHeaderShapeIsUnsupported() {
        assertThat(tool.execute(Map.of("url", "https://fixture.invalid", "method", "DELETE"), null).isSuccess())
                .isFalse();
        assertThat(tool.execute(Map.of("url", "https://fixture.invalid", "headers", Map.of("x", 1)), null).isSuccess())
                .isFalse();
        verifyNoInteractions(client);
    }
}
