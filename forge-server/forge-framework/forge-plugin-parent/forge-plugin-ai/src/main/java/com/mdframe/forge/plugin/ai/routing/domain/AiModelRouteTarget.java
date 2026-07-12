package com.mdframe.forge.plugin.ai.routing.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("ai_model_route_target")
public class AiModelRouteTarget extends TenantEntity {
    @TableId private Long id;
    private Long policyId;
    private Long modelId;
    private Integer priority;
    private String status;
    @TableLogic private String delFlag;
}
