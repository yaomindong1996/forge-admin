package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

import java.util.List;

/**
 * 能力挂接批量保存参数。
 */
@Data
public class BusinessBindingBatchSaveDTO {

    private String targetType;

    private Long targetId;

    private String targetCode;

    private List<BusinessBindingDTO> bindings;
}
