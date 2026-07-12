package com.mdframe.forge.plugin.capability.highrisk.domain;

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
@TableName("ai_capability_policy")
public class AiCapabilityPolicy extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long capabilityId;
    private String capabilityVersion;
    private String riskLevel;
    private String approvalFlowModelKey;
    private String approvalCandidateGroup;
    private Integer expireSeconds;
    private String status;
    @TableLogic
    private Integer delFlag;
}
