package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 仅更新规则状态，携带页面旧状态防止并发覆盖。 */
@Data
public class DataScopeConfigStatusDTO {
    @NotNull(message = "规则 ID 不能为空")
    private Long id;
    @NotNull(message = "目标状态不能为空")
    @Min(0)
    @Max(1)
    private Integer enabled;
    @NotNull(message = "原状态不能为空，请刷新列表")
    @Min(0)
    @Max(1)
    private Integer expectedEnabled;
}
