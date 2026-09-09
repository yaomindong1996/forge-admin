package com.mdframe.forge.starter.flow.service;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flowable SPEL 表达式服务
 * <p>提供在流程定义中可用的自定义方法，用于动态计算审批人</p>
 *
 * <p>使用示例：</p>
 * <pre>
 * ${flowSpelService.findDeptManager(execution.getVariable('deptId'))}
 * ${flowSpelService.findUsersByRole(execution.getVariable('roleKey'))}
 * ${flowSpelService.findRegionManager(execution.getVariable('regionCode'))}
 * </pre>
 */
@Slf4j
@Service("flowSpelService")
public class FlowSpelService {

    private static final int MAX_RESULT_USERS = 200;

    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    /**
     * 根据部门ID查找部门负责人
     *
     * @param deptId 部门ID
     * @return 部门负责人的用户ID，如果未找到返回 null
     */
    public String findDeptManager(Object deptId) {
        log.debug("SPEL: 查找部门负责人");

        if (deptId == null || flowOrgIntegrationService == null) {
            log.warn("SPEL: 部门ID为空或组织服务未注入，无法查找负责人");
            return null;
        }

        try {
            List<String> managerIds = flowOrgIntegrationService.getDeptManagerByDeptId(deptId.toString());
            return managerIds.isEmpty() ? null : managerIds.get(0);
        } catch (Exception e) {
            log.error("SPEL: 查找部门负责人失败", e);
            return null;
        }
    }

    /**
     * 根据角色标识查找所有具有该角色的用户
     *
     * @param roleKey 角色标识（如 'dept_manager', 'hr', 'finance'）
     * @return 用户ID列表，多个用户用逗号分隔（用于会签）
     */
    public String findUsersByRole(Object roleKey) {
        log.debug("SPEL: 查找角色用户");

        if (roleKey == null || flowOrgIntegrationService == null) {
            log.warn("SPEL: 角色标识为空或服务未注入，无法查找用户");
            return null;
        }

        try {
            List<String> userIds = flowOrgIntegrationService.getUserIdsByRoleCode(roleKey.toString());
            if (userIds == null || userIds.isEmpty()) {
                log.warn("SPEL: 角色下没有有效用户");
                return null;
            }
            return userIds.stream().limit(MAX_RESULT_USERS).collect(Collectors.joining(","));
        } catch (Exception e) {
            log.error("SPEL: 查找角色用户失败", e);
            return null;
        }
    }

    /**
     * 根据流程用户组编码查找用户，供 candidateUsers 或会签表达式使用。
     * 禁用用户组和禁用成员由组织集成服务在租户边界内过滤。
     */
    public String findUsersByGroup(Object groupCode) {
        log.debug("SPEL: 查找流程用户组用户");

        if (groupCode == null || flowOrgIntegrationService == null) {
            log.warn("SPEL: 用户组编码为空或组织服务未注入，无法查找用户");
            return null;
        }
        try {
            List<String> userIds = flowOrgIntegrationService.getUserIdsByGroupCode(groupCode.toString());
            if (userIds == null || userIds.isEmpty()) {
                log.warn("SPEL: 用户组没有有效成员");
                return null;
            }
            return userIds.stream().limit(MAX_RESULT_USERS).collect(Collectors.joining(","));
        } catch (Exception e) {
            log.error("SPEL: 查找流程用户组用户失败", e);
            return null;
        }
    }

    /**
     * 根据行政区划代码查找区域负责人
     *
     * @param regionCode 行政区划代码
     * @return 区域负责人的用户ID
     */
    public String findRegionManager(Object regionCode) {
        log.debug("SPEL: 查找区域负责人");

        if (regionCode == null || flowOrgIntegrationService == null) {
            log.warn("SPEL: 区划代码为空或用户服务未注入，无法查找负责人");
            return null;
        }

        try {
            List<String> users = flowOrgIntegrationService.getUserIdsByRegionCode(regionCode.toString());

            if (users.isEmpty()) {
                log.warn("SPEL: 未找到区域负责人");
                return null;
            }

            // 返回第一个用户作为区域负责人
            return users.get(0);
        } catch (Exception e) {
            log.error("SPEL: 查找区域负责人失败", e);
            return null;
        }
    }

    /**
     * 根据用户ID查找其直属上级
     *
     * @param userId 用户ID
     * @return 直属上级的用户ID
     */
    public String findUserLeader(Object userId) {
        log.debug("SPEL: 查找用户上级");

        if (userId == null || flowOrgIntegrationService == null) {
            log.warn("SPEL: 用户ID为空或组织集成服务未注入，无法查找上级");
            return null;
        }

        try {
            // 使用FlowOrgIntegrationService查找上级
            String leaderId = flowOrgIntegrationService.getLeaderUserIdByLevel(userId.toString(), 1);

            if (leaderId == null) {
                // 如果按层级查找失败，尝试获取所有上级的第一个
                List<String> leaderIds = flowOrgIntegrationService.getLeaderUserIds(userId.toString());
                leaderId = leaderIds.isEmpty() ? null : leaderIds.get(0);
            }

            return leaderId;
        } catch (Exception e) {
            log.error("SPEL: 查找用户上级失败", e);
            return null;
        }
    }

