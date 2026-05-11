package com.mdframe.forge.plugin.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;

import java.util.List;

public interface DataDatasetFieldService extends IService<DataDatasetField> {

    List<DataDatasetField> listByDatasetId(Long datasetId);

    void saveBatchFields(Long datasetId, List<DataDatasetField> fields);

    void deleteByDatasetId(Long datasetId);
}