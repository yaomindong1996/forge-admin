package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 对象关系运行入口 VO
 */
@Data
public class BusinessRelationRuntimeVO {

    /**
     * 关系 ID
     */
    private Long relationId;

    /**
     * 关系名称（业务语言）
     */
    private String relationName;

    /**
     * 关系类型：ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
     */
    private String relationType;

    /**
     * 源对象编码
     */
    private String sourceObjectCode;

    /**
     * 源对象名称
     */
    private String sourceObjectName;

    /**
     * 目标对象编码
     */
    private String targetObjectCode;

    /**
     * 目标对象名称
     */
    private String targetObjectName;

    /**
     * 目标应用入口 ID
     */
    private Long targetAppId;

    /**
     * 目标应用入口编码
     */
    private String targetAppCode;

    /**
     * 目标运行配置 Key
     */
    private String targetConfigKey;

    /**
     * 关联字段（源对象侧）
     */
    private String sourceField;

    /**
     * 关联字段（目标对象侧）
     */
    private String targetField;

    /**
     * 是否可打开
     */
    private Boolean canOpen;

    /**
     * 打开方式：ROUTE, IFRAME, EXTERNAL
     */
    private String openType;

    /**
     * 目标运行页路径
     */
    private String targetUrl;

    /**
     * 默认筛选参数（JSON 格式）
     */
    private String defaultFilter;

    /**
     * 不可打开原因
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
}
