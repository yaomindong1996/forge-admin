package com.mdframe.forge.starter.flow.delegate;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.mapper.FlowBusinessMapper;
import com.mdframe.forge.starter.flow.service.FlowCcService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BPMN 抄送节点委托。
 *
 * <p>抄送节点在设计器中表现为 serviceTask + flowable:type="cc"，执行时从节点
 * flowable:candidateUsers / flowable:candidateGroups 读取接收人配置。</p>
 */
@Slf4j
@Component("flowCcNodeDelegate")
public class FlowCcNodeDelegate implements JavaDelegate {

    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";

    private final FlowCcService flowCcService;
    private final FlowBusinessMapper flowBusinessMapper;

    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    @Autowired(required = false)
    private ProcessEngine processEngine;

    public FlowCcNodeDelegate(FlowCcService flowCcService, FlowBusinessMapper flowBusinessMapper) {
        this.flowCcService = flowCcService;
        this.flowBusinessMapper = flowBusinessMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        FlowElement currentNode = execution.getCurrentFlowElement();
        if (currentNode == null) {
            log.warn("流程抄送节点执行失败：当前节点为空，processInstanceId={}", execution.getProcessInstanceId());
            return;
        }

        FlowBusiness business = flowBusinessMapper.selectByProcessInstanceId(execution.getProcessInstanceId());
        List<String> configuredNames = splitValues(readFlowableAttribute(currentNode, "candidateUserNames"));
        List<String> receiverIds = resolveReceiverIds(currentNode, execution);
        if (receiverIds.isEmpty()) {
            log.warn("流程抄送节点未配置接收人：processInstanceId={}, activityId={}",
                    execution.getProcessInstanceId(), execution.getCurrentActivityId());
            return;
        }

        Runnable send = () -> flowCcService.sendCc(
                execution.getProcessInstanceId(),
                business != null ? business.getProcessDefKey() : execution.getProcessDefinitionId(),
                execution.getCurrentActivityId(),
                resolveTitle(currentNode, business),
                resolveContent(currentNode, business),
                business != null ? business.getBusinessKey() : execution.getProcessInstanceBusinessKey(),
                receiverIds,
                resolveUserNames(receiverIds, configuredNames),
                business != null ? business.getApplyUserId() : textValue(execution.getVariable("initiator")),
                business != null ? business.getApplyUserName() : null);

        if (business != null && business.getTenantId() != null) {
            TenantContextHolder.executeWithTenant(business.getTenantId(), send);
        } else {
            send.run();
        }

        log.info("流程抄送节点发送完成：processInstanceId={}, activityId={}, receivers={}",
                execution.getProcessInstanceId(), execution.getCurrentActivityId(), receiverIds);
    }

    private List<String> resolveReceiverIds(FlowElement node, DelegateExecution execution) {
        Set<String> result = new LinkedHashSet<>();
        for (String item : splitValues(readFlowableAttribute(node, "candidateUsers"))) {
            addResolvedValues(result, item, execution);
        }
        for (String group : splitValues(readFlowableAttribute(node, "candidateGroups"))) {
            for (String resolvedGroup : resolveValue(group, execution)) {
                addGroupUsers(result, resolvedGroup);
            }
        }
        return new ArrayList<>(result);
    }

    private void addResolvedValues(Set<String> values, String rawValue, DelegateExecution execution) {
        for (String value : resolveValue(rawValue, execution)) {
            addNonBlank(values, value);
        }
    }

