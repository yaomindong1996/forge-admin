package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 扩展执行审计，仅保存治理元数据和脱敏错误摘要。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_extension_execution_log")
public class AiBusinessExtensionExecutionLog extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long extensionId;

    private String extensionCode;

    private Integer versionNo;

    private Long applicationId;

    private Long objectId;

    private Long entryId;

    private String hookCode;

    private String resultStatus;

    private Long durationMs;

    private String errorCode;

    private String errorSummary;

    private Long actorUserId;

    @TableLogic
    private String delFlag;
}
