package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 登录日志Mapper
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    Page<SysLoginLog> selectLoginLogPage(Page<SysLoginLog> page,
                                         @Param("query") SysLoginLog query,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime);
}
