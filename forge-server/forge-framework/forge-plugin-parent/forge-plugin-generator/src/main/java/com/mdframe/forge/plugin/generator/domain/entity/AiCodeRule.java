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
 * 通用编码生成规则。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_code_rule")
public class AiCodeRule extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;

    private String ruleName;

    private String scene;

    private String template;

    private String resetPolicy;

    private Integer seqLength;

    /** 1-启用，0-停用 */
    private Integer status;

    /** 1-内置规则，0-用户自定义 */
    private Integer builtin;

    private String remark;

    private String options;

    @TableLogic
    private String delFlag;
}
