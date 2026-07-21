package com.mdframe.forge.plugin.job.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.job.dto.JobLogQuery;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import com.mdframe.forge.plugin.job.mapper.SysJobLogMapper;
import com.mdframe.forge.plugin.job.service.ISysJobLogService;
import com.mdframe.forge.plugin.job.service.JobManagementSecurityService;
import com.mdframe.forge.plugin.job.support.JobLogSanitizer;
import com.mdframe.forge.plugin.job.vo.JobLogDetailVO;
import com.mdframe.forge.plugin.job.vo.JobLogExportVO;
import com.mdframe.forge.plugin.job.vo.JobLogVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 任务日志Service实现
 */
@Slf4j
@Service("sysJobLogService")
@RequiredArgsConstructor
public class SysJobLogServiceImpl extends ServiceImpl<SysJobLogMapper, SysJobLog> implements ISysJobLogService {

    private static final int MAX_RETENTION_DAYS = 3650;

    private final JobLogSanitizer logSanitizer;

    private final JobManagementSecurityService managementSecurityService;
    
    @Override
    public Page<JobLogVO> selectLogPage(Page<JobLogVO> page, JobLogQuery query) {
        return this.baseMapper.selectLogPage(page, query);
    }

    @Override
    public JobLogDetailVO selectLogDetail(Long id) {
        managementSecurityService.assertSensitiveLogAccess();
        JobLogDetailVO detail = this.baseMapper.selectLogDetail(id);
        if (detail != null) {
            detail.setResultSummary(logSanitizer.sanitizeResult(detail.getResultSummary()));
            detail.setExceptionSummary(logSanitizer.sanitizeException(detail.getExceptionSummary()));
        }
        return detail;
    }

    @Override
    public List<JobLogExportVO> selectExportList(Map<String, Object> queryParams) {
        return this.baseMapper.selectExportList(toQuery(queryParams));
    }
    
    @Override
    public int cleanLog(int days) {
        if (days < 0 || days > MAX_RETENTION_DAYS) {
            throw new BusinessException("日志保留天数必须为0到3650天");
        }
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(days);
        return this.baseMapper.cleanPhysicalBefore(beforeDate);
    }

    private JobLogQuery toQuery(Map<String, Object> queryParams) {
        JobLogQuery query = new JobLogQuery();
        if (queryParams == null || queryParams.isEmpty()) {
            return query;
        }
        query.setJobConfigId(toLong(queryParams.get("jobConfigId")));
        query.setJobName(toText(queryParams.get("jobName")));
        query.setJobGroup(toText(queryParams.get("jobGroup")));
        query.setStatus(toInteger(queryParams.get("status")));
        query.setTriggerType(toText(queryParams.get("triggerType")));
        query.setStartTime(toDateTime(queryParams.get("startTime")));
        query.setEndTime(toDateTime(queryParams.get("endTime")));
        return query;
    }

    private Long toLong(Object value) {
        String text = toText(value);
        return text == null ? null : Long.valueOf(text);
    }

    private Integer toInteger(Object value) {
        String text = toText(value);
        return text == null ? null : Integer.valueOf(text);
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private LocalDateTime toDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        String text = toText(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.parse(text);
        }
    }
}
