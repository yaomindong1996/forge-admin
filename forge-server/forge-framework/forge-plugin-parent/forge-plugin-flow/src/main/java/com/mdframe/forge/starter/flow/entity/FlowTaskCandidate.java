package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程任务候选关系。候选用户/候选组以关系行保存，逗号字段仅作为迁移兼容回退。
 */
@Data
@TableName("sys_flow_task_candidate")
public class FlowTaskCandidate {

    public static final String TYPE_USER = "USER";
    public static final String TYPE_GROUP = "GROUP";
    public static final String SOURCE_FLOWABLE = "FLOWABLE";
    public static final String SOURCE_DYNAMIC_SIGN = "DYNAMIC_SIGN";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** Flowable taskId，而非本地任务表主键。 */
    private String taskId;

    /** 动态加签的父任务；Flowable 原生候选关系为空。 */
    private String parentTaskId;

    /** Flowable 多实例加签产生的子任务和执行，普通候选关系为空。 */
    private String childTaskId;

    private String childExecutionId;

    private String processInstanceId;

    private String candidateType;

    private String candidateValue;

    private String source;

    /** BEFORE/AFTER/PARALLEL，仅用于关系审计和后续编排。 */
    private String signMode;

    private String operatorId;

    private String reason;

    /** 动态加签/减签请求幂等键，不向普通查询响应暴露。 */
    private String idempotencyKey;

    /** 幂等键对应的规范请求摘要。 */
    private String requestDigest;

    /** 1 有效，0 已移除。 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
