package com.mdframe.forge.plugin.job.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.plugin.job.service.ISysJobLogService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 任务日志REST接口
 */
@RestController
@RequestMapping("/job/log")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge.job", name = "enable-api", havingValue = "true", matchIfMissing = true)
@ApiPermissionIgnore
@ApiDecrypt
@ApiEncrypt
public class JobLogController {
    
    private final ISysJobLogService jobLogService;
    
    /**
     * 分页查询日志
     */
    @GetMapping("/page")
    public RespInfo<Page<SysJobLog>> page(PageQuery pageQuery, SysJobLog query) {
        Page<SysJobLog> page = jobLogService.selectLogPage(pageQuery.toPage(), query);
        return RespInfo.success(page);
    }
    
    /**
     * 查询日志详情
     */
    @GetMapping("/{id}")
    public RespInfo<SysJobLog> detail(@PathVariable Long id) {
        SysJobLog log = jobLogService.getById(id);
        return RespInfo.success(log);
    }
    
    /**
     * 清理日志
     */
    @DeleteMapping("/clean")
    public RespInfo<Integer> clean(@RequestParam(defaultValue = "30") int days) {
        int count = jobLogService.cleanLog(days);
        return RespInfo.success(count);
    }
}
