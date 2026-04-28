package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysRegionDTO;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.mapper.SysRegionMapper;
import com.mdframe.forge.plugin.system.service.ISysRegionService;
import com.mdframe.forge.plugin.system.vo.SysRegionTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 行政区划Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysRegionServiceImpl extends ServiceImpl<SysRegionMapper, SysRegion> implements ISysRegionService {
    
    private final SysRegionMapper regionMapper;
    
    @Override
    public List<SysRegionTreeVO> selectRegionTree() {
        // 1. 查询第一级（省级，parentCode为空或null）
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(SysRegion::getParentCode).or().eq(SysRegion::getParentCode, ""))
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> rootRegions = regionMapper.selectList(wrapper);
        
        if (rootRegions.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 一次性查询所有parentCode，判断哪些code有子节点
        Set<String> codesWithChildren = getCodesWithChildren(rootRegions.stream()
                .map(SysRegion::getCode)
                .collect(Collectors.toList()));
        
        // 3. 构建VO
        return rootRegions.stream().map(region -> {
            SysRegionTreeVO vo = new SysRegionTreeVO();
            BeanUtil.copyProperties(region, vo);
            vo.setHasChildren(codesWithChildren.contains(region.getCode()));
            return vo;
        }).toList();
    }
    
    /**
     * 批量查询哪些code有子节点（一次SQL）
     */
    private Set<String> getCodesWithChildren(List<String> parentCodes) {
        if (parentCodes.isEmpty()) {
            return new HashSet<>();
        }
        
        // 查询所有parentCode在这些code中的记录
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysRegion::getParentCode)
                .in(SysRegion::getParentCode, parentCodes)
                .isNotNull(SysRegion::getParentCode);
        
        List<SysRegion> children = regionMapper.selectList(wrapper);
        
        // 返回有子节点的code集合
        return children.stream()
                .map(SysRegion::getParentCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }
    
    @Override
    public SysRegion selectRegionByCode(String code) {
        return regionMapper.selectById(code);
    }
    
    @Override
    public boolean insertRegion(SysRegionDTO dto) {
        SysRegion region = new SysRegion();
        BeanUtil.copyProperties(dto, region);
        return regionMapper.insert(region) > 0;
    }
    
    @Override
    public boolean updateRegion(SysRegionDTO dto) {
        SysRegion region = new SysRegion();
        BeanUtil.copyProperties(dto, region);
        return regionMapper.updateById(region) > 0;
    }
    
    @Override
    public boolean deleteRegionByCode(String code) {
        return regionMapper.deleteById(code) > 0;
    }
    
    @Override
    public List<SysRegion> selectChildrenByParentCode(String parentCode) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getParentCode, parentCode).orderByAsc(SysRegion::getLevel).orderByAsc(SysRegion::getCode);
        return regionMapper.selectList(wrapper);
    }
    
    @Override
    public List<SysRegionTreeVO> selectChildrenVOByParentCode(String parentCode) {
        // 1. 查询子节点
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getParentCode, parentCode)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> children = regionMapper.selectList(wrapper);
        
        if (children.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 批量查询哪些code有子节点
        Set<String> codesWithChildren = getCodesWithChildren(children.stream()
                .map(SysRegion::getCode)
                .collect(Collectors.toList()));
        
        // 3. 构建VO
        return children.stream().map(region -> {
            SysRegionTreeVO vo = new SysRegionTreeVO();
            BeanUtil.copyProperties(region, vo);
            vo.setHasChildren(codesWithChildren.contains(region.getCode()));
            return vo;
        }).toList();
    }
    
    @Override
    public List<SysRegion> searchRegionByName(String name) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(SysRegion::getName, name).orderByAsc(SysRegion::getLevel).orderByAsc(SysRegion::getCode);
        return regionMapper.selectList(wrapper);
    }
    
    @Override
    public String buildAncestors(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        StringBuilder ancestors = new StringBuilder();
        String currentCode = code;
        while (StrUtil.isNotBlank(currentCode)) {
            SysRegion region = regionMapper.selectById(currentCode);
            if (region == null) {
                break;
            }
            if (ancestors.length() > 0) {
                ancestors.insert(0, ",");
            }
            ancestors.insert(0, currentCode);
            currentCode = region.getParentCode();
        }
        return ancestors.toString();
    }
}
