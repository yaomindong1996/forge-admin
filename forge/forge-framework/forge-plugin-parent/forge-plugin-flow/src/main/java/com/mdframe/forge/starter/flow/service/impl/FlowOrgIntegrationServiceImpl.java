package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.*;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程组织架构集成服务实现
 * 基于系统管理模块的组织架构模型实现
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowOrgIntegrationServiceImpl implements FlowOrgIntegrationService {

    private final ISysUserService sysUserService;
    private final ISysOrgService sysOrgService;
    private final ISysRoleService sysRoleService;
    private final ISysPostService sysPostService;
    private final ISysUserOrgService sysUserOrgService;
    
    // 直接使用Mapper操作关联表
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserPostMapper sysUserPostMapper;

    @Override
    public Map<String, Object> getUserInfo(String userId) {
        log.debug("获取用户信息: userId={}", userId);
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            Long id = Long.parseLong(userId);
            SysUser user = sysUserService.getById(id);
            if (user == null) {
                return Collections.emptyMap();
            }
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", String.valueOf(user.getId()));
            userInfo.put("username", user.getUsername());
            userInfo.put("realName", user.getRealName());
            userInfo.put("name", user.getRealName());
            userInfo.put("userStatus", user.getUserStatus());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhone());
            
            // 获取用户主组织
            String deptId = getUserDeptId(userId);
            userInfo.put("deptId", deptId);
            
            // 获取用户主岗位
            String postId = getUserMainPostId(userId);
            userInfo.put("postId", postId);
            
            return userInfo;
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID格式: {}", userId);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<String> getLeaderUserIds(String userId) {
        log.debug("获取上级领导: userId={}", userId);
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 获取用户所在部门的负责人作为上级领导
        List<String> managerIds = getDeptManagerUserIds(userId);
        
        // 如果部门有负责人，返回负责人
        if (!managerIds.isEmpty()) {
            return managerIds;
        }
        
        // 如果当前部门没有负责人，尝试获取上级部门的负责人
        String deptId = getUserDeptId(userId);
        if (deptId != null) {
            String parentDeptId = getParentDeptId(deptId);
            if (parentDeptId != null) {
                return getDeptManagerByDeptId(parentDeptId);
            }
        }
        
        return Collections.emptyList();
    }

    @Override
    public String getLeaderUserIdByLevel(String userId, int level) {
        log.debug("获取指定层级上级领导: userId={}, level={}", userId, level);
        if (userId == null || userId.isEmpty() || level < 1) {
            return null;
        }
        
        String currentDeptId = getUserDeptId(userId);
        int currentLevel = 0;
        
        // 从当前部门开始向上查找
        while (currentDeptId != null && currentLevel < level) {
            currentLevel++;
            
            if (currentLevel == level) {
                // 到达指定层级，返回该部门负责人
                List<String> managerIds = getDeptManagerByDeptId(currentDeptId);
                return managerIds.isEmpty() ? null : managerIds.get(0);
            }
            
            // 继续向上查找
            currentDeptId = getParentDeptId(currentDeptId);
        }
        
        return null;
    }

    @Override
    public String getDeptManagerUserIdByLevel(String userId, int level) {
        log.debug("获取指定层级部门经理: userId={}, level={}", userId, level);
        if (userId == null || userId.isEmpty() || level < 1) {
            return null;
        }
        
        String currentDeptId = getUserDeptId(userId);
        int currentLevel = 0;
        
        // 从当前部门开始向上查找
        while (currentDeptId != null && currentLevel < level) {
            currentLevel++;
            String parentDeptId = getParentDeptId(currentDeptId);
            
            if (currentLevel == level && parentDeptId != null) {
                // 到达指定层级，返回上级部门的负责人
                List<String> managerIds = getDeptManagerByDeptId(parentDeptId);
                return managerIds.isEmpty() ? null : managerIds.get(0);
            }
            
            currentDeptId = parentDeptId;
        }
        
        return null;
    }

    @Override
    public List<String> getDeptManagerUserIds(String userId) {
        log.debug("获取部门负责人: userId={}", userId);
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 1. 获取用户所在部门
        String deptId = getUserDeptId(userId);
        if (deptId == null) {
            return Collections.emptyList();
        }
        
        // 2. 获取部门负责人
        return getDeptManagerByDeptId(deptId);
    }

    @Override
    public List<String> getDeptManagerByDeptId(String deptId) {
        log.debug("获取部门负责人: deptId={}", deptId);
        if (deptId == null || deptId.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Long id = Long.parseLong(deptId);
            SysOrg org = sysOrgService.getById(id);
            if (org == null || org.getLeaderId() == null) {
                return Collections.emptyList();
            }
            
            // 返回部门负责人ID
            return Collections.singletonList(String.valueOf(org.getLeaderId()));
        } catch (NumberFormatException e) {
            log.warn("无效的部门ID格式: {}", deptId);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getUserIdsByRoleCode(String roleCode) {
        log.debug("根据角色编码获取用户: roleCode={}", roleCode);
        if (roleCode == null || roleCode.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 1. 根据角色编码查找角色
        SysRole role = sysRoleService.lambdaQuery()
                .eq(SysRole::getRoleKey, roleCode)
                .eq(SysRole::getRoleStatus, 1)
                .one();
        
        if (role == null) {
            log.debug("未找到角色: roleCode={}", roleCode);
            return Collections.emptyList();
        }
        
        // 2. 根据角色ID获取用户
        return getUserIdsByRoleId(String.valueOf(role.getId()));
    }

    @Override
    public List<String> getUserIdsByRoleId(String roleId) {
        log.debug("根据角色ID获取用户: roleId={}", roleId);
        if (roleId == null || roleId.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Long rid = Long.parseLong(roleId);
            
            // 查询用户角色关联表
            LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserRole::getRoleId, rid);
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
            
            if (userRoles.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 获取用户ID列表
            List<Long> userIds = userRoles.stream()
                    .map(SysUserRole::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 过滤有效用户
            List<SysUser> users = sysUserService.lambdaQuery()
                    .in(SysUser::getId, userIds)
                    .eq(SysUser::getUserStatus, 1)
                    .list();
            
            return users.stream()
                    .map(u -> String.valueOf(u.getId()))
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("无效的角色ID格式: {}", roleId);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getUserIdsByDeptId(String deptId) {
        log.debug("根据部门ID获取用户: deptId={}", deptId);
        if (deptId == null || deptId.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Long oid = Long.parseLong(deptId);
            
            // 查询用户组织关联表
            List<SysUserOrg> userOrgs = sysUserOrgService.lambdaQuery()
                    .eq(SysUserOrg::getOrgId, oid)
                    .list();
            
            if (userOrgs.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 获取用户ID列表
            List<Long> userIds = userOrgs.stream()
                    .map(SysUserOrg::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 过滤有效用户
            List<SysUser> users = sysUserService.lambdaQuery()
                    .in(SysUser::getId, userIds)
                    .eq(SysUser::getUserStatus, 1)
                    .list();
            
            return users.stream()
                    .map(u -> String.valueOf(u.getId()))
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("无效的部门ID格式: {}", deptId);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getUserIdsByDeptAndPost(String deptId, String postId) {
        log.debug("根据部门和岗位获取用户: deptId={}, postId={}", deptId, postId);
        if (deptId == null || deptId.isEmpty() || postId == null || postId.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Long oid = Long.parseLong(deptId);
            Long pid = Long.parseLong(postId);
            
            // 1. 获取部门下的用户
            List<String> deptUserIds = getUserIdsByDeptId(deptId);
            if (deptUserIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 2. 获取岗位下的用户
            LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserPost::getPostId, pid);
            List<SysUserPost> userPosts = sysUserPostMapper.selectList(wrapper);
            
            if (userPosts.isEmpty()) {
                return Collections.emptyList();
            }
            
            Set<String> postUserIds = userPosts.stream()
                    .map(up -> String.valueOf(up.getUserId()))
                    .collect(Collectors.toSet());
            
            // 3. 取交集
            return deptUserIds.stream()
                    .filter(postUserIds::contains)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("无效的部门ID或岗位ID格式: deptId={}, postId={}", deptId, postId);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getUserIdsByPostId(String postId) {
        log.debug("根据岗位ID获取用户: postId={}", postId);
        if (postId == null || postId.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Long pid = Long.parseLong(postId);
            
            // 查询用户岗位关联表
            LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserPost::getPostId, pid);
            List<SysUserPost> userPosts = sysUserPostMapper.selectList(wrapper);
            
            if (userPosts.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 获取用户ID列表
            List<Long> userIds = userPosts.stream()
                    .map(SysUserPost::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 过滤有效用户
            List<SysUser> users = sysUserService.lambdaQuery()
                    .in(SysUser::getId, userIds)
                    .eq(SysUser::getUserStatus, 1)
                    .list();
            
            return users.stream()
                    .map(u -> String.valueOf(u.getId()))
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("无效的岗位ID格式: {}", postId);
            return Collections.emptyList();
        }
    }

    @Override
    public String getUserDeptId(String userId) {
        log.debug("获取用户部门ID: userId={}", userId);
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        
        try {
            Long uid = Long.parseLong(userId);
            
            // 查询用户主组织
            SysUserOrg userOrg = sysUserOrgService.lambdaQuery()
                    .eq(SysUserOrg::getUserId, uid)
                    .eq(SysUserOrg::getIsMain, 1)
                    .one();
            
            if (userOrg == null) {
                // 如果没有主组织，取第一个组织
                userOrg = sysUserOrgService.lambdaQuery()
                        .eq(SysUserOrg::getUserId, uid)
                        .last("LIMIT 1")
                        .one();
            }
            
            return userOrg != null ? String.valueOf(userOrg.getOrgId()) : null;
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID格式: {}", userId);
            return null;
        }
    }

    @Override
    public String getUserDeptName(String userId) {
        log.debug("获取用户部门名称: userId={}", userId);
        String deptId = getUserDeptId(userId);
        if (deptId == null) {
            return null;
        }
        
        try {
            Long oid = Long.parseLong(deptId);
            SysOrg org = sysOrgService.getById(oid);
            return org != null ? org.getOrgName() : null;
        } catch (NumberFormatException e) {
            log.warn("无效的部门ID格式: {}", deptId);
            return null;
        }
    }

    @Override
    public String getParentDeptId(String deptId) {
        log.debug("获取上级部门ID: deptId={}", deptId);
        if (deptId == null || deptId.isEmpty()) {
            return null;
        }
        
        try {
            Long oid = Long.parseLong(deptId);
            SysOrg org = sysOrgService.getById(oid);
            if (org == null || org.getParentId() == null || org.getParentId() == 0L) {
                return null;
            }
            return String.valueOf(org.getParentId());
        } catch (NumberFormatException e) {
            log.warn("无效的部门ID格式: {}", deptId);
            return null;
        }
    }

    @Override
    public List<String> getChildDeptIds(String deptId, boolean recursive) {
        log.debug("获取子部门ID列表: deptId={}, recursive={}", deptId, recursive);
        if (deptId == null || deptId.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Long oid = Long.parseLong(deptId);
            
            if (recursive) {
                // 递归获取所有子部门
                SysOrg org = sysOrgService.getById(oid);
                if (org == null) {
                    return Collections.emptyList();
                }
                
                // 使用ancestors字段查询所有子部门
                List<SysOrg> childOrgs = sysOrgService.lambdaQuery()
                        .likeRight(SysOrg::getAncestors, org.getAncestors() + "," + oid)
                        .eq(SysOrg::getOrgStatus, 1)
                        .list();
                
                return childOrgs.stream()
                        .map(o -> String.valueOf(o.getId()))
                        .collect(Collectors.toList());
            } else {
                // 只获取直接子部门
                List<SysOrg> childOrgs = sysOrgService.lambdaQuery()
                        .eq(SysOrg::getParentId, oid)
                        .eq(SysOrg::getOrgStatus, 1)
                        .list();
                
                return childOrgs.stream()
                        .map(o -> String.valueOf(o.getId()))
                        .collect(Collectors.toList());
            }
        } catch (NumberFormatException e) {
            log.warn("无效的部门ID格式: {}", deptId);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasRole(String userId, String roleCode) {
        log.debug("判断用户是否拥有角色: userId={}, roleCode={}", userId, roleCode);
        if (userId == null || userId.isEmpty() || roleCode == null || roleCode.isEmpty()) {
            return false;
        }
        
        try {
            Long uid = Long.parseLong(userId);
            
            // 获取用户角色ID列表
            LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserRole::getUserId, uid);
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
            
            if (userRoles.isEmpty()) {
                return false;
            }
            
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            
            // 检查是否有匹配的角色
            long count = sysRoleService.lambdaQuery()
                    .in(SysRole::getId, roleIds)
                    .eq(SysRole::getRoleKey, roleCode)
                    .eq(SysRole::getRoleStatus, 1)
                    .count();
            
            return count > 0;
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID格式: {}", userId);
            return false;
        }
    }
    
    @Override
    public List<String> getUserRoleCodes(String userId) {
        log.debug("获取用户角色编码列表: userId={}", userId);
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            Long uid = Long.parseLong(userId);
            
            // 获取用户角色ID列表
            LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserRole::getUserId, uid);
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(wrapper);
            
            if (userRoles.isEmpty()) {
                return Collections.emptyList();
            }
            
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            
            // 获取角色编码列表
            List<SysRole> roles = sysRoleService.lambdaQuery()
                    .in(SysRole::getId, roleIds)
                    .eq(SysRole::getRoleStatus, 1)
                    .list();
            
            return roles.stream()
                    .map(SysRole::getRoleKey)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID格式: {}", userId);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isInDept(String userId, String deptId) {
        log.debug("判断用户是否在部门: userId={}, deptId={}", userId, deptId);
        if (userId == null || userId.isEmpty() || deptId == null || deptId.isEmpty()) {
            return false;
        }
        
        try {
            Long uid = Long.parseLong(userId);
            Long oid = Long.parseLong(deptId);
            
            long count = sysUserOrgService.lambdaQuery()
                    .eq(SysUserOrg::getUserId, uid)
                    .eq(SysUserOrg::getOrgId, oid)
                    .count();
            
            return count > 0;
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID或部门ID格式: userId={}, deptId={}", userId, deptId);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getUserList(String keyword, String deptId) {
        log.debug("获取用户列表: keyword={}, deptId={}", keyword, deptId);
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserStatus, 1);
        
        // 关键字搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getRealName, keyword)
            );
        }
        
        List<SysUser> users;
        
        // 部门过滤
        if (deptId != null && !deptId.isEmpty()) {
            try {
                Long oid = Long.parseLong(deptId);
                List<SysUserOrg> userOrgs = sysUserOrgService.lambdaQuery()
                        .eq(SysUserOrg::getOrgId, oid)
                        .list();
                
                if (userOrgs.isEmpty()) {
                    return Collections.emptyList();
                }
                
                List<Long> userIds = userOrgs.stream()
                        .map(SysUserOrg::getUserId)
                        .collect(Collectors.toList());
                
                wrapper.in(SysUser::getId, userIds);
            } catch (NumberFormatException e) {
                log.warn("无效的部门ID格式: {}", deptId);
            }
        }
        
        users = sysUserService.list(wrapper);
        
        return users.stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", String.valueOf(user.getId()));
                    map.put("username", user.getUsername());
                    map.put("name", user.getRealName());
                    map.put("avatar", user.getAvatar());
                    map.put("deptId", getUserDeptId(String.valueOf(user.getId())));
                    map.put("deptName", getUserDeptName(String.valueOf(user.getId())));
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getDeptTree() {
        log.debug("获取部门树");
        
        // 获取所有有效组织
        List<SysOrg> orgs = sysOrgService.lambdaQuery()
                .eq(SysOrg::getOrgStatus, 1)
                .orderByAsc(SysOrg::getSort)
                .list();
        
        // 构建树形结构
        return buildOrgTree(orgs, 0L);
    }

    /**
     * 递归构建组织树
     */
    private List<Map<String, Object>> buildOrgTree(List<SysOrg> allOrgs, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (SysOrg org : allOrgs) {
            if (Objects.equals(org.getParentId(), parentId)) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", String.valueOf(org.getId()));
                node.put("name", org.getOrgName());
                node.put("parentId", String.valueOf(org.getParentId()));
                node.put("orgType", org.getOrgType());
                node.put("leaderId", org.getLeaderId() != null ? String.valueOf(org.getLeaderId()) : null);
                node.put("leaderName", org.getLeaderName());
                
                // 递归获取子节点
                List<Map<String, Object>> children = buildOrgTree(allOrgs, org.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                
                result.add(node);
            }
        }
        
        return result;
    }

    @Override
    public List<Map<String, Object>> getRoleList() {
        log.debug("获取角色列表");
        
        List<SysRole> roles = sysRoleService.lambdaQuery()
                .eq(SysRole::getRoleStatus, 1)
                .orderByAsc(SysRole::getSort)
                .list();
        
        return roles.stream()
                .map(role -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", String.valueOf(role.getId()));
                    map.put("roleName", role.getRoleName());
                    map.put("roleKey", role.getRoleKey());
                    map.put("dataScope", role.getDataScope());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getPostList(String deptId) {
        log.debug("获取岗位列表: deptId={}", deptId);
        
        LambdaQueryWrapper<SysPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPost::getPostStatus, 1);
        
        if (deptId != null && !deptId.isEmpty()) {
            try {
                Long oid = Long.parseLong(deptId);
                wrapper.eq(SysPost::getOrgId, oid);
            } catch (NumberFormatException e) {
                log.warn("无效的部门ID格式: {}", deptId);
            }
        }
        
        wrapper.orderByAsc(SysPost::getSort);
        
        List<SysPost> posts = sysPostService.list(wrapper);
        
        return posts.stream()
                .map(post -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", String.valueOf(post.getId()));
                    map.put("postName", post.getPostName());
                    map.put("postCode", post.getPostCode());
                    map.put("postType", post.getPostType());
                    map.put("orgId", String.valueOf(post.getOrgId()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户主岗位ID
     */
    private String getUserMainPostId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        
        try {
            Long uid = Long.parseLong(userId);
            
            LambdaQueryWrapper<SysUserPost> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUserPost::getUserId, uid)
                   .eq(SysUserPost::getIsMain, 1);
            SysUserPost userPost = sysUserPostMapper.selectOne(wrapper);
            
            if (userPost == null) {
                wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SysUserPost::getUserId, uid)
                       .last("LIMIT 1");
                userPost = sysUserPostMapper.selectOne(wrapper);
            }
            
            return userPost != null ? String.valueOf(userPost.getPostId()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
