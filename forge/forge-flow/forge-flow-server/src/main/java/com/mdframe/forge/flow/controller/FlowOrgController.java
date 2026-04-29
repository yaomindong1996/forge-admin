package com.mdframe.forge.flow.controller;

import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.tenant.IgnoreTenant;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程组织架构接口
 * 提供审批人配置所需的组织架构相关数据
 */
@RestController
@RequestMapping("/api/flow/org")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@IgnoreTenant
public class FlowOrgController {

    private final FlowOrgIntegrationService flowOrgIntegrationService;

    /**
     * 获取用户列表
     * @param keyword 关键字（用户名/姓名）
     * @param deptId 部门ID（可选）
     * @return 用户列表
     */
    @GetMapping("/users")
    public RespInfo<List<Map<String, Object>>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deptId) {
        List<Map<String, Object>> users = flowOrgIntegrationService.getUserList(keyword, deptId);
        return RespInfo.success(users);
    }

    /**
     * 获取部门树
     * @return 部门树结构
     */
    @GetMapping("/deptTree")
    public RespInfo<List<Map<String, Object>>> getDeptTree() {
        List<Map<String, Object>> deptTree = flowOrgIntegrationService.getDeptTree();
        return RespInfo.success(deptTree);
    }

    /**
     * 获取角色列表
     * @return 角色列表
     */
    @GetMapping("/roles")
    public RespInfo<List<Map<String, Object>>> getRoleList() {
        List<Map<String, Object>> roles = flowOrgIntegrationService.getRoleList();
        return RespInfo.success(roles);
    }

    /**
     * 获取岗位列表
     * @param deptId 部门ID（可选）
     * @return 岗位列表
     */
    @GetMapping("/posts")
    public RespInfo<List<Map<String, Object>>> getPostList(
            @RequestParam(required = false) String deptId) {
        List<Map<String, Object>> posts = flowOrgIntegrationService.getPostList(deptId);
        return RespInfo.success(posts);
    }

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/user/{userId}")
    public RespInfo<Map<String, Object>> getUserInfo(@PathVariable String userId) {
        Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(userId);
        return RespInfo.success(userInfo);
    }

    /**
     * 根据用户ID获取上级领导用户ID列表
     * @param userId 用户ID
     * @return 上级领导用户ID列表
     */
    @GetMapping("/user/{userId}/leaders")
    public RespInfo<List<String>> getLeaderUserIds(@PathVariable String userId) {
        List<String> leaders = flowOrgIntegrationService.getLeaderUserIds(userId);
        return RespInfo.success(leaders);
    }

    /**
     * 根据用户ID获取部门负责人用户ID列表
     * @param userId 用户ID
     * @return 部门负责人用户ID列表
     */
    @GetMapping("/user/{userId}/deptManagers")
    public RespInfo<List<String>> getDeptManagerUserIds(@PathVariable String userId) {
        List<String> managers = flowOrgIntegrationService.getDeptManagerUserIds(userId);
        return RespInfo.success(managers);
    }

    /**
     * 根据部门ID获取部门下所有用户ID列表
     * @param deptId 部门ID
     * @return 用户ID列表
     */
    @GetMapping("/dept/{deptId}/users")
    public RespInfo<List<String>> getUserIdsByDeptId(@PathVariable String deptId) {
        List<String> userIds = flowOrgIntegrationService.getUserIdsByDeptId(deptId);
        return RespInfo.success(userIds);
    }

    /**
     * 根据角色编码获取用户ID列表
     * @param roleCode 角色编码
     * @return 用户ID列表
     */
    @GetMapping("/role/{roleCode}/users")
    public RespInfo<List<String>> getUserIdsByRoleCode(@PathVariable String roleCode) {
        List<String> userIds = flowOrgIntegrationService.getUserIdsByRoleCode(roleCode);
        return RespInfo.success(userIds);
    }

    /**
     * 根据岗位ID获取用户ID列表
     * @param postId 岗位ID
     * @return 用户ID列表
     */
    @GetMapping("/post/{postId}/users")
    public RespInfo<List<String>> getUserIdsByPostId(@PathVariable String postId) {
        List<String> userIds = flowOrgIntegrationService.getUserIdsByPostId(postId);
        return RespInfo.success(userIds);
    }
}