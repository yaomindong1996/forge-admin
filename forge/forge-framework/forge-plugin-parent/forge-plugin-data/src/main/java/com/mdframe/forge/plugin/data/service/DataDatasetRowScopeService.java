package com.mdframe.forge.plugin.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.data.dto.DataDatasetRowScopeDTO;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetRowScope;
import com.mdframe.forge.plugin.data.support.DataDatasetRowScopeCondition;
import com.mdframe.forge.plugin.data.support.DbDialect;

public interface DataDatasetRowScopeService extends IService<DataDatasetRowScope> {

    DataDatasetRowScope getByDatasetId(Long datasetId);

    void saveDatasetRowScope(Long datasetId, DataDatasetRowScopeDTO dto);

    void deleteDatasetRowScope(Long datasetId);

    DataDatasetRowScopeCondition buildCondition(DataDataset dataset, DbDialect dialect);
}
