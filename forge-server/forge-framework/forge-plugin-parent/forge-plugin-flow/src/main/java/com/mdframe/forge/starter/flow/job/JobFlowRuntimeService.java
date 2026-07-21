package com.mdframe.forge.starter.flow.job;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.mapper.FlowModelMapper;
import com.mdframe.forge.starter.job.flow.JobFlowBindingSnapshot;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionRequest;
import com.mdframe.forge.starter.job.flow.JobFlowExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 定时任务专用流程运行时，只允许启动保存时固定的已发布流程定义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobFlowRuntimeService {

    private static final long START_LOCK_WAIT_SECONDS = 5L;

    private final FlowModelMapper flowModelMapper;
    private final FlowBusinessMapper flowBusinessMapper;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final IdentityService identityService;
    private final JobFlowTechnicalIdentityProperties identityProperties;
    private final Map<String, ReentrantLock> startLocks = new ConcurrentHashMap<>();

    public JobFlowBindingSnapshot validateBinding(String modelKey, Integer modelVersion) {
        requireIdentity();
        if (modelKey == null || modelKey.isBlank() || modelVersion == null || modelVersion <= 0) {
            throw new BusinessException(400, "流程模型Key和版本不能为空");
        }
        JobFlowBindingSnapshot snapshot = flowModelMapper.selectPublishedJobBinding(
                identityProperties.getTenantId(), modelKey.trim(), modelVersion);
        if (snapshot == null) {
            throw new BusinessException(400, "未找到指定的已发布流程版本");
        }
        validateProcessDefinition(snapshot);
        return snapshot;
    }

    @Transactional(rollbackFor = Exception.class)
    public JobFlowExecutionResult start(JobFlowExecutionRequest request) {
        if (request == null) {
            throw new BusinessException(400, "任务流程启动请求不能为空");
        }
        requireIdentity();
        Long tenantId = identityProperties.getTenantId();
        ReentrantLock lock = acquireStartLock(tenantId, request.businessKey());
        boolean unlockInFinally = true;
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                unlockInFinally = false;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        unlockStartLock(tenantId, request.businessKey(), lock);
                    }
                });
            }
            JobFlowExecutionResult existing = findExisting(tenantId, request.businessKey());
            if (existing != null) {
                return existing;
            }

            JobFlowBindingSnapshot current = validateBinding(
                    request.binding().modelKey(), request.binding().modelVersion());
            if (!current.equals(request.binding())) {
                throw new BusinessException(409, "任务流程绑定快照已失效，请重新保存任务");
            }

            FlowBusiness business = buildBusiness(request, current, tenantId);
            try {
                if (flowBusinessMapper.insert(business) <= 0) {
                    throw new BusinessException("创建任务流程业务关联失败");
                }
            } catch (DuplicateKeyException exception) {
                JobFlowExecutionResult recovered = findExisting(tenantId, request.businessKey());
                if (recovered != null) {
                    return recovered;
                }
                throw new BusinessException("任务流程正在发起，请稍后重试", exception);
            }

            ProcessInstance processInstance;
            identityService.setAuthenticatedUserId(identityProperties.getUserId());
            try {
                processInstance = runtimeService.startProcessInstanceById(
                        current.processDefinitionId(), request.businessKey(), buildVariables(request));
            } finally {
                identityService.setAuthenticatedUserId(null);
            }
            if (processInstance == null || processInstance.getId() == null
                    || processInstance.getId().isBlank()) {
                throw new BusinessException("Flowable未返回流程实例ID");
            }

            business.setProcessInstanceId(processInstance.getId());
            business.setUpdateTime(LocalDateTime.now());
            if (flowBusinessMapper.updateById(business) <= 0) {
                throw new BusinessException("任务流程实例关联更新失败");
            }
            log.info("任务流程启动成功: businessKey={}, processDefinitionId={}, processInstanceId={}",
                    request.businessKey(), current.processDefinitionId(), processInstance.getId());
            return new JobFlowExecutionResult(request.businessKey(), processInstance.getId(), false);
        } finally {
            if (unlockInFinally) {
                unlockStartLock(tenantId, request.businessKey(), lock);
            }
        }
    }

    public JobFlowExecutionResult findByBusinessKey(String businessKey) {
        requireIdentity();
        if (businessKey == null || businessKey.isBlank()) {
            throw new BusinessException(400, "任务流程businessKey不能为空");
        }
        return findExisting(identityProperties.getTenantId(), businessKey);
    }

    private void validateProcessDefinition(JobFlowBindingSnapshot snapshot) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(snapshot.processDefinitionId())
                .singleResult();
        if (definition == null) {
            throw new BusinessException(400, "固定流程定义不存在");
        }
        if (definition.isSuspended()) {
            throw new BusinessException(409, "固定流程定义已挂起");
        }
        if (!snapshot.processDefinitionId().equals(definition.getId())
                || !snapshot.modelKey().equals(definition.getKey())) {
            throw new BusinessException(409, "流程定义与模型Key不匹配");
        }
        if (!snapshot.modelVersion().equals(definition.getVersion())) {
            throw new BusinessException(409, "流程定义版本与发布快照不匹配");
        }
        if (!snapshot.deploymentId().equals(definition.getDeploymentId())) {
            throw new BusinessException(409, "流程定义部署与发布快照不匹配");
        }
    }

    private FlowBusiness buildBusiness(JobFlowExecutionRequest request,
                                       JobFlowBindingSnapshot binding, Long tenantId) {
        LocalDateTime now = LocalDateTime.now();
        FlowBusiness business = new FlowBusiness();
        business.setId(UUID.randomUUID().toString());
        business.setTenantId(tenantId);
        business.setBusinessKey(request.businessKey());
        business.setBusinessType("JOB");
        business.setProcessDefId(binding.processDefinitionId());
        business.setProcessDefKey(binding.modelKey());
        business.setTitle("定时任务流程-" + request.jobConfigId());
        business.setStatus("running");
        business.setApplyUserId(identityProperties.getUserId());
        business.setApplyUserName(identityProperties.getUserName());
        business.setApplyDeptId(String.valueOf(identityProperties.getActiveOrgId()));
        business.setApplyDeptName(identityProperties.getActiveOrgName());
        business.setApplyTime(now);
        business.setCreateTime(now);
        business.setUpdateTime(now);
        return business;
    }

    private Map<String, Object> buildVariables(JobFlowExecutionRequest request) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("initiator", identityProperties.getUserId());
        variables.put("startUserId", identityProperties.getUserId());
        variables.put("startUserName", identityProperties.getUserName());
        variables.put("startDeptId", String.valueOf(identityProperties.getActiveOrgId()));
        variables.put("startDeptName", identityProperties.getActiveOrgName());
        variables.put("tenantId", identityProperties.getTenantId());
        variables.put("activeOrgId", identityProperties.getActiveOrgId());
        variables.put("businessKey", request.businessKey());
        variables.put("flowBusinessKey", request.businessKey());
        variables.put("businessType", "JOB");
        variables.put("processTitle", "定时任务流程-" + request.jobConfigId());
        variables.put("jobConfigId", request.jobConfigId());
        variables.put("executionId", request.executionId());
        variables.put("jobInput", new LinkedHashMap<>(request.jobInput()));
        return variables;
    }

    private JobFlowExecutionResult findExisting(Long tenantId, String businessKey) {
        FlowBusiness business = flowBusinessMapper.selectByBusinessKeyAndTenantId(tenantId, businessKey);
        if (business == null) {
            return null;
        }
        if (business.getProcessInstanceId() == null || business.getProcessInstanceId().isBlank()) {
            throw new BusinessException(409, "任务流程正在发起，请稍后重试");
        }
        return new JobFlowExecutionResult(businessKey, business.getProcessInstanceId(), true);
    }

    private void requireIdentity() {
        try {
            identityProperties.requireConfigured();
        } catch (IllegalStateException exception) {
            throw new BusinessException(503, exception.getMessage(), exception);
        }
    }

    private ReentrantLock acquireStartLock(Long tenantId, String businessKey) {
        String lockKey = tenantId + ":" + businessKey;
        ReentrantLock lock = startLocks.computeIfAbsent(lockKey, key -> new ReentrantLock());
        try {
            if (!lock.tryLock(START_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new BusinessException(409, "任务流程正在发起，请稍后重试");
            }
            return lock;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("任务流程启动锁等待被中断", exception);
        }
    }

    private void unlockStartLock(Long tenantId, String businessKey, ReentrantLock lock) {
        if (lock == null || !lock.isHeldByCurrentThread()) {
            return;
        }
        String lockKey = tenantId + ":" + businessKey;
        lock.unlock();
        if (!lock.isLocked() && !lock.hasQueuedThreads()) {
            startLocks.remove(lockKey, lock);
        }
    }
}
