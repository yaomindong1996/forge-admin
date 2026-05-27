package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台能力挂接。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_binding")
public class AiBusinessBinding extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** SUITE/OBJECT/APP */
    private String targetType;

    private Long targetId;

    private String targetCode;

    /** FLOW/APPROVAL/REPORT/PERMISSION/MESSAGE/TRIGGER/IMPORT/EXPORT/MOBILE/INTEGRATION */
    private String bindingType;

    private String bindingKey;

    private String bindingName;

    /** 挂接配置 JSON */
    private String bindingConfig;

    private String description;

    /** 1-启用，0-禁用 */
    private Integer status;

    private Integer sortOrder;
}
