package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingBatchSaveDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessBindingService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingVO;
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
 * 业务应用平台能力挂接接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/binding")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessBindingController {

    private final BusinessBindingService bindingService;

    @GetMapping("/list")
    @SaCheckPermission("ai:businessBinding:config")
    @OperationLog(module = "能力挂接", type = OperationType.QUERY, desc = "查询能力挂接列表")
    public RespInfo<List<BusinessBindingVO>> list(BusinessBindingQueryDTO query) {
        return RespInfo.success(bindingService.list(query));
    }

    @PostMapping
    @SaCheckPermission("ai:businessBinding:config")
    @OperationLog(module = "能力挂接", type = OperationType.ADD, desc = "新增能力挂接")
    public RespInfo<Long> create(@RequestBody BusinessBindingDTO dto) {
        return RespInfo.success(bindingService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("ai:businessBinding:config")
    @OperationLog(module = "能力挂接", type = OperationType.UPDATE, desc = "修改能力挂接")
    public RespInfo<Void> update(@RequestBody BusinessBindingDTO dto) {
        bindingService.update(dto);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessBinding:config")
    @OperationLog(module = "能力挂接", type = OperationType.DELETE, desc = "删除能力挂接")
    public RespInfo<Void> delete(@PathVariable Long id) {
        bindingService.delete(id);
        return RespInfo.success();
    }

    @PostMapping("/batch-save")
    @SaCheckPermission("ai:businessBinding:config")
    @OperationLog(module = "能力挂接", type = OperationType.UPDATE, desc = "批量保存能力挂接")
    public RespInfo<Void> batchSave(@RequestBody BusinessBindingBatchSaveDTO dto) {
        bindingService.batchSave(dto);
        return RespInfo.success();
    }
}
