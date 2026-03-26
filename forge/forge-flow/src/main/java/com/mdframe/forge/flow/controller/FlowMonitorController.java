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
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;
    private final ISysUserService sysUserService;

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
            @RequestParam(required = false) String modelKey) {
        
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
}
