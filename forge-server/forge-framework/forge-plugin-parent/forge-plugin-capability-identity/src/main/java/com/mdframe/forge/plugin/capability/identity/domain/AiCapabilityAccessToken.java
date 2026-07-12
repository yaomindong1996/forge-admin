package com.mdframe.forge.plugin.capability.identity.domain;

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
@TableName("ai_capability_access_token")
public class AiCapabilityAccessToken extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String tokenKeyId;
    private String tokenPrefix;
    private String tokenHash;
    private Long clientId;
    private Integer credentialVersion;
    private String actorType;
    private Long actorUserId;
    private Long serviceUserId;
    private Long activeOrgId;
    private String audience;
    private String scopes;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    @TableLogic
    private Integer delFlag;
}
