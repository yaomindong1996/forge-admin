package com.mdframe.forge.plugin.ai.resource;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 运维侧精确源地址白名单校验与 SSRF 防护。UI 参数不能扩大该边界。
 */
@Component
public class McpDestinationPolicy {

    private final Set<String> allowed;

    public McpDestinationPolicy(@Value("${forge.ai.resources.mcp.allowed-origins:}") String origins) {
        this.allowed = new HashSet<>();
        if (origins != null) {
            for (String item : origins.split(",")) {
                if (!item.isBlank()) {
                    this.allowed.add(origin(parse(item.strip())));
                }
            }
        }
    }

    public static URI parse(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || !Set.of("https", "http").contains(uri.getScheme())
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || uri.getPort() == 0 || uri.getPort() > 65535 || value.length() > 500
                    || value.chars().anyMatch(c -> c <= 32 || c == 127)) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (RuntimeException e) {
            throw new BusinessException("请填写不含凭据、参数和片段的HTTP服务地址");
        }
    }

    public static String origin(URI uri) {
        int port = uri.getPort() < 0 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
        return uri.getScheme() + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
    }

    public boolean allowed(String endpoint) {
        try {
            return allowed.contains(origin(parse(endpoint)));
        } catch (BusinessException e) {
            return false;
        }
    }

    public URI require(String endpoint) {
        URI uri = parse(endpoint);
        if (!allowed(endpoint)) {
            throw new BusinessException("此服务地址尚未加入服务器出站白名单，请联系管理员配置精确源地址");
        }
        try {
            InetAddress[] resolved = InetAddress.getAllByName(uri.getHost());
            boolean literal = uri.getHost().matches("[0-9.]+") || uri.getHost().contains(":");
            for (InetAddress ip : resolved) {
                if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress()
                        || ip.isMulticastAddress()
                        || (ip.isSiteLocalAddress() && !literal)
                        || (!"https".equals(uri.getScheme()) && !(literal && ip.isSiteLocalAddress()))) {
                    throw new BusinessException("不允许访问本机、元数据地址或非HTTPS公网服务");
                }
            }
            return uri;
        } catch (UnknownHostException e) {
            throw new BusinessException("服务地址无法解析，请检查域名");
        }
    }
}
