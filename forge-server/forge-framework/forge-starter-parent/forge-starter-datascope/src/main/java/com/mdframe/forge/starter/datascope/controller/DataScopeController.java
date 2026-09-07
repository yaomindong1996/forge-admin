package com.mdframe.forge.starter.datascope.controller;

import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.session.SessionHelper;
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
    @OperationLog(module = "数据权限配置管理", type = OperationType.UPDATE, desc = "刷新数据权限")
    public RespInfo<String> refreshCache() {
        SessionHelper.assertAdmin("只有超级管理员可以刷新数据权限");
        dataScopeService.refreshDataScopeCache();
        return RespInfo.success("数据权限已刷新，请在相关页面重新查询");
    }
}
