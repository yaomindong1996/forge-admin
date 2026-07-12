package com.mdframe.forge.plugin.ai.model.capability.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_capability")
public class AiModelCapability extends TenantEntity {
    @TableId private Long id;
    private Long modelId;
    private String capabilityCode;
    private String configJson;
    private String status;
    @TableLogic private String delFlag;
}
