package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessSuiteDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessSuiteQueryDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessBootstrapService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessSuiteService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessSuiteVO;
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
 * 业务应用平台业务套件接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/suite")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessSuiteController {

    private final BusinessSuiteService suiteService;
    private final BusinessBootstrapService bootstrapService;

    @GetMapping("/page")
    @SaCheckPermission("ai:businessSuite:list")
    @OperationLog(module = "业务套件", type = OperationType.QUERY, desc = "分页查询业务套件")
    public RespInfo<Page<BusinessSuiteVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                BusinessSuiteQueryDTO query) {
        return RespInfo.success(suiteService.page(pageNum, pageSize, query));
    }

    @GetMapping("/list")
    @SaCheckPermission("ai:businessSuite:list")
    @OperationLog(module = "业务套件", type = OperationType.QUERY, desc = "查询业务套件列表")
    public RespInfo<List<BusinessSuiteVO>> list(BusinessSuiteQueryDTO query) {
        return RespInfo.success(suiteService.list(query));
    }

    @GetMapping("/summary")
    @SaCheckPermission("ai:businessSuite:list")
    @OperationLog(module = "业务套件", type = OperationType.QUERY, desc = "查询业务套件汇总")
    public RespInfo<List<BusinessSuiteSummaryVO>> summary() {
        return RespInfo.success(suiteService.summary());
    }

    @PostMapping("/sync-lowcode-domains")
    @SaCheckPermission("ai:businessSuite:edit")
    @OperationLog(module = "业务套件", type = OperationType.UPDATE, desc = "同步低代码领域为业务套件")
    public RespInfo<Void> syncLowcodeDomains() {
        bootstrapService.syncSuitesFromLowcodeDomains();
        return RespInfo.success();
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ai:businessSuite:list")
    @OperationLog(module = "业务套件", type = OperationType.QUERY, desc = "查询业务套件详情")
    public RespInfo<BusinessSuiteVO> detail(@PathVariable Long id) {
        return RespInfo.success(suiteService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("ai:businessSuite:edit")
    @OperationLog(module = "业务套件", type = OperationType.ADD, desc = "新增业务套件")
    public RespInfo<Long> create(@RequestBody BusinessSuiteDTO dto) {
        return RespInfo.success(suiteService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("ai:businessSuite:edit")
    @OperationLog(module = "业务套件", type = OperationType.UPDATE, desc = "修改业务套件")
    public RespInfo<Void> update(@RequestBody BusinessSuiteDTO dto) {
        suiteService.update(dto);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessSuite:edit")
    @OperationLog(module = "业务套件", type = OperationType.DELETE, desc = "删除业务套件")
    public RespInfo<Void> delete(@PathVariable Long id) {
        suiteService.delete(id);
        return RespInfo.success();
    }
}
