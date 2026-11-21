package com.mdframe.forge.plugin.ai.skill.pkg.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体与技能资料包固定版本的绑定关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent_skill_package")
public class AiAgentSkillPackage extends TenantEntity {
    @TableId
    private Long id;
    private Long agentId;
    private Long packageId;
    private Long versionId;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
