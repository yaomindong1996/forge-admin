package com.mdframe.forge.starter.job.flow;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动任务流程所需的可信上下文。技术身份由执行器实现自行注入。
 */
public record JobFlowExecutionRequest(
        Long jobConfigId,
        Long executionId,
        String businessKey,
        JobFlowBindingSnapshot binding,
        Map<String, Object> jobInput) {

    public JobFlowExecutionRequest {
        if (jobConfigId == null || jobConfigId <= 0) {
            throw new IllegalArgumentException("任务配置ID必须大于0");
        }
        if (executionId == null || executionId <= 0) {
            throw new IllegalArgumentException("任务执行ID必须大于0");
        }
        String expectedBusinessKey = "job:" + jobConfigId + ":" + executionId;
        if (!expectedBusinessKey.equals(businessKey)) {
            throw new IllegalArgumentException("任务流程businessKey不合法");
        }
        if (binding == null) {
            throw new IllegalArgumentException("流程绑定快照不能为空");
        }
        Map<String, Object> input = jobInput == null ? Map.of() : jobInput;
        jobInput = Collections.unmodifiableMap(new LinkedHashMap<>(input));
    }
}
