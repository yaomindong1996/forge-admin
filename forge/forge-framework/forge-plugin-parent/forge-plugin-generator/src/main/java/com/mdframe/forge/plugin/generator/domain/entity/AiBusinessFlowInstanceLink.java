package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 业务应用平台-单据流程实例关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_flow_instance_link")
public class AiBusinessFlowInstanceLink extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String objectCode;

    private Long recordId;

    private String businessKey;

    private String flowModelKey;

    private String processInstanceId;

    private String flowStatus;

    private Long startUserId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String result;

    /** 流程变量快照 JSON */
    private String variablesSnapshot;
}
