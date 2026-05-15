package com.mdframe.forge.plugin.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.data.entity.DataDatasetAcl;
import com.mdframe.forge.plugin.data.support.DataDatasetAccessQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataDatasetAclMapper extends BaseMapper<DataDatasetAcl> {

    List<DataDatasetAcl> selectByDatasetId(@Param("tenantId") Long tenantId, @Param("datasetId") Long datasetId);

    int deleteByDatasetId(@Param("tenantId") Long tenantId, @Param("datasetId") Long datasetId);

    int countMatchedAcl(@Param("datasetId") Long datasetId, @Param("access") DataDatasetAccessQuery access);
}
