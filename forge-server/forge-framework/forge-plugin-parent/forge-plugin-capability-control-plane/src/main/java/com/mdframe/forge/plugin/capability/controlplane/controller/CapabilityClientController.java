package com.mdframe.forge.plugin.capability.controlplane.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.capability.controlplane.dto.CapabilityClientCreateDTO;
import com.mdframe.forge.plugin.capability.controlplane.service.CapabilityClientService;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityClientSecretVO;
import com.mdframe.forge.plugin.capability.controlplane.vo.CapabilityClientVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
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
@RequestMapping("/ai/capability/client")
@RequiredArgsConstructor
public class CapabilityClientController {

    private final CapabilityClientService clientService;

    @GetMapping("/page")
    @SaCheckPermission("ai:capability:client:query")
    @OperationLog(module = "AI中枢客户端", type = OperationType.QUERY, desc = "分页查询机器客户端")
    public RespInfo<Page<CapabilityClientVO>> page(
            PageQuery pageQuery,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return RespInfo.success(clientService.page(
                SessionHelper.getTenantId(), pageQuery, keyword, status));
    }

    @PostMapping("/add")
    @SaCheckPermission("ai:capability:client:add")
    @OperationLog(
            module = "AI中枢客户端",
            type = OperationType.ADD,
            desc = "创建机器客户端",
            saveResponseResult = false)
    @ApiDecrypt
    @ApiEncrypt
    public RespInfo<CapabilityClientSecretVO> add(@Valid @RequestBody CapabilityClientCreateDTO dto) {
        return RespInfo.success(clientService.create(SessionHelper.getTenantId(), dto));
    }

    @PostMapping("/rotate/{id}")
    @SaCheckPermission("ai:capability:client:rotate")
    @OperationLog(
            module = "AI中枢客户端",
            type = OperationType.UPDATE,
            desc = "轮换机器客户端密钥",
            saveResponseResult = false)
    @ApiEncrypt
    public RespInfo<CapabilityClientSecretVO> rotate(@PathVariable Long id) {
        return RespInfo.success(clientService.rotate(SessionHelper.getTenantId(), id));
    }

    @PostMapping("/revoke/{id}")
    @SaCheckPermission("ai:capability:client:revoke")
    @OperationLog(module = "AI中枢客户端", type = OperationType.UPDATE, desc = "吊销机器客户端")
    public RespInfo<Void> revoke(@PathVariable Long id) {
        clientService.revoke(SessionHelper.getTenantId(), id);
        return RespInfo.success();
    }
}
