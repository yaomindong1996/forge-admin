package com.mdframe.forge.plugin.external.strategy.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.strategy.ExternalAuthStrategy;
import com.mdframe.forge.plugin.external.strategy.ExternalCustomAuthAdapter;
import com.mdframe.forge.plugin.external.strategy.ExternalCustomAuthAdapterFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;

@Component
@RequiredArgsConstructor
public class CustomAuthStrategy implements ExternalAuthStrategy {

    private final ExternalCustomAuthAdapterFactory adapterFactory;

    @Override
    public String getAuthType() {
        return "Custom";
    }

    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, String authConfig) {
        JSONObject config = JSON.parseObject(authConfig);
        String adapterType = config.getString("adapter");
        JSONObject adapterConfig = config.getJSONObject("config");
        ExternalCustomAuthAdapter adapter = adapterFactory.getAdapter(adapterType);
        adapter.applyAuth(requestBuilder, adapterConfig);
    }

    @Override
    public boolean validateConfig(String authConfig) {
        if (authConfig == null || authConfig.isEmpty()) {
            return false;
        }
        try {
            JSONObject config = JSON.parseObject(authConfig);
            return config.getString("adapter") != null;
        } catch (Exception e) {
            return false;
        }
    }
}
