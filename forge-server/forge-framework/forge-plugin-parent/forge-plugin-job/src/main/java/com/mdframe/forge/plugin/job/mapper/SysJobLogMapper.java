package com.mdframe.forge.plugin.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 任务日志Mapper
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {

    int cleanPhysicalBefore(@Param("beforeDate") LocalDateTime beforeDate);
}
