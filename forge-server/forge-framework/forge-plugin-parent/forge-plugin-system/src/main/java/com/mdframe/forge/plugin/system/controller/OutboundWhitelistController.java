package com.mdframe.forge.plugin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.outbound.constant.OutboundPermissions;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistQuery;
import com.mdframe.forge.starter.outbound.domain.dto.OutboundWhitelistSaveRequest;
import com.mdframe.forge.starter.outbound.domain.entity.SysOutboundWhitelist;
import com.mdframe.forge.starter.outbound.service.OutboundWhitelistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/outbound-whitelist")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class OutboundWhitelistController {

    private static final String ADMIN_MESSAGE = "只有超级管理员可以维护出站白名单";

    private final OutboundWhitelistService whitelistService;

    @GetMapping("/page")
    @SaCheckPermission(OutboundPermissions.LIST)
    public RespInfo<Page<SysOutboundWhitelist>> page(OutboundWhitelistQuery query) {
        assertPlatformAdmin();
        return RespInfo.success(whitelistService.page(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(OutboundPermissions.LIST)
    public RespInfo<SysOutboundWhitelist> detail(@PathVariable Long id) {
        assertPlatformAdmin();
        return RespInfo.success(whitelistService.getById(id));
    }

    @PostMapping
    @SaCheckPermission(OutboundPermissions.ADD)
    @OperationLog(module = "出站白名单", type = OperationType.ADD, desc = "新增出站白名单")
    public RespInfo<SysOutboundWhitelist> create(@Valid @RequestBody OutboundWhitelistSaveRequest request) {
        assertPlatformAdmin();
        return RespInfo.success(whitelistService.create(request));
    }

    @PutMapping
    @SaCheckPermission(OutboundPermissions.EDIT)
    @OperationLog(module = "出站白名单", type = OperationType.UPDATE, desc = "修改出站白名单")
    public RespInfo<SysOutboundWhitelist> update(@Valid @RequestBody OutboundWhitelistSaveRequest request) {
        assertPlatformAdmin();
        return RespInfo.success(whitelistService.update(request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(OutboundPermissions.REMOVE)
    @OperationLog(module = "出站白名单", type = OperationType.DELETE, desc = "删除出站白名单")
    public RespInfo<Void> delete(@PathVariable Long id) {
        assertPlatformAdmin();
        whitelistService.delete(id);
        return RespInfo.success();
    }

    private void assertPlatformAdmin() {
        SessionHelper.assertAdmin(ADMIN_MESSAGE);
    }
}
