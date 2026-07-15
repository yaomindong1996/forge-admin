package com.mdframe.forge.plugin.generator.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionQueryDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionTestDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionVersionDTO;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessExtensionExecutionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessExtensionLockService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessExtensionService;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessExtensionVersionService;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionInputField;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionHandler;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.LowcodeExtensionRegistry;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionDiffVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionLockVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionValidationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionVersionVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.ServerBindingHandlerVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 低代码业务扩展治理接口。
 */
@RestController
@RequestMapping("/ai/business/extension")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class BusinessExtensionController {

    private final BusinessExtensionService extensionService;
    private final BusinessExtensionVersionService versionService;
    private final BusinessExtensionLockService lockService;
    private final BusinessExtensionExecutionService executionService;
    private final LowcodeExtensionRegistry extensionRegistry;

    @GetMapping("/page")
    @SaCheckPermission("ai:businessExtension:list")
    @OperationLog(module = "业务扩展", type = OperationType.QUERY, desc = "分页查询业务扩展")
    public RespInfo<Page<BusinessExtensionVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            BusinessExtensionQueryDTO query) {
        return RespInfo.success(extensionService.page(pageNum, pageSize, query));
    }

    @GetMapping("/list")
    @SaCheckPermission("ai:businessExtension:list")
    @OperationLog(module = "业务扩展", type = OperationType.QUERY, desc = "查询业务扩展列表")
    public RespInfo<List<BusinessExtensionVO>> list(BusinessExtensionQueryDTO query) {
        return RespInfo.success(extensionService.list(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ai:businessExtension:list")
    @OperationLog(module = "业务扩展", type = OperationType.QUERY, desc = "查询业务扩展详情")
    public RespInfo<BusinessExtensionVO> detail(@PathVariable Long id) {
        return RespInfo.success(extensionService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("ai:businessExtension:add")
    @OperationLog(module = "业务扩展", type = OperationType.ADD, desc = "新增业务扩展草稿")
    public RespInfo<Long> create(@RequestBody BusinessExtensionDTO dto) {
        return RespInfo.success(extensionService.create(dto));
    }

    @PutMapping
    @SaCheckPermission("ai:businessExtension:edit")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "修改业务扩展元数据")
    public RespInfo<Void> update(@RequestBody BusinessExtensionDTO dto) {
        lockService.assertOwned(dto.getId(), dto.getLockToken());
        extensionService.update(dto);
        return RespInfo.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ai:businessExtension:delete")
    @OperationLog(module = "业务扩展", type = OperationType.DELETE, desc = "逻辑删除业务扩展")
    public RespInfo<Void> delete(@PathVariable Long id) {
        extensionService.delete(id);
        return RespInfo.success();
    }

    @GetMapping("/{id}/versions")
    @SaCheckPermission("ai:businessExtension:list")
    @OperationLog(module = "业务扩展", type = OperationType.QUERY, desc = "查询扩展版本历史")
    public RespInfo<List<BusinessExtensionVersionVO>> versions(@PathVariable Long id) {
        return RespInfo.success(versionService.list(id));
    }

    @PostMapping("/{id}/versions")
    @SaCheckPermission("ai:businessExtension:edit")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "保存扩展新草稿版本")
    public RespInfo<Integer> saveDraft(@PathVariable Long id,
                                       @RequestBody BusinessExtensionVersionDTO dto) {
        return RespInfo.success(versionService.saveDraft(id, dto));
    }

    @GetMapping("/{id}/versions/diff")
    @SaCheckPermission("ai:businessExtension:list")
    @OperationLog(module = "业务扩展", type = OperationType.QUERY, desc = "查询扩展版本差异")
    public RespInfo<BusinessExtensionDiffVO> diff(@PathVariable Long id,
                                                  @RequestParam(required = false) Integer baseVersion,
                                                  @RequestParam(required = false) Integer targetVersion) {
        return RespInfo.success(versionService.diff(id, baseVersion, targetVersion));
    }

    @PostMapping("/{id}/versions/{versionNo}/rollback")
    @SaCheckPermission("ai:businessExtension:rollback")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "回滚扩展历史版本为新草稿")
    public RespInfo<Integer> rollback(@PathVariable Long id,
                                      @PathVariable Integer versionNo,
                                      @RequestParam String lockToken) {
        return RespInfo.success(versionService.rollback(id, versionNo, lockToken));
    }

    @PostMapping("/{id}/lock")
    @SaCheckPermission("ai:businessExtension:edit")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "获取扩展编辑锁")
    public RespInfo<BusinessExtensionLockVO> acquireLock(@PathVariable Long id) {
        return RespInfo.success(lockService.acquire(id));
    }

    @PutMapping("/{id}/lock")
    @SaCheckPermission("ai:businessExtension:edit")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "续期扩展编辑锁")
    public RespInfo<BusinessExtensionLockVO> renewLock(@PathVariable Long id,
                                                       @RequestParam String lockToken) {
        return RespInfo.success(lockService.renew(id, lockToken));
    }

    @DeleteMapping("/{id}/lock")
    @SaCheckPermission("ai:businessExtension:edit")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "释放扩展编辑锁")
    public RespInfo<Void> releaseLock(@PathVariable Long id,
                                      @RequestParam String lockToken) {
        lockService.release(id, lockToken);
        return RespInfo.success();
    }

    @PostMapping("/{id}/validate")
    @SaCheckPermission("ai:businessExtension:validate")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "校验扩展当前草稿")
    public RespInfo<BusinessExtensionValidationVO> validate(@PathVariable Long id) {
        return RespInfo.success(executionService.validate(id));
    }

    @PostMapping("/{id}/test")
    @SaCheckPermission("ai:businessExtension:test")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "执行扩展受限测试")
    public RespInfo<BusinessExtensionValidationVO> test(@PathVariable Long id,
                                                        @RequestBody(required = false) BusinessExtensionTestDTO dto) {
        return RespInfo.success(executionService.test(id, dto));
    }

    @PutMapping("/{id}/status")
    @SaCheckPermission("ai:businessExtension:status")
    @OperationLog(module = "业务扩展", type = OperationType.UPDATE, desc = "启停业务扩展")
    public RespInfo<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        executionService.updateStatus(id, status);
        return RespInfo.success();
    }

    @GetMapping("/server-handlers")
    @SaCheckPermission("ai:businessExtension:list")
    @OperationLog(module = "业务扩展", type = OperationType.QUERY, desc = "查询服务端扩展白名单目录")
    public RespInfo<List<ServerBindingHandlerVO>> serverHandlers() {
        return RespInfo.success(extensionRegistry.registeredHandlers().values().stream()
                .map(this::toHandlerVO)
                .toList());
    }

    private ServerBindingHandlerVO toHandlerVO(LowcodeExtensionHandler handler) {
        ServerBindingHandlerVO vo = new ServerBindingHandlerVO();
        vo.setHandlerCode(handler.handlerCode());
        vo.setHandlerName(handler.handlerName());
        vo.setAllowedHooks(handler.allowedHooks());
        vo.setInputSchema(toSchemaMap(handler.inputSchema()));
        vo.setOutputSchema(toSchemaMap(handler.outputSchema()));
        vo.setTimeoutMs(handler.timeoutMs());
        vo.setRiskLevel(handler.riskLevel());
        vo.setRequiredPermission(handler.requiredPermission());
        return vo;
    }

    private Map<String, Object> toSchemaMap(Map<String, ExtensionInputField> schema) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (schema == null) {
            return result;
        }
        schema.forEach((key, field) -> result.put(key, Map.of(
                "type", field.getType(),
                "required", field.isRequired()
        )));
        return result;
    }
}
