package com.mdframe.forge.plugin.capability.controlplane.domain;

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
@TableName("ai_capability_client")
public class AiCapabilityClient extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String clientCode;
    private String clientName;
    private String keyId;
    private String keyPrefix;
    private String keyHash;
    private Integer credentialVersion;
    private Long serviceUserId;
    private Long activeOrgId;
    private Integer oauthEnabled;
    private String oauthClientType;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private String remark;
    @TableLogic
    private Integer delFlag;
}
