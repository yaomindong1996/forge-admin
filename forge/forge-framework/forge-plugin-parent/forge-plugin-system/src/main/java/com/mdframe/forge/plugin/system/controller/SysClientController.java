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
        @RequestParam(defaultValue = "1") Long current,
        @RequestParam(defaultValue = "10") Long size,
        @RequestParam(required = false) String clientCode,
        @RequestParam(required = false) String clientName,
        @RequestParam(required = false) Integer status
    ) {
        Page<SysClient> page = new Page<>();
        page.setSize(size);
        page.setCurrent(current);
        
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
        SysClient client = clientService.getById(id);
        if (client == null) {
            return RespInfo.error("客户端不存在");
        }
        return RespInfo.success(convertToVO(client));
    }
    
    @PostMapping
    public RespInfo<Boolean> create(@RequestBody SysClientDTO dto) {
        SysClient client = new SysClient();
        BeanUtils.copyProperties(dto, client);
        boolean success = clientService.save(client);
        return RespInfo.success(success);
    }
    
    @PutMapping
    public RespInfo<Boolean> update(@RequestBody SysClientDTO dto) {
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
        boolean success = clientService.removeById(id);
        return RespInfo.success(success);
    }
    
    @GetMapping("/online/{clientCode}")
    public RespInfo<List<SysOnlineUser>> getOnlineUsers(@PathVariable String clientCode) {
        List<SysOnlineUser> onlineUsers = onlineUserService.getOnlineUsersByClient(clientCode);
        return RespInfo.success(onlineUsers);
    }
    
    @GetMapping("/online/stats")
    public RespInfo<Map<String, Long>> getOnlineStats() {
        Map<String, Long> stats = onlineUserService.getOnlineCountByClient();
        return RespInfo.success(stats);
    }
    
    @PostMapping("/kickout/{userId}/{clientCode}")
    public RespInfo<Boolean> kickout(@PathVariable Long userId, @PathVariable String clientCode) {
        onlineUserService.kickoutByClient(userId, clientCode);
        return RespInfo.success(true);
    }
    
    @GetMapping("/secret/{id}")
    public RespInfo<String> getAppSecret(@PathVariable Long id) {
        String maskedSecret = clientService.getMaskedAppSecret(id);
        return RespInfo.success(maskedSecret);
    }
    
    @PostMapping("/reload-cache/{clientCode}")
    public RespInfo<Boolean> reloadCache(@PathVariable String clientCode) {
        clientService.reloadClientConfigCache(clientCode);
        return RespInfo.success(true);
    }
    
    private SysClientVO convertToVO(SysClient client) {
        SysClientVO vo = new SysClientVO();
        BeanUtils.copyProperties(client, vo);
        vo.setAppSecretMasked(clientService.getMaskedAppSecret(client.getId()));
        return vo;
    }
}
