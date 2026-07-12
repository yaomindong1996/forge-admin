package com.mdframe.forge.flow.client;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlowClientDelegatedIdentityTest {

    @Test
    void shouldStartDelegatedFlowWithoutClientSuppliedUserFields() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.exchange(
                eq("http://flow/api/flow/instance/start-delegated/order_approval"),
                eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(
                        "{\"code\":200,\"msg\":\"ok\",\"data\":\"process-1\"}",
                        HttpStatus.OK));
        FlowClient client = new FlowClient(restTemplate, "http://flow", "static-token");
        client.setTokenProvider(() -> "delegated-token");

        FlowResult<String> result = client.startProcessForDelegatedUser(
                "order_approval", "order:1001", "order", "采购审批", Map.of("amount", 100));

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<HttpEntity<String>> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(any(String.class), eq(HttpMethod.POST), entity.capture(), eq(String.class));
        assertThat(entity.getValue().getHeaders().getFirst("Authorization"))
                .isEqualTo("Bearer delegated-token");
        assertThat(entity.getValue().getBody()).contains("\"businessKey\":\"order:1001\"")
                .doesNotContain("userId", "userName", "deptId", "deptName");
    }

    @Test
    void shouldNotFallbackToStaticTokenWhenDelegatedIdentityIssuanceFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        FlowClient client = new FlowClient(restTemplate, "http://flow", "static-token");
        client.setTokenProvider(() -> {
            throw new FlowTokenAcquisitionException("delegation unavailable", null);
        });

        assertThatThrownBy(() -> client.startProcessForDelegatedUser(
                "order_approval", "order:1001", "order", "采购审批", Map.of()))
                .isInstanceOf(FlowClientException.class)
                .hasMessageContaining("获取流程调用身份失败");
        verify(restTemplate, never()).exchange(any(String.class), any(), any(), eq(String.class));
    }
}
