package com.mdframe.forge.plugin.ai.resource;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mdframe.forge.plugin.ai.agent.engine.permission.AgentToolPermissionService;
import com.mdframe.forge.plugin.ai.agent.engine.permission.PermissionDecision;
import com.mdframe.forge.plugin.ai.agent.engine.tool.AgentTool;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolContext;
import com.mdframe.forge.plugin.ai.agent.engine.tool.ToolResult;
import com.mdframe.forge.plugin.ai.provider.support.AiSecretCrypto;
import com.mdframe.forge.plugin.ai.resource.domain.McpConnection;
import com.mdframe.forge.plugin.ai.resource.domain.McpTool;
import com.mdframe.forge.plugin.ai.resource.dto.McpConnectionSaveDTO;
import com.mdframe.forge.plugin.ai.resource.mapper.McpResourceMapper;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP 远程服务连接与工具契约管理器。
 */
@Component
@RequiredArgsConstructor
public class McpResourceManager {

    private static final int MAX_CONNECTIONS_PER_TENANT = 200;

    private final McpResourceMapper mapper;
    private final McpDestinationPolicy destinations;
    private final McpGateway gateway;
    private final McpCatalogTransactions transactions;
    private final AiSecretCrypto secrets;
    private final AgentToolPermissionService permissions;

    public static Long tenant() {
        Long id = SessionHelper.getTenantId();
        if (id == null || id <= 0 || SessionHelper.getUserId() == null) {
            throw new BusinessException("请重新登录后操作资源");
        }
        return id;
    }

