package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.auth.config.FlowDelegationSessionVerifier;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 流程实例管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/flow/instance")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowInstanceController {

    private final FlowInstanceService flowInstanceService;
    private final FlowBusinessMapper flowBusinessMapper;
    private final FlowOrgIntegrationService flowOrgIntegrationService;
    private final FlowDelegationSessionVerifier flowDelegationSessionVerifier;

    /**
     * 发起流程
     */
    @PostMapping("/start/{modelKey}")
    public RespInfo<String> start(
            @PathVariable String modelKey,
            @RequestBody Map<String, Object> params) {
        return startInternal(modelKey, params, false);
    }

    /**
     * 用户委托流程发起入口。发起人、租户和当前组织只从已验证 Session 解析，
     * 不接受请求体中的代办身份。
     */
    @PostMapping("/start-delegated/{modelKey}")
    @SaCheckPermission("ai:businessFlow:start")
    public RespInfo<String> startDelegated(
            @PathVariable String modelKey,
            @RequestBody Map<String, Object> params) {
        flowDelegationSessionVerifier.requireTrustedDelegation();
        return startInternal(modelKey, params, true);
    }

    /**
     * 平台 R3 高风险审批专用委托入口。
     */
    @PostMapping("/start-delegated-approval/{modelKey}")
    @SaCheckPermission("ai:capability:approval:submit")
    public RespInfo<String> startDelegatedApproval(
            @PathVariable String modelKey,
            @RequestBody Map<String, Object> params) {
        flowDelegationSessionVerifier.requireTrustedDelegation();
        return startInternal(modelKey, params, true);
    }

    private RespInfo<String> startInternal(
            String modelKey,
            Map<String, Object> params,
            boolean trustedDelegationRequired) {
        String businessKey = (String) params.get("businessKey");
        String businessType = (String) params.get("businessType");
        String title = (String) params.get("title");
        
        // 提取变量（排除特殊字段）
        Map<String, Object> variables = new HashMap<>(params);
        variables.remove("businessKey");
        variables.remove("businessType");
        variables.remove("title");
        variables.remove("userId");
        variables.remove("userName");
        variables.remove("deptId");
        variables.remove("deptName");
        // FlowClient 发送的 body 中包含 variables 嵌套字段，需要把展开后删除外层 key
        Object nestedVars = variables.remove("variables");
        if (nestedVars instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> innerVars = (Map<String, Object>) nestedVars;
            variables.putAll(innerVars);
        }
        
        // FlowClient 会显式传入业务发起人；Session 仅作为人工从流程中心发起时的兜底。
        String requestUserId = toText(params.get("userId"));
        String requestUserName = toText(params.get("userName"));
        String requestDeptId = toText(params.get("deptId"));
        String requestDeptName = toText(params.get("deptName"));
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (trustedDelegationRequired && (loginUser == null
                || loginUser.getUserId() == null
                || loginUser.getTenantId() == null
                || loginUser.getActiveOrgId() == null)) {
            throw new IllegalArgumentException("FLOW_START_DELEGATION_REQUIRED");
        }
        String userId = trustedDelegationRequired
                ? String.valueOf(loginUser.getUserId())
                : StringUtils.hasText(requestUserId)
                        ? requestUserId
                        : loginUser != null ? String.valueOf(loginUser.getUserId()) : null;
        String userName = trustedDelegationRequired
                ? loginUser.getRealName()
                : StringUtils.hasText(requestUserName)
                        ? requestUserName
                        : loginUser != null ? loginUser.getRealName() : null;
        String deptId = trustedDelegationRequired
                ? loginUser.getActiveOrgId().toString()
                : StringUtils.hasText(requestDeptId)
                        ? requestDeptId
                        : loginUser != null && loginUser.getMainOrgId() != null
                                ? String.valueOf(loginUser.getMainOrgId()) : null;
        String deptName = trustedDelegationRequired ? loginUser.getActiveOrgName() : requestDeptName;

        if (!StringUtils.hasText(deptName) && StringUtils.hasText(userId) && flowOrgIntegrationService != null) {
            deptName = flowOrgIntegrationService.getUserDeptName(userId);
        }
        
        log.info("启动流程: modelKey={}, userId={}, userName={}, deptId={}, deptName={}",
                modelKey, userId, userName, deptId, deptName);
        
        // 如果没有 businessKey，自动生成一个
        if (businessKey == null || businessKey.isEmpty()) {
            businessKey = UUID.randomUUID().toString();
        }
        
        // 如果没有 businessType，使用 modelKey 作为默认值
        if (businessType == null || businessType.isEmpty()) {
            businessType = modelKey;
        }
        
        // 如果没有 title，使用默认标题
        if (title == null || title.isEmpty()) {
            title = modelKey + "流程";
        }
        
        String processInstanceId = flowInstanceService.startProcess(
                modelKey, businessKey, businessType, title, variables,
                userId, userName, deptId, deptName);
        
        return RespInfo.success("流程发起成功", processInstanceId);
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 获取流程状态
     */
    @GetMapping("/status/{businessKey}")
    public RespInfo<FlowBusiness> getStatus(@PathVariable String businessKey) {
        FlowBusiness business = flowInstanceService.getProcessStatus(businessKey);
        return RespInfo.success(business);
    }

    /**
     * 终止流程
     */
    @PostMapping("/terminate/{businessKey}")
    public RespInfo<Void> terminate(
            @PathVariable String businessKey,
            @RequestBody Map<String, Object> params) {
        
        String userId = (String) params.get("userId");
        String reason = (String) params.get("reason");
        
        flowInstanceService.terminateProcess(businessKey, userId, reason);
        
        return RespInfo.success("流程已终止", null);
    }

    /**
     * 删除流程实例
     */
    @DeleteMapping("/{businessKey}")
    public RespInfo<Void> delete(
            @PathVariable String businessKey,
            @RequestParam String userId) {
        
        flowInstanceService.deleteProcess(businessKey, userId);
        
        return RespInfo.success("删除成功", null);
    }

    /**
     * 获取流程变量
     */
    @GetMapping("/variables/{businessKey}")
    public RespInfo<Map<String, Object>> getVariables(@PathVariable String businessKey) {
        Map<String, Object> variables = flowInstanceService.getProcessVariables(businessKey);
        return RespInfo.success(variables);
    }

    /**
     * 更新流程变量
     */
    @PutMapping("/variables/{businessKey}")
    public RespInfo<Void> updateVariables(
            @PathVariable String businessKey,
            @RequestBody Map<String, Object> variables) {
        
        flowInstanceService.updateProcessVariables(businessKey, variables);
        
        return RespInfo.success("更新成功", null);
    }

    /**
     * 分页查询流程实例列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param processDefKey 流程定义Key（可选）
     * @param status 状态（可选）
     * @param title 标题（模糊查询，可选）
     * @param applyUserId 申请人ID（可选）
     */
    @GetMapping("/page")
    public RespInfo<IPage<FlowBusiness>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String processDefKey,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String applyUserId) {
        
        Page<FlowBusiness> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
        
        wrapper.eq(processDefKey != null && !processDefKey.isEmpty(),
                FlowBusiness::getProcessDefKey, processDefKey)
               .eq(status != null && !status.isEmpty(),
                FlowBusiness::getStatus, status)
               .like(title != null && !title.isEmpty(),
                FlowBusiness::getTitle, title)
               .eq(applyUserId != null && !applyUserId.isEmpty(),
                FlowBusiness::getApplyUserId, applyUserId)
               .orderByDesc(FlowBusiness::getCreateTime);
        
        IPage<FlowBusiness> result = flowBusinessMapper.selectPage(page, wrapper);
        return RespInfo.success(result);
    }

    /**
     * 根据流程实例ID获取流程详情
     */
    @GetMapping("/detail/{processInstanceId}")
    public RespInfo<FlowBusiness> getByProcessInstanceId(@PathVariable String processInstanceId) {
        LambdaQueryWrapper<FlowBusiness> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowBusiness::getProcessInstanceId, processInstanceId);
        FlowBusiness business = flowBusinessMapper.selectOne(wrapper);
        return RespInfo.success(business);
    }
}
