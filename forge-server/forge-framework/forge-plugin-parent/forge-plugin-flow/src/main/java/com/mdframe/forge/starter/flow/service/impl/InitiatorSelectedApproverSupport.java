package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.dto.FlowStartConfig;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 发起人自选审批人的 BPMN 与启动变量协议。 */
final class InitiatorSelectedApproverSupport {

    static final String VARIABLE_NAME = "PROCESS_START_USER";
    static final String FLOWABLE_NS = "http://flowable.org/bpmn";
    private static final Pattern COLLECTION_PATTERN = Pattern.compile(
            "^\\$\\{PROCESS_START_USER\\[['\"]([^'\"\\]]+)['\"]\\]\\}$");

    private InitiatorSelectedApproverSupport() {
    }

    static List<FlowStartConfig.ApproverNode> discover(BpmnModel bpmnModel) {
        List<FlowStartConfig.ApproverNode> result = new ArrayList<>();
        if (bpmnModel == null || bpmnModel.getMainProcess() == null) {
            return result;
        }
        for (UserTask task : bpmnModel.getMainProcess().findFlowElementsOfType(UserTask.class)) {
            String nodeKey = resolveNodeKey(task);
            if (nodeKey == null) {
                continue;
            }
            FlowStartConfig.ApproverNode node = new FlowStartConfig.ApproverNode();
            node.setNodeKey(nodeKey);
            node.setNodeName(task.getName());
            node.setMultiple(task.getLoopCharacteristics() != null);
            result.add(node);
        }
        return result;
    }

    static List<FlowStartConfig.ApproverNode> discoverNextNodes(BpmnModel bpmnModel) {
        List<FlowStartConfig.ApproverNode> result = new ArrayList<>();
        if (bpmnModel == null || bpmnModel.getMainProcess() == null) {
            return result;
        }
        Map<String, FlowElement> elements = new LinkedHashMap<>();
        for (FlowElement element : bpmnModel.getMainProcess().getFlowElements()) {
            elements.put(element.getId(), element);
        }
        for (StartEvent start : bpmnModel.getMainProcess().findFlowElementsOfType(StartEvent.class)) {
            discoverNext(start, elements, result, new java.util.HashSet<>());
        }
        return result;
    }

    static List<String> preflightNextApprovers(BpmnModel bpmnModel) {
        List<String> diagnostics = new ArrayList<>();
        for (FlowStartConfig.ApproverNode node : discoverNextNodes(bpmnModel)) {
            boolean configured = hasText(node.getAssignee())
                    || hasText(node.getCandidateUsers())
                    || hasText(node.getCandidateGroups());
            if (!configured && !isInitiatorSelected(bpmnModel, node.getNodeKey())) {
                String name = hasText(node.getNodeName()) ? node.getNodeName() : node.getNodeKey();
                diagnostics.add("审批节点「" + name + "」未配置处理人、候选用户或候选组");
            }
        }
        return diagnostics;
    }

    private static void discoverNext(FlowNode node, Map<String, FlowElement> elements,
                                     List<FlowStartConfig.ApproverNode> result, java.util.Set<String> visited) {
        if (node == null || !visited.add(node.getId())) {
            return;
        }
        for (SequenceFlow flow : node.getOutgoingFlows()) {
            FlowElement target = elements.get(flow.getTargetRef());
            if (!(target instanceof FlowNode targetNode)) {
                continue;
            }
            if (target instanceof UserTask task) {
                FlowStartConfig.ApproverNode preview = new FlowStartConfig.ApproverNode();
                preview.setNodeKey(task.getId());
                preview.setNodeName(task.getName());
                preview.setMultiple(task.getLoopCharacteristics() != null);
                preview.setAssignee(task.getAssignee());
                preview.setCandidateUsers(attribute(task, "candidateUsers"));
                preview.setCandidateGroups(attribute(task, "candidateGroups"));
                if (result.stream().noneMatch(item -> task.getId().equals(item.getNodeKey()))) {
                    result.add(preview);
                }
                continue;
            }
            discoverNext(targetNode, elements, result, visited);
        }
    }

    private static String attribute(UserTask task, String name) {
        if ("candidateUsers".equals(name) && task.getCandidateUsers() != null
                && !task.getCandidateUsers().isEmpty()) {
            return String.join(",", task.getCandidateUsers());
        }
        if ("candidateGroups".equals(name) && task.getCandidateGroups() != null
                && !task.getCandidateGroups().isEmpty()) {
            return String.join(",", task.getCandidateGroups());
        }
        String value = task.getAttributeValue(FLOWABLE_NS, name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean isInitiatorSelected(BpmnModel bpmnModel, String nodeKey) {
        if (bpmnModel == null || bpmnModel.getMainProcess() == null || nodeKey == null) {
            return false;
        }
        for (UserTask task : bpmnModel.getMainProcess().findFlowElementsOfType(UserTask.class)) {
            if (nodeKey.equals(resolveNodeKey(task))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static void validateAndNormalize(BpmnModel bpmnModel, Map<String, Object> variables) {
        List<FlowStartConfig.ApproverNode> nodes = discover(bpmnModel);
        if (nodes.isEmpty()) {
            return;
        }
        Object raw = variables.get(VARIABLE_NAME);
        if (!(raw instanceof Map<?, ?> selections)) {
            throw new IllegalArgumentException("请为发起人自选节点配置审批人");
        }
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        for (FlowStartConfig.ApproverNode node : nodes) {
            List<String> ids = readUserIds(selections.get(node.getNodeKey()));
            if (ids.isEmpty()) {
                String name = node.getNodeName() == null || node.getNodeName().isBlank()
                        ? node.getNodeKey() : node.getNodeName();
                throw new IllegalArgumentException("请选择节点「" + name + "」的审批人");
            }
            if (!Boolean.TRUE.equals(node.getMultiple()) && ids.size() > 1) {
                ids = List.of(ids.get(0));
            }
            normalized.put(node.getNodeKey(), ids);
            if (!Boolean.TRUE.equals(node.getMultiple())) {
                variables.put(singleAssigneeVariable(node.getNodeKey()), ids.get(0));
            }
        }
        variables.put(VARIABLE_NAME, normalized);
    }

    static List<String> readUserIds(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(item -> item != null && !String.valueOf(item).isBlank())
                    .map(item -> String.valueOf(item).trim())
                    .distinct()
                    .toList();
        }
        if (value == null) {
            return List.of();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? List.of() : List.of(text);
    }

    static String singleAssigneeVariable(String nodeKey) {
        String sanitized = nodeKey == null ? "" : nodeKey.replaceAll("[^A-Za-z0-9_]", "_");
        if (sanitized.isBlank()) {
            sanitized = "node";
        }
        return "INITIATOR_SELECT_" + sanitized;
    }

    private static String resolveNodeKey(UserTask task) {
        MultiInstanceLoopCharacteristics loop = task.getLoopCharacteristics();
        if (loop != null) {
            String collection = loop.getCollectionString();
            if (collection == null || collection.isBlank()) {
                collection = loop.getInputDataItem();
            }
            Matcher matcher = COLLECTION_PATTERN.matcher(collection == null ? "" : collection.trim());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        String assigneeType = task.getAttributeValue(FLOWABLE_NS, "assigneeType");
        if ("initiatorSelect".equals(assigneeType == null ? "" : assigneeType.trim())) {
            return task.getId();
        }
        return null;
    }
}
