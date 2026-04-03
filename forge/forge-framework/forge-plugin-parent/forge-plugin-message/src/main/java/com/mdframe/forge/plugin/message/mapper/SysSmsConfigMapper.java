package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysSmsConfigMapper extends BaseMapper<SysSmsConfig> {

    SysSmsConfig selectEnabledConfig(@Param("tenantId") Long tenantId);
}
