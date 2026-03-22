package com.mdframe.forge.starter.flow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 流程审批节点配置实体
 * 用于存储审批节点的详细配置信息
 */
@Data
@TableName("sys_flow_node_config")
public class FlowNodeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 流程模型ID
     */
    private String modelId;

    /**
     * 节点ID（BPMN中的节点标识）
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型：approval-审批节点, service-服务节点, cc-抄送节点
     */
    private String nodeType;

    // ==================== 审批人配置 ====================

    /**
     * 审批人类型：
     * user-指定用户
     * role-按角色
     * dept-按部门
     * post-按岗位
     * leader-上级领导
     * deptManager-部门负责人
     * initiator-发起人
     * expr-表达式
     */
    private String assigneeType;

    /**
     * 审批人值（JSON格式，存储用户ID、角色编码等）
     */
    private String assigneeValue;

    /**
     * 审批人表达式（UEL表达式）
     */
    private String assigneeExpr;

    // ==================== 多人审批策略 ====================

    /**
     * 多实例类型：
     * none-单人审批
     * sequential-依次审批
     * parallel-并行会签
     */
    private String multiInstanceType;

    /**
     * 完成条件：
     * all-全部通过
     * any-任一通过
     * rate-按比例通过
     */
    private String completionCondition;

    /**
     * 通过比例（0-100）
     */
    private BigDecimal passRate;

    // ==================== 超时设置 ====================

    /**
     * 超时天数
     */
    private Integer dueDateDays;

    /**
     * 超时小时数
     */
    private Integer dueDateHours;

    /**
     * 超时动作：
     * auto_pass-自动通过
     * auto_reject-自动拒绝
     * notify-发送通知
     * none-无动作
     */
    private String timeoutAction;

    /**
     * 超时通知人（JSON格式）
     */
    private String timeoutNotifyUsers;

    // ==================== 审批操作权限 ====================

    /**
     * 允许转办
     */
    private Boolean allowDelegate;

    /**
     * 允许转交
     */
    private Boolean allowTransfer;

    /**
     * 允许加签
     */
    private Boolean allowAddSign;

    /**
     * 允许减签
     */
    private Boolean allowCounterSign;

    /**
     * 允许驳回
     */
    private Boolean allowReject;

    /**
     * 允许驳回至发起人
     */
    private Boolean allowRejectToStart;

    /**
     * 允许撤回
     */
    private Boolean allowWithdraw;

    // ==================== 其他配置 ====================

    /**
     * 表单Key
     */
    private String formKey;

    /**
     * 优先级（0-100）
     */
    private Integer priority;

    /**
     * 任务监听器配置（JSON格式）
     */
    private String taskListeners;

    /**
     * 扩展配置（JSON格式）
     */
    private String extConfig;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;

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
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
