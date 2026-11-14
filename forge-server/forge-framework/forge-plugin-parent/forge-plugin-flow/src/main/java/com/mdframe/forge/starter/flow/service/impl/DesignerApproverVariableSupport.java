package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设计器内置审批人（上级领导 / 部门主管 / HR）在 BPMN 中写成 ${initiatorLeader}、${deptManager}、${hr}，
 * 运行时依赖同名流程变量。发起时按发起人组织关系补齐；补不齐就拒绝发起，
 * 否则流程会在走到该节点时才报 Unknown property，实例停在半路。
 */
final class DesignerApproverVariableSupport {

    static final String LEADER = "initiatorLeader";
    static final String DEPT_MANAGER = "deptManager";
    static final String HR = "hr";
    static final String HR_ROLE_CODE = "hr";

    private static final Pattern BUILT_IN_ASSIGNEE = Pattern.compile(
            "^\\$\\{\\s*(" + LEADER + "|" + DEPT_MANAGER + "|" + HR + ")\\s*}$");

    private DesignerApproverVariableSupport() {
    }

    static void enrich(BpmnModel bpmnModel, Map<String, Object> variables, String userId, String deptId,
                       FlowOrgIntegrationService orgService) {
        if (bpmnModel == null || bpmnModel.getMainProcess() == null || orgService == null) {
            return;
        }
        Map<String, String> required = new LinkedHashMap<>();
        for (UserTask task : bpmnModel.getMainProcess().findFlowElementsOfType(UserTask.class)) {
            String assignee = task.getAssignee();
            if (assignee == null) {
                continue;
            }
            Matcher matcher = BUILT_IN_ASSIGNEE.matcher(assignee.trim());
            if (matcher.matches()) {
                String nodeName = task.getName() == null || task.getName().isBlank() ? task.getId() : task.getName();
                required.putIfAbsent(matcher.group(1), nodeName);
            }
        }
        for (Map.Entry<String, String> entry : required.entrySet()) {
            String variable = entry.getKey();
            Object existing = variables.get(variable);
            if (existing != null && !existing.toString().isBlank()) {
                continue;
            }
            String resolved = resolve(variable, userId, deptId, orgService);
            if (resolved == null || resolved.isBlank()) {
                throw new BusinessException(missingMessage(variable, entry.getValue()));
            }
            variables.put(variable, resolved);
        }
    }

    private static String resolve(String variable, String userId, String deptId, FlowOrgIntegrationService org) {
        if (LEADER.equals(variable)) {
            return firstNonBlank(org.getLeaderUserIdByLevel(userId, 1), first(org.getLeaderUserIds(userId)));
        }
        if (DEPT_MANAGER.equals(variable)) {
            return firstNonBlank(org.getDeptManagerUserIdByLevel(userId, 1),
                    first(org.getDeptManagerUserIds(userId)),
                    deptId == null ? null : first(org.getDeptManagerByDeptId(deptId)));
        }
        return first(org.getUserIdsByRoleCode(HR_ROLE_CODE));
    }

    private static String missingMessage(String variable, String nodeName) {
        if (LEADER.equals(variable)) {
            return "发起人未设置上级领导，无法确定审批节点「" + nodeName + "」的审批人";
        }
        if (DEPT_MANAGER.equals(variable)) {
            return "发起人所在部门未设置部门主管，无法确定审批节点「" + nodeName + "」的审批人";
        }
        return "未找到角色编码为 " + HR_ROLE_CODE + " 的人员，无法确定审批节点「" + nodeName + "」的 HR 审批人";
    }

    private static String first(List<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
