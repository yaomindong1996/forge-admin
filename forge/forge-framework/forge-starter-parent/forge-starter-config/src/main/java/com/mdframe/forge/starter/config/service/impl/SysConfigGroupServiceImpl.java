package com.mdframe.forge.starter.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import com.mdframe.forge.starter.config.mapper.SysConfigGroupMapper;
import com.mdframe.forge.starter.config.service.ISysConfigGroupService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置分组服务实现类
 * 对应数据库表: sys_config_group
 */
@Service
public class SysConfigGroupServiceImpl extends ServiceImpl<SysConfigGroupMapper, SysConfigGroup> implements ISysConfigGroupService {

    @Override
    public SysConfigGroup selectByGroupCode(String groupCode) {
        LambdaQueryWrapper<SysConfigGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfigGroup::getGroupCode, groupCode);
        return this.getOne(queryWrapper);
    }

    @Override
    public List<SysConfigGroup> selectEnabledGroups() {
        LambdaQueryWrapper<SysConfigGroup> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfigGroup::getStatus, 1); // 只查询启用的配置
        queryWrapper.orderByAsc(SysConfigGroup::getSort); // 按排序字段升序排列
        return this.list(queryWrapper);
    }

    @Override
    public boolean updateConfigValueByGroupCode(String groupCode, String configValue) {
        SysConfigGroup configGroup = new SysConfigGroup();
        configGroup.setConfigValue(configValue);
        
        LambdaQueryWrapper<SysConfigGroup> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(SysConfigGroup::getGroupCode, groupCode);
        
        return this.update(configGroup, updateWrapper);
    }

    @Override
    public boolean updateStatusByGroupCode(String groupCode, Integer status) {
        SysConfigGroup configGroup = new SysConfigGroup();
        configGroup.setStatus(status);
        
        LambdaQueryWrapper<SysConfigGroup> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(SysConfigGroup::getGroupCode, groupCode);
        
        return this.update(configGroup, updateWrapper);
    }
}