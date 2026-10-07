package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 客户端只能确认预览版本，不得指定工作区、租户或执行命令。 */
@Data
public class SysPluginTaskCommandDTO {
    @NotNull
    @Min(0)
    private Integer revision;
    @NotNull
    @Pattern(regexp = "[a-f0-9]{64}")
    private String sha256;
}
