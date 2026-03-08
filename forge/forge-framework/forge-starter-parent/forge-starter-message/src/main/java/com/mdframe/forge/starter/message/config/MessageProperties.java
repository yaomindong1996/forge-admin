package com.mdframe.forge.starter.message.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "forge.message")
public class MessageProperties {

    /**
     * 默认渠道键（web/sms/other），当未指定时使用
     */
    private String defaultChannel = "web";

    /**
     * 渠道配置，如：forge.message.channel.web.enabled=true
     */
    private Map<String, ChannelConfig> channel;

    public static class ChannelConfig {
        private boolean enabled = true;
        private Map<String, String> config;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<String, String> getConfig() { return config; }
        public void setConfig(Map<String, String> config) { this.config = config; }
    }

    public String getDefaultChannel() { return defaultChannel; }
    public void setDefaultChannel(String defaultChannel) { this.defaultChannel = defaultChannel; }
    public Map<String, ChannelConfig> getChannel() { return channel; }
    public void setChannel(Map<String, ChannelConfig> channel) { this.channel = channel; }
}
