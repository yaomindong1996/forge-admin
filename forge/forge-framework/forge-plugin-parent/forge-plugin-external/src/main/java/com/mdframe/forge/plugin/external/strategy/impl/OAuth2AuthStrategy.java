package com.mdframe.forge.plugin.external.strategy.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.strategy.ExternalAuthStrategy;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OAuth2AuthStrategy implements ExternalAuthStrategy {

    @Override
    public String getAuthType() {
        return "OAuth2";
    }

    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, String authConfig) {
        JSONObject config = JSON.parseObject(authConfig);
        String accessToken = requestAccessToken(config);
        String tokenType = config.getString("tokenType");
        if (tokenType == null || tokenType.isEmpty()) {
            tokenType = "Bearer";
        }
        requestBuilder.header("Authorization", tokenType + " " + accessToken);
    }

    @Override
    public boolean validateConfig(String authConfig) {
        if (authConfig == null || authConfig.isEmpty()) {
            return false;
        }
        try {
            JSONObject config = JSON.parseObject(authConfig);
            return config.getString("tokenUrl") != null
                    && config.getString("clientId") != null
                    && config.getString("clientSecret") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String requestAccessToken(JSONObject config) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", defaultValue(config.getString("grantType"), "client_credentials"));
        form.put("client_id", config.getString("clientId"));
        form.put("client_secret", config.getString("clientSecret"));
        if (config.getString("scope") != null && !config.getString("scope").isEmpty()) {
            form.put("scope", config.getString("scope"));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getString("tokenUrl")))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)))
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("OAuth2获取Token失败，HTTP状态码: " + response.statusCode());
            }
            JSONObject body = JSON.parseObject(response.body());
            String tokenType = body.getString("token_type");
            if (tokenType != null && !tokenType.isEmpty()) {
                config.put("tokenType", tokenType);
            }
            String accessToken = body.getString("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                throw new BusinessException("OAuth2响应缺少access_token");
            }
            return accessToken;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("OAuth2获取Token失败: " + e.getMessage());
        }
    }

    private String encodeForm(Map<String, String> form) {
        return form.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
