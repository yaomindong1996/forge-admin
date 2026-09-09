package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.mapper.FlowCcMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.WorkspaceService;
import com.mdframe.forge.starter.flow.vo.WorkspaceSummaryVO;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 我的工作台聚合服务实现。
 */
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final FlowTaskMapper flowTaskMapper;
    private final FlowCcMapper flowCcMapper;

    @Override
    public WorkspaceSummaryVO summary(String userId) {
        WorkspaceSummaryVO summary = new WorkspaceSummaryVO();
        summary.setTodoCount(todoCount(userId));
        Long tenantId = SessionHelper.getTenantId();
        summary.setDoneWeekCount(defaultZero(flowTaskMapper.countWorkspaceDoneSince(userId, startOfWeek(), tenantId)));
        summary.setStartedRunningCount(defaultZero(flowTaskMapper.countWorkspaceStartedRunning(userId, tenantId)));
        summary.setCcUnreadCount(defaultZero(flowCcMapper.countWorkspaceUnread(userId, tenantId)));
        return summary;
    }

    @Override
    public Long todoCount(String userId) {
        return defaultZero(flowTaskMapper.countWorkspaceTodo(userId, SessionHelper.getTenantId(),
                SessionHelper.getActiveOrgId()));
    }

    private LocalDateTime startOfWeek() {
        return LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atTime(LocalTime.MIN);
    }

    private Long defaultZero(Long value) {
        return value == null ? 0L : value;
    }
}
