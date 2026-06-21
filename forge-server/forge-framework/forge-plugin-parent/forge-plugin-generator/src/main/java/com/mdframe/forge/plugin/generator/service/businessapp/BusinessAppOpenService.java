package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.generator.constant.BusinessAppMode;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppOpenInfoVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务访问入口打开方式解析服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessAppOpenService {

    private static final Set<String> RUNTIME_OPEN_MODES = Set.of("LIST", "CREATE_FORM", "DETAIL");

    private final BusinessAppMapper businessAppMapper;
    private final AiCrudConfigMapper aiCrudConfigMapper;

    public BusinessAppOpenInfoVO openInfo(Long id) {
        AiBusinessApp app = businessAppMapper.selectEntityById(resolveTenantId(), id);
        if (app == null) {
            throw new BusinessException("访问入口不存在");
        }
        return buildOpenInfo(app);
    }

    public BusinessAppOpenInfoVO buildRuntimeOpenInfo(AiBusinessApp app) {
        return buildOpenInfo(app);
    }

    private BusinessAppOpenInfoVO buildOpenInfo(AiBusinessApp app) {
        JSONObject options = readOptions(app.getOptions());
        JSONObject adminMenu = readAdminMenu(options);
        Long menuResourceId = readLong(firstNonNull(adminMenu.get("menuResourceId"), options.get("menuResourceId")));
        String activeMenuKey = menuResourceId == null ? null : String.valueOf(menuResourceId);
        String runtimeOpenMode = resolveRuntimeOpenMode(options.getString("runtimeOpenMode"));
        boolean codeDownloadMode = isCodeDownloadRuntime(app, options);
        String targetUrl = codeDownloadMode
                ? appendQuery("/app-center", Map.of("codeAppId", String.valueOf(app.getId())))
                : resolveTargetUrl(app, runtimeOpenMode, menuResourceId);

        BusinessAppOpenInfoVO vo = new BusinessAppOpenInfoVO();
        vo.setAppId(app.getId());
        vo.setAppCode(app.getAppCode());
        vo.setAppName(app.getAppName());
        vo.setAppType(app.getAppType());
        vo.setEntryMode(app.getEntryMode());
        vo.setConfigKey(app.getConfigKey());
        vo.setRuntimeOpenMode(runtimeOpenMode);
        vo.setMenuResourceId(menuResourceId);
        vo.setActiveMenuKey(activeMenuKey);
        String permissionCode = StringUtils.trimToNull(options.getString("permissionCode"));
        boolean basePermissionGranted = hasPermission("ai:businessApp:open");
        boolean entryPermissionGranted = permissionCode == null || hasPermission(permissionCode);
        vo.setPermissionGranted(basePermissionGranted && entryPermissionGranted);
        vo.setOpenType(resolveOpenType(app.getEntryMode()));
        vo.setTargetUrl(targetUrl);
        vo.setTargetRoute(targetUrl);

        // RUNTIME 模式下校验 configKey 对应的运行配置是否存在
        String runtimeMessage = codeDownloadMode ? null : validateRuntimeConfig(app);
        vo.setRuntimeStatus(runtimeMessage == null ? "AVAILABLE" : "MISSING");
        vo.setRuntimeMessage(runtimeMessage);

        boolean enabled = Integer.valueOf(1).equals(app.getStatus());
        boolean hasTarget = StringUtils.isNotBlank(vo.getTargetUrl());
        boolean runtimeValid = runtimeMessage == null;
        String securityMessage = validateOpenSecurity(app, vo.getOpenType(), vo.getTargetUrl());
        vo.setCanOpen(!codeDownloadMode && enabled && hasTarget && runtimeValid && Boolean.TRUE.equals(vo.getPermissionGranted()) && securityMessage == null);
        if (!enabled) {
            vo.setMessage("访问入口已停用");
            vo.setNextAction("ENABLE_APP_ENTRY");
            vo.setNextActionLabel("启用访问入口");
        } else if (codeDownloadMode) {
            vo.setMessage("该访问入口为下载代码模式，请在应用管理中预览或下载功能代码");
            vo.setNextAction("OPEN_CODE_PANEL");
            vo.setNextActionLabel("查看功能代码");
        } else if ("RUNTIME".equals(StringUtils.defaultIfBlank(app.getEntryMode(), "").toUpperCase()) && !runtimeValid) {
            vo.setMessage(runtimeMessage);
            vo.setNextAction("PUBLISH_APP");
            vo.setNextActionLabel("发布应用");
        } else if (!hasTarget) {
            vo.setMessage("访问入口尚未配置打开地址");
            vo.setNextAction("CONFIGURE_ENTRY");
            vo.setNextActionLabel("配置打开地址");
        } else if (!Boolean.TRUE.equals(vo.getPermissionGranted())) {
            vo.setMessage(permissionCode == null ? "缺少访问入口打开权限" : "缺少访问入口权限: " + permissionCode);
            vo.setNextAction("REQUEST_PERMISSION");
            vo.setNextActionLabel("联系管理员授权");
        } else if (securityMessage != null) {
            vo.setMessage(securityMessage);
            vo.setNextAction("CONFIGURE_SECURITY");
            vo.setNextActionLabel("配置域名白名单");
        } else {
            vo.setMessage("可打开访问入口");
            vo.setNextAction("OPEN_APP");
            vo.setNextActionLabel("打开访问入口");
        }
        return vo;
    }

    private boolean isCodeDownloadRuntime(AiBusinessApp app, JSONObject options) {
        String entryMode = StringUtils.defaultIfBlank(app.getEntryMode(), "").toUpperCase();
        return "RUNTIME".equals(entryMode)
                && BusinessAppMode.isCodeDownload(options == null ? null : options.get("appMode"));
    }

    private String validateRuntimeConfig(AiBusinessApp app) {
        String entryMode = StringUtils.defaultIfBlank(app.getEntryMode(), "").toUpperCase();
        if (!"RUNTIME".equals(entryMode)) {
            return null;
        }
        String configKey = StringUtils.trimToNull(app.getConfigKey());
        if (configKey == null) {
            return "访问入口未配置运行配置，请先配置业务单元并发布应用";
        }
        AiCrudConfig config = aiCrudConfigMapper.selectByConfigKey(resolveTenantId(), configKey);
        if (config == null) {
            return "运行配置不存在，请先发布应用";
        }
        if ("1".equals(config.getStatus())) {
            return "运行配置已停用，请先启用运行配置";
        }
        if (!"PUBLISHED".equals(config.getPublishStatus())) {
            return "运行配置未发布，请先发布应用";
        }
        return null;
    }

    private String resolveTargetUrl(AiBusinessApp app, String runtimeOpenMode, Long menuResourceId) {
        String entryMode = StringUtils.defaultIfBlank(app.getEntryMode(), "ROUTE").toUpperCase();
        if ("RUNTIME".equals(entryMode)) {
            if (StringUtils.isNotBlank(app.getConfigKey())) {
                return buildRuntimeTargetRoute(app, runtimeOpenMode, menuResourceId);
            }
            return StringUtils.trimToNull(app.getEntryUrl());
        }
        String entryUrl = StringUtils.trimToNull(app.getEntryUrl());
        if (isInternalPath(entryUrl)) {
            return appendMenuContext(entryUrl, app, menuResourceId);
        }
        return entryUrl;
    }

    private String buildRuntimeTargetRoute(AiBusinessApp app, String runtimeOpenMode, Long menuResourceId) {
        JSONObject options = readOptions(app.getOptions());
        Map<String, String> query = new LinkedHashMap<>();
        query.put("appId", String.valueOf(app.getId()));
        if (menuResourceId != null) {
            query.put("menuKey", String.valueOf(menuResourceId));
            query.put("menuResourceId", String.valueOf(menuResourceId));
        }
        query.put("runtimeOpenMode", runtimeOpenMode);
        String targetPageKey = StringUtils.trimToNull(options.getString("targetPageKey"));
        if (targetPageKey == null) {
            targetPageKey = "DETAIL".equals(runtimeOpenMode) ? "detail" : "list";
        }
        query.put("pageKey", targetPageKey);
        String targetFormKey = StringUtils.trimToNull(options.getString("targetFormKey"));
        if (targetFormKey != null) {
            query.put("formKey", targetFormKey);
        }
        if ("CREATE_FORM".equals(runtimeOpenMode)) {
            query.put("mode", "create");
        } else if ("DETAIL".equals(runtimeOpenMode)) {
            query.put("mode", "detail");
        }
        Object defaultParams = options.get("defaultParams");
        if (defaultParams instanceof JSONObject params) {
            params.forEach((key, value) -> {
                if (StringUtils.isNotBlank(key) && value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                    query.putIfAbsent(key, String.valueOf(value));
                }
            });
        }
        if (StringUtils.isNotBlank(app.getAppName())) {
            query.put("title", app.getAppName());
        }
        return appendQuery("/ai/crud-page/" + app.getConfigKey(), query);
    }

    private String appendMenuContext(String targetUrl, AiBusinessApp app, Long menuResourceId) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("appId", String.valueOf(app.getId()));
        if (menuResourceId != null) {
            query.put("menuKey", String.valueOf(menuResourceId));
            query.put("menuResourceId", String.valueOf(menuResourceId));
        }
        if (StringUtils.isNotBlank(app.getAppName())) {
            query.put("title", app.getAppName());
        }
        return appendQuery(targetUrl, query);
    }

    private String appendQuery(String targetUrl, Map<String, String> query) {
        if (StringUtils.isBlank(targetUrl) || query == null || query.isEmpty()) {
            return targetUrl;
        }
        StringBuilder builder = new StringBuilder(targetUrl);
        builder.append(targetUrl.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()) || StringUtils.isBlank(entry.getValue())) {
                continue;
            }
            if (!first) {
                builder.append("&");
            }
            builder.append(urlEncode(entry.getKey()))
                    .append("=")
                    .append(urlEncode(entry.getValue()));
            first = false;
        }
        if (first) {
            return targetUrl;
        }
        return builder.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String validateOpenSecurity(AiBusinessApp app, String openType, String targetUrl) {
        if (!requiresExternalSecurity(openType) || StringUtils.isBlank(targetUrl) || isInternalPath(targetUrl)) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(targetUrl);
        } catch (IllegalArgumentException e) {
            return "访问入口地址格式不正确";
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

    private JSONObject readOptions(String options) {
        if (StringUtils.isBlank(options)) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(options);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JSONObject readAdminMenu(JSONObject options) {
        JSONObject adminMenu = options.getJSONObject("adminMenu");
        return adminMenu == null ? new JSONObject() : adminMenu;
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveRuntimeOpenMode(String value) {
        String mode = StringUtils.defaultIfBlank(value, "LIST").toUpperCase();
        return RUNTIME_OPEN_MODES.contains(mode) ? mode : "LIST";
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
