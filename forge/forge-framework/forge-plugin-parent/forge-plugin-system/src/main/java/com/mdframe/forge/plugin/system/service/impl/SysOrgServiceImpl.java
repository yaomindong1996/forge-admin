package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysOrgDTO;
import com.mdframe.forge.plugin.system.dto.SysOrgQuery;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.service.ISysOrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
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
        Page<SysOrg> page = new Page<>(query.getPageNum(), query.getPageSize());
        return orgMapper.selectOrgPage(page, query);
    }

    @Override
    public List<SysOrg> selectOrgTree(SysOrgQuery query) {
        List<SysOrg> allOrgs = orgMapper.selectOrgList(query);
        return buildTree(allOrgs);
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
        List<Long> ids = orgMapper.selectOrgAndChildrenIds(orgId);
        return ids != null && !ids.isEmpty() ? ids : null;
    }
}
