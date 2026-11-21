package com.mdframe.forge.plugin.ai.resource.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_mcp_tool")
public class McpTool extends TenantEntity {
    @TableId
    private Long id;
    private Long connectionId;
    private Integer connectionVersion;
    private String toolKey;
    private String remoteName;
    private String title;
    private String description;
    private String inputSchema;
    private String contractHash;
    private Integer available;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
