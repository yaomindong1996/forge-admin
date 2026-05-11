package com.mdframe.forge.plugin.external.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.external.dto.ExternalApiLogQuery;
import com.mdframe.forge.plugin.external.entity.ExternalApiLog;
import com.mdframe.forge.plugin.external.vo.ExternalApiLogSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ExternalApiLogMapper extends BaseMapper<ExternalApiLog> {

    IPage<ExternalApiLog> selectLogPage(Page<ExternalApiLog> page, @Param("query") ExternalApiLogQuery query);

    ExternalApiLogSummary selectLogSummary(@Param("query") ExternalApiLogQuery query);

    int clearLogs(@Param("query") ExternalApiLogQuery query);
}
