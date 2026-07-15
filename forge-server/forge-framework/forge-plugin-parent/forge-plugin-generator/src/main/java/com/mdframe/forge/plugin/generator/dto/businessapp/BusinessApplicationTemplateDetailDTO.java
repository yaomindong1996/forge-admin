package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 主子表模板中的单个明细对象配置。
 */
@Data
public class BusinessApplicationTemplateDetailDTO {

    private BusinessApplicationTemplateObjectSourceDTO source;

    private String objectName;

    private String objectCode;

    private String foreignKeyField;

    private String relationName;
}
