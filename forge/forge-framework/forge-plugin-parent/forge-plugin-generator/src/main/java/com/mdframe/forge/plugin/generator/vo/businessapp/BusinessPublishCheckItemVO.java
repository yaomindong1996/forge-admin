package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 业务对象发布检查项。
 */
@Data
public class BusinessPublishCheckItemVO {

    private String itemCode;

    private String category;

    /** PASS/WARN/BLOCK */
    private String level;

    private String title;

    private String message;

    private String fieldCode;

    private String zoneKey;

    private String fixAction;

    private String fixActionLabel;

    private String fixTarget;

    private Integer sortOrder;
}
