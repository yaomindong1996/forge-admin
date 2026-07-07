package com.mdframe.forge.plugin.data.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_data_dataset_category")
public class DataDatasetCategory extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long parentId;

    private Integer level;

    private String ancestors;

    private String categoryCode;

    private String categoryName;

    private Integer sortOrder;

    private Integer status;

    @TableLogic
    private String delFlag;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    @TableField(exist = false)
    private List<DataDatasetCategory> children;
}
