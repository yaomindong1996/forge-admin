package com.mdframe.forge.plugin.ai.resource;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mdframe.forge.plugin.ai.resource.domain.McpConnection;
import com.mdframe.forge.plugin.ai.resource.domain.McpTool;
import com.mdframe.forge.plugin.ai.resource.mapper.McpResourceMapper;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class McpCatalogTransactions {

    private final McpResourceMapper mapper;
    private final McpGateway gateway;

    @Transactional(rollbackFor = Exception.class)
    public void replace(McpConnection expected, List<McpSchema.Tool> tools, Long actor, Long dept) {
        var current = mapper.connection(expected.getTenantId(), expected.getId());
        if (current == null || !current.getRowVersion().equals(expected.getRowVersion())) {
            throw new BusinessException("连接已被修改，请刷新后重新同步");
        }
        mapper.invalidate(current.getTenantId(), current.getId(), actor);
        for (var remote : tools) {
            String hash = McpResourceManager.contract(current, remote, gateway);
            String key = "mcp_" + current.getId() + "_" + hash.substring(0, 24);
            if (mapper.tool(current.getTenantId(), key) != null) {
                mapper.touchTool(current.getTenantId(), key, current.getRowVersion(), actor);
                continue;
            }
            McpTool row = new McpTool();
            row.setId(IdWorker.getId());
            row.setTenantId(current.getTenantId());
            row.setConnectionId(current.getId());
            row.setConnectionVersion(current.getRowVersion());
            row.setToolKey(key);
            row.setRemoteName(remote.name());
            row.setTitle(trim(remote.title() == null ? remote.name() : remote.title(), 160));
            row.setDescription(trim(remote.description(), 2000));
            row.setInputSchema(gateway.schema(remote));
            row.setContractHash(hash);
            row.setAvailable(EnableStatus.ENABLED.getCode());
            row.setCreateBy(actor);
            row.setUpdateBy(actor);
            row.setCreateDept(dept);
            mapper.insertTool(row);
        }
        if (mapper.syncResult(current.getTenantId(), current.getId(), current.getRowVersion(),
                McpSyncState.SYNCED.getCode(), null, actor) != 1) {
            throw new BusinessException("连接已被修改，请重新同步");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void failed(McpConnection current, Long actor) {
        mapper.syncResult(current.getTenantId(), current.getId(), current.getRowVersion(),
                McpSyncState.FAILED.getCode(), "MCP_CONNECT_FAILED", actor);
    }

    private String trim(String value, int length) {
        return value == null ? "" : value.substring(0, Math.min(value.length(), length));
    }
}
