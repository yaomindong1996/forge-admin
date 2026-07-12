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
@TableName("ai_capability_version")
public class AiCapabilityVersion extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private Long capabilityId;
    private String version;
    private String inputSchema;
    private String outputSchema;
    private String sourceType;
    private String sourceKey;
    private String sourceVersion;
    private String behavior;
    private String riskLevel;
    private String visibility;
    private String policySnapshot;
    private String schemaChecksum;
    private String status;
    @TableLogic
    private Integer delFlag;
}
