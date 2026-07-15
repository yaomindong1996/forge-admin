package com.mdframe.forge.plugin.generator.dto.businessapp;

import lombok.Data;

/**
 * 应用模板中的对象资产来源。
 */
@Data
public class BusinessApplicationTemplateObjectSourceDTO {

    /** DATABASE_TABLE、EXISTING_OBJECT。 */
    private String sourceType;

    private Long objectId;

    private Long datasourceId;

    private String tableName;
}
