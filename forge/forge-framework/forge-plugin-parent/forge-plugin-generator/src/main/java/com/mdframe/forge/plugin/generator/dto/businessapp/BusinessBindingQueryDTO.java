package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 能力挂接查询参数。
 */
@Data
public class BusinessBindingQueryDTO {

    private String targetType;

    private Long targetId;

    private String targetCode;

    private String bindingType;

    private Integer status;
}
