package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessEngineSummaryService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessEngineSummaryVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 引擎中心运行状态接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/engine")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessEngineController {

    private final BusinessEngineSummaryService engineSummaryService;

    @GetMapping("/summary")
    @SaCheckPermission("ai:businessEngine:runtime")
    @OperationLog(module = "引擎中心", type = OperationType.QUERY, desc = "查询引擎运行状态汇总")
    public RespInfo<List<BusinessEngineSummaryVO>> summary() {
        return RespInfo.success(engineSummaryService.summary());
    }
}
