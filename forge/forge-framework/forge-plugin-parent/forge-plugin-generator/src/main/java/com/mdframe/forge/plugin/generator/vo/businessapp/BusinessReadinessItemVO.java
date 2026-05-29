package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 就绪度检查项 VO
 */
@Data
public class BusinessReadinessItemVO {

    /**
     * 检查项标识
     */
    private String itemCode;

    /**
     * 检查项名称
     */
    private String itemName;

    /**
     * 检查项状态：RUNNABLE / CONFIGURED / REGISTERED / MISSING / ERROR
     */
    private String status;

    /**
     * 状态文案
     */
    private String statusLabel;

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
