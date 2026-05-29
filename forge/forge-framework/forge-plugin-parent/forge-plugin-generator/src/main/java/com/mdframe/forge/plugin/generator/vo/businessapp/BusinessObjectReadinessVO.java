package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.List;

/**
 * 业务对象就绪度 VO
 */
@Data
public class BusinessObjectReadinessVO {

    /**
     * 业务对象 ID
     */
    private Long objectId;

    /**
     * 业务套件编码
     */
    private String suiteCode;

    /**
     * 业务对象编码
     */
    private String objectCode;

    /**
     * 业务对象名称
     */
    private String objectName;

    /**
     * 整体就绪状态：RUNNABLE / CONFIGURED / REGISTERED / MISSING / ERROR
     */
    private String overallStatus;

    /**
     * 就绪度评分（0-100）
     */
    private Integer score;

    /**
     * 就绪度检查项列表
     */
    private List<BusinessReadinessItemVO> items;

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
