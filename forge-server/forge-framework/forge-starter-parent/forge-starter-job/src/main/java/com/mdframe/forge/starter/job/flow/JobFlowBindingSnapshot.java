package com.mdframe.forge.starter.job.flow;

/**
 * 保存任务时由流程服务返回的不可变发布快照。
 */
public record JobFlowBindingSnapshot(
        String modelKey,
        Integer modelVersion,
        String deploymentId,
        String processDefinitionId) {

    public JobFlowBindingSnapshot {
        modelKey = requireText(modelKey, "流程模型Key不能为空");
        if (modelVersion == null || modelVersion <= 0) {
            throw new IllegalArgumentException("流程模型版本必须大于0");
        }
        deploymentId = requireText(deploymentId, "流程部署ID不能为空");
        processDefinitionId = requireText(processDefinitionId, "流程定义ID不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
