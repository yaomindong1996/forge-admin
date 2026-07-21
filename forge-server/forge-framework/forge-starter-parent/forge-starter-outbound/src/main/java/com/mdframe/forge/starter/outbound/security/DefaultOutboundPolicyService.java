package com.mdframe.forge.starter.outbound.security;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.model.OutboundRequestContext;
import com.mdframe.forge.starter.outbound.model.ValidatedOutboundTarget;
import com.mdframe.forge.starter.outbound.service.impl.OutboundWhitelistServiceImpl;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class DefaultOutboundPolicyService implements OutboundPolicyService {

    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;

    private final SysOutboundWhitelistMapper whitelistMapper;
    private final OutboundDnsResolver dnsResolver;
    private final IpAddressClassifier addressClassifier;
    private final OutboundHostNormalizer hostNormalizer;

    @Override
    public ValidatedOutboundTarget validate(OutboundRequestContext context) {
        if (context == null || context.getScene() == null || context.getUrl() == null) {
            throw new OutboundSecurityException("出站请求上下文不完整");
        }
        String scene = normalizeScene(context.getScene());
        ParsedTarget parsed = parseTarget(context.getUrl());
        List<SysOutboundWhitelist> rules = whitelistMapper.selectActiveRules(
                OutboundWhitelistServiceImpl.PLATFORM_TENANT_ID,
                scene,
                parsed.scheme(),
                parsed.host(),
                parsed.port());
        if (rules == null || rules.isEmpty()) {
            throw new OutboundSecurityException("目标地址不在出站白名单中");
        }

        List<InetAddress> addresses = resolveAddresses(parsed.host());
        boolean privateAllowed = (OutboundScenes.FLOW_API.equals(scene) || OutboundScenes.JOB_RPC.equals(scene))
                && rules.stream().allMatch(rule -> Integer.valueOf(1).equals(rule.getAllowPrivate()));
        IpAddressClassifier.AddressType targetType = IpAddressClassifier.AddressType.PUBLIC;
        for (InetAddress address : addresses) {
            IpAddressClassifier.AddressType type = addressClassifier.classify(address);
            if (type == IpAddressClassifier.AddressType.BLOCKED) {
                throw new OutboundSecurityException("目标主机解析到禁止访问的地址");
            }
            if (type == IpAddressClassifier.AddressType.PRIVATE) {
                if (!privateAllowed) {
                    throw new OutboundSecurityException("目标主机解析到未授权的私网地址");
                }
                targetType = IpAddressClassifier.AddressType.PRIVATE;
            }
        }
        return new ValidatedOutboundTarget(
                scene, parsed.uri(), parsed.host(), parsed.port(), addresses, targetType);
    }

    private ParsedTarget parseTarget(String value) {
        if (value.isBlank()) {
            throw new OutboundSecurityException("目标URL不能为空");
        }
        try {
            URI source = new URI(value.trim());
            String scheme = source.getScheme() == null ? null : source.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new OutboundSecurityException("目标URL仅支持HTTP或HTTPS协议");
            }
            if (!source.isAbsolute() || source.isOpaque() || source.getRawAuthority() == null
                    || source.getRawAuthority().contains("%") || source.getRawUserInfo() != null
                    || source.getFragment() != null || source.getHost() == null) {
                throw new OutboundSecurityException("目标URL格式不安全");
            }
            String host;
            try {
                host = hostNormalizer.normalize(source.getHost());
            } catch (BusinessException exception) {
                throw new OutboundSecurityException(exception.getMessage());
            }
            int explicitPort = source.getPort();
            int port = explicitPort == -1 ? defaultPort(scheme) : explicitPort;
            if (port < 1 || port > 65535) {
                throw new OutboundSecurityException("目标URL端口不合法");
            }
            URI canonical = buildCanonicalUri(source, scheme, host, explicitPort);
            return new ParsedTarget(canonical, scheme, host, port);
        } catch (URISyntaxException | IllegalArgumentException exception) {
            throw new OutboundSecurityException("目标URL格式不合法");
        }
    }

    private URI buildCanonicalUri(URI source, String scheme, String host, int explicitPort)
            throws URISyntaxException {
        String authorityHost = host.contains(":") ? "[" + host + "]" : host;
        StringBuilder value = new StringBuilder(scheme).append("://").append(authorityHost);
        if (explicitPort != -1) {
            value.append(':').append(explicitPort);
        }
        if (source.getRawPath() != null) {
            value.append(source.getRawPath());
        }
        if (source.getRawQuery() != null) {
            value.append('?').append(source.getRawQuery());
        }
        return new URI(value.toString());
    }

    private List<InetAddress> resolveAddresses(String host) {
        try {
            List<InetAddress> addresses = dnsResolver.resolveAll(host);
            if (addresses == null || addresses.isEmpty()) {
                throw new OutboundSecurityException("目标主机没有可用的DNS解析结果");
            }
            return addresses.stream().distinct().toList();
        } catch (UnknownHostException exception) {
            throw new OutboundSecurityException("目标主机DNS解析失败");
        }
    }

    private String normalizeScene(String value) {
        String scene = value.trim().toUpperCase(Locale.ROOT);
        if (!OutboundScenes.SUPPORTED.contains(scene)) {
            throw new OutboundSecurityException("不支持的出站场景");
        }
        return scene;
    }

    private int defaultPort(String scheme) {
        return "https".equals(scheme) ? HTTPS_PORT : HTTP_PORT;
    }

    private record ParsedTarget(URI uri, String scheme, String host, int port) {
    }
}
