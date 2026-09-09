package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.core.domain.FlowEventMessage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_flow_model")
public class FlowModel {
    
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 租户ID
     */
    private Long tenantId;
    
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
     * 分类名称（列表展示用）
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 流程类型（leave-请假/expense-报销/approval-审批）
     */
    private String flowType;

    /**
     * 设计器类型（approval-审批流程/business-业务流程）
     */
    private String designerType;
    
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
     * 待办卡片详情深链模板（企业协同待办卡片点击跳转地址）。
     * <p>为空时使用全局默认待办详情页；配置后按流程模型覆盖，支持占位符：
     * {@code {taskId}}、{@code {businessKey}}、{@code {processInstanceId}}（自动 URL 编码）。
     * 可填相对路径（形如 {@code /#/pages/xxx?bizKey={businessKey}}，自动拼接连接的 H5 域名）
     * 或完整 http/https 地址。</p>
     */
    private String todoDetailUrlTemplate;

    /**
     * 是否允许审批人从当前流程历史用户任务中选择任意节点退回。
     */
    private Boolean allowMultiReturn;

    /**
     * 通知配置（事件×渠道矩阵）。
     * <p>JSON 结构，key 为事件类型（todo-新待办 / result-审批结果 / cc-抄送），
     * value 为 {@code {"channels": ["WEB","EMAIL","SMS","COLLABORATION"], "templateCode": "模板编码覆盖"}}。</p>
     * <p>渠道列表：WEB-站内信（基础渠道）/ EMAIL-邮件 / SMS-短信 / COLLABORATION-企业协同（企微等，按连接平台路由）。
     * {@code templateCode} 为空时按事件使用默认模板编码。</p>
     * <p>未配置（NULL）时保持默认通知行为：待办推站内信 + 连接开启待办推送时推企微卡片。</p>
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String notifyConfig;

    /**
     * 状态，取值见 {@link com.mdframe.forge.starter.flow.enums.FlowModelStatus}。
     */
    private Integer status;
    
    
    private Integer importanceLevel;

    /** 模型目录排序值，数值越小越靠前。 */
    private Integer sortOrder;
    
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
