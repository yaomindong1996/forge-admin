package com.mdframe.forge.plugin.ai.resource;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.ai.resource.dto.McpConnectionActionDTO;
import com.mdframe.forge.plugin.ai.resource.dto.McpConnectionSaveDTO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/mcp/connection")
@RequiredArgsConstructor
public class McpResourceController {

    private final McpResourceManager resources;

    @GetMapping
    @SaCheckPermission("ai:mcp:connection:list")
    public RespInfo<List<ResourceViews.Connection>> list() {
        return RespInfo.success(resources.list());
    }

    @PostMapping
    @ApiDecrypt
    @SaCheckPermission("ai:mcp:connection:add")
    @OperationLog(module = "MCP资源", desc = "新增服务连接", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<ResourceViews.Connection> create(@Valid @RequestBody McpConnectionSaveDTO dto) {
        if (dto.id() != null) {
            throw new BusinessException("请使用编辑入口");
        }
        return RespInfo.success(resources.save(dto));
    }

    @PutMapping
    @ApiDecrypt
    @SaCheckPermission("ai:mcp:connection:edit")
    @OperationLog(module = "MCP资源", desc = "修改服务连接", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<ResourceViews.Connection> update(@Valid @RequestBody McpConnectionSaveDTO dto) {
        if (dto.id() == null) {
            throw new BusinessException("请选择需要编辑的服务");
        }
        return RespInfo.success(resources.save(dto));
    }

    @PostMapping("/{id}/sync")
    @SaCheckPermission({"ai:mcp:connection:list", "ai:mcp:connection:discover"})
    @OperationLog(module = "MCP资源", desc = "测试连接并同步工具目录", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> sync(@PathVariable Long id, @Valid @RequestBody McpConnectionActionDTO dto) {
        resources.sync(id, dto.expectedRowVersion());
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:mcp:connection:delete")
    @OperationLog(module = "MCP资源", desc = "删除服务连接", saveRequestParams = false, saveResponseResult = false)
    public RespInfo<Void> delete(@PathVariable Long id, @RequestParam int expectedRowVersion) {
        if (expectedRowVersion < 1) {
            throw new BusinessException("请刷新后删除");
        }
        resources.delete(id, expectedRowVersion);
        return RespInfo.success();
    }
}
