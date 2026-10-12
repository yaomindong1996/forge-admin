package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.dto.DataDimensionItemDTO;
import com.mdframe.forge.plugin.data.dto.DataDimensionSaveDTO;
import com.mdframe.forge.plugin.data.entity.DataConnection;
import com.mdframe.forge.plugin.data.entity.DataDimension;
import com.mdframe.forge.plugin.data.entity.DataDimensionItem;
import com.mdframe.forge.plugin.data.mapper.DataConnectionMapper;
import com.mdframe.forge.plugin.data.mapper.DataDatasetFieldMapper;
import com.mdframe.forge.plugin.data.mapper.DataDimensionItemMapper;
import com.mdframe.forge.plugin.data.mapper.DataDimensionMapper;
import com.mdframe.forge.plugin.data.service.DataDimensionService;
import com.mdframe.forge.plugin.data.support.DbDialect;
import com.mdframe.forge.plugin.data.support.DbDialectFactory;
import com.mdframe.forge.plugin.data.support.JdbcDataSourceProvider;
import com.mdframe.forge.plugin.data.support.SqlSafetyValidator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.mdframe.forge.starter.core.enums.EnableStatus;

@Service
@RequiredArgsConstructor
public class DataDimensionServiceImpl extends ServiceImpl<DataDimensionMapper, DataDimension>
    implements DataDimensionService {

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_SQL = "SQL";
    private static final int MAX_SYNC_ROWS = 5000;

    private final DataDimensionMapper dimensionMapper;
    private final DataDimensionItemMapper itemMapper;
    private final DataDatasetFieldMapper fieldMapper;
    private final DataConnectionMapper connectionMapper;
    private final JdbcDataSourceProvider dataSourceProvider;
    private final DbDialectFactory dialectFactory;
    private final SqlSafetyValidator sqlSafetyValidator;

    @Override
    public IPage<DataDimension> page(String dimensionName, String sourceType, Integer status, Integer pageNum, Integer pageSize) {
        Page<DataDimension> page = new Page<>(pageNum, pageSize);
        return dimensionMapper.selectDimensionPage(page, SessionHelper.getTenantId(), dimensionName, sourceType, status);
    }

    @Override
    public List<DataDimension> listEnabled() {
        return dimensionMapper.selectDimensionList(SessionHelper.getTenantId(), 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDimension saveDimension(DataDimensionSaveDTO dto) {
        validateDimension(dto);
        DataDimension entity = convertToEntity(dto, null);
        entity.setTenantId(SessionHelper.getTenantId());
        save(entity);
        return getById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDimension updateDimension(DataDimensionSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("维度ID不能为空");
        }
        DataDimension existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException("维度不存在或已删除");
        }
        validateDimension(dto);
        DataDimension entity = convertToEntity(dto, existing);
        entity.setId(existing.getId());
        entity.setTenantId(existing.getTenantId());
        updateById(entity);
        return getById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDimension(Long id) {
        DataDimension dimension = getById(id);
        if (dimension == null) {
            throw new BusinessException("维度不存在或已删除");
        }
        Long tenantId = SessionHelper.getTenantId();
        if (fieldMapper.selectCountByDimensionId(tenantId, id) > 0) {
            throw new BusinessException("当前维度已被数据集字段引用，无法删除");
        }
        itemMapper.deleteByDimensionId(tenantId, id);
        removeById(id);
    }

    @Override
    public List<DataDimensionItem> listItems(Long dimensionId) {
        requireDimension(dimensionId);
        return itemMapper.selectItemsByDimensionId(SessionHelper.getTenantId(), dimensionId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveItems(Long dimensionId, List<DataDimensionItemDTO> items) {
        DataDimension dimension = requireDimension(dimensionId);
        if (SOURCE_SQL.equals(dimension.getSourceType())) {
            throw new BusinessException("SQL来源维度请通过同步更新维度值");
        }
        saveDimensionItems(dimensionId, convertItems(items));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DataDimensionItem> syncItems(Long dimensionId) {
        DataDimension dimension = requireDimension(dimensionId);
        if (!SOURCE_SQL.equals(dimension.getSourceType())) {
            throw new BusinessException("只有SQL来源维度支持同步");
        }
        List<DataDimensionItem> items = querySqlItems(dimension);
        saveDimensionItems(dimensionId, items);

        DataDimension update = new DataDimension();
        update.setId(dimensionId);
        update.setLastSyncTime(LocalDateTime.now());
        updateById(update);
        return itemMapper.selectItemsByDimensionId(SessionHelper.getTenantId(), dimensionId, null);
    }

    private void validateDimension(DataDimensionSaveDTO dto) {
        if (!StringUtils.hasText(dto.getDimensionCode())) {
            throw new BusinessException("维度编码不能为空");
        }
        if (!StringUtils.hasText(dto.getDimensionName())) {
            throw new BusinessException("维度名称不能为空");
        }
        String sourceType = normalizeSourceType(dto.getSourceType());
        DataDimension existing = dimensionMapper.selectDimensionByCode(SessionHelper.getTenantId(), dto.getDimensionCode().trim());
        if (existing != null && !Objects.equals(existing.getId(), dto.getId())) {
            throw new BusinessException("维度编码已存在");
        }
        if (SOURCE_SQL.equals(sourceType)) {
            if (dto.getConnectionId() == null) {
                throw new BusinessException("SQL来源维度必须选择数据连接");
            }
            if (!StringUtils.hasText(dto.getSqlText())) {
                throw new BusinessException("SQL来源维度必须填写同步SQL");
            }
            requireEnabledConnection(dto.getConnectionId());
            sqlSafetyValidator.validate(dto.getSqlText());
        }
    }

    private DataDimension convertToEntity(DataDimensionSaveDTO dto, DataDimension existing) {
        String sourceType = normalizeSourceType(dto.getSourceType());
        DataDimension entity = new DataDimension();
        entity.setDimensionCode(dto.getDimensionCode() != null ? dto.getDimensionCode().trim() : null);
        entity.setDimensionName(dto.getDimensionName() != null ? dto.getDimensionName().trim() : null);
        entity.setSourceType(sourceType);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : existing != null ? existing.getStatus() : EnableStatus.ENABLED.getCode());
        entity.setDescription(dto.getDescription());
        if (SOURCE_SQL.equals(sourceType)) {
            entity.setConnectionId(dto.getConnectionId());
            entity.setSqlText(dto.getSqlText());
            entity.setValueColumn(trimToNull(dto.getValueColumn()));
            entity.setLabelColumn(trimToNull(dto.getLabelColumn()));
            entity.setLastSyncTime(existing != null ? existing.getLastSyncTime() : null);
        }
        return entity;
    }

    private String normalizeSourceType(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            return SOURCE_MANUAL;
        }
        String normalized = sourceType.trim().toUpperCase();
        if (!SOURCE_MANUAL.equals(normalized) && !SOURCE_SQL.equals(normalized)) {
            throw new BusinessException("维度来源类型不支持：" + sourceType);
        }
        return normalized;
    }

    private DataDimension requireDimension(Long dimensionId) {
        DataDimension dimension = getById(dimensionId);
        if (dimension == null) {
            throw new BusinessException("维度不存在或已删除");
        }
        return dimension;
    }

    private DataConnection requireEnabledConnection(Long connectionId) {
        DataConnection connection = connectionMapper.selectById(connectionId);
        if (connection == null) {
            throw new BusinessException("数据连接不存在或已删除");
        }
        if (!EnableStatus.ENABLED.matches(connection.getStatus())) {
            throw new BusinessException("数据连接已禁用");
        }
        return connection;
    }

    private List<DataDimensionItem> convertItems(List<DataDimensionItemDTO> itemDTOs) {
        List<DataDimensionItem> items = new ArrayList<>();
        if (itemDTOs == null) {
            return items;
        }
        int index = 0;
        for (DataDimensionItemDTO dto : itemDTOs) {
            if (dto == null || !StringUtils.hasText(dto.getItemValue())) {
                continue;
            }
            if (!StringUtils.hasText(dto.getItemLabel())) {
                throw new BusinessException("维度值[" + dto.getItemValue() + "]缺少显示名称");
            }
            DataDimensionItem item = new DataDimensionItem();
            item.setItemValue(dto.getItemValue().trim());
            item.setItemLabel(dto.getItemLabel().trim());
            item.setSort(dto.getSort() != null ? dto.getSort() : index);
            item.setStatus(dto.getStatus() != null ? dto.getStatus() : EnableStatus.ENABLED.getCode());
            item.setExtraJson(dto.getExtraJson());
            items.add(item);
            index++;
        }
        return items;
    }

    private List<DataDimensionItem> querySqlItems(DataDimension dimension) {
        DataConnection connection = requireEnabledConnection(dimension.getConnectionId());
        sqlSafetyValidator.validate(dimension.getSqlText());
        List<DataDimensionItem> items = new ArrayList<>();
        try (Connection conn = dataSourceProvider.getConnection(connection)) {
            DbDialect dialect = dialectFactory.getDialect(connection.getDbType());
            String sql = dialect.buildLimitSql("SELECT * FROM (" + dimension.getSqlText() + ") dim_src", MAX_SYNC_ROWS);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                if (columnCount == 0) {
                    return items;
                }
                String valueColumn = resolveColumn(dimension.getValueColumn(), metaData, 1);
                String labelColumn = resolveColumn(dimension.getLabelColumn(), metaData, Math.min(2, columnCount));
                int sort = 0;
                while (rs.next()) {
                    Object value = rs.getObject(valueColumn);
                    if (value == null) {
                        continue;
                    }
                    Object label = rs.getObject(labelColumn);
                    DataDimensionItem item = new DataDimensionItem();
                    item.setItemValue(String.valueOf(value));
                    item.setItemLabel(label != null ? String.valueOf(label) : String.valueOf(value));
                    item.setSort(sort++);
                    item.setStatus(EnableStatus.ENABLED.getCode());
                    items.add(item);
                }
                rs.close();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("同步维度数据失败：" + e.getMessage());
        }
        return items;
    }

    private String resolveColumn(String configuredColumn, ResultSetMetaData metaData, int fallbackIndex) throws Exception {
        if (StringUtils.hasText(configuredColumn)) {
            return configuredColumn.trim();
        }
        return metaData.getColumnLabel(fallbackIndex);
    }

    private void saveDimensionItems(Long dimensionId, List<DataDimensionItem> items) {
        Long tenantId = SessionHelper.getTenantId();
        Long userId = SessionHelper.getUserId();
        Long deptId = SessionHelper.getMainOrgId();
        itemMapper.deleteByDimensionId(tenantId, dimensionId);
        for (DataDimensionItem item : items) {
            item.setDimensionId(dimensionId);
            item.setTenantId(tenantId);
            item.setCreateBy(userId);
            item.setCreateDept(deptId);
            item.setUpdateBy(userId);
        }
        if (!items.isEmpty()) {
            itemMapper.insert(items.get(0));
            for (int i = 1; i < items.size(); i++) {
                itemMapper.insert(items.get(i));
            }
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
