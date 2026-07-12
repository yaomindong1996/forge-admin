package com.mdframe.forge.plugin.capability.secureaction.publish;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class BusinessActionCapabilityPublishDTO {

    private String suiteCode;

    @NotBlank
    private String objectCode;

    @NotBlank
    private String actionCode;

    @NotBlank
    private String capabilityCode;

    private String version = "1.0.0";

    private String description;

    @NotEmpty
    private Set<String> allowedFields = new LinkedHashSet<>();

    private Set<String> requiredFields = new LinkedHashSet<>();
}
