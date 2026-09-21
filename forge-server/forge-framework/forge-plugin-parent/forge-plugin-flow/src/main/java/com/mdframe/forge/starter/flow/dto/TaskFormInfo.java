package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 任务表单信息DTO
 * 用于返回待办任务的表单配置信息
 */
@Data
public class TaskFormInfo {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务定义Key
     */
    private String taskDefKey;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义Key
     */
    private String processDefKey;

    /**
     * 业务Key
     */
    private String businessKey;

    /**
     * 任务标题
     */
    private String title;

    /** Current local task mirror status, returned after visibility validation. */
    private Integer status;

    /** Current assignee; blank means the task is still unclaimed. */
    private String assignee;

    /** Legacy candidate-user snapshot used by the business-form access check. */
    private String candidateUsers;

    /** Legacy candidate-group snapshot used by the business-form access check. */
    private String candidateGroups;

    /**
     * 表单类型：dynamic-动态表单, external-外部表单
     */
    private String formType;

    /**
     * 动态表单JSON配置（formType=dynamic时有值）
     */
    private String formJson;

    /**
     * 业务表单模式：BUSINESS_OBJECT_FORM/BUSINESS_CODE_FORM/EXTERNAL
     */
    private String formMode;

    /**
     * 外部表单URL（formType=external时有值）
     */
    private String formUrl;

    /**
     * 外置表单打开方式：modal-弹窗, newTab-新标签页, redirect-当前页跳转
     */
    private String formTarget;

    /**
     * 表单Key
     */
    private String formKey;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 代码表单 Provider Key
     */
    private String providerKey;

    /**
     * 业务表单视图 Key
     */
    private String viewKey;

    /**
     * 结构化表单引用
     */
    private Map<String, Object> formRef;

    /**
     * 节点表单字段权限配置JSON
     */
    private String formFieldPermissions;

    /**
     * 节点打印模板策略：INHERIT-继承应用场景绑定，RESTRICT-限制为 printTemplateIds 子集。
     */
    private String printTemplatePolicy;

    /**
     * 节点允许的打印模板 ID，逗号分隔；只在 RESTRICT 策略下生效。
     */
    private String printTemplateIds;

    /**
     * 流程变量（表单数据）
     */
    private Map<String, Object> variables;

    /**
     * 流程表单实例ID
     */
    private Long formInstanceId;

    /**
     * 提交时表单Schema快照
     */
    private String schemaSnapshot;

    /**
     * 提交时表单数据快照（JSON）
     */
    private String formData;

    /**
     * 数据模式：PROCESS_ONLY/BUSINESS_OBJECT/HYBRID
     */
    private String dataMode;

    /**
     * 业务对象编码
     */
    private String objectCode;

    /**
     * 业务记录ID
     */
    private Long recordId;

    /**
     * 发起人ID
     */
    private String startUserId;

    /**
     * 发起人姓名
     */
    private String startUserName;

    /**
     * 发起部门ID
     */
    private String startDeptId;

    /**
     * 发起部门名称
     */
    private String startDeptName;

    /**
     * 是否允许通过
     */
    private Boolean allowApprove;

    /**
     * 是否允许转办
     */
    private Boolean allowDelegate;

    /**
     * 是否允许驳回
     */
    private Boolean allowReject;

    /**
     * 是否允许驳回至发起人
     */
    private Boolean allowRejectToStart;

    /**
     * 是否允许退回
     */
    private Boolean allowReturn;

    /** 是否允许从当前流程历史中选择任意节点退回。 */
    private Boolean allowMultiReturn;

    /** 当前任务可退回的历史用户任务。 */
    private List<ReturnTarget> returnTargets;

    /** 是否存在可直送的退回源节点。 */
    private Boolean allowDirectSend;

    /** 退回源节点定义 Key。 */
    private String returnSourceActivityId;

    /** 退回源节点名称。 */
    private String returnSourceActivityName;

    /**
     * 是否允许终结流程
     */
    private Boolean allowTerminate;

    /**
     * 是否需要签名
     */
    private Boolean requireSignature;

    /**
     * 是否需要审批意见
     */
    private Boolean requireComment;

    /**
     * 节点审批职责说明。
     */
    private String responsibilityDescription;

    /**
     * 兼容旧版单条审批要点文本。
     */
    private String approvalPoint;

    /**
     * 节点审批要点清单。
     */
    private List<FlowApprovalPointDTO> approvalPoints;

    @Data
    public static class ReturnTarget {
        private String activityId;
        private String activityName;
        private java.util.Date endTime;
    }
}
