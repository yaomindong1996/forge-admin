package com.mdframe.forge.starter.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.social.domain.entity.SysUserSocial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户三方账号绑定Mapper接口
 */
@Mapper
public interface SysUserSocialMapper extends BaseMapper<SysUserSocial> {
}
