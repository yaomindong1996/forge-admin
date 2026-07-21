package com.mdframe.forge.plugin.job.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.job.dto.JobLogQuery;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import com.mdframe.forge.plugin.job.vo.JobLogDetailVO;
import com.mdframe.forge.plugin.job.vo.JobLogExportVO;
import com.mdframe.forge.plugin.job.vo.JobLogVO;

import java.util.List;
import java.util.Map;

/**
 * 任务日志Service
 */
public interface ISysJobLogService extends IService<SysJobLog> {
    
    /**
     * 分页查询日志
     */
    Page<JobLogVO> selectLogPage(Page<JobLogVO> page, JobLogQuery query);

    /**
     * 查询日志详情
     */
    JobLogDetailVO selectLogDetail(Long id);

    /**
     * 按动态导出引擎传入的筛选条件查询白名单字段。
     */
    List<JobLogExportVO> selectExportList(Map<String, Object> queryParams);
    
    /**
     * 清理日志
     * @param days 保留最近N天的日志
     */
    int cleanLog(int days);
}
