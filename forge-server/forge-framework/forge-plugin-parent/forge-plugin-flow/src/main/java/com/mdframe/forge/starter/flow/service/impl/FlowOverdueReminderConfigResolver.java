package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.dto.FlowOverdueReminderConfig;
import com.mdframe.forge.starter.flow.entity.FlowNodeConfig;
import com.mdframe.forge.starter.flow.entity.FlowTask;
import com.mdframe.forge.starter.flow.mapper.FlowNodeConfigMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.engine.RepositoryService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 解析审批节点逾期提醒配置，优先读取 BPMN UserTask 扩展属性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowOverdueReminderConfigResolver {

    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";

    private final RepositoryService repositoryService;
    private final FlowNodeConfigMapper flowNodeConfigMapper;

    public FlowOverdueReminderConfig resolve(FlowTask task) {
        FlowOverdueReminderConfig config = new FlowOverdueReminderConfig();
        if (task == null || isBlank(task.getTaskDefKey())) {
            return config;
        }

        FlowNode flowNode = getFlowNode(task);
        boolean hasBpmnReminderConfig = flowNode != null && applyBpmnConfig(config, flowNode);
        if (!hasBpmnReminderConfig) {
            applyNodeConfig(config, task);
        }
        normalize(config);
        return config;
    }

    private FlowNode getFlowNode(FlowTask task) {
        if (isBlank(task.getProcessDefId())) {
            return null;
        }
        try {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefId());
            if (bpmnModel == null || bpmnModel.getMainProcess() == null) {
                return null;
            }
            FlowElement element = bpmnModel.getMainProcess().getFlowElement(task.getTaskDefKey());
            return element instanceof FlowNode ? (FlowNode) element : null;
        } catch (Exception e) {
            log.warn("读取流程节点BPMN配置失败: processDefId={}, taskDefKey={}",
                    task.getProcessDefId(), task.getTaskDefKey(), e);
            return null;
        }
    }

    private boolean applyBpmnConfig(FlowOverdueReminderConfig config, FlowNode flowNode) {
        boolean configured = false;

        Boolean enabled = readBoolean(flowNode, "overdueReminderEnabled");
        if (enabled != null) {
            config.setEnabled(enabled);
            configured = true;
        }

        String templateCode = readString(flowNode, "overdueReminderTemplateCode");
        if (!isBlank(templateCode)) {
            config.setTemplateCode(templateCode.trim());
            configured = true;
        }

        String channels = readString(flowNode, "overdueReminderChannels");
        if (!isBlank(channels)) {
            config.setChannels(splitCsv(channels));
            configured = true;
        }

        String repeatMode = readString(flowNode, "overdueReminderRepeatMode");
        if (!isBlank(repeatMode)) {
            config.setRepeatMode(repeatMode.trim());
            configured = true;
        }

        Integer intervalMinutes = readInteger(flowNode, "overdueReminderIntervalMinutes");
        if (intervalMinutes != null) {
            config.setIntervalMinutes(intervalMinutes);
            configured = true;
        }

        Integer maxTimes = readInteger(flowNode, "overdueReminderMaxTimes");
        if (maxTimes != null) {
            config.setMaxTimes(maxTimes);
            configured = true;
        }

        return configured;
    }

    private void applyNodeConfig(FlowOverdueReminderConfig config, FlowTask task) {
        if (isBlank(task.getProcessDefKey()) || isBlank(task.getTaskDefKey())) {
            return;
        }
        FlowNodeConfig nodeConfig = flowNodeConfigMapper.selectByModelKeyAndNode(
                task.getProcessDefKey(), task.getTaskDefKey(), currentTenantId());
        if (nodeConfig == null) {
            return;
        }
        if (nodeConfig.getOverdueReminderEnabled() != null) {
            config.setEnabled(nodeConfig.getOverdueReminderEnabled());
        }
        if (!isBlank(nodeConfig.getOverdueReminderTemplateCode())) {
            config.setTemplateCode(nodeConfig.getOverdueReminderTemplateCode().trim());
        }
        if (!isBlank(nodeConfig.getOverdueReminderChannels())) {
            config.setChannels(splitCsv(nodeConfig.getOverdueReminderChannels()));
        }
        if (!isBlank(nodeConfig.getOverdueReminderRepeatMode())) {
            config.setRepeatMode(nodeConfig.getOverdueReminderRepeatMode().trim());
        }
        if (nodeConfig.getOverdueReminderIntervalMinutes() != null) {
            config.setIntervalMinutes(nodeConfig.getOverdueReminderIntervalMinutes());
        }
        if (nodeConfig.getOverdueReminderMaxTimes() != null) {
            config.setMaxTimes(nodeConfig.getOverdueReminderMaxTimes());
        }
    }

    private Long currentTenantId() {
        Long contextTenantId = TenantContextHolder.getTenantId();
        if (contextTenantId != null && contextTenantId > 0) {
            return contextTenantId;
        }
        return SessionHelper.getTenantId();
    }

    private String readString(FlowNode flowNode, String name) {
        String value = flowNode.getAttributeValue(FLOWABLE_NS, name);
        if (isBlank(value)) {
            Map<String, List<ExtensionElement>> extensions = flowNode.getExtensionElements();
            List<ExtensionElement> elements = extensions != null ? extensions.get(name) : null;
            if (elements != null && !elements.isEmpty()) {
                value = elements.get(0).getElementText();
            }
        }
        return value;
    }

    private Boolean readBoolean(FlowNode flowNode, String name) {
        String value = readString(flowNode, name);
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized) || "1".equals(normalized)
                || "Y".equalsIgnoreCase(normalized) || "yes".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized) || "0".equals(normalized)
                || "N".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
            return false;
        }
        return null;
    }

    private Integer readInteger(FlowNode flowNode, String name) {
        String value = readString(flowNode, name);
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("流程节点逾期提醒数字配置无效: name={}, value={}", name, value);
            return null;
        }
    }

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .toList();
    }

    private void normalize(FlowOverdueReminderConfig config) {
        if (isBlank(config.getTemplateCode())) {
            config.setTemplateCode(FlowOverdueReminderConfig.DEFAULT_TEMPLATE_CODE);
        }
        if (config.getChannels() == null || config.getChannels().isEmpty()) {
            config.setChannels(List.of(FlowOverdueReminderConfig.DEFAULT_CHANNEL));
        } else {
            config.setChannels(config.getChannels().stream()
                    .filter(channel -> !isBlank(channel))
                    .map(channel -> channel.trim().toUpperCase())
                    .distinct()
                    .toList());
        }
        if (!FlowOverdueReminderConfig.REPEAT_INTERVAL.equals(config.getRepeatMode())) {
            config.setRepeatMode(FlowOverdueReminderConfig.REPEAT_ONCE);
            config.setMaxTimes(1);
        }
        if (config.getIntervalMinutes() == null || config.getIntervalMinutes() < 30) {
            config.setIntervalMinutes(30);
        }
        if (config.getMaxTimes() == null || config.getMaxTimes() < 1) {
            config.setMaxTimes(1);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
