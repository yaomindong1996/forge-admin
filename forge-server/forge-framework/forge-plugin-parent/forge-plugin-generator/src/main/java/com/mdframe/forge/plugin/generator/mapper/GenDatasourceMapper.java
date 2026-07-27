package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 代码生成器数据源配置Mapper接口
 */
@Mapper
public interface GenDatasourceMapper extends BaseMapper<GenDatasource> {

    Page<GenDatasource> selectDatasourcePage(Page<GenDatasource> page,
                                              @Param("datasourceName") String datasourceName,
                                              @Param("usageScope") String usageScope);

    List<GenDatasource> selectEnabledDatasources(@Param("usageScope") String usageScope);

    GenDatasource selectDefaultDatasource();
}
