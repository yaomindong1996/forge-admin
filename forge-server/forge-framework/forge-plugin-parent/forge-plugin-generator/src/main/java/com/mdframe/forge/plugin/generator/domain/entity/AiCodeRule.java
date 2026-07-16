package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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

    /** 编码分类，对应 sys_code_rule_category 字典。 */
    private String category;

    /** VARIABLE 分段设计时引用的低代码业务对象。 */
    private Long sourceObjectId;

    /** 低代码业务对象稳定编码，用于运行时匹配。 */
    private String sourceObjectCode;

    private String template;

    private String resetPolicy;

    private Integer seqLength;

    /** 1-启用，0-停用 */
    private Integer status;

    /** 1-内置规则，0-用户自定义 */
    private Integer builtin;

    /** 1-可在业务字段配置中选择，0-仅保留兼容或内部调用。 */
    private Integer inCodeList;

    /** 乐观锁版本号。 */
    @Version
    private Integer versionNo;

    private String remark;

    private String options;

    @TableLogic
    private String delFlag;
}
