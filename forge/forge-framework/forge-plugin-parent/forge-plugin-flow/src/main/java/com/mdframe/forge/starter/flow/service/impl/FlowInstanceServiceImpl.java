package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程实例服务实现
 */
@Slf4j
@Service
public class FlowInstanceServiceImpl implements FlowInstanceService {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FlowBusinessMapper flowBusinessMapper;

    /** 组织架构服务（可选，未引入时跳过上级领导变量注入）*/
    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    /** 用户服务（可选，用于获取用户角色信息）*/
    @Autowired(required = false)
    private ISysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(String modelKey, String businessKey, String title,
                               Map<String, Object> variables, String userId, String userName,
                               String deptId, String deptName) {
        return startProcess(modelKey, businessKey, null, title, variables,
                userId, userName, deptId, deptName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(String modelKey, String businessKey, String businessType,
                                String title, Map<String, Object> variables, String userId,
                                String userName, String deptId, String deptName) {
        // 1. 获取流程定义
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
                .createProcessDefinitionQuery()
                .processDefinitionKey(modelKey)
                .latestVersion()
                .singleResult();

        if (processDefinition == null) {
            log.error("[流程启动失败] 流程定义不存在: modelKey={}, 请检查流程是否已部署", modelKey);
            throw new RuntimeException("流程定义不存在：" + modelKey);
        }

        // 2. 构建流程变量
        Map<String, Object> vars = variables != null ? new HashMap<>(variables) : new HashMap<>();

        // 内置变量：发起人信息
        vars.put("initiator", userId);
        vars.put("startUserId", userId);
        vars.put("startUserName", userName);
        vars.put("startDeptId", deptId);
        vars.put("startDeptName", deptName);

        // 内置变量：业务主键
        vars.put("businessKey", businessKey);
        vars.put("businessType", businessType);

        // 内置变量：流程标题
        vars.put("processTitle", title);

        // 自动注入上级领导变量（兼容 BPMN 中直接使用 ${initiatorLeader} 的老式写法）
        if (userId != null && !userId.isEmpty() && flowOrgIntegrationService != null) {
            try {
                String leaderId = flowOrgIntegrationService.getLeaderUserIdByLevel(userId, 1);
                if (leaderId == null) {
                    List<String> leaderIds = flowOrgIntegrationService.getLeaderUserIds(userId);
                    leaderId = leaderIds.isEmpty() ? null : leaderIds.get(0);
                }
                if (leaderId != null) {
                    vars.put("initiatorLeader", leaderId);
                    log.info("自动注入上级领导变量：initiatorLeader={}", leaderId);
                } else {
                    log.warn("未找到发起人的上级领导，initiatorLeader 变量未注入：userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("获取上级领导失败，initiatorLeader 变量未注入：userId={}", userId, e);
            }
        }

        // 自动注入行政区划编码（用于按区域查找审批人）
        if (userId != null && !userId.isEmpty() && sysUserService != null) {
            try {
                SysUser user = sysUserService.selectUserById(Long.parseLong(userId));
                if (user != null && user.getRegionCode() != null && !user.getRegionCode().isEmpty()) {
                    vars.put("regionCode", user.getRegionCode());
                    vars.put("startUserRegionCode", user.getRegionCode());
                    log.info("自动注入行政区划编码变量：regionCode={}, startUserRegionCode={}",
                            user.getRegionCode(), user.getRegionCode());
                } else {
                    log.warn("用户行政区划编码为空，regionCode 变量未注入：userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("获取用户行政区划编码失败，regionCode 变量未注入：userId={}", userId, e);
            }
        }

        // 自动注入用户角色信息
        if (userId != null && !userId.isEmpty() && sysUserService != null) {
            try {
                List<Long> roleIds = sysUserService.selectUserRoleIds(Long.parseLong(userId));
                if (roleIds != null && !roleIds.isEmpty()) {
                    // 注入角色ID列表（逗号分隔）
                    String roleIdsStr = roleIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    vars.put("startUserRoleIds", roleIdsStr);
                    log.info("自动注入用户角色ID变量：startUserRoleIds={}", roleIdsStr);
                }
            } catch (Exception e) {
                log.warn("获取用户角色失败，startUserRoleIds 变量未注入：userId={}", userId, e);
            }
        }

        // 自动注入组织ID信息
        if (userId != null && !userId.isEmpty() && sysUserService != null) {
            try {
                List<Long> orgIds = sysUserService.selectUserOrgIds(Long.parseLong(userId));
                if (orgIds != null && !orgIds.isEmpty()) {
                    // 注入组织ID列表（逗号分隔）
                    String orgIdsStr = orgIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    vars.put("startUserOrgIds", orgIdsStr);
                    log.info("自动注入用户组织ID变量：startUserOrgIds={}", orgIdsStr);
                }
            } catch (Exception e) {
                log.warn("获取用户组织失败，startUserOrgIds 变量未注入：userId={}", userId, e);
            }
        }

        // 3. 先保存业务关联（必须在启动流程之前，否则事件监听器查询不到业务信息）
        FlowBusiness business = new FlowBusiness();
        business.setBusinessKey(businessKey);
        business.setBusinessType(businessType);
        business.setProcessDefId(processDefinition.getId());
        business.setProcessDefKey(processDefinition.getKey());
        business.setTitle(title);
        business.setStatus("running");
        business.setApplyUserId(userId);
        business.setApplyUserName(userName);
        business.setApplyDeptId(deptId);
        business.setApplyDeptName(deptName);
        business.setApplyTime(LocalDateTime.now());
        business.setCreateTime(LocalDateTime.now());
        business.setUpdateTime(LocalDateTime.now());

        flowBusinessMapper.insert(business);
        log.info("保存业务信息成功：businessKey={}", businessKey);

        // 4. 启动流程（会触发 TASK_CREATED 事件，此时业务信息已存在）
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                modelKey,
                businessKey,
                vars
        );

        // 5. 更新流程实例ID
        business.setProcessInstanceId(processInstance.getId());
        flowBusinessMapper.updateById(business);

        log.info("启动流程成功：businessKey={}, processInstanceId={}",
                businessKey, processInstance.getId());

        return processInstance.getId();
    }

    @Override
    public FlowBusiness getProcessStatus(String businessKey) {
        LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowBusiness::getBusinessKey, businessKey);
        return flowBusinessMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateProcess(String businessKey, String userId, String reason) {
        FlowBusiness business = getProcessStatus(businessKey);
        if (business == null) {
            throw new RuntimeException("流程实例不存在");
        }

        runtimeService.deleteProcessInstance(business.getProcessInstanceId(), reason);

        business.setStatus("canceled");
        business.setEndTime(LocalDateTime.now());
        flowBusinessMapper.updateById(business);

        log.info("终止流程：businessKey={}, reason={}", businessKey, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcess(String businessKey, String userId) {
        FlowBusiness business = getProcessStatus(businessKey);
        if (business != null && business.getProcessInstanceId() != null) {
            runtimeService.deleteProcessInstance(business.getProcessInstanceId(), "删除");
            flowBusinessMapper.deleteById(business.getId());
            log.info("删除流程实例：businessKey={}", businessKey);
        }
    }

    @Override
    public Map<String, Object> getProcessVariables(String businessKey) {
        FlowBusiness business = getProcessStatus(businessKey);
        if (business == null || business.getProcessInstanceId() == null) {
            return new HashMap<>();
        }
        return runtimeService.getVariables(business.getProcessInstanceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProcessVariables(String businessKey, Map<String, Object> variables) {
        FlowBusiness business = getProcessStatus(businessKey);
        if (business == null || business.getProcessInstanceId() == null) {
            throw new RuntimeException("流程实例不存在");
        }

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            runtimeService.setVariable(business.getProcessInstanceId(), entry.getKey(), entry.getValue());
        }

        log.info("更新流程变量：businessKey={}", businessKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackToActivity(String processInstanceId, String targetActivityId, String userId, String reason) {
        log.info("流程节点回退：processInstanceId={}, targetActivityId={}, userId={}, reason={}",
                processInstanceId, targetActivityId, userId, reason);

        if (processInstanceId == null || targetActivityId == null) {
            throw new RuntimeException("流程实例ID和目标节点ID不能为空");
        }

        try {
            // 使用Flowable的RuntimeService进行节点跳转
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveActivityIdsToSingleActivityId(getCurrentActivityIds(processInstanceId), targetActivityId)
                    .changeState();

            log.info("流程节点回退成功：processInstanceId={}, targetActivityId={}", processInstanceId, targetActivityId);
        } catch (Exception e) {
            log.error("流程节点回退失败", e);
            throw new RuntimeException("流程节点回退失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reassignTask(String taskId, String newAssignee, String userId, String reason) {
        log.info("任务转派：taskId={}, newAssignee={}, userId={}, reason={}",
                taskId, newAssignee, userId, reason);

        if (taskId == null || newAssignee == null) {
            throw new RuntimeException("任务ID和新处理人ID不能为空");
        }

        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new RuntimeException("任务不存在：" + taskId);
            }

            // 设置新的处理人
            taskService.setAssignee(taskId, newAssignee);

            // 添加转派备注
            if (reason != null && !reason.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), "转派", reason);
            }

            log.info("任务转派成功：taskId={}, newAssignee={}", taskId, newAssignee);
        } catch (Exception e) {
            log.error("任务转派失败", e);
            throw new RuntimeException("任务转派失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateProcessByInstanceId(String processInstanceId, String userId, String reason) {
        log.info("根据流程实例ID终止流程：processInstanceId={}, userId={}, reason={}",
                processInstanceId, userId, reason);

        if (processInstanceId == null) {
            throw new RuntimeException("流程实例ID不能为空");
        }

        try {
            // 检查流程实例是否存在于运行时表
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null) {
                // 流程实例不存在于运行时表，可能已经完成
                log.warn("流程实例不存在或已完成：processInstanceId={}", processInstanceId);
                throw new RuntimeException("流程实例不存在或已完成，无法终止");
            }

            // 检查流程实例状态
            if (processInstance.isSuspended()) {
                // 如果流程已挂起，先激活再终止
                log.info("流程实例已挂起，先激活再终止：processInstanceId={}", processInstanceId);
                runtimeService.activateProcessInstanceById(processInstanceId);
            }

            // 删除流程实例
            runtimeService.deleteProcessInstance(processInstanceId, reason);

            // 更新业务状态
            LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FlowBusiness::getProcessInstanceId, processInstanceId);
            FlowBusiness business = flowBusinessMapper.selectOne(wrapper);

            if (business != null) {
                business.setStatus("terminated");
                business.setEndTime(LocalDateTime.now());
                flowBusinessMapper.updateById(business);
            }

            log.info("流程终止成功：processInstanceId={}", processInstanceId);
        } catch (Exception e) {
            log.error("流程终止失败", e);
            throw new RuntimeException("流程终止失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前流程实例的所有活动节点ID
     */
    private List<String> getCurrentActivityIds(String processInstanceId) {
        return runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .filter(execution -> execution.getActivityId() != null)
                .map(org.flowable.engine.runtime.Execution::getActivityId)
                .collect(Collectors.toList());
    }
}
