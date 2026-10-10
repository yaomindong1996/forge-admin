package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PluginBuildClaimDTO extends PluginBuildLeaseDTO {
    @NotNull
    @Min(0)
    private Integer revision;
    @NotNull
    @Pattern(regexp = "[a-f0-9]{64}")
    private String sha256;
    @NotNull
    @Pattern(regexp = "[a-f0-9]{40}")
    private String sourceCommit;
    @NotNull
    @Pattern(regexp = "[a-z0-9][a-z0-9./_-]{0,128}@sha256:[a-f0-9]{64}")
    private String image;
}
