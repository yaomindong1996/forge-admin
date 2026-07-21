package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.job.JobFlowRuntimeService;
import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionRequest;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务启动固定流程定义的专用服务接口。
 */
@RestController
@RequestMapping("/api/flow/job")
@RequiredArgsConstructor
@IgnoreTenant
@SaCheckPermission("system:jobConfig:trigger")
public class JobFlowController {

    private final JobFlowRuntimeService runtimeService;

    @PostMapping("/bindings/validate")
    public RespInfo<JobFlowBindingSnapshot> validateBinding(
            @RequestBody BindingRequest request) {
        return RespInfo.success(runtimeService.validateBinding(
                request.modelKey(), request.modelVersion()));
    }

    @PostMapping("/executions/start")
    public RespInfo<JobFlowExecutionResult> start(
            @RequestBody JobFlowExecutionRequest request) {
        return RespInfo.success(runtimeService.start(request));
    }

    @GetMapping("/executions/status")
    public RespInfo<JobFlowExecutionResult> status(
            @RequestParam String businessKey) {
        return RespInfo.success(runtimeService.findByBusinessKey(businessKey));
    }

    public record BindingRequest(String modelKey, Integer modelVersion) {
    }
}
