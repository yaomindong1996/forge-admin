package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessExtensionStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * 扩展生命周期与失败策略的单一规则入口。
 */
@Component
public class BusinessExtensionStateMachine {

    private static final Set<String> FAILURE_POLICIES = Set.of("BLOCK", "WARN", "IGNORE");
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    public void assertCanEnable(AiBusinessExtension extension, AiBusinessExtensionVersion version) {
        if (extension == null || version == null) {
            throw new BusinessException("扩展草稿版本不存在");
        }
        if (!Integer.valueOf(1).equals(version.getValidationPassed())) {
            throw new BusinessException("扩展当前版本尚未通过校验");
        }
        if (!Integer.valueOf(1).equals(version.getTestPassed())) {
            throw new BusinessException("扩展当前版本尚未通过测试");
        }
        if (!Integer.valueOf(extension.getDraftVersion()).equals(version.getVersionNo())) {
            throw new BusinessException("扩展测试版本与当前草稿不一致");
        }
        if (!BusinessExtensionStatus.TESTED.equals(extension.getStatus())) {
            throw new BusinessException("只有已测试的扩展才能启用");
        }
    }

    public String statusAfterContentChange(String currentStatus) {
        return BusinessExtensionStatus.DRAFT;
    }

    public void validateFailurePolicy(String hookCode, String riskLevel, String failurePolicy) {
        String hook = normalize(hookCode);
        String risk = normalize(riskLevel);
        String policy = normalize(failurePolicy);
        if (!FAILURE_POLICIES.contains(policy)) {
            throw new BusinessException("不支持的扩展失败策略: " + failurePolicy);
        }
        if (!RISK_LEVELS.contains(risk)) {
            throw new BusinessException("不支持的扩展风险级别: " + riskLevel);
        }
        if ("HIGH".equals(risk) && hook.startsWith("BEFORE_") && !"BLOCK".equals(policy)) {
            throw new BusinessException("高风险前置钩子只能使用阻断策略");
        }
        if ("IGNORE".equals(policy) && (!(hook.startsWith("AFTER_")) || !"LOW".equals(risk))) {
            throw new BusinessException("忽略策略只允许低风险后置钩子使用");
        }
    }

    private String normalize(String value) {
        return StringUtils.defaultString(value).trim().toUpperCase(Locale.ROOT);
    }
}
