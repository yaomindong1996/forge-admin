package com.mdframe.forge.plugin.external.strategy.impl;

import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.strategy.ExternalCustomAuthAdapter;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;

/**
 * 企查查开放平台签名认证：AppKey + Timespan + Secret 生成 Sign。
 */
@Component
public class QichachaAuthAdapter implements ExternalCustomAuthAdapter {

    @Override
    public String getAdapterType() {
        return "qichacha";
    }

    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, JSONObject config) {
        String appKey = read(config, "appKey", "AppKey");
        String secret = read(config, "secret", "Secret");
        if (appKey == null || secret == null) {
            throw new BusinessException("企查查认证配置需要填写 AppKey 和 Secret");
        }
        String timespan = String.valueOf(Instant.now().getEpochSecond());
        requestBuilder.header("Token", appKey);
        requestBuilder.header("Timespan", timespan);
        requestBuilder.header("Sign", md5(appKey + timespan + secret));
    }

    private String read(JSONObject config, String... keys) {
        if (config == null) {
            return null;
        }
        for (String key : keys) {
            String value = config.getString(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String md5(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString().toUpperCase(Locale.ROOT);
        } catch (Exception exception) {
            throw new BusinessException("企查查签名生成失败");
        }
    }
}
