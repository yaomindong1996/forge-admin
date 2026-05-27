package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 能力挂接保存参数。
 */
@Data
public class BusinessBindingDTO {

    private Long id;

    private String targetType;

    private Long targetId;

    private String targetCode;

    private String bindingType;

    private String bindingKey;

    private String bindingName;

    private String bindingConfig;

    private String description;

    private Integer status;

    private Integer sortOrder;
}
