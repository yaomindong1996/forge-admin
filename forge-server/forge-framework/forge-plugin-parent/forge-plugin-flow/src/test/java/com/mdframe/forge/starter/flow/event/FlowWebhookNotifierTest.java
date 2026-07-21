package com.mdframe.forge.starter.flow.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowWebhookNotifierTest {

    @Test
    void shouldUseFlowSceneWithoutInnerCallHeader() {
        SecureOutboundClient client = mock(SecureOutboundClient.class);
        when(client.execute(any())).thenReturn(new OutboundResponse(204, Map.of(), new byte[0]));
        FlowWebhookNotifier notifier = new FlowWebhookNotifier(client, objectMapper(), 0);

        notifier.notify("https://hooks.example.com/callback?secret=hidden", message());

        ArgumentCaptor<OutboundRequest> captor = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(client).execute(captor.capture());
        OutboundRequest request = captor.getValue();
        assertEquals(OutboundScenes.FLOW_API, request.getScene());
        assertEquals("POST", request.getMethod());
        assertEquals("application/json", request.getContentType());
        assertEquals("PROCESS_COMPLETED", request.getHeaders().get("X-Flow-Event-Type"));
        assertEquals("leave", request.getHeaders().get("X-Flow-Process-Key"));
        assertEquals("biz-1", request.getHeaders().get("X-Flow-Business-Key"));
        assertFalse(request.getHeaders().keySet().stream()
                .anyMatch(name -> "X-Inner-Call".equalsIgnoreCase(name)));
        assertTrue(new String(request.getBody(), java.nio.charset.StandardCharsets.UTF_8)
                .contains("\"eventType\":\"PROCESS_COMPLETED\""));
    }

    @Test
    void shouldRetryNon2xxResponseAndOnlyAcceptSuccess() {
        SecureOutboundClient client = mock(SecureOutboundClient.class);
        when(client.execute(any()))
                .thenReturn(new OutboundResponse(503, Map.of(), "unavailable".getBytes()))
                .thenReturn(new OutboundResponse(200, Map.of(), "ignored".getBytes()));
        FlowWebhookNotifier notifier = new FlowWebhookNotifier(client, objectMapper(), 0);

        notifier.notify("https://hooks.example.com/callback", message());

        verify(client, times(2)).execute(any());
    }

    private FlowEventMessage message() {
        return FlowEventMessage.builder()
                .eventType(FlowEventMessage.PROCESS_COMPLETED)
                .eventTime(LocalDateTime.of(2026, 7, 20, 12, 0))
                .processDefKey("leave")
                .businessKey("biz-1")
                .build();
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
