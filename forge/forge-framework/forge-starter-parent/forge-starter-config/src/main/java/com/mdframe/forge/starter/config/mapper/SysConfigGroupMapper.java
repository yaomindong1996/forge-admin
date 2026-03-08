package com.mdframe.forge.starter.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.config.entity.SysConfigGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置分组数据访问层
 * 对应数据库表: sys_config_group
 */
@Mapper
public interface SysConfigGroupMapper extends BaseMapper<SysConfigGroup> {
}