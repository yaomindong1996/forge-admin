package com.mdframe.forge.plugin.external.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.external.dto.ExternalApiQuery;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExternalApiMapper extends BaseMapper<ExternalApi> {

    IPage<ExternalApi> selectApiPage(Page<ExternalApi> page, @Param("query") ExternalApiQuery query);
    
    List<ExternalApi> selectApisBySystemId(@Param("systemId") Long systemId);
    
    ExternalApi selectApiByCode(@Param("apiCode") String apiCode, @Param("systemId") Long systemId);

    List<ExternalApi> selectApiListWithSystem(@Param("tenantId") Long tenantId);
}