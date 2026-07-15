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
 * 不可变业务应用发布版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_application_version")
public class AiBusinessApplicationVersion extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long applicationId;

    private Integer versionNo;

    private String snapshotJson;

    private String snapshotHash;

    private String publishStatus;

    private String publishSummary;

    private Integer sourceVersionNo;

    private Long publishedBy;

    private LocalDateTime publishedTime;

    @TableLogic
    private String delFlag;
}
