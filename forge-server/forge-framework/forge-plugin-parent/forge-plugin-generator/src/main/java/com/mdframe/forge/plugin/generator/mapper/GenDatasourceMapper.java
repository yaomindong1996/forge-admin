package com.mdframe.forge.plugin.generator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.GenDatasource;
import com.mdframe.forge.plugin.generator.vo.GenDatasourceReferenceHitVO;
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

    /**
     * 统计仍引用该数据源（按 ID 或编码）的业务/配置条目。
     */
    List<GenDatasourceReferenceHitVO> selectReferenceHits(@Param("datasourceId") Long datasourceId,
                                                          @Param("datasourceCode") String datasourceCode);
}
