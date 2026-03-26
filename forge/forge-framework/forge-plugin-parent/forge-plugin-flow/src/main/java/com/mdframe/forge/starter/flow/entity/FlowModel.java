package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程模型实体
 */
@Data
@TableName("sys_flow_model")
public class FlowModel {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 模型标识（唯一）
     */
    private String modelKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 描述
     */
    private String description;

    /**
     * 分类
     */
    private String category;

    /**
     * 流程类型（leave-请假/expense-报销/approval-审批）
     */
    private String flowType;

    /**
     * 表单类型（dynamic-动态表单/custom-业务表单）
     */
    private String formType;

    /**
     * 表单ID（业务表单时使用）
     */
    private String formId;

    /**
     * 动态表单JSON配置
     */
    private String formJson;

    /**
     * BPMN流程定义XML
     */
    private String bpmnXml;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * Flowable流程定义ID
     */
    private String processDefinitionId;

    /**
     * Flowable部署ID
     */
    private String deploymentId;

    /**
     * 部署KEY（发布后生成）
     */
    private String deploymentKey;

    /**
     * 事件通知方式
     * <ul>
     *   <li>{@code none}    - 不通知（默认）</li>
     *   <li>{@code redis}   - Redis Pub/Sub，发布到 {@code flow:event:{modelKey}} 频道</li>
     *   <li>{@code webhook} - HTTP Webhook，POST 回调到 {@code webhookUrl}</li>
     * </ul>
     * <p>两种方式互斥，由流程模型配置决定。</p>
     */
    private String notifyType;

    /**
     * Webhook 回调地址
     * <p>仅当 {@code notifyType = webhook} 时生效。流程状态变更时，流程服务向此 URL 发送 POST 回调。
     * 请求体为 {@link FlowEventMessage} JSON，
     * 同时携带请求头：X-Flow-Event-Type / X-Flow-Process-Key / X-Flow-Business-Key。</p>
     */
    private String webhookUrl;

    /**
     * 状态（0-设计/1-已发布/2-已挂起/3-已禁用）
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime deployTime;

    /**
     * 最后修改人
     */
    private String lastUpdateBy;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标志（0-正常/1-删除）
     */
    @TableLogic
    private Integer delFlag;
}
