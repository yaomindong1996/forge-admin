package com.mdframe.forge.plugin.job.executor;

/**
 * 任务执行器路由接口
 * 负责根据部署模式选择合适的执行方式（本地/远程）
 */
public interface IJobExecutorRouter {
    
    /**
     * 执行任务
     *
     * @param executeMode 执行模式：BEAN/HANDLER/RPC
     * @param executorBean Bean名称（BEAN模式）
     * @param executorMethod 方法名（BEAN模式）
     * @param executorHandler Handler名称（HANDLER模式）
     * @param executorService 远程服务名（RPC模式）
     * @param jobParam 任务参数
     * @return 执行结果
     * @throws Exception 执行异常
     */
    String route(String executeMode,
                 String executorBean,
                 String executorMethod,
                 String executorHandler,
                 String executorService,
                 String jobParam) throws Exception;
    
    /**
     * 判断是否支持该执行模式
     */
    boolean support(String executeMode);
}
