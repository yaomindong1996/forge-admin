package com.mdframe.forge.starter.outbound.security;

import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.model.OutboundRequestContext;
import com.mdframe.forge.starter.outbound.model.ValidatedOutboundTarget;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultOutboundPolicyServiceTest {

    private SysOutboundWhitelistMapper mapper;
    private StubDnsResolver resolver;
    private DefaultOutboundPolicyService service;

    @BeforeEach
    void setUp() {
        mapper = mock(SysOutboundWhitelistMapper.class);
        resolver = new StubDnsResolver();
        service = new DefaultOutboundPolicyService(
                mapper, resolver, new IpAddressClassifier(Set.of()), new OutboundHostNormalizer());
    }

    @Test
    void shouldNormalizeUrlAndValidateAllPublicDnsAddresses() throws Exception {
        resolver.addresses = addresses("8.8.8.8", "2606:4700:4700::1111");
        when(mapper.selectActiveRules(1L, OutboundScenes.FLOW_API, "https", "example.com", 443))
                .thenReturn(List.of(rule(0)));

        ValidatedOutboundTarget target = service.validate(
                new OutboundRequestContext(OutboundScenes.FLOW_API, "HTTPS://Example.COM./notify?token=secret"));

        assertEquals("https", target.getUri().getScheme());
        assertEquals("example.com", target.getUri().getHost());
        assertEquals(443, target.getPort());
        assertEquals(2, target.getAddresses().size());
        verify(mapper).selectActiveRules(1L, OutboundScenes.FLOW_API, "https", "example.com", 443);
    }

    @Test
    void shouldRejectUnsupportedOrAmbiguousUrlFormsBeforeWhitelistLookup() {
        List<String> invalidUrls = List.of(
                "file:///etc/passwd",
                "https://user:password@example.com/path",
                "https://user%40example.com@evil.example/path",
                "https://example%2ecom/path",
                "https://127.1/path",
                "https://0177.0.0.1/path",
                "https://0x7f000001/path",
                "https://example.com:70000/path");

        for (String url : invalidUrls) {
            assertThrows(OutboundSecurityException.class,
                    () -> service.validate(new OutboundRequestContext(OutboundScenes.FLOW_API, url)), url);
        }
        verify(mapper, never()).selectActiveRules(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void shouldFailClosedWhenAnyDnsAddressIsBlocked() throws Exception {
        resolver.addresses = addresses("8.8.8.8", "127.0.0.1");
        when(mapper.selectActiveRules(1L, OutboundScenes.FLOW_API, "https", "api.example.com", 443))
                .thenReturn(List.of(rule(1)));

        OutboundSecurityException exception = assertThrows(OutboundSecurityException.class,
                () -> service.validate(context(OutboundScenes.FLOW_API, "https://api.example.com/callback")));

        assertEquals("目标主机解析到禁止访问的地址", exception.getMessage());
    }

    @Test
    void shouldOnlyAllowPrivateAddressesForExplicitFlowRule() throws Exception {
        resolver.addresses = addresses("10.0.0.8");
        when(mapper.selectActiveRules(1L, OutboundScenes.FLOW_API, "https", "internal.example.com", 443))
                .thenReturn(List.of(rule(1)));

        ValidatedOutboundTarget target = service.validate(
                context(OutboundScenes.FLOW_API, "https://internal.example.com/hook"));

        assertEquals(IpAddressClassifier.AddressType.PRIVATE, target.getAddressType());
    }

    @Test
    void shouldOnlyAllowPrivateAddressesForExplicitJobRpcRule() throws Exception {
        resolver.addresses = addresses("10.20.30.40");
        when(mapper.selectActiveRules(1L, OutboundScenes.JOB_RPC, "http", "executor.internal", 8580))
                .thenReturn(List.of(rule(1)));

        ValidatedOutboundTarget target = service.validate(
                context(OutboundScenes.JOB_RPC, "http://executor.internal:8580/job/executor/execute"));

        assertEquals(IpAddressClassifier.AddressType.PRIVATE, target.getAddressType());
    }

    @Test
    void shouldRejectPrivateAddressForJobWebhookEvenIfPersistedRuleIsPermissive() throws Exception {
        resolver.addresses = addresses("192.168.1.8");
        when(mapper.selectActiveRules(1L, OutboundScenes.JOB_WEBHOOK, "https", "hooks.example.com", 443))
                .thenReturn(List.of(rule(1)));

        assertThrows(OutboundSecurityException.class,
                () -> service.validate(context(OutboundScenes.JOB_WEBHOOK, "https://hooks.example.com/task")));
    }

    @Test
    void shouldRejectEmptyDnsAndMissingWhitelist() throws Exception {
        when(mapper.selectActiveRules(1L, OutboundScenes.FLOW_API, "https", "unknown.example.com", 443))
                .thenReturn(List.of());
        assertThrows(OutboundSecurityException.class,
                () -> service.validate(context(OutboundScenes.FLOW_API, "https://unknown.example.com")));

        when(mapper.selectActiveRules(1L, OutboundScenes.FLOW_API, "https", "empty.example.com", 443))
                .thenReturn(List.of(rule(0)));
        resolver.addresses = List.of();
        assertThrows(OutboundSecurityException.class,
                () -> service.validate(context(OutboundScenes.FLOW_API, "https://empty.example.com")));
    }

    private OutboundRequestContext context(String scene, String url) {
        return new OutboundRequestContext(scene, url);
    }

    private SysOutboundWhitelist rule(int allowPrivate) {
        SysOutboundWhitelist rule = new SysOutboundWhitelist();
        rule.setAllowPrivate(allowPrivate);
        return rule;
    }

    private List<InetAddress> addresses(String... values) throws Exception {
        return java.util.Arrays.stream(values).map(value -> {
            try {
                return InetAddress.getByName(value);
            } catch (Exception exception) {
                throw new IllegalArgumentException(exception);
            }
        }).toList();
    }

    private static class StubDnsResolver implements OutboundDnsResolver {

        private List<InetAddress> addresses = List.of();

        @Override
        public List<InetAddress> resolveAll(String host) {
            return addresses;
        }
    }
}
