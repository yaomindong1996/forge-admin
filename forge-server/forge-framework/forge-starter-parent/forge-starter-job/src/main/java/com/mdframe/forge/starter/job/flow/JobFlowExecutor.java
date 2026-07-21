package com.mdframe.forge.starter.job.flow;

/**
 * 定时任务启动固定 Flowable 流程定义的中立适配契约。
 */
public interface JobFlowExecutor {

    JobFlowBindingSnapshot validateBinding(String modelKey, Integer modelVersion);

    JobFlowExecutionResult start(JobFlowExecutionRequest request);

    JobFlowExecutionResult findByBusinessKey(String businessKey);
}
