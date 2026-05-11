package com.mdframe.forge.plugin.external.strategy.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.strategy.ExternalAuthStrategy;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class BasicAuthStrategy implements ExternalAuthStrategy {

    @Override
    public String getAuthType() {
        return "Basic";
    }

    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, String authConfig) {
        JSONObject config = JSON.parseObject(authConfig);
        String username = config.getString("username");
        String password = config.getString("password");
        String credential = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        requestBuilder.header("Authorization", "Basic " + credential);
    }

    @Override
    public boolean validateConfig(String authConfig) {
        if (authConfig == null || authConfig.isEmpty()) {
            return false;
        }
        try {
            JSONObject config = JSON.parseObject(authConfig);
            return config.getString("username") != null && config.getString("password") != null;
        } catch (Exception e) {
            return false;
        }
    }
}
