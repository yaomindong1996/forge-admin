package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台业务对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_object")
public class AiBusinessObject extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String suiteCode;

    private String objectCode;

    private String objectName;

    /** MASTER/DETAIL/LOOKUP/TRANSACTION */
    private String objectType;

    private Long modelId;

    private String modelCode;

    private String displayField;

    private String icon;

    private String description;

    /** 1-启用，0-禁用 */
    private Integer status;

    private Integer sortOrder;

    /** 扩展配置 JSON */
    private String options;
}
