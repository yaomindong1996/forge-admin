package com.mdframe.forge.plugin.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_data_business_definition")
public class DataBusinessDefinition extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String businessCode;

    private String businessName;

    private String businessDesc;

    private String analysisGoal;

    private String metricDefinition;

    private String dimensionDefinition;

    private String usageGuide;

    private Integer status;

    @TableLogic
    private String delFlag;

    @TableField(exist = false)
    private Integer datasetCount;

    @TableField(exist = false)
    private String datasetNames;
}
