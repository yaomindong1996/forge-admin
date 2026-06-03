package com.mdframe.forge.plugin.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 任务调度配置属性
 */
@Data
@ConfigurationProperties(prefix = "forge.job")
public class JobProperties {

    /**
     * 是否启用任务调度
     */
    private Boolean enabled = true;

    /**
     * 部署模式：STANDALONE-单体模式 DISTRIBUTED-分布式模式
     */
    private DeployMode deployMode = DeployMode.STANDALONE;

    /**
     * 分布式模式配置
     */
    private Distributed distributed = new Distributed();

    /**
     * Quartz线程池大小
     */
    private Integer threadPoolSize = 20;

    /**
     * 是否启用Quartz集群模式
     */
    private Boolean clustered = true;

    /**
     * Quartz集群节点检查间隔（毫秒）
     */
    private Long clusterCheckinInterval = 15000L;

    /**
     * Quartz misfire阈值（毫秒）
     */
    private Long misfireThreshold = 12000L;

    /**
     * Quartz表前缀
     */
    private String tablePrefix = "QRTZ_";

    @Data
    public static class Distributed {
        /**
         * 执行器服务注册中心类型：nacos, eureka, consul
         */
        private String registryType = "nacos";

        /**
         * 执行器服务名称列表（逗号分隔）
         */
        private String executorServices;

        /**
         * RPC调用超时时间（毫秒）
         */
        private Integer timeout = 30000;

        /**
         * 失败重试次数
         */
        private Integer retryCount = 3;
    }

    /**
     * 部署模式枚举
     */
    public enum DeployMode {
        /**
         * 单体模式：任务和执行器在同一进程
         */
        STANDALONE,

        /**
         * 分布式模式：调度中心和执行器分离
         */
        DISTRIBUTED
    }
}
