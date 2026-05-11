package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.mapper.DataDatasetMapper;
import com.mdframe.forge.plugin.data.service.DataDatasetService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataDatasetServiceImpl extends ServiceImpl<DataDatasetMapper, DataDataset>
        implements DataDatasetService {

    private final DataDatasetMapper datasetMapper;

    @Override
    public IPage<DataDataset> page(String datasetName, Long connectionId, String datasetType, Integer status,
        Integer pageNum, Integer pageSize) {
        Long tenantId = SessionHelper.getTenantId();
        Page<DataDataset> page = new Page<>(pageNum, pageSize);
        return datasetMapper.selectDatasetPage(page, tenantId, datasetName, connectionId, datasetType, status);
    }

    @Override
    public List<DataDataset> listByConnectionId(Long connectionId) {
        Long tenantId = SessionHelper.getTenantId();
        return datasetMapper.selectDatasetList(tenantId, connectionId);
    }

    @Override
    public DataDataset getByCode(String datasetCode) {
        Long tenantId = SessionHelper.getTenantId();
        return datasetMapper.selectDatasetByCode(datasetCode, tenantId);
    }
}