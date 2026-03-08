package com.mdframe.forge.plugin.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.job.entity.SysJobConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务配置Mapper
 */
@Mapper
public interface SysJobConfigMapper extends BaseMapper<SysJobConfig> {
}
