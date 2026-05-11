package com.mdframe.forge.plugin.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_data_dataset_field")
public class DataDatasetField extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long datasetId;

    private String fieldName;

    private String fieldLabel;

    private String sourceColumn;

    private String dbType;

    private String dataType;

    private String fieldRole;

    private String defaultAgg;

    private Integer queryEnabled;

    private Integer displayEnabled;

    private String sensitiveLevel;

    private String maskRule;

    private String dictType;

    private Integer sort;

    private String description;

    private Long createDept;
}