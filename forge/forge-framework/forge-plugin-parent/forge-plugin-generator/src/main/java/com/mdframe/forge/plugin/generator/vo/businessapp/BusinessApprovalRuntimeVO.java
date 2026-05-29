package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 审批运行状态 VO
 */
@Data
public class BusinessApprovalRuntimeVO {

    /**
     * 目标对象编码
     */
    private String targetCode;

    /**
     * 目标对象名称
     */
    private String targetName;

    /**
     * 记录 ID
     */
    private Long recordId;

    /**
     * 是否已配置审批流程
     */
    private Boolean hasFlow;

    /**
     * 流程定义 ID
     */
    private String flowDefinitionId;

    /**
     * 流程定义名称
     */
    private String flowDefinitionName;

    /**
     * 是否可发起审批
     */
    private Boolean canStart;

    /**
     * 审批状态：NONE / PENDING / APPROVED / REJECTED
     */
    private String approvalStatus;

    /**
     * 审批状态文案
     */
    private String approvalStatusLabel;

    /**
     * 详细说明
     */
    private String message;

    /**
     * 下一步操作标识
     */
    private String nextAction;

    /**
     * 下一步操作文案
     */
    private String nextActionLabel;

    /**
     * 下一步操作跳转 URL
     */
    private String nextActionUrl;
}
