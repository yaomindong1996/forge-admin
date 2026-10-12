package com.mdframe.forge.plugin.ai.agent.engine.context;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

/** 显式装饰提交的任务，避免全局线程池 BeanPostProcessor 覆盖专用池的身份传递。 */
@Component
public class AgentLoopTaskExecutor {

    private final AsyncTaskExecutor executor;
    private final AgentExecutionTaskDecorator decorator;

    public AgentLoopTaskExecutor(@Qualifier("forgeAgentTaskExecutor") AsyncTaskExecutor executor,
                                 AgentExecutionTaskDecorator decorator) {
        this.executor = executor;
        this.decorator = decorator;
    }

    public void execute(Runnable task) {
        executor.execute(decorator.decorate(task));
    }
}
