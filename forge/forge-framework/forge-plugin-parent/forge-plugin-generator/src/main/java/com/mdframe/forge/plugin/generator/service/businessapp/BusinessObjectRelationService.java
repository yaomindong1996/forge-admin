package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectRelationDTO;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 业务应用平台对象关系服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectRelationService extends ServiceImpl<BusinessObjectRelationMapper, AiBusinessObjectRelation> {

    private static final Set<String> RELATION_TYPES = Set.of("REFERENCE", "DETAIL", "CHILD_LIST", "MANY_TO_MANY");

    private final BusinessObjectService objectService;

    public List<BusinessObjectRelationVO> listByObject(Long objectId) {
        AiBusinessObject object = objectService.requireEntity(objectId);
        return baseMapper.selectRelationsByObject(resolveTenantId(), object.getSuiteCode(), object.getObjectCode());
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRelations(Long objectId, List<BusinessObjectRelationDTO> relations) {
        AiBusinessObject sourceObject = objectService.requireEntity(objectId);
        List<Long> savedIds = new ArrayList<>();
        if (relations != null) {
            for (BusinessObjectRelationDTO dto : relations) {
                if (dto == null) {
                    continue;
                }
                AiBusinessObjectRelation relation = dto.getId() == null
                        ? new AiBusinessObjectRelation()
                        : requireRelationInObjectScope(sourceObject, dto.getId());
                copyDtoToEntity(dto, relation, sourceObject, dto.getId() == null);
                if (dto.getId() == null) {
                    save(relation);
                } else {
                    updateById(relation);
                }
                savedIds.add(relation.getId());
            }
        }
        baseMapper.deleteMissingRelations(
                resolveTenantId(), sourceObject.getSuiteCode(), sourceObject.getObjectCode(), savedIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRelation(Long objectId, Long relationId) {
        AiBusinessObject object = objectService.requireEntity(objectId);
        AiBusinessObjectRelation relation = requireRelation(relationId);
        if (!object.getSuiteCode().equals(relation.getSuiteCode())
                || (!object.getObjectCode().equals(relation.getSourceObjectCode())
                && !object.getObjectCode().equals(relation.getTargetObjectCode()))) {
            throw new BusinessException("对象关系不属于当前业务对象");
        }
        removeById(relation.getId());
    }

    private void copyDtoToEntity(BusinessObjectRelationDTO dto, AiBusinessObjectRelation relation,
                                 AiBusinessObject sourceObject, boolean create) {
        String targetObjectCode = StringUtils.trimToNull(dto.getTargetObjectCode());
        String relationType = StringUtils.defaultIfBlank(dto.getRelationType(), "REFERENCE").toUpperCase();
        String relationName = StringUtils.trimToNull(dto.getRelationName());
        if (StringUtils.isBlank(targetObjectCode)) {
            throw new BusinessException("目标业务对象不能为空");
        }
        if (!RELATION_TYPES.contains(relationType)) {
            throw new BusinessException("对象关系类型不正确");
        }
        if (StringUtils.isBlank(relationName)) {
            throw new BusinessException("对象关系名称不能为空");
        }
        objectService.requireByCode(sourceObject.getSuiteCode(), targetObjectCode);
        Long excludeId = create ? null : relation.getId();
        if (baseMapper.countByScope(resolveTenantId(), sourceObject.getSuiteCode(), sourceObject.getObjectCode(),
                targetObjectCode, relationType, relationName, excludeId) > 0) {
            throw new BusinessException("对象关系已存在: " + relationName);
        }
        relation.setTenantId(resolveTenantId());
        relation.setSuiteCode(sourceObject.getSuiteCode());
        relation.setSourceObjectCode(sourceObject.getObjectCode());
        relation.setTargetObjectCode(targetObjectCode);
        relation.setRelationType(relationType);
        relation.setRelationName(relationName);
        relation.setSourceFieldCode(StringUtils.trimToNull(dto.getSourceFieldCode()));
        relation.setTargetFieldCode(StringUtils.trimToNull(dto.getTargetFieldCode()));
        relation.setRelationConfig(StringUtils.trimToNull(dto.getRelationConfig()));
        relation.setDescription(StringUtils.trimToNull(dto.getDescription()));
        relation.setStatus(normalizeStatus(dto.getStatus()));
        relation.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    private AiBusinessObjectRelation requireRelationInObjectScope(AiBusinessObject object, Long relationId) {
        AiBusinessObjectRelation relation = requireRelation(relationId);
        if (!object.getSuiteCode().equals(relation.getSuiteCode())
                || !object.getObjectCode().equals(relation.getSourceObjectCode())) {
            throw new BusinessException("对象关系不属于当前业务对象");
        }
        return relation;
    }

    private AiBusinessObjectRelation requireRelation(Long id) {
        if (id == null) {
            throw new BusinessException("对象关系ID不能为空");
        }
        AiBusinessObjectRelation relation = baseMapper.selectRelationById(resolveTenantId(), id);
        if (relation == null) {
            throw new BusinessException("对象关系不存在");
        }
        return relation;
    }

    private Integer normalizeStatus(Integer status) {
        int value = status == null ? 1 : status;
        if (value != 0 && value != 1) {
            throw new BusinessException("状态值不正确");
        }
        return value;
    }

    private Long resolveTenantId() {
        Long tenantId;
        try {
            tenantId = SessionHelper.getTenantId();
        } catch (Exception e) {
            tenantId = null;
        }
        return tenantId != null ? tenantId : 1L;
    }
}
