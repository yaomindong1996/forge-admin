package com.mdframe.forge.plugin.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job_api_token")
public class SysJobApiToken extends TenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String callerName;
    private String callerDescription;
    private String tokenKeyId;
    private String tokenPrefix;
    private String tokenHash;
    private String scopes;
    private String resourceJobIds;
    private String resourceJobGroups;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    private String remark;

    @TableLogic
    private Integer delFlag;
}
