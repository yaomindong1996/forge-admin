package com.mdframe.forge.plugin.ai.routing.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiModelRoutePolicySaveDTO {
    private Long id;
    private String policyCode;
    private String policyName;
    private List<String> requiredCapabilities;
    private String status;
    private String remark;
    private List<Target> targets;
    @Data public static class Target { private Long modelId; private Integer priority; private String status; }
}
