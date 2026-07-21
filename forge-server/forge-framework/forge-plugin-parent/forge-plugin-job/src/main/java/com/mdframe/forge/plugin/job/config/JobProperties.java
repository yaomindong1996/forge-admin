package com.mdframe.forge.plugin.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

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
     * 独立服务账号开放 API 配置。
     */
    private OpenApi openApi = new OpenApi();

    /**
     * 远程执行器服务间认证 Token，只允许通过部署配置注入。
     */
    private String executorToken;

    /**
     * 运行中任务心跳间隔。
     */
    private Duration executionHeartbeatInterval = Duration.ofSeconds(30);

    /**
     * 启动时判定执行记录失联的超时时间。
     */
    private Duration executionRecoveryTimeout = Duration.ofMinutes(15);

    /**
     * 获取 Quartz 配置同步分布式锁的最长等待时间。
     */
    private Long scheduleSyncLockWaitMillis = 5000L;

    /**
     * Quartz表前缀
     */
    private String tablePrefix = "QRTZ_";

    public String validatedExecutorToken() {
        if (executorToken == null || executorToken.trim().length() < 32) {
            throw new IllegalStateException("任务执行器服务Token必须配置且至少32个字符");
        }
        return executorToken.trim();
    }

    public Duration validatedExecutionHeartbeatInterval() {
        if (executionHeartbeatInterval == null
                || executionHeartbeatInterval.compareTo(Duration.ofSeconds(5)) < 0
                || executionHeartbeatInterval.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalStateException("任务执行心跳间隔必须为5秒到5分钟");
        }
        return executionHeartbeatInterval;
    }

    public Duration validatedExecutionRecoveryTimeout() {
        if (executionRecoveryTimeout == null
                || executionRecoveryTimeout.compareTo(Duration.ofMinutes(1)) < 0
                || executionRecoveryTimeout.compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalStateException("任务执行恢复超时必须为1分钟到24小时");
        }
        Duration heartbeat = validatedExecutionHeartbeatInterval();
        if (executionRecoveryTimeout.compareTo(heartbeat.multipliedBy(2)) <= 0) {
            throw new IllegalStateException("任务执行恢复超时必须大于心跳间隔的两倍");
        }
        return executionRecoveryTimeout;
    }

    public long validatedScheduleSyncLockWaitMillis() {
        if (scheduleSyncLockWaitMillis == null
                || scheduleSyncLockWaitMillis < 0
                || scheduleSyncLockWaitMillis > 30000) {
            throw new IllegalStateException("任务同步锁等待时间必须为0到30000毫秒");
        }
        return scheduleSyncLockWaitMillis;
    }

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

    @Data
    public static class OpenApi {

        private Boolean enabled = true;

        private String tokenPepper;

        private Duration idempotencyTtl = Duration.ofHours(24);

        private Duration lastUsedTouchInterval = Duration.ofMinutes(1);

        private Integer readRateLimitPerMinute = 120;

        private Integer triggerRateLimitPerMinute = 20;

        private Long idempotencyLockWaitMillis = 2000L;

        private Long idempotencyLockLeaseMillis = 30000L;

        public Duration validatedIdempotencyTtl() {
            if (idempotencyTtl == null || idempotencyTtl.compareTo(Duration.ofMinutes(1)) < 0
                    || idempotencyTtl.compareTo(Duration.ofHours(24)) > 0) {
                throw new IllegalStateException("开放API幂等有效期必须为1分钟到24小时");
            }
            return idempotencyTtl;
        }

        public Duration validatedLastUsedTouchInterval() {
            if (lastUsedTouchInterval == null || lastUsedTouchInterval.isNegative()
                    || lastUsedTouchInterval.compareTo(Duration.ofMinutes(5)) > 0) {
                throw new IllegalStateException("开放API最后使用时间节流必须为0到5分钟");
            }
            return lastUsedTouchInterval;
        }

        public int validatedReadRateLimit() {
            return validateRateLimit(readRateLimitPerMinute, "读取");
        }

        public int validatedTriggerRateLimit() {
            return validateRateLimit(triggerRateLimitPerMinute, "触发");
        }

        private int validateRateLimit(Integer value, String type) {
            if (value == null || value < 1 || value > 10000) {
                throw new IllegalStateException("开放API" + type + "限流必须为1到10000次/分钟");
            }
            return value;
        }
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
