package com.mdframe.forge.plugin.capability.controlplane.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.capability.controlplane.domain.AiCapability;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityPublishDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityCatalogService;
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
@RequestMapping("/ai/capability")
@RequiredArgsConstructor
public class CapabilityCatalogController {

    private final CapabilityCatalogService catalogService;

    @GetMapping("/page")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(module = "AI中枢能力", type = OperationType.QUERY, desc = "分页查询能力目录")
    public RespInfo<Page<AiCapability>> page(
            PageQuery pageQuery,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String publishStatus) {
        return RespInfo.success(catalogService.page(
                SessionHelper.getTenantId(), pageQuery, keyword, publishStatus));
    }

    @PostMapping("/getById")
    @SaCheckPermission("ai:capability:query")
    @OperationLog(module = "AI中枢能力", type = OperationType.QUERY, desc = "查询能力详情")
    public RespInfo<AiCapability> getById(@RequestParam Long id) {
        return RespInfo.success(catalogService.getById(SessionHelper.getTenantId(), id));
    }

    @PostMapping("/publish")
    @SaCheckPermission("ai:capability:publish")
    @OperationLog(module = "AI中枢能力", type = OperationType.ADD, desc = "发布能力版本")
    @ApiDecrypt
    public RespInfo<Long> publish(@Valid @RequestBody CapabilityPublishDTO dto) {
        return RespInfo.success(catalogService.publish(SessionHelper.getTenantId(), dto));
    }

    @PostMapping("/disable/{id}")
    @SaCheckPermission("ai:capability:publish")
    @OperationLog(module = "AI中枢能力", type = OperationType.UPDATE, desc = "停用能力")
    public RespInfo<Void> disable(@PathVariable Long id) {
        catalogService.disable(SessionHelper.getTenantId(), id);
        return RespInfo.success();
    }
}
