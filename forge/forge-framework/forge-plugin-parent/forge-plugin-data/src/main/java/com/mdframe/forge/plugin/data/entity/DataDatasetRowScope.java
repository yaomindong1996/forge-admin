package com.mdframe.forge.plugin.data.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_data_dataset_row_scope")
public class DataDatasetRowScope extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long datasetId;

    private Integer enabled;

    private String scopeMode;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tenantColumn;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String orgColumn;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String userColumn;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String regionColumn;

    private String regionStrategy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;
}
