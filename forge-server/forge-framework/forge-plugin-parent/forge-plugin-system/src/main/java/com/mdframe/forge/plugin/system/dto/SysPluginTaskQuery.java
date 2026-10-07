package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysPluginTaskQuery {
    @NotNull
    @Min(1)
    private Integer pageNum = 1;
    @NotNull
    @Min(1)
    @Max(100)
    private Integer pageSize = 15;
    @Size(max = 100)
    private String keyword;
    @Pattern(regexp = "await_confirmation|blocked|queued|cancelled")
    private String status;
}
