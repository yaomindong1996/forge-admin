package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysOperationLogQuery;
import com.mdframe.forge.plugin.system.entity.SysOperationLog;
import com.mdframe.forge.plugin.system.mapper.SysOperationLogMapper;
import com.mdframe.forge.plugin.system.service.ISysOperationLogService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现
 */
@Service("sysOperationLogService")
@RequiredArgsConstructor
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements ISysOperationLogService {

    @Override
    public Page<SysOperationLog> page(PageQuery pageQuery, SysOperationLogQuery query) {
        Page<SysOperationLog> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        return baseMapper.selectOperationLogPage(page, query);
    }

    @Override
    public SysOperationLog detail(Long id) {
        if (id == null) {
            throw new RuntimeException("日志ID不能为空");
        }
        return baseMapper.selectOperationLogDetail(id);
    }

    @Override
    public List<SysOperationLog> selectExportList(SysOperationLogQuery query) {
        return baseMapper.selectOperationLogExportList(query);
    }
}
