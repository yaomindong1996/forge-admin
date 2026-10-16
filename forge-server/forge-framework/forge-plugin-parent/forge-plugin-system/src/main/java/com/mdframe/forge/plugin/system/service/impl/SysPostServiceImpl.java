package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysPostDTO;
import com.mdframe.forge.plugin.system.dto.SysPostQuery;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysPost;
import com.mdframe.forge.plugin.system.entity.SysTenant;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.service.ISysPostService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 岗位Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements ISysPostService {

    private final SysPostMapper postMapper;
    private final SysTenantMapper tenantMapper;
    private final SysOrgMapper orgMapper;
    private final SysUserPostMapper userPostMapper;

    @Override
    public IPage<SysPost> selectPostPage(SysPostQuery query) {
        SysPostQuery finalQuery = normalizePostQueryTenant(query);
        Page<SysPost> page = new Page<>(finalQuery.getPageNum(), finalQuery.getPageSize());
        return TenantContextHolder.executeIgnore(() -> postMapper.selectPostPage(page, finalQuery));
    }

    @Override
    public List<SysPost> selectPostList(SysPostQuery query) {
        SysPostQuery finalQuery = normalizePostQueryTenant(query);
        return TenantContextHolder.executeIgnore(() -> postMapper.selectPostList(finalQuery));
    }

    @Override
    public SysPost selectPostById(Long id) {
        SysPost post = TenantContextHolder.executeIgnore(() -> postMapper.selectById(id));
        if (post != null) {
            assertTenantReadable(post.getTenantId());
        }
        return post;
    }

    @Override
    public boolean insertPost(SysPostDTO dto) {
        SysPost post = new SysPost();
        BeanUtil.copyProperties(dto, post);
        Long tenantId = resolveWriteTenantId(dto.getTenantId(), null);
        post.setTenantId(tenantId);
        validateOrgTenant(post.getOrgId(), tenantId);
        return TenantContextHolder.executeIgnore(() -> postMapper.insert(post) > 0);
    }

    @Override
    public boolean updatePost(SysPostDTO dto) {
        SysPost existing = TenantContextHolder.executeIgnore(() -> postMapper.selectById(dto.getId()));
        if (existing == null) {
            return false;
        }
        assertTenantReadable(existing.getTenantId());
        SysPost post = new SysPost();
        BeanUtil.copyProperties(dto, post);
        Long tenantId = resolveWriteTenantId(dto.getTenantId(), existing.getTenantId());
        post.setTenantId(tenantId);
        validateOrgTenant(post.getOrgId(), tenantId);
        return TenantContextHolder.executeIgnore(() -> postMapper.updateById(post) > 0);
    }

    @Override
    public boolean deletePostById(Long id) {
        SysPost post = TenantContextHolder.executeIgnore(() -> postMapper.selectById(id));
        if (post == null) {
            return false;
        }
        assertTenantReadable(post.getTenantId());
        return TenantContextHolder.executeIgnore(() -> {
            userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>()
                    .eq(SysUserPost::getPostId, id)
                    .eq(SysUserPost::getTenantId, post.getTenantId()));
            return postMapper.deleteById(id) > 0;
        });
    }

    @Override
    public boolean deletePostByIds(Long[] ids) {
        for (Long id : ids) {
            deletePostById(id);
        }
        return true;
    }

    private SysPostQuery normalizePostQueryTenant(SysPostQuery query) {
        SysPostQuery normalizedQuery = query == null ? new SysPostQuery() : query;
        Long tenantId = resolveQueryTenantId(normalizedQuery.getTenantId());
        normalizedQuery.setTenantId(tenantId);
        return normalizedQuery;
    }

    private Long resolveQueryTenantId(Long requestedTenantId) {
        LoginUser loginUser = requireLoginUser();
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

    private void validateOrgTenant(Long orgId, Long tenantId) {
        // orgId 非空由 DTO @NotNull 保证；此处只校验租户归属
        SysOrg org = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(orgId));
        if (org == null || !Objects.equals(org.getTenantId(), tenantId)) {
            throw new BusinessException("所属组织不属于当前操作租户");
        }
    }

    private void assertTenantReadable(Long tenantId) {
        LoginUser loginUser = requireLoginUser();
        if (!loginUser.isAdmin() && !Objects.equals(tenantId, loginUser.getTenantId())) {
            throw new RuntimeException("无权操作非本租户岗位");
        }
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return loginUser;
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