    public List<ResourceViews.Connection> list() {
        Long tenant = tenant();
        var tools = mapper.tools(tenant, null);
        return mapper.connections(tenant).stream()
                .map(row -> view(row, (int) tools.stream()
                        .filter(t -> t.getConnectionId().equals(row.getId())
                                && EnableStatus.ENABLED.matches(t.getAvailable())
                                && t.getConnectionVersion().equals(row.getRowVersion()))
                        .count()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResourceViews.Connection save(McpConnectionSaveDTO dto) {
        Long tenant = tenant();
        McpDestinationPolicy.parse(dto.endpoint());
        if (dto.bearerToken() != null
                && (dto.bearerToken().chars().anyMatch(c -> c < 33 || c > 126) || dto.bearerToken().contains("****"))) {
            throw new BusinessException("令牌包含无效字符，请粘贴原始令牌或留空保留原值");
        }
        if (dto.clearSecret() && dto.bearerToken() != null && !dto.bearerToken().isBlank()) {
            throw new BusinessException("请勿同时填写和清除令牌");
        }
        McpConnection row;
        if (dto.id() == null) {
            if (dto.expectedRowVersion() != 0) {
                throw new BusinessException("新连接版本无效");
            }
            if (mapper.connections(tenant).size() >= MAX_CONNECTIONS_PER_TENANT) {
                throw new BusinessException("当前租户最多配置200个MCP服务");
            }
            if (mapper.byCode(tenant, dto.connectionCode()) != null) {
                throw new BusinessException("服务编码已存在，请修改编码或刷新列表");
            }
            row = new McpConnection();
            row.setId(IdWorker.getId());
            row.setTenantId(tenant);
            row.setConnectionCode(dto.connectionCode());
            row.setCreateBy(SessionHelper.getUserId());
            row.setCreateDept(SessionHelper.getActiveOrgId());
            row.setDelFlag(0L);
            row.setRowVersion(1);
        } else {
            row = require(dto.id());
            if (!row.getConnectionCode().equals(dto.connectionCode())
                    || !Objects.equals(row.getRowVersion(), dto.expectedRowVersion())) {
                throw new BusinessException("连接已变化，请刷新后编辑");
            }
        }
        row.setConnectionName(dto.connectionName().strip());
        row.setEndpoint(dto.endpoint());
        row.setTimeoutSeconds(dto.timeoutSeconds());
        row.setEnabled(dto.enabled());
        row.setSyncState(McpSyncState.UNSYNCED.getCode());
        row.setUpdateBy(SessionHelper.getUserId());
        if (dto.clearSecret()) {
            row.setSecretCipher(null);
        }
        if (dto.bearerToken() != null && !dto.bearerToken().isBlank()) {
            row.setSecretCipher(secrets.encrypt(dto.bearerToken()));
        }
        if (dto.id() == null) {
            mapper.insert(row);
        } else {
            if (mapper.updateConnection(row, dto.expectedRowVersion()) != 1) {
                throw new BusinessException("连接已变化，请刷新后编辑");
            }
            mapper.invalidate(tenant, row.getId(), SessionHelper.getUserId());
            row.setRowVersion(dto.expectedRowVersion() + 1);
        }
        return view(row, 0);
    }

    public McpConnection require(Long id) {
        var row = mapper.connection(tenant(), id);
        if (row == null) {
            throw new BusinessException("MCP服务不存在或无权访问");
        }
        return row;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, int expected) {
        if (mapper.deleteConnection(tenant(), id, expected, SessionHelper.getUserId()) != 1) {
            throw new BusinessException("服务已变化，请刷新后删除");
        }
        mapper.invalidate(tenant(), id, SessionHelper.getUserId());
    }

    public void sync(Long id, int expected) {
        var row = require(id);
        if (!Objects.equals(row.getRowVersion(), expected)) {
            throw new BusinessException("连接已变化，请刷新后同步");
        }
        // 在 try 外先校验白名单，避免把被拦截的源地址误报为远端不可达
        destinations.require(row.getEndpoint());
        List<McpSchema.Tool> tools;
        try {
            tools = gateway.discover(row);
        } catch (Exception e) {
            transactions.failed(row, SessionHelper.getUserId());
            throw new BusinessException("连接或目录读取失败，请检查地址、令牌和超时后重试；已有绑定与权限保持不变");
        }
        transactions.replace(row, tools, SessionHelper.getUserId(), SessionHelper.getActiveOrgId());
    }

    public static String contract(McpConnection row, McpSchema.Tool tool, McpGateway gateway) {
        try {
            String data = row.getId() + ":" + row.getRowVersion() + ":" + gateway.canonical(tool);
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean available(McpConnection connection, McpTool tool) {
        return connection != null
                && EnableStatus.ENABLED.matches(connection.getEnabled())
                && EnableStatus.ENABLED.matches(tool.getAvailable())
                && Objects.equals(tool.getConnectionVersion(), connection.getRowVersion());
    }

    public AgentTool resolve(String key) {
        return resolveForRuntime(key, tenant(), SessionHelper.getUserId());
    }

    public AgentTool resolveForRuntime(String key, Long resolvedTenant, Long resolvedActor) {
        if (resolvedTenant == null || resolvedTenant <= 0 || resolvedActor == null || resolvedActor <= 0) {
            throw new BusinessException("工具执行身份无效");
        }
        var tool = mapper.tool(resolvedTenant, key);
        if (tool == null) {
            return null;
        }
        var connection = mapper.connection(resolvedTenant, tool.getConnectionId());
        if (!available(connection, tool)) {
            return null;
        }
        final Integer fixedRevision = connection.getRowVersion();
        final String fixedContract = tool.getContractHash();
        final String fixedName = tool.getRemoteName();
        return new AgentTool() {
            @Override
            public String getKey() {
                return tool.getToolKey();
            }

            @Override
            public String getExecutionContract() {
                return fixedContract;
            }

            @Override
            public String getSource() {
                return "mcp";
            }

            @Override
            public String getSourceLabel() {
                return connection.getConnectionName() + " / " + fixedName;
            }

            @Override
            public String getDescription() {
                return tool.getDescription();
            }

            @Override
            public String getParametersSchema() {
                return tool.getInputSchema();
            }

            @Override
            public ToolResult execute(Map<String, Object> args, ToolContext context) {
                try {
                    if (context == null || !Objects.equals(context.getTenantId(), resolvedTenant)
                            || context.getUserId() == null || !Objects.equals(context.getUserId(), resolvedActor)
                            || context.getAgentId() == null) {
                        return ToolResult.error("工具执行身份不完整");
                    }
                    PermissionDecision decision = permissions.getDecision(context.getAgentId(), getKey());
                    if (decision == PermissionDecision.DENY) {
                        return ToolResult.error("此智能体已被禁止执行该工具");
                    }
                    var current = mapper.connection(context.getTenantId(), connection.getId());
                    var currentTool = mapper.tool(context.getTenantId(), getKey());
                    if (currentTool == null || !available(current, currentTool)
                            || !Objects.equals(current.getRowVersion(), fixedRevision)
                            || !Objects.equals(currentTool.getContractHash(), fixedContract)
                            || !Objects.equals(currentTool.getRemoteName(), fixedName)) {
                        return ToolResult.error("工具目录已失效，请重新核对资源");
                    }
                    return ToolResult.data(gateway.call(
                            current, currentTool.getRemoteName(), currentTool.getContractHash(), args));
                } catch (Exception e) {
                    return ToolResult.error("MCP工具未完成，请检查连接、授权和目录版本后重试");
                }
            }
        };
    }

    private ResourceViews.Connection view(McpConnection row, int count) {
        return new ResourceViews.Connection(
                row.getId(), row.getConnectionCode(), row.getConnectionName(), row.getEndpoint(),
                row.getSecretCipher() != null && !row.getSecretCipher().isBlank(),
                row.getTimeoutSeconds(), row.getEnabled(), row.getRowVersion(),
                row.getSyncState(), row.getErrorCode(), row.getSyncedAt(),
                count, destinations.allowed(row.getEndpoint()));
    }
}
