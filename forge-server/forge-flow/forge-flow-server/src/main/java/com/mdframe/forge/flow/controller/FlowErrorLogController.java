package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.flow.dto.FlowErrorLogRetryDTO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.util.PageParamResolver;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.vo.FlowErrorLogPageVO;
import com.mdframe.forge.starter.flow.vo.FlowErrorLogStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程错误日志接口
 */
@Slf4j
@RestController
@RequestMapping("/api/flow/monitor")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class FlowErrorLogController {

    private final FlowErrorLogService flowErrorLogService;

    /**
     * 分页查询错误日志
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/error-logs")
    public RespInfo<FlowErrorLogPageVO> getErrorLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String processInstanceId,
            @RequestParam(required = false) String activityId,
            @RequestParam(required = false) Integer status) {

        FlowErrorLogPageVO result = new FlowErrorLogPageVO();
        try {
            int currentPage = PageParamResolver.resolve(page, pageNum);
            int safeSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
            Page<FlowErrorLog> pageParam = new Page<>(currentPage, safeSize);
            IPage<FlowErrorLog> pageResult = flowErrorLogService.pageErrors(
                    pageParam, processInstanceId, activityId, status);

            result.setList(pageResult.getRecords());
            result.setTotal(pageResult.getTotal());
            result.setPageNum(pageResult.getCurrent());
            result.setPageSize(pageResult.getSize());
            result.setDegraded(false);
        } catch (Exception e) {
            log.error("查询错误日志失败", e);
            result.setList(Collections.emptyList());
            result.setTotal(0);
            result.setPageNum(Math.max(1, pageNum == null ? 1 : pageNum));
            result.setPageSize(Math.max(1, pageSize == null ? 10 : pageSize));
            result.setDegraded(true);
            result.setErrorCode("FLOW_ERROR_LOGS_UNAVAILABLE");
        }
        return RespInfo.success(result);
    }

    /**
     * 获取错误日志详情
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/error-logs/{logId}")
    public RespInfo<FlowErrorLog> getErrorLogDetail(@PathVariable String logId) {
        FlowErrorLog errorLog = flowErrorLogService.getCurrentTenantError(logId);
        if (errorLog == null) {
            return RespInfo.error("错误日志不存在");
        }
        return RespInfo.success(errorLog);
    }

    /**
     * 获取错误日志统计
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/error-logs/statistics")
    public RespInfo<FlowErrorLogStatisticsVO> getErrorLogStatistics() {
        FlowErrorLogStatisticsVO statistics = new FlowErrorLogStatisticsVO();
        try {
            Map<String, Object> raw = flowErrorLogService.getStatistics();
            statistics.setTotal(readLong(raw, "total"));
            statistics.setUnresolved(readLong(raw, "unresolved"));
            statistics.setRetried(readLong(raw, "retried"));
            statistics.setRetryFailed(readLong(raw, "retryFailed"));
            statistics.setDegraded(false);
        } catch (Exception e) {
            log.error("获取错误日志统计失败", e);
            statistics.setDegraded(true);
            statistics.setErrorCode("FLOW_ERROR_LOG_STATS_UNAVAILABLE");
        }
        return RespInfo.success(statistics);
    }

    private long readLong(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /**
     * 重试失败节点
     */
    @SaCheckPermission("flow:monitor:manage")
    @PostMapping("/error-logs/{logId}/retry")
    public RespInfo<Void> retryNode(
            @PathVariable String logId,
            @RequestBody(required = false) FlowErrorLogRetryDTO dto) {
        if (dto == null) {
            dto = new FlowErrorLogRetryDTO();
        }

        LoginUser loginUser = SessionHelper.getLoginUser();
        String userId = loginUser != null ? String.valueOf(loginUser.getUserId()) : null;
        String processInstanceId = optionalText(dto.getProcessInstanceId());
        String activityId = optionalText(dto.getActivityId());
        String reason = optionalText(dto.getReason());

        if (processInstanceId == null) {
            return RespInfo.error("流程实例ID不能为空");
        }
        if (reason == null) {
            reason = "管理员手动重试";
        }

        flowErrorLogService.retryNode(processInstanceId, activityId, logId, userId, reason);
        return RespInfo.success("重试成功", null);
    }

    private String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 解决错误日志（标记为已解决）
     */
    @SaCheckPermission("flow:monitor:manage")
    @PutMapping("/error-logs/{logId}/resolve")
    public RespInfo<Void> resolveError(@PathVariable String logId) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        String userId = loginUser != null ? String.valueOf(loginUser.getUserId()) : null;
        flowErrorLogService.resolveError(logId, userId);

        return RespInfo.success("已标记为已解决", null);
    }
}
