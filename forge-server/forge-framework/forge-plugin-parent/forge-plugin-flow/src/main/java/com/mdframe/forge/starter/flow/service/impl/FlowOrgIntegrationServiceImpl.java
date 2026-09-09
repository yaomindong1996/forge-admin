package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.*;
import com.mdframe.forge.plugin.system.mapper.*;
import com.mdframe.forge.plugin.system.service.*;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import com.mdframe.forge.starter.flow.service.FlowUserGroupService;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
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

    private static final int MAX_FLOW_CANDIDATES = 200;

    private final ISysUserService sysUserService;
    private final ISysOrgService sysOrgService;
    private final ISysRoleService sysRoleService;
    private final ISysPostService sysPostService;
    private final ISysUserOrgService sysUserOrgService;
    
    // 直接使用Mapper操作关联表
    private final SysUserOrgRoleMapper sysUserOrgRoleMapper;
    private final SysUserOrgMapper sysUserOrgMapper;
    private final SysUserPostMapper sysUserPostMapper;
    private final SysPostMapper sysPostMapper;
    private final SysUserTenantMapper sysUserTenantMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOrgMapper sysOrgMapper;
    private final SysRoleMapper sysRoleMapper;
    private final FlowUserGroupService flowUserGroupService;

    @Override
    public Map<String, Object> getUserInfo(String userId) {
        log.debug("获取用户信息: userId={}", userId);
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyMap();
        }

        Long tenantId = resolveTenantId();
        if (tenantId == null || tenantId <= 0) {
            log.warn("获取用户信息时缺少可信租户上下文: userId={}", userId);
            return Collections.emptyMap();
        }
        
        try {
            Long id = Long.parseLong(userId);
            Map<String, Object> row = TenantContextHolder.executeWithTenant(tenantId,
                    () -> sysUserMapper.selectFlowUserInfo(tenantId, id));
            if (row == null || row.isEmpty()) {
                return Collections.emptyMap();
            }
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.putAll(row);
            userInfo.put("realName", firstNonBlank(textValue(row.get("realName")), textValue(row.get("name"))));
            userInfo.put("name", firstNonBlank(textValue(row.get("realName")), textValue(row.get("name"))));
            return userInfo;
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID格式: {}", userId);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Map<String, Object>> getUserInfoBatch(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Long tenantId = resolveTenantId();
        if (tenantId == null || tenantId <= 0) {
            log.warn("批量获取流程用户信息时缺少可信租户上下文");
            return Collections.emptyMap();
        }
        List<Long> ids = userIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .map(this::parsePositiveId)
                .filter(Objects::nonNull)
                .limit(MAX_FLOW_CANDIDATES)
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = TenantContextHolder.executeWithTenant(
                tenantId, () -> sysUserMapper.selectFlowUserInfoBatch(tenantId, ids));
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty() || row.get("id") == null) {
                continue;
            }
            Map<String, Object> normalized = new HashMap<>(row);
            String name = firstNonBlank(textValue(row.get("realName")), textValue(row.get("name")));
            normalized.put("realName", name);
            normalized.put("name", name);
            result.put(String.valueOf(row.get("id")), normalized);
        }
        return result;
    }

    @Override
    public boolean isUserAvailableForTenant(String userId, Long tenantId) {
        if (userId == null || userId.isBlank() || tenantId == null || tenantId <= 0) {
            return false;
        }
        try {
            Long id = Long.parseLong(userId.trim());
            SysUser user = TenantContextHolder.executeIgnore(() -> sysUserService.getById(id));
            return user != null
                    && (user.getDelFlag() == null || user.getDelFlag() == 0)
                    && EnableStatus.ENABLED.matches(user.getUserStatus())
                    && sysUserTenantMapper.countEnabledMembership(id, tenantId) > 0;
        } catch (NumberFormatException exception) {
            return false;
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
            Long tenantId = requireTenantId();
            SysOrg org = TenantContextHolder.executeWithTenant(
                    tenantId, () -> sysOrgMapper.selectFlowOrgById(tenantId, id));
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
        
        SysRole role = sysRoleMapper.selectActiveFlowRoleByKey(requireTenantId(), roleCode.trim());
        
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
            Long tenantId = requireTenantId();
            Long rid = Long.parseLong(roleId.trim());
            List<Long> userIds = sysUserOrgRoleMapper.selectUserIdsByRoleIdsAcrossOrg(tenantId, List.of(rid));
            if (userIds.isEmpty()) {
                return Collections.emptyList();
            }
            return userIds.stream().map(String::valueOf).distinct().collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("无效的角色ID格式: {}", roleId);
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getUserIdsByGroupCode(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            return Collections.emptyList();
        }
        return flowUserGroupService.resolveUserIdsByCode(groupCode.trim());
    }

    @Override
    public List<String> getUserGroupCodes(String userId) {
        Long uid = parsePositiveId(userId);
        if (uid == null) {
            return Collections.emptyList();
        }
        return flowUserGroupService.resolveGroupCodesByUserId(uid);
    }

    @Override
    public List<String> getUserIdsByRegionCode(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            return Collections.emptyList();
        }
        Long tenantId = requireTenantId();
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> sysUserMapper.selectFlowUserIdsByRegion(tenantId, regionCode.trim()))
                .stream().map(String::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<String> getUserIdsByDeptAndRoleCode(String deptId, String roleCode) {
        Long orgId = parsePositiveId(deptId);
        if (orgId == null || roleCode == null || roleCode.isBlank()) {
            return Collections.emptyList();
        }
        Long tenantId = requireTenantId();
        SysRole role = sysRoleMapper.selectActiveFlowRoleByKey(tenantId, roleCode.trim());
        if (role == null) {
            return Collections.emptyList();
        }
        List<Long> userIds = sysUserOrgRoleMapper.selectUserIdsByRoleIds(tenantId, orgId, List.of(role.getId()));
        return userIds.stream().limit(MAX_FLOW_CANDIDATES).map(String::valueOf).collect(Collectors.toList());
    }

    @Override
    public List<String> getUserIdsByDeptId(String deptId) {
        log.debug("根据部门ID获取用户: deptId={}", deptId);
        if (deptId == null || deptId.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Long tenantId = requireTenantId();
            Long oid = Long.parseLong(deptId.trim());
            return sysUserMapper.selectFlowUserIdsByOrg(tenantId, oid).stream()
                    .map(String::valueOf).distinct().collect(Collectors.toList());
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
            Long tenantId = requireTenantId();
            Long oid = Long.parseLong(deptId.trim());
            Long pid = Long.parseLong(postId.trim());
            return sysUserMapper.selectFlowUserIdsByOrgAndPost(tenantId, oid, pid).stream()
                    .map(String::valueOf).distinct().collect(Collectors.toList());
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
            Long tenantId = requireTenantId();
            Long pid = Long.parseLong(postId.trim());
            return sysUserMapper.selectFlowUserIdsByPost(tenantId, pid).stream()
                    .map(String::valueOf).distinct().collect(Collectors.toList());
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
            Long tenantId = requireTenantId();
            Long uid = Long.parseLong(userId);

            // 查询用户主组织；主组织缺失时回退到任一有效组织，所有条件在 XML 中显式绑定租户。
            SysUserOrg userOrg = TenantContextHolder.executeWithTenant(
                    tenantId, () -> sysUserOrgMapper.selectFlowMainOrgByUser(tenantId, uid));

            if (userOrg == null) {
                userOrg = TenantContextHolder.executeWithTenant(
                        tenantId, () -> sysUserOrgMapper.selectFlowAnyOrgByUser(tenantId, uid));
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
            Long tenantId = requireTenantId();
            SysOrg org = TenantContextHolder.executeWithTenant(
                    tenantId, () -> sysOrgMapper.selectFlowOrgById(tenantId, oid));
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
            Long tenantId = requireTenantId();
            SysOrg org = TenantContextHolder.executeWithTenant(
                    tenantId, () -> sysOrgMapper.selectFlowOrgById(tenantId, oid));
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
            Long tenantId = requireTenantId();
            if (recursive) {
                return sysOrgMapper.selectOrgAndChildrenIdsByTenant(oid, tenantId).stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList());
            }
            return sysOrgMapper.selectOrgChildrenByParentId(oid, tenantId).stream()
                    .map(node -> String.valueOf(node.getId()))
                    .collect(Collectors.toList());
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
            
            List<Long> roleIds = selectCurrentOrgRoleIdsByUser(uid);
            if (roleIds.isEmpty()) {
                return false;
            }

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
            
            List<Long> roleIds = selectCurrentOrgRoleIdsByUser(uid);
            if (roleIds.isEmpty()) {
                return Collections.emptyList();
            }

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

            Long tenantId = requireTenantId();
            Long count = TenantContextHolder.executeWithTenant(
                    tenantId, () -> sysUserOrgMapper.countFlowUserOrg(tenantId, uid, oid));
            
            return count != null && count > 0;
        } catch (NumberFormatException e) {
            log.warn("无效的用户ID或部门ID格式: userId={}, deptId={}", userId, deptId);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getUserList(String keyword, String deptId) {
        log.debug("获取用户列表: keyword={}, deptId={}", keyword, deptId);
        Long tenantId = requireTenantId();
        Long orgId = parsePositiveId(deptId);
        if (deptId != null && !deptId.isBlank() && orgId == null) {
            return Collections.emptyList();
        }
        return TenantContextHolder.executeWithTenant(tenantId,
                () -> sysUserMapper.selectFlowUsers(tenantId, trimToNull(keyword), orgId));
    }

    @Override
    public List<Map<String, Object>> getDeptTree() {
        log.debug("获取部门树");

        Long tenantId = requireTenantId();
        List<SysOrg> orgs = TenantContextHolder.executeWithTenant(
                tenantId, () -> sysOrgMapper.selectFlowOrgList(tenantId));
        
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

        Long tenantId = requireTenantId();
        List<SysRole> roles = TenantContextHolder.executeWithTenant(
                tenantId, () -> sysRoleMapper.selectActiveFlowRoles(tenantId));
        
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
        Long tenantId = requireTenantId();
        Long orgId = null;
        if (deptId != null && !deptId.isEmpty()) {
            orgId = parsePositiveId(deptId);
            if (orgId == null) {
                log.warn("无效的部门ID格式: {}", deptId);
                return Collections.emptyList();
            }
        }

        Long queryOrgId = orgId;
        List<SysPost> posts = TenantContextHolder.executeWithTenant(
                tenantId, () -> sysPostMapper.selectFlowPosts(tenantId, queryOrgId));
        
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

    private List<Long> selectUserIdsByCurrentOrgRole(Long roleId) {
        Long tenantId = resolveTenantId();
        Long orgId = resolveActiveOrgId();
        if (tenantId == null || orgId == null || roleId == null) {
            return Collections.emptyList();
        }
        return sysUserOrgRoleMapper.selectUserIdsByRoleIds(tenantId, orgId, List.of(roleId));
    }

    private List<Long> selectCurrentOrgRoleIdsByUser(Long userId) {
        Long tenantId = resolveTenantId();
        Long orgId = resolveActiveOrgId();
        if (tenantId == null || orgId == null || userId == null) {
            return Collections.emptyList();
        }
        return sysUserOrgRoleMapper.selectActiveRoleIdsByUserOrg(tenantId, userId, orgId);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            tenantId = SessionHelper.getTenantId();
        }
        return tenantId;
    }

    private Long requireTenantId() {
        Long tenantId = resolveTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalStateException("FLOW_ORG_TENANT_REQUIRED");
        }
        return tenantId;
    }

    private Long parsePositiveId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            log.warn("流程组织查询收到非法ID参数");
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private Long resolveActiveOrgId() {
        return SessionHelper.getActiveOrgId();
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
