package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

/**
 * 应用工作台就绪度问题。
 */
@Data
public class BusinessApplicationReadinessIssueVO {

    private String issueCode;

    private String level;

    private String title;

    private String message;

    private String sectionKey;

    private String actionPanel;

    private String assetType;

    private Long assetId;

    private String assetCode;
}
