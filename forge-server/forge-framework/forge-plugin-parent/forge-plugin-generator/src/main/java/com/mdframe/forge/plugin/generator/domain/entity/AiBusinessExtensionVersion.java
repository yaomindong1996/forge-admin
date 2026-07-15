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
 * 不可覆盖的低代码扩展版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_extension_version")
public class AiBusinessExtensionVersion extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long extensionId;

    private Integer versionNo;

    private String content;

    private String processedContent;

    private String configJson;

    private String contentHash;

    private Integer validationPassed;

    private String validationSummary;

    private Integer testPassed;

    private String testSummary;

    private String changeSummary;

    @TableLogic
    private String delFlag;
}
