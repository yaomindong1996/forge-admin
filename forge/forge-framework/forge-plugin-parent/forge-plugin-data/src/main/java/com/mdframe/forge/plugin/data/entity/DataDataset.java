package com.mdframe.forge.plugin.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_data_dataset")
public class DataDataset extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String datasetCode;

    private String datasetName;

    private Long connectionId;

    private String datasetType;

    private String tableName;

    private String sqlText;

    private String paramSchemaJson;

    private String defaultOrderJson;

    private Integer maxRows;

    private Integer timeoutSeconds;

    private Integer cacheEnabled;

    private Integer cacheTtlSeconds;

    private Integer status;

    private String description;

    private Long createDept;
}