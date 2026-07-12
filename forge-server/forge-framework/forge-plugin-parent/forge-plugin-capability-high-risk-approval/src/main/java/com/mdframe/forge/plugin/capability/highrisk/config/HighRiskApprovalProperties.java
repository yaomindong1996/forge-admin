package com.mdframe.forge.plugin.capability.highrisk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "forge.capability.high-risk")
public class HighRiskApprovalProperties {

    private boolean enabled;
    private String flowModelKey = "forge_capability_high_risk_approval";
    private Crypto crypto = new Crypto();

    @Data
    public static class Crypto {
        private String activeKeyId;
        private Map<String, String> keys = new LinkedHashMap<>();
    }
}
