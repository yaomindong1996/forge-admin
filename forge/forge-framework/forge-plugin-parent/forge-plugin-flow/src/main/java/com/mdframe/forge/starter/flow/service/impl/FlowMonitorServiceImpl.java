package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.service.FlowMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;

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

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final ManagementService managementService;

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
        
        // 超时任务数量
        long timeoutCount = taskService.createTaskQuery()
                .active()
                .count(); // TODO: 根据实际超时配置计算
        result.put("timeoutCount", timeoutCount);
        
        return result;
    }

    @Override
    public Map<String, Object> getProcessInstanceStats(String processDefinitionKey) {
        Map<String, Object> result = new HashMap<>();
        
        // 运行中
        long runningCount = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .count();
        result.put("runningCount", runningCount);
        
        // 已完成
        long completedCount = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .finished()
                .count();
        result.put("completedCount", completedCount);
        
        // 平均完成时间
        List<HistoricProcessInstance> completedInstances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .finished()
                .list();
        
        if (!completedInstances.isEmpty()) {
            long totalDuration = 0;
            for (HistoricProcessInstance instance : completedInstances) {
                if (instance.getDurationInMillis() != null) {
                    totalDuration += instance.getDurationInMillis();
                }
            }
            result.put("avgDuration", totalDuration / completedInstances.size());
        } else {
            result.put("avgDuration", 0);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getProcessInstanceList(String processDefinitionKey, String status, int pageNum, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> instances = new ArrayList<>();
        
        int firstResult = (pageNum - 1) * pageSize;
        
        List<ProcessInstance> processInstances;
        long total;
        
        if ("running".equals(status)) {
            processInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .active()
                    .listPage(firstResult, pageSize);
            total = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .active()
                    .count();
        } else if ("suspended".equals(status)) {
            processInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .suspended()
                    .listPage(firstResult, pageSize);
            total = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .suspended()
                    .count();
        } else if ("completed".equals(status)) {
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
                instance.put("status", "completed");
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
            instance.put("status", pi.isSuspended() ? "suspended" : "running");
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
            result.put("status", processInstance.isSuspended() ? "suspended" : "running");
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
                result.put("status", "completed");
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
        Map<String, Object> result = new HashMap<>();
        
        // TODO: 实现任务执行统计
        result.put("totalTasks", 0);
        result.put("avgDuration", 0);
        result.put("maxDuration", 0);
        result.put("minDuration", 0);
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getTimeoutTasks(String processDefinitionKey) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // TODO: 根据超时配置查询超时任务
        List<Task> tasks = taskService.createTaskQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .list();
        
        for (Task task : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("name", task.getName());
            item.put("assignee", task.getAssignee());
            item.put("createTime", task.getCreateTime());
            item.put("processInstanceId", task.getProcessInstanceId());
            result.add(item);
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getUpcomingTimeoutTasks(String processDefinitionKey, int advanceMinutes) {
        // TODO: 实现即将超时任务查询
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getProcessEfficiencyAnalysis(String processDefinitionKey, String startTime, String endTime) {
        Map<String, Object> result = new HashMap<>();
        
        // TODO: 实现流程效率分析
        result.put("avgCompletionTime", 0);
        result.put("avgTaskTime", 0);
        result.put("completionRate", 0);
        result.put("timeoutRate", 0);
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getNodeDurationStats(String processDefinitionKey, String startTime, String endTime) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // TODO: 实现节点耗时统计
        return result;
    }

    @Override
    public List<Map<String, Object>> getApproverEfficiencyStats(String userId, String startTime, String endTime) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // TODO: 实现审批人效率统计
        return result;
    }

    @Override
    public Map<String, Object> getProcessBottleneckAnalysis(String processDefinitionKey) {
        Map<String, Object> result = new HashMap<>();
        
        // TODO: 实现流程瓶颈分析
        result.put("bottleneckNodes", new ArrayList<>());
        result.put("suggestions", new ArrayList<>());
        
        return result;
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
        return runtimeService.getVariables(processInstanceId);
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