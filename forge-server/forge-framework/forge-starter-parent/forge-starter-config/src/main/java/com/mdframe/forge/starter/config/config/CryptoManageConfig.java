package com.mdframe.forge.starter.config.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mdframe.forge.starter.core.context.CryptoProperties;
import com.mdframe.forge.starter.core.util.CryptoDeploymentSecretPolicy;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置中心可维护的加密配置。
 * 部署级密钥字段不属于管理 API 协议，必须由环境变量或外部配置注入。
 */
@Data
public class CryptoManageConfig {

    private Boolean enabled = true;
    private String algorithm = "SM4";
    private Boolean enableDynamicKey = true;
    private Long sessionKeyExpire = 7200L;
    private Boolean enableApiCrypto = true;
    private Boolean enableFieldCrypto = true;
    private Boolean enableReplayProtection = false;
    private Long replayTimeWindow = 300L;
    private List<String> replayIncludePaths = new ArrayList<>();
    private List<String> replayExcludePaths = new ArrayList<>();
    private List<String> excludePaths = new ArrayList<>();
    private Boolean enableDesensitize = true;

    @JsonIgnore
    private final Map<String, Object> unknownProperties = new LinkedHashMap<>();

    public static CryptoManageConfig from(CryptoProperties properties) {
        CryptoManageConfig config = new CryptoManageConfig();
        if (properties == null) {
            return config;
        }
        config.setEnabled(properties.getEnabled());
        config.setAlgorithm(properties.getAlgorithm());
        config.setEnableDynamicKey(properties.getEnableDynamicKey());
        config.setSessionKeyExpire(properties.getSessionKeyExpire());
        config.setEnableApiCrypto(properties.getEnableApiCrypto());
        config.setEnableFieldCrypto(properties.getEnableFieldCrypto());
        config.setEnableReplayProtection(properties.getEnableReplayProtection());
        config.setReplayTimeWindow(properties.getReplayTimeWindow());
        config.setReplayIncludePaths(copyList(properties.getReplayIncludePaths()));
        config.setReplayExcludePaths(copyList(properties.getReplayExcludePaths()));
        config.setExcludePaths(copyList(properties.getExcludePaths()));
        config.setEnableDesensitize(properties.getEnableDesensitize());
        return config;
    }

    @JsonAnySetter
    public void captureUnknownProperty(String name, Object value) {
        unknownProperties.put(name, value);
    }

    public boolean hasDeploymentSecretFields() {
        return unknownProperties.keySet().stream()
                .anyMatch(CryptoDeploymentSecretPolicy::isDeploymentSecretJsonField);
    }

    private static List<String> copyList(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
