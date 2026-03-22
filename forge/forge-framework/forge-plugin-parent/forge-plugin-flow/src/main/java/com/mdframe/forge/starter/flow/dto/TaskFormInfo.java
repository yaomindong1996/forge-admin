package com.mdframe.forge.starter.flow.dto;

import lombok.Data;

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

    /**
     * 表单类型：dynamic-动态表单, external-外部表单
     */
    private String formType;

    /**
     * 动态表单JSON配置（formType=dynamic时有值）
     */
    private String formJson;

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
     * 流程变量（表单数据）
     */
    private Map<String, Object> variables;

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
}