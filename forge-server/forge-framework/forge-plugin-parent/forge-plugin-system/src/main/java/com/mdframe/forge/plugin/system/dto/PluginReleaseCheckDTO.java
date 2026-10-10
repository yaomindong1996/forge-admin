package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 只读核验，不接受身份、路径、命令或部署目标。候选清单摘要仅绑定本次请求。 */
@Data
public class PluginReleaseCheckDTO {
    @NotNull
    @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    private String checkId;
    @NotNull
    @Pattern(regexp = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
    private String reviewId;
    @NotNull @Min(1)
    private Integer revision;
    @NotNull @Pattern(regexp = "[a-f0-9]{64}")
    private String serverResultSha256;
    @NotNull @Pattern(regexp = "[a-f0-9]{64}")
    private String manifestSha256;
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
}
