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
@TableName("ai_capability")
public class AiCapability extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String capabilityCode;
    private String protocolToolName;
    private String capabilityName;
    private String description;
    private String sourceType;
    private String sourceKey;
    private String sourceVersion;
    private String currentVersion;
    private String schemaChecksum;
    private String behavior;
    private String riskLevel;
    private String visibility;
    private String publishStatus;
    private Integer enabled;
    @TableLogic
    private Integer delFlag;
}
