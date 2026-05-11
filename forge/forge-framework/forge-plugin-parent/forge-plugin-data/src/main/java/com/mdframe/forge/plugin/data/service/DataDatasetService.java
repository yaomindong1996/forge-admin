package com.mdframe.forge.plugin.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.data.entity.DataDataset;

import java.util.List;

public interface DataDatasetService extends IService<DataDataset> {

    IPage<DataDataset> page(String datasetName, Long connectionId, String datasetType, Integer status, 
        Integer pageNum, Integer pageSize);

    List<DataDataset> listByConnectionId(Long connectionId);

    DataDataset getByCode(String datasetCode);
}