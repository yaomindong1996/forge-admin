package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowCc;
import com.mdframe.forge.starter.flow.entity.FlowComment;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowFillBatchItem;
import com.mdframe.forge.starter.flow.entity.FlowFormInstance;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowCcMapper;
import com.mdframe.forge.starter.flow.mapper.FlowCommentMapper;
import com.mdframe.forge.starter.flow.mapper.FlowErrorLogMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFillBatchItemMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormInstanceMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
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
@IgnoreTenant
public class FlowMonitorController {

    private final FlowBusinessMapper flowBusinessMapper;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowCommentMapper flowCommentMapper;
    private final FlowCcMapper flowCcMapper;
    private final FlowErrorLogMapper flowErrorLogMapper;
    private final FlowFormInstanceMapper flowFormInstanceMapper;
    private final FlowFillBatchItemMapper flowFillBatchItemMapper;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ISysUserService sysUserService;
    private final FlowInstanceService flowInstanceService;

    /**
     * 获取流程监控统计数据
     */
    @GetMapping("/statistics")
    public RespInfo<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            // 运行中的流程实例数量
            long runningInstances = runtimeService.createProcessInstanceQuery()
                    .active()
                    .count();
            statistics.put("runningInstances", runningInstances);
            
            // 待办任务数量
            long pendingTasks = taskService.createTaskQuery()
                    .active()
                    .count();
            statistics.put("pendingTasks", pendingTasks);
            
            // 今日完成的流程数量
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            Date startDate = Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
            
            long todayCompleted = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .finishedAfter(startDate)
                    .count();
            statistics.put("todayCompleted", todayCompleted);
            
