package com.mdframe.forge.plugin.external.strategy.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.strategy.ExternalAuthStrategy;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;

@Component
public class ApiKeyAuthStrategy implements ExternalAuthStrategy {

    @Override
    public String getAuthType() {
        return "ApiKey";
    }

    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, String authConfig) {
        JSONObject config = JSON.parseObject(authConfig);
        String position = config.getString("position");
        if (!"header".equalsIgnoreCase(position)) {
            return;
        }
        String name = config.getString("name");
        String value = config.getString("value");
        if (name != null && value != null) {
            requestBuilder.header(name, value);
        }
    }

    @Override
    public boolean validateConfig(String authConfig) {
        if (authConfig == null || authConfig.isEmpty()) {
            return false;
        }
        try {
            JSONObject config = JSON.parseObject(authConfig);
            return config.getString("name") != null && config.getString("value") != null;
        } catch (Exception e) {
            return false;
        }
    }
}
