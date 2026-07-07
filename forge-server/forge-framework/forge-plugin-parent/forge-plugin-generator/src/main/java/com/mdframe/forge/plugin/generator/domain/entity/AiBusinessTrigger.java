package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 业务应用平台-触发器规则实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_trigger")
public class AiBusinessTrigger extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String suiteCode;

    private String objectCode;

    private String triggerName;

    private String triggerDesc;

    /** EVENT/SCHEDULE/MANUAL */
    private String triggerType;

    /** 业务场景模板类型 */
    private String scenarioType;

    /** ASYNC/SYNC_BLOCK */
    private String blockingMode;

    /** 1-开发者高级模式，0-业务结构化模式 */
    private Integer developerMode;

    /** RECORD_CREATED/RECORD_UPDATED/RECORD_DELETED/STATUS_CHANGED/FIELD_CHANGED */
    private String eventType;

    /** 事件条件表达式JSON */
    private String eventCondition;

    /** START_FLOW/SEND_MESSAGE/CREATE_RECORD/UPDATE_FIELD/WEBHOOK */
    private String actionType;

    /** 动作配置JSON */
    private String actionConfig;

    /** 1-启用，0-禁用 */
    private Integer status;

    private Integer sortOrder;

    private Long executeCount;

    private LocalDateTime lastExecuteTime;

    @TableLogic
    private String delFlag;
}
