package com.mdframe.forge.plugin.job.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.plugin.job.service.ISysJobConfigService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 定时任务管理REST接口
 */
@RestController
@RequestMapping("/job/config")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.job", name = "enable-api", havingValue = "true", matchIfMissing = true)
@ApiPermissionIgnore
@ApiDecrypt
@ApiEncrypt
public class JobConfigController {
    
    private final ISysJobConfigService jobConfigService;
    
    /**
     * 分页查询任务列表
     */
    @GetMapping("/page")
    public RespInfo<Page<SysJobConfig>> page(PageQuery pageQuery, SysJobConfig query) {
        Page<SysJobConfig> page = jobConfigService.selectJobPage(pageQuery.toPage(), query);
        return RespInfo.success(page);
    }
    
    /**
     * 查询任务详情
     */
    @GetMapping("/{id}")
    public RespInfo<SysJobConfig> detail(@PathVariable Long id) {
        SysJobConfig config = jobConfigService.getById(id);
        return RespInfo.success(config);
    }
    
    /**
     * 添加任务
     */
    @PostMapping
    public RespInfo<Void> add(@RequestBody SysJobConfig jobConfig) {
        boolean success = jobConfigService.addJob(jobConfig);
        return success ? RespInfo.success() : RespInfo.error("添加任务失败");
    }
    
    /**
     * 更新任务
     */
    @PutMapping
    public RespInfo<Void> update(@RequestBody SysJobConfig jobConfig) {
        boolean success = jobConfigService.updateJob(jobConfig);
        return success ? RespInfo.success() : RespInfo.error("更新任务失败");
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable Long id) {
        boolean success = jobConfigService.deleteJob(id);
        return success ? RespInfo.success() : RespInfo.error("删除任务失败");
    }
    
    /**
     * 启动任务
     */
    @PostMapping("/{id}/start")
    public RespInfo<Void> start(@PathVariable Long id) {
        boolean success = jobConfigService.startJob(id);
        return success ? RespInfo.success() : RespInfo.error("启动任务失败");
    }
    
    /**
     * 停止任务
     */
    @PostMapping("/{id}/stop")
    public RespInfo<Void> stop(@PathVariable Long id) {
        boolean success = jobConfigService.stopJob(id);
        return success ? RespInfo.success() : RespInfo.error("停止任务失败");
    }
    
    /**
     * 立即执行一次
     */
    @PostMapping("/{id}/trigger")
    public RespInfo<Void> trigger(@PathVariable Long id) {
        boolean success = jobConfigService.triggerJob(id);
        return success ? RespInfo.success() : RespInfo.error("触发任务失败");
    }
    
    /**
     * 更新Cron表达式
     */
    @PostMapping("/{id}/cron")
    public RespInfo<Void> updateCron(@PathVariable Long id, @RequestParam String cronExpression) {
        boolean success = jobConfigService.updateCron(id, cronExpression);
        return success ? RespInfo.success() : RespInfo.error("更新Cron表达式失败");
    }
}
