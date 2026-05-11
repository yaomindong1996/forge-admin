package com.mdframe.forge.plugin.external.strategy.impl;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.strategy.ExternalCustomAuthAdapter;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;
import java.util.Map;

@Component
public class HeaderMapCustomAuthAdapter implements ExternalCustomAuthAdapter {

    @Override
    public String getAdapterType() {
        return "header_map";
    }

    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, JSONObject config) {
        if (config == null || config.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                requestBuilder.header(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
    }
}
