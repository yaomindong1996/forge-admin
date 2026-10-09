package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 追加审计，无更新/删除入口；既有审批被关闭后记录仍保留。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_plugin_artifact_registration")
public class SysPluginArtifactRegistration extends TenantEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String taskId;
    private String reviewId;
    private String requestId;
    private String commandSha256;
    private Integer expectedRevision;
    private String releaseId;
    private String manifestSha256;
    private String repositoryId;
    private String serverResultSha256;
    @ToString.Exclude
    private String metadataJson;
    private Boolean localVerified;
    private Boolean notDeployed;
    @ToString.Exclude
    private String note;
    @TableLogic
    private Integer delFlag;
}
