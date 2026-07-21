package com.mdframe.forge.starter.flow.job;

import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionRequest;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionResult;
import com.mdframe.forge.starter.job.flow.JobFlowExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 与 Flowable 引擎同进程部署时使用的任务流程适配器。
 */
@Component
@RequiredArgsConstructor
public class LocalJobFlowExecutor implements JobFlowExecutor {

    private final JobFlowRuntimeService runtimeService;

    @Override
    public JobFlowBindingSnapshot validateBinding(String modelKey, Integer modelVersion) {
        return runtimeService.validateBinding(modelKey, modelVersion);
    }

    @Override
    public JobFlowExecutionResult start(JobFlowExecutionRequest request) {
        return runtimeService.start(request);
    }

    @Override
    public JobFlowExecutionResult findByBusinessKey(String businessKey) {
        return runtimeService.findByBusinessKey(businessKey);
    }
}
