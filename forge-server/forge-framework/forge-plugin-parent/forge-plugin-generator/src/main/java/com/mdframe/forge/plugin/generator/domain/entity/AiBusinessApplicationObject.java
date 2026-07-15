package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 应用与业务对象关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_application_object")
public class AiBusinessApplicationObject extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long applicationId;

    private Long objectId;

    /** PRIMARY/DETAIL/REFERENCE/SHARED。 */
    private String objectRole;

    private Integer sortOrder;

    /** 应用内对象展示配置 JSON。 */
    private String options;

    @TableLogic
    private String delFlag;
}
