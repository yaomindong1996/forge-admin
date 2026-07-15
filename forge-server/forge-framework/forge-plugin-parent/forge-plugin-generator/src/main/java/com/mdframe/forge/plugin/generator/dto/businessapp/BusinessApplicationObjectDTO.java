package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 应用内业务对象关联参数。
 */
@Data
public class BusinessApplicationObjectDTO {

    private Long objectId;

    private String objectRole;

    private Integer sortOrder;

    private String options;
}
