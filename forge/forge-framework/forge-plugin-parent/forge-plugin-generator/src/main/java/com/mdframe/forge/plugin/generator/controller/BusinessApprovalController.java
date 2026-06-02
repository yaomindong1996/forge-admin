package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApprovalRuntimeService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApprovalRuntimeVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 业务审批运行兼容接口。
 * <p>
 * 新页面应使用 /ai/business/flow，旧审批入口仅转发到流程运行服务。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/approval")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessApprovalController {

    private final BusinessApprovalRuntimeService approvalRuntimeService;

    @GetMapping("/runtime")
    @SaCheckPermission("ai:businessApproval:start")
    @OperationLog(module = "业务审批", type = OperationType.QUERY, desc = "查询审批运行状态")
    public RespInfo<BusinessApprovalRuntimeVO> getApprovalRuntime(
            @RequestParam String targetCode,
            @RequestParam(required = false) Long recordId) {
        return RespInfo.success(approvalRuntimeService.getApprovalRuntime(targetCode, recordId));
    }

    @PostMapping("/start")
    @SaCheckPermission("ai:businessApproval:start")
    @OperationLog(module = "业务审批", type = OperationType.ADD, desc = "发起业务审批")
    public RespInfo<Long> startApproval(
            @RequestParam String targetCode,
            @RequestParam Long recordId) {
        Long approvalId = approvalRuntimeService.startApproval(targetCode, recordId);
        return RespInfo.success(approvalId);
    }
}