    /**
     * 根据部门ID和角色标识查找用户
     *
     * @param deptId 部门ID
     * @param roleKey 角色标识
     * @return 用户ID列表，多个用户用逗号分隔
     */
    public String findUsersByDeptAndRole(Object deptId, Object roleKey) {
        log.debug("SPEL: 查找部门角色用户");

        if (deptId == null || roleKey == null || flowOrgIntegrationService == null) {
            log.warn("SPEL: 部门ID或角色标识为空或服务未注入，无法查找用户");
            return null;
        }

        try {
            List<String> filteredUserIds = flowOrgIntegrationService.getUserIdsByDeptAndRoleCode(
                    deptId.toString(), roleKey.toString());
            if (filteredUserIds == null || filteredUserIds.isEmpty()) {
                log.warn("SPEL: 部门角色下没有有效用户");
                return null;
            }
            return filteredUserIds.stream().limit(MAX_RESULT_USERS).collect(Collectors.joining(","));
        } catch (Exception e) {
            log.error("SPEL: 查找部门角色用户失败", e);
            return null;
        }
    }

    /**
     * 根据流程变量动态查找审批人
     * <p>支持复杂的业务逻辑，例如根据订单金额、订单类型等条件动态确定审批人</p>
     *
     * @param execution 流程执行上下文
     * @param businessType 业务类型（如 'order', 'contract', 'leave'）
     * @return 审批人用户ID
     */
    public String findApproverByBusinessRule(DelegateExecution execution, String businessType) {
        log.debug("SPEL: 根据业务规则查找审批人");

        if (execution == null || businessType == null) {
            log.warn("SPEL: 执行上下文或业务类型为空");
            return null;
        }

        try {
            Map<String, Object> variables = execution.getVariables();
            log.debug("SPEL: 已读取流程变量用于业务规则判断, variableCount={}", variables.size());

            // 根据业务类型实现不同的审批规则
            if ("order".equals(businessType)) {
                // 订单审批：根据金额确定审批层级
                Object amountObj = variables.get("orderAmount");
                if (amountObj != null) {
                    double amount = Double.parseDouble(amountObj.toString());
                    if (amount > 10000) {
                        return findUsersByRole("finance_director");
                    } else if (amount > 5000) {
                        return findUsersByRole("finance_manager");
                    } else {
                        return findUsersByRole("finance_staff");
                    }
                }
            } else if ("leave".equals(businessType)) {
                // 请假审批：根据天数确定审批层级
                Object daysObj = variables.get("leaveDays");
                if (daysObj != null) {
                    int days = Integer.parseInt(daysObj.toString());
                    if (days > 7) {
                        return findUsersByRole("hr_director");
                    } else {
                        String userId = (String) variables.get("startUserId");
                        return findUserLeader(userId);
                    }
                }
            } else if ("contract".equals(businessType)) {
                // 合同审批：根据金额确定审批层级
                Object amountObj = variables.get("contractAmount");
                if (amountObj != null) {
                    double amount = Double.parseDouble(amountObj.toString());
                    if (amount > 100000) {
                        return findUsersByRole("ceo");
                    } else if (amount > 50000) {
                        return findUsersByRole("dept_director");
                    } else {
                        return findUsersByRole("dept_manager");
                    }
                }
            }

            log.warn("SPEL: 未匹配到业务规则");
            return null;
        } catch (Exception e) {
            log.error("SPEL: 根据业务规则查找审批人失败", e);
            return null;
        }
    }

    /**
     * 获取流程发起人
     *
     * @param execution 流程执行上下文
     * @return 发起人用户ID
     */
    public String getInitiator(DelegateExecution execution) {
        if (execution == null) {
            log.warn("SPEL: 执行上下文为空");
            return null;
        }

        try {
            Object initiator = execution.getVariable("initiator");
            log.debug("SPEL: 已读取流程发起人变量");
            return initiator != null ? initiator.toString() : null;
        } catch (Exception e) {
            log.error("SPEL: 获取流程发起人失败", e);
            return null;
        }
    }

    /**
     * 获取流程发起人的上级
     *
     * @param execution 流程执行上下文
     * @return 发起人上级的用户ID
     */
    public String getInitiatorLeader(DelegateExecution execution) {
        String initiator = getInitiator(execution);
        if (initiator == null) {
            return null;
        }
        return findUserLeader(initiator);
    }

    /**
     * 根据条件表达式动态选择审批人
     * <p>支持复杂的条件判断，例如：amount > 10000 ? 'user1' : 'user2'</p>
     *
     * @param condition 条件（true/false）
     * @param trueValue 条件为真时返回的值
     * @param falseValue 条件为假时返回的值
     * @return 选择的审批人
     */
    public String conditionalAssignee(boolean condition, String trueValue, String falseValue) {
        log.debug("SPEL: 条件选择审批人, condition={}", condition);
        return condition ? trueValue : falseValue;
    }

    /**
     * 合并多个审批人列表（用于会签场景）
     *
     * @param assigneeLists 多个审批人列表
     * @return 合并后的审批人列表，用逗号分隔
     */
    public String mergeAssignees(String... assigneeLists) {
        if (assigneeLists == null || assigneeLists.length == 0) {
            return null;
        }

        List<String> allAssignees = new ArrayList<>();
        for (String list : assigneeLists) {
            if (list != null && !list.trim().isEmpty()) {
                String[] assignees = list.split(",");
                for (String assignee : assignees) {
                    String trimmed = assignee.trim();
                    if (!trimmed.isEmpty() && !allAssignees.contains(trimmed)) {
                        allAssignees.add(trimmed);
                    }
                }
            }
        }

        String result = String.join(",", allAssignees);
        return result;
    }

}
