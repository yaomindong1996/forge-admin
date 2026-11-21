package com.mdframe.forge.plugin.ai.skill.pkg.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_skill_package_version")
public class SkillPackageVersion extends TenantEntity {
    @TableId
    private Long id;
    private Long packageId;
    private String versionName;
    private String packageHash;
    private String archiveHash;
    private String manifestJson;
    private String snapshotJson;
    private Integer fileCount;
    private Integer totalBytes;
    private String status;
    @TableField(exist = false)
    private String packageCode;
    @TableField(exist = false)
    private String packageName;
    @TableField(exist = false)
    private String description;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
