package com.mdframe.forge.plugin.capability.controlplane.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_capability_invocation_log")
public class AiCapabilityInvocationLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String requestId;
    private Long clientId;
    private String clientCode;
    private Long capabilityId;
    private String capabilityCode;
    private String capabilityVersion;
    private String actorType;
    private Long actorUserId;
    private Long serviceUserId;
    private Long activeOrgId;
    private String resultStatus;
    private String resultCode;
    private String errorCode;
    private String schemaPath;
    private String traceId;
    private Long durationMs;
    @TableLogic
    private Integer delFlag;
}
