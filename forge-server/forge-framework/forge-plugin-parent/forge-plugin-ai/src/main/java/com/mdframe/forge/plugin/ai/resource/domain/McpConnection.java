package com.mdframe.forge.plugin.ai.resource.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mdframe.forge.starter.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_mcp_connection")
public class McpConnection extends TenantEntity {
    @TableId
    private Long id;
    private String connectionCode;
    private String connectionName;
    private String endpoint;
    private String secretCipher;
    private Integer timeoutSeconds;
    private Integer enabled;
    private Integer rowVersion;
    private String syncState;
    private String errorCode;
    private LocalDateTime syncedAt;
    @TableLogic(value = "0", delval = "id")
    private Long delFlag;
}
