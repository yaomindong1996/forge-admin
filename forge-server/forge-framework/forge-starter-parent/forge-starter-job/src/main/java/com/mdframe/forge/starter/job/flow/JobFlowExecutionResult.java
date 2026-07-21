package com.mdframe.forge.starter.job.flow;

/**
 * 流程启动或幂等恢复结果。
 */
public record JobFlowExecutionResult(
        String businessKey,
        String processInstanceId,
        boolean recovered) {

    public JobFlowExecutionResult {
        businessKey = requireText(businessKey, "流程businessKey不能为空");
        processInstanceId = requireText(processInstanceId, "流程实例ID不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
