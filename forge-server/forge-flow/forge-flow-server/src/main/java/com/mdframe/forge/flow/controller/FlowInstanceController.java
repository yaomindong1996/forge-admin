package com.mdframe.forge.flow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.flow.dto.FlowInstanceStartDTO;
import com.mdframe.forge.flow.dto.FlowInstanceTerminateDTO;
import com.mdframe.forge.flow.dto.FlowVariablesUpdateDTO;
import com.mdframe.forge.flow.identity.FlowSessionIdentity;
import com.mdframe.forge.starter.auth.config.FlowDelegationSessionVerifier;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.service.FlowInstanceService;
import com.mdframe.forge.starter.flow.service.FlowMonitorService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    private final FlowMonitorService flowMonitorService;
    private final FlowOrgIntegrationService flowOrgIntegrationService;
    private final FlowDelegationSessionVerifier flowDelegationSessionVerifier;

    /**
     * 发起流程
     */
    @PostMapping("/start/{modelKey}")
    public RespInfo<String> start(
            @PathVariable String modelKey,
            @RequestBody FlowInstanceStartDTO dto) {
        return startInternal(modelKey, dto, false);
    }

    /**
     * 用户委托流程发起入口。发起人、租户和当前组织只从已验证 Session 解析，
     * 不接受请求体中的代办身份。
     */
    @PostMapping("/start-delegated/{modelKey}")
    @SaCheckPermission("ai:businessFlow:start")
    public RespInfo<String> startDelegated(
            @PathVariable String modelKey,
            @RequestBody FlowInstanceStartDTO dto) {
        flowDelegationSessionVerifier.requireTrustedDelegation();
        return startInternal(modelKey, dto, true);
    }

    /**
     * 平台 R3 高风险审批专用委托入口。
     */
    @PostMapping("/start-delegated-approval/{modelKey}")
    @SaCheckPermission("ai:capability:approval:submit")
    public RespInfo<String> startDelegatedApproval(
            @PathVariable String modelKey,
            @RequestBody FlowInstanceStartDTO dto) {
        flowDelegationSessionVerifier.requireTrustedDelegation();
        return startInternal(modelKey, dto, true);
    }

    private RespInfo<String> startInternal(
            String modelKey,
            FlowInstanceStartDTO dto,
            boolean trustedDelegationRequired) {
        String businessKey = dto.getBusinessKey();
        String businessType = dto.getBusinessType();
        String title = dto.getTitle();
        Map<String, Object> variables = dto.resolveVariables();

        // FlowClient 会显式传入业务发起人；Session 仅作为人工从流程中心发起时的兜底。
        String requestUserId = toText(dto.getUserId());
        String requestUserName = toText(dto.getUserName());
        String requestDeptId = toText(dto.getDeptId());
        String requestDeptName = toText(dto.getDeptName());
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

    private String toText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
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
            @RequestBody FlowInstanceTerminateDTO dto) {
        String userId = FlowSessionIdentity.requireUserId(dto == null ? null : dto.getUserId());
        flowInstanceService.terminateProcess(businessKey, userId, dto == null ? null : dto.getReason());
        return RespInfo.success("流程已终止", null);
    }

    /**
     * 删除流程实例
     */
    @DeleteMapping("/{businessKey}")
    public RespInfo<Void> delete(
            @PathVariable String businessKey,
            @RequestParam(required = false) String userId) {
        flowInstanceService.deleteProcess(businessKey, FlowSessionIdentity.requireUserId(userId));
        
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
            @RequestBody FlowVariablesUpdateDTO request) {
        
        flowInstanceService.updateProcessVariables(businessKey, request.getVariables());
        
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
        
        return RespInfo.success(flowMonitorService.getBusinessPage(
                pageNum, pageSize, processDefKey, status, title, applyUserId));
    }

    /**
     * 根据流程实例ID获取流程详情
     */
    @GetMapping("/detail/{processInstanceId}")
    public RespInfo<FlowBusiness> getByProcessInstanceId(@PathVariable String processInstanceId) {
        return RespInfo.success(flowMonitorService.getBusinessByProcessInstanceId(processInstanceId));
    }
}
