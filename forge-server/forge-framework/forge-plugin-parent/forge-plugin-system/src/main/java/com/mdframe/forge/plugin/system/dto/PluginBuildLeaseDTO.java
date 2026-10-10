package com.mdframe.forge.plugin.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

@Data
public class PluginBuildLeaseDTO {
    @NotNull
    @Pattern(regexp = "[a-f0-9]{64}")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String leaseToken;
}
