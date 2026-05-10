package com.mdframe.forge.plugin.external.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.external.dto.ExternalSystemDTO;
import com.mdframe.forge.plugin.external.dto.ExternalSystemQuery;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/external/system")
@RequiredArgsConstructor
public class ExternalSystemController {

    private final ExternalSystemService systemService;

    @GetMapping("/page")
    public RespInfo<IPage<ExternalSystem>> page(ExternalSystemQuery query) {
        return RespInfo.success(systemService.page(query));
    }

    @GetMapping("/{id}")
    public RespInfo<ExternalSystem> getById(@PathVariable Long id) {
        return RespInfo.success(systemService.getById(id));
    }

    @PostMapping
    public RespInfo<Void> add(@Validated @RequestBody ExternalSystemDTO dto) {
        ExternalSystem entity = convertDtoToEntity(dto);
        systemService.save(entity);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> edit(@Validated @RequestBody ExternalSystemDTO dto) {
        ExternalSystem entity = convertDtoToEntity(dto);
        systemService.updateById(entity);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> remove(@PathVariable Long id) {
        systemService.removeById(id);
        return RespInfo.success();
    }

    @GetMapping("/list")
    public RespInfo<List<ExternalSystem>> list() {
        return RespInfo.success(systemService.listAll());
    }

    private ExternalSystem convertDtoToEntity(ExternalSystemDTO dto) {
        ExternalSystem entity = new ExternalSystem();
        entity.setId(dto.getId());
        entity.setSystemCode(dto.getSystemCode());
        entity.setSystemName(dto.getSystemName());
        entity.setSystemDesc(dto.getSystemDesc());
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setAuthType(dto.getAuthType());
        entity.setBasicUsername(dto.getBasicUsername());
        entity.setBasicPassword(dto.getBasicPassword());
        entity.setTokenValue(dto.getTokenValue());
        entity.setTokenHeaderName(dto.getTokenHeaderName());
        entity.setTokenPrefix(dto.getTokenPrefix());
        entity.setOauth2TokenUrl(dto.getOauth2TokenUrl());
        entity.setOauth2ClientId(dto.getOauth2ClientId());
        entity.setOauth2ClientSecret(dto.getOauth2ClientSecret());
        entity.setOauth2GrantType(dto.getOauth2GrantType());
        entity.setOauth2Scope(dto.getOauth2Scope());
        entity.setApiKeyName(dto.getApiKeyName());
        entity.setApiKeyValue(dto.getApiKeyValue());
        entity.setApiKeyPosition(dto.getApiKeyPosition());
        entity.setCustomAuthAdapter(dto.getCustomAuthAdapter());
        entity.setCustomAuthConfig(dto.getCustomAuthConfig());
        entity.setProxyEnabled(dto.getProxyEnabled());
        entity.setProxyHost(dto.getProxyHost());
        entity.setProxyPort(dto.getProxyPort());
        entity.setProxyUsername(dto.getProxyUsername());
        entity.setProxyPassword(dto.getProxyPassword());
        entity.setRetryEnabled(dto.getRetryEnabled());
        entity.setRetryMaxAttempts(dto.getRetryMaxAttempts());
        entity.setRetryBackoffInterval(dto.getRetryBackoffInterval());
        entity.setConnectTimeout(dto.getConnectTimeout());
        entity.setReadTimeout(dto.getReadTimeout());
        entity.setWriteTimeout(dto.getWriteTimeout());
        entity.setSslVerifyEnabled(dto.getSslVerifyEnabled());
        entity.setRequestLoggingEnabled(dto.getRequestLoggingEnabled());
        entity.setSystemStatus(dto.getSystemStatus());
        entity.setRemark(dto.getRemark());
        return entity;
    }
}