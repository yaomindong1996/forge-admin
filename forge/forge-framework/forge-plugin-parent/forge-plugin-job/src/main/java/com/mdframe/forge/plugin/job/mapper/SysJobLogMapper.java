package com.mdframe.forge.plugin.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.job.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务日志Mapper
 */
@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {
}
