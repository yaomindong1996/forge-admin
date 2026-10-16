package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.system.dto.SysOperationLogQuery;
import com.mdframe.forge.plugin.system.entity.SysOperationLog;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.List;

/**
 * 操作日志服务
 */
public interface ISysOperationLogService extends IService<SysOperationLog> {

    /**
     * 分页查询操作日志。
     */
    Page<SysOperationLog> page(PageQuery pageQuery, SysOperationLogQuery query);

    /**
     * 查询操作日志详情。
     */
    SysOperationLog detail(Long id);

    /**
     * 查询导出列表。
     */
    List<SysOperationLog> selectExportList(SysOperationLogQuery query);
}
