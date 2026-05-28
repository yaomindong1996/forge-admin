package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppOpenInfoVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务应用入口打开方式解析服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessAppOpenService {

    private final BusinessAppMapper businessAppMapper;

    public BusinessAppOpenInfoVO openInfo(Long id) {
        AiBusinessApp app = businessAppMapper.selectEntityById(resolveTenantId(), id);
        if (app == null) {
            throw new BusinessException("应用入口不存在");
        }
        return buildOpenInfo(app);
    }

    public BusinessAppOpenInfoVO buildRuntimeOpenInfo(AiBusinessApp app) {
        return buildOpenInfo(app);
    }

    private BusinessAppOpenInfoVO buildOpenInfo(AiBusinessApp app) {
        BusinessAppOpenInfoVO vo = new BusinessAppOpenInfoVO();
        vo.setAppId(app.getId());
        vo.setAppCode(app.getAppCode());
        vo.setAppName(app.getAppName());
        vo.setAppType(app.getAppType());
        vo.setEntryMode(app.getEntryMode());
        vo.setConfigKey(app.getConfigKey());
        vo.setPermissionGranted(hasPermission("ai:businessApp:open"));
        vo.setOpenType(resolveOpenType(app.getEntryMode()));
        vo.setTargetUrl(resolveTargetUrl(app));

        boolean enabled = Integer.valueOf(1).equals(app.getStatus());
        boolean hasTarget = StringUtils.isNotBlank(vo.getTargetUrl());
        String securityMessage = validateOpenSecurity(app, vo.getOpenType(), vo.getTargetUrl());
        vo.setCanOpen(enabled && hasTarget && Boolean.TRUE.equals(vo.getPermissionGranted()) && securityMessage == null);
        if (!enabled) {
            vo.setMessage("应用入口已停用");
            vo.setNextAction("ENABLE_APP_ENTRY");
            vo.setNextActionLabel("启用应用入口");
        } else if (!hasTarget) {
            vo.setMessage("应用入口尚未配置打开地址");
            vo.setNextAction("CONFIGURE_ENTRY");
            vo.setNextActionLabel("配置打开地址");
        } else if (!Boolean.TRUE.equals(vo.getPermissionGranted())) {
            vo.setMessage("缺少应用入口打开权限");
            vo.setNextAction("REQUEST_PERMISSION");
            vo.setNextActionLabel("联系管理员授权");
        } else if (securityMessage != null) {
            vo.setMessage(securityMessage);
            vo.setNextAction("CONFIGURE_SECURITY");
            vo.setNextActionLabel("配置域名白名单");
        } else {
            vo.setMessage("可打开应用入口");
            vo.setNextAction("OPEN_APP");
            vo.setNextActionLabel("打开应用入口");
        }
        return vo;
    }

    private String resolveTargetUrl(AiBusinessApp app) {
        String entryMode = StringUtils.defaultIfBlank(app.getEntryMode(), "ROUTE").toUpperCase();
        if ("RUNTIME".equals(entryMode)) {
            if (StringUtils.isNotBlank(app.getConfigKey())) {
                return "/ai/crud-page/" + app.getConfigKey();
            }
            return StringUtils.trimToNull(app.getEntryUrl());
        }
        return StringUtils.trimToNull(app.getEntryUrl());
    }

    private String validateOpenSecurity(AiBusinessApp app, String openType, String targetUrl) {
        if (!requiresExternalSecurity(openType) || StringUtils.isBlank(targetUrl) || isInternalPath(targetUrl)) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(targetUrl);
        } catch (IllegalArgumentException e) {
            return "应用入口地址格式不正确";
        }
        String scheme = StringUtils.lowerCase(uri.getScheme());
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return "外部入口仅允许 HTTP/HTTPS 地址";
        }
        if (StringUtils.isNotBlank(uri.getUserInfo())) {
            return "外部入口地址不能包含用户名或密码";
        }
        if (containsSensitiveQuery(uri.getQuery())) {
            return "外部入口地址不能包含长期 Token、密码或密钥";
        }
        String host = StringUtils.lowerCase(uri.getHost());
        List<String> allowedDomains = resolveAllowedDomains(app.getOptions());
        if (allowedDomains.isEmpty()) {
            return "外部入口未配置域名白名单";
        }
        boolean matched = allowedDomains.stream().anyMatch(domain -> matchesDomain(host, domain));
        return matched ? null : "外部入口域名不在白名单内";
    }

    private boolean requiresExternalSecurity(String openType) {
        String type = StringUtils.defaultString(openType).toUpperCase();
        return "IFRAME".equals(type) || "EXTERNAL".equals(type) || "H5".equals(type);
    }

    private boolean isInternalPath(String targetUrl) {
        return StringUtils.startsWith(targetUrl, "/") && !StringUtils.startsWith(targetUrl, "//");
    }

    private boolean containsSensitiveQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return false;
        }
        String lowerQuery = StringUtils.lowerCase(query);
        return lowerQuery.contains("token=")
                || lowerQuery.contains("access_token=")
                || lowerQuery.contains("password=")
                || lowerQuery.contains("secret=")
                || lowerQuery.contains("ak=")
                || lowerQuery.contains("sk=");
    }

    private List<String> resolveAllowedDomains(String options) {
        if (StringUtils.isBlank(options)) {
            return List.of();
        }
        try {
            JSONObject object = JSON.parseObject(options);
            Object value = object.get("allowedDomains");
            List<String> domains = new ArrayList<>();
            if (value instanceof JSONArray array) {
                for (Object item : array) {
                    addAllowedDomain(domains, item);
                }
            } else {
                addAllowedDomain(domains, value);
            }
            return domains;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void addAllowedDomain(List<String> domains, Object value) {
        if (value == null) {
            return;
        }
        String domain = StringUtils.lowerCase(StringUtils.trimToNull(String.valueOf(value)));
        if (StringUtils.isNotBlank(domain)) {
            domains.add(domain);
        }
    }

    private boolean matchesDomain(String host, String allowedDomain) {
        if (StringUtils.isBlank(host) || StringUtils.isBlank(allowedDomain)) {
            return false;
        }
        if (allowedDomain.startsWith("*.")) {
            String suffix = allowedDomain.substring(1);
            return host.endsWith(suffix) && host.length() > suffix.length();
        }
        return host.equals(allowedDomain);
    }

    private String resolveOpenType(String entryMode) {
        String mode = StringUtils.defaultIfBlank(entryMode, "ROUTE").toUpperCase();
        return switch (mode) {
            case "RUNTIME" -> "ROUTE";
            case "IFRAME" -> "IFRAME";
            case "EXTERNAL" -> "EXTERNAL";
            case "H5" -> "H5";
            case "API" -> "API";
            default -> "ROUTE";
        };
    }

    private boolean hasPermission(String permission) {
        try {
            return SessionHelper.hasPermission(permission);
        } catch (Exception e) {
            return false;
        }
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
