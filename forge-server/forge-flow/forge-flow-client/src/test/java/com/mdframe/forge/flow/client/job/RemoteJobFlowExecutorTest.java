package com.mdframe.forge.flow.client.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.flow.client.FlowClientException;
import com.mdframe.forge.flow.client.FlowResult;
import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionRequest;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionResult;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteJobFlowExecutorTest {

    @Mock
    private SecureOutboundClient outboundClient;

    private ObjectMapper objectMapper;
    private RemoteJobFlowExecutor executor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        JobFlowRemoteProperties properties = new JobFlowRemoteProperties();
        properties.setEnabled(true);
        properties.setUrl("http://flow.internal:8581/");
        properties.setToken("service-token");
        executor = new RemoteJobFlowExecutor(outboundClient, objectMapper, properties);
    }

    @Test
    void shouldValidateBindingThroughFlowApiSceneWithoutIdentityFields() throws Exception {
        JobFlowBindingSnapshot binding = binding();
        when(outboundClient.execute(any())).thenReturn(success(binding));

        JobFlowBindingSnapshot result = executor.validateBinding("daily-settlement", 7);

        assertEquals(binding, result);
        ArgumentCaptor<OutboundRequest> request = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(outboundClient).execute(request.capture());
        assertEquals(OutboundScenes.FLOW_API, request.getValue().getScene());
        assertEquals("POST", request.getValue().getMethod());
        assertEquals("Bearer service-token", request.getValue().getHeaders().get("Authorization"));
        assertFalse(request.getValue().getHeaders().containsKey("X-Inner-Call"));
        JsonNode body = objectMapper.readTree(request.getValue().getBody());
        assertEquals("daily-settlement", body.get("modelKey").asText());
        assertEquals(7, body.get("modelVersion").asInt());
        assertFalse(body.has("userId"));
        assertFalse(body.has("tenantId"));
        assertFalse(body.has("activeOrgId"));
    }

    @Test
    void shouldStartWithNestedJobInputAndNoTopLevelIdentity() throws Exception {
        JobFlowExecutionRequest execution = execution();
        JobFlowExecutionResult expected = new JobFlowExecutionResult(
                execution.businessKey(), "process-1", false);
        when(outboundClient.execute(any())).thenReturn(success(expected));

        JobFlowExecutionResult result = executor.start(execution);

        assertEquals(expected, result);
        ArgumentCaptor<OutboundRequest> request = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(outboundClient).execute(request.capture());
        JsonNode body = objectMapper.readTree(request.getValue().getBody());
        assertTrue(body.has("jobInput"));
        assertEquals(1000, body.get("jobInput").get("amount").asInt());
        assertFalse(body.has("userId"));
        assertFalse(body.has("tenantId"));
        assertFalse(body.has("activeOrgId"));
    }

    @Test
    void shouldRecoverOriginalProcessByBusinessKeyAfterServerFailure() throws Exception {
        JobFlowExecutionRequest execution = execution();
        JobFlowExecutionResult recovered = new JobFlowExecutionResult(
                execution.businessKey(), "process-existing", true);
        when(outboundClient.execute(any()))
                .thenReturn(response(503, "unavailable"))
                .thenReturn(success(recovered));

        JobFlowExecutionResult result = executor.start(execution);

        assertEquals("process-existing", result.processInstanceId());
        assertTrue(result.recovered());
        verify(outboundClient, times(2)).execute(any());
    }

    @Test
    void shouldRecoverOriginalProcessByBusinessKeyAfterMalformedResponse() throws Exception {
        JobFlowExecutionRequest execution = execution();
        JobFlowExecutionResult recovered = new JobFlowExecutionResult(
                execution.businessKey(), "process-existing", true);
        when(outboundClient.execute(any()))
                .thenReturn(response(200, "not-json"))
                .thenReturn(success(recovered));

        JobFlowExecutionResult result = executor.start(execution);

        assertEquals("process-existing", result.processInstanceId());
        assertTrue(result.recovered());
        verify(outboundClient, times(2)).execute(any());
    }

    @Test
    void shouldRejectClientFailureWithoutRecoveryQuery() {
        when(outboundClient.execute(any())).thenReturn(response(400, "bad request"));

        FlowClientException error = assertThrows(FlowClientException.class,
                () -> executor.start(execution()));

        assertTrue(error.getMessage().contains("400"));
        verify(outboundClient).execute(any());
    }

    @Test
    void shouldRejectExplicitBusinessFailureWithoutRecoveryQuery() throws Exception {
        when(outboundClient.execute(any())).thenReturn(new OutboundResponse(
                200, Map.of(), objectMapper.writeValueAsBytes(FlowResult.error("rejected"))));

        FlowClientException error = assertThrows(FlowClientException.class,
                () -> executor.start(execution()));

        assertTrue(error.getMessage().contains("业务调用失败"));
        verify(outboundClient).execute(any());
    }

    @Test
    void shouldRecoverOriginalProcessByBusinessKeyAfterTransportFailure() throws Exception {
        JobFlowExecutionRequest execution = execution();
        JobFlowExecutionResult recovered = new JobFlowExecutionResult(
                execution.businessKey(), "process-existing", true);
        when(outboundClient.execute(any()))
                .thenThrow(new IllegalStateException("timeout"))
                .thenReturn(success(recovered));

        JobFlowExecutionResult result = executor.start(execution);

        assertEquals("process-existing", result.processInstanceId());
        assertTrue(result.recovered());
        ArgumentCaptor<OutboundRequest> requests = ArgumentCaptor.forClass(OutboundRequest.class);
        verify(outboundClient, times(2)).execute(requests.capture());
        assertTrue(requests.getAllValues().get(0).getUrl().endsWith("/api/flow/job/executions/start"));
        assertTrue(requests.getAllValues().get(1).getUrl()
                .contains("/api/flow/job/executions/status?businessKey=job%3A11%3A22"));
    }

    @Test
    void shouldFailClosedWhenRecoveryCannotFindProcess() throws Exception {
        when(outboundClient.execute(any()))
                .thenThrow(new IllegalStateException("timeout"))
                .thenReturn(success(null));

        FlowClientException error = assertThrows(FlowClientException.class,
                () -> executor.start(execution()));

        assertTrue(error.getMessage().contains("状态未知"));
    }

    @Test
    void shouldRequireServerConfiguredToken() {
        JobFlowRemoteProperties properties = new JobFlowRemoteProperties();
        properties.setEnabled(true);
        properties.setUrl("http://flow.internal:8581");
        RemoteJobFlowExecutor missingToken = new RemoteJobFlowExecutor(
                outboundClient, objectMapper, properties);

        assertThrows(FlowClientException.class,
                () -> missingToken.validateBinding("daily-settlement", 7));
    }

    private OutboundResponse success(Object data) throws Exception {
        return new OutboundResponse(200, Map.of(),
                objectMapper.writeValueAsBytes(FlowResult.success(data)));
    }

    private OutboundResponse response(int statusCode, String body) {
        return new OutboundResponse(statusCode, Map.of(), body.getBytes(StandardCharsets.UTF_8));
    }

    private JobFlowBindingSnapshot binding() {
        return new JobFlowBindingSnapshot(
                "daily-settlement", 7, "deployment-7", "daily-settlement:7:definition-7");
    }

    private JobFlowExecutionRequest execution() {
        return new JobFlowExecutionRequest(
                11L, 22L, "job:11:22", binding(), Map.of("amount", 1000));
    }
}
