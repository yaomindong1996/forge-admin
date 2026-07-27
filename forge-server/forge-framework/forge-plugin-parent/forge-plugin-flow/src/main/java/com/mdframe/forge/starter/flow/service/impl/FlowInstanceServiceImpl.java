package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowErrorLog;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowTaskMapper;
import com.mdframe.forge.starter.flow.service.FlowErrorLogService;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowModelService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 流程实例服务实现
 */
@Slf4j
@Service
public class FlowInstanceServiceImpl implements FlowInstanceService {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final long FLOW_START_LOCK_WAIT_SECONDS = 5L;

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FlowBusinessMapper flowBusinessMapper;

    @Autowired
    private FlowTaskMapper flowTaskMapper;

    /** 组织架构服务（可选，未引入时跳过上级领导变量注入）*/
    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    /** 用户服务（可选，用于获取用户角色信息）*/
    @Autowired(required = false)
    private ISysUserService sysUserService;

    @Autowired
    private FlowErrorLogService flowErrorLogService;

    @Autowired(required = false)
    private FlowModelService flowModelService;

    private final Map<String, ReentrantLock> localFlowStartLocks = new ConcurrentHashMap<>();

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
        Long tenantId = resolveTenantId();
        ReentrantLock startLock = acquireFlowStartLock(tenantId, businessKey);
        boolean unlockInFinally = true;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                unlockInFinally = false;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        unlockFlowStartLock(tenantId, businessKey, startLock);
                    }
                });
            }
            FlowBusiness existingBusiness = flowBusinessMapper.selectByBusinessKeyAndTenantId(tenantId, businessKey);
            if (isReusableExistingBusiness(existingBusiness)) {
                log.info("[流程启动幂等] 业务流程已存在，复用原流程实例: tenantId={}, businessKey={}, processInstanceId={}",
                        tenantId, businessKey, existingBusiness.getProcessInstanceId());
                return existingBusiness.getProcessInstanceId();
            }
            if (existingBusiness != null) {
                throw new RuntimeException("业务流程已存在且不可重复发起：" + businessKey);
            }

        // 1. 获取流程定义
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
                .createProcessDefinitionQuery()
                .processDefinitionKey(modelKey)
                .latestVersion()
                .singleResult();

        if (processDefinition == null) {
            processDefinition = deployModelOnDemand(modelKey, tenantId);
        }

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
        vars.putIfAbsent("businessKey", businessKey);
        vars.putIfAbsent("flowBusinessKey", businessKey);
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
                LoginUser loginUser = SessionHelper.getLoginUser();
                Long activeOrgId = loginUser == null ? null : loginUser.getActiveOrgId();
                Long roleTenantId = loginUser == null ? tenantId : loginUser.getTenantId();
                List<Long> roleIds = activeOrgId == null
                        ? List.of()
                        : sysUserService.selectUserOrgRoleIds(Long.parseLong(userId), activeOrgId, roleTenantId);
                if (roleIds != null && !roleIds.isEmpty()) {
                    // 注入角色ID列表（逗号分隔）
                    String roleIdsStr = roleIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    vars.put("startUserRoleIds", roleIdsStr);
                    log.info("自动注入用户角色ID变量：startUserRoleIds={}", roleIdsStr);
                }
                if (activeOrgId != null) {
                    vars.put("startUserActiveOrgId", String.valueOf(activeOrgId));
                    log.info("自动注入当前组织变量：startUserActiveOrgId={}", activeOrgId);
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
        business.setTenantId(tenantId);
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

        try {
            flowBusinessMapper.insert(business);
        } catch (DuplicateKeyException e) {
            return handleDuplicateBusinessKey(tenantId, businessKey, e);
        }
        log.info("保存业务信息成功：businessKey={}", businessKey);

        // 4. 启动流程（会触发 TASK_CREATED 事件，此时业务信息已存在）
        ProcessInstance processInstance;
        try {
            processInstance = runtimeService.startProcessInstanceByKey(
                    modelKey,
                    businessKey,
                    vars
            );
        } catch (Exception e) {
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setProcessDefKey(modelKey);
            errorLog.setBusinessKey(businessKey);
            errorLog.setErrorStage("PROCESS_START");
            errorLog.setErrorMessage("启动流程失败：" + e.getMessage());
            flowErrorLogService.recordError(errorLog, e);
            throw e;
        }

        // 5. 更新流程实例ID
        business.setProcessInstanceId(processInstance.getId());
        flowBusinessMapper.updateById(business);

        log.info("启动流程成功：businessKey={}, processInstanceId={}",
                businessKey, processInstance.getId());

        return processInstance.getId();
        } finally {
            if (unlockInFinally) {
                unlockFlowStartLock(tenantId, businessKey, startLock);
            }
        }
    }

    @Override
    public FlowBusiness getProcessStatus(String businessKey) {
        return flowBusinessMapper.selectByBusinessKeyAndTenantId(resolveTenantId(), businessKey);
    }

    private String handleDuplicateBusinessKey(Long tenantId, String businessKey, DuplicateKeyException e) {
        FlowBusiness existingBusiness = flowBusinessMapper.selectByBusinessKeyAndTenantId(tenantId, businessKey);
        if (isReusableExistingBusiness(existingBusiness)) {
            log.info("[流程启动幂等] 业务流程唯一键已存在，复用原流程实例: tenantId={}, businessKey={}, processInstanceId={}",
                    tenantId, businessKey, existingBusiness.getProcessInstanceId());
            return existingBusiness.getProcessInstanceId();
        }
        if (existingBusiness == null || isBlank(existingBusiness.getProcessInstanceId())) {
            log.warn("[流程启动幂等] 业务流程正在发起，实例ID尚未写回: tenantId={}, businessKey={}",
                    tenantId, businessKey);
            throw new RuntimeException("流程正在发起，请稍后重试", e);
        }
        log.warn("[流程启动幂等] 业务流程已存在且不可重复发起: tenantId={}, businessKey={}, status={}, processInstanceId={}",
                tenantId, businessKey, existingBusiness.getStatus(), existingBusiness.getProcessInstanceId());
        throw new RuntimeException("业务流程已存在且不可重复发起：" + businessKey, e);
    }

    private boolean isReusableExistingBusiness(FlowBusiness business) {
        if (business == null || isBlank(business.getProcessInstanceId())) {
            return false;
        }
        if (isEndedStatus(business.getStatus())) {
            return false;
        }
        if (isRuntimeProcessActive(business.getProcessInstanceId())) {
            return true;
        }
        String status = normalizeStatus(business.getStatus());
        return "running".equals(status) || "draft".equals(status) || "suspended".equals(status);
    }

    private ProcessDefinition deployModelOnDemand(String modelKey, Long tenantId) {
        if (flowModelService == null || isBlank(modelKey)) {
            return null;
        }
        try {
            FlowModel model = flowModelService.getOne(new LambdaQueryWrapper<FlowModel>()
                    .eq(FlowModel::getTenantId, tenantId)
                    .eq(FlowModel::getModelKey, modelKey)
                    .eq(FlowModel::getDelFlag, 0)
                    .last("LIMIT 1"));
            if (model == null || isBlank(model.getBpmnXml())) {
                return null;
            }
            log.warn("[流程启动] 流程定义不存在，尝试从模型自动部署: tenantId={}, modelKey={}, modelId={}",
                    tenantId, modelKey, model.getId());
            flowModelService.deployModel(model.getId(), "流程启动时自动部署缺失定义");
            return processEngine.getRepositoryService()
                    .createProcessDefinitionQuery()
                    .processDefinitionKey(modelKey)
                    .latestVersion()
                    .singleResult();
        } catch (Exception e) {
            log.error("[流程启动] 自动部署流程模型失败: tenantId={}, modelKey={}", tenantId, modelKey, e);
            return null;
        }
    }

    private boolean isRuntimeProcessActive(String processInstanceId) {
        try {
            return runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult() != null;
        } catch (Exception e) {
            log.debug("[流程启动幂等] 查询运行中流程失败: processInstanceId={}, error={}",
                    processInstanceId, e.getMessage());
            return false;
        }
    }

    private boolean isEndedStatus(String status) {
        String normalized = normalizeStatus(status);
        return "approved".equals(normalized)
                || "rejected".equals(normalized)
                || "canceled".equals(normalized)
                || "terminated".equals(normalized)
                || "completed".equals(normalized);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = SessionHelper.getTenantId();
        }
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private ReentrantLock acquireFlowStartLock(Long tenantId, String businessKey) {
        String lockKey = buildFlowStartLockKey(tenantId, businessKey);
        ReentrantLock localLock = localFlowStartLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
        try {
            if (!localLock.tryLock(FLOW_START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new RuntimeException("流程正在发起，请勿重复提交");
            }
            return localLock;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("流程发起锁等待被中断，请稍后重试", e);
        }
    }

    private void unlockFlowStartLock(Long tenantId, String businessKey, ReentrantLock localLock) {
        if (localLock == null || !localLock.isHeldByCurrentThread()) {
            return;
        }
        String lockKey = buildFlowStartLockKey(tenantId, businessKey);
        localLock.unlock();
        if (!localLock.isLocked() && !localLock.hasQueuedThreads()) {
            localFlowStartLocks.remove(lockKey, localLock);
        }
    }

    private String buildFlowStartLockKey(Long tenantId, String businessKey) {
        return "forge:flow:start:" + safeLockToken(tenantId) + ":" + safeLockToken(businessKey);
    }

    private String safeLockToken(Object value) {
        if (value == null) {
            return "null";
        }
        return String.valueOf(value).replaceAll("[^A-Za-z0-9:_-]", "_");
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateProcess(String businessKey, String userId, String reason) {
        FlowBusiness business = getProcessStatus(businessKey);
        if (business == null) {
            throw new RuntimeException("流程实例不存在");
        }

        try {
            runtimeService.deleteProcessInstance(business.getProcessInstanceId(), reason);

            business.setStatus("canceled");
            business.setEndTime(LocalDateTime.now());
            flowBusinessMapper.updateById(business);

            log.info("终止流程：businessKey={}, reason={}", businessKey, reason);
        } catch (Exception e) {
            log.error("终止流程失败", e);
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setProcessInstanceId(business.getProcessInstanceId());
            errorLog.setBusinessKey(businessKey);
            errorLog.setErrorStage("PROCESS_TERMINATE");
            errorLog.setErrorMessage("终止流程失败：" + e.getMessage());
            flowErrorLogService.recordError(errorLog, e);
            throw e;
        }
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
        return readProcessVariables(business.getProcessInstanceId());
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

    private Map<String, Object> readProcessVariables(String processInstanceId) {
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
    @Transactional(rollbackFor = Exception.class)
    public void rollbackToActivity(String processInstanceId, String targetActivityId, String userId, String reason) {
        log.info("流程节点回退：processInstanceId={}, targetActivityId={}, userId={}, reason={}",
                processInstanceId, targetActivityId, userId, reason);

        if (processInstanceId == null || targetActivityId == null) {
            throw new RuntimeException("流程实例ID和目标节点ID不能为空");
        }
        lockCurrentTenantProcessInstance(processInstanceId);

        try {
            // 使用Flowable的RuntimeService进行节点跳转
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveActivityIdsToSingleActivityId(getCurrentActivityIds(processInstanceId), targetActivityId)
                    .changeState();

            log.info("流程节点回退成功：processInstanceId={}, targetActivityId={}", processInstanceId, targetActivityId);
        } catch (Exception e) {
            log.error("流程节点回退失败", e);
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setProcessInstanceId(processInstanceId);
            errorLog.setActivityId(targetActivityId);
            errorLog.setErrorStage("PROCESS_ROLLBACK");
            errorLog.setErrorMessage("流程节点回退失败：" + e.getMessage());
            flowErrorLogService.recordError(errorLog, e);
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

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null || task.getProcessInstanceId() == null) {
            throw new BusinessException(404, "任务不存在或不属于当前租户");
        }
        lockCurrentTenantProcessInstance(task.getProcessInstanceId());

        try {
            String owner = task.getAssignee() != null ? task.getAssignee() : userId;
            if (owner != null && !owner.isEmpty()) {
                taskService.setOwner(taskId, owner);
            }

            // 设置新的处理人
            taskService.setAssignee(taskId, newAssignee);

            // 添加转派备注
            if (reason != null && !reason.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), "转派", reason);
            }

            FlowTask flowTask = flowTaskMapper.selectByTaskId(taskId);
            if (flowTask != null) {
                flowTask.setAssignee(newAssignee);
                flowTask.setOwner(owner);
                flowTask.setStatus(0);
                flowTask.setComment(reason);
                flowTaskMapper.updateById(flowTask);
            }

            log.info("任务转派成功：taskId={}, newAssignee={}", taskId, newAssignee);
        } catch (Exception e) {
            log.error("任务转派失败", e);
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setTaskId(taskId);
            errorLog.setErrorStage("TASK_REASSIGN");
            errorLog.setErrorMessage("任务转派失败：" + e.getMessage());
            flowErrorLogService.recordError(errorLog, e);
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
        Long tenantId = requireCurrentTenantId();
        FlowBusiness business = lockCurrentTenantProcessInstance(processInstanceId, tenantId);

        try {
            // 检查流程实例是否存在于运行时表
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null) {
                log.warn("流程实例不存在或已完成：processInstanceId={}", processInstanceId);
                throw new RuntimeException("流程实例不存在或已完成，无法终止");
            }

            // 检查流程实例状态
            if (processInstance.isSuspended()) {
                log.info("流程实例已挂起，先激活再终止：processInstanceId={}", processInstanceId);
                runtimeService.activateProcessInstanceById(processInstanceId);
            }

            // 删除流程实例
            runtimeService.deleteProcessInstance(processInstanceId, reason);

            business.setStatus("terminated");
            business.setEndTime(LocalDateTime.now());
            flowBusinessMapper.updateById(business);

            log.info("流程终止成功：processInstanceId={}", processInstanceId);
        } catch (Exception e) {
            log.error("流程终止失败", e);
            FlowErrorLog errorLog = new FlowErrorLog();
            errorLog.setProcessInstanceId(processInstanceId);
            errorLog.setErrorStage("PROCESS_TERMINATE");
            errorLog.setErrorMessage("终止流程失败：" + e.getMessage());
            flowErrorLogService.recordError(errorLog, e);
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

    private FlowBusiness lockCurrentTenantProcessInstance(String processInstanceId) {
        return lockCurrentTenantProcessInstance(processInstanceId, requireCurrentTenantId());
    }

    private FlowBusiness lockCurrentTenantProcessInstance(String processInstanceId, Long tenantId) {
        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceIdAndTenantIdForUpdate(
                processInstanceId, tenantId);
        if (business == null) {
            throw new BusinessException(404, "流程实例不存在或不属于当前租户");
        }
        return business;
    }

    private Long requireCurrentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = SessionHelper.getTenantId();
        }
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException(403, "无法确定当前租户，禁止管理流程实例");
        }
        return tenantId;
    }
}
