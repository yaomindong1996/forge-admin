package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.dto.DataConnectionSaveDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.mapper.DataConnectionMapper;
import com.mdframe.forge.plugin.data.service.DataConnectionService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.crypto.persistence.PersistentCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DataConnectionServiceImpl extends ServiceImpl<DataConnectionMapper, DataConnection>
        implements DataConnectionService {

    private final DataConnectionMapper connectionMapper;
    private final PersistentCryptoService persistentCryptoService;

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
    public boolean saveConnection(DataConnectionSaveDTO dto) {
        DataConnection entity = toEntity(dto, null);
        return connectionMapper.insert(entity) > 0;
    }

    @Override
    public boolean updateConnection(DataConnectionSaveDTO dto) {
        DataConnection existing = connectionMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("数据连接不存在或已删除");
        }
        DataConnection entity = toEntity(dto, existing);
        return connectionMapper.updateById(entity) > 0;
    }

    @Override
    public boolean hasDatasetReference(Long connectionId) {
        Long tenantId = SessionHelper.getTenantId();
        int count = connectionMapper.selectDatasetCountByConnectionId(connectionId, tenantId);
        return count > 0;
    }

    private DataConnection toEntity(DataConnectionSaveDTO dto, DataConnection existing) {
        DataConnection entity = new DataConnection();
        entity.setId(dto.getId());
        entity.setTenantId(SessionHelper.getTenantId());
        entity.setConnectionCode(dto.getConnectionCode());
        entity.setConnectionName(dto.getConnectionName());
        entity.setDbType(dto.getDbType());
        entity.setDriverClassName(dto.getDriverClassName());
        entity.setJdbcUrl(dto.getJdbcUrl());
        entity.setUsername(dto.getUsername());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entity.setPasswordCipher(persistentCryptoService.encrypt(dto.getPassword(), null));
        } else if (existing != null) {
            entity.setPasswordCipher(existing.getPasswordCipher());
        }
        entity.setSchemaName(dto.getSchemaName());
        entity.setTestSql(dto.getTestSql() != null ? dto.getTestSql() : "SELECT 1");
        entity.setPoolConfigJson(dto.getPoolConfigJson());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setDescription(dto.getDescription());
        return entity;
    }
}
