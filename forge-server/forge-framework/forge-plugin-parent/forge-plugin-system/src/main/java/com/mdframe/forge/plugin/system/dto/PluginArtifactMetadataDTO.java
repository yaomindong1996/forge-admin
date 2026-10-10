package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 小型、无路径的人工登记元数据；manifest摘要并非平台读取实际清单后的认证。 */
@Data
public class PluginArtifactMetadataDTO {
    @NotNull @Min(1) @Max(1)
    private Integer protocolVersion;
    @NotNull @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    private String taskId;
    @NotNull @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    private String reviewId;
    @NotNull @Min(1)
    private Integer revision;
    @NotNull @Pattern(regexp = "[a-f0-9]{64}")
    private String serverResultSha256;
    @NotNull @Pattern(regexp = "rel-[a-f0-9]{64}")
    private String releaseId;
    @NotNull @Pattern(regexp = "[a-f0-9]{64}")
    private String manifestSha256;
    @NotNull @Pattern(regexp = "[a-z][a-z0-9-]{0,63}")
    private String repositoryId;
    @NotNull @Pattern(regexp = "[a-f0-9]{64}")
    private String resultSha256;
    @NotNull @Pattern(regexp = "[a-z][a-z0-9-]{1,31}")
    private String pluginId;
    @NotNull @Size(min = 1, max = 128)
    private String pluginVersion;
    @NotNull @Size(min = 1, max = 128)
    private String coreVersion;
    @NotNull @Pattern(regexp = "install|replace")
    private String operation;
    @Valid @NotNull
    private PluginBuildResultDTO result;

    public PluginReleaseCheckDTO approvalCommand() {
        var command = new PluginReleaseCheckDTO();
        command.setReviewId(reviewId);
        command.setRevision(revision);
        command.setServerResultSha256(serverResultSha256);
        command.setManifestSha256(manifestSha256);
        command.setPluginId(pluginId);
        command.setPluginVersion(pluginVersion);
        command.setCoreVersion(coreVersion);
        command.setOperation(operation);
        command.setResult(result);
        return command;
    }
}
