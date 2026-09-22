package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationAiAssistantChatDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationAiAssistantConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDataScopeAdapterDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDistributionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationFormDataProvisionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPageDesignDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPublishDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPortalConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationRolePermissionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationRollbackDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationTemplateInitializeDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAiAppGenerateResult;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeCodegenRequest;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationAiAssistantService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationAiInitializeService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationCodegenService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationFormDataService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationObjectService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationPageDesignService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationPermissionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationPublishRecoveryService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationPublishRunService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationPublishService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationRollbackService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationRuntimeService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationTemplateService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationVersionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationWorkspaceService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAiAssistantReplyVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationAiInitializeResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationCreateVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationFormDataVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPageDesignVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPermissionWorkspaceVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishCheckVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationPublishRunVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationReadinessVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationRolePermissionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationRuntimeVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationTemplateResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVersionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationWorkspaceVO;
import com.mdframe.forge.plugin.generator.vo.lowcode.LowcodeCodePreviewVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 业务应用聚合接口。
 */
@RestController
@RequestMapping("/ai/business/application")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessApplicationController {

    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessApplicationFormDataService formDataService;
    private final BusinessApplicationPageDesignService pageDesignService;
    private final BusinessApplicationTemplateService templateService;
    private final BusinessApplicationAiInitializeService aiInitializeService;
    private final BusinessApplicationAiAssistantService aiAssistantService;
    private final BusinessApplicationWorkspaceService workspaceService;
    private final BusinessApplicationPermissionService permissionService;
    private final BusinessApplicationPublishService publishService;
    private final BusinessApplicationVersionService versionService;
    private final BusinessApplicationPublishRunService publishRunService;
    private final BusinessApplicationPublishRecoveryService recoveryService;
    private final BusinessApplicationRollbackService rollbackService;
    private final BusinessApplicationRuntimeService runtimeService;
    private final BusinessApplicationCodegenService codegenService;

    @GetMapping("/page")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "分页查询业务应用")
    public RespInfo<Page<BusinessApplicationVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            BusinessApplicationQueryDTO query) {
        return RespInfo.success(applicationService.page(pageNum, pageSize, query));
    }

    @GetMapping("/list")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询业务应用列表")
    public RespInfo<List<BusinessApplicationVO>> list(BusinessApplicationQueryDTO query) {
        return RespInfo.success(applicationService.list(query));
    }

    @GetMapping("/workbench")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询当前用户工作台应用")
    public RespInfo<List<BusinessApplicationVO>> workbenchApplications() {
        return RespInfo.success(runtimeService.workbenchApplications());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询业务应用详情")
    public RespInfo<BusinessApplicationVO> detail(@PathVariable Long id) {
        return RespInfo.success(applicationService.detail(id));
    }

    @GetMapping("/by-code/{applicationCode}")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "按编码查询业务应用详情")
    public RespInfo<BusinessApplicationVO> detailByCode(@PathVariable String applicationCode) {
        return RespInfo.success(applicationService.detailByCode(applicationCode));
    }

    @GetMapping("/by-slug/{portalSlug}")
    @SaCheckPermission("ai:businessApplication:portal")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "按门户地址查询业务应用")
    public RespInfo<BusinessApplicationVO> detailBySlug(@PathVariable String portalSlug) {
        return RespInfo.success(runtimeService.runtimeByCodeOrSlug(portalSlug).getApplication());
    }

    @GetMapping("/slug-available")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "校验应用门户地址")
    public RespInfo<Boolean> slugAvailable(
            @RequestParam String portalSlug,
            @RequestParam(required = false) Long excludeId) {
        return RespInfo.success(applicationService.slugAvailable(portalSlug, excludeId));
    }

    @PutMapping("/{id}/portal-config")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "保存应用门户配置")
    public RespInfo<Void> savePortalConfig(
            @PathVariable Long id,
            @RequestBody BusinessApplicationPortalConfigDTO dto) {
        applicationService.savePortalConfig(id, dto);
        return RespInfo.success();
    }

    @PutMapping("/{id}/ai-assistant-config")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "保存应用 AI 助理配置")
    public RespInfo<Void> saveAiAssistantConfig(
            @PathVariable Long id,
            @RequestBody BusinessApplicationAiAssistantConfigDTO dto) {
        applicationService.saveAiAssistantConfig(id, dto);
        return RespInfo.success();
    }

    @GetMapping("/{id}/ai-assistant-status")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用 AI 助理状态")
    public RespInfo<Map<String, Object>> aiAssistantStatus(@PathVariable Long id) {
        return RespInfo.success(aiAssistantService.status(id));
    }

    @PostMapping("/portal/{applicationCodeOrSlug}/assistant/chat")
    @SaCheckPermission("ai:businessApplication:portal")
    public RespInfo<BusinessApplicationAiAssistantReplyVO> chatWithPortalAssistant(
            @PathVariable String applicationCodeOrSlug,
            @RequestBody BusinessApplicationAiAssistantChatDTO request) {
        return RespInfo.success(aiAssistantService.chat(applicationCodeOrSlug, request));
    }

    @PostMapping("/{id}/distribute/workbench")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "分发业务应用")
    public RespInfo<Map<String, Object>> distribute(
            @PathVariable Long id,
            @RequestBody BusinessApplicationDistributionDTO dto) {
        return RespInfo.success(applicationService.distribute(id, dto));
    }

    @GetMapping("/by-code/{applicationCode}/workspace")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "按编码查询应用工作台快照")
    public RespInfo<BusinessApplicationWorkspaceVO> workspaceByCode(@PathVariable String applicationCode) {
        return RespInfo.success(workspaceService.workspaceByCode(applicationCode));
    }

    @GetMapping("/by-code/{applicationCode}/permissions")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用权限目录")
    public RespInfo<BusinessApplicationPermissionWorkspaceVO> permissionWorkspace(
            @PathVariable String applicationCode) {
        return RespInfo.success(permissionService.workspace(applicationCode));
    }

    @GetMapping("/by-code/{applicationCode}/permissions/roles/{roleId}")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用角色权限")
    public RespInfo<BusinessApplicationRolePermissionVO> rolePermission(
            @PathVariable String applicationCode, @PathVariable Long roleId) {
        return RespInfo.success(permissionService.rolePermission(applicationCode, roleId));
    }

    @PutMapping("/by-code/{applicationCode}/permissions/roles/{roleId}")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "保存应用角色权限")
    public RespInfo<BusinessApplicationRolePermissionVO> saveRolePermission(
            @PathVariable String applicationCode,
            @PathVariable Long roleId,
            @RequestBody BusinessApplicationRolePermissionDTO request) {
        return RespInfo.success(permissionService.saveRolePermission(applicationCode, roleId, request));
    }

    @PutMapping("/by-code/{applicationCode}/permissions/objects/{objectId}/data-scope-adapter")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "保存对象数据范围适配")
    public RespInfo<BusinessApplicationPermissionWorkspaceVO.ObjectPermission> saveDataScopeAdapter(
            @PathVariable String applicationCode,
            @PathVariable Long objectId,
            @RequestBody BusinessApplicationDataScopeAdapterDTO request) {
        return RespInfo.success(permissionService.saveDataScopeAdapter(applicationCode, objectId, request));
    }

    @GetMapping("/by-code/{applicationCode}/runtime")
    @SaCheckPermission("ai:businessApplication:runtime")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询已发布应用运行配置")
    public RespInfo<BusinessApplicationRuntimeVO> runtimeByCode(@PathVariable String applicationCode) {
        return RespInfo.success(runtimeService.runtimeByCode(applicationCode));
    }

    @GetMapping("/portal/{applicationCodeOrSlug}/runtime")
    @SaCheckPermission("ai:businessApplication:portal")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用门户运行配置")
    public RespInfo<BusinessApplicationRuntimeVO> portalRuntime(
            @PathVariable String applicationCodeOrSlug) {
        return RespInfo.success(runtimeService.runtimeByCodeOrSlug(applicationCodeOrSlug));
    }

    @GetMapping("/{id}/workspace")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用工作台摘要")
    public RespInfo<BusinessApplicationWorkspaceVO> workspace(@PathVariable Long id) {
        return RespInfo.success(workspaceService.workspace(id));
    }

    @GetMapping("/{id}/readiness")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用就绪度")
    public RespInfo<BusinessApplicationReadinessVO> readiness(@PathVariable Long id) {
        return RespInfo.success(workspaceService.readiness(id));
    }

    @GetMapping("/{id}/code/options")
    @SaCheckPermission("ai:businessApplication:code")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用代码包设置")
    public RespInfo<Map<String, Object>> codeOptions(@PathVariable Long id) {
        return RespInfo.success(codegenService.getOptions(id));
    }

    @PutMapping("/{id}/code/options")
    @SaCheckPermission("ai:businessApplication:code")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "保存应用代码包设置")
    public RespInfo<Void> saveCodeOptions(
            @PathVariable Long id, @RequestBody LowcodeCodegenRequest request) {
        codegenService.saveOptions(id, request);
        return RespInfo.success();
    }

    @GetMapping("/{id}/code/preview")
    @SaCheckPermission("ai:businessApplication:codePreview")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "预览应用完整代码")
    public RespInfo<LowcodeCodePreviewVO> previewCode(
            @PathVariable Long id, LowcodeCodegenRequest request) {
        return RespInfo.success(codegenService.previewCode(id, request));
    }

    @GetMapping("/{id}/code/download")
    @SaCheckPermission("ai:businessApplication:codeDownload")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "下载应用完整代码")
    public void downloadCode(@PathVariable Long id,
                             LowcodeCodegenRequest request,
                             HttpServletResponse response) throws Exception {
        byte[] zipBytes = codegenService.downloadCode(id, request);
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + codegenService.resolveDownloadFilename(id) + "\"");
        response.setContentLength(zipBytes.length);
        response.getOutputStream().write(zipBytes);
        response.getOutputStream().flush();
    }

    @PostMapping
    @SaCheckPermission("ai:businessApplication:add")
    @OperationLog(module = "业务应用", type = OperationType.ADD, desc = "新增业务应用")
    public RespInfo<BusinessApplicationCreateVO> create(@RequestBody BusinessApplicationDTO dto) {
        return RespInfo.success(applicationService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "修改业务应用")
    public RespInfo<Void> update(@RequestBody BusinessApplicationDTO dto) {
        applicationService.update(dto);
        if (dto != null && dto.getId() != null) {
            applicationObjectService.detachOrphanPageFormObjects(dto.getId());
        }
        return RespInfo.success();
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("ai:businessApplication:status")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "启停业务应用")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        applicationService.updateStatus(id, status);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessApplication:delete")
    @OperationLog(module = "业务应用", type = OperationType.DELETE, desc = "删除业务应用")
    public RespInfo<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return RespInfo.success();
    }

    @GetMapping("/{id}/objects")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用业务对象")
    public RespInfo<List<BusinessApplicationObjectVO>> listObjects(@PathVariable Long id) {
        return RespInfo.success(applicationObjectService.list(id));
    }

    @PutMapping("/{id}/objects")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "保存应用业务对象")
    public RespInfo<Void> replaceObjects(@PathVariable Long id,
                                         @RequestBody List<BusinessApplicationObjectDTO> objects) {
        applicationObjectService.replace(id, objects);
        return RespInfo.success();
    }

    @PostMapping("/{id}/form-data/provision")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.ADD, desc = "准备页面表单数据存储")
    public RespInfo<BusinessApplicationFormDataVO> provisionFormData(
            @PathVariable Long id,
            @RequestBody BusinessApplicationFormDataProvisionDTO dto) {
        return RespInfo.success(formDataService.provision(id, dto));
    }

    @PostMapping("/{id}/design-page")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "保存应用页面设计")
    public RespInfo<BusinessApplicationPageDesignVO> designPage(
            @PathVariable Long id,
            @RequestBody BusinessApplicationPageDesignDTO dto) {
        BusinessApplicationPageDesignVO result = pageDesignService.save(id, dto);
        applicationObjectService.detachOrphanPageFormObjects(id);
        return RespInfo.success(result);
    }

    @PostMapping("/{id}/initialize-template")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.ADD, desc = "按模板初始化业务应用")
    public RespInfo<BusinessApplicationTemplateResultVO> initializeTemplate(
            @PathVariable Long id,
            @RequestBody BusinessApplicationTemplateInitializeDTO dto) {
        return RespInfo.success(templateService.initialize(id, dto));
    }

    @PostMapping("/{id}/initialize-ai")
    @SaCheckPermission("ai:businessApplication:edit")
    @OperationLog(module = "业务应用", type = OperationType.ADD, desc = "按 AI 方案初始化业务应用")
    public RespInfo<BusinessApplicationAiInitializeResultVO> initializeAi(
            @PathVariable Long id,
            @RequestBody LowcodeAiAppGenerateResult plan) {
        return RespInfo.success(aiInitializeService.initialize(id, plan));
    }

    @PostMapping("/{id}/publish/check")
    @SaCheckPermission("ai:businessApplication:publish")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "准备并执行应用发布预检查")
    public RespInfo<BusinessApplicationPublishCheckVO> publishCheck(
            @PathVariable Long id,
            @RequestBody(required = false) BusinessApplicationPublishDTO dto) {
        return RespInfo.success(publishService.check(id, dto));
    }

    @PostMapping("/{id}/publish")
    @SaCheckPermission("ai:businessApplication:publish")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "协调发布业务应用")
    public RespInfo<BusinessApplicationPublishResultVO> publish(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) BusinessApplicationPublishDTO dto) {
        return RespInfo.success(publishService.publish(id, dto, idempotencyKey));
    }

    @GetMapping("/{id}/versions")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用发布版本")
    public RespInfo<List<BusinessApplicationVersionVO>> versions(@PathVariable Long id) {
        return RespInfo.success(versionService.list(id));
    }

    @GetMapping("/{id}/versions/{versionNo}")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用发布版本详情")
    public RespInfo<BusinessApplicationVersionVO> versionDetail(
            @PathVariable Long id,
            @PathVariable Integer versionNo) {
        return RespInfo.success(versionService.detail(id, versionNo));
    }

    @GetMapping("/{id}/publish-runs")
    @SaCheckPermission("ai:businessApplication:list")
    @OperationLog(module = "业务应用", type = OperationType.QUERY, desc = "查询应用发布运行记录")
    public RespInfo<List<BusinessApplicationPublishRunVO>> publishRuns(@PathVariable Long id) {
        return RespInfo.success(publishRunService.list(id));
    }

    @PostMapping("/{id}/publish-runs/{runId}/recover")
    @SaCheckPermission("ai:businessApplication:recover")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "恢复应用协调发布")
    public RespInfo<BusinessApplicationPublishResultVO> recover(
            @PathVariable Long id,
            @PathVariable Long runId) {
        return RespInfo.success(recoveryService.recover(id, runId));
    }

    @PostMapping("/{id}/versions/{versionNo}/rollback")
    @SaCheckPermission("ai:businessApplication:rollback")
    @OperationLog(module = "业务应用", type = OperationType.UPDATE, desc = "回滚业务应用历史版本")
    public RespInfo<BusinessApplicationPublishResultVO> rollback(
            @PathVariable Long id,
            @PathVariable Integer versionNo,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) BusinessApplicationRollbackDTO dto) {
        return RespInfo.success(rollbackService.rollback(id, versionNo, dto, idempotencyKey));
    }
}
