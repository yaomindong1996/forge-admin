package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysOrgDTO;
import com.mdframe.forge.plugin.system.dto.SysOrgQuery;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.service.ISysOrgService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysOrgServiceImpl extends ServiceImpl<SysOrgMapper, SysOrg> implements ISysOrgService {

    private final SysOrgMapper orgMapper;

    @Override
    public IPage<SysOrg> selectOrgPage(SysOrgQuery query) {
        LambdaQueryWrapper<SysOrg> wrapper = buildQueryWrapper(query);
        Page<SysOrg> page = new Page<>(query.getPageNum(), query.getPageSize());
        return orgMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysOrg> selectOrgTree(SysOrgQuery query) {
        LambdaQueryWrapper<SysOrg> wrapper = buildQueryWrapper(query);
        List<SysOrg> allOrgs = orgMapper.selectList(wrapper);
        return buildTree(allOrgs, 0L);
    }

    /**
     * 构建树形结构
     */
    private List<SysOrg> buildTree(List<SysOrg> allOrgs, Long parentId) {
        return allOrgs.stream()
                .filter(org -> org.getParentId().equals(parentId))
                .peek(org -> {
                    List<SysOrg> children = buildTree(allOrgs, org.getId());
                    org.setChildren(children.isEmpty() ? null : children);
                })
                .toList();
    }

    @Override
    public SysOrg selectOrgById(Long id) {
        return orgMapper.selectById(id);
    }

    @Override
    public boolean insertOrg(SysOrgDTO dto) {
        SysOrg org = new SysOrg();
        BeanUtil.copyProperties(dto, org);

        if (org.getParentId() == null || org.getParentId() == 0) {
            org.setAncestors("0");
        } else {
            SysOrg parentOrg = orgMapper.selectById(org.getParentId());
            if (parentOrg != null) {
                org.setAncestors(parentOrg.getAncestors() + "," + org.getParentId());
            } else {
                org.setAncestors("0");
            }
        }

        return orgMapper.insert(org) > 0;
    }

    @Override
    public boolean updateOrg(SysOrgDTO dto) {
        SysOrg org = new SysOrg();
        BeanUtil.copyProperties(dto, org);
        return orgMapper.updateById(org) > 0;
    }

    @Override
    public boolean deleteOrgById(Long id) {
        return orgMapper.deleteById(id) > 0;
    }

    @Override
    public List<Long> selectOrgAndChildrenIds(Long orgId) {
        SysOrg org = orgMapper.selectById(orgId);
        if (org == null) {
            return null;
        }

        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SysOrg::getId, orgId)
                .or()
                .apply("FIND_IN_SET({0}, ancestors) > 0", orgId));

        List<SysOrg> orgs = orgMapper.selectList(wrapper);
        return orgs.stream().map(SysOrg::getId).collect(Collectors.toList());
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<SysOrg> buildQueryWrapper(SysOrgQuery query) {
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getTenantId() != null, SysOrg::getTenantId, query.getTenantId())
                .like(StringUtils.isNotBlank(query.getOrgName()), SysOrg::getOrgName, query.getOrgName())
                .eq(query.getParentId() != null, SysOrg::getParentId, query.getParentId())
                .eq(query.getOrgType() != null, SysOrg::getOrgType, query.getOrgType())
                .eq(query.getOrgStatus() != null, SysOrg::getOrgStatus, query.getOrgStatus())
                .orderByAsc(SysOrg::getSort)
                .orderByDesc(SysOrg::getCreateTime);
        return wrapper;
    }
}
