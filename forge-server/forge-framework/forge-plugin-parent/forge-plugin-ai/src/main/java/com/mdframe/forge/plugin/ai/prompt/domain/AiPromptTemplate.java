package com.mdframe.forge.plugin.ai.prompt.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * AI 提示词模板。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_prompt_template")
public class AiPromptTemplate extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String templateName;

    private String templateCode;

    private String usageScene;

    private String businessCategory;

    private String domainCategory;

    private String templateTags;

    private String description;

    private String promptContent;

    private String exampleInput;

    private String status;

    @TableLogic
    private String delFlag;

    private String isRecommended;

    private Integer sortOrder;

    private Integer useCount;

    private Integer testCount;

    private Integer downloadCount;

    private String remark;
}
