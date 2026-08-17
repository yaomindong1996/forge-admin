package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysCachePolicyEditDTO;
import com.mdframe.forge.plugin.system.dto.SysCachePolicyQuery;
import com.mdframe.forge.plugin.system.service.ISysManagedCachePolicyService;
import com.mdframe.forge.plugin.system.vo.SysManagedCachePolicyVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/cache/policy")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysManagedCachePolicyController {

    private final ISysManagedCachePolicyService cachePolicyService;

    @ModelAttribute
    public void assertPlatformAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以管理受管缓存策略");
    }

    @GetMapping("/page")
    @OperationLog(module = "缓存管理", type = OperationType.QUERY, desc = "分页查询受管缓存策略")
    public RespInfo<Page<SysManagedCachePolicyVO>> page(SysCachePolicyQuery query) {
        return RespInfo.success(cachePolicyService.page(query));
    }

    @PostMapping("/edit")
    @OperationLog(module = "缓存管理", type = OperationType.UPDATE, desc = "修改受管缓存策略")
    public RespInfo<Void> edit(@Valid @RequestBody SysCachePolicyEditDTO dto) {
        cachePolicyService.edit(dto);
        return RespInfo.success();
    }

    @PostMapping("/reset")
    @OperationLog(module = "缓存管理", type = OperationType.UPDATE, desc = "恢复受管缓存默认策略")
    public RespInfo<Void> reset(@RequestParam String applicationCode, @RequestParam String cacheName) {
        cachePolicyService.reset(applicationCode, cacheName);
        return RespInfo.success();
    }

    @PostMapping("/clear")
    @OperationLog(module = "缓存管理", type = OperationType.OTHER, desc = "清空受管缓存")
    public RespInfo<Void> clear(@RequestParam String applicationCode, @RequestParam String cacheName) {
        cachePolicyService.clear(applicationCode, cacheName);
        return RespInfo.success();
    }
}
