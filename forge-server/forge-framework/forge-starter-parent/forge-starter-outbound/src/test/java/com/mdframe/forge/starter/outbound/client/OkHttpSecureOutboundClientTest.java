package com.mdframe.forge.starter.outbound.client;

import com.mdframe.forge.starter.outbound.config.OutboundProperties;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import com.mdframe.forge.starter.outbound.model.ValidatedOutboundTarget;
import com.mdframe.forge.starter.outbound.security.IpAddressClassifier;
import com.mdframe.forge.starter.outbound.security.OutboundPolicyService;
import com.mdframe.forge.starter.outbound.security.OutboundSecurityException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OkHttpSecureOutboundClientTest {

    private MockWebServer server;
    private OutboundPolicyService policyService;
    private OutboundProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        policyService = mock(OutboundPolicyService.class);
        properties = new OutboundProperties();
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        properties.setWriteTimeout(Duration.ofSeconds(1));
        properties.setCallTimeout(Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void shouldExecuteWithValidatedConnectionDnsAndBoundedResponse() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("accepted"));
        ValidatedOutboundTarget target = target("allowed.example", "/notify");
        when(policyService.validate(any())).thenReturn(target);
        OkHttpSecureOutboundClient client = new OkHttpSecureOutboundClient(policyService, properties);

        OutboundResponse response = client.execute(OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url(target.getUri().toString())
                .method("POST")
                .headers(Map.of("X-Event-Id", "evt-1"))
                .contentType("application/json")
                .body("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .build());

        assertEquals(200, response.getStatusCode());
        assertEquals("accepted", response.bodyAsUtf8());
        RecordedRequest recorded = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("POST", recorded.getMethod());
        assertEquals("evt-1", recorded.getHeader("X-Event-Id"));
        assertEquals("{}", recorded.getBody().readUtf8());
        verify(policyService, org.mockito.Mockito.times(2)).validate(any());
    }

    @Test
    void shouldRejectDangerousHeadersAndOversizedRequestBeforeNetwork() {
        OkHttpSecureOutboundClient client = new OkHttpSecureOutboundClient(policyService, properties);
        assertThrows(OutboundSecurityException.class, () -> client.execute(OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url("http://allowed.example")
                .method("GET")
                .headers(Map.of("Host", "127.0.0.1"))
                .build()));
        assertThrows(OutboundSecurityException.class, () -> client.execute(OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url("http://allowed.example")
                .method("POST")
                .headers(Map.of("X-Inner-Call", "true"))
                .build()));

        properties.setMaxRequestBytes(2);
        assertThrows(OutboundSecurityException.class, () -> client.execute(OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url("http://allowed.example")
                .method("POST")
                .body(new byte[3])
                .build()));

        verify(policyService, never()).validate(any());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void shouldBlockDnsRebindingBeforeOpeningConnection() throws Exception {
        ValidatedOutboundTarget initial = targetWithAddress("rebind.example", "/hook", "8.8.8.8");
        when(policyService.validate(any()))
                .thenReturn(initial)
                .thenThrow(new OutboundSecurityException("目标主机解析到未授权的私网地址"));
        OkHttpSecureOutboundClient client = new OkHttpSecureOutboundClient(policyService, properties);

        assertThrows(OutboundSecurityException.class, () -> client.execute(OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url(initial.getUri().toString())
                .method("POST")
                .body(new byte[0])
                .build()));

        assertEquals(0, server.getRequestCount());
    }

    @Test
    void shouldRejectRedirectByDefault() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(302)
                .addHeader("Location", "http://redirect.example:" + server.getPort() + "/next"));
        ValidatedOutboundTarget target = target("allowed.example", "/start");
        when(policyService.validate(any())).thenReturn(target);
        OkHttpSecureOutboundClient client = new OkHttpSecureOutboundClient(policyService, properties);

        assertThrows(OutboundSecurityException.class, () -> client.execute(OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url(target.getUri().toString())
                .method("GET")
                .build()));

        assertEquals(1, server.getRequestCount());
    }

    @Test
    void shouldRevalidateRedirectAndStripCrossOriginCredentials() throws Exception {
        properties.setRedirectsEnabled(true);
        properties.setMaxRedirects(2);
        server.enqueue(new MockResponse().setResponseCode(302)
                .addHeader("Location", "http://redirect.example:" + server.getPort() + "/next"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("done"));
        ValidatedOutboundTarget first = target("allowed.example", "/start");
        ValidatedOutboundTarget second = target("redirect.example", "/next");
        when(policyService.validate(any())).thenReturn(first, first, second, second);
        OkHttpSecureOutboundClient client = new OkHttpSecureOutboundClient(policyService, properties);

        OutboundResponse response = client.execute(OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url(first.getUri().toString())
                .method("GET")
                .headers(Map.of("Authorization", "Bearer secret", "Cookie", "sid=secret"))
                .build());

        assertEquals(200, response.getStatusCode());
        server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest redirected = server.takeRequest(1, TimeUnit.SECONDS);
        assertNull(redirected.getHeader("Authorization"));
        assertNull(redirected.getHeader("Cookie"));
    }

    @Test
    void shouldRejectOversizedOrTimedOutResponse() throws Exception {
        properties.setMaxResponseBytes(8);
        server.enqueue(new MockResponse().setResponseCode(200).setBody("123456789"));
        ValidatedOutboundTarget target = target("allowed.example", "/large");
        when(policyService.validate(any())).thenReturn(target);
        OkHttpSecureOutboundClient client = new OkHttpSecureOutboundClient(policyService, properties);
        assertThrows(OutboundSecurityException.class, () -> client.execute(request(target)));

        properties.setMaxResponseBytes(1024);
        properties.setReadTimeout(Duration.ofMillis(50));
        server.enqueue(new MockResponse().setResponseCode(200)
                .setBody("slow")
                .setBodyDelay(200, TimeUnit.MILLISECONDS));
        assertThrows(OutboundSecurityException.class, () -> client.execute(request(target)));
    }

    private OutboundRequest request(ValidatedOutboundTarget target) {
        return OutboundRequest.builder()
                .scene(OutboundScenes.FLOW_API)
                .url(target.getUri().toString())
                .method("GET")
                .build();
    }

    private ValidatedOutboundTarget target(String host, String path) throws Exception {
        return targetWithAddress(host, path, "127.0.0.1");
    }

    private ValidatedOutboundTarget targetWithAddress(String host, String path, String address) throws Exception {
        URI uri = new URI("http://" + host + ":" + server.getPort() + path);
        return new ValidatedOutboundTarget(
                OutboundScenes.FLOW_API,
                uri,
                host,
                server.getPort(),
                java.util.List.of(InetAddress.getByName(address)),
                IpAddressClassifier.AddressType.PUBLIC);
    }
}
