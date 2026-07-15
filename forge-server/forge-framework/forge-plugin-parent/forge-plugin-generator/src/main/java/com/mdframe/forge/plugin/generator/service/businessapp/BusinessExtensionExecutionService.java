package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.constant.BusinessExtensionType;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionExecutionLog;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionTestDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionExecutionLogMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionExecutionContext;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ExtensionExecutionResult;
import com.mdframe.forge.plugin.generator.service.businessapp.extension.ServerBindingExecutor;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionValidationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 扩展校验、受限测试、启停和服务端钩子执行编排。
 */
@Service
@RequiredArgsConstructor
public class BusinessExtensionExecutionService {

    private final BusinessExtensionMapper extensionMapper;
    private final BusinessExtensionVersionMapper versionMapper;
    private final BusinessExtensionExecutionLogMapper auditMapper;
    private final BusinessExtensionValidationService validationService;
    private final BusinessExtensionStateMachine stateMachine;
    private final ServerBindingExecutor serverBindingExecutor;
    private final ObjectMapper objectMapper;
    private final BusinessApplicationChangeTracker applicationChangeTracker;

    @Transactional(rollbackFor = Exception.class)
    public BusinessExtensionValidationVO validate(Long extensionId) {
        AiBusinessExtension extension = requireExtension(extensionId);
        AiBusinessExtensionVersion version = requireDraftVersion(extension);
        BusinessExtensionValidationVO result = validationService.validate(extension, version);
        versionMapper.updateValidationResult(resolveTenantId(), extensionId, version.getVersionNo(),
                result.isPassed() ? 1 : 0, result.getSummary());
        if (!result.isPassed()) {
            versionMapper.updateTestResult(resolveTenantId(), extensionId, version.getVersionNo(), 0,
                    "内容校验失败，测试结果已失效");
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessExtensionValidationVO test(Long extensionId, BusinessExtensionTestDTO dto) {
        AiBusinessExtension extension = requireExtension(extensionId);
        AiBusinessExtensionVersion version = requireDraftVersion(extension);
        BusinessExtensionValidationVO validation = validationService.validate(extension, version);
        versionMapper.updateValidationResult(resolveTenantId(), extensionId, version.getVersionNo(),
                validation.isPassed() ? 1 : 0, validation.getSummary());
        if (!validation.isPassed()) {
            versionMapper.updateTestResult(resolveTenantId(), extensionId, version.getVersionNo(), 0,
                    "校验未通过，不能测试");
            return validation;
        }

        BusinessExtensionValidationVO result = new BusinessExtensionValidationVO();
        try {
            if (BusinessExtensionType.SERVER_BINDING.equals(extension.getExtensionType())) {
                ExtensionExecutionResult executionResult = executeServerBinding(
                        extension, version, dto == null ? Map.of() : dto.getInput());
                if (!executionResult.isSuccess()) {
                    result.getIssues().add("Java 增强处理器返回失败: "
                            + StringUtils.defaultIfBlank(executionResult.getCode(), "HANDLER_FAILED"));
                }
            } else if (BusinessExtensionType.CLIENT_JS.equals(extension.getExtensionType())
                    && (dto == null || !"PASSED".equals(dto.getClientSandboxResult()))) {
                result.getIssues().add("客户端脚本必须先通过 Worker 沙箱测试");
            }
        } catch (BusinessException e) {
            result.getIssues().add(e.getMessage());
        }
        result.setPassed(result.getIssues().isEmpty());
        result.setSummary(result.isPassed() ? "受限测试通过" : String.join("；", result.getIssues()));
        versionMapper.updateTestResult(resolveTenantId(), extensionId, version.getVersionNo(),
                result.isPassed() ? 1 : 0, result.getSummary());
        if (result.isPassed()) {
            extensionMapper.updateLifecycle(resolveTenantId(), extensionId,
                    BusinessExtensionStatus.TESTED, extension.getEnabledVersion());
            applicationChangeTracker.markApplicationChanged(extension.getApplicationId());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long extensionId, String targetStatus) {
        AiBusinessExtension extension = requireExtension(extensionId);
        if (BusinessExtensionStatus.ENABLED.equals(targetStatus)) {
            AiBusinessExtensionVersion version = requireDraftVersion(extension);
            stateMachine.assertCanEnable(extension, version);
            extensionMapper.updateLifecycle(resolveTenantId(), extensionId,
                    BusinessExtensionStatus.ENABLED, extension.getDraftVersion());
            applicationChangeTracker.markApplicationChanged(extension.getApplicationId());
            return;
        }
        if (BusinessExtensionStatus.DISABLED.equals(targetStatus)) {
            if (!BusinessExtensionStatus.ENABLED.equals(extension.getStatus())) {
                throw new BusinessException("只有已启用扩展才能停用");
            }
            extensionMapper.updateLifecycle(resolveTenantId(), extensionId,
                    BusinessExtensionStatus.DISABLED, extension.getEnabledVersion());
            applicationChangeTracker.markApplicationChanged(extension.getApplicationId());
            return;
        }
        throw new BusinessException("扩展状态接口只允许启用或停用");
    }

    public List<ExtensionExecutionResult> executeHook(Long applicationId, Long objectId, Long entryId,
                                                       String hookCode, Map<String, Object> input) {
        List<AiBusinessExtension> extensions = extensionMapper.selectEnabledForHook(
                resolveTenantId(), applicationId, objectId, entryId, hookCode);
        List<ExtensionExecutionResult> results = new ArrayList<>();
        for (AiBusinessExtension extension : extensions) {
            if (!BusinessExtensionType.SERVER_BINDING.equals(extension.getExtensionType())) {
                continue;
            }
            AiBusinessExtensionVersion version = versionMapper.selectVersion(
                    resolveTenantId(), extension.getId(), extension.getEnabledVersion());
            try {
                ExtensionExecutionResult result = executeServerBinding(extension, version, input);
                if (!result.isSuccess() && "BLOCK".equals(extension.getFailurePolicy())) {
                    throw new BusinessException("Java 增强执行失败: "
                            + StringUtils.defaultIfBlank(result.getCode(), "HANDLER_FAILED"));
                }
                results.add(result);
            } catch (BusinessException e) {
                if ("BLOCK".equals(extension.getFailurePolicy())) {
                    throw e;
                }
                results.add(ExtensionExecutionResult.failure("EXTENSION_FAILED",
                        "扩展执行失败，已按" + extension.getFailurePolicy() + "策略处理"));
            }
        }
        return results;
    }

    private ExtensionExecutionResult executeServerBinding(AiBusinessExtension extension,
                                                            AiBusinessExtensionVersion version,
                                                            Map<String, Object> input) {
        if (version == null) {
            throw new BusinessException("扩展执行版本不存在");
        }
        long startedAt = System.nanoTime();
        String resultStatus = "SUCCESS";
        String errorCode = null;
        String errorSummary = null;
        try {
            JsonNode config = objectMapper.readTree(version.getConfigJson());
            String handlerCode = config.path("handlerCode").asText(null);
            ExtensionExecutionContext context = new ExtensionExecutionContext();
            context.setTenantId(resolveTenantId());
            context.setActorUserId(resolveUserId());
            context.setApplicationId(extension.getApplicationId());
            context.setObjectId(extension.getObjectId());
            context.setEntryId(extension.getEntryId());
            context.setExtensionId(extension.getId());
            context.setExtensionCode(extension.getExtensionCode());
            context.setVersionNo(version.getVersionNo());
            context.setHandlerCode(handlerCode);
            context.setHookCode(extension.getHookCode());
            context.setInput(input == null ? Map.of() : input);
            ExtensionExecutionResult result = serverBindingExecutor.execute(context);
            if (!result.isSuccess()) {
                resultStatus = "FAILED";
                errorCode = StringUtils.abbreviate(
                        StringUtils.defaultIfBlank(result.getCode(), "HANDLER_FAILED"), 64);
                errorSummary = "服务端扩展返回失败结果（详细信息已脱敏）";
            }
            return result;
        } catch (BusinessException e) {
            resultStatus = "FAILED";
            errorCode = "EXTENSION_EXECUTION_FAILED";
            errorSummary = "服务端扩展执行失败（详细信息已脱敏）";
            throw e;
        } catch (Exception e) {
            resultStatus = "FAILED";
            errorCode = "EXTENSION_CONFIG_INVALID";
            errorSummary = "服务端扩展配置无效（详细信息已脱敏）";
            throw new BusinessException(errorSummary);
        } finally {
            recordAudit(extension, version, resultStatus,
                    (System.nanoTime() - startedAt) / 1_000_000L, errorCode, errorSummary);
        }
    }

    private void recordAudit(AiBusinessExtension extension, AiBusinessExtensionVersion version,
                             String resultStatus, Long durationMs, String errorCode, String errorSummary) {
        AiBusinessExtensionExecutionLog audit = new AiBusinessExtensionExecutionLog();
        audit.setTenantId(resolveTenantId());
        audit.setExtensionId(extension.getId());
        audit.setExtensionCode(extension.getExtensionCode());
        audit.setVersionNo(version.getVersionNo());
        audit.setApplicationId(extension.getApplicationId());
        audit.setObjectId(extension.getObjectId());
        audit.setEntryId(extension.getEntryId());
        audit.setHookCode(extension.getHookCode());
        audit.setResultStatus(resultStatus);
        audit.setDurationMs(durationMs);
        audit.setErrorCode(errorCode);
        audit.setErrorSummary(StringUtils.abbreviate(errorSummary, 500));
        audit.setActorUserId(resolveUserId());
        auditMapper.insert(audit);
    }

    private AiBusinessExtension requireExtension(Long extensionId) {
        AiBusinessExtension extension = extensionMapper.selectEntityById(resolveTenantId(), extensionId);
        if (extension == null) {
            throw new BusinessException("业务扩展不存在");
        }
        return extension;
    }

    private AiBusinessExtensionVersion requireDraftVersion(AiBusinessExtension extension) {
        AiBusinessExtensionVersion version = versionMapper.selectVersion(
                resolveTenantId(), extension.getId(), extension.getDraftVersion());
        if (version == null) {
            throw new BusinessException("扩展当前草稿版本不存在");
        }
        return version;
    }

    private Long resolveTenantId() {
        try {
            Long value = SessionHelper.getTenantId();
            return value == null ? 1L : value;
        } catch (Exception e) {
            return 1L;
        }
    }

    private Long resolveUserId() {
        try {
            Long value = SessionHelper.getUserId();
            return value == null ? 1L : value;
        } catch (Exception e) {
            return 1L;
        }
    }
}
