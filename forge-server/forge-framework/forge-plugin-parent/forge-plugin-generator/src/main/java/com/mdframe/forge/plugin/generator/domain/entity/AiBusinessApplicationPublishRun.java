package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 可恢复的业务应用协调发布运行单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_application_publish_run")
public class AiBusinessApplicationPublishRun extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long applicationId;

    private String idempotencyKey;

    private String operationType;

    private Integer targetVersionNo;

    private Integer sourceVersionNo;

    private String runStatus;

    private String currentStep;

    private String snapshotJson;

    private String snapshotHash;

    private String selectionJson;

    private String stepResultsJson;

    private Long resultVersionId;

    private String errorCode;

    private String errorSummary;

    private Integer attemptCount;

    private Long startedBy;

    private LocalDateTime startedTime;

    private LocalDateTime finishedTime;

    @TableLogic
    private String delFlag;
}
