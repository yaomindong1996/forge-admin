package com.mdframe.forge.plugin.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.data.entity.DataDatasetRowScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface DataDatasetRowScopeMapper extends BaseMapper<DataDatasetRowScope> {

    DataDatasetRowScope selectByDatasetId(@Param("tenantId") Long tenantId, @Param("datasetId") Long datasetId);

    int deleteByDatasetId(@Param("tenantId") Long tenantId, @Param("datasetId") Long datasetId);

    Integer selectMinDataScope(@Param("tenantId") Long tenantId, @Param("roleIds") List<Long> roleIds);

    Set<Long> selectCustomOrgIds(@Param("tenantId") Long tenantId, @Param("roleIds") List<Long> roleIds);

    Set<Long> selectOrgAndChildIds(@Param("tenantId") Long tenantId, @Param("orgIds") List<Long> orgIds);

    List<String> selectRegionAndDirectChildCodes(@Param("regionCode") String regionCode);

    List<String> selectRegionAndDescendantCodes(@Param("regionCode") String regionCode);
}
