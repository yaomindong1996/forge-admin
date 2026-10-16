package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.FlowEntryDTO;
import com.mdframe.forge.starter.flow.dto.FlowEntryQueryDTO;
import com.mdframe.forge.starter.flow.entity.FlowEntry;
import com.mdframe.forge.starter.flow.enums.FlowEnableStatus;
import com.mdframe.forge.starter.flow.entity.FlowEntryFieldMapping;
import com.mdframe.forge.starter.flow.entity.FlowFormVersion;
import com.mdframe.forge.starter.flow.mapper.FlowEntryFieldMappingMapper;
import com.mdframe.forge.starter.flow.mapper.FlowEntryMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormVersionMapper;
import com.mdframe.forge.starter.flow.service.FlowEntryService;
import com.mdframe.forge.starter.flow.vo.FlowEntryRuntimeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程入口服务实现。
 */
@Service
@RequiredArgsConstructor
public class FlowEntryServiceImpl extends ServiceImpl<FlowEntryMapper, FlowEntry> implements FlowEntryService {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final FlowEntryMapper flowEntryMapper;
    private final FlowEntryFieldMappingMapper mappingMapper;
    private final FlowFormVersionMapper formVersionMapper;

    @Override
    public IPage<FlowEntry> pageEntries(FlowEntryQueryDTO query, Integer pageNum, Integer pageSize) {
        return flowEntryMapper.selectEntryPage(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public FlowEntry getEntryDetail(Long id) {
        FlowEntry entry = getById(id);
        if (entry != null) {
            entry.setFieldMappings(mappingMapper.selectByEntryId(entry.getId()));
        }
        return entry;
    }

    @Override
    public FlowEntry getByEntryCode(String entryCode) {
        if (!StringUtils.hasText(entryCode)) {
            return null;
        }
        FlowEntry entry = flowEntryMapper.selectByEntryCode(entryCode);
        if (entry != null) {
            entry.setFieldMappings(mappingMapper.selectByEntryId(entry.getId()));
        }
        return entry;
    }

    @Override
    public FlowEntryRuntimeVO getRuntimeEntry(String entryCode) {
        FlowEntry entry = getByEntryCode(entryCode);
        if (entry == null || !FlowEnableStatus.ENABLED.matches(entry.getStatus())) {
            throw new RuntimeException("流程入口不存在或未启用：" + entryCode);
        }
        FlowFormVersion version = formVersionMapper.selectByIdForRuntime(entry.getFormVersionId());
        if (version == null) {
            throw new RuntimeException("入口绑定的表单版本不存在");
        }

        FlowEntryRuntimeVO vo = new FlowEntryRuntimeVO();
        vo.setEntry(entry);
        vo.setFormVersion(version);
        vo.setFormSchema(version.getFormSchema());
        vo.setFieldRegistry(version.getFieldRegistry());
        vo.setFieldMappings(entry.getFieldMappings());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEntry(FlowEntryDTO dto) {
        validateEntry(dto);
        Long duplicateCount = flowEntryMapper.countByEntryCode(dto.getEntryCode(), dto.getId());
        if (duplicateCount != null && duplicateCount > 0) {
            throw new RuntimeException("入口编码已存在：" + dto.getEntryCode());
        }
        FlowFormVersion version = formVersionMapper.selectByIdForRuntime(dto.getFormVersionId());
        if (version == null) {
            throw new RuntimeException("入口必须绑定已发布表单版本");
        }

        FlowEntry entry = toEntity(dto);
        if (!StringUtils.hasText(entry.getFormKey())) {
            entry.setFormKey(version.getFormKey());
        }
        if (!StringUtils.hasText(entry.getDataMode())) {
            entry.setDataMode(version.getDefaultDataMode());
        }
        if (entry.getStatus() == null) {
            entry.setStatus(FlowEnableStatus.ENABLED.getCode());
        }
        if (entry.getSort() == null) {
            entry.setSort(0);
        }
        if (entry.getTenantId() == null) {
            entry.setTenantId(resolveTenantId());
        }

        if (entry.getId() == null) {
            save(entry);
        } else {
            updateById(entry);
            mappingMapper.deleteByEntryId(entry.getId());
        }
        saveMappings(entry.getId(), dto.getFieldMappings());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEntry(Long id) {
        mappingMapper.deleteByEntryId(id);
        removeById(id);
    }

    private void validateEntry(FlowEntryDTO dto) {
        if (dto == null) {
            throw new RuntimeException("入口配置不能为空");
        }
        if (!StringUtils.hasText(dto.getEntryCode())) {
            throw new RuntimeException("入口编码不能为空");
        }
        if (!StringUtils.hasText(dto.getEntryName())) {
            throw new RuntimeException("入口名称不能为空");
        }
        if (!StringUtils.hasText(dto.getModelKey())) {
            throw new RuntimeException("流程模型Key不能为空");
        }
        if (dto.getFormVersionId() == null) {
            throw new RuntimeException("表单版本不能为空");
        }
    }

    private FlowEntry toEntity(FlowEntryDTO dto) {
        FlowEntry entry = new FlowEntry();
        entry.setId(dto.getId());
        entry.setEntryCode(dto.getEntryCode());
        entry.setEntryName(dto.getEntryName());
        entry.setEntryDesc(dto.getEntryDesc());
        entry.setModelKey(dto.getModelKey());
        entry.setFormKey(dto.getFormKey());
        entry.setFormVersionId(dto.getFormVersionId());
        entry.setDataMode(dto.getDataMode());
        entry.setObjectCode(dto.getObjectCode());
        entry.setConfigKey(dto.getConfigKey());
        entry.setVisibleScope(dto.getVisibleScope());
        entry.setTitleTemplate(dto.getTitleTemplate());
        entry.setBusinessKeyTemplate(dto.getBusinessKeyTemplate());
        entry.setSubmitStrategy(dto.getSubmitStrategy());
        entry.setStatus(dto.getStatus());
        entry.setSort(dto.getSort());
        return entry;
    }

    private void saveMappings(Long entryId, List<FlowEntryFieldMapping> mappings) {
        List<FlowEntryFieldMapping> safeMappings = mappings == null ? new ArrayList<>() : mappings;
        int sort = 0;
        for (FlowEntryFieldMapping mapping : safeMappings) {
            if (mapping == null || !StringUtils.hasText(mapping.getFormField())) {
                continue;
            }
            mapping.setId(null);
            mapping.setEntryId(entryId);
            mapping.setTenantId(resolveTenantId());
            mapping.setSort(mapping.getSort() == null ? sort : mapping.getSort());
            mapping.setRequired(mapping.getRequired() == null ? 0 : mapping.getRequired());
            if (!StringUtils.hasText(mapping.getTargetType())) {
                mapping.setTargetType("FLOW_VARIABLE");
            }
            mappingMapper.insert(mapping);
            sort++;
        }
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
        } catch (Exception e) {
            return DEFAULT_TENANT_ID;
        }
    }
}
