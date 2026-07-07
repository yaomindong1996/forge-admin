package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysClientDTO;
import com.mdframe.forge.plugin.system.entity.SysClient;
import com.mdframe.forge.plugin.system.entity.SysOnlineUser;
import com.mdframe.forge.plugin.system.service.IClientService;
import com.mdframe.forge.plugin.system.service.ISysOnlineUserService;
import com.mdframe.forge.plugin.system.vo.SysClientVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/client")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysClientController {
    
    private final IClientService clientService;
    private final ISysOnlineUserService onlineUserService;
    
    @GetMapping("/page")
    public RespInfo<Page<SysClientVO>> page(
        @RequestParam(defaultValue = "1") Long pageNum,
        @RequestParam(defaultValue = "10") Long pageSize,
        @RequestParam(required = false) String clientCode,
        @RequestParam(required = false) String clientName,
        @RequestParam(required = false) Integer status
    ) {
        assertPlatformAdmin();
        Page<SysClient> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        if (clientCode != null && !clientCode.isEmpty()) {
            wrapper.like(SysClient::getClientCode, clientCode);
        }
        if (clientName != null && !clientName.isEmpty()) {
            wrapper.like(SysClient::getClientName, clientName);
        }
        if (status != null) {
            wrapper.eq(SysClient::getStatus, status);
        }
        
        wrapper.orderByDesc(SysClient::getCreateTime);
        
        Page<SysClient> result = clientService.page(page, wrapper);
        
        Page<SysClientVO> voPage = new Page<>();
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).toList());
        voPage.setTotal(result.getTotal());
        voPage.setSize(result.getSize());
        voPage.setCurrent(result.getCurrent());
        
        return RespInfo.success(voPage);
    }
    
    @GetMapping("/{id}")
    public RespInfo<SysClientVO> getById(@PathVariable Long id) {
        assertPlatformAdmin();
        SysClient client = clientService.getById(id);
        if (client == null) {
            return RespInfo.error("客户端不存在");
        }
        return RespInfo.success(convertToVO(client));
    }
    
    @PostMapping
    public RespInfo<Boolean> create(@RequestBody SysClientDTO dto) {
        assertPlatformAdmin();
        SysClient client = new SysClient();
        BeanUtils.copyProperties(dto, client);
        boolean success = clientService.save(client);
        return RespInfo.success(success);
    }
    
    @PutMapping
    public RespInfo<Boolean> update(@RequestBody SysClientDTO dto) {
        assertPlatformAdmin();
        SysClient client = new SysClient();
        BeanUtils.copyProperties(dto, client);
        boolean success = clientService.updateById(client);
        
        if (success && client.getClientCode() != null) {
            clientService.reloadClientConfigCache(client.getClientCode());
        }
        
        return RespInfo.success(success);
    }
    
    @DeleteMapping("/{id}")
    public RespInfo<Boolean> delete(@PathVariable Long id) {
        assertPlatformAdmin();
        boolean success = clientService.removeById(id);
        return RespInfo.success(success);
    }
    
    @GetMapping("/online/{clientCode}")
    public RespInfo<List<SysOnlineUser>> getOnlineUsers(@PathVariable String clientCode) {
        assertPlatformAdmin();
        List<SysOnlineUser> onlineUsers = onlineUserService.getOnlineUsersByClient(clientCode);
        return RespInfo.success(onlineUsers);
    }
    
    @GetMapping("/list")
    public RespInfo<List<SysClientVO>> list() {
        LambdaQueryWrapper<SysClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysClient::getStatus, 1);
        wrapper.orderByAsc(SysClient::getId);
        
        List<SysClient> clients = clientService.list(wrapper);
        List<SysClientVO> voList = clients.stream().map(this::convertToOptionVO).toList();
        
        return RespInfo.success(voList);
    }
    
    @GetMapping("/online/stats")
    public RespInfo<Map<String, Long>> getOnlineStats() {
        assertPlatformAdmin();
        Map<String, Long> stats = onlineUserService.getOnlineCountByClient();
        return RespInfo.success(stats);
    }
    
    @PostMapping("/kickout/{userId}/{clientCode}")
    public RespInfo<Boolean> kickout(@PathVariable Long userId, @PathVariable String clientCode) {
        assertPlatformAdmin();
        onlineUserService.kickoutByClient(userId, clientCode);
        return RespInfo.success(true);
    }
    
    @GetMapping("/secret/{id}")
    public RespInfo<String> getAppSecret(@PathVariable Long id) {
        assertPlatformAdmin();
        String maskedSecret = clientService.getMaskedAppSecret(id);
        return RespInfo.success(maskedSecret);
    }
    
    @PostMapping("/reload-cache/{clientCode}")
    public RespInfo<Boolean> reloadCache(@PathVariable String clientCode) {
        assertPlatformAdmin();
        clientService.reloadClientConfigCache(clientCode);
        return RespInfo.success(true);
    }

    private void assertPlatformAdmin() {
        SessionHelper.assertAdmin("只有超级管理员可以维护客户端配置");
    }
    
    private SysClientVO convertToVO(SysClient client) {
        SysClientVO vo = new SysClientVO();
        BeanUtils.copyProperties(client, vo);
        vo.setAppSecretMasked(clientService.getMaskedAppSecret(client.getId()));
        return vo;
    }

    private SysClientVO convertToOptionVO(SysClient client) {
        SysClientVO vo = new SysClientVO();
        vo.setId(client.getId());
        vo.setClientCode(client.getClientCode());
        vo.setClientName(client.getClientName());
        vo.setStatus(client.getStatus());
        return vo;
    }
}
