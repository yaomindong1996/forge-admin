package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PluginBuildResultDTO {
    @NotNull
    private Boolean success;
    @NotNull
    @Pattern(regexp = "job-[a-zA-Z0-9_-]{1,64}")
    private String jobId;
    @NotNull
    @Pattern(regexp = "[a-f0-9]{64}")
    private String packageSha256;
    @NotNull
    @Pattern(regexp = "[a-f0-9]{40}")
    private String sourceCommit;
    @NotNull
    @Pattern(regexp = "[a-z0-9][a-z0-9./_-]{0,128}@sha256:[a-f0-9]{64}")
    private String image;
    @NotNull
    @Pattern(regexp = "source_snapshot|package_preflight|source_preflight|container_build|artifact_verification")
    private String phase;
    @Pattern(regexp = "[A-Z][A-Z0-9_]{0,95}")
    private String failureCode;
    @Pattern(regexp = "[a-f0-9]{64}")
    private String sourceSha256;
    @Min(1)
    @Max(4096)
    private Integer artifactCount;
    @Min(1)
    @Max(1073741824L)
    private Long artifactBytes;
    @Pattern(regexp = "[a-f0-9]{64}")
    private String artifactManifestSha256;
}
