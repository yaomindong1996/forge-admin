package com.mdframe.forge.plugin.external.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.external.dto.ExternalApiLogQuery;
import com.mdframe.forge.plugin.external.entity.ExternalApiLog;
import com.mdframe.forge.plugin.external.mapper.ExternalApiLogMapper;
import com.mdframe.forge.plugin.external.service.ExternalApiLogService;
import com.mdframe.forge.plugin.external.vo.ExternalApiLogSummary;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalApiLogServiceImpl extends ServiceImpl<ExternalApiLogMapper, ExternalApiLog>
        implements ExternalApiLogService {

    private final ExternalApiLogMapper logMapper;

    @Override
    public IPage<ExternalApiLog> page(ExternalApiLogQuery query) {
        query.setTenantId(SessionHelper.getTenantId());
        Page<ExternalApiLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        return logMapper.selectLogPage(page, query);
    }

    @Override
    public ExternalApiLogSummary summary(ExternalApiLogQuery query) {
        query.setTenantId(SessionHelper.getTenantId());
        ExternalApiLogSummary summary = logMapper.selectLogSummary(query);
        if (summary == null) {
            return new ExternalApiLogSummary();
        }
        return summary;
    }

    @Override
    public int clearLogs(ExternalApiLogQuery query) {
        query.setTenantId(SessionHelper.getTenantId());
        return logMapper.clearLogs(query);
    }
}
