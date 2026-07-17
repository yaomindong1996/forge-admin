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
 * 结构化编码规则分段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_code_rule_segment")
public class AiCodeRuleSegment extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    /** 稳定分段键；排序变化时保持不变，并参与序列 key。 */
    private String segmentKey;

    private Integer segmentOrder;

    /** DATE/FIXED/SEQ/VARIABLE/SYS_VAR。 */
    private String segmentType;

    /** 日期格式、固定值、变量名或系统变量名。 */
    private String segmentValue;

    /** VARIABLE 的取值来源：CUSTOM/LOWCODE。 */
    private String variableSource;

    private Integer segmentLength;

    private Integer padEnabled;

    private String padChar;

    private String padDirection;

    private Integer groupEnabled;

    private Integer includeInCode;

    private String radixType;

    private Integer resetEnabled;

    private String resetPolicy;

    private Long startValue;

    private Integer excludeAmbiguous;

    @TableLogic
    private String delFlag;
}
