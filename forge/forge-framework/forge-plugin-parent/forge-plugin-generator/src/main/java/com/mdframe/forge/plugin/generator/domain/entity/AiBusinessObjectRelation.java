package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台对象关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_object_relation")
public class AiBusinessObjectRelation extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String suiteCode;

    private String sourceObjectCode;

    private String targetObjectCode;

    /** REFERENCE/DETAIL/CHILD_LIST/MANY_TO_MANY */
    private String relationType;

    private String relationName;

    private String sourceFieldCode;

    private String targetFieldCode;

    /** 关系配置 JSON */
    private String relationConfig;

    private String description;

    /** 1-启用，0-禁用 */
    private Integer status;

    private Integer sortOrder;
}
