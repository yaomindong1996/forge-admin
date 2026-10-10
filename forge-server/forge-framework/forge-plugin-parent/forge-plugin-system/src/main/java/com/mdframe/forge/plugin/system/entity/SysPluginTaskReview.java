package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 只追加、无删除接口；人工声明不能当作系统独立验证。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_plugin_task_review")
public class SysPluginTaskReview extends TenantEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String taskId;
    private String requestId;
    private String commandSha256;
    private Integer expectedRevision;
    private String packageSha256;
    private String resultSha256;
    private String decision;
    private String previousStatus;
    private String targetStatus;
    private Boolean executorStopped;
    private Boolean notDeployed;
    private Boolean artifactsReviewed;
    private Boolean migrationsReviewed;
    @ToString.Exclude
    private String note;
    @TableLogic
    private Integer delFlag;
}
