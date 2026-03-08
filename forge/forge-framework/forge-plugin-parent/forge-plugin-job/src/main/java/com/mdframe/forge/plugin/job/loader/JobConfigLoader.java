package com.mdframe.forge.plugin.job.loader;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.plugin.job.mapper.SysJobConfigMapper;
import com.mdframe.forge.plugin.job.model.JobConfig;
import com.mdframe.forge.plugin.job.scheduler.JobScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 * 启动时加载数据库中的任务配置
 */
@Slf4j
//@Component
@ConditionalOnProperty(prefix = "forge.job", name = "auto-load", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class JobConfigLoader implements ApplicationRunner {
    
    private final SysJobConfigMapper jobConfigMapper;
    private final JobScheduler jobScheduler;
    
    @Override
    public void run(ApplicationArguments args) {
        log.info("开始加载数据库任务配置...");
        
        try {
            // 查询所有任务配置
            LambdaQueryWrapper<SysJobConfig> wrapper = new LambdaQueryWrapper<>();
            List<SysJobConfig> jobConfigs = jobConfigMapper.selectList(wrapper);
            
            if (jobConfigs == null || jobConfigs.isEmpty()) {
                log.info("数据库中没有任务配置");
                return;
            }
            
            int successCount = 0;
            int failCount = 0;
            
            for (SysJobConfig jobConfig : jobConfigs) {
                try {
                    // 转换为JobConfig
                    JobConfig config = new JobConfig();
                    config.setJobName(jobConfig.getJobName());
                    config.setJobGroup(jobConfig.getJobGroup());
                    config.setDescription(jobConfig.getDescription());
                    config.setExecutorBean(jobConfig.getExecutorBean());
                    config.setExecutorMethod(jobConfig.getExecutorMethod());
                    config.setExecutorHandler(jobConfig.getExecutorHandler());
                    config.setExecutorService(jobConfig.getExecutorService());
                    config.setCronExpression(jobConfig.getCronExpression());
                    config.setJobParam(jobConfig.getJobParam());
                    config.setStatus(jobConfig.getStatus());
                    config.setExecuteMode(jobConfig.getExecuteMode());
                    config.setRetryCount(jobConfig.getRetryCount());
                    
                    // 注册任务
                    boolean success = jobScheduler.addJob(config);
                    if (success) {
                        successCount++;
                        log.debug("加载任务成功: {}.{}", config.getJobGroup(), config.getJobName());
                    } else {
                        failCount++;
                        log.warn("加载任务失败: {}.{}", config.getJobGroup(), config.getJobName());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("加载任务异常: {}.{}", jobConfig.getJobGroup(), jobConfig.getJobName(), e);
                }
            }
            
            log.info("任务配置加载完成，成功: {}, 失败: {}, 总计: {}", successCount, failCount, jobConfigs.size());
        } catch (Exception e) {
            log.error("加载数据库任务配置失败", e);
        }
    }
}
