package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysRole;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysUserRole;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.service.ISysOrgService;
import com.mdframe.forge.plugin.system.service.ISysRoleService;
import com.mdframe.forge.plugin.system.service.ISysUserService;
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

    @Autowired(required = false)
    private ISysUserService sysUserService;

    @Autowired(required = false)
    private ISysRoleService sysRoleService;

    @Autowired(required = false)
    private ISysOrgService sysOrgService;

    @Autowired(required = false)
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired(required = false)
    private FlowOrgIntegrationService flowOrgIntegrationService;

    /**
     * 根据部门ID查找部门负责人
     *
     * @param deptId 部门ID
     * @return 部门负责人的用户ID，如果未找到返回 null
     */
    public String findDeptManager(Object deptId) {
        log.info("SPEL: 查找部门负责人, deptId={}", deptId);

        if (deptId == null || sysOrgService == null) {
            log.warn("SPEL: 部门ID为空或组织服务未注入，无法查找负责人");
            return null;
        }

        try {
            Long orgId = Long.parseLong(deptId.toString());
            SysOrg org = sysOrgService.selectOrgById(orgId);

            if (org != null && org.getLeaderId() != null) {
                String managerId = String.valueOf(org.getLeaderId());
                log.info("SPEL: 部门负责人查询结果: {}", managerId);
                return managerId;
            }

            log.warn("SPEL: 未找到部门负责人, deptId={}", deptId);
            return null;
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
        log.info("SPEL: 查找角色用户, roleKey={}", roleKey);

        if (roleKey == null || sysRoleService == null || sysUserRoleMapper == null) {
            log.warn("SPEL: 角色标识为空或服务未注入，无法查找用户");
            return null;
        }

        try {
            // 根据roleKey查找角色
            LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(SysRole::getRoleKey, roleKey.toString());
            SysRole role = sysRoleService.getOne(roleWrapper);

            if (role == null) {
                log.warn("SPEL: 未找到角色, roleKey={}", roleKey);
                return null;
            }

            // 查找该角色下的所有用户
            LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
            userRoleWrapper.eq(SysUserRole::getRoleId, role.getId());
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleWrapper);

            if (userRoles.isEmpty()) {
                log.warn("SPEL: 角色下没有用户, roleKey={}", roleKey);
                return null;
            }

            String result = userRoles.stream()
                    .map(ur -> String.valueOf(ur.getUserId()))
                    .collect(Collectors.joining(","));

            log.info("SPEL: 角色用户查询结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("SPEL: 查找角色用户失败", e);
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
        log.info("SPEL: 查找区域负责人, regionCode={}", regionCode);

        if (regionCode == null || sysUserService == null) {
            log.warn("SPEL: 区划代码为空或用户服务未注入，无法查找负责人");
            return null;
        }

        try {
            // 根据区划代码查找用户（假设用户表有regionCode字段，且该区域有负责人标识）
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getRegionCode, regionCode.toString());
            wrapper.eq(SysUser::getUserStatus, 1); // 只查询正常状态的用户
            List<SysUser> users = sysUserService.list(wrapper);

            if (users.isEmpty()) {
                log.warn("SPEL: 未找到区域负责人, regionCode={}", regionCode);
                return null;
            }

            // 返回第一个用户作为区域负责人
            String managerId = String.valueOf(users.get(0).getId());
            log.info("SPEL: 区域负责人查询结果: {}", managerId);
            return managerId;
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
        log.info("SPEL: 查找用户上级, userId={}", userId);

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

            log.info("SPEL: 用户上级查询结果: {}", leaderId);
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
        log.info("SPEL: 查找部门角色用户, deptId={}, roleKey={}", deptId, roleKey);

        if (deptId == null || roleKey == null || sysRoleService == null || sysUserRoleMapper == null || sysUserService == null) {
            log.warn("SPEL: 部门ID或角色标识为空或服务未注入，无法查找用户");
            return null;
        }

        try {
            // 根据roleKey查找角色
            LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.eq(SysRole::getRoleKey, roleKey.toString());
            SysRole role = sysRoleService.getOne(roleWrapper);

            if (role == null) {
                log.warn("SPEL: 未找到角色, roleKey={}", roleKey);
                return null;
            }

            // 查找该角色下的所有用户ID
            LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
            userRoleWrapper.eq(SysUserRole::getRoleId, role.getId());
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleWrapper);

            if (userRoles.isEmpty()) {
                log.warn("SPEL: 角色下没有用户, roleKey={}", roleKey);
                return null;
            }

            List<Long> userIds = userRoles.stream()
                    .map(SysUserRole::getUserId)
                    .collect(Collectors.toList());

            // 从用户服务中筛选出属于指定部门的用户
            // 注意：这里假设通过FlowOrgIntegrationService或直接查询用户组织关系
            List<String> filteredUserIds = new ArrayList<>();
//            for (Long userId : userIds) {
//                List<Long> selectUserOrgIds = sysUserService.selectUserOrgIds(userId);
//                if (flowOrgIntegrationService != null) {
//                    String userDeptId = flowOrgIntegrationService.getUserMainOrgId(String.valueOf(userId));
//                    if (deptId.toString().equals(userDeptId)) {
//                        filteredUserIds.add(String.valueOf(userId));
//                    }
//                }
//            }

            if (filteredUserIds.isEmpty()) {
                log.warn("SPEL: 部门角色下没有用户, deptId={}, roleKey={}", deptId, roleKey);
                return null;
            }

            String result = String.join(",", filteredUserIds);
            log.info("SPEL: 部门角色用户查询结果: {}", result);
            return result;
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
        log.info("SPEL: 根据业务规则查找审批人, businessType={}", businessType);

        if (execution == null || businessType == null) {
            log.warn("SPEL: 执行上下文或业务类型为空");
            return null;
        }

        try {
            Map<String, Object> variables = execution.getVariables();
            log.info("SPEL: 流程变量: {}", variables);

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

            log.warn("SPEL: 未匹配到业务规则, businessType={}", businessType);
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
            log.info("SPEL: 流程发起人: {}", initiator);
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
        log.info("SPEL: 条件选择审批人, condition={}, trueValue={}, falseValue={}",
                 condition, trueValue, falseValue);
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
        log.info("SPEL: 合并审批人列表: {}", result);
        return result;
    }
}
