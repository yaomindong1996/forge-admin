package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用工作台就绪度摘要。
 */
@Data
public class BusinessApplicationReadinessVO {

    private Boolean ready;

    private String status;

    private Long blockingCount;

    private Long warningCount;

    private List<BusinessApplicationReadinessIssueVO> issues = new ArrayList<>();
}
