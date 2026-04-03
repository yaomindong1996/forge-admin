package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.message.domain.entity.SysEmailConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysEmailConfigMapper extends BaseMapper<SysEmailConfig> {

    SysEmailConfig selectEnabledConfig(@Param("tenantId") Long tenantId);
}
