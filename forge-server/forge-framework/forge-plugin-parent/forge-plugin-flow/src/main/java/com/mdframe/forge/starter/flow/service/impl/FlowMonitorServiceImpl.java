package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.enums.FlowBusinessStatus;
import com.mdframe.forge.starter.flow.enums.FlowTaskStatus;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowCcMapper;
import com.mdframe.forge.starter.flow.mapper.FlowCommentMapper;
import com.mdframe.forge.starter.flow.mapper.FlowErrorLogMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFillBatchItemMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowMonitorService;
import com.mdframe.forge.starter.flow.service.support.FlowCleanupTransactionExecutor;
import com.mdframe.forge.starter.flow.spi.FlowMonitorUserLookup;
import com.mdframe.forge.starter.flow.vo.FlowMonitorDailyStatVO;
import com.mdframe.forge.starter.flow.vo.FlowMonitorProcessInstanceDetailVO;
import com.mdframe.forge.starter.flow.vo.FlowMonitorProcessInstancePageVO;
import com.mdframe.forge.starter.flow.vo.FlowMonitorProcessInstanceVO;
import com.mdframe.forge.starter.flow.vo.FlowMonitorStatisticsVO;
import com.mdframe.forge.starter.flow.vo.FlowMonitorTaskPageVO;
import com.mdframe.forge.starter.flow.vo.FlowMonitorTaskTreeItemVO;
import com.mdframe.forge.starter.flow.vo.FlowMonitorTaskTreeNodeVO;
import com.mdframe.forge.starter.flow.vo.FlowMonitorTaskTrendVO;
import com.mdframe.forge.starter.flow.vo.FlowProcessDistributionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 流程监控服务实现
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowMonitorServiceImpl implements FlowMonitorService {

    private static final String PROCESS_CLEANUP_FAILURE_MESSAGE = "流程数据删除失败";

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final ManagementService managementService;
    private final ObjectProvider<FlowMonitorUserLookup> userLookupProvider;
    private final FlowCleanupTransactionExecutor cleanupTransactionExecutor;
    private final FlowBusinessMapper flowBusinessMapper;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowCommentMapper flowCommentMapper;
    private final FlowCcMapper flowCcMapper;
    private final FlowErrorLogMapper flowErrorLogMapper;
    private final FlowFormInstanceMapper flowFormInstanceMapper;
    private final FlowFillBatchItemMapper flowFillBatchItemMapper;

    @Override
    public Map<String, Object> getProcessInstanceOverview() {
        Map<String, Object> result = new HashMap<>();
        
        // 运行中的流程实例数量
        long runningCount = runtimeService.createProcessInstanceQuery().count();
        result.put("runningCount", runningCount);
        
        // 已完成的流程实例数量
        long completedCount = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .count();
        result.put("completedCount", completedCount);
        
        // 挂起的流程实例数量
        long suspendedCount = runtimeService.createProcessInstanceQuery()
                .suspended()
                .count();
        result.put("suspendedCount", suspendedCount);
        
        // 今日新增
        long todayNewCount = historyService.createHistoricProcessInstanceQuery()
                .startedAfter(getTodayStart())
                .count();
        result.put("todayNewCount", todayNewCount);
        
        // 今日完成
        long todayCompletedCount = historyService.createHistoricProcessInstanceQuery()
                .finishedAfter(getTodayStart())
                .count();
        result.put("todayCompletedCount", todayCompletedCount);
        
        return result;
    }

    @Override
    public Map<String, Object> getTaskOverview() {
        Map<String, Object> result = new HashMap<>();
        
        // 待办任务数量
        long todoCount = taskService.createTaskQuery().count();
        result.put("todoCount", todoCount);
        
        // 已办任务数量（今日）
        long doneCount = historyService.createHistoricTaskInstanceQuery()
                .taskCompletedAfter(getTodayStart())
                .count();
        result.put("doneCount", doneCount);
        
        // 候选任务数量（未分配办理人的任务）
        long candidateCount = taskService.createTaskQuery()
                .taskUnassigned()
                .count();
        result.put("candidateCount", candidateCount);
        
        // 尚未接入节点超时配置计算，不能把全部活动任务误报为超时任务。
        result.put("timeoutCount", null);
        result.put("timeoutMetricsAvailable", false);
        
        return result;
    }

    @Override
    public Map<String, Object> getProcessInstanceStats(String processDefinitionKey) {
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止查看流程统计");
        Map<String, Object> aggregated = flowBusinessMapper.selectProcessInstanceStats(
                tenantId, processDefinitionKey);
        if (aggregated == null) {
            aggregated = new HashMap<>();
        }
        aggregated.putIfAbsent("runningCount", 0L);
        aggregated.putIfAbsent("completedCount", 0L);
        aggregated.putIfAbsent("avgDuration", 0L);
        return aggregated;
    }

    @Override
    public Map<String, Object> getProcessInstanceList(String processDefinitionKey, String status, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> instances = new ArrayList<>();
        
        int firstResult = (pageNum - 1) * pageSize;
        
        List<ProcessInstance> processInstances;
        long total;
        
        if (FlowBusinessStatus.RUNNING.matches(status)) {
            processInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .active()
                    .listPage(firstResult, pageSize);
            total = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .active()
                    .count();
        } else if (FlowBusinessStatus.SUSPENDED.matches(status)) {
            processInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .suspended()
                    .listPage(firstResult, pageSize);
            total = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .suspended()
                    .count();
        } else if (FlowBusinessStatus.COMPLETED.matches(status)) {
            List<HistoricProcessInstance> historicInstances = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .finished()
                    .orderByProcessInstanceEndTime()
                    .desc()
                    .listPage(firstResult, pageSize);
            
            for (HistoricProcessInstance hpi : historicInstances) {
                Map<String, Object> instance = new HashMap<>();
                instance.put("id", hpi.getId());
                instance.put("processDefinitionId", hpi.getProcessDefinitionId());
                instance.put("processDefinitionKey", hpi.getProcessDefinitionKey());
                instance.put("processDefinitionName", hpi.getProcessDefinitionName());
                instance.put("startTime", hpi.getStartTime());
                instance.put("endTime", hpi.getEndTime());
                instance.put("durationInMillis", hpi.getDurationInMillis());
                instance.put("startUserId", hpi.getStartUserId());
                instance.put("status", FlowBusinessStatus.COMPLETED.getCode());
                instances.add(instance);
            }
            
            total = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .finished()
                    .count();
            
            result.put("list", instances);
            result.put("total", total);
            return result;
        } else {
            processInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .listPage(firstResult, pageSize);
            total = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .count();
        }
        
        for (ProcessInstance pi : processInstances) {
            Map<String, Object> instance = new HashMap<>();
            instance.put("id", pi.getId());
            instance.put("processDefinitionId", pi.getProcessDefinitionId());
            instance.put("processDefinitionKey", pi.getProcessDefinitionKey());
            instance.put("processDefinitionName", pi.getProcessDefinitionName());
            instance.put("startTime", pi.getStartTime());
            instance.put("startUserId", pi.getStartUserId());
            instance.put("status", pi.isSuspended()
                    ? FlowBusinessStatus.SUSPENDED.getCode()
                    : FlowBusinessStatus.RUNNING.getCode());
            instance.put("businessKey", pi.getBusinessKey());
            instances.add(instance);
        }
        
        result.put("list", instances);
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> getProcessInstanceDetail(String processInstanceId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取流程实例信息
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        
        if (processInstance != null) {
            result.put("id", processInstance.getId());
            result.put("processDefinitionId", processInstance.getProcessDefinitionId());
            result.put("processDefinitionKey", processInstance.getProcessDefinitionKey());
            result.put("processDefinitionName", processInstance.getProcessDefinitionName());
            result.put("startTime", processInstance.getStartTime());
            result.put("startUserId", processInstance.getStartUserId());
            result.put("businessKey", processInstance.getBusinessKey());
            result.put("status", processInstance.isSuspended()
                    ? FlowBusinessStatus.SUSPENDED.getCode()
                    : FlowBusinessStatus.RUNNING.getCode());
        } else {
            // 从历史获取
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            
            if (hpi != null) {
                result.put("id", hpi.getId());
                result.put("processDefinitionId", hpi.getProcessDefinitionId());
                result.put("processDefinitionKey", hpi.getProcessDefinitionKey());
                result.put("processDefinitionName", hpi.getProcessDefinitionName());
                result.put("startTime", hpi.getStartTime());
                result.put("endTime", hpi.getEndTime());
                result.put("durationInMillis", hpi.getDurationInMillis());
                result.put("startUserId", hpi.getStartUserId());
                result.put("businessKey", hpi.getBusinessKey());
                result.put("status", FlowBusinessStatus.COMPLETED.getCode());
            }
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getExecutionHistory(String processInstanceId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
        
        for (HistoricActivityInstance activity : activities) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", activity.getId());
            item.put("activityId", activity.getActivityId());
            item.put("activityName", activity.getActivityName());
            item.put("activityType", activity.getActivityType());
            item.put("assignee", activity.getAssignee());
            item.put("startTime", activity.getStartTime());
            item.put("endTime", activity.getEndTime());
            item.put("durationInMillis", activity.getDurationInMillis());
            result.add(item);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getTaskExecutionStats(String processDefinitionKey, String startTime, String endTime) {
        throw monitorCapabilityUnavailable("任务执行统计");
    }

    @Override
    public List<Map<String, Object>> getTimeoutTasks(String processDefinitionKey) {
        throw monitorCapabilityUnavailable("超时任务查询");
    }

    @Override
    public List<Map<String, Object>> getUpcomingTimeoutTasks(String processDefinitionKey, int advanceMinutes) {
        throw monitorCapabilityUnavailable("即将超时任务查询");
    }

    @Override
    public Map<String, Object> getProcessEfficiencyAnalysis(String processDefinitionKey, String startTime, String endTime) {
        throw monitorCapabilityUnavailable("流程效率分析");
    }

    @Override
    public List<Map<String, Object>> getNodeDurationStats(String processDefinitionKey, String startTime, String endTime) {
        throw monitorCapabilityUnavailable("节点耗时统计");
    }

    @Override
    public List<Map<String, Object>> getApproverEfficiencyStats(String userId, String startTime, String endTime) {
        throw monitorCapabilityUnavailable("审批人效率统计");
    }

    @Override
    public Map<String, Object> getProcessBottleneckAnalysis(String processDefinitionKey) {
        throw monitorCapabilityUnavailable("流程瓶颈分析");
    }

    private BusinessException monitorCapabilityUnavailable(String capability) {
        return new BusinessException(capability + "尚未实现，当前接口已关闭以避免返回伪造统计结果");
    }

    @Override
    public List<Map<String, Object>> getActiveNodes(String processInstanceId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        
        for (Task task : tasks) {
            Map<String, Object> node = new HashMap<>();
            node.put("taskId", task.getId());
            node.put("taskName", task.getName());
            node.put("taskDefinitionKey", task.getTaskDefinitionKey());
            node.put("assignee", task.getAssignee());
            node.put("createTime", task.getCreateTime());
            result.add(node);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        Map<String, Object> variables = new HashMap<>();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance != null) {
            variables.putAll(runtimeService.getVariables(processInstanceId));
            return variables;
        }

        List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (HistoricVariableInstance variable : historicVariables) {
            variables.put(variable.getVariableName(), variable.getValue());
        }
        return variables;
    }

    @Override
    public Map<String, Object> getProcessDiagramHighlight(String processInstanceId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取已完成的节点
        List<String> completedActivityIds = new ArrayList<>();
        List<HistoricActivityInstance> completedActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .finished()
                .list();
        
        for (HistoricActivityInstance activity : completedActivities) {
            completedActivityIds.add(activity.getActivityId());
        }
        result.put("completedActivityIds", completedActivityIds);
        
        // 获取当前活动节点
        List<String> activeActivityIds = new ArrayList<>();
        List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
        
        for (Task task : activeTasks) {
            activeActivityIds.add(task.getTaskDefinitionKey());
        }
        result.put("activeActivityIds", activeActivityIds);
        
        // 获取已执行的连线
        List<String> completedSequenceFlowIds = new ArrayList<>();
        // TODO: 获取已执行的连线
        
        result.put("completedSequenceFlowIds", completedSequenceFlowIds);
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getDeploymentStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionKey()
                .asc()
                .list();
        
        Map<String, Map<String, Object>> statsMap = new LinkedHashMap<>();
        
        for (ProcessDefinition pd : processDefinitions) {
            String key = pd.getKey();
            if (!statsMap.containsKey(key)) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("key", key);
                stats.put("name", pd.getName());
                stats.put("version", pd.getVersion());
                stats.put("deploymentId", pd.getDeploymentId());
                stats.put("suspended", pd.isSuspended());
                statsMap.put(key, stats);
            }
        }
        
        result.addAll(statsMap.values());
        return result;
    }

    @Override
    public List<Map<String, Object>> getProcessDefinitionVersions(String processDefinitionKey) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        List<ProcessDefinition> versions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .orderByProcessDefinitionVersion()
                .desc()
                .list();
        
        for (ProcessDefinition pd : versions) {
            Map<String, Object> version = new HashMap<>();
            version.put("id", pd.getId());
            version.put("key", pd.getKey());
            version.put("name", pd.getName());
            version.put("version", pd.getVersion());
            version.put("deploymentId", pd.getDeploymentId());
            version.put("suspended", pd.isSuspended());
            result.add(version);
        }
        
        return result;
    }

    @Override
    public IPage<FlowBusiness> getBusinessPage(int pageNum,
                                               int pageSize,
                                               String processDefKey,
                                               String status,
                                               String title,
                                               String applyUserId) {
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止查看流程实例");
        return flowBusinessMapper.selectBusinessPage(
                new Page<>(pageNum, pageSize), tenantId, processDefKey, status, title, applyUserId);
    }

    @Override
    public FlowBusiness getBusinessByProcessInstanceId(String processInstanceId) {
        return requireCurrentTenantProcessInstance(processInstanceId);
    }

    @Override
    public FlowMonitorStatisticsVO getAdminStatistics() {
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止查看流程监控统计");
        try {
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            Map<String, Object> statistics = flowBusinessMapper.selectMonitorStatistics(tenantId, startOfDay);
            if (statistics == null) {
                return degradedAdminStatistics("FLOW_MONITOR_STATS_UNAVAILABLE");
            }
            return toMonitorStatistics(statistics);
        } catch (Exception e) {
            log.error("获取统计数据失败：tenantId={}", tenantId, e);
            return degradedAdminStatistics("FLOW_MONITOR_STATS_UNAVAILABLE");
        }
    }

    @Override
    public FlowMonitorProcessInstancePageVO getAdminProcessInstances(int pageNum,
                                                                     int pageSize,
                                                                     String processName,
                                                                     String initiator,
                                                                     String status,
                                                                     String modelKey,
                                                                     LocalDateTime startTime,
                                                                     LocalDateTime endTime,
                                                                     Boolean overdue) {
        FlowMonitorProcessInstancePageVO result = new FlowMonitorProcessInstancePageVO();
        List<FlowMonitorProcessInstanceVO> list = new ArrayList<>();
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止查看流程实例");
        try {
            IPage<FlowBusiness> pageResult = flowBusinessMapper.selectMonitorBusinessPage(
                    new Page<>(pageNum, pageSize), tenantId,
                    processName, initiator, status, modelKey, startTime, endTime, overdue);
            Map<String, Map<String, Object>> taskSummaries = loadActiveTaskSummaries(pageResult.getRecords(), tenantId);
            for (FlowBusiness business : pageResult.getRecords()) {
                list.add(toAdminProcessInstance(business, taskSummaries.get(business.getProcessInstanceId())));
            }
            result.setList(list);
            result.setTotal(pageResult.getTotal());
            result.setPageNum(pageResult.getCurrent());
            result.setPageSize(pageResult.getSize());
            result.setDegraded(false);
        } catch (Exception e) {
            log.error("查询流程实例列表失败：tenantId={}", tenantId, e);
            result.setList(list);
            result.setTotal(0);
            result.setPageNum(Math.max(1, pageNum));
            result.setPageSize(Math.max(1, pageSize));
            result.setDegraded(true);
            result.setErrorCode("FLOW_MONITOR_INSTANCES_UNAVAILABLE");
        }
        return result;
    }

    @Override
    public FlowMonitorProcessInstanceDetailVO getAdminProcessInstanceDetail(String processInstanceId) {
        FlowBusiness business = requireCurrentTenantProcessInstance(processInstanceId);
        FlowMonitorProcessInstanceDetailVO result = new FlowMonitorProcessInstanceDetailVO();
        result.setId(business.getProcessInstanceId());
        result.setProcessName(business.getTitle());
        result.setProcessDefKey(business.getProcessDefKey());
        result.setProcessDefName(business.getTitle());
        result.setInitiatorName(business.getApplyUserName());
        result.setInitiatorId(business.getApplyUserId());
        result.setStatus(business.getStatus());
        result.setStartTime(business.getCreateTime());
        result.setBusinessKey(business.getBusinessKey());
        result.setEndTime(business.getEndTime());
        result.setDeptName(business.getApplyDeptName());
        return result;
    }

    @Override
    public FlowMonitorTaskPageVO getAdminProcessInstanceTasks(String processInstanceId, int pageNum, int pageSize) {
        FlowBusiness business = requireCurrentTenantProcessInstance(processInstanceId);
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止查看流程任务");
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.min(Math.max(1, pageSize), 100);
        IPage<com.mdframe.forge.starter.flow.entity.FlowTask> page = flowTaskMapper
                .selectAdminTasksByProcessInstance(new Page<>(safePage, safeSize), business.getProcessInstanceId(), tenantId);
        FlowMonitorTaskPageVO result = new FlowMonitorTaskPageVO();
        result.setList(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        List<com.mdframe.forge.starter.flow.entity.FlowTask> treeTasks = flowTaskMapper
                .selectAdminTaskTreeByProcessInstance(business.getProcessInstanceId(), tenantId, 500);
        result.setTaskTree(buildAdminTaskTree(treeTasks));
        result.setCurrentTaskIds(treeTasks.stream()
                .filter(task -> task != null && FlowTaskStatus.isActionable(task.getStatus()))
                .map(com.mdframe.forge.starter.flow.entity.FlowTask::getTaskId)
                .filter(Objects::nonNull)
                .toList());
        result.setTaskTreeTruncated(treeTasks.size() >= 500);
        result.setDegraded(false);
        return result;
    }

    private List<FlowMonitorTaskTreeNodeVO> buildAdminTaskTree(
            List<com.mdframe.forge.starter.flow.entity.FlowTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        Map<String, FlowMonitorTaskTreeNodeVO> groups = new LinkedHashMap<>();
        for (com.mdframe.forge.starter.flow.entity.FlowTask task : tasks) {
            if (task == null) {
                continue;
            }
            String nodeKey = isBlank(task.getTaskDefKey()) ? "unknown" : task.getTaskDefKey();
            FlowMonitorTaskTreeNodeVO group = groups.computeIfAbsent(nodeKey, key -> {
                FlowMonitorTaskTreeNodeVO item = new FlowMonitorTaskTreeNodeVO();
                item.setKey("node:" + key);
                item.setLabel(isBlank(task.getTaskName()) ? key : task.getTaskName());
                item.setNodeKey(key);
                item.setChildren(new ArrayList<>());
                return item;
            });
            FlowMonitorTaskTreeItemVO child = new FlowMonitorTaskTreeItemVO();
            child.setKey(task.getTaskId());
            child.setLabel(firstNonBlank(task.getAssigneeName(), task.getAssignee(), "待签收"));
            child.setTaskId(task.getTaskId());
            child.setStatus(task.getStatus());
            child.setActive(FlowTaskStatus.isActionable(task.getStatus()));
            child.setCreateTime(task.getCreateTime());
            child.setCompleteTime(task.getCompleteTime());
            group.getChildren().add(child);
        }
        return new ArrayList<>(groups.values());
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    @Override
    public FlowMonitorTaskTrendVO getTaskTrend() {
        FlowMonitorTaskTrendVO result = new FlowMonitorTaskTrendVO();
        boolean degraded = false;
        List<String> dates = new ArrayList<>();
        List<Long> created = new ArrayList<>();
        List<Long> completed = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusDays(6);
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止查看任务趋势");

        try {
            List<FlowMonitorDailyStatVO> rows = flowBusinessMapper.selectDailyTrend(
                    tenantId, firstDay.atStartOfDay(), today.plusDays(1).atStartOfDay());
            Map<LocalDate, FlowMonitorDailyStatVO> rowsByDate = new HashMap<>();
            for (FlowMonitorDailyStatVO row : rows) {
                rowsByDate.put(row.getStatDate(), row);
            }

            for (int offset = 0; offset < 7; offset++) {
                LocalDate date = firstDay.plusDays(offset);
                FlowMonitorDailyStatVO row = rowsByDate.get(date);
                dates.add(date.toString().substring(5));
                created.add(row == null || row.getCreatedCount() == null ? 0L : row.getCreatedCount());
                completed.add(row == null || row.getCompletedCount() == null ? 0L : row.getCompletedCount());
            }
        } catch (Exception e) {
            log.error("获取任务趋势数据失败：tenantId={}", tenantId, e);
            degraded = true;
            for (int offset = 0; offset < 7; offset++) {
                dates.add(firstDay.plusDays(offset).toString().substring(5));
                created.add(0L);
                completed.add(0L);
            }
        }

        result.setDates(dates);
        result.setCreated(created);
        result.setCompleted(completed);
        result.setDegraded(degraded);
        if (degraded) {
            result.setErrorCode("FLOW_MONITOR_TASK_TREND_UNAVAILABLE");
        }
        return result;
    }

    @Override
    public List<FlowProcessDistributionVO> getProcessDistribution() {
        List<FlowProcessDistributionVO> result = new ArrayList<>();
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止查看流程分布");
        try {
            for (Map<String, Object> row : flowBusinessMapper.selectProcessDistribution(tenantId)) {
                FlowProcessDistributionVO item = new FlowProcessDistributionVO();
                item.setName(String.valueOf(row.getOrDefault("name", "")));
                Object value = row.get("value");
                item.setValue(value instanceof Number ? ((Number) value).longValue() : 0L);
                result.add(item);
            }
            if (result.isEmpty()) {
                FlowProcessDistributionVO emptyItem = new FlowProcessDistributionVO();
                emptyItem.setName("暂无数据");
                emptyItem.setValue(0L);
                result.add(emptyItem);
            }
        } catch (Exception e) {
            log.error("获取流程分布数据失败：tenantId={}", tenantId, e);
        }
        return result;
    }

    @Override
    public void assertCurrentTenantProcessInstance(String processInstanceId) {
        requireCurrentTenantProcessInstance(processInstanceId);
    }

    @Override
    public void assertCurrentTenantTask(String taskId) {
        if (isBlank(taskId)) {
            throw new BusinessException(400, "任务ID不能为空");
        }
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止管理流程任务");
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        if (task == null || isBlank(task.getProcessInstanceId())) {
            throw new BusinessException(404, "任务不存在或不属于当前租户");
        }
        if (flowBusinessMapper.selectByProcessInstanceIdAndTenantId(
                task.getProcessInstanceId(), tenantId) == null) {
            throw new BusinessException(404, "任务不存在或不属于当前租户");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendProcessInstance(String processInstanceId) {
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止管理流程实例");
        lockCurrentTenantProcessInstance(processInstanceId, tenantId);
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance == null) {
            throw new BusinessException(400, "流程实例不存在或已完成，无法挂起");
        }
        if (processInstance.isSuspended()) {
            throw new BusinessException(400, "流程实例已经是挂起状态");
        }

        runtimeService.suspendProcessInstanceById(processInstanceId);
        flowBusinessMapper.updateStatusByProcessInstanceId(
                processInstanceId, FlowBusinessStatus.SUSPENDED.getCode(), tenantId);
        log.info("流程挂起成功：processInstanceId={}", processInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateProcessInstance(String processInstanceId) {
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止管理流程实例");
        lockCurrentTenantProcessInstance(processInstanceId, tenantId);
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance == null) {
            throw new BusinessException(400, "流程实例不存在或已完成，无法激活");
        }
        if (!processInstance.isSuspended()) {
            throw new BusinessException(400, "流程实例已经是激活状态");
        }

        runtimeService.activateProcessInstanceById(processInstanceId);
        flowBusinessMapper.updateStatusByProcessInstanceId(
                processInstanceId, FlowBusinessStatus.RUNNING.getCode(), tenantId);
        log.info("流程激活成功：processInstanceId={}", processInstanceId);
    }

    @Override
    public Map<String, Object> deleteProcessInstanceData(String processInstanceId, String reason) {
        if (isBlank(processInstanceId)) {
            throw new BusinessException(400, "流程实例ID不能为空");
        }
        Long tenantId = resolveCleanupTenantId();
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantId(processInstanceId, tenantId);
        if (business == null) {
            throw new BusinessException(404, "流程实例不存在或不属于当前租户");
        }
        return cleanupProcessInstanceIds(List.of(processInstanceId), tenantId, reason);
    }

    @Override
    public Map<String, Object> cleanupProcessInstances(String processName,
                                                       String initiator,
                                                       String status,
                                                       String modelKey,
                                                       LocalDateTime startTime,
                                                       LocalDateTime endTime,
                                                       String reason) {
        Long tenantId = resolveCleanupTenantId();
        List<FlowBusiness> businesses;
        Set<String> processInstanceIds = new LinkedHashSet<>();
        List<String> businessOnlyIds = new ArrayList<>();
        try {
            businesses = flowBusinessMapper.selectBusinessesForCleanup(tenantId,
                    processName, initiator, status, modelKey, startTime, endTime);
            for (FlowBusiness business : businesses) {
                if (isBlank(business.getProcessInstanceId())) {
                    businessOnlyIds.add(business.getId());
                } else {
                    processInstanceIds.add(business.getProcessInstanceId());
                }
            }
        } catch (Exception e) {
            log.error("查询批量流程清理候选数据失败：modelKey={}, status={}", modelKey, status, e);
            throw new BusinessException(500, PROCESS_CLEANUP_FAILURE_MESSAGE, e);
        }

        try {
            Map<String, Object> result = cleanupProcessInstanceIds(processInstanceIds, tenantId, reason);
            int businessOnlyDeletedCount = businessOnlyIds.isEmpty()
                    ? 0 : flowBusinessMapper.deleteBusinessRecordsWithoutProcessInstance(businessOnlyIds, tenantId);
            int deletedCount = ((Number) result.getOrDefault("deletedCount", 0)).intValue();
            result.put("deletedCount", deletedCount + businessOnlyDeletedCount);
            result.put("businessOnlyDeletedCount", businessOnlyDeletedCount);
            result.put("matchedBusinessCount", businesses.size());

            log.info("批量删除流程数据完成：modelKey={}, status={}, matchedBusinessCount={}, result={}",
                    modelKey, status, businesses.size(), result);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量删除流程数据失败：modelKey={}, status={}", modelKey, status, e);
            throw new BusinessException(500, PROCESS_CLEANUP_FAILURE_MESSAGE, e);
        }
    }

    private Map<String, Map<String, Object>> loadActiveTaskSummaries(List<FlowBusiness> businesses, Long tenantId) {
        List<String> processInstanceIds = businesses.stream()
                .map(FlowBusiness::getProcessInstanceId)
                .filter(id -> !isBlank(id))
                .distinct()
                .toList();
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> rows = flowTaskMapper.selectActiveTaskSummaries(processInstanceIds, tenantId);
        Map<String, Map<String, Object>> summaries = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String processInstanceId = Objects.toString(row.get("processInstanceId"), null);
            if (processInstanceId != null) {
                summaries.putIfAbsent(processInstanceId, row);
            }
        }
        return summaries;
    }

    private FlowMonitorProcessInstanceVO toAdminProcessInstance(FlowBusiness business,
                                                                Map<String, Object> taskSummary) {
        FlowMonitorProcessInstanceVO item = new FlowMonitorProcessInstanceVO();
        item.setId(business.getProcessInstanceId());
        item.setProcessName(business.getTitle());
        item.setProcessDefKey(business.getProcessDefKey());
        item.setProcessDefName(business.getTitle());
        item.setInitiatorName(business.getApplyUserName());
        item.setInitiatorId(business.getApplyUserId());
        item.setStatus(business.getStatus());
        item.setStartTime(business.getCreateTime());
        item.setBusinessKey(business.getBusinessKey());
        item.setDuration(formatDuration(business.getCreateTime()));

        item.setCurrentNode("-");
        item.setCurrentAssignee("-");
        if (FlowBusinessStatus.isPending(business.getStatus()) && taskSummary != null) {
            try {
                item.setCurrentNode(Objects.toString(taskSummary.get("taskName"), "-"));
                String assignee = Objects.toString(taskSummary.get("assignee"), null);
                if (assignee == null) {
                        item.setCurrentAssignee("待认领");
                } else {
                        FlowMonitorUserLookup userLookup = userLookupProvider.getIfAvailable();
                        String displayName = userLookup == null ? null : userLookup.findDisplayName(assignee);
                        item.setCurrentAssignee(isBlank(displayName) ? assignee : displayName);
                }
            } catch (Exception e) {
                item.setCurrentNode("-");
                item.setCurrentAssignee("-");
                log.warn("查询流程监控当前任务失败：processInstanceId={}", business.getProcessInstanceId(), e);
            }
        }
        return item;
    }

    private String formatDuration(LocalDateTime createTime) {
        if (createTime == null) {
            return "-";
        }
        long startMillis = createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long durationMinutes = (System.currentTimeMillis() - startMillis) / (1000 * 60);
        if (durationMinutes < 60) {
            return durationMinutes + "分钟";
        }
        if (durationMinutes < 24 * 60) {
            return durationMinutes / 60 + "小时";
        }
        return durationMinutes / (24 * 60) + "天";
    }

    private Map<String, Object> cleanupProcessInstanceIds(Collection<String> processInstanceIds,
                                                          Long tenantId,
                                                          String reason) {
        long cleanupStartTime = System.currentTimeMillis();
        int deletedCount = 0;
        int runtimeDeletedCount = 0;
        int historyDeletedCount = 0;
        int forgeRecordDeletedCount = 0;
        int failedCount = 0;
        List<Map<String, String>> failures = new ArrayList<>();

        for (String processInstanceId : processInstanceIds) {
            if (isBlank(processInstanceId)) {
                continue;
            }
            try {
                long instanceStartTime = System.currentTimeMillis();
                ProcessCleanupResult cleanupResult = cleanupTransactionExecutor.execute(() ->
                        cleanupSingleProcessInstance(processInstanceId, tenantId, reason));
                if (cleanupResult == null) {
                    throw new IllegalStateException("流程实例清理事务未返回结果");
                }
                runtimeDeletedCount += cleanupResult.runtimeDeleted() ? 1 : 0;
                historyDeletedCount += cleanupResult.historyDeleted() ? 1 : 0;
                forgeRecordDeletedCount += cleanupResult.forgeRecordDeletedCount();
                deletedCount++;
                log.info("删除流程数据完成：processInstanceId={}, runtimeDeleted={}, historyDeleted={}, forgeRecordDeleted={}, cost={}ms",
                        processInstanceId, cleanupResult.runtimeDeleted(), cleanupResult.historyDeleted(),
                        cleanupResult.forgeRecordDeletedCount(), elapsedMillis(instanceStartTime));
            } catch (Exception e) {
                failedCount++;
                Map<String, String> failure = new HashMap<>();
                failure.put("processInstanceId", processInstanceId);
                failure.put("message", PROCESS_CLEANUP_FAILURE_MESSAGE);
                failures.add(failure);
                log.warn("删除流程数据失败：processInstanceId={}", processInstanceId, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("runtimeDeletedCount", runtimeDeletedCount);
        result.put("historyDeletedCount", historyDeletedCount);
        result.put("forgeRecordDeletedCount", forgeRecordDeletedCount);
        result.put("failedCount", failedCount);
        result.put("failures", failures);
        log.info("流程数据清理汇总：inputCount={}, deletedCount={}, runtimeDeletedCount={}, historyDeletedCount={}, forgeRecordDeletedCount={}, failedCount={}, cost={}ms",
                processInstanceIds.size(), deletedCount, runtimeDeletedCount, historyDeletedCount,
                forgeRecordDeletedCount, failedCount, elapsedMillis(cleanupStartTime));
        if (failedCount > 0) {
            throw new BusinessException(500, "部分流程数据删除失败", result);
        }
        return result;
    }

    private ProcessCleanupResult cleanupSingleProcessInstance(String processInstanceId,
                                                               Long tenantId,
                                                               String reason) {
        lockCurrentTenantProcessInstance(processInstanceId, tenantId);
        boolean runtimeDeleted = false;
        boolean historyDeleted = false;

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (processInstance != null) {
            long runtimeStartTime = System.currentTimeMillis();
            runtimeService.deleteProcessInstance(processInstanceId, reason);
            runtimeDeleted = true;
            log.info("删除流程运行时数据完成：processInstanceId={}, cost={}ms",
                    processInstanceId, elapsedMillis(runtimeStartTime));
        }

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historicProcessInstance != null) {
            long historyStartTime = System.currentTimeMillis();
            historyService.deleteHistoricProcessInstance(processInstanceId);
            historyDeleted = true;
            log.info("删除流程历史数据完成：processInstanceId={}, cost={}ms",
                    processInstanceId, elapsedMillis(historyStartTime));
        }

        long forgeRecordStartTime = System.currentTimeMillis();
        int forgeRecordDeletedCount = deleteForgeFlowRecords(processInstanceId, tenantId);
        log.info("删除Forge流程关联记录完成：processInstanceId={}, deletedCount={}, cost={}ms",
                processInstanceId, forgeRecordDeletedCount, elapsedMillis(forgeRecordStartTime));
        return new ProcessCleanupResult(runtimeDeleted, historyDeleted, forgeRecordDeletedCount);
    }

    private int deleteForgeFlowRecords(String processInstanceId, Long tenantId) {
        int deleted = 0;
        deleted += flowTaskMapper.deleteByProcessInstanceIdPhysically(processInstanceId, tenantId);
        deleted += flowCommentMapper.deleteByProcessInstanceIdPhysically(processInstanceId, tenantId);
        deleted += flowCcMapper.deleteByProcessInstanceIdPhysically(processInstanceId, tenantId);
        deleted += flowErrorLogMapper.deleteByProcessInstanceIdPhysically(processInstanceId, tenantId);
        deleted += flowFormInstanceMapper.deleteByProcessInstanceIdLogically(processInstanceId, tenantId);
        deleted += flowFillBatchItemMapper.deleteByProcessInstanceIdLogically(processInstanceId, tenantId);
        deleted += flowBusinessMapper.deleteByProcessInstanceIdPhysically(processInstanceId, tenantId);
        return deleted;
    }

    private Long resolveCleanupTenantId() {
        return resolveCurrentTenantId("无法确定当前租户，禁止删除流程数据");
    }

    private FlowBusiness requireCurrentTenantProcessInstance(String processInstanceId) {
        Long tenantId = resolveCurrentTenantId("无法确定当前租户，禁止访问流程实例");
        return requireCurrentTenantProcessInstance(processInstanceId, tenantId);
    }

    private FlowBusiness requireCurrentTenantProcessInstance(String processInstanceId, Long tenantId) {
        if (isBlank(processInstanceId)) {
            throw new BusinessException(400, "流程实例ID不能为空");
        }
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantId(processInstanceId, tenantId);
        if (business == null) {
            throw new BusinessException(404, "流程实例不存在或不属于当前租户");
        }
        return business;
    }

    private FlowBusiness lockCurrentTenantProcessInstance(String processInstanceId, Long tenantId) {
        if (isBlank(processInstanceId)) {
            throw new BusinessException(400, "流程实例ID不能为空");
        }
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantIdForUpdate(
                processInstanceId, tenantId);
        if (business == null) {
            throw new BusinessException(404, "流程实例不存在或不属于当前租户");
        }
        return business;
    }

    private FlowMonitorStatisticsVO degradedAdminStatistics(String errorCode) {
        FlowMonitorStatisticsVO statistics = new FlowMonitorStatisticsVO();
        statistics.setDegraded(true);
        statistics.setErrorCode(errorCode);
        return statistics;
    }

    private FlowMonitorStatisticsVO toMonitorStatistics(Map<String, Object> values) {
        FlowMonitorStatisticsVO statistics = new FlowMonitorStatisticsVO();
        statistics.setRunningInstances(toLong(values.get("runningInstances")));
        statistics.setPendingTasks(toLong(values.get("pendingTasks")));
        statistics.setTodayCompleted(toLong(values.get("todayCompleted")));
        statistics.setTimeoutTasks(toLong(values.get("timeoutTasks")));
        statistics.setDegraded(Boolean.TRUE.equals(values.get("degraded")));
        Object errorCode = values.get("errorCode");
        statistics.setErrorCode(errorCode == null ? null : String.valueOf(errorCode));
        return statistics;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long resolveCurrentTenantId(String failureMessage) {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException(403, failureMessage);
        }
        return tenantId;
    }

    private long elapsedMillis(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ProcessCleanupResult(boolean runtimeDeleted,
                                        boolean historyDeleted,
                                        int forgeRecordDeletedCount) {
    }

    /**
     * 获取今日开始时间
     */
    private Date getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
