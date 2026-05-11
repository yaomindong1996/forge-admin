package com.mdframe.forge.plugin.external.strategy;

import com.alibaba.fastjson2.JSONObject;

import java.net.http.HttpRequest;

public interface ExternalCustomAuthAdapter {

    String getAdapterType();

    void applyAuth(HttpRequest.Builder requestBuilder, JSONObject config);
}
