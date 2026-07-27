package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysLoginLog;
import com.mdframe.forge.plugin.system.mapper.SysLoginLogMapper;
import com.mdframe.forge.plugin.system.service.ISysLoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog>
        implements ISysLoginLogService {

    private final SysLoginLogMapper loginLogMapper;

    @Override
    public Page<SysLoginLog> selectLoginLogPage(Page<SysLoginLog> page, SysLoginLog query,
                                                String startTime, String endTime) {
        return loginLogMapper.selectLoginLogPage(page, query, startTime, endTime);
    }
}
