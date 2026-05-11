package com.mdframe.forge.plugin.external.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.external.dto.ExternalApiLogQuery;
import com.mdframe.forge.plugin.external.entity.ExternalApiLog;
import com.mdframe.forge.plugin.external.vo.ExternalApiLogSummary;

public interface ExternalApiLogService extends IService<ExternalApiLog> {

    IPage<ExternalApiLog> page(ExternalApiLogQuery query);

    ExternalApiLogSummary summary(ExternalApiLogQuery query);

    int clearLogs(ExternalApiLogQuery query);
}
