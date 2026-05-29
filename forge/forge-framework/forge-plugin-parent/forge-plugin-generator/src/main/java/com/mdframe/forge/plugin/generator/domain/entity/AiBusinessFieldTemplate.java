package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务字段模板。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_field_template")
public class AiBusinessFieldTemplate extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String templateCode;

    private String templateName;

    private String suiteCode;

    private String fieldType;

    /** 字段协议 JSON */
    private String fieldSchema;

    /** 1-启用，0-禁用 */
    private Integer status;

    private Integer sortOrder;

    private String description;

    /** 扩展配置 JSON */
    private String options;
}
