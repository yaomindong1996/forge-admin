package com.mdframe.forge.plugin.ai.skill.pkg.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_skill_package_file")
public class SkillPackageFile extends TenantEntity {
    @TableId
    private Long id;
    private Long versionId;
    private String filePath;
    private String content;
    private String contentHash;
    private Integer sizeBytes;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
