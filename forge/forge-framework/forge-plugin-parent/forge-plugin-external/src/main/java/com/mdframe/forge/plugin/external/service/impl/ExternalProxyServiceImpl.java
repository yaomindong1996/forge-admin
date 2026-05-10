package com.mdframe.forge.plugin.external.service.impl;

import com.alibaba.fastjson2.JSON;
import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import com.mdframe.forge.plugin.external.adapter.DataAdapterFactory;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.service.ExternalApiService;
import com.mdframe.forge.plugin.external.service.ExternalProxyService;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import com.mdframe.forge.plugin.external.strategy.AuthStrategy;
import com.mdframe.forge.plugin.external.strategy.AuthStrategyFactory;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalProxyServiceImpl implements ExternalProxyService {

    private final ExternalApiService apiService;
    private final ExternalSystemService systemService;
    private final AuthStrategyFactory authFactory;
    private final DataAdapterFactory adapterFactory;

    @Override
    public Object proxyRequest(Long apiId, Map<String, Object> params) {
        ExternalApi api = apiService.getById(apiId);
        if (api == null || api.getApiStatus() != 1) {
            throw new BusinessException("接口不存在或已停用");
        }

        ExternalSystem system = systemService.getById(api.getSystemId());
        if (system == null || system.getSystemStatus() != 1) {
            throw new BusinessException("系统不存在或已停用");
        }

        String fullUrl = buildFullUrl(system.getBaseUrl(), api.getApiPath());

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(30));

        AuthStrategy authStrategy = authFactory.getStrategy(system.getAuthType());
        authStrategy.applyAuth(requestBuilder, buildAuthConfig(system));

        applyRequestMethod(requestBuilder, api.getApiMethod(), params);

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = sendRequest(client, requestBuilder.build());

        Object originalData = JSON.parse(response.body());

        if (api.getResponseTransformEnabled() != null && api.getResponseTransformEnabled() 
                && api.getResponseTransformScript() != null && !api.getResponseTransformScript().isEmpty()) {
            DataAdapter adapter = adapterFactory.getAdapter("Script");
            return adapter.transform(originalData, api.getResponseTransformScript());
        }

        return originalData;
    }

    private String buildAuthConfig(ExternalSystem system) {
        if (system.getAuthType() == null || "None".equals(system.getAuthType())) {
            return null;
        }
        if ("BearerToken".equals(system.getAuthType())) {
            com.alibaba.fastjson2.JSONObject config = new com.alibaba.fastjson2.JSONObject();
            config.put("token", system.getTokenValue());
            config.put("tokenHeader", system.getTokenHeaderName());
            config.put("tokenPrefix", system.getTokenPrefix());
            return config.toJSONString();
        }
        return system.getCustomAuthConfig();
    }

    private String buildFullUrl(String baseUrl, String apiPath) {
        String url = baseUrl;
        if (!baseUrl.endsWith("/")) {
            url += "/";
        }
        url += apiPath.startsWith("/") ? apiPath.substring(1) : apiPath;
        return url;
    }

    private void applyRequestMethod(HttpRequest.Builder builder, String method, Map<String, Object> params) {
        switch (method.toUpperCase()) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(params)));
                builder.header("Content-Type", "application/json");
                break;
            case "PUT":
                builder.PUT(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(params)));
                builder.header("Content-Type", "application/json");
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                builder.GET();
        }
    }

    private HttpResponse<String> sendRequest(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new BusinessException("请求外部接口失败: " + e.getMessage());
        }
    }
}