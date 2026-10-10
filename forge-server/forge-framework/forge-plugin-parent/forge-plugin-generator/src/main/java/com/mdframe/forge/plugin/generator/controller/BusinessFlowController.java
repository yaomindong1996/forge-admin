package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowCallbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowResubmitDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowStartDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFlowWithdrawDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskActionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormContextQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessTaskFormSaveDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessFlowStatusFieldService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessStartableObjectService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFieldVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessBindingSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowBindingVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFlowRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessTaskFormContextVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 业务流程配置接口。
 */
@Slf4j
@RestController
@RequestMapping("/ai/business/flow")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessFlowController {

    private final BusinessFlowService flowService;
    private final BusinessFlowStatusFieldService flowStatusFieldService;
    private final BusinessStartableObjectService startableObjectService;

    @GetMapping("/binding/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询业务流程绑定")
    public RespInfo<BusinessFlowBindingVO> getBinding(@PathVariable String objectCode) {
        return RespInfo.success(flowService.getFlowBinding(objectCode));
    }

    /** 业务发起页获取发起人自选审批节点，不暴露设计态配置权限。 */
    @GetMapping("/start-config/{objectCode}")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询业务流程发起配置")
    public RespInfo<Map<String, Object>> getStartConfig(@PathVariable String objectCode) {
        return RespInfo.success(flowService.getBusinessStartConfig(objectCode));
    }

    /** 移动端发起审批目录，普通员工只需登录；前端再与菜单取交集。 */
    @GetMapping("/startable-objects")
    @ApiPermissionIgnore
    public RespInfo<List<BusinessStartableObjectVO>> startableObjects() {
        return RespInfo.success(startableObjectService.listStartableObjects());
    }

    @GetMapping("/model/{modelKey}/variables")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询流程变量候选项")
    public RespInfo<Map<String, Object>> variables(@PathVariable String modelKey,
                                                   @RequestParam(required = false) String objectCode) {
        return RespInfo.success(flowService.getVariableCandidates(modelKey, objectCode));
    }

    @GetMapping("/model/{modelKey}/business-bindings")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "按流程模型查询业务绑定")
    public RespInfo<List<BusinessBindingSummaryVO>> listBusinessBindingsByModelKey(@PathVariable String modelKey) {
        return RespInfo.success(flowService.listBusinessBindingsByModelKey(modelKey));
    }

    @GetMapping("/form-assets/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询业务流程表单资产")
    public RespInfo<Map<String, Object>> formAssets(@PathVariable String objectCode,
                                                    @RequestParam(defaultValue = "false") Boolean includeInternal,
                                                    @RequestParam(required = false) Long applicationId) {
        return RespInfo.success(flowService.getFormAssets(
                objectCode, Boolean.TRUE.equals(includeInternal), applicationId));
    }

    @PostMapping("/status-field/{objectId}/ensure")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "补齐流程状态字段与数据库列")
    public RespInfo<BusinessFieldVO> ensureStatusField(@PathVariable Long objectId) {
        return RespInfo.success(flowStatusFieldService.ensure(objectId));
    }

    // 以下为待办审批人运行时接口：审批人多为企微免登普通员工，不持有设计态权限 ai:businessFlow:view，
    // 故仅要求登录态，越权防护依赖 BusinessFlowService 的任务归属校验（loadFlowTaskDetail + 字段一致性断言）。
    @GetMapping("/task-form-context")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询业务待办表单上下文")
    public RespInfo<BusinessTaskFormContextVO> taskFormContext(BusinessTaskFormContextQueryDTO query) {
        return RespInfo.success(flowService.getTaskFormContext(query));
    }

    @GetMapping("/task-form-context/readonly")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询业务历史表单上下文")
    public RespInfo<BusinessTaskFormContextVO> taskFormReadonlyContext(BusinessTaskFormContextQueryDTO query) {
        return RespInfo.success(flowService.getTaskFormReadonlyContext(query));
    }

    @PutMapping("/task-form-context")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "保存业务待办表单字段")
    public RespInfo<BusinessTaskFormContextVO> saveTaskFormContext(@RequestBody BusinessTaskFormSaveDTO dto) {
        return RespInfo.success(flowService.saveTaskFormContext(dto));
    }

    @PostMapping("/task-action")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "办理业务待办任务")
    public RespInfo<BusinessFlowRuntimeVO> completeTask(@RequestBody BusinessTaskActionDTO dto) {
        return RespInfo.success(flowService.completeBusinessTask(dto));
    }

    @PutMapping("/binding/{objectCode}")
    @SaCheckPermission("ai:businessFlow:config")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "保存业务流程绑定")
    public RespInfo<Void> saveBinding(@PathVariable String objectCode,
                                      @RequestBody BusinessFlowBindingDTO dto) {
        flowService.saveFlowBinding(objectCode, dto);
        return RespInfo.success();
    }

    @PostMapping("/start")
    @SaCheckPermission("ai:businessFlow:start")
    @OperationLog(module = "业务流程", type = OperationType.ADD, desc = "发起业务流程")
    public RespInfo<BusinessFlowRuntimeVO> start(@RequestBody BusinessFlowStartDTO dto) {
        return RespInfo.success(flowService.startDocumentFlow(dto));
    }

    @PostMapping("/resubmit")
    @SaCheckPermission("ai:businessFlow:start")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "驳回修改后重提")
    public RespInfo<BusinessFlowRuntimeVO> resubmit(@RequestBody BusinessFlowResubmitDTO dto) {
        return RespInfo.success(flowService.resubmit(dto));
    }

    @PostMapping("/withdraw")
    @SaCheckPermission("ai:businessDocument:withdraw")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "撤回业务流程")
    public RespInfo<BusinessFlowRuntimeVO> withdraw(@RequestBody BusinessFlowWithdrawDTO dto) {
        return RespInfo.success(flowService.withdrawDocumentFlow(dto));
    }

    @GetMapping("/status/{objectCode}/{recordId}")
    @SaCheckPermission("ai:businessFlow:view")
    @OperationLog(module = "业务流程", type = OperationType.QUERY, desc = "查询业务流程状态")
    public RespInfo<BusinessFlowRuntimeVO> status(@PathVariable String objectCode,
                                                 @PathVariable Long recordId) {
        return RespInfo.success(flowService.getFlowStatus(objectCode, recordId));
    }

    @PostMapping("/callback")
    @SaCheckPermission("ai:businessFlow:callback")
    @OperationLog(module = "业务流程", type = OperationType.UPDATE, desc = "处理业务流程回调")
    public RespInfo<Void> callback(@RequestBody BusinessFlowCallbackDTO dto) {
        flowService.handleFlowCallback(dto);
        return RespInfo.success();
    }
}
