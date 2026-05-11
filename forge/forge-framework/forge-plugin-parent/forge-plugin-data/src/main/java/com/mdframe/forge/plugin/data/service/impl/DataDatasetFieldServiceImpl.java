package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;
import com.mdframe.forge.plugin.data.mapper.DataDatasetFieldMapper;
import com.mdframe.forge.plugin.data.service.DataDatasetFieldService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataDatasetFieldServiceImpl extends ServiceImpl<DataDatasetFieldMapper, DataDatasetField>
        implements DataDatasetFieldService {

    private final DataDatasetFieldMapper fieldMapper;

    @Override
    public List<DataDatasetField> listByDatasetId(Long datasetId) {
        Long tenantId = SessionHelper.getTenantId();
        return fieldMapper.selectFieldListByDatasetId(datasetId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchFields(Long datasetId, List<DataDatasetField> fields) {
        Long tenantId = SessionHelper.getTenantId();
        Long userId = SessionHelper.getUserId();
        Long deptId = SessionHelper.getMainOrgId();
        
        fieldMapper.deleteByDatasetId(datasetId, tenantId);
        
        for (DataDatasetField field : fields) {
            field.setDatasetId(datasetId);
            field.setTenantId(tenantId);
            field.setCreateBy(userId);
            field.setCreateDept(deptId);
            field.setUpdateBy(userId);
        }
        
        saveBatch(fields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDatasetId(Long datasetId) {
        Long tenantId = SessionHelper.getTenantId();
        fieldMapper.deleteByDatasetId(datasetId, tenantId);
    }
}