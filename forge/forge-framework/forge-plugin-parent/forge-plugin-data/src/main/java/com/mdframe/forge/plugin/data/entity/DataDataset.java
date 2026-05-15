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

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long categoryId;

    @TableField(exist = false)
    private String connectionName;

    @TableField(exist = false)
    private String categoryCode;

    @TableField(exist = false)
    private String categoryName;

    private String datasetType;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tableName;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String sqlText;

    private String paramSchemaJson;

    private String defaultOrderJson;

    private Integer maxRows;

    private Integer timeoutSeconds;

    private Integer cacheEnabled;

    private Integer cacheTtlSeconds;

    private Integer status;

    private Integer publishStatus;

    private String accessMode;

    private String description;

    private Long createDept;
}
