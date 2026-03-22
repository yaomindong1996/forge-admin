package com.mdframe.forge.starter.flow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * @date 2026/3/21
 */
@Component
@Slf4j
public class TestListener implements TaskListener, ExecutionListener {
    
    @Override
    public void notify(DelegateTask delegateTask) {
        log.info("监听器监听:{}", delegateTask.getTaskDefinitionKey());
    }
    
    @Override
    public void notify(DelegateExecution execution) {
        log.info("监听器监听DelegateExecution:{}", execution.getId());
    }
}
