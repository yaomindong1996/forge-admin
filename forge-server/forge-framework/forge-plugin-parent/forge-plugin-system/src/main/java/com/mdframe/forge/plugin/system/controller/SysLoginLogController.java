package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysLoginLog;
import com.mdframe.forge.plugin.system.service.ISysLoginLogService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志查询接口
 */
@RestController
@RequestMapping("/system/loginLog")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysLoginLogController {

    private final ISysLoginLogService loginLogService;

    /**
     * 分页查询登录日志
     */
    @GetMapping("/page")
    public RespInfo<Page<SysLoginLog>> page(
            PageQuery pageQuery,
            SysLoginLog query,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Page<SysLoginLog> page = loginLogService.selectLoginLogPage(
                pageQuery.toPage(), query, startTime, endTime);
        return RespInfo.success(page);
    }

    /**
     * 查询登录日志详情
     */
    @GetMapping("/{id}")
    public RespInfo<SysLoginLog> detail(@PathVariable Long id) {
        SysLoginLog log = loginLogService.getById(id);
        return RespInfo.success(log);
    }
}
