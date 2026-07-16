package com.mdframe.forge.plugin.generator.dto.businessapp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 编码规则启停协议。
 */
@Data
public class CodeRuleStatusDTO {

    @NotNull(message = "规则ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;

    @NotNull(message = "版本号不能为空")
    private Integer versionNo;
}
