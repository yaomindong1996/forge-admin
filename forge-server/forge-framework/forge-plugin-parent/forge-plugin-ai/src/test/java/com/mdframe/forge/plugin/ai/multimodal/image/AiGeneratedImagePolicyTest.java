package com.mdframe.forge.plugin.ai.multimodal.image;

import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.outbound.client.OkHttpSecureOutboundClient;
import com.mdframe.forge.starter.outbound.config.OutboundProperties;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.security.DefaultOutboundPolicyService;
import com.mdframe.forge.starter.outbound.security.IpAddressClassifier;
import com.mdframe.forge.starter.outbound.security.OutboundDnsResolver;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 真策略 + 真受控客户端；合成白名单/DNS 在连接前拒绝，绝不访问这些地址。 */
class AiGeneratedImagePolicyTest {
    private final SysOutboundWhitelistMapper rules = mock(SysOutboundWhitelistMapper.class);
    private final OutboundDnsResolver dns = mock(OutboundDnsResolver.class);
    private final FileManager files = mock(FileManager.class);

    @Test
    void should_failClosed_when_cdnWasNotExplicitlyWhitelisted() {
        assertThatThrownBy(() -> store().store("https://cdn.example.invalid/image"))
                .hasMessageContaining("白名单");
        verifyNoInteractions(dns, files);
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.0.0.1", "169.254.169.254", "::1", "fd00::1"})
    void should_blockPrivateAndMetadataTargets_even_when_ruleRequestsPrivateAccess(String address) throws Exception {
        var rule = new SysOutboundWhitelist();
        rule.setAllowPrivate(1);
        when(rules.selectActiveRules(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(rule));
        when(dns.resolveAll("cdn.example.invalid")).thenReturn(List.of(InetAddress.getByName(address)));
        assertThatThrownBy(() -> store().store("https://cdn.example.invalid/image"))
                .isInstanceOf(com.mdframe.forge.starter.outbound.security.OutboundSecurityException.class);
        verifyNoInteractions(files);
    }

    @Test
    void should_rejectMixedDnsAnswers_without_networkOrStorage() throws Exception {
        when(rules.selectActiveRules(anyLong(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of(new SysOutboundWhitelist()));
        when(dns.resolveAll("cdn.example.invalid")).thenReturn(List.of(InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("127.0.0.1")));
        assertThatThrownBy(() -> store().store("https://cdn.example.invalid/image"))
                .hasMessageContaining("禁止访问");
        verifyNoInteractions(files);
    }

    @ParameterizedTest
    @ValueSource(strings = {"file:///etc/passwd", "ftp://cdn.example.invalid/a",
            "https://user:pass@cdn.example.invalid/a",
            "https://cdn.example.invalid/a#fragment", "https://127.1/a", "https://cdn.example.invalid:70000/a"})
    void should_rejectAmbiguousTargets_before_whitelistDnsOrStorage(String url) {
        assertThatThrownBy(() -> store().store(url))
                .isInstanceOf(com.mdframe.forge.starter.outbound.security.OutboundSecurityException.class);
        verifyNoInteractions(rules, dns, files);
    }

    private AiGeneratedImageStore store() {
        var policy = new DefaultOutboundPolicyService(rules, dns,
                new IpAddressClassifier(Set.of()), new OutboundHostNormalizer());
        var client = new OkHttpSecureOutboundClient(policy, new OutboundProperties());
        var properties = new AiImageGenerationProperties();
        return new AiGeneratedImageStore(client, files, properties, new AiImageContentValidator(properties));
    }
}
