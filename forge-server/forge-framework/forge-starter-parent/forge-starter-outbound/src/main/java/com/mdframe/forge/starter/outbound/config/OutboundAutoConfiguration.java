package com.mdframe.forge.starter.outbound.config;

import com.mdframe.forge.starter.outbound.client.OkHttpSecureOutboundClient;
import com.mdframe.forge.starter.outbound.client.SecureOutboundClient;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.security.DefaultOutboundPolicyService;
import com.mdframe.forge.starter.outbound.security.IpAddressClassifier;
import com.mdframe.forge.starter.outbound.security.OutboundDnsResolver;
import com.mdframe.forge.starter.outbound.security.OutboundPolicyService;
import com.mdframe.forge.starter.outbound.security.SystemOutboundDnsResolver;
import com.mdframe.forge.starter.outbound.service.OutboundWhitelistService;
import com.mdframe.forge.starter.outbound.service.impl.OutboundWhitelistServiceImpl;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(OutboundProperties.class)
@MapperScan(basePackageClasses = SysOutboundWhitelistMapper.class)
public class OutboundAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboundHostNormalizer outboundHostNormalizer() {
        return new OutboundHostNormalizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboundWhitelistService outboundWhitelistService(
            SysOutboundWhitelistMapper mapper,
            OutboundHostNormalizer hostNormalizer) {
        return new OutboundWhitelistServiceImpl(mapper, hostNormalizer);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboundDnsResolver outboundDnsResolver() {
        return new SystemOutboundDnsResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public IpAddressClassifier ipAddressClassifier() {
        return new IpAddressClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboundPolicyService outboundPolicyService(
            SysOutboundWhitelistMapper mapper,
            OutboundDnsResolver dnsResolver,
            IpAddressClassifier classifier,
            OutboundHostNormalizer hostNormalizer) {
        return new DefaultOutboundPolicyService(mapper, dnsResolver, classifier, hostNormalizer);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecureOutboundClient secureOutboundClient(
            OutboundPolicyService policyService,
            OutboundProperties properties) {
        return new OkHttpSecureOutboundClient(policyService, properties);
    }
}
