package com.mdframe.forge.plugin.job.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.job.constant.JobInvokeMode;
import com.mdframe.forge.plugin.job.dto.JobConfigSaveRequest;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionRequest;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionResult;
import com.mdframe.forge.starter.job.flow.JobFlowExecutor;
import lombok.RequiredArgsConstructor;
import org.quartz.JobDataMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 管理任务流程的可信绑定快照与幂等启动边界。
 */
@Service
@RequiredArgsConstructor
public class JobFlowOrchestrationService {

    private static final TypeReference<Map<String, Object>> JOB_INPUT_TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final ObjectProvider<JobFlowExecutor> flowExecutorProvider;

    public void applyBinding(JobConfigSaveRequest request, SysJobConfig target) {
        if (request == null || target == null) {
            throw new BusinessException("任务流程绑定参数不能为空");
        }
        target.setInvokeMode(request.getInvokeMode());
        if (JobInvokeMode.SINGLE.equals(request.getInvokeMode())) {
            clearFlowBinding(target);
            return;
        }

        JobFlowBindingSnapshot snapshot = requireExecutor().validateBinding(
                request.getFlowModelKey(), request.getFlowModelVersion());
        if (snapshot == null) {
            throw new BusinessException("流程服务未返回可信绑定快照");
        }
        if (!Objects.equals(request.getFlowModelKey(), snapshot.modelKey())
                || !Objects.equals(request.getFlowModelVersion(), snapshot.modelVersion())) {
            throw new BusinessException("流程服务返回的绑定快照与请求模型版本不一致");
        }
        clearExecutionTarget(target);
        target.setFlowModelKey(snapshot.modelKey());
        target.setFlowModelVersion(snapshot.modelVersion());
        target.setFlowDeploymentId(snapshot.deploymentId());
        target.setFlowProcessDefinitionId(snapshot.processDefinitionId());
    }

    public String start(Long jobConfigId, Long executionId, JobDataMap jobDataMap) {
        if (jobDataMap == null) {
            throw new BusinessException("任务流程调度快照不能为空");
        }
        String businessKey = "job:" + jobConfigId + ":" + executionId;
        JobFlowExecutionRequest request;
        try {
            request = new JobFlowExecutionRequest(
                    jobConfigId,
                    executionId,
                    businessKey,
                    bindingFrom(jobDataMap),
                    parseJobInput(jobDataMap.getString("jobParam")));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("任务流程调度快照不合法", exception);
        }

        JobFlowExecutor executor = requireExecutor();
        return requireMatchingResult(executor.start(request), businessKey).processInstanceId();
    }

    private JobFlowBindingSnapshot bindingFrom(JobDataMap jobDataMap) {
        return new JobFlowBindingSnapshot(
                jobDataMap.getString("flowModelKey"),
                integerValue(jobDataMap, "flowModelVersion"),
                jobDataMap.getString("flowDeploymentId"),
                jobDataMap.getString("flowProcessDefinitionId"));
    }

    private Map<String, Object> parseJobInput(String jobParam) {
        if (jobParam == null || jobParam.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> input = objectMapper.readValue(jobParam, JOB_INPUT_TYPE);
            if (input == null) {
                throw new BusinessException("FLOW任务参数必须是合法JSON对象");
            }
            return new LinkedHashMap<>(input);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("FLOW任务参数必须是合法JSON对象", exception);
        }
    }

    private JobFlowExecutionResult requireMatchingResult(JobFlowExecutionResult result,
                                                         String businessKey) {
        if (result == null) {
            throw new BusinessException("流程服务未返回流程实例信息");
        }
        if (!businessKey.equals(result.businessKey())) {
            throw new BusinessException("流程服务返回的businessKey与任务执行不一致");
        }
        return result;
    }

    private JobFlowExecutor requireExecutor() {
        JobFlowExecutor executor = flowExecutorProvider.getIfAvailable();
        if (executor == null) {
            throw new BusinessException("未配置任务流程执行器");
        }
        return executor;
    }

    private Integer integerValue(JobDataMap jobDataMap, String key) {
        Object value = jobDataMap.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void clearExecutionTarget(SysJobConfig target) {
        target.setExecuteMode(null);
        target.setExecutorBean(null);
        target.setExecutorMethod(null);
        target.setExecutorHandler(null);
        target.setExecutorService(null);
    }

    private void clearFlowBinding(SysJobConfig target) {
        target.setFlowModelKey(null);
        target.setFlowModelVersion(null);
        target.setFlowDeploymentId(null);
        target.setFlowProcessDefinitionId(null);
    }
}
