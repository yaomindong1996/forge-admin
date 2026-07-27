package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.system.entity.SysLoginLog;

public interface ISysLoginLogService extends IService<SysLoginLog> {

    Page<SysLoginLog> selectLoginLogPage(Page<SysLoginLog> page, SysLoginLog query,
                                         String startTime, String endTime);
}
