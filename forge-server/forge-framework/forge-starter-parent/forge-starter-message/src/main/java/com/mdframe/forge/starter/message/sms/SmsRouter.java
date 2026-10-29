package com.mdframe.forge.starter.message.sms;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.starter.message.config.SmsConfigProvider;
import com.mdframe.forge.starter.message.config.SmsConfigProvider.SmsConfig;
import com.mdframe.forge.starter.message.config.SmsConfigProvider.SmsRoutePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 租户级短信通道选路：验证码锁主、PRIMARY_ONLY、加权轮询、Failover 顺序。
 */
@Slf4j
@RequiredArgsConstructor
public class SmsRouter {

    private final SmsConfigProvider configProvider;
    private final SmsRouteSequence routeSequence;

    public List<SmsConfig> resolveAttemptOrder(String scene, Long tenantIdHint) {
        List<SmsConfig> enabled = new ArrayList<>(configProvider.listEnabledSmsConfigs());
        if (enabled.isEmpty()) {
            return List.of();
        }
        SmsRoutePolicy policy = configProvider.getRoutePolicy();
        if (policy == null) {
            policy = SmsRoutePolicy.defaults();
        }

        SmsConfig primary = findPrimary(enabled);
        if (isCaptchaLocked(scene, policy) && primary != null) {
            return orderWithFirst(enabled, primary);
        }

        if ("PRIMARY_ONLY".equalsIgnoreCase(trim(policy.getRouteMode()))) {
            if (primary != null) {
                return List.of(primary);
            }
            return List.of(enabled.get(0));
        }

        SmsConfig chosen = chooseWeighted(enabled, tenantIdHint);
        return orderWithFirst(enabled, chosen);
    }

    public String resolveTemplateId(SmsConfig config, String scene, String requestTemplate) {
        if (hasText(requestTemplate)) {
            return requestTemplate;
        }
        if (config == null) {
            return null;
        }
        String mapped = readSceneTemplate(config.getSceneTemplates(), scene);
        if (hasText(mapped)) {
            return mapped;
        }
        return config.getTemplateId();
    }

    public int maxAttempts(SmsRoutePolicy policy, int channelCount) {
        if (policy == null || !policy.isFailoverEnabled()) {
            return 1;
        }
        int configured = Math.max(1, policy.getFailoverMaxAttempts());
        return Math.min(configured, Math.max(1, channelCount));
    }

    private boolean isCaptchaLocked(String scene, SmsRoutePolicy policy) {
        return policy.isCaptchaLockPrimary()
                && SmsScene.LOGIN_CAPTCHA.equalsIgnoreCase(trim(scene));
    }

    private SmsConfig findPrimary(List<SmsConfig> configs) {
        for (SmsConfig config : configs) {
            if ("PRIMARY".equalsIgnoreCase(trim(config.getChannelRole()))) {
                return config;
            }
        }
        return null;
    }

    private List<SmsConfig> orderWithFirst(List<SmsConfig> enabled, SmsConfig first) {
        List<SmsConfig> ordered = new ArrayList<>();
        if (first != null) {
            ordered.add(first);
        }
        for (SmsConfig config : enabled) {
            if (first != null && Objects.equals(first.getConfigId(), config.getConfigId())) {
                continue;
            }
            ordered.add(config);
        }
        return ordered;
    }

    private SmsConfig chooseWeighted(List<SmsConfig> enabled, Long tenantIdHint) {
        int totalWeight = 0;
        for (SmsConfig config : enabled) {
            totalWeight += normalizeWeight(config.getWeight());
        }
        if (totalWeight <= 0) {
            return enabled.get(0);
        }
        long tenantId = tenantIdHint == null ? 0L : tenantIdHint;
        long seq = Math.floorMod(routeSequence.next(tenantId), totalWeight);
        long cursor = 0;
        for (SmsConfig config : enabled) {
            cursor += normalizeWeight(config.getWeight());
            if (seq < cursor) {
                return config;
            }
        }
        return enabled.get(enabled.size() - 1);
    }

    private int normalizeWeight(Integer weight) {
        if (weight == null || weight < 1) {
            return 1;
        }
        return Math.min(weight, 1000);
    }

    private String readSceneTemplate(String sceneTemplatesJson, String scene) {
        if (!hasText(sceneTemplatesJson) || !hasText(scene)) {
            return null;
        }
        try {
            JSONObject object = JSON.parseObject(sceneTemplatesJson);
            if (object == null) {
                return null;
            }
            Object value = object.get(scene);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
