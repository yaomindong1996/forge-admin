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
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysRegion::getLevel)
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> allRegions = regionMapper.selectList(wrapper);
        return buildTreeVO(allRegions, null);
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

    private List<SysRegionTreeVO> buildTreeVO(List<SysRegion> allRegions, String parentCode) {
        List<SysRegionTreeVO> treeList = new ArrayList<>();
        for (SysRegion region : allRegions) {
            if (StrUtil.isBlank(parentCode) && StrUtil.isBlank(region.getParentCode)) {
                SysRegionTreeVO vo = new SysRegionTreeVO();
                BeanUtil.copyProperties(region, vo);
                vo.setChildren(buildTreeVO(allRegions, region.getCode()));
                treeList.add(vo);
            } else if (StrUtil.isNotBlank(parentCode) && parentCode.equals(region.getParentCode())) {
                SysRegionTreeVO vo = new SysRegionTreeVO();
                BeanUtil.copyProperties(region, vo);
                vo.setChildren(buildTreeVO(allRegions, region.getCode()));
                treeList.add(vo);
            }
        }
        return treeList.isEmpty() ? null : treeList;
    }
}