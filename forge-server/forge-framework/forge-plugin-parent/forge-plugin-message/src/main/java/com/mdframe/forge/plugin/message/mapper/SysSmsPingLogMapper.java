package com.mdframe.forge.plugin.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.message.domain.entity.SysSmsPingLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysSmsPingLogMapper extends BaseMapper<SysSmsPingLog> {

    List<SysSmsPingLog> selectRecent(@Param("tenantId") Long tenantId, @Param("limit") Integer limit);
}
