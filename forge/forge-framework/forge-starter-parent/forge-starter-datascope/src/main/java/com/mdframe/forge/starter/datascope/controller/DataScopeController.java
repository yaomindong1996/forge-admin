package com.mdframe.forge.starter.datascope.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.datascope.service.IDataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据权限管理Controller
 */
@RestController
@RequestMapping("/datascope")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.datascope", name = "enable-api", havingValue = "true",matchIfMissing = true)
public class DataScopeController {
    
    private final IDataScopeService dataScopeService;
    
    /**
     * 刷新数据权限配置缓存
     */
    @PostMapping("/refreshCache")
    public RespInfo<String> refreshCache() {
        dataScopeService.refreshDataScopeCache();
        return RespInfo.success("缓存刷新成功");
    }
}
