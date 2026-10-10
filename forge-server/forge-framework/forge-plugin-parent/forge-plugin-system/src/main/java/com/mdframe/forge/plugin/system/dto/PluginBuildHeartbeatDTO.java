package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PluginBuildHeartbeatDTO extends PluginBuildLeaseDTO {
    @NotNull
    @Pattern(regexp = "source_snapshot|package_preflight|source_preflight|container_build|artifact_verification")
    private String phase;
}
