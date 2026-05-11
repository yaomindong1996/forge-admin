package com.mdframe.forge.plugin.external.strategy.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.strategy.ExternalAuthStrategy;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.net.http.HttpRequest;

@Component
public class CurrentTokenAuthStrategy implements ExternalAuthStrategy {

    @Override
    public String getAuthType() {
        return "CurrentToken";
    }

    @Override
    public void applyAuth(HttpRequest.Builder requestBuilder, String authConfig) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("当前请求没有登录上下文，无法透传用户Token");
        }
        JSONObject config = authConfig == null || authConfig.isEmpty() ? new JSONObject() : JSON.parseObject(authConfig);
        String header = defaultValue(config.getString("tokenHeader"), "Authorization");
        String prefix = defaultValue(config.getString("tokenPrefix"), "Bearer");
        String token = StpUtil.getTokenValue();
        requestBuilder.header(header, prefix.isEmpty() ? token : prefix + " " + token);
    }

    @Override
    public boolean validateConfig(String authConfig) {
        return true;
    }

    private String defaultValue(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
