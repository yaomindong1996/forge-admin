package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用协调发布预检查结果。
 */
@Data
public class BusinessApplicationPublishCheckVO {

    private Long applicationId;

    private String applicationCode;

    private Boolean publishable;

    /** READY/WARNING/BLOCKED。 */
    private String status;

    private Long blockingCount;

    private Long warningCount;

    private List<BusinessApplicationReadinessIssueVO> issues = new ArrayList<>();

    private BusinessApplicationAssetSelectionVO selection;
}
