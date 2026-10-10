package com.mdframe.forge.plugin.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_plugin_build")
public class SysPluginBuild extends TenantEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String workerId;
    @TableField(select = false)
    @ToString.Exclude
    private String leaseHash;
    private String phase;
    private LocalDateTime leaseExpiresTime;
    private LocalDateTime deadlineTime;
    private LocalDateTime startedTime;
    private LocalDateTime finishedTime;
    private String sourceCommit;
    private String image;
    private String resultJson;
    private String resultSha256;
    @TableLogic
    private Integer delFlag;
}
