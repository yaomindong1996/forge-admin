package com.mdframe.forge.plugin.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataDatasetFieldMapper extends BaseMapper<DataDatasetField> {

    List<DataDatasetField> selectFieldListByDatasetId(@Param("datasetId") Long datasetId, @Param("tenantId") Long tenantId);

    int deleteByDatasetId(@Param("datasetId") Long datasetId, @Param("tenantId") Long tenantId);
}