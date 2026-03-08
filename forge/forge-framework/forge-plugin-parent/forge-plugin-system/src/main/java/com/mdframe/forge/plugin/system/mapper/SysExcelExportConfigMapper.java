package com.mdframe.forge.plugin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.system.entity.SysExcelExportConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * Excel导出配置Mapper
 */
@Mapper
public interface SysExcelExportConfigMapper extends BaseMapper<SysExcelExportConfig> {
}
