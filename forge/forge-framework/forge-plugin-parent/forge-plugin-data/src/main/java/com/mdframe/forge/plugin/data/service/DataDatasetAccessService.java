package com.mdframe.forge.plugin.data.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.data.dto.DataDatasetAclDTO;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetAcl;
import com.mdframe.forge.plugin.data.enums.DataDatasetAccessLevelEnum;
import com.mdframe.forge.plugin.data.support.DataDatasetAccessQuery;

import java.util.List;

public interface DataDatasetAccessService extends IService<DataDatasetAcl> {

    DataDatasetAccessQuery buildCurrentUserAccessQuery(DataDatasetAccessLevelEnum requiredLevel);

    boolean canAccess(DataDataset dataset, DataDatasetAccessLevelEnum requiredLevel);

    void requireAccess(DataDataset dataset, DataDatasetAccessLevelEnum requiredLevel);

    List<DataDatasetAcl> listDatasetAcl(Long datasetId);

    void saveDatasetAcl(Long datasetId, List<DataDatasetAclDTO> aclItems);

    void deleteDatasetAcl(Long datasetId);
}
