package com.mdframe.forge.plugin.ai.skill.pkg.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_skill_package")
public class SkillPackage extends TenantEntity {
    @TableId
    private Long id;
    private String packageCode;
    private String packageName;
    private String description;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
