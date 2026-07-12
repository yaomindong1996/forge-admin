package com.mdframe.forge.plugin.ai.routing.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiModelRoutePolicyVO {
    private Long id;
    private String policyCode;
    private String policyName;
    private List<String> requiredCapabilities;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private List<Target> targets;

    @Data
    public static class Target {
        private Long id;
        private Long policyId;
        private Long modelId;
        private String modelName;
        private String providerName;
        private Integer priority;
        private String status;
    }
}
