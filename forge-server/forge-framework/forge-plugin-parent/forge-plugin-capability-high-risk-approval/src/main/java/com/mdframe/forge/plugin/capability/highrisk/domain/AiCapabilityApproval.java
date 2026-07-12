package com.mdframe.forge.plugin.capability.highrisk.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_capability_approval")
public class AiCapabilityApproval extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String requestId;
    private Long clientId;
    private Integer credentialVersion;
    private Long capabilityId;
    private String capabilityCode;
    private String capabilityVersion;
    private Long actorUserId;
    private Long serviceUserId;
    private Long activeOrgId;
    private String idempotencyKey;
    private String requestDigest;
    private String businessStateDigest;
    private String keyId;
    private String wrappedDek;
    private String payloadIv;
    private String payloadCiphertext;
    private String payloadAuthTag;
    private String flowModelKey;
    private String processInstanceId;
    private String executeStatus;
    private String resultCode;
    private String resultSnapshot;
    private String errorCode;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;
    @TableLogic
    private Integer delFlag;
}
