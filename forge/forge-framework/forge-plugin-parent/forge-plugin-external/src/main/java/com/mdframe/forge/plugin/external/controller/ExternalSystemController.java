package com.mdframe.forge.plugin.external.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.external.dto.ExternalSystemDTO;
import com.mdframe.forge.plugin.external.dto.ExternalSystemQuery;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/external/system")
@RequiredArgsConstructor
@ApiEncrypt
@ApiDecrypt
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
        validateSystem(dto);
        ExternalSystem entity = convertDtoToEntity(dto);
        systemService.save(entity);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> edit(@Validated @RequestBody ExternalSystemDTO dto) {
        validateSystem(dto);
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
        entity.setCustomAuthAdapter(isCustomAuth(dto.getAuthType()) ? dto.getCustomAuthAdapter() : null);
        entity.setCustomAuthConfig(normalizeCustomAuthConfig(dto));
        entity.setTrustedInternal(Boolean.TRUE.equals(dto.getTrustedInternal()));
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

    private void validateSystem(ExternalSystemDTO dto) {
        if (isBlank(dto.getSystemName())) {
            throw new BusinessException("系统名称不能为空");
        }
        if (isBlank(dto.getSystemCode())) {
            throw new BusinessException("系统编码不能为空");
        }
        if (isBlank(dto.getBaseUrl())) {
            throw new BusinessException("基础URL不能为空");
        }
        if (isBlank(dto.getAuthType())) {
            dto.setAuthType("none");
        }
        String authType = dto.getAuthType();
        if ("basic".equalsIgnoreCase(authType)) {
            requireNotBlank(dto.getBasicUsername(), "Basic用户名不能为空");
            requireNotBlank(dto.getBasicPassword(), "Basic密码不能为空");
        }
        if ("token".equalsIgnoreCase(authType)) {
            requireNotBlank(dto.getTokenValue(), "Token值不能为空");
        }
        if ("current_token".equalsIgnoreCase(authType)) {
            if (isBlank(dto.getTokenHeaderName())) {
                dto.setTokenHeaderName("Authorization");
            }
            if (dto.getTokenPrefix() == null) {
                dto.setTokenPrefix("Bearer");
            }
        }
        if ("api_key".equalsIgnoreCase(authType)) {
            requireNotBlank(dto.getApiKeyName(), "API Key名称不能为空");
            requireNotBlank(dto.getApiKeyValue(), "API Key值不能为空");
            if (isBlank(dto.getApiKeyPosition())) {
                dto.setApiKeyPosition("header");
            }
        }
        if ("oauth2".equalsIgnoreCase(authType)) {
            requireNotBlank(dto.getOauth2TokenUrl(), "OAuth2 Token URL不能为空");
            requireNotBlank(dto.getOauth2ClientId(), "OAuth2 Client ID不能为空");
            requireNotBlank(dto.getOauth2ClientSecret(), "OAuth2 Client Secret不能为空");
        }
        if (Boolean.TRUE.equals(dto.getProxyEnabled())) {
            requireNotBlank(dto.getProxyHost(), "代理主机不能为空");
            if (dto.getProxyPort() == null) {
                throw new BusinessException("代理端口不能为空");
            }
        }
    }

    private String normalizeCustomAuthConfig(ExternalSystemDTO dto) {
        if (!isCustomAuth(dto.getAuthType())) {
            return null;
        }
        if (isBlank(dto.getCustomAuthAdapter())) {
            throw new BusinessException("请选择认证适配器");
        }
        if (isBlank(dto.getCustomAuthConfig())) {
            return "{}";
        }
        try {
            JSONObject config = JSON.parseObject(dto.getCustomAuthConfig());
            if (config == null) {
                throw new BusinessException("自定义认证配置必须是JSON对象");
            }
            return config.toJSONString();
        } catch (Exception e) {
            throw new BusinessException("自定义认证配置必须是JSON对象");
        }
    }

    private boolean isCustomAuth(String authType) {
        return authType != null && "custom".equalsIgnoreCase(authType);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void requireNotBlank(String value, String message) {
        if (isBlank(value)) {
            throw new BusinessException(message);
        }
    }
}
