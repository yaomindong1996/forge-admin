package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysUserPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-岗位关联Mapper接口
 */
@Mapper
public interface SysUserPostMapper extends BaseMapper<SysUserPost> {

}
