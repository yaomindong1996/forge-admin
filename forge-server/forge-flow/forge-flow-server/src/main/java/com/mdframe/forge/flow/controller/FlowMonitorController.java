package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.util.PageParamResolver;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 流程监控接口
 */
@Slf4j
@RestController
@RequestMapping("/api/flow/monitor")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class FlowMonitorController {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final FlowInstanceService flowInstanceService;
    private final FlowMonitorService flowMonitorService;

    /**
     * 获取流程监控统计数据
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/statistics")
    public RespInfo<Map<String, Object>> getStatistics() {
        return RespInfo.success(flowMonitorService.getAdminStatistics());
    }

    /**
     * 分页查询流程实例列表（监控用）
     * @param page 旧版页码别名
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param processName 流程名称（模糊查询，可选）
     * @param initiator 发起人（模糊查询，可选）
     * @param status 状态（可选）
     * @param modelKey 流程模型Key（可选，用于从模型页面跳转）
    */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/instances")
    public RespInfo<Map<String, Object>> getInstances(
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String initiator,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String modelKey,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        int currentPage = PageParamResolver.resolve(page, pageNum);
        return RespInfo.success(flowMonitorService.getAdminProcessInstances(
                currentPage, pageSize, processName, initiator, status, modelKey,
                toLocalDateTime(startTime), toLocalDateTime(endTime)));
    }

    /**
     * 获取流程实例详情
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/instance/{processInstanceId}")
    public RespInfo<Map<String, Object>> getInstanceDetail(@PathVariable String processInstanceId) {
        return RespInfo.success(flowMonitorService.getAdminProcessInstanceDetail(processInstanceId));
    }

    /**
     * 获取任务处理趋势（最近7天）
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/taskTrend")
    public RespInfo<Map<String, Object>> getTaskTrend() {
        return RespInfo.success(flowMonitorService.getTaskTrend());
    }

    /**
     * 获取流程分布统计（按流程模型分组）
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/processDistribution")
    public RespInfo<List<Map<String, Object>>> getProcessDistribution() {
        return RespInfo.success(flowMonitorService.getProcessDistribution());
    }

    /**
     * 管理员终止流程
     */
    @SaCheckPermission("flow:monitor:manage")
    @PostMapping("/terminate/{processInstanceId}")
    public RespInfo<Void> terminateProcess(
            @PathVariable String processInstanceId,
            @RequestBody Map<String, Object> params) {

        LoginUser loginUser = SessionHelper.getLoginUser();
        String userId = loginUser != null ? String.valueOf(loginUser.getUserId()) : null;
        String reason = (String) params.get("reason");

        if (reason == null || reason.isEmpty()) {
            reason = "管理员终止流程";
        }

        flowInstanceService.terminateProcessByInstanceId(processInstanceId, userId, reason);

        return RespInfo.success("流程已终止", null);
    }

    /**
     * 管理员回退流程节点
     */
    @SaCheckPermission("flow:monitor:manage")
    @PostMapping("/rollback/{processInstanceId}")
    public RespInfo<Void> rollbackProcess(
            @PathVariable String processInstanceId,
            @RequestBody Map<String, Object> params) {

        LoginUser loginUser = SessionHelper.getLoginUser();
        String userId = loginUser != null ? String.valueOf(loginUser.getUserId()) : null;
        String targetActivityId = (String) params.get("targetActivityId");
        String reason = (String) params.get("reason");

        if (targetActivityId == null || targetActivityId.isEmpty()) {
            return RespInfo.error("目标节点ID不能为空");
        }

        if (reason == null || reason.isEmpty()) {
            reason = "管理员回退流程";
        }

        flowInstanceService.rollbackToActivity(processInstanceId, targetActivityId, userId, reason);

        return RespInfo.success("流程已回退", null);
    }

    /**
     * 管理员转派任务
     */
    @SaCheckPermission("flow:monitor:manage")
    @PostMapping("/reassign/{taskId}")
    public RespInfo<Void> reassignTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> params) {

        LoginUser loginUser = SessionHelper.getLoginUser();
        String userId = loginUser != null ? String.valueOf(loginUser.getUserId()) : null;
        String newAssignee = (String) params.get("newAssignee");
        String reason = (String) params.get("reason");

        if (newAssignee == null || newAssignee.isEmpty()) {
            return RespInfo.error("新处理人ID不能为空");
        }

        if (reason == null || reason.isEmpty()) {
            reason = "管理员转派任务";
        }

        flowInstanceService.reassignTask(taskId, newAssignee, userId, reason);

        return RespInfo.success("任务已转派", null);
    }

    /**
     * 管理员删除单个流程实例数据。
     */
    @SaCheckPermission("flow:monitor:cleanup")
    @PostMapping("/instance/{processInstanceId}/delete")
    public RespInfo<Map<String, Object>> deleteProcessInstance(
            @PathVariable String processInstanceId,
            @RequestBody(required = false) Map<String, Object> params) {
        if (isBlank(processInstanceId)) {
            throw new BusinessException(400, "流程实例ID不能为空");
        }

        String reason = stringParam(params, "reason");
        if (isBlank(reason)) {
            reason = "管理员删除流程数据";
        }

        Map<String, Object> result = flowMonitorService.deleteProcessInstanceData(processInstanceId, reason);
        return RespInfo.success("流程数据已删除", result);
    }

    /**
     * 管理员删除当前筛选条件下的流程实例数据。
     */
    @SaCheckPermission("flow:monitor:cleanup")
    @PostMapping("/instances/cleanup")
    public RespInfo<Map<String, Object>> cleanupProcessInstances(@RequestBody Map<String, Object> params) {
        String confirmText = stringParam(params, "confirmText");
        if (!"确认删除流程数据".equals(confirmText)) {
            throw new BusinessException(400, "确认文本不正确，无法删除流程数据");
        }

        String processName = stringParam(params, "processName");
        String initiator = stringParam(params, "initiator");
        String status = stringParam(params, "status");
        String modelKey = stringParam(params, "modelKey");
        Long startTime = longParam(params, "startTime");
        Long endTime = longParam(params, "endTime");

        String reason = stringParam(params, "reason");
        if (isBlank(reason)) {
            reason = "管理员批量删除流程数据";
        }

        Map<String, Object> result = flowMonitorService.cleanupProcessInstances(
                processName, initiator, status, modelKey,
                toLocalDateTime(startTime), toLocalDateTime(endTime), reason);
        return RespInfo.success("流程数据已删除", result);
    }

    /**
     * 获取流程实例的所有变量
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/variables/{processInstanceId}")
    public RespInfo<Map<String, Object>> getProcessVariables(@PathVariable String processInstanceId) {
        flowMonitorService.assertCurrentTenantProcessInstance(processInstanceId);
        try {
            Map<String, Object> variables = new HashMap<>();

            // 先尝试从运行时获取（运行中或挂起的流程）
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance != null) {
                // 流程还在运行中，从运行时服务获取变量
                variables = runtimeService.getVariables(processInstanceId);
                log.info("从运行时获取流程变量：processInstanceId={}, 变量数量={}", processInstanceId, variables.size());
            } else {
                // 流程已完成，从历史服务获取变量
                List<org.flowable.variable.api.history.HistoricVariableInstance> historicVariables =
                        historyService.createHistoricVariableInstanceQuery()
                                .processInstanceId(processInstanceId)
                                .list();

                for (org.flowable.variable.api.history.HistoricVariableInstance variable : historicVariables) {
                    variables.put(variable.getVariableName(), variable.getValue());
                }
                log.info("从历史获取流程变量：processInstanceId={}, 变量数量={}", processInstanceId, variables.size());
            }

            return RespInfo.success(variables);
        } catch (Exception e) {
            log.error("获取流程变量失败：processInstanceId={}", processInstanceId, e);
            return RespInfo.error("获取流程变量失败，请稍后重试");
        }
    }

    /**
     * 获取流程实例的历史活动节点列表（用于回退）
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/activities/{processInstanceId}")
    public RespInfo<List<Map<String, Object>>> getProcessActivities(@PathVariable String processInstanceId) {
        flowMonitorService.assertCurrentTenantProcessInstance(processInstanceId);
        try {
            List<Map<String, Object>> activities = new ArrayList<>();

            // 查询历史活动实例
            List<org.flowable.engine.history.HistoricActivityInstance> historicActivities =
                    historyService.createHistoricActivityInstanceQuery()
                            .processInstanceId(processInstanceId)
                            .orderByHistoricActivityInstanceStartTime()
                            .asc()
                            .list();

            for (org.flowable.engine.history.HistoricActivityInstance activity : historicActivities) {
                // 只返回用户任务节点
                if ("userTask".equals(activity.getActivityType())) {
                    Map<String, Object> activityMap = new HashMap<>();
                    activityMap.put("activityId", activity.getActivityId());
                    activityMap.put("activityName", activity.getActivityName());
                    activityMap.put("activityType", activity.getActivityType());
                    activityMap.put("assignee", activity.getAssignee());
                    activityMap.put("startTime", activity.getStartTime());
                    activityMap.put("endTime", activity.getEndTime());
                    activities.add(activityMap);
                }
            }

            return RespInfo.success(activities);
        } catch (Exception e) {
            log.error("获取流程活动节点失败", e);
            return RespInfo.error("获取流程活动节点失败，请稍后重试");
        }
    }

    /**
     * 获取流程实例的当前任务列表
     */
    @SaCheckPermission("flow:monitor:view")
    @GetMapping("/current-tasks/{processInstanceId}")
    public RespInfo<List<Map<String, Object>>> getCurrentTasks(@PathVariable String processInstanceId) {
        flowMonitorService.assertCurrentTenantProcessInstance(processInstanceId);
        try {
            List<Map<String, Object>> tasks = new ArrayList<>();

            // 查询当前活动任务
            List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .list();

            for (Task task : activeTasks) {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getId());
                taskMap.put("name", task.getName());
                taskMap.put("assignee", task.getAssignee());
                taskMap.put("createTime", task.getCreateTime());
                tasks.add(taskMap);
            }

            return RespInfo.success(tasks);
        } catch (Exception e) {
            log.error("获取当前任务失败", e);
            return RespInfo.error("获取当前任务失败，请稍后重试");
        }
    }

    /**
     * 挂起流程实例
     */
    @SaCheckPermission("flow:monitor:manage")
    @PostMapping("/suspend/{processInstanceId}")
    public RespInfo<Void> suspendProcessInstance(@PathVariable String processInstanceId) {
        try {
            flowMonitorService.suspendProcessInstance(processInstanceId);
            return RespInfo.success("流程已挂起", null);
        } catch (BusinessException e) {
            log.warn("挂起流程被拒绝：processInstanceId={}, reason={}", processInstanceId, e.getMessage());
            return RespInfo.error(e.getMessage());
        } catch (Exception e) {
            log.error("挂起流程失败：processInstanceId={}", processInstanceId, e);
            return RespInfo.error("挂起流程失败，请稍后重试");
        }
    }

    /**
     * 激活流程实例
     */
    @SaCheckPermission("flow:monitor:manage")
    @PostMapping("/activate/{processInstanceId}")
    public RespInfo<Void> activateProcessInstance(@PathVariable String processInstanceId) {
        try {
            flowMonitorService.activateProcessInstance(processInstanceId);
            return RespInfo.success("流程已激活", null);
        } catch (BusinessException e) {
            log.warn("激活流程被拒绝：processInstanceId={}, reason={}", processInstanceId, e.getMessage());
            return RespInfo.error(e.getMessage());
        } catch (Exception e) {
            log.error("激活流程失败：processInstanceId={}", processInstanceId, e);
            return RespInfo.error("激活流程失败，请稍后重试");
        }
    }

    private String stringParam(Map<String, Object> params, String key) {
        if (params == null || params.get(key) == null) {
            return null;
        }
        String value = String.valueOf(params.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    private Long longParam(Map<String, Object> params, String key) {
        String value = stringParam(params, key);
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, key + " 参数格式错误");
        }
    }

    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
