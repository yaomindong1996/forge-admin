package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 流程实例管理接口
 */
@RestController
@RequestMapping("/api/flow/instance")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowInstanceController {

    private final FlowInstanceService flowInstanceService;

    /**
     * 发起流程
     */
    @PostMapping("/start/{modelKey}")
    public RespInfo<String> start(
            @PathVariable String modelKey,
            @RequestBody Map<String, Object> params) {
        
        String businessKey = (String) params.get("businessKey");
        String businessType = (String) params.get("businessType");
        String title = (String) params.get("title");
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) params.get("variables");
        String userId = (String) params.get("userId");
        String userName = (String) params.get("userName");
        String deptId = (String) params.get("deptId");
        String deptName = (String) params.get("deptName");
        
        String processInstanceId = flowInstanceService.startProcess(
                modelKey, businessKey, businessType, title, variables,
                userId, userName, deptId, deptName);
        
        return RespInfo.success("流程发起成功", processInstanceId);
    }

    /**
     * 获取流程状态
     */
    @GetMapping("/status/{businessKey}")
    public RespInfo<FlowBusiness> getStatus(@PathVariable String businessKey) {
        FlowBusiness business = flowInstanceService.getProcessStatus(businessKey);
        return RespInfo.success(business);
    }

    /**
     * 终止流程
     */
    @PostMapping("/terminate/{businessKey}")
    public RespInfo<Void> terminate(
            @PathVariable String businessKey,
            @RequestBody Map<String, Object> params) {
        
        String userId = (String) params.get("userId");
        String reason = (String) params.get("reason");
        
        flowInstanceService.terminateProcess(businessKey, userId, reason);
        
        return RespInfo.success("流程已终止", null);
    }

    /**
     * 删除流程实例
     */
    @DeleteMapping("/{businessKey}")
    public RespInfo<Void> delete(
            @PathVariable String businessKey,
            @RequestParam String userId) {
        
        flowInstanceService.deleteProcess(businessKey, userId);
        
        return RespInfo.success("删除成功", null);
    }

    /**
     * 获取流程变量
     */
    @GetMapping("/variables/{businessKey}")
    public RespInfo<Map<String, Object>> getVariables(@PathVariable String businessKey) {
        Map<String, Object> variables = flowInstanceService.getProcessVariables(businessKey);
        return RespInfo.success(variables);
    }

    /**
     * 更新流程变量
     */
    @PutMapping("/variables/{businessKey}")
    public RespInfo<Void> updateVariables(
            @PathVariable String businessKey,
            @RequestBody Map<String, Object> variables) {
        
        flowInstanceService.updateProcessVariables(businessKey, variables);
        
        return RespInfo.success("更新成功", null);
    }
}
