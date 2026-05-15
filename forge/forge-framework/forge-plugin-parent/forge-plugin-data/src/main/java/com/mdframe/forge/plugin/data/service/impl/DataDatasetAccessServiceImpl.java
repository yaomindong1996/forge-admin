package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.dto.DataDatasetAclDTO;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetAcl;
import com.mdframe.forge.plugin.data.enums.DataDatasetAccessLevelEnum;
import com.mdframe.forge.plugin.data.enums.DataDatasetAccessModeEnum;
import com.mdframe.forge.plugin.data.enums.DataDatasetAclSubjectTypeEnum;
import com.mdframe.forge.plugin.data.mapper.DataDatasetAclMapper;
import com.mdframe.forge.plugin.data.service.DataDatasetAccessService;
import com.mdframe.forge.plugin.data.support.DataDatasetAccessQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DataDatasetAccessServiceImpl extends ServiceImpl<DataDatasetAclMapper, DataDatasetAcl>
        implements DataDatasetAccessService {

    private final DataDatasetAclMapper aclMapper;

    @Override
    public DataDatasetAccessQuery buildCurrentUserAccessQuery(DataDatasetAccessLevelEnum requiredLevel) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        DataDatasetAccessQuery query = new DataDatasetAccessQuery();
        query.setTenantId(SessionHelper.getTenantId());
        query.setAccessLevels(DataDatasetAccessLevelEnum.allowedLevels(requiredLevel));
        if (loginUser == null) {
            return query;
        }
        query.setUserId(loginUser.getUserId());
        query.setRoleIds(loginUser.getRoleIds());
        query.setOrgIds(loginUser.getOrgIds());
        query.setSkipAccessFilter(loginUser.isAdmin() || loginUser.isTenantAdmin());
        return query;
    }

    @Override
    public boolean canAccess(DataDataset dataset, DataDatasetAccessLevelEnum requiredLevel) {
        if (dataset == null) {
            return false;
        }
        DataDatasetAccessLevelEnum normalizedLevel = requiredLevel != null ? requiredLevel : DataDatasetAccessLevelEnum.VIEW;
        if (!DataDatasetAccessModeEnum.isPrivate(dataset.getAccessMode())
                && normalizedLevel != DataDatasetAccessLevelEnum.MANAGE) {
            return true;
        }
        DataDatasetAccessQuery accessQuery = buildCurrentUserAccessQuery(normalizedLevel);
        if (accessQuery.isSkipAccessFilter()) {
            return true;
        }
        Long userId = accessQuery.getUserId();
        if (userId != null && userId.equals(dataset.getCreateBy())) {
            return true;
        }
        if (accessQuery.getTenantId() == null || userId == null) {
            return false;
        }
        return aclMapper.countMatchedAcl(dataset.getId(), accessQuery) > 0;
    }

    @Override
    public void requireAccess(DataDataset dataset, DataDatasetAccessLevelEnum requiredLevel) {
        if (!canAccess(dataset, requiredLevel)) {
            throw new BusinessException("无权访问该数据集");
        }
    }

    @Override
    public List<DataDatasetAcl> listDatasetAcl(Long datasetId) {
        return aclMapper.selectByDatasetId(SessionHelper.getTenantId(), datasetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDatasetAcl(Long datasetId, List<DataDatasetAclDTO> aclItems) {
        Long tenantId = SessionHelper.getTenantId();
        aclMapper.deleteByDatasetId(tenantId, datasetId);
        if (aclItems == null || aclItems.isEmpty()) {
            return;
        }

        Long userId = SessionHelper.getUserId();
        Long deptId = SessionHelper.getMainOrgId();
        Set<String> uniqueKeys = new HashSet<>();
        List<DataDatasetAcl> entities = new ArrayList<>();
        for (DataDatasetAclDTO item : aclItems) {
            if (item == null) {
                continue;
            }
            String subjectType = normalizeSubjectType(item.getSubjectType());
            Long subjectId = item.getSubjectId();
            if (!DataDatasetAclSubjectTypeEnum.isValid(subjectType) || subjectId == null) {
                throw new BusinessException("数据集授权主体无效");
            }
            DataDatasetAccessLevelEnum accessLevel = DataDatasetAccessLevelEnum.getByCode(item.getAccessLevel());
            String uniqueKey = subjectType + ":" + subjectId + ":" + accessLevel.getCode();
            if (!uniqueKeys.add(uniqueKey)) {
                continue;
            }
            DataDatasetAcl entity = new DataDatasetAcl();
            entity.setTenantId(tenantId);
            entity.setDatasetId(datasetId);
            entity.setSubjectType(subjectType);
            entity.setSubjectId(subjectId);
            entity.setAccessLevel(accessLevel.getCode());
            entity.setCreateBy(userId);
            entity.setCreateDept(deptId);
            entity.setUpdateBy(userId);
            entities.add(entity);
        }
        if (!entities.isEmpty()) {
            saveBatch(entities);
        }
    }

    @Override
    public void deleteDatasetAcl(Long datasetId) {
        aclMapper.deleteByDatasetId(SessionHelper.getTenantId(), datasetId);
    }

    private String normalizeSubjectType(String subjectType) {
        return subjectType == null ? null : subjectType.trim().toUpperCase(Locale.ROOT);
    }
}
