package com.mdframe.forge.plugin.job.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 定时任务配置
 */
@Configuration
public class ScheduleConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, JobProperties jobProperties) {

        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        if (dataSource instanceof DynamicRoutingDataSource) {
            DynamicRoutingDataSource dynamicRoutingDataSource = (DynamicRoutingDataSource) dataSource;
            DataSource master = dynamicRoutingDataSource.getDataSource("master");
            factory.setDataSource(master);
        } else {
            factory.setDataSource(dataSource);
        }
        // quartz参数
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "ForgeScheduler");
        prop.put("org.quartz.scheduler.instanceId", "AUTO");
        // 线程池配置
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", String.valueOf(normalizeThreadPoolSize(jobProperties)));
        prop.put("org.quartz.threadPool.threadPriority", "5");
        // JobStore配置
        prop.put("org.quartz.jobStore.class", "org.springframework.scheduling.quartz.LocalDataSourceJobStore");
        prop.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        prop.put("org.quartz.jobStore.isClustered", String.valueOf(Boolean.TRUE.equals(jobProperties.getClustered())));
        prop.put("org.quartz.jobStore.clusterCheckinInterval", String.valueOf(normalizePositive(
                jobProperties.getClusterCheckinInterval(), 15000L)));
        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "1");
        prop.put("org.quartz.jobStore.txIsolationLevelSerializable", "true");

        prop.put("org.quartz.jobStore.misfireThreshold", String.valueOf(normalizePositive(
                jobProperties.getMisfireThreshold(), 12000L)));
        prop.put("org.quartz.jobStore.tablePrefix", normalizeTablePrefix(jobProperties.getTablePrefix()));
        factory.setQuartzProperties(prop);

        factory.setSchedulerName("ForgeScheduler_");
        // 延时启动
        factory.setStartupDelay(1);
        factory.setApplicationContextSchedulerContextKey("applicationContextKey");
        // 可选，QuartzScheduler
        // 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了
        factory.setOverwriteExistingJobs(true);
        // 设置自动启动，默认为true
        factory.setAutoStartup(true);

        return factory;
    }

    private int normalizeThreadPoolSize(JobProperties jobProperties) {
        Integer threadPoolSize = jobProperties.getThreadPoolSize();
        if (threadPoolSize == null) {
            return 20;
        }
        return Math.min(Math.max(threadPoolSize, 1), 100);
    }

    private long normalizePositive(Long value, long fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private String normalizeTablePrefix(String tablePrefix) {
        if (tablePrefix == null || tablePrefix.isBlank()) {
            return "QRTZ_";
        }
        return tablePrefix.trim();
    }
}
