package com.mdframe.forge.plugin.ai.agent.engine.tool.builtin;

import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.model.OutboundRequest;
import com.mdframe.forge.starter.outbound.model.OutboundRequestContext;
import com.mdframe.forge.starter.outbound.model.OutboundResponse;
import com.mdframe.forge.starter.outbound.security.DefaultOutboundPolicyService;
import com.mdframe.forge.starter.outbound.security.IpAddressClassifier;
import com.mdframe.forge.starter.outbound.security.OutboundDnsResolver;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 真实策略校验器 + Mock Mapper/DNS/传输；不访问公网或真实白名单库。 */
class AgentHttpToolPolicyTest {

    private final SysOutboundWhitelistMapper rules = mock(SysOutboundWhitelistMapper.class);
    private final OutboundDnsResolver dns = mock(OutboundDnsResolver.class);
    private final SecureOutboundClient client = mock(SecureOutboundClient.class);
    private final HttpTool tool = new HttpTool(client);

    @BeforeEach
    void setUp() {
        var policy = new DefaultOutboundPolicyService(rules, dns,
                new IpAddressClassifier(Set.of()), new OutboundHostNormalizer());
        when(client.execute(any())).thenAnswer(invocation -> {
            OutboundRequest request = invocation.getArgument(0);
            policy.validate(new OutboundRequestContext(request.getScene(), request.getUrl()));
            return new OutboundResponse(200, Map.of(), new byte[0]);
        });
    }

    @Test
    void should_passKnownSceneWithExplicitPublicTargetRule_when_httpToolRuns() throws Exception {
        when(rules.selectActiveRules(1L, OutboundScenes.EXTERNAL_CONNECTOR, "https", "fixture.invalid", 443))
                .thenReturn(List.of(rule(0)));
        when(dns.resolveAll(anyString())).thenReturn(List.of(InetAddress.getByName("8.8.8.8")));

        assertThat(tool.execute(Map.of("url", "https://fixture.invalid/api"), null).isSuccess()).isTrue();

        verify(rules).selectActiveRules(1L, OutboundScenes.EXTERNAL_CONNECTOR, "https", "fixture.invalid", 443);
    }

    @Test
    void should_notAllowPrivateTargetEvenWithBadLegacyRule_when_agentUsesExternalConnectorScene() throws Exception {
        when(rules.selectActiveRules(1L, OutboundScenes.EXTERNAL_CONNECTOR, "https", "fixture.invalid", 443))
                .thenReturn(List.of(rule(1)));
        when(dns.resolveAll(anyString())).thenReturn(List.of(InetAddress.getByName("10.0.0.8")));

        assertThat(tool.execute(Map.of("url", "https://fixture.invalid/api"), null).isSuccess()).isFalse();
    }

    @Test
    void should_failClosed_when_noTargetWhitelistExists() {
        assertThat(tool.execute(Map.of("url", "https://fixture.invalid/api"), null).isSuccess()).isFalse();
    }

    private SysOutboundWhitelist rule(int allowPrivate) {
        SysOutboundWhitelist rule = new SysOutboundWhitelist();
        rule.setAllowPrivate(allowPrivate);
        return rule;
    }
}
