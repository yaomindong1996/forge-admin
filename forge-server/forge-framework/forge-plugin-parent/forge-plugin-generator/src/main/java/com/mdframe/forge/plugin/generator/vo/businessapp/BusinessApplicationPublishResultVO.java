package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用协调发布或恢复结果。
 */
@Data
public class BusinessApplicationPublishResultVO {

    private Long runId;

    private Long applicationId;

    private String operationType;

    private String runStatus;

    private Integer targetVersionNo;

    private Long resultVersionId;

    private Boolean recoverable;

    private String currentStep;

    private String message;

    private List<BusinessApplicationPublishStepVO> steps = new ArrayList<>();
}
