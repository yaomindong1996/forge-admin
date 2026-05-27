package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessBootstrapService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRuntimeInfoVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectVO;
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
 * 业务应用平台业务对象接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/object")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessObjectController {

    private final BusinessObjectService objectService;
    private final BusinessBootstrapService bootstrapService;

    @GetMapping("/page")
    @SaCheckPermission("ai:businessObject:list")
    @OperationLog(module = "业务对象", type = OperationType.QUERY, desc = "分页查询业务对象")
    public RespInfo<Page<BusinessObjectVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 BusinessObjectQueryDTO query) {
        return RespInfo.success(objectService.page(pageNum, pageSize, query));
    }

    @GetMapping("/list")
    @SaCheckPermission("ai:businessObject:list")
    @OperationLog(module = "业务对象", type = OperationType.QUERY, desc = "查询业务对象列表")
    public RespInfo<List<BusinessObjectVO>> list(BusinessObjectQueryDTO query) {
        return RespInfo.success(objectService.list(query));
    }

    @PostMapping("/sync-lowcode-models")
    @SaCheckPermission("ai:businessObject:edit")
    @OperationLog(module = "业务对象", type = OperationType.UPDATE, desc = "同步低代码模型为业务对象")
    public RespInfo<Void> syncLowcodeModels() {
        bootstrapService.syncObjectsFromLowcodeModels();
        return RespInfo.success();
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ai:businessObject:list")
    @OperationLog(module = "业务对象", type = OperationType.QUERY, desc = "查询业务对象详情")
    public RespInfo<BusinessObjectVO> detail(@PathVariable Long id) {
        return RespInfo.success(objectService.detail(id));
    }

    @GetMapping("/{id}/runtime-info")
    @SaCheckPermission("ai:businessObject:list")
    @OperationLog(module = "业务对象", type = OperationType.QUERY, desc = "查询业务对象运行信息")
    public RespInfo<BusinessObjectRuntimeInfoVO> runtimeInfo(@PathVariable Long id) {
        return RespInfo.success(objectService.runtimeInfo(id));
    }

    @PostMapping
    @SaCheckPermission("ai:businessObject:add")
    @OperationLog(module = "业务对象", type = OperationType.ADD, desc = "新增业务对象")
    public RespInfo<Long> create(@RequestBody BusinessObjectDTO dto) {
        return RespInfo.success(objectService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("ai:businessObject:edit")
    @OperationLog(module = "业务对象", type = OperationType.UPDATE, desc = "修改业务对象")
    public RespInfo<Void> update(@RequestBody BusinessObjectDTO dto) {
        objectService.update(dto);
        return RespInfo.success();
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("ai:businessObject:edit")
    @OperationLog(module = "业务对象", type = OperationType.UPDATE, desc = "启停业务对象")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        objectService.updateStatus(id, status);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessObject:delete")
    @OperationLog(module = "业务对象", type = OperationType.DELETE, desc = "删除业务对象")
    public RespInfo<Void> delete(@PathVariable Long id) {
        objectService.delete(id);
        return RespInfo.success();
    }
}
