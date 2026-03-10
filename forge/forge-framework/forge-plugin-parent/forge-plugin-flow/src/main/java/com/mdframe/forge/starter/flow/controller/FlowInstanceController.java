package com.mdframe.forge.starter.flow.controller;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程实例管理接口
 */
@RestController
@RequestMapping("/api/flow/instance")
@RequiredArgsConstructor
public class FlowInstanceController {

    private final FlowInstanceService flowInstanceService;

    /**
     * 发起流程
     */
    @PostMapping("/start/{modelKey}")
    public Map<String, Object> start(
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
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "流程发起成功");
        response.put("processInstanceId", processInstanceId);
        return response;
    }

    /**
     * 获取流程状态
     */
    @GetMapping("/status/{businessKey}")
    public Map<String, Object> getStatus(@PathVariable String businessKey) {
        FlowBusiness business = flowInstanceService.getProcessStatus(businessKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", business);
        return response;
    }

    /**
     * 终止流程
     */
    @PostMapping("/terminate/{businessKey}")
    public Map<String, Object> terminate(
            @PathVariable String businessKey,
            @RequestBody Map<String, Object> params) {
        
        String userId = (String) params.get("userId");
        String reason = (String) params.get("reason");
        
        flowInstanceService.terminateProcess(businessKey, userId, reason);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "流程已终止");
        return response;
    }

    /**
     * 删除流程实例
     */
    @DeleteMapping("/{businessKey}")
    public Map<String, Object> delete(
            @PathVariable String businessKey,
            @RequestParam String userId) {
        
        flowInstanceService.deleteProcess(businessKey, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "删除成功");
        return response;
    }

    /**
     * 获取流程变量
     */
    @GetMapping("/variables/{businessKey}")
    public Map<String, Object> getVariables(@PathVariable String businessKey) {
        Map<String, Object> variables = flowInstanceService.getProcessVariables(businessKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", variables);
        return response;
    }

    /**
     * 更新流程变量
     */
    @PutMapping("/variables/{businessKey}")
    public Map<String, Object> updateVariables(
            @PathVariable String businessKey,
            @RequestBody Map<String, Object> variables) {
        
        flowInstanceService.updateProcessVariables(businessKey, variables);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "更新成功");
        return response;
    }
}