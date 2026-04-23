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
        
        // 设置祖先编码
        if (org.getParentId() == null || org.getParentId() == 0) {
            // 顶级组织，祖先编码为空或特定格式
            org.setAncestors(",");
        } else {
            // 非顶级组织，获取父组织的祖先编码并添加自己的ID
            SysOrg parentOrg = orgMapper.selectById(org.getParentId());
            if (parentOrg != null) {
                org.setAncestors(parentOrg.getAncestors() + org.getParentId() + ",");
            } else {
                // 如果父组织不存在，默认设置为顶级组织的格式
                org.setAncestors(",");
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
        // 获取指定组织的详细信息
        SysOrg org = orgMapper.selectById(orgId);
        if (org == null) {
            return null;
        }
        
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        
        // 判断是否是顶级组织（parentId=0）
        if (org.getParentId() == null || org.getParentId() == 0) {
            // 顶级组织，查询所有父组织为0的组织（包括自己和所有一级子组织，以及更下级组织）
            // 顶级组织的 ancestors 字段是 ","，所以查询所有包含 ",0," 或自身ID的组织
            String ancestors = "," + orgId + ",";
            wrapper.and(w -> w.eq(SysOrg::getParentId, 0).or().like(SysOrg::getAncestors, ancestors));
        } else {
            // 非顶级组织，利用 ancestors 字段查询该组织及其所有子组织
            // ancestors 字段格式为：,1,2,3,
            String ancestors = org.getAncestors() + orgId + ",";
            wrapper.and(w -> w.eq(SysOrg::getId, orgId).or().like(SysOrg::getAncestors, ancestors));
        }
        
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
