package com.mdframe.forge.plugin.generator.vo.businessapp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 协调发布单步结果。
 */
@Data
public class BusinessApplicationPublishStepVO {

    private String stepCode;

    private String stepName;

    /** PENDING/RUNNING/SUCCESS/SKIPPED/FAILED。 */
    private String status;

    private String message;

    private LocalDateTime startedTime;

    private LocalDateTime finishedTime;
}
