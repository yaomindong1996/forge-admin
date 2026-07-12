package com.mdframe.forge.plugin.ai.routing.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data @EqualsAndHashCode(callSuper = true) @TableName("ai_model_route_policy")
public class AiModelRoutePolicy extends TenantEntity {
    @TableId private Long id;
    private String policyCode;
    private String policyName;
    private String requiredCapabilities;
    private String status;
    private String remark;
    @TableLogic private String delFlag;
}
