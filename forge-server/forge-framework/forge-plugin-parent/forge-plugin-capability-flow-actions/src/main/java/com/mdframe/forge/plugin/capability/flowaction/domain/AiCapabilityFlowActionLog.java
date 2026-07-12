package com.mdframe.forge.plugin.capability.flowaction.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_capability_flow_action_log")
public class AiCapabilityFlowActionLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String requestId;
    private Long clientId;
    private Long capabilityId;
    private String capabilityCode;
    private String capabilityVersion;
    private String operation;
    private String objectCode;
    private String recordId;
    private String taskRef;
    private String idempotencyKey;
    private String requestDigest;
    private String actorType;
    private Long actorUserId;
    private Long serviceUserId;
    private Long activeOrgId;
    private String executeStatus;
    private String resultCode;
    private String resultSnapshot;
    private String errorCode;
    private Long durationMs;

    @TableLogic
    private Integer delFlag;
}
