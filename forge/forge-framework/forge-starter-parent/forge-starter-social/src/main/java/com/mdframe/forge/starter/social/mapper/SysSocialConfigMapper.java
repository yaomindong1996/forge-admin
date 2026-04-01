package com.mdframe.forge.starter.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.starter.social.domain.entity.SysSocialConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 三方登录配置Mapper接口
 */
@Mapper
public interface SysSocialConfigMapper extends BaseMapper<SysSocialConfig> {

    /**
     * 根据平台和租户查询配置
     */
    SysSocialConfig selectByPlatformAndTenant(@Param("platform") String platform, @Param("tenantId") Long tenantId);

    /**
     * 查询租户下所有启用的配置
     */
    List<SysSocialConfig> selectEnabledByTenant(@Param("tenantId") Long tenantId);

    /**
     * 查询所有启用的配置
     */
    List<SysSocialConfig> selectAllEnabled();
}
