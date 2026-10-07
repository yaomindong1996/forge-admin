package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 固定查询协议，无调用方租户/用户身份字段。 */
@Data
public class SysPluginQuery {
    @Min(1)
    private int pageNum = 1;

    @Min(1)
    @Max(100)
    private int pageSize = 15;

    @Size(max = 100)
    private String keyword;

    @Pattern(regexp = "builtin|external")
    private String origin;
}
