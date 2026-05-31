package com.mdframe.forge.plugin.generator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 业务对象设计版本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_business_object_design_version")
public class AiBusinessObjectDesignVersion extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long objectId;

    private String suiteCode;

    private String objectCode;

    private Long configId;

    private String configKey;

    private Long crudConfigVersionId;

    private Integer versionNo;

    /** 版本类型：draft/publish/rollback */
    private String versionType;

    /** 模型快照 JSON */
    private String modelSnapshot;

    /** 页面快照 JSON */
    private String pageSnapshot;

    /** 关系快照 JSON */
    private String relationSnapshot;

    /** 表单优先设计器扩展快照 JSON */
    private String designerOptionsSnapshot;

    /** 发布状态：DRAFT/PUBLISHED/FAILED */
    private String publishStatus;

    private Integer publishVersion;

    private String remark;
}
