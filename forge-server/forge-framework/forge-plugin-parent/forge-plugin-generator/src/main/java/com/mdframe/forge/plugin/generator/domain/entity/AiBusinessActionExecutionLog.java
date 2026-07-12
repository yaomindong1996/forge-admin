package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务应用平台-通用动作执行日志。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_action_execution_log")
public class AiBusinessActionExecutionLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String suiteCode;

    private String objectCode;

    private String recordId;

    private String actionCode;

    private String actionName;

    private String executeStatus;

    private String requestDigest;

    private String stepResult;

    private String resultMessage;

    private String errorMessage;

    private String correlationId;

    private String idempotencyKey;

    private Long durationMs;

    private String capabilityRequestId;

    private Long clientId;

    private Long serviceUserId;

    private String actorType;
}
