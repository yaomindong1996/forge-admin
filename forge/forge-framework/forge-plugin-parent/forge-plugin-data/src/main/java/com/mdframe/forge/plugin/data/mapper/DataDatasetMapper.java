package com.mdframe.forge.plugin.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DataDatasetMapper extends BaseMapper<DataDataset> {

    IPage<DataDataset> selectDatasetPage(Page<DataDataset> page, @Param("tenantId") Long tenantId,
        @Param("datasetName") String datasetName, @Param("connectionId") Long connectionId, 
        @Param("datasetType") String datasetType, @Param("status") Integer status);

    List<DataDataset> selectDatasetList(@Param("tenantId") Long tenantId, @Param("connectionId") Long connectionId);

    DataDataset selectDatasetByCode(@Param("datasetCode") String datasetCode, @Param("tenantId") Long tenantId);
}