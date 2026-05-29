package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectDesignVersion;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignVersionDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectDesignVersionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectDesignVersionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 业务对象设计版本服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessObjectDesignVersionService
        extends ServiceImpl<BusinessObjectDesignVersionMapper, AiBusinessObjectDesignVersion> {

    private final ObjectMapper objectMapper;
    private final BusinessObjectService objectService;

    public List<BusinessObjectDesignVersionVO> listByObjectId(Long objectId) {
        requireObjectId(objectId);
        return baseMapper.selectByObjectId(resolveTenantId(), objectId).stream()
                .map(this::toVO)
                .toList();
    }

    public BusinessObjectDesignVersionVO detail(Long objectId, Long versionId) {
        requireObjectId(objectId);
        if (versionId == null) {
            throw new BusinessException("设计版本ID不能为空");
        }
        AiBusinessObjectDesignVersion version = baseMapper.selectVersionById(resolveTenantId(), objectId, versionId);
        if (version == null) {
            throw new BusinessException("设计版本不存在");
        }
        return toVO(version);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createVersion(BusinessObjectDesignVersionDTO dto) {
        if (dto == null || dto.getObjectId() == null) {
            throw new BusinessException("业务对象ID不能为空");
        }
        AiBusinessObject object = objectService.requireEntity(dto.getObjectId());
        AiBusinessObjectDesignVersion version = new AiBusinessObjectDesignVersion();
        version.setTenantId(resolveTenantId());
        version.setObjectId(object.getId());
        version.setSuiteCode(StringUtils.defaultIfBlank(dto.getSuiteCode(), object.getSuiteCode()));
        version.setObjectCode(StringUtils.defaultIfBlank(dto.getObjectCode(), object.getObjectCode()));
        version.setConfigId(dto.getConfigId());
        version.setConfigKey(StringUtils.trimToNull(dto.getConfigKey()));
        version.setCrudConfigVersionId(dto.getCrudConfigVersionId());
        version.setVersionNo(dto.getVersionNo() == null ? nextVersionNo(object.getId()) : dto.getVersionNo());
        version.setVersionType(StringUtils.defaultIfBlank(dto.getVersionType(), "draft").toLowerCase(Locale.ROOT));
        version.setModelSnapshot(writeJson(dto.getModelSnapshot()));
        version.setPageSnapshot(writeJson(dto.getPageSnapshot()));
        version.setRelationSnapshot(writeJson(dto.getRelationSnapshot()));
        version.setPublishStatus(StringUtils.defaultIfBlank(dto.getPublishStatus(), "DRAFT").toUpperCase(Locale.ROOT));
        version.setPublishVersion(dto.getPublishVersion());
        version.setRemark(StringUtils.trimToNull(dto.getRemark()));
        save(version);
        return version.getId();
    }

    public Integer nextVersionNo(Long objectId) {
        requireObjectId(objectId);
        Integer maxVersionNo = baseMapper.selectMaxVersionNo(resolveTenantId(), objectId);
        return maxVersionNo == null ? 1 : maxVersionNo + 1;
    }

    private BusinessObjectDesignVersionVO toVO(AiBusinessObjectDesignVersion version) {
        BusinessObjectDesignVersionVO vo = new BusinessObjectDesignVersionVO();
        vo.setId(version.getId());
        vo.setObjectId(version.getObjectId());
        vo.setSuiteCode(version.getSuiteCode());
        vo.setObjectCode(version.getObjectCode());
        vo.setConfigId(version.getConfigId());
        vo.setConfigKey(version.getConfigKey());
        vo.setCrudConfigVersionId(version.getCrudConfigVersionId());
        vo.setVersionNo(version.getVersionNo());
        vo.setVersionType(version.getVersionType());
        vo.setModelSnapshot(readJson(version.getModelSnapshot(), LowcodeModelSchema.class));
        vo.setPageSnapshot(readJson(version.getPageSnapshot(), LowcodePageSchema.class));
        vo.setRelationSnapshot(readJson(version.getRelationSnapshot(), Object.class));
        vo.setPublishStatus(version.getPublishStatus());
        vo.setPublishVersion(version.getPublishVersion());
        vo.setRemark(version.getRemark());
        vo.setCreateTime(version.getCreateTime());
        vo.setCreateBy(version.getCreateBy());
        return vo;
    }

    private void requireObjectId(Long objectId) {
        if (objectId == null) {
            throw new BusinessException("业务对象ID不能为空");
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException("设计版本快照序列化失败");
        }
    }

    private <T> T readJson(String json, Class<T> type) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new BusinessException("设计版本快照格式不正确");
        }
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
