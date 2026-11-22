package com.mdframe.forge.plugin.ai.resource;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.outbound.constant.OutboundScenes;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.mapper.SysOutboundWhitelistMapper;
import com.mdframe.forge.starter.outbound.service.impl.OutboundWhitelistServiceImpl;
import com.mdframe.forge.starter.outbound.support.OutboundHostNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * MCP 远程出站策略：SSRF 防护 + 精确源白名单（配置文件或平台出站白名单 AI_MCP 场景）。
 * <p>
 * 默认允许「公网 HTTPS」无需逐个加白；HTTP / 私网仍须运维或超级管理员放行精确源地址。
 * 普通业务表单参数不能扩大该边界。
 */
@Component
public class McpDestinationPolicy {

    private final Set<String> allowed;
    private final boolean allowUnlistedHttps;
    private final SysOutboundWhitelistMapper whitelistMapper;
    private final OutboundHostNormalizer hosts = new OutboundHostNormalizer();

    @Autowired
    public McpDestinationPolicy(
            @Value("${forge.ai.resources.mcp.allowed-origins:}") String origins,
            @Value("${forge.ai.resources.mcp.allow-unlisted-https:true}") boolean allowUnlistedHttps,
            SysOutboundWhitelistMapper whitelistMapper) {
        this.allowUnlistedHttps = allowUnlistedHttps;
        this.whitelistMapper = whitelistMapper;
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
        }
        catch (RuntimeException e) {
            throw new BusinessException("请填写不含凭据、参数和片段的HTTP服务地址");
        }
    }

    public static String origin(URI uri) {
        int port = uri.getPort() < 0 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
        return uri.getScheme() + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
    }

    /** 列表角标：是否预期可同步（未做 DNS；最终以 {@link #require} 为准）。 */
    public boolean allowed(String endpoint) {
        try {
            URI uri = parse(endpoint);
            if (listed(uri)) {
                return true;
            }
            return allowUnlistedHttps && "https".equals(uri.getScheme());
        }
        catch (BusinessException e) {
            return false;
        }
    }

    public URI require(String endpoint) {
        URI uri = parse(endpoint);
        boolean listed = listed(uri);
        if (!listed && !(allowUnlistedHttps && "https".equals(uri.getScheme()))) {
            throw new BusinessException(allowUnlistedHttps
                    ? "HTTP 或私网 MCP 地址需加入出站白名单（公网 HTTPS 通常无需配置；可在本页由超级管理员一键放行）"
                    : "此服务地址尚未加入出站白名单，请联系超级管理员在 MCP 页放行或配置精确源地址");
        }
        try {
            InetAddress[] resolved = InetAddress.getAllByName(uri.getHost());
            boolean literal = uri.getHost().matches("[0-9.]+") || uri.getHost().contains(":");
            for (InetAddress ip : resolved) {
                if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress()
                        || ip.isMulticastAddress()
                        || (ip.isSiteLocalAddress() && !literal)) {
                    throw new BusinessException("不允许访问本机、元数据地址或未明确放行的私网地址");
                }
                // 未加白时仅放行公网 HTTPS；已加白的公网 HTTP 允许（运维明确授权）
                if (!listed && !"https".equals(uri.getScheme())) {
                    throw new BusinessException("未加白的 MCP 地址仅支持 HTTPS 公网服务");
                }
            }
            return uri;
        }
        catch (UnknownHostException e) {
            throw new BusinessException("服务地址无法解析，请检查域名");
        }
    }

    private boolean listed(URI uri) {
        if (allowed.contains(origin(uri))) {
            return true;
        }
        return listedInDb(uri);
    }

    private boolean listedInDb(URI uri) {
        if (whitelistMapper == null) {
            return false;
        }
        try {
            String host = hosts.normalize(uri.getHost());
            int port = uri.getPort() < 0 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort();
            List<SysOutboundWhitelist> rules = whitelistMapper.selectActiveRules(
                    OutboundWhitelistServiceImpl.PLATFORM_TENANT_ID,
                    OutboundScenes.AI_MCP,
                    uri.getScheme().toLowerCase(Locale.ROOT),
                    host,
                    port);
            return rules != null && !rules.isEmpty();
        }
        catch (RuntimeException e) {
            return false;
        }
    }
}
