package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.dto.DataDatasetRowScopeDTO;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetRowScope;
import com.mdframe.forge.plugin.data.enums.DataDatasetRowScopeStrategyEnum;
import com.mdframe.forge.plugin.data.enums.DataDatasetRowScopeTypeEnum;
import com.mdframe.forge.plugin.data.mapper.DataDatasetRowScopeMapper;
import com.mdframe.forge.plugin.data.service.DataDatasetRowScopeService;
import com.mdframe.forge.plugin.data.support.DataDatasetRowScopeCondition;
import com.mdframe.forge.plugin.data.support.DbDialect;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DataDatasetRowScopeServiceImpl extends ServiceImpl<DataDatasetRowScopeMapper, DataDatasetRowScope>
        implements DataDatasetRowScopeService {

    private static final String DEFAULT_SCOPE_MODE = "SYSTEM_DATA_SCOPE";

    private final DataDatasetRowScopeMapper rowScopeMapper;

    @Override
    public DataDatasetRowScope getByDatasetId(Long datasetId) {
        return rowScopeMapper.selectByDatasetId(SessionHelper.getTenantId(), datasetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDatasetRowScope(Long datasetId, DataDatasetRowScopeDTO dto) {
        rowScopeMapper.deleteByDatasetId(SessionHelper.getTenantId(), datasetId);
        if (dto == null) {
            return;
        }
        DataDatasetRowScope entity = convertToEntity(datasetId, dto);
        save(entity);
    }

    @Override
    public void deleteDatasetRowScope(Long datasetId) {
        rowScopeMapper.deleteByDatasetId(SessionHelper.getTenantId(), datasetId);
    }

    @Override
    public DataDatasetRowScopeCondition buildCondition(DataDataset dataset, DbDialect dialect) {
        if (dataset == null || dialect == null) {
            return DataDatasetRowScopeCondition.disabled();
        }
        DataDatasetRowScope rowScope = getByDatasetId(dataset.getId());
        if (rowScope == null || !Integer.valueOf(1).equals(rowScope.getEnabled())) {
            return DataDatasetRowScopeCondition.disabled();
        }
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            return denyAllCondition();
        }
        DataDatasetRowScopeTypeEnum scopeType = resolveScopeType(loginUser);
        if (scopeType == DataDatasetRowScopeTypeEnum.ALL || loginUser.isAdmin()) {
            return DataDatasetRowScopeCondition.disabled();
        }
        return switch (scopeType) {
            case TENANT_ALL, TENANT_ALL_LEGACY -> buildTenantCondition(rowScope, dialect, loginUser);
            case ORG -> buildOrgCondition(rowScope, dialect, loginUser.getOrgIds());
            case ORG_AND_CHILD -> buildOrgAndChildCondition(rowScope, dialect, loginUser);
            case SELF -> buildSelfCondition(rowScope, dialect, loginUser);
            case REGION -> buildRegionCondition(rowScope, dialect, loginUser);
            case ALL -> DataDatasetRowScopeCondition.disabled();
        };
    }

    private DataDatasetRowScope convertToEntity(Long datasetId, DataDatasetRowScopeDTO dto) {
        DataDatasetRowScope entity = new DataDatasetRowScope();
        entity.setTenantId(SessionHelper.getTenantId());
        entity.setDatasetId(datasetId);
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 0);
        entity.setScopeMode(isBlank(dto.getScopeMode()) ? DEFAULT_SCOPE_MODE : dto.getScopeMode());
        entity.setTenantColumn(trimToNull(dto.getTenantColumn()));
        entity.setOrgColumn(trimToNull(dto.getOrgColumn()));
        entity.setUserColumn(trimToNull(dto.getUserColumn()));
        entity.setRegionColumn(trimToNull(dto.getRegionColumn()));
        entity.setRegionStrategy(DataDatasetRowScopeStrategyEnum.getByCode(dto.getRegionStrategy()).getCode());
        entity.setRemark(dto.getRemark());
        entity.setCreateBy(SessionHelper.getUserId());
        entity.setCreateDept(SessionHelper.getMainOrgId());
        entity.setUpdateBy(SessionHelper.getUserId());
        return entity;
    }

    private DataDatasetRowScopeTypeEnum resolveScopeType(LoginUser loginUser) {
        if (loginUser.isTenantAdmin()) {
            return DataDatasetRowScopeTypeEnum.TENANT_ALL;
        }
        List<Long> roleIds = loginUser.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            return DataDatasetRowScopeTypeEnum.SELF;
        }
        Integer minDataScope = rowScopeMapper.selectMinDataScope(loginUser.getTenantId(), roleIds);
        return DataDatasetRowScopeTypeEnum.getByCode(minDataScope);
    }

    private DataDatasetRowScopeCondition buildTenantCondition(DataDatasetRowScope rowScope, DbDialect dialect,
            LoginUser loginUser) {
        if (isBlank(rowScope.getTenantColumn())) {
            return DataDatasetRowScopeCondition.disabled();
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("__scopeTenantId", loginUser.getTenantId());
        return DataDatasetRowScopeCondition.of("AND " + resolveColumn(rowScope.getTenantColumn(), dialect)
                + " = :__scopeTenantId", params);
    }

    private DataDatasetRowScopeCondition buildOrgCondition(DataDatasetRowScope rowScope, DbDialect dialect,
            List<Long> orgIds) {
        requireColumn(rowScope.getOrgColumn(), "组织字段");
        if (orgIds == null || orgIds.isEmpty()) {
            return denyAllCondition();
        }
        return buildInCondition(resolveColumn(rowScope.getOrgColumn(), dialect), "__scopeOrg", orgIds);
    }

    private DataDatasetRowScopeCondition buildOrgAndChildCondition(DataDatasetRowScope rowScope, DbDialect dialect,
            LoginUser loginUser) {
        requireColumn(rowScope.getOrgColumn(), "组织字段");
        List<Long> orgIds = loginUser.getOrgIds();
        if (orgIds == null || orgIds.isEmpty()) {
            return denyAllCondition();
        }
        Set<Long> scopedOrgIds = rowScopeMapper.selectOrgAndChildIds(loginUser.getTenantId(), orgIds);
        if (scopedOrgIds == null || scopedOrgIds.isEmpty()) {
            return denyAllCondition();
        }
        return buildInCondition(resolveColumn(rowScope.getOrgColumn(), dialect), "__scopeOrg", scopedOrgIds);
    }

    private DataDatasetRowScopeCondition buildSelfCondition(DataDatasetRowScope rowScope, DbDialect dialect,
            LoginUser loginUser) {
        requireColumn(rowScope.getUserColumn(), "用户字段");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("__scopeUserId", loginUser.getUserId());
        return DataDatasetRowScopeCondition.of("AND " + resolveColumn(rowScope.getUserColumn(), dialect)
                + " = :__scopeUserId", params);
    }

    private DataDatasetRowScopeCondition buildRegionCondition(DataDatasetRowScope rowScope, DbDialect dialect,
            LoginUser loginUser) {
        requireColumn(rowScope.getRegionColumn(), "行政区划字段");
        if (isBlank(loginUser.getRegionCode())) {
            return denyAllCondition();
        }
        Integer regionLevel = loginUser.getRegionLevel();
        if (regionLevel != null && regionLevel <= 1) {
            return DataDatasetRowScopeCondition.disabled();
        }
        List<String> regionCodes = resolveRegionCodes(rowScope, loginUser.getRegionCode());
        if (regionCodes == null || regionCodes.isEmpty()) {
            return denyAllCondition();
        }
        return buildInCondition(resolveColumn(rowScope.getRegionColumn(), dialect), "__scopeRegion", regionCodes);
    }

    private List<String> resolveRegionCodes(DataDatasetRowScope rowScope, String regionCode) {
        DataDatasetRowScopeStrategyEnum strategy = DataDatasetRowScopeStrategyEnum.getByCode(rowScope.getRegionStrategy());
        return switch (strategy) {
            case SELF -> List.of(regionCode);
            case SELF_AND_CHILDREN -> rowScopeMapper.selectRegionAndDirectChildCodes(regionCode);
            case SELF_AND_DESCENDANTS -> rowScopeMapper.selectRegionAndDescendantCodes(regionCode);
        };
    }

    private DataDatasetRowScopeCondition buildInCondition(String column, String paramPrefix, Iterable<?> values) {
        Map<String, Object> params = new LinkedHashMap<>();
        List<String> placeholders = new ArrayList<>();
        int index = 0;
        Set<Object> uniqueValues = new LinkedHashSet<>();
        for (Object value : values) {
            if (value != null) {
                uniqueValues.add(value);
            }
        }
        for (Object value : uniqueValues) {
            String paramName = paramPrefix + index++;
            params.put(paramName, value);
            placeholders.add(":" + paramName);
        }
        if (placeholders.isEmpty()) {
            return denyAllCondition();
        }
        return DataDatasetRowScopeCondition.of("AND " + column + " IN (" + String.join(", ", placeholders) + ")",
                params);
    }

    private DataDatasetRowScopeCondition denyAllCondition() {
        return DataDatasetRowScopeCondition.of("AND 1 = 0", new LinkedHashMap<>());
    }

    private void requireColumn(String column, String label) {
        if (isBlank(column)) {
            throw new BusinessException("数据集行权限已启用，但未配置" + label);
        }
    }

    private String resolveColumn(String column, DbDialect dialect) {
        String trimmed = column.trim();
        if (trimmed.contains(".") || trimmed.contains("(") || trimmed.contains(")") || trimmed.contains("`")
                || trimmed.contains("\"") || trimmed.contains(" ")) {
            return trimmed;
        }
        return dialect.quoteIdentifier(trimmed);
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
