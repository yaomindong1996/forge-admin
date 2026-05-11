package com.mdframe.forge.plugin.external.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.external.dto.ExternalSystemQuery;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExternalSystemMapper extends BaseMapper<ExternalSystem> {

    IPage<ExternalSystem> selectSystemPage(Page<ExternalSystem> page, @Param("query") ExternalSystemQuery query);

    List<ExternalSystem> selectSystemList(@Param("tenantId") Long tenantId);
    
    ExternalSystem selectSystemByCode(@Param("systemCode") String systemCode, @Param("tenantId") Long tenantId);
}
