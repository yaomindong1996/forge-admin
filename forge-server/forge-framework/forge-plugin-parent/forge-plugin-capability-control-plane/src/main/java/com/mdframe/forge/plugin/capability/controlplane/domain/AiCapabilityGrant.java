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
@TableName("ai_capability_grant")
public class AiCapabilityGrant extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private Long clientId;
    private Long capabilityId;
    private String versionStrategy;
    private String fixedVersion;
    private String fieldPolicy;
    private String status;
    private LocalDateTime expiresAt;
    @TableLogic
    private Integer delFlag;
}
