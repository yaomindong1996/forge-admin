package com.mdframe.forge.plugin.job.executor;

/**
 * 任务执行器接口
 * 业务任务需实现此接口
 */
public interface IJobExecutor {
    
    /**
     * 执行任务
     * @param param 任务参数
     * @return 执行结果
     */
    String execute(String param) throws Exception;
}
