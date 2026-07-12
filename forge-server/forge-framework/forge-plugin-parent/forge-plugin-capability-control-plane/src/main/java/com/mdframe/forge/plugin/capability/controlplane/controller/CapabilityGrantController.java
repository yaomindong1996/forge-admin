package com.mdframe.forge.plugin.capability.controlplane.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapabilityGrant;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityGrantCreateDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityGrantService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/capability/grant")
@RequiredArgsConstructor
public class CapabilityGrantController {

    private final CapabilityGrantService grantService;

    @GetMapping("/page")
    @SaCheckPermission("ai:capability:grant:query")
    @OperationLog(module = "AI中枢授权", type = OperationType.QUERY, desc = "分页查询能力授权")
    public RespInfo<Page<AiCapabilityGrant>> page(
            PageQuery pageQuery,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long capabilityId,
            @RequestParam(required = false) String status) {
        return RespInfo.success(grantService.page(
                SessionHelper.getTenantId(), pageQuery, clientId, capabilityId, status));
    }

    @PostMapping("/add")
    @SaCheckPermission("ai:capability:grant:add")
    @OperationLog(module = "AI中枢授权", type = OperationType.ADD, desc = "新增能力授权")
    @ApiDecrypt
    public RespInfo<Long> add(@Valid @RequestBody CapabilityGrantCreateDTO dto) {
        return RespInfo.success(grantService.grant(SessionHelper.getTenantId(), dto));
    }

    @PostMapping("/revoke/{id}")
    @SaCheckPermission("ai:capability:grant:revoke")
    @OperationLog(module = "AI中枢授权", type = OperationType.UPDATE, desc = "撤销能力授权")
    public RespInfo<Void> revoke(@PathVariable Long id) {
        grantService.revoke(SessionHelper.getTenantId(), id);
        return RespInfo.success();
    }
}
