package com.mdframe.forge.starter.message.config;

import java.util.List;

public interface SmsConfigProvider {

    SmsConfig getSmsConfig();

    default List<SmsConfig> listEnabledSmsConfigs() {
        SmsConfig config = getSmsConfig();
        return config == null ? List.of() : List.of(config);
    }

    default SmsRoutePolicy getRoutePolicy() {
        return SmsRoutePolicy.defaults();
    }

    class SmsConfig {
        private String configId;
        private String supplier;
        private String accessKeyId;
        private String accessKeySecret;
        private String signature;
        private String templateId;
        private String sceneTemplates;
        private Integer weight;
        private Integer retryInterval;
        private Integer maxRetries;
        private Integer maximum;
        private String extraConfig;
        private Integer dailyLimit;
        private Integer minuteLimit;
        private Integer status;
        private String channelRole;

        public String getConfigId() { return configId; }
        public void setConfigId(String configId) { this.configId = configId; }
        public String getSupplier() { return supplier; }
        public void setSupplier(String supplier) { this.supplier = supplier; }
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getSceneTemplates() { return sceneTemplates; }
        public void setSceneTemplates(String sceneTemplates) { this.sceneTemplates = sceneTemplates; }
        public Integer getWeight() { return weight; }
        public void setWeight(Integer weight) { this.weight = weight; }
        public Integer getRetryInterval() { return retryInterval; }
        public void setRetryInterval(Integer retryInterval) { this.retryInterval = retryInterval; }
        public Integer getMaxRetries() { return maxRetries; }
        public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
        public Integer getMaximum() { return maximum; }
        public void setMaximum(Integer maximum) { this.maximum = maximum; }
        public String getExtraConfig() { return extraConfig; }
        public void setExtraConfig(String extraConfig) { this.extraConfig = extraConfig; }
        public Integer getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
        public Integer getMinuteLimit() { return minuteLimit; }
        public void setMinuteLimit(Integer minuteLimit) { this.minuteLimit = minuteLimit; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getChannelRole() { return channelRole; }
        public void setChannelRole(String channelRole) { this.channelRole = channelRole; }
    }

    class SmsRoutePolicy {
        private String routeMode = "WEIGHTED_RR";
        private boolean captchaLockPrimary = true;
        private boolean failoverEnabled = true;
        private int failoverMaxAttempts = 2;

        public static SmsRoutePolicy defaults() {
            return new SmsRoutePolicy();
        }

        public String getRouteMode() { return routeMode; }
        public void setRouteMode(String routeMode) { this.routeMode = routeMode; }
        public boolean isCaptchaLockPrimary() { return captchaLockPrimary; }
        public void setCaptchaLockPrimary(boolean captchaLockPrimary) { this.captchaLockPrimary = captchaLockPrimary; }
        public boolean isFailoverEnabled() { return failoverEnabled; }
        public void setFailoverEnabled(boolean failoverEnabled) { this.failoverEnabled = failoverEnabled; }
        public int getFailoverMaxAttempts() { return failoverMaxAttempts; }
        public void setFailoverMaxAttempts(int failoverMaxAttempts) { this.failoverMaxAttempts = failoverMaxAttempts; }
    }
}
