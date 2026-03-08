package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysUserOrg;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-组织关联Mapper接口
 */
@Mapper
public interface SysUserOrgMapper extends BaseMapper<SysUserOrg> {

}
