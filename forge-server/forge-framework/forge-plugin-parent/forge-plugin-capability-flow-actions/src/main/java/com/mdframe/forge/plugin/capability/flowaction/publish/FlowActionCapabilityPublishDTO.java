package com.mdframe.forge.plugin.capability.flowaction.publish;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FlowActionCapabilityPublishDTO {

    @NotBlank
    private String capabilityCode;

    @NotBlank
    private String version;

    @NotBlank
    private String suiteCode;

    @NotBlank
    private String objectCode;

    @NotBlank
    private String operation;

    private String description;
}