            // 超时任务数量（这里简化处理，实际需要根据业务规则判断）
            // 暂时返回0，后续可以根据任务的创建时间和预期处理时间计算
            statistics.put("timeoutTasks", 0);
            
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            // 返回默认值
            statistics.put("runningInstances", 0);
            statistics.put("pendingTasks", 0);
            statistics.put("todayCompleted", 0);
            statistics.put("timeoutTasks", 0);
        }
        
        return RespInfo.success(statistics);
    }

    /**
     * 分页查询流程实例列表（监控用）
     * @param page 页码
     * @param pageSize 每页大小
     * @param processName 流程名称（模糊查询，可选）
     * @param initiator 发起人（模糊查询，可选）
     * @param status 状态（可选）
     * @param modelKey 流程模型Key（可选，用于从模型页面跳转）
     */
    @GetMapping("/instances")
    public RespInfo<Map<String, Object>> getInstances(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String processName,
            @RequestParam(required = false) String initiator,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String modelKey,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        
        try {
            // 构建查询条件
            LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
            
            // 流程名称模糊查询
            wrapper.like(processName != null && !processName.isEmpty(),
                    FlowBusiness::getTitle, processName);
            
            // 发起人模糊查询
            wrapper.like(initiator != null && !initiator.isEmpty(),
                    FlowBusiness::getApplyUserName, initiator);
            
            // 状态查询
            wrapper.eq(status != null && !status.isEmpty(),
                    FlowBusiness::getStatus, status);
            
            // 模型Key查询（对应 processDefKey）
            wrapper.eq(modelKey != null && !modelKey.isEmpty(),
                    FlowBusiness::getProcessDefKey, modelKey);

            LocalDateTime startDateTime = toLocalDateTime(startTime);
            LocalDateTime endDateTime = toLocalDateTime(endTime);
            wrapper.ge(startDateTime != null, FlowBusiness::getCreateTime, startDateTime);
            wrapper.le(endDateTime != null, FlowBusiness::getCreateTime, endDateTime);
            
            // 按创建时间倒序
            wrapper.orderByDesc(FlowBusiness::getCreateTime);
            
            // 分页查询
            Page<FlowBusiness> pageParam = new Page<>(page, pageSize);
            IPage<FlowBusiness> pageResult = flowBusinessMapper.selectPage(pageParam, wrapper);
            
            // 转换结果
            for (FlowBusiness business : pageResult.getRecords()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", business.getProcessInstanceId());
                item.put("processName", business.getTitle());
                item.put("processDefKey", business.getProcessDefKey());
                item.put("processDefName", business.getTitle());
                item.put("initiatorName", business.getApplyUserName());
                item.put("initiatorId", business.getApplyUserId());
                item.put("status", business.getStatus());
                item.put("startTime", business.getCreateTime());
                item.put("businessKey", business.getBusinessKey());
                
                // 计算运行时长
                if (business.getCreateTime() != null) {
                    ZonedDateTime zonedDateTime = business.getCreateTime().atZone(ZoneId.systemDefault());
                    long timestampMillis = zonedDateTime.toInstant().toEpochMilli();
                    long durationMs = System.currentTimeMillis() - timestampMillis;
                    long durationMinutes = durationMs / (1000 * 60);
                    if (durationMinutes < 60) {
                        item.put("duration", durationMinutes + "分钟");
                    } else if (durationMinutes < 24 * 60) {
                        item.put("duration", (durationMinutes / 60) + "小时");
                    } else {
                        item.put("duration", (durationMinutes / (24 * 60)) + "天");
                    }
                } else {
                    item.put("duration", "-");
                }
                
                // 获取当前节点和处理人
                if ("running".equals(business.getStatus()) || "active".equals(business.getStatus())) {
                    try {
                        // 查询当前活动任务
                        List<Task> tasks = taskService.createTaskQuery()
                                .processInstanceId(business.getProcessInstanceId())
                                .active()
                                .list();
                        
                        if (!tasks.isEmpty()) {
                            Task currentTask = tasks.get(0);
                            item.put("currentNode", currentTask.getName());
                            
                            // 获取处理人名称
                            if (currentTask.getAssignee() != null) {
                                SysUser sysUser = sysUserService.selectUserById(
                                        Long.parseLong(currentTask.getAssignee()));
                                item.put("currentAssignee", sysUser != null ? sysUser.getRealName() : currentTask.getAssignee());
                            } else {
                                item.put("currentAssignee", "待认领");
                            }
                        } else {
                            item.put("currentNode", "-");
                            item.put("currentAssignee", "-");
                        }
                    } catch (Exception e) {
                        item.put("currentNode", "-");
                        item.put("currentAssignee", "-");
                    }
                } else {
                    item.put("currentNode", "-");
                    item.put("currentAssignee", "-");
                }
                
                list.add(item);
            }
            
            result.put("list", list);
            result.put("total", pageResult.getTotal());
            
        } catch (Exception e) {
            log.error("查询流程实例列表失败", e);
            result.put("list", list);
            result.put("total", 0);
        }
        
        return RespInfo.success(result);
    }

    /**
     * 获取流程实例详情
     */
    @GetMapping("/instance/{processInstanceId}")
    public RespInfo<Map<String, Object>> getInstanceDetail(@PathVariable String processInstanceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询业务信息
            LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FlowBusiness::getProcessInstanceId, processInstanceId);
            FlowBusiness business = flowBusinessMapper.selectOne(wrapper);
            
            if (business != null) {
                result.put("id", business.getProcessInstanceId());
                result.put("processName", business.getTitle());
                result.put("processDefKey", business.getProcessDefKey());
                result.put("processDefName", business.getTitle());
                result.put("initiatorName", business.getApplyUserName());
                result.put("initiatorId", business.getApplyUserId());
                result.put("status", business.getStatus());
                result.put("startTime", business.getCreateTime());
                result.put("businessKey", business.getBusinessKey());
                result.put("endTime", business.getEndTime());
                result.put("deptName", business.getApplyDeptName());
            }
            
        } catch (Exception e) {
            log.error("获取流程实例详情失败", e);
        }
        
        return RespInfo.success(result);
    }

    /**
     * 获取任务处理趋势（最近7天）
     */
    @GetMapping("/taskTrend")
    public RespInfo<Map<String, Object>> getTaskTrend() {
        Map<String, Object> result = new HashMap<>();
        List<String> dateList = new ArrayList<>();
        List<Long> createdList = new ArrayList<>();
        List<Long> completedList = new ArrayList<>();
        
        try {
            LocalDate today = LocalDate.now();
            
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dateList.add(date.toString().substring(5)); // MM-dd 格式
                
                LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);
                Date startDate = Date.from(dayStart.atZone(ZoneId.systemDefault()).toInstant());
                Date endDate = Date.from(dayEnd.atZone(ZoneId.systemDefault()).toInstant());
                
                // 当日新增任务数（从 FlowBusiness 创建数统计）
                long created = flowBusinessMapper.selectCount(
                    new LambdaQueryWrapper<FlowBusiness>()
                        .ge(FlowBusiness::getCreateTime, dayStart)
                        .le(FlowBusiness::getCreateTime, dayEnd)
                );
                createdList.add(created);
                
                // 当日完成任务数（从 FlowBusiness 状态为已完成统计）
                long completed = flowBusinessMapper.selectCount(
                    new LambdaQueryWrapper<FlowBusiness>()
                        .ge(FlowBusiness::getEndTime, dayStart)
                        .le(FlowBusiness::getEndTime, dayEnd)
                        .isNotNull(FlowBusiness::getEndTime)
                );
                completedList.add(completed);
            }
            
            result.put("dates", dateList);
            result.put("created", createdList);
            result.put("completed", completedList);
            
        } catch (Exception e) {
            log.error("获取任务趋势数据失败", e);
            result.put("dates", dateList);
            result.put("created", createdList);
            result.put("completed", completedList);
        }
        
        return RespInfo.success(result);
    }

    /**
     * 获取流程分布统计（按流程模型分组）
     */
    @GetMapping("/processDistribution")
    public RespInfo<List<Map<String, Object>>> getProcessDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // 查询所有有 processDefKey 的业务记录
            List<FlowBusiness> businesses = flowBusinessMapper.selectList(
                new LambdaQueryWrapper<FlowBusiness>()
                    .select(FlowBusiness::getProcessDefKey, FlowBusiness::getTitle)
                    .isNotNull(FlowBusiness::getProcessDefKey)
                    .ne(FlowBusiness::getProcessDefKey, "")
            );
            
            // 按 processDefKey 分组统计
            Map<String, Long> distribution = businesses.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    FlowBusiness::getProcessDefKey,
                    java.util.stream.Collectors.counting()
                ));
            
            // 获取每个 processDefKey 对应的标题
            Map<String, String> keyToTitle = businesses.stream()
                .filter(b -> b.getTitle() != null && !b.getTitle().isEmpty())
                .collect(java.util.stream.Collectors.toMap(
                    FlowBusiness::getProcessDefKey,
                    FlowBusiness::getTitle,
                    (existing, replacement) -> existing
                ));
            
            // 转换为结果格式
            for (Map.Entry<String, Long> entry : distribution.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", keyToTitle.getOrDefault(entry.getKey(), entry.getKey()));
                item.put("value", entry.getValue());
                result.add(item);
            }
            
            // 按数量降序排序，取前 10
            result.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));
            if (result.size() > 10) {
                result = new ArrayList<>(result.subList(0, 10));
            }
            
            // 如果没有数据，返回提示
            if (result.isEmpty()) {
                Map<String, Object> emptyItem = new HashMap<>();
                emptyItem.put("name", "暂无数据");
                emptyItem.put("value", 0);
                result.add(emptyItem);
            }
            
        } catch (Exception e) {
            log.error("获取流程分布数据失败", e);
        }

        return RespInfo.success(result);
    }

    /**
     * 管理员终止流程
     */
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
    @PostMapping("/instance/{processInstanceId}/delete")
    @Transactional(rollbackFor = Exception.class)
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

        Map<String, Object> result = cleanupProcessInstanceIds(List.of(processInstanceId), reason);
        return RespInfo.success("流程数据已删除", result);
    }

    /**
     * 管理员删除当前筛选条件下的流程实例数据。
     */
    @PostMapping("/instances/cleanup")
    @Transactional(rollbackFor = Exception.class)
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

        List<FlowBusiness> businesses = flowBusinessMapper.selectList(
                buildBusinessQuery(processName, initiator, status, modelKey, startTime, endTime));

        Set<String> processInstanceIds = new LinkedHashSet<>();
        for (FlowBusiness business : businesses) {
            if (!isBlank(business.getProcessInstanceId())) {
                processInstanceIds.add(business.getProcessInstanceId());
            }
        }
        addFlowableProcessIdsByModelKey(processInstanceIds, modelKey, status);

        String reason = stringParam(params, "reason");
        if (isBlank(reason)) {
            reason = "管理员批量删除流程数据";
        }

        Map<String, Object> result = cleanupProcessInstanceIds(processInstanceIds, reason);

        int businessOnlyDeletedCount = 0;
        for (FlowBusiness business : businesses) {
            if (isBlank(business.getProcessInstanceId())) {
                businessOnlyDeletedCount += flowBusinessMapper.deleteById(business.getId());
            }
        }

        int deletedCount = ((Number) result.getOrDefault("deletedCount", 0)).intValue();
        result.put("deletedCount", deletedCount + businessOnlyDeletedCount);
        result.put("businessOnlyDeletedCount", businessOnlyDeletedCount);
        result.put("matchedBusinessCount", businesses.size());

        log.info("批量删除流程数据完成：modelKey={}, status={}, matchedBusinessCount={}, result={}",
                modelKey, status, businesses.size(), result);
        return RespInfo.success("流程数据已删除", result);
    }

    /**
     * 获取流程实例的所有变量
     */
    @GetMapping("/variables/{processInstanceId}")
    public RespInfo<Map<String, Object>> getProcessVariables(@PathVariable String processInstanceId) {
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
            return RespInfo.error("获取流程变量失败：" + e.getMessage());
        }
    }

    /**
     * 获取流程实例的历史活动节点列表（用于回退）
     */
    @GetMapping("/activities/{processInstanceId}")
    public RespInfo<List<Map<String, Object>>> getProcessActivities(@PathVariable String processInstanceId) {
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
            return RespInfo.error("获取流程活动节点失败：" + e.getMessage());
        }
    }

    /**
     * 获取流程实例的当前任务列表
     */
    @GetMapping("/current-tasks/{processInstanceId}")
    public RespInfo<List<Map<String, Object>>> getCurrentTasks(@PathVariable String processInstanceId) {
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
            return RespInfo.error("获取当前任务失败：" + e.getMessage());
        }
    }

    /**
     * 挂起流程实例
     */
    @PostMapping("/suspend/{processInstanceId}")
    public RespInfo<Void> suspendProcessInstance(@PathVariable String processInstanceId) {
        try {
            // 检查流程实例是否存在
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null) {
                log.warn("流程实例不存在或已完成：processInstanceId={}", processInstanceId);
                return RespInfo.error("流程实例不存在或已完成，无法挂起");
            }

            // 检查是否已经挂起
            if (processInstance.isSuspended()) {
                log.warn("流程实例已经是挂起状态：processInstanceId={}", processInstanceId);
                return RespInfo.error("流程实例已经是挂起状态");
            }

            // 挂起流程实例
            runtimeService.suspendProcessInstanceById(processInstanceId);

            // 更新业务表状态
            LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FlowBusiness::getProcessInstanceId, processInstanceId);
            FlowBusiness business = flowBusinessMapper.selectOne(wrapper);
            if (business != null) {
                business.setStatus("suspended");
                flowBusinessMapper.updateById(business);
            }

            log.info("流程挂起成功：processInstanceId={}", processInstanceId);
            return RespInfo.success("流程已挂起", null);
        } catch (Exception e) {
            log.error("挂起流程失败：processInstanceId={}", processInstanceId, e);
            return RespInfo.error("挂起流程失败：" + e.getMessage());
        }
    }

    /**
     * 激活流程实例
     */
    @PostMapping("/activate/{processInstanceId}")
    public RespInfo<Void> activateProcessInstance(@PathVariable String processInstanceId) {
        try {
            // 检查流程实例是否存在
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null) {
                log.warn("流程实例不存在或已完成：processInstanceId={}", processInstanceId);
                return RespInfo.error("流程实例不存在或已完成，无法激活");
            }

            // 检查是否已经激活
            if (!processInstance.isSuspended()) {
                log.warn("流程实例已经是激活状态：processInstanceId={}", processInstanceId);
                return RespInfo.error("流程实例已经是激活状态");
            }

            // 激活流程实例
            runtimeService.activateProcessInstanceById(processInstanceId);

            // 更新业务表状态
            LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FlowBusiness::getProcessInstanceId, processInstanceId);
            FlowBusiness business = flowBusinessMapper.selectOne(wrapper);
            if (business != null) {
                business.setStatus("running");
                flowBusinessMapper.updateById(business);
            }

            log.info("流程激活成功：processInstanceId={}", processInstanceId);
            return RespInfo.success("流程已激活", null);
        } catch (Exception e) {
            log.error("激活流程失败：processInstanceId={}", processInstanceId, e);
            return RespInfo.error("激活流程失败：" + e.getMessage());
        }
    }

    private LambdaQueryWrapper<FlowBusiness> buildBusinessQuery(String processName,
                                                               String initiator,
                                                               String status,
                                                               String modelKey,
                                                               Long startTime,
                                                               Long endTime) {
        LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!isBlank(processName), FlowBusiness::getTitle, processName);
        wrapper.like(!isBlank(initiator), FlowBusiness::getApplyUserName, initiator);
        wrapper.eq(!isBlank(status), FlowBusiness::getStatus, status);
        wrapper.eq(!isBlank(modelKey), FlowBusiness::getProcessDefKey, modelKey);
        LocalDateTime startDateTime = toLocalDateTime(startTime);
        LocalDateTime endDateTime = toLocalDateTime(endTime);
        wrapper.ge(startDateTime != null, FlowBusiness::getCreateTime, startDateTime);
        wrapper.le(endDateTime != null, FlowBusiness::getCreateTime, endDateTime);
        return wrapper;
    }

    private void addFlowableProcessIdsByModelKey(Set<String> processInstanceIds, String modelKey, String status) {
        if (isBlank(modelKey)) {
            return;
        }

        if (isBlank(status) || "running".equals(status)) {
            List<ProcessInstance> runningInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(modelKey)
                    .active()
                    .list();
            for (ProcessInstance processInstance : runningInstances) {
                processInstanceIds.add(processInstance.getId());
            }
        }

        if (isBlank(status) || "suspended".equals(status)) {
            List<ProcessInstance> suspendedInstances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey(modelKey)
                    .suspended()
                    .list();
            for (ProcessInstance processInstance : suspendedInstances) {
                processInstanceIds.add(processInstance.getId());
            }
        }

        if (isBlank(status)) {
            List<HistoricProcessInstance> historicInstances = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(modelKey)
                    .list();
            for (HistoricProcessInstance historicInstance : historicInstances) {
                processInstanceIds.add(historicInstance.getId());
            }
        } else if (!"running".equals(status) && !"suspended".equals(status)) {
            List<HistoricProcessInstance> historicInstances = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(modelKey)
                    .finished()
                    .list();
            for (HistoricProcessInstance historicInstance : historicInstances) {
                processInstanceIds.add(historicInstance.getId());
            }
        }
    }

    private Map<String, Object> cleanupProcessInstanceIds(Collection<String> processInstanceIds, String reason) {
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
                ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (processInstance != null) {
                    long runtimeStartTime = System.currentTimeMillis();
                    runtimeService.deleteProcessInstance(processInstanceId, reason);
                    runtimeDeletedCount++;
                    log.info("删除流程运行时数据完成：processInstanceId={}, cost={}ms",
                            processInstanceId, elapsedMillis(runtimeStartTime));
                }

                HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .singleResult();
                if (historicProcessInstance != null) {
                    long historyStartTime = System.currentTimeMillis();
                    historyService.deleteHistoricProcessInstance(processInstanceId);
                    historyDeletedCount++;
                    log.info("删除流程历史数据完成：processInstanceId={}, cost={}ms",
                            processInstanceId, elapsedMillis(historyStartTime));
                }

                long forgeRecordStartTime = System.currentTimeMillis();
                int currentForgeRecordDeletedCount = deleteForgeFlowRecords(processInstanceId);
                forgeRecordDeletedCount += currentForgeRecordDeletedCount;
                log.info("删除Forge流程关联记录完成：processInstanceId={}, deletedCount={}, cost={}ms",
                        processInstanceId,
                        currentForgeRecordDeletedCount,
                        elapsedMillis(forgeRecordStartTime));
                deletedCount++;
                log.info("删除流程数据完成：processInstanceId={}, runtimeDeleted={}, historyDeleted={}, forgeRecordDeleted={}, cost={}ms",
                        processInstanceId,
                        processInstance != null,
                        historicProcessInstance != null,
                        currentForgeRecordDeletedCount,
                        elapsedMillis(instanceStartTime));
            } catch (Exception e) {
                failedCount++;
                Map<String, String> failure = new HashMap<>();
                failure.put("processInstanceId", processInstanceId);
                failure.put("message", e.getMessage());
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

    private long elapsedMillis(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    private int deleteForgeFlowRecords(String processInstanceId) {
        int deleted = 0;
        deleted += flowTaskMapper.delete(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getProcessInstanceId, processInstanceId));
        deleted += flowCommentMapper.delete(new LambdaQueryWrapper<FlowComment>()
                .eq(FlowComment::getProcessInstanceId, processInstanceId));
        deleted += flowCcMapper.delete(new LambdaQueryWrapper<FlowCc>()
                .eq(FlowCc::getProcessInstanceId, processInstanceId));
        deleted += flowErrorLogMapper.delete(new LambdaQueryWrapper<FlowErrorLog>()
                .eq(FlowErrorLog::getProcessInstanceId, processInstanceId));
        deleted += flowFormInstanceMapper.delete(new LambdaQueryWrapper<FlowFormInstance>()
                .eq(FlowFormInstance::getProcessInstanceId, processInstanceId));
        deleted += flowFillBatchItemMapper.delete(new LambdaQueryWrapper<FlowFillBatchItem>()
                .eq(FlowFillBatchItem::getProcessInstanceId, processInstanceId));
        deleted += flowBusinessMapper.delete(new LambdaQueryWrapper<FlowBusiness>()
                .eq(FlowBusiness::getProcessInstanceId, processInstanceId));
        return deleted;
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
