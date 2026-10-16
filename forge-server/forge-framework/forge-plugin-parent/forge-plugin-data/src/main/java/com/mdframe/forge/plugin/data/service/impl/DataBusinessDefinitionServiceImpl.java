package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.dto.DataBusinessDatasetDTO;
import com.mdframe.forge.plugin.data.dto.DataBusinessDefinitionSaveDTO;
import com.mdframe.forge.plugin.data.entity.DataBusinessDataset;
import com.mdframe.forge.plugin.data.entity.DataBusinessDefinition;
import com.mdframe.forge.plugin.data.entity.DataDataset;
import com.mdframe.forge.plugin.data.entity.DataDatasetField;
import com.mdframe.forge.plugin.data.enums.DataDatasetAccessLevelEnum;
import com.mdframe.forge.plugin.data.enums.DatasetPublishStatusEnum;
import com.mdframe.forge.plugin.data.mapper.DataBusinessDatasetMapper;
import com.mdframe.forge.plugin.data.mapper.DataBusinessDefinitionMapper;
import com.mdframe.forge.plugin.data.service.DataBusinessDefinitionService;
import com.mdframe.forge.plugin.data.service.DataDatasetAccessService;
import com.mdframe.forge.plugin.data.service.DataDatasetFieldService;
import com.mdframe.forge.plugin.data.service.DataDatasetService;
import com.mdframe.forge.plugin.data.support.DataDatasetFieldViewAssembler;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.plugin.data.vo.DataBusinessAiContextVO;
import com.mdframe.forge.plugin.data.vo.DataBusinessDatasetVO;
import com.mdframe.forge.plugin.data.vo.DataBusinessDefinitionDetailVO;
import com.mdframe.forge.plugin.data.vo.DataDatasetFieldVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataBusinessDefinitionServiceImpl
    extends ServiceImpl<DataBusinessDefinitionMapper, DataBusinessDefinition>
    implements DataBusinessDefinitionService {

    private static final String SENSITIVE_LEVEL_HIDDEN = "HIDDEN";
    private static final String SENSITIVE_LEVEL_MASK = "MASK";

    private final DataBusinessDefinitionMapper businessMapper;
    private final DataBusinessDatasetMapper businessDatasetMapper;
    private final DataDatasetService datasetService;
    private final DataDatasetFieldService datasetFieldService;
    private final DataDatasetAccessService datasetAccessService;
    private final DataDatasetFieldViewAssembler fieldViewAssembler;

    @Override
    public IPage<DataBusinessDefinition> page(String businessName, Integer status, Integer pageNum, Integer pageSize) {
        Page<DataBusinessDefinition> page = new Page<>(pageNum, pageSize);
        return businessMapper.selectBusinessPage(page, SessionHelper.getTenantId(), businessName, status);
    }

    @Override
    public List<DataBusinessDefinition> listEnabled() {
        return businessMapper.selectBusinessList(SessionHelper.getTenantId(), 1);
    }

    @Override
    public DataBusinessDefinitionDetailVO getDetail(Long id) {
        DataBusinessDefinition business = requireBusiness(id);
        return toDetailVO(business, listBusinessDatasets(id, false));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataBusinessDefinition saveBusiness(DataBusinessDefinitionSaveDTO dto) {
        validateBusiness(dto);
        DataBusinessDefinition entity = toEntity(dto, null);
        entity.setTenantId(SessionHelper.getTenantId());
        save(entity);
        saveDatasetBindings(entity.getId(), dto.getDatasets());
        return getById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataBusinessDefinition updateBusiness(DataBusinessDefinitionSaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("业务定义ID不能为空");
        }
        DataBusinessDefinition existing = requireBusiness(dto.getId());
        validateBusiness(dto);
        DataBusinessDefinition entity = toEntity(dto, existing);
        entity.setId(existing.getId());
        entity.setTenantId(existing.getTenantId());
        updateById(entity);
        saveDatasetBindings(entity.getId(), dto.getDatasets());
        return getById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBusiness(Long id) {
        DataBusinessDefinition business = requireBusiness(id);
        businessDatasetMapper.deleteByBusinessId(SessionHelper.getTenantId(), business.getId());
        removeById(business.getId());
    }

    @Override
    public DataBusinessAiContextVO getAiContext(Long id) {
        DataBusinessDefinition business = requireBusiness(id);
        if (!EnableStatus.ENABLED.matches(business.getStatus())) {
            throw new BusinessException("业务定义已禁用");
        }
        List<DataBusinessDatasetVO> datasets = listBusinessDatasets(id, true);
        DataBusinessAiContextVO vo = new DataBusinessAiContextVO();
        vo.setBusinessId(business.getId());
        vo.setBusinessCode(business.getBusinessCode());
        vo.setBusinessName(business.getBusinessName());
        vo.setBusinessDesc(business.getBusinessDesc());
        vo.setAnalysisGoal(business.getAnalysisGoal());
        vo.setMetricDefinition(business.getMetricDefinition());
        vo.setDimensionDefinition(business.getDimensionDefinition());
        vo.setUsageGuide(business.getUsageGuide());
        vo.setDatasets(datasets);
        return vo;
    }

    private void validateBusiness(DataBusinessDefinitionSaveDTO dto) {
        if (!StringUtils.hasText(dto.getBusinessCode())) {
            throw new BusinessException("业务编码不能为空");
        }
        if (!StringUtils.hasText(dto.getBusinessName())) {
            throw new BusinessException("业务名称不能为空");
        }
        if (!StringUtils.hasText(dto.getBusinessDesc())) {
            throw new BusinessException("业务定义描述不能为空");
        }
        if (dto.getDatasets() == null || dto.getDatasets().isEmpty()) {
            throw new BusinessException("请至少绑定一个数据集");
        }
        DataBusinessDefinition existing = businessMapper.selectBusinessByCode(
            SessionHelper.getTenantId(), dto.getBusinessCode().trim());
        if (existing != null && !Objects.equals(existing.getId(), dto.getId())) {
            throw new BusinessException("业务编码已存在");
        }
        validateDatasetBindings(dto.getDatasets());
    }

    private void validateDatasetBindings(List<DataBusinessDatasetDTO> datasets) {
        Set<Long> datasetIds = new HashSet<>();
        for (DataBusinessDatasetDTO item : datasets) {
            if (item == null || item.getDatasetId() == null) {
                throw new BusinessException("绑定数据集不能为空");
            }
            if (!datasetIds.add(item.getDatasetId())) {
                throw new BusinessException("绑定数据集不能重复");
            }
        }
        // 批量查询所有数据集，避免循环 getById
        Map<Long, DataDataset> datasetMap = datasetService.listByIds(datasetIds).stream()
                .collect(Collectors.toMap(DataDataset::getId, Function.identity(), (left, right) -> left));
        for (DataBusinessDatasetDTO item : datasets) {
            DataDataset dataset = datasetMap.get(item.getDatasetId());
            if (dataset == null) {
                throw new BusinessException("数据集不存在或已删除");
            }
            if (!EnableStatus.ENABLED.matches(dataset.getStatus())) {
                throw new BusinessException("数据集已禁用：" + dataset.getDatasetName());
            }
            if (!DatasetPublishStatusEnum.isPublished(dataset.getPublishStatus())) {
                throw new BusinessException("只能绑定已发布数据集：" + dataset.getDatasetName());
            }
            datasetAccessService.requireAccess(dataset, DataDatasetAccessLevelEnum.QUERY);
        }
    }

    private DataBusinessDefinition toEntity(DataBusinessDefinitionSaveDTO dto, DataBusinessDefinition existing) {
        DataBusinessDefinition entity = new DataBusinessDefinition();
        entity.setBusinessCode(trimToNull(dto.getBusinessCode()));
        entity.setBusinessName(trimToNull(dto.getBusinessName()));
        entity.setBusinessDesc(trimToNull(dto.getBusinessDesc()));
        entity.setAnalysisGoal(trimToNull(dto.getAnalysisGoal()));
        entity.setMetricDefinition(trimToNull(dto.getMetricDefinition()));
        entity.setDimensionDefinition(trimToNull(dto.getDimensionDefinition()));
        entity.setUsageGuide(trimToNull(dto.getUsageGuide()));
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : existing != null ? existing.getStatus() : EnableStatus.ENABLED.getCode());
        return entity;
    }

    private void saveDatasetBindings(Long businessId, List<DataBusinessDatasetDTO> datasets) {
        Long tenantId = SessionHelper.getTenantId();
        businessDatasetMapper.deleteByBusinessId(tenantId, businessId);
        int index = 0;
        for (DataBusinessDatasetDTO dto : datasets) {
            DataBusinessDataset entity = new DataBusinessDataset();
            entity.setTenantId(tenantId);
            entity.setBusinessId(businessId);
            entity.setDatasetId(dto.getDatasetId());
            entity.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : index == 0 ? 1 : 0);
            entity.setSort(dto.getSort() != null ? dto.getSort() : index);
            entity.setUsageRemark(trimToNull(dto.getUsageRemark()));
            businessDatasetMapper.insert(entity);
            index++;
        }
    }

    private DataBusinessDefinition requireBusiness(Long id) {
        DataBusinessDefinition business = getById(id);
        if (business == null) {
            throw new BusinessException("业务定义不存在或已删除");
        }
        return business;
    }

    private List<DataBusinessDatasetVO> listBusinessDatasets(Long businessId, boolean onlyAccessible) {
        List<DataBusinessDataset> bindings = businessDatasetMapper.selectByBusinessId(SessionHelper.getTenantId(), businessId);
        if (bindings.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查询所有 dataset，避免循环 getById
        Set<Long> datasetIds = bindings.stream()
                .map(DataBusinessDataset::getDatasetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DataDataset> datasetMap = datasetIds.isEmpty()
                ? Collections.emptyMap()
                : datasetService.listByIds(datasetIds).stream()
                        .collect(Collectors.toMap(DataDataset::getId, Function.identity(), (left, right) -> left));

        // 批量查询所有字段，避免循环 listByDatasetId
        Map<Long, List<DataDatasetField>> fieldMap = new HashMap<>();
        for (Long datasetId : datasetIds) {
            fieldMap.put(datasetId, datasetFieldService.listByDatasetId(datasetId));
        }

        List<DataBusinessDatasetVO> result = new ArrayList<>();
        for (DataBusinessDataset binding : bindings) {
            DataDataset dataset = datasetMap.get(binding.getDatasetId());
            if (dataset == null || !DatasetPublishStatusEnum.isPublished(dataset.getPublishStatus())) {
                continue;
            }
            if (onlyAccessible && !datasetAccessService.canAccess(dataset, DataDatasetAccessLevelEnum.QUERY)) {
                continue;
            }
            List<DataDatasetField> fields = fieldMap.getOrDefault(binding.getDatasetId(), Collections.emptyList());
            result.add(toDatasetVO(binding, fields, onlyAccessible));
        }
        return result;
    }

    private DataBusinessDefinitionDetailVO toDetailVO(DataBusinessDefinition business, List<DataBusinessDatasetVO> datasets) {
        DataBusinessDefinitionDetailVO vo = new DataBusinessDefinitionDetailVO();
        vo.setId(business.getId());
        vo.setBusinessCode(business.getBusinessCode());
        vo.setBusinessName(business.getBusinessName());
        vo.setBusinessDesc(business.getBusinessDesc());
        vo.setAnalysisGoal(business.getAnalysisGoal());
        vo.setMetricDefinition(business.getMetricDefinition());
        vo.setDimensionDefinition(business.getDimensionDefinition());
        vo.setUsageGuide(business.getUsageGuide());
        vo.setStatus(business.getStatus());
        vo.setDatasetCount(datasets.size());
        vo.setCreateTime(business.getCreateTime());
        vo.setUpdateTime(business.getUpdateTime());
        vo.setDatasets(datasets);
        return vo;
    }

    private DataBusinessDatasetVO toDatasetVO(DataBusinessDataset binding, List<DataDatasetField> fields, boolean sanitizeForAi) {
        DataBusinessDatasetVO vo = new DataBusinessDatasetVO();
        vo.setId(binding.getId());
        vo.setDatasetId(binding.getDatasetId());
        vo.setDatasetCode(binding.getDatasetCode());
        vo.setDatasetName(binding.getDatasetName());
        vo.setDatasetType(binding.getDatasetType());
        vo.setDescription(binding.getDatasetDescription());
        vo.setParamSchemaJson(binding.getParamSchemaJson());
        vo.setIsPrimary(binding.getIsPrimary());
        vo.setSort(binding.getSort());
        vo.setUsageRemark(binding.getUsageRemark());
        List<DataDatasetFieldVO> fieldVos = fieldViewAssembler.toVOList(fields);
        vo.setFields(sanitizeForAi ? sanitizeAiFields(fieldVos) : fieldVos);
        return vo;
    }

    private List<DataDatasetFieldVO> sanitizeAiFields(List<DataDatasetFieldVO> fields) {
        List<DataDatasetFieldVO> result = new ArrayList<>();
        for (DataDatasetFieldVO field : fields) {
            String sensitiveLevel = normalizeSensitiveLevel(field.getSensitiveLevel());
            if (SENSITIVE_LEVEL_HIDDEN.equals(sensitiveLevel)) {
                continue;
            }
            if (SENSITIVE_LEVEL_MASK.equals(sensitiveLevel)) {
                field.setMaskRule(null);
                field.setSourceColumn(null);
                field.setDescription("敏感字段，仅允许脱敏展示或聚合统计");
            }
            result.add(field);
        }
        return result;
    }

    private String normalizeSensitiveLevel(String sensitiveLevel) {
        return StringUtils.hasText(sensitiveLevel) ? sensitiveLevel.trim().toUpperCase() : "";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
