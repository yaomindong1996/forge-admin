package com.mdframe.forge.report.ai.agent.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent")
public class AiAgent extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String agentName;
    private String agentCode;
    private String description;
    private String systemPrompt;
    private Long providerId;
    private String modelName;
    private BigDecimal temperature;
    private Integer maxTokens;
    private String extraConfig;
    private String status;
}
