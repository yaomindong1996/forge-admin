package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.flow.identity.FlowSessionIdentity;
import com.mdframe.forge.flow.dto.FlowTaskActionDTO;
import com.mdframe.forge.flow.dto.FlowTaskApproveDTO;
import com.mdframe.forge.flow.dto.FlowTaskDelegateDTO;
import com.mdframe.forge.flow.dto.FlowTaskRejectDTO;
import com.mdframe.forge.flow.dto.FlowTaskWithdrawDTO;
import com.mdframe.forge.flow.dto.FlowTaskReassignDTO;
import com.mdframe.forge.flow.dto.FlowTaskSignDTO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.ProcessDiagramInfo;
import com.mdframe.forge.starter.flow.dto.TaskFormInfo;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.vo.FlowHistoryPageVO;
import com.mdframe.forge.starter.flow.vo.FlowTaskSignRelationVO;
import com.mdframe.forge.starter.flow.service.FlowOverdueReminderService;
import com.mdframe.forge.starter.flow.service.FlowTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程任务接口（我的待办/已办/我发起的）
 */
@RestController
@RequestMapping("/api/flow/task")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowTaskController {

    private final FlowTaskService flowTaskService;
    private final FlowOverdueReminderService flowOverdueReminderService;

    /**
     * 我的待办任务
     */
    @GetMapping("/todo")
    public RespInfo<IPage<FlowTask>> todo(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        String trustedUserId = FlowSessionIdentity.requireUserId(userId);
        Page<FlowTask> page = FlowSessionIdentity.page(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.todoTasks(page, trustedUserId, title, category, status);
        return RespInfo.success(result);
    }

    /**
     * 我的已办任务
     */
    @GetMapping("/done")
    public RespInfo<IPage<FlowTask>> done(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        String trustedUserId = FlowSessionIdentity.requireUserId(userId);
        Page<FlowTask> page = FlowSessionIdentity.page(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.doneTasks(page, trustedUserId, title, category, status);
        return RespInfo.success(result);
    }

    /**
     * 我发起的流程
     */
    @GetMapping("/started")
    public RespInfo<IPage<FlowTask>> started(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        String trustedUserId = FlowSessionIdentity.requireUserId(userId);
        Page<FlowTask> page = FlowSessionIdentity.page(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.startedTasks(page, trustedUserId, title, category, status);
        return RespInfo.success(result);
    }

    /**
     * 候选任务（未签收的任务）
     */
    @GetMapping("/candidate")
    public RespInfo<IPage<FlowTask>> candidateTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String title) {
        String trustedUserId = FlowSessionIdentity.requireUserId(userId);
        Page<FlowTask> page = FlowSessionIdentity.page(pageNum, pageSize);
        IPage<FlowTask> result = flowTaskService.candidateTasks(page, trustedUserId, groupId, title);
        return RespInfo.success(result);
    }

    /**
     * 签收任务
     */
    @PostMapping("/claim")
    @ApiPermissionIgnore
    public RespInfo<Void> claim(@RequestParam String taskId, @RequestParam(required = false) String userId) {
        flowTaskService.claimTask(taskId, FlowSessionIdentity.requireUserId(userId));
        return RespInfo.success("签收成功", null);
    }

    /**
     * 审批通过
     */
    @PostMapping("/approve")
    @ApiPermissionIgnore
    public RespInfo<Void> approve(@RequestBody FlowTaskApproveDTO dto) {
        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        Long tenantId = resolveTrustedTenant(dto.getTenantId());
        flowTaskService.approve(dto.getTaskId(), userId, optionalText(dto.getComment()),
                optionalText(dto.getSignature()), dto.getVariables(),
                tenantId, optionalText(dto.getIdempotencyKey()), optionalText(dto.getRequestDigest()),
                dto.getApprovalPointResults());
        return RespInfo.success("审批通过", null);
    }

    /**
     * 审批驳回
     */
    @PostMapping("/reject")
    @ApiPermissionIgnore
    public RespInfo<Void> reject(@RequestBody FlowTaskRejectDTO dto) {
        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        Long tenantId = resolveTrustedTenant(dto.getTenantId());
        flowTaskService.reject(dto.getTaskId(), userId, optionalText(dto.getComment()),
                optionalText(dto.getSignature()), tenantId,
                optionalText(dto.getIdempotencyKey()), optionalText(dto.getRequestDigest()));
        return RespInfo.success("已驳回", null);
    }

    /**
     * 驳回至流程设计中标记的发起人修改路径。目标节点由 BPMN 回路决定，
     * 本接口只保留动作语义并写入 rejectToStart 流程变量。
     */
    @PostMapping("/reject-to-start")
    @ApiPermissionIgnore
    public RespInfo<Void> rejectToStart(@RequestBody FlowTaskRejectDTO dto) {
        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        Long tenantId = resolveTrustedTenant(dto.getTenantId());
        flowTaskService.rejectToStart(dto.getTaskId(), userId, optionalText(dto.getComment()),
                optionalText(dto.getSignature()), tenantId,
                optionalText(dto.getIdempotencyKey()), optionalText(dto.getRequestDigest()));
        return RespInfo.success("已驳回至发起人修改路径", null);
    }

    private Long resolveTrustedTenant(Long requestedTenant) {
        Long sessionTenant = SessionHelper.getTenantId();
        if (sessionTenant == null || sessionTenant <= 0) {
            throw new IllegalArgumentException("FLOW_TASK_TENANT_REQUIRED");
        }
        if (requestedTenant != null && !sessionTenant.equals(requestedTenant)) {
            throw new IllegalArgumentException("FLOW_TASK_TENANT_MISMATCH");
        }
        return sessionTenant;
    }

    private String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private boolean isBlankId(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 转办
     */
    @PostMapping("/delegate")
    @ApiPermissionIgnore
    public RespInfo<Void> delegate(@RequestBody FlowTaskDelegateDTO dto) {
        if (isBlankId(dto.getTaskId())) {
            return RespInfo.error("任务ID不能为空");
        }
        if (isBlankId(dto.getTargetUserId())) {
            return RespInfo.error("转办人ID不能为空");
        }

        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        Long tenantId = resolveTrustedTenant(null);
        flowTaskService.delegate(dto.getTaskId(), userId, dto.getTargetUserId(),
                optionalText(dto.getComment()), optionalText(dto.getSignature()), tenantId,
                optionalText(dto.getIdempotencyKey()), optionalText(dto.getRequestDigest()));
        return RespInfo.success("转办成功", null);
    }

    /** 动态加签：增加当前任务的候选办理人。 */
    @PostMapping("/add-sign")
    public RespInfo<Void> addSign(@RequestBody FlowTaskSignDTO dto) {
        if (isBlankId(dto.getTaskId()) || isBlankId(dto.getTargetUserId())) {
            return RespInfo.error("任务ID和目标用户不能为空");
        }
        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        Long tenantId = resolveTrustedTenant(null);
        flowTaskService.addSign(dto.getTaskId(), userId, dto.getTargetUserId(), optionalText(dto.getComment()),
                optionalText(dto.getSignMode()), tenantId, optionalText(dto.getIdempotencyKey()),
                optionalText(dto.getRequestDigest()));
        return RespInfo.success("加签成功", null);
    }

    /** 动态减签：移除当前任务的候选办理人。 */
    @PostMapping("/reduce-sign")
    public RespInfo<Void> reduceSign(@RequestBody FlowTaskSignDTO dto) {
        if (isBlankId(dto.getTaskId()) || isBlankId(dto.getTargetUserId())) {
            return RespInfo.error("任务ID和目标用户不能为空");
        }
        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        Long tenantId = resolveTrustedTenant(null);
        flowTaskService.reduceSign(dto.getTaskId(), userId, dto.getTargetUserId(), optionalText(dto.getComment()),
                optionalText(dto.getSignMode()), tenantId, optionalText(dto.getIdempotencyKey()),
                optionalText(dto.getRequestDigest()));
        return RespInfo.success("减签成功", null);
    }

    /** 查询当前任务的动态加签关系及撤销状态。 */
    @GetMapping("/{taskId}/sign-relations")
    public RespInfo<List<FlowTaskSignRelationVO>> signRelations(
            @PathVariable String taskId,
            @RequestParam(required = false) String userId) {
        return RespInfo.success(flowTaskService.getSignRelations(taskId, FlowSessionIdentity.requireUserId(userId)));
    }

    /**
     * 退回上一审批节点
     */
    @PostMapping("/return")
    public RespInfo<Void> returnTask(@RequestBody FlowTaskActionDTO dto) {
        if (isBlankId(dto.getTaskId())) {
            return RespInfo.error("任务ID不能为空");
        }

        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        flowTaskService.returnTask(dto.getTaskId(), userId,
                optionalText(dto.getComment()), optionalText(dto.getSignature()),
                optionalText(dto.getTargetActivityId()));
        return RespInfo.success("已退回", null);
    }

    /**
     * 流程发起人、当前处理人或任务拥有人改派当前任务。
     */
    @PostMapping("/reassign")
    @ApiPermissionIgnore
    public RespInfo<Void> reassign(@RequestBody FlowTaskReassignDTO dto) {
        if (isBlankId(dto.getTaskId())) {
            return RespInfo.error("任务ID不能为空");
        }
        if (isBlankId(dto.getNewAssignee())) {
            return RespInfo.error("新处理人ID不能为空");
        }
        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        flowTaskService.reassignByInitiator(dto.getTaskId(), userId,
                dto.getNewAssignee(), optionalText(dto.getReason()));
        return RespInfo.success("任务已改派", null);
    }

    /**
     * 终结流程
     */
    @PostMapping("/terminate")
    public RespInfo<Void> terminateTask(@RequestBody FlowTaskActionDTO dto) {
        if (isBlankId(dto.getTaskId())) {
            return RespInfo.error("任务ID不能为空");
        }

        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        flowTaskService.terminateTask(dto.getTaskId(), userId,
                optionalText(dto.getComment()), optionalText(dto.getSignature()));
        return RespInfo.success("流程已终结", null);
    }

    /**
     * 撤回流程
     */
    @PostMapping("/withdraw")
    public RespInfo<Void> withdraw(@RequestBody FlowTaskWithdrawDTO dto) {
        if (isBlankId(dto.getProcessInstanceId())) {
            return RespInfo.error("流程实例ID不能为空");
        }

        String userId = FlowSessionIdentity.requireUserId(dto.getUserId());
        flowTaskService.withdraw(dto.getProcessInstanceId(), userId);
        return RespInfo.success("撤回成功", null);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    @ApiPermissionIgnore
    public RespInfo<FlowTask> getById(@PathVariable String taskId) {
        FlowTask task = flowTaskService.getTaskDetail(taskId);
        return RespInfo.success(task);
    }

    /**
     * 获取流程图（高亮当前节点）
     */
    @GetMapping("/diagram/{processInstanceId}")
    @ApiPermissionIgnore
    public ResponseEntity<byte[]> getProcessDiagram(@PathVariable String processInstanceId) {
        byte[] diagram = flowTaskService.getProcessDiagram(processInstanceId);
        
        if (diagram == null || diagram.length == 0) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(diagram);
    }

    /**
     * 获取流程图详情（包含节点信息，用于交互式展示）
     */
    @GetMapping("/diagram-info/{processInstanceId}")
    @ApiPermissionIgnore
    public RespInfo<ProcessDiagramInfo> getProcessDiagramInfo(
            @PathVariable String processInstanceId,
            @RequestParam(defaultValue = "false") boolean includeImage) {
        ProcessDiagramInfo diagramInfo = flowTaskService.getProcessDiagramInfo(processInstanceId, includeImage);
        if (diagramInfo == null) {
            return RespInfo.error("流程图信息不存在");
        }
        return RespInfo.success(diagramInfo);
    }

    /**
     * 催办
     */
    @PostMapping("/remind")
    @SaCheckPermission("flow:task:remind")
    public RespInfo<Void> remind(@RequestParam String taskId) {
        flowTaskService.remind(taskId);
        return RespInfo.success("催办成功", null);
    }

    /**
     * 手动触发逾期提醒扫描。
     */
    @PostMapping("/overdue-reminder/scan")
    @SaCheckPermission("flow:task:remind")
    public RespInfo<Void> scanOverdueReminders() {
        flowOverdueReminderService.scanAndSendOverdueReminders();
        return RespInfo.success("逾期提醒扫描已触发", null);
    }

    /**
     * 获取流程审批时间轴
     * 按时间顺序返回审批节点，包含发起、审批、驳回、转办等操作
     */
    @GetMapping("/history/{processInstanceId}")
    @ApiPermissionIgnore
    public RespInfo<List<Map<String, Object>>> getProcessHistory(@PathVariable String processInstanceId) {
        List<Map<String, Object>> history = flowTaskService.getProcessHistory(processInstanceId);
        return RespInfo.success(history);
    }

    /** 分页获取类型化审批时间轴，旧 history 接口继续保留兼容。 */
    @GetMapping("/history/{processInstanceId}/page")
    @ApiPermissionIgnore
    public RespInfo<FlowHistoryPageVO> getProcessHistoryPage(
            @PathVariable String processInstanceId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return RespInfo.success(flowTaskService.getProcessHistoryPage(processInstanceId, pageNum, pageSize));
    }

    /**
     * 获取任务表单信息
     * 包含表单类型、表单配置、流程变量等
     */
    @GetMapping("/form/{taskId}")
    @ApiPermissionIgnore
    public RespInfo<TaskFormInfo> getTaskFormInfo(@PathVariable String taskId) {
        TaskFormInfo formInfo = flowTaskService.getTaskFormInfo(taskId);
        return RespInfo.success(formInfo);
    }

    /**
     * 获取流程关联表单信息。
     * 用于已办、抄送、流程历史等没有运行中任务的只读查看场景。
     */
    @GetMapping("/form")
    @ApiPermissionIgnore
    public RespInfo<TaskFormInfo> getProcessFormInfo(@RequestParam(required = false) String processInstanceId,
                                                     @RequestParam(required = false) String businessKey,
                                                     @RequestParam(required = false) String processDefKey,
                                                     @RequestParam(required = false) String taskId,
                                                     @RequestParam(required = false) String taskDefKey) {
        TaskFormInfo formInfo = flowTaskService.getProcessFormInfo(
                processInstanceId, businessKey, processDefKey, taskId, taskDefKey);
        return RespInfo.success(formInfo);
    }
}