    private List<String> resolveValue(String rawValue, DelegateExecution execution) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return List.of();
        }
        String variableName = unwrapSimpleExpression(value);
        if (variableName == null) {
            if (isExpression(value)) {
                return valuesFromObject(evaluateExpression(value, execution));
            }
            return List.of(value);
        }
        if (variableName.startsWith("user_")) {
            return List.of(variableName.substring("user_".length()));
        }
        if (hasVariable(execution, variableName)) {
            return valuesFromObject(execution.getVariable(variableName));
        }
        return valuesFromObject(evaluateExpression(value, execution));
    }

    private Object evaluateExpression(String expressionText, DelegateExecution execution) {
        if (!isExpression(expressionText) || processEngine == null) {
            return null;
        }
        try {
            if (!(processEngine.getProcessEngineConfiguration() instanceof ProcessEngineConfigurationImpl configuration)) {
                return null;
            }
            ExpressionManager expressionManager = configuration.getExpressionManager();
            if (expressionManager == null) {
                return null;
            }
            Expression expression = expressionManager.createExpression(expressionText);
            return expression.getValue(execution);
        } catch (Exception e) {
            log.warn("流程抄送节点表达式解析失败：expression={}, processInstanceId={}, activityId={}",
                    expressionText, execution.getProcessInstanceId(), execution.getCurrentActivityId(), e);
            return null;
        }
    }

    private boolean isExpression(String value) {
        String text = trimToNull(value);
        return text != null && text.startsWith("${") && text.endsWith("}");
    }

    private boolean hasVariable(DelegateExecution execution, String variableName) {
        try {
            return execution.hasVariable(variableName);
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> valuesFromObject(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Iterable<?>) {
            List<String> result = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                String text = trimToNull(item);
                if (text != null) {
                    result.add(text);
                }
            }
            return result;
        }
        if (value.getClass().isArray()) {
            List<String> result = new ArrayList<>();
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                String text = trimToNull(java.lang.reflect.Array.get(value, i));
                if (text != null) {
                    result.add(text);
                }
            }
            return result;
        }
        return splitValues(String.valueOf(value));
    }

    private void addGroupUsers(Set<String> receiverIds, String group) {
        if (flowOrgIntegrationService == null || trimToNull(group) == null) {
            return;
        }
        try {
            List<String> userIds = isNumeric(group)
                    ? flowOrgIntegrationService.getUserIdsByRoleId(group)
                    : flowOrgIntegrationService.getUserIdsByRoleCode(group);
            if (userIds == null || userIds.isEmpty()) {
                userIds = flowOrgIntegrationService.getUserIdsByGroupCode(group);
            }
            if ((userIds == null || userIds.isEmpty()) && isNumeric(group)) {
                userIds = flowOrgIntegrationService.getUserIdsByDeptId(group);
            }
            if (userIds != null) {
                for (String userId : userIds) {
                    addNonBlank(receiverIds, userId);
                }
            }
        } catch (Exception e) {
            log.warn("流程抄送节点解析候选组失败：group={}", group, e);
        }
    }

    private String resolveTitle(FlowElement node, FlowBusiness business) {
        String configuredTitle = trimToNull(readFlowableAttribute(node, "title"));
        if (configuredTitle != null) {
            return configuredTitle;
        }
        if (business != null && trimToNull(business.getTitle()) != null) {
            return business.getTitle();
        }
        return trimToNull(node.getName()) != null ? node.getName() : "流程抄送";
    }

    private String resolveContent(FlowElement node, FlowBusiness business) {
        String configuredContent = trimToNull(readFlowableAttribute(node, "content"));
        if (configuredContent != null) {
            return configuredContent;
        }
        String title = business != null ? trimToNull(business.getTitle()) : null;
        String businessKey = business != null ? trimToNull(business.getBusinessKey()) : null;
        String summary = title != null ? title : businessKey;
        return summary == null ? "请知悉流程抄送。" : "请知悉流程抄送：" + summary;
    }

    private List<String> resolveUserNames(List<String> userIds, List<String> configuredNames) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            if (i < configuredNames.size() && trimToNull(configuredNames.get(i)) != null) {
                names.add(configuredNames.get(i));
                continue;
            }
            names.add(resolveUserName(userIds.get(i)));
        }
        return names;
    }

    private String resolveUserName(String userId) {
        if (flowOrgIntegrationService == null) {
            return null;
        }
        try {
            Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(userId);
            if (userInfo == null) {
                return null;
            }
            Object name = userInfo.get("name");
            if (name == null) {
                name = userInfo.get("realName");
            }
            return name == null ? null : String.valueOf(name);
        } catch (Exception e) {
            log.debug("流程抄送节点解析用户姓名失败：userId={}", userId);
            return null;
        }
    }

    private String readFlowableAttribute(FlowElement node, String name) {
        String value = node.getAttributeValue(FLOWABLE_NS, name);
        if (trimToNull(value) != null) {
            return value;
        }
        return node.getAttributeValue(null, name);
    }

    private List<String> splitValues(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : text.split("[,;，；\\s]+")) {
            String normalized = trimToNull(item);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String unwrapSimpleExpression(String value) {
        String text = trimToNull(value);
        if (text == null || !text.startsWith("${") || !text.endsWith("}")) {
            return null;
        }
        String body = text.substring(2, text.length() - 1).trim();
        return body.matches("[A-Za-z_$][\\w$]*|user_\\d+") ? body : null;
    }

    private void addNonBlank(Set<String> values, String value) {
        String text = trimToNull(value);
        if (text != null) {
            values.add(text);
        }
    }

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
