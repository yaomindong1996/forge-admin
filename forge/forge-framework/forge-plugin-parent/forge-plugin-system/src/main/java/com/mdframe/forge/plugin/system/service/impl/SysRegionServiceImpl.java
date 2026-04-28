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

import java.util.ArrayList;
import java.util.List;

/**
 * 行政区划Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysRegionServiceImpl extends ServiceImpl<SysRegionMapper, SysRegion> implements ISysRegionService {

    private final SysRegionMapper regionMapper;

    @Override
    public List<SysRegionTreeVO> selectRegionTree() {
        // 懒加载：只查询第一级（省级，parentCode为空或null）
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(SysRegion::getParentCode)
                .or()
                .eq(SysRegion::getParentCode, ""))
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> rootRegions = regionMapper.selectList(wrapper);
        
        return rootRegions.stream().map(region -> {
            SysRegionTreeVO vo = new SysRegionTreeVO();
            BeanUtil.copyProperties(region, vo);
            // 检查是否有子节点
            vo.setHasChildren(hasChildren(region.getCode()));
            return vo;
        }).toList();
    }
    
    /**
     * 检查是否有子节点
     */
    private boolean hasChildren(String code) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getParentCode, code);
        return regionMapper.selectCount(wrapper) > 0;
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
        wrapper.eq(SysRegion::getParentCode, parentCode)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
        return regionMapper.selectList(wrapper);
    }

    @Override
    public List<SysRegionTreeVO> selectChildrenVOByParentCode(String parentCode) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getParentCode, parentCode)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> children = regionMapper.selectList(wrapper);
        
        return children.stream().map(region -> {
            SysRegionTreeVO vo = new SysRegionTreeVO();
            BeanUtil.copyProperties(region, vo);
            vo.setHasChildren(hasChildren(region.getCode()));
            return vo;
        }).toList();
    }

    @Override
    public List<SysRegion> searchRegionByName(String name) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(SysRegion::getName, name)
                .orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
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
        }
        return treeList.isEmpty() ? null : treeList;
    }
}
