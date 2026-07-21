package com.mdframe.forge.plugin.job.executor.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.config.JobProperties;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteJobExecutorRouterTest {

    private static final String TOKEN = "job-executor-token-32-characters-minimum";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUseControlledJobRpcSceneAndReturnResponseData() throws Exception {
        SecureOutboundClient client = mock(SecureOutboundClient.class);
        when(client.execute(any())).thenReturn(response(200,
                "{\"code\":200,\"message\":\"操作成功\",\"data\":\"completed\"}"));
        RemoteJobExecutorRouter router = router(client, TOKEN);

        String result = router.route("RPC", null, null,
                "sampleHandler", "executor.internal:8580", "{\"value\":1}");

        assertEquals("completed", result);
        ArgumentCaptor<OutboundRequest> captor = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(client).execute(captor.capture());
        OutboundRequest request = captor.getValue();
        assertEquals(OutboundScenes.JOB_RPC, request.getScene());
        assertEquals("http://executor.internal:8580/job/executor/execute", request.getUrl());
        assertEquals("Bearer " + TOKEN, request.getHeaders().get("Authorization"));
        assertEquals("application/json", request.getContentType());
        JsonNode body = objectMapper.readTree(request.getBody());
        assertEquals("sampleHandler", body.get("handlerName").asText());
        assertEquals("{\"value\":1}", body.get("param").asText());
    }

    @Test
    void shouldRejectBusinessErrorInsideSuccessfulHttpResponse() {
        SecureOutboundClient client = mock(SecureOutboundClient.class);
        when(client.execute(any())).thenReturn(response(200,
                "{\"code\":500,\"message\":\"任务执行失败\"}"));
        RemoteJobExecutorRouter router = router(client, TOKEN);

        RuntimeException error = assertThrows(RuntimeException.class, () -> router.route(
                "RPC", null, null, "sampleHandler", "executor.internal", null));

        assertTrue(error.getMessage().contains("任务执行失败"));
    }

    @Test
    void shouldRejectNonSuccessfulHttpResponse() {
        SecureOutboundClient client = mock(SecureOutboundClient.class);
        when(client.execute(any())).thenReturn(response(401,
                "{\"code\":401,\"message\":\"执行器认证失败\"}"));
        RemoteJobExecutorRouter router = router(client, TOKEN);

        RuntimeException error = assertThrows(RuntimeException.class, () -> router.route(
                "RPC", null, null, "sampleHandler", "executor.internal", null));

        assertTrue(error.getMessage().contains("401"));
    }

    @Test
    void shouldRejectMalformedSuccessfulResponse() {
        SecureOutboundClient client = mock(SecureOutboundClient.class);
        when(client.execute(any())).thenReturn(response(200, "not-json"));
        RemoteJobExecutorRouter router = router(client, TOKEN);

        RuntimeException error = assertThrows(RuntimeException.class, () -> router.route(
                "RPC", null, null, "sampleHandler", "executor.internal", null));

        assertTrue(error.getMessage().contains("响应格式"));
    }

    @Test
    void shouldFailClosedWhenServiceTokenIsTooShort() {
        SecureOutboundClient client = mock(SecureOutboundClient.class);
        RemoteJobExecutorRouter router = router(client, "short-token");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> router.route(
                "RPC", null, null, "sampleHandler", "executor.internal", null));

        assertTrue(error.getMessage().contains("至少32个字符"));
    }

    private RemoteJobExecutorRouter router(SecureOutboundClient client, String token) {
        JobProperties properties = new JobProperties();
        properties.setExecutorToken(token);
        return new RemoteJobExecutorRouter(properties, client, objectMapper);
    }

    private OutboundResponse response(int status, String body) {
        return new OutboundResponse(status, Map.of(), body.getBytes(StandardCharsets.UTF_8));
    }
}

