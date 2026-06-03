package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 业务流程配置接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/flow")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessFlowController {

    private final BusinessFlowService flowService;

    @GetMapping("/binding/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询业务流程绑定")
    public RespInfo<BusinessFlowBindingVO> getBinding(@PathVariable String objectCode) {
        return RespInfo.success(flowService.getFlowBinding(objectCode));
    }

    @GetMapping("/model/{modelKey}/variables")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询流程变量候选项")
    public RespInfo<Map<String, Object>> variables(@PathVariable String modelKey,
                                                   @RequestParam(required = false) String objectCode) {
        return RespInfo.success(flowService.getVariableCandidates(modelKey, objectCode));
    }

    @PutMapping("/binding/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "保存业务流程绑定")
    public RespInfo<Void> saveBinding(@PathVariable String objectCode,
                                      @RequestBody BusinessFlowBindingDTO dto) {
        flowService.saveFlowBinding(objectCode, dto);
        return RespInfo.success();
    }

    @PostMapping("/start")
    @SaCheckPermission("ai:businessFlow:start")
    @OperationLog(module = "业务流程", type = OperationType.ADD, desc = "发起业务流程")
    public RespInfo<BusinessFlowRuntimeVO> start(@RequestBody BusinessFlowStartDTO dto) {
        return RespInfo.success(flowService.startDocumentFlow(dto));
    }

    @GetMapping("/status/{objectCode}/{recordId}")
    @SaCheckPermission("ai:businessFlow:view")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询业务流程状态")
    public RespInfo<BusinessFlowRuntimeVO> status(@PathVariable String objectCode,
                                                 @PathVariable Long recordId) {
        return RespInfo.success(flowService.getFlowStatus(objectCode, recordId));
    }

    @PostMapping("/callback")
    @SaCheckPermission("ai:businessFlow:callback")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "处理业务流程回调")
    public RespInfo<Void> callback(@RequestBody BusinessFlowCallbackDTO dto) {
        flowService.handleFlowCallback(dto);
        return RespInfo.success();
    }
}
