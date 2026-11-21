package com.mdframe.forge.plugin.ai.resource.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_knowledge_resource_profile")
public class AiKnowledgeResourceProfile extends TenantEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long embeddingModelId;
    private Long rerankModelId;
    private Long vectorStoreInstanceId;
    private Integer dimension;
    private Integer chunkSize;
    private Integer revision;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
