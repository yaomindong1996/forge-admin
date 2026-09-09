package com.mdframe.forge.starter.flow.service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/**
 * 流程组织架构集成服务接口
 * 提供审批人计算所需的组织架构相关功能
 *
 * @author forge
 */
public interface FlowOrgIntegrationService {

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息Map，包含id, name, deptId, postId等
     */
    Map<String, Object> getUserInfo(String userId);

    /**
     * 批量读取流程用户详情。实现必须沿用当前租户、启用状态和逻辑删除边界。
     * 默认实现保留兼容性，具体实现应覆盖为单次 SQL 查询。
     */
    default Map<String, Map<String, Object>> getUserInfoBatch(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        userIds.stream().filter(id -> id != null && !id.isBlank()).distinct().forEach(id -> {
            Map<String, Object> info = getUserInfo(id);
            if (info != null && !info.isEmpty()) {
                result.put(id, info);
            }
        });
        return result;
    }

    /** 校验用户存在、启用且属于指定租户。 */
    boolean isUserAvailableForTenant(String userId, Long tenantId);

    /**
     * 根据用户ID获取上级领导用户ID列表
     * @param userId 用户ID
     * @return 上级领导用户ID列表
     */
    List<String> getLeaderUserIds(String userId);

    /**
     * 根据用户ID和层级获取上级领导用户ID
     * @param userId 用户ID
     * @param level 层级（1=直属领导，2=二级领导，以此类推）
     * @return 上级领导用户ID
     */
    String getLeaderUserIdByLevel(String userId, int level);

    /**
     * 根据用户ID和层级获取部门经理用户ID
     * @param userId 用户ID
     * @param level 层级（1=直属部门经理，2=上级部门经理，以此类推）
     * @return 部门经理用户ID
     */
    String getDeptManagerUserIdByLevel(String userId, int level);

    /**
     * 根据用户ID获取部门负责人用户ID列表
     * @param userId 用户ID
     * @return 部门负责人用户ID列表
     */
    List<String> getDeptManagerUserIds(String userId);

    /**
     * 根据部门ID获取部门负责人用户ID列表
     * @param deptId 部门ID
     * @return 部门负责人用户ID列表
     */
    List<String> getDeptManagerByDeptId(String deptId);

    /**
     * 根据角色编码获取用户ID列表
     * @param roleCode 角色编码
     * @return 用户ID列表
     */
    List<String> getUserIdsByRoleCode(String roleCode);

    /** 根据流程用户组编码获取租户内当前有效用户。 */
    List<String> getUserIdsByGroupCode(String groupCode);

    /** 获取用户所属的流程用户组编码。 */
    List<String> getUserGroupCodes(String userId);

    /**
     * 根据角色ID获取用户ID列表
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<String> getUserIdsByRoleId(String roleId);

    /** 根据行政区划编码获取租户内有效用户。 */
    List<String> getUserIdsByRegionCode(String regionCode);

    /** 根据部门和角色编码获取租户内有效用户。 */
    List<String> getUserIdsByDeptAndRoleCode(String deptId, String roleCode);

    /**
     * 根据部门ID获取部门下所有用户ID列表
     * @param deptId 部门ID
     * @return 用户ID列表
     */
    List<String> getUserIdsByDeptId(String deptId);

    /**
     * 根据部门ID获取部门下指定岗位的用户ID列表
     * @param deptId 部门ID
     * @param postId 岗位ID
     * @return 用户ID列表
     */
    List<String> getUserIdsByDeptAndPost(String deptId, String postId);

    /**
     * 根据岗位ID获取用户ID列表
     * @param postId 岗位ID
     * @return 用户ID列表
     */
    List<String> getUserIdsByPostId(String postId);

    /**
     * 获取用户所在部门ID
     * @param userId 用户ID
     * @return 部门ID
     */
    String getUserDeptId(String userId);

    /**
     * 获取用户所在部门名称
     * @param userId 用户ID
     * @return 部门名称
     */
    String getUserDeptName(String userId);

    /**
     * 获取部门上级部门ID
     * @param deptId 部门ID
     * @return 上级部门ID
     */
    String getParentDeptId(String deptId);

    /**
     * 获取部门下所有子部门ID列表
     * @param deptId 部门ID
     * @param recursive 是否递归获取
     * @return 子部门ID列表
     */
    List<String> getChildDeptIds(String deptId, boolean recursive);

    /**
     * 判断用户是否拥有指定角色
     * @param userId 用户ID
     * @param roleCode 角色编码
     * @return 是否拥有
     */
    boolean hasRole(String userId, String roleCode);
    
    /**
     * 获取用户的角色编码列表
     * @param userId 用户ID
     * @return 角色编码列表
     */
    List<String> getUserRoleCodes(String userId);

    /**
     * 判断用户是否在指定部门
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 是否在部门
     */
    boolean isInDept(String userId, String deptId);

    /**
     * 获取用户列表（用于审批人选择）
     * @param keyword 关键字（用户名/姓名）
     * @param deptId 部门ID（可选）
     * @return 用户列表
     */
    List<Map<String, Object>> getUserList(String keyword, String deptId);

    /**
     * 获取部门树（用于部门选择）
     * @return 部门树结构
     */
    List<Map<String, Object>> getDeptTree();

    /**
     * 获取角色列表（用于角色选择）
     * @return 角色列表
     */
    List<Map<String, Object>> getRoleList();

    /**
     * 获取岗位列表（用于岗位选择）
     * @param deptId 部门ID（可选）
     * @return 岗位列表
     */
    List<Map<String, Object>> getPostList(String deptId);
}
