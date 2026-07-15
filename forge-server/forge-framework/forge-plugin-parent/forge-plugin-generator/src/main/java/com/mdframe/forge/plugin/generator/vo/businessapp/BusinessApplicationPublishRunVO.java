package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用协调发布运行单视图。
 */
@Data
public class BusinessApplicationPublishRunVO {

    private Long id;

    private Long applicationId;

    private String operationType;

    private Integer targetVersionNo;

    private Integer sourceVersionNo;

    private String runStatus;

    private String currentStep;

    private Long resultVersionId;

    private String errorCode;

    private String errorSummary;

    private Integer attemptCount;

    private Long startedBy;

    private LocalDateTime startedTime;

    private LocalDateTime finishedTime;

    private List<BusinessApplicationPublishStepVO> steps = new ArrayList<>();
}
