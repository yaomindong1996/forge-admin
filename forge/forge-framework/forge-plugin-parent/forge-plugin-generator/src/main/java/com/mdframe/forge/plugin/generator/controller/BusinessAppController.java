package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessBootstrapService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessAppService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppOpenInfoVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务应用平台应用入口接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/app")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessAppController {

    private final BusinessAppService appService;
    private final BusinessBootstrapService bootstrapService;

    @GetMapping("/page")
    @SaCheckPermission("ai:businessApp:list")
    @OperationLog(module = "应用入口", type = OperationType.QUERY, desc = "分页查询应用入口")
    public RespInfo<Page<BusinessAppVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              BusinessAppQueryDTO query) {
        return RespInfo.success(appService.page(pageNum, pageSize, query));
    }

    @GetMapping("/list")
    @SaCheckPermission("ai:businessApp:list")
    @OperationLog(module = "应用入口", type = OperationType.QUERY, desc = "查询应用入口列表")
    public RespInfo<List<BusinessAppVO>> list(BusinessAppQueryDTO query) {
        return RespInfo.success(appService.list(query));
    }

    @PostMapping("/sync-published-crud-configs")
    @SaCheckPermission("ai:businessApp:edit")
    @OperationLog(module = "应用入口", type = OperationType.UPDATE, desc = "同步已发布低代码应用入口")
    public RespInfo<Void> syncPublishedCrudConfigs() {
        bootstrapService.syncAppsFromPublishedCrudConfigs();
        return RespInfo.success();
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ai:businessApp:list")
    @OperationLog(module = "应用入口", type = OperationType.QUERY, desc = "查询应用入口详情")
    public RespInfo<BusinessAppVO> detail(@PathVariable Long id) {
        return RespInfo.success(appService.detail(id));
    }

    @GetMapping("/{id}/open-info")
    @SaCheckPermission("ai:businessApp:open")
    @OperationLog(module = "应用入口", type = OperationType.QUERY, desc = "查询应用入口打开信息")
    public RespInfo<BusinessAppOpenInfoVO> openInfo(@PathVariable Long id) {
        return RespInfo.success(appService.openInfo(id));
    }

    @PostMapping
    @SaCheckPermission("ai:businessApp:add")
    @OperationLog(module = "应用入口", type = OperationType.ADD, desc = "新增应用入口")
    public RespInfo<Long> create(@RequestBody BusinessAppDTO dto) {
        return RespInfo.success(appService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("ai:businessApp:edit")
    @OperationLog(module = "应用入口", type = OperationType.UPDATE, desc = "修改应用入口")
    public RespInfo<Void> update(@RequestBody BusinessAppDTO dto) {
        appService.update(dto);
        return RespInfo.success();
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("ai:businessApp:status")
    @OperationLog(module = "应用入口", type = OperationType.UPDATE, desc = "启停应用入口")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        appService.updateStatus(id, status);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessApp:delete")
    @OperationLog(module = "应用入口", type = OperationType.DELETE, desc = "删除应用入口")
    public RespInfo<Void> delete(@PathVariable Long id) {
        appService.delete(id);
        return RespInfo.success();
    }
}
