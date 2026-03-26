package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowModel;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private FlowBusinessMapper flowBusinessMapper;

    /** 组织架构服务（可选，未引入时跳过上级领导变量注入）*/
    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

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
            throw new RuntimeException("流程定义不存在：" + modelKey);
        }

        // 2. 构建流程变量
        Map<String, Object> vars = variables != null ? new HashMap<>(variables) : new HashMap<>();
        vars.put("initiator", userId);
        vars.put("startUserId", userId);
        vars.put("startUserName", userName);
        vars.put("startDeptId", deptId);
        vars.put("startDeptName", deptName);

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
}