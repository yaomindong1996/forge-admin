package com.mdframe.forge.plugin.generator.service.businessapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 业务事件模型。
 * <p>
 * 当动态 CRUD 执行增删改操作时，由 BusinessEventPublisher 发布此事件，
 * 触发器引擎（BusinessTriggerExecutor）根据事件类型和条件匹配触发器并执行动作。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessEvent {

    /** 事件类型 */
    private String eventType;

    /** 业务套件编码 */
    private String suiteCode;

    /** 业务对象编码 */
    private String objectCode;

    /** 运行配置键 */
    private String configKey;

    /** 记录ID */
    private String recordId;

    /** 当前记录数据 */
    private Map<String, Object> recordData;

    /** 变更前数据（UPDATE/STATUS_CHANGED 时有值） */
    private Map<String, Object> previousData;

    /** 操作用户ID */
    private Long operatorId;

    /** 操作用户名称 */
    private String operatorName;

    /** 租户ID */
    private Long tenantId;

    // ========== 事件类型常量 ==========

    public static final String RECORD_CREATED = "RECORD_CREATED";
    public static final String RECORD_UPDATED = "RECORD_UPDATED";
    public static final String RECORD_DELETED = "RECORD_DELETED";
    public static final String STATUS_CHANGED = "STATUS_CHANGED";
    public static final String FIELD_CHANGED = "FIELD_CHANGED";
    public static final String FLOW_APPROVED = "FLOW_APPROVED";
    public static final String FLOW_REJECTED = "FLOW_REJECTED";
    public static final String FLOW_CANCELED = "FLOW_CANCELED";
}
