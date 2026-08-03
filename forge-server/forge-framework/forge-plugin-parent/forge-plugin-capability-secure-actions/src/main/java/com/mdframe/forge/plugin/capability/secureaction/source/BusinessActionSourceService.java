package com.mdframe.forge.plugin.capability.secureaction.source;

import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionPublishedModelPolicy;
import com.mdframe.forge.plugin.capability.secureaction.publish.SecureActionStepValidator;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessObjectActionService;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectActionVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.List;

/**
 * 基于不可变业务对象发布快照生成能力注册候选项。
 */
public class BusinessActionSourceService {

    private final BusinessObjectActionService actionService;
    private final SecureActionStepValidator stepValidator;
    private final SecureActionPublishedModelPolicy publishedModelPolicy;

    public BusinessActionSourceService(
            BusinessObjectActionService actionService,
            SecureActionStepValidator stepValidator,
            SecureActionPublishedModelPolicy publishedModelPolicy) {
        this.actionService = actionService;
        this.stepValidator = stepValidator;
        this.publishedModelPolicy = publishedModelPolicy;
    }

    public BusinessActionRegistrationSource resolveRegistrationSource(
            String suiteCode,
            String objectCode) {
        BusinessObjectActionService.ResolvedPublishedBusinessActions resolved =
                actionService.resolvePublishedActions(suiteCode, objectCode, null);
        List<BusinessActionRegistrationAction> actions = resolved.actions().stream()
                .map(this::toRegistrationAction)
                .toList();
        List<BusinessActionRegistrationField> fields = publishedModelPolicy
                .writableFields(resolved.version())
                .values()
                .stream()
                .sorted(Comparator.comparing(
                        LowcodeFieldSchema::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(this::toRegistrationField)
                .toList();
        return new BusinessActionRegistrationSource(
                resolved.object().getId(),
                resolved.object().getSuiteCode(),
                resolved.object().getObjectCode(),
                resolved.object().getObjectName(),
                resolved.version().getPublishVersion(),
                actions,
                fields);
    }

    private BusinessActionRegistrationAction toRegistrationAction(BusinessObjectActionVO action) {
        SecureActionStepValidator.ValidationResult validation = stepValidator.inspect(action);
        boolean enabled = !Integer.valueOf(0).equals(action.getStatus());
        String unavailableReason = enabled
                ? validation.unavailableReason()
                : "业务动作「" + actionDisplayName(action)
                        + "」在已发布版本中已停用，请先启用动作并重新发布业务对象";
        return new BusinessActionRegistrationAction(
                action.getActionCode(),
                action.getActionName(),
                action.getActionType(),
                action.getStatus(),
                enabled && validation.publishable(),
                unavailableReason,
                validation.stepTypes());
    }

    private BusinessActionRegistrationField toRegistrationField(LowcodeFieldSchema field) {
        return new BusinessActionRegistrationField(
                field.getField(),
                StringUtils.defaultIfBlank(field.getLabel(), field.getField()),
                field.getColumnName(),
                field.getDataType(),
                field.getLength(),
                field.getPrecision(),
                field.getRequired(),
                field.getDictType(),
                field.getFieldStatus(),
                field.getRemark());
    }

    private String actionDisplayName(BusinessObjectActionVO action) {
        return StringUtils.firstNonBlank(action.getActionName(), action.getActionCode(), "未命名动作");
    }
}
