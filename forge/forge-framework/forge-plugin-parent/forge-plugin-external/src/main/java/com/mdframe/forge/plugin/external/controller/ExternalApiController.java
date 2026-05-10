package com.mdframe.forge.plugin.external.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.external.dto.ExternalApiDTO;
import com.mdframe.forge.plugin.external.dto.ExternalApiQuery;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import com.mdframe.forge.plugin.external.service.ExternalApiService;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/external/api")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ExternalApiService apiService;

    @GetMapping("/page")
    public RespInfo<IPage<ExternalApi>> page(ExternalApiQuery query) {
        return RespInfo.success(apiService.page(query));
    }

    @GetMapping("/{id}")
    public RespInfo<ExternalApi> getById(@PathVariable Long id) {
        return RespInfo.success(apiService.getById(id));
    }

    @PostMapping
    public RespInfo<Void> add(@Validated @RequestBody ExternalApiDTO dto) {
        ExternalApi entity = convertDtoToEntity(dto);
        apiService.save(entity);
        return RespInfo.success();
    }

    @PutMapping
    public RespInfo<Void> edit(@Validated @RequestBody ExternalApiDTO dto) {
        ExternalApi entity = convertDtoToEntity(dto);
        apiService.updateById(entity);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    public RespInfo<Void> remove(@PathVariable Long id) {
        apiService.removeById(id);
        return RespInfo.success();
    }

    @GetMapping("/list")
    public RespInfo<List<ExternalApi>> list(@RequestParam(required = false) Long systemId) {
        if (systemId != null) {
            return RespInfo.success(apiService.listBySystemId(systemId));
        }
        return RespInfo.success(apiService.list());
    }

    private ExternalApi convertDtoToEntity(ExternalApiDTO dto) {
        ExternalApi entity = new ExternalApi();
        entity.setId(dto.getId());
        entity.setSystemId(dto.getSystemId());
        entity.setApiCode(dto.getApiCode());
        entity.setApiName(dto.getApiName());
        entity.setApiDesc(dto.getApiDesc());
        entity.setApiPath(dto.getApiPath());
        entity.setApiMethod(dto.getApiMethod());
        entity.setRequestContentType(dto.getRequestContentType());
        entity.setRequestHeaders(dto.getRequestHeaders());
        entity.setRequestParams(dto.getRequestParams());
        entity.setRequestBodyTemplate(dto.getRequestBodyTemplate());
        entity.setResponseContentType(dto.getResponseContentType());
        entity.setResponseDataPath(dto.getResponseDataPath());
        entity.setResponseTotalPath(dto.getResponseTotalPath());
        entity.setParamMappingEnabled(dto.getParamMappingEnabled());
        entity.setParamMappings(dto.getParamMappings());
        entity.setResponseTransformEnabled(dto.getResponseTransformEnabled());
        entity.setResponseTransformScript(dto.getResponseTransformScript());
        entity.setErrorCodePath(dto.getErrorCodePath());
        entity.setErrorMsgPath(dto.getErrorMsgPath());
        entity.setSuccessCodes(dto.getSuccessCodes());
        entity.setRateLimitEnabled(dto.getRateLimitEnabled());
        entity.setRateLimitQps(dto.getRateLimitQps());
        entity.setCacheEnabled(dto.getCacheEnabled());
        entity.setCacheTtl(dto.getCacheTtl());
        entity.setCacheKeyTemplate(dto.getCacheKeyTemplate());
        entity.setPermissionCheckEnabled(dto.getPermissionCheckEnabled());
        entity.setRequiredPermission(dto.getRequiredPermission());
        entity.setApiStatus(dto.getApiStatus());
        entity.setSortOrder(dto.getSortOrder());
        entity.setRemark(dto.getRemark());
        return entity;
    }
}