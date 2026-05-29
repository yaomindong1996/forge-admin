package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.List;

/**
 * 业务套件验收状态 VO
 */
@Data
public class BusinessSuiteAcceptanceVO {

    /**
     * 业务套件编码
     */
    private String suiteCode;

    /**
     * 业务套件名称
     */
    private String suiteName;

    /**
     * 整体验收状态：PASSED / PARTIAL / FAILED
     */
    private String overallStatus;

    /**
     * 验收评分（0-100）
     */
    private Integer score;

    /**
     * 核心对象验收状态列表
     */
    private List<ObjectAcceptanceVO> objects;

    /**
     * 引擎能力验收状态列表
     */
    private List<EngineAcceptanceVO> engines;

    /**
     * 渠道验收状态列表
     */
    private List<ChannelAcceptanceVO> channels;

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

    /**
     * 对象验收状态 VO
     */
    @Data
    public static class ObjectAcceptanceVO {

        /**
         * 业务对象编码
         */
        private String objectCode;

        /**
         * 业务对象名称
         */
        private String objectName;

        /**
         * 就绪状态：RUNNABLE / CONFIGURED / REGISTERED / MISSING / ERROR
         */
        private String readinessStatus;

        /**
         * 状态文案
         */
        private String statusLabel;

        /**
         * 详细说明
         */
        private String message;

        /**
         * 是否可运行
         */
        private Boolean runnable;
    }

    /**
     * 引擎能力验收状态 VO
     */
    @Data
    public static class EngineAcceptanceVO {

        /**
         * 引擎类型：APPROVAL / REPORT / MESSAGE / PERMISSION / TRIGGER
         */
        private String engineType;

        /**
         * 引擎名称
         */
        private String engineName;

        /**
         * 接入数量
         */
        private Integer totalCount;

        /**
         * 可运行数量
         */
        private Integer runnableCount;

        /**
         * 待配置数量
         */
        private Integer pendingCount;

        /**
         * 异常数量
         */
        private Integer errorCount;

        /**
         * 状态：RUNNABLE / PARTIAL / MISSING
         */
        private String status;
    }

    /**
     * 渠道验收状态 VO
     */
    @Data
    public static class ChannelAcceptanceVO {

        /**
         * 渠道类型：MOBILE / EMBEDDED / INTEGRATION
         */
        private String channelType;

        /**
         * 渠道名称
         */
        private String channelName;

        /**
         * 接入数量
         */
        private Integer totalCount;

        /**
         * 可用数量
         */
        private Integer availableCount;

        /**
         * 状态：RUNNABLE / PARTIAL / MISSING
         */
        private String status;
    }
}
