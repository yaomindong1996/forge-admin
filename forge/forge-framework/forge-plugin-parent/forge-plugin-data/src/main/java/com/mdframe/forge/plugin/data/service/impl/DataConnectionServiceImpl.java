package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.mapper.DataConnectionMapper;
import com.mdframe.forge.plugin.data.service.DataConnectionService;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataConnectionServiceImpl extends ServiceImpl<DataConnectionMapper, DataConnection>
        implements DataConnectionService {

    private final DataConnectionMapper connectionMapper;

    @Override
    public IPage<DataConnection> page(String connectionName, String dbType, Integer status, 
        Integer pageNum, Integer pageSize) {
        Long tenantId = SessionHelper.getTenantId();
        Page<DataConnection> page = new Page<>(pageNum, pageSize);
        return connectionMapper.selectConnectionPage(page, tenantId, connectionName, dbType, status);
    }

    @Override
    public List<DataConnection> listAll() {
        Long tenantId = SessionHelper.getTenantId();
        return connectionMapper.selectConnectionList(tenantId);
    }

    @Override
    public DataConnection getByCode(String connectionCode) {
        Long tenantId = SessionHelper.getTenantId();
        return connectionMapper.selectConnectionByCode(connectionCode, tenantId);
    }

    @Override
    public boolean hasDatasetReference(Long connectionId) {
        Long tenantId = SessionHelper.getTenantId();
        int count = connectionMapper.selectDatasetCountByConnectionId(connectionId, tenantId);
        return count > 0;
    }
}