package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysOrgDTO;
import com.mdframe.forge.plugin.system.dto.SysOrgQuery;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.system.service.ISysOrgService;
import com.mdframe.forge.plugin.system.vo.SysOrgTreeVO;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import com.mdframe.forge.starter.trans.annotation.DictTranslate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组织Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysOrgServiceImpl extends ServiceImpl<SysOrgMapper, SysOrg> implements ISysOrgService {

    private static final String[] ORG_READ_PERMISSIONS = {
            "system:org:list", "system:org:query",
            "system:user:list", "system:user:query",
            "system:role:list", "system:role:query"
    };

    private final SysOrgMapper orgMapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserOrgMapper userOrgMapper;
    private final IUserLoadService userLoadService;

    @Override
    @DictTranslate
    public IPage<SysOrg> selectOrgPage(SysOrgQuery query) {
        query = normalizeOrgQueryTenant(query);
        Page<SysOrg> page = new Page<>(query.getPageNum(), query.getPageSize());
        SysOrgQuery finalQuery = query;
        return TenantContextHolder.executeIgnore(() -> orgMapper.selectOrgPage(page, finalQuery));
    }

    @Override
    @DictTranslate
    public List<SysOrg> selectOrgTree(SysOrgQuery query) {
        query = normalizeOrgQueryTenant(query);
        SysOrgQuery finalQuery = query;
        List<SysOrg> allOrgs = TenantContextHolder.executeIgnore(() -> orgMapper.selectOrgList(finalQuery));
        return buildTree(allOrgs);
    }
    
    @Override
    @DictTranslate
    public List<SysOrgTreeVO> selectOrgLazyTree(SysOrgQuery query) {
        query = normalizeOrgQueryTenant(query);
        SysOrgQuery finalQuery = query;
        List<SysOrgTreeVO> allOrgs = TenantContextHolder.executeIgnore(() -> orgMapper.selectOrgLazyTree(finalQuery));
        if (query != null && query.getParentId() != null) {
            return allOrgs;
        }
        return buildLazyRootNodes(allOrgs);
    }
    
    @Override
    @DictTranslate
    public List<SysOrgTreeVO> selectOrgChildrenByParentId(Long parentId) {
        return selectOrgChildrenByParentId(parentId, null);
    }

    @Override
    @DictTranslate
    public List<SysOrgTreeVO> selectOrgChildrenByParentId(Long parentId, Long tenantId) {
        Long operationTenantId = resolveQueryTenantId(tenantId);
        return TenantContextHolder.executeIgnore(() -> orgMapper.selectOrgChildrenByParentId(parentId, operationTenantId));
    }

    @Override
    @DictTranslate
    public List<SysOrgTreeVO> selectCurrentUserOrgOptions() {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.getTenantId() == null) {
            return List.of();
        }
        List<Long> orgIds = selectCurrentUserBoundOrgIds(loginUser);
        if (orgIds.isEmpty() && loginUser.isAdmin()) {
            SysOrgQuery query = new SysOrgQuery();
            query.setTenantId(loginUser.getTenantId());
            return selectOrgTree(query).stream()
                    .map(this::toOrgTreeVO)
                    .toList();
        }
        if (orgIds.isEmpty()) {
            return List.of();
        }
        List<SysOrg> orgs = TenantContextHolder.executeIgnore(() ->
                orgMapper.selectEnabledOrgsByIds(loginUser.getTenantId(), orgIds));
        return orgs.stream()
                .map(this::toOrgTreeVO)
                .toList();
    }

    @Override
    public LoginUser switchCurrentOrg(Long orgId) {
        if (orgId == null) {
            throw new RuntimeException("组织ID不能为空");
        }
        LoginUser currentUser = requireLoginUser();
        SysOrg org = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(orgId));
        if (org == null || !Objects.equals(org.getTenantId(), currentUser.getTenantId())) {
            throw new RuntimeException("组织不存在或不属于当前租户");
        }
        List<Long> boundOrgIds = selectCurrentUserBoundOrgIds(currentUser);
        if (boundOrgIds.isEmpty() && !currentUser.isAdmin()) {
            throw new RuntimeException("当前用户未加入任何组织");
        }
        if (!boundOrgIds.isEmpty() && !boundOrgIds.contains(orgId)) {
            throw new RuntimeException("当前用户未加入该组织");
        }

        LoginUser switchedUser = userLoadService.loadUserByUserId(
                currentUser.getUserId(), currentUser.getTenantId(), orgId);
        switchedUser.setLoginTime(currentUser.getLoginTime());
        switchedUser.setLoginIp(currentUser.getLoginIp());
        switchedUser.setUserClient(currentUser.getUserClient());
        SessionHelper.setLoginUser(switchedUser);
        return switchedUser;
    }

    private List<Long> selectCurrentUserBoundOrgIds(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null || loginUser.getTenantId() == null) {
            return List.of();
        }
        List<Long> orgIds = TenantContextHolder.executeIgnore(() ->
                userOrgMapper.selectOrgIdsByUserTenant(loginUser.getTenantId(), loginUser.getUserId()));
        if (orgIds == null || orgIds.isEmpty()) {
            return List.of();
        }
        return orgIds.stream()
                .distinct()
                .toList();
    }

    private SysOrgTreeVO toOrgTreeVO(SysOrg org) {
        SysOrgTreeVO vo = new SysOrgTreeVO();
        vo.setId(org.getId());
        vo.setTenantId(org.getTenantId());
        vo.setOrgName(org.getOrgName());
        vo.setParentId(org.getParentId());
        vo.setAncestors(org.getAncestors());
        vo.setSort(org.getSort());
        vo.setOrgType(org.getOrgType());
        vo.setOrgStatus(org.getOrgStatus());
        vo.setLeaderId(org.getLeaderId());
        vo.setLeaderName(org.getLeaderName());
        vo.setPhone(org.getPhone());
        vo.setAddress(org.getAddress());
        vo.setRegionCode(org.getRegionCode());
        vo.setRemark(org.getRemark());
        List<SysOrg> children = org.getChildren();
        if (children != null && !children.isEmpty()) {
            vo.setChildren(children.stream()
                    .map(this::toOrgTreeVO)
                    .toList());
            vo.setHasChildren(true);
        } else {
            vo.setChildren(null);
            vo.setHasChildren(false);
        }
        return vo;
    }

    /**
     * 构建树形结构
     * 自动识别根节点：parentId 不在结果集中的节点即为根节点
     * 这样即使数据权限过滤掉了顶级组织，下级组织仍能正常构建为树
     */
    private List<SysOrg> buildTree(List<SysOrg> allOrgs) {
        if (allOrgs == null || allOrgs.isEmpty()) {
            return allOrgs;
        }
        // 收集所有节点ID
        Set<Long> allIds = allOrgs.stream()
                .map(SysOrg::getId)
                .collect(Collectors.toSet());
        // 找出根节点：parentId 不在结果集中的节点
        return allOrgs.stream()
                .filter(org -> !allIds.contains(org.getParentId()))
                .peek(org -> {
                    List<SysOrg> children = buildChildren(allOrgs, org.getId());
                    org.setChildren(children.isEmpty() ? null : children);
                })
                .toList();
    }

    /**
     * 递归构建子节点
     */
    private List<SysOrg> buildChildren(List<SysOrg> allOrgs, Long parentId) {
        return allOrgs.stream()
                .filter(org -> org.getParentId().equals(parentId))
                .peek(org -> {
                    List<SysOrg> children = buildChildren(allOrgs, org.getId());
                    org.setChildren(children.isEmpty() ? null : children);
                })
                .toList();
    }

    /**
     * 懒加载根节点按当前可见结果集识别。
     * 数据权限过滤掉顶级组织时，父节点不可见的下级组织需要作为当前视图根节点返回。
     */
    private List<SysOrgTreeVO> buildLazyRootNodes(List<SysOrgTreeVO> allOrgs) {
        if (allOrgs == null || allOrgs.isEmpty()) {
            return allOrgs;
        }
        Set<Long> visibleIds = allOrgs.stream()
                .map(SysOrgTreeVO::getId)
                .collect(Collectors.toSet());
        Set<Long> visibleParentIds = allOrgs.stream()
                .map(SysOrgTreeVO::getParentId)
                .filter(parentId -> parentId != null && visibleIds.contains(parentId))
                .collect(Collectors.toSet());
        return allOrgs.stream()
                .filter(org -> isLazyRootNode(org, visibleIds))
                .peek(org -> {
                    org.setChildren(null);
                    if (visibleParentIds.contains(org.getId())) {
                        org.setHasChildren(true);
                    }
                })
                .toList();
    }

    private boolean isLazyRootNode(SysOrgTreeVO org, Set<Long> visibleIds) {
        Long parentId = org.getParentId();
        return parentId == null || parentId == 0L || !visibleIds.contains(parentId);
    }

    @Override
    public SysOrg selectOrgById(Long id) {
        SysOrg org = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(id));
        if (org != null) {
            assertTenantReadable(org.getTenantId());
        }
        return org;
    }

    @Override
    public boolean insertOrg(SysOrgDTO dto) {
        assertOrgWriteAllowed("system:org:add");
        SysOrg org = new SysOrg();
        BeanUtil.copyProperties(dto, org);
        Long tenantId = resolveWriteTenantId(dto.getTenantId(), null);
        org.setTenantId(tenantId);

        if (org.getParentId() == null || org.getParentId() == 0) {
            org.setAncestors("0");
        } else {
            SysOrg parentOrg = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(org.getParentId()));
            if (parentOrg != null) {
                assertSameTenant(parentOrg.getTenantId(), tenantId, "上级组织不属于当前操作租户");
                org.setAncestors(parentOrg.getAncestors() + "," + org.getParentId());
            } else {
                org.setAncestors("0");
            }
        }

        return TenantContextHolder.executeIgnore(() -> orgMapper.insert(org) > 0);
    }

    @Override
    public boolean updateOrg(SysOrgDTO dto) {
        assertOrgWriteAllowed("system:org:edit");
        SysOrg existing = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(dto.getId()));
        if (existing == null) {
            return false;
        }
        assertTenantReadable(existing.getTenantId());
        SysOrg org = new SysOrg();
        BeanUtil.copyProperties(dto, org);
        Long tenantId = resolveWriteTenantId(dto.getTenantId(), existing.getTenantId());
        org.setTenantId(tenantId);
        if (org.getParentId() != null && org.getParentId() != 0) {
            SysOrg parentOrg = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(org.getParentId()));
            if (parentOrg != null) {
                assertSameTenant(parentOrg.getTenantId(), tenantId, "上级组织不属于当前操作租户");
            }
        }
        return TenantContextHolder.executeIgnore(() -> orgMapper.updateById(org) > 0);
    }

    @Override
    public boolean deleteOrgById(Long id) {
        assertOrgWriteAllowed("system:org:remove");
        validateOrgDeletable(id);
        return TenantContextHolder.executeIgnore(() -> orgMapper.deleteById(id) > 0);
    }

    @Override
    public List<Long> selectOrgAndChildrenIds(Long orgId) {
        List<Long> ids = orgMapper.selectOrgAndChildrenIds(orgId);
        return ids != null && !ids.isEmpty() ? ids : null;
    }

    private void validateOrgDeletable(Long id) {
        if (id == null) {
            throw new RuntimeException("组织ID不能为空");
        }
        SysOrg org = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(id));
        if (org == null) {
            throw new RuntimeException("组织不存在");
        }
        assertTenantReadable(org.getTenantId());
        Long childCount = TenantContextHolder.executeIgnore(() -> orgMapper.countChildOrgs(id));
        if (childCount != null && childCount > 0) {
            throw new RuntimeException("当前组织下存在子组织，不能删除");
        }
        Long userBindingCount = TenantContextHolder.executeIgnore(() -> orgMapper.countUserOrgBindings(id));
        if (userBindingCount != null && userBindingCount > 0) {
            throw new RuntimeException("当前组织下已绑定用户，不能删除");
        }
    }

    private SysOrgQuery normalizeOrgQueryTenant(SysOrgQuery query) {
        SysOrgQuery normalizedQuery = query == null ? new SysOrgQuery() : query;
        Long tenantId = resolveQueryTenantId(normalizedQuery.getTenantId());
        normalizedQuery.setTenantId(tenantId);
        return normalizedQuery;
    }

    private Long resolveQueryTenantId(Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
        assertOrgReadAllowed(loginUser);
        Long tenantId = loginUser.getTenantId();
        validateTenantEnabled(tenantId);
        return tenantId;
    }

    private Long resolveWriteTenantId(Long requestedTenantId, Long fallbackTenantId) {
        LoginUser loginUser = requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        validateTenantEnabled(tenantId);
        return tenantId;
    }

    private void assertTenantReadable(Long tenantId) {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin() && !Objects.equals(tenantId, loginUser.getTenantId())) {
            throw new RuntimeException("无权操作非本租户组织");
        }
    }

    private void assertSameTenant(Long actualTenantId, Long expectedTenantId, String message) {
        if (!Objects.equals(expectedTenantId, actualTenantId)) {
            throw new RuntimeException(message);
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
    }

    private void assertOrgReadAllowed(LoginUser loginUser) {
        if (loginUser.isAdmin() || loginUser.isTenantAdmin()) {
            return;
        }
        if (!hasAnyPermission(loginUser, ORG_READ_PERMISSIONS)) {
            throw new RuntimeException("无权访问组织管理功能");
        }
    }

    private void assertOrgWriteAllowed(String permission) {
        LoginUser loginUser = requireLoginUser();
        if (loginUser.isAdmin() || loginUser.isTenantAdmin()) {
            return;
        }
        if (!hasAnyPermission(loginUser, permission)) {
            throw new RuntimeException("无权维护组织信息");
        }
    }

    private boolean hasAnyPermission(LoginUser loginUser, String... permissions) {
        Set<String> userPermissions = loginUser == null ? null : loginUser.getPermissions();
        if (userPermissions == null || userPermissions.isEmpty() || permissions == null) {
            return false;
        }
        if (userPermissions.contains("*") || userPermissions.contains("*:*:*")) {
            return true;
        }
        for (String permission : permissions) {
            if (hasPermission(userPermissions, permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(Set<String> userPermissions, String permission) {
        if (permission == null || userPermissions.contains(permission)) {
            return permission != null;
        }
        int splitIndex = permission.lastIndexOf(':');
        while (splitIndex > 0) {
            String wildcardPermission = permission.substring(0, splitIndex) + ":*";
            if (userPermissions.contains(wildcardPermission)) {
                return true;
            }
            splitIndex = permission.lastIndexOf(':', splitIndex - 1);
        }
        return false;
    }

    private void validateTenantEnabled(Long tenantId) {
        if (tenantId == null) {
            throw new RuntimeException("租户不能为空");
        }
        Long count = TenantContextHolder.executeIgnore(() ->
                tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getId, tenantId)
                        .eq(SysTenant::getTenantStatus, 1)));
        if (count == null || count == 0) {
            throw new RuntimeException("租户不存在或已禁用");
        }
    }
}
