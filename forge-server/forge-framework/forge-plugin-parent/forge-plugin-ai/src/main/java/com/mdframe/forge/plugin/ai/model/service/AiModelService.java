package com.mdframe.forge.plugin.ai.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.plugin.ai.model.capability.domain.AiModelCapability;
import com.mdframe.forge.plugin.ai.model.capability.mapper.AiModelCapabilityMapper;
import com.mdframe.forge.plugin.ai.model.dto.AiModelSaveDTO;
import com.mdframe.forge.plugin.ai.model.vo.AiModelVO;
import com.mdframe.forge.plugin.ai.routing.constant.AiModelCapabilityCode;
import com.mdframe.forge.plugin.ai.health.AiModelHealthKey;
import com.mdframe.forge.plugin.ai.health.AiModelHealthRegistry;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 模型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService extends ServiceImpl<AiModelMapper, AiModel> {

    private final AiModelCapabilityMapper capabilityMapper;
    private final AiModelHealthRegistry healthRegistry;

    @Transactional(rollbackFor = Exception.class)
    public Long addModel(AiModelSaveDTO dto) {
        validateGovernance(dto);
        AiModel model = fromDto(dto);
        addModel(model);
        replaceCapabilities(model.getId(), dto.getCapabilityCodes());
        return model.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateModel(AiModelSaveDTO dto) {
        if (dto == null || dto.getId() == null) throw new BusinessException("模型ID不能为空");
        validateGovernance(dto);
        AiModel model = fromDto(dto);
        updateModel(model);
        replaceCapabilities(model.getId(), dto.getCapabilityCodes());
        AiModel persisted = getById(model.getId());
        if (persisted != null && persisted.getTenantId() != null) {
            resetHealthAfterCommit(new AiModelHealthKey(
                    persisted.getTenantId(), persisted.getProviderId(), persisted.getId()));
        }
    }

    public AiModelVO toView(AiModel model) {
        return toViews(List.of(model)).get(0);
    }

    public List<AiModelVO> toViews(List<AiModel> models) {
        if (models == null || models.isEmpty()) return List.of();
        Map<Long, Set<String>> capabilities = selectEnabledCapabilityCodes(
                models.stream().map(AiModel::getId).toList());
        return models.stream().map(model -> {
        AiModelVO vo = new AiModelVO();
        org.springframework.beans.BeanUtils.copyProperties(model, vo);
        vo.setCapabilityCodes(capabilities.getOrDefault(model.getId(), Set.of()).stream().sorted().toList());
        if (model.getTenantId() != null) {
            vo.setHealthStatus(healthRegistry.snapshot(new AiModelHealthKey(model.getTenantId(), model.getProviderId(), model.getId())).status().name());
        } else {
            vo.setHealthStatus("UNKNOWN");
        }
        return vo;
        }).toList();
    }

    public Map<Long, Set<String>> selectEnabledCapabilityCodes(Collection<Long> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) return Collections.emptyMap();
        return capabilityMapper.selectEnabledByModelIds(modelIds).stream().collect(Collectors.groupingBy(
                AiModelCapability::getModelId, Collectors.mapping(AiModelCapability::getCapabilityCode, Collectors.toSet())));
    }

    /**
     * 新增模型 + 双写同步
     */
    @Transactional(rollbackFor = Exception.class)
    public void addModel(AiModel model) {
        // 校验同一供应商下 modelId 唯一
        long count = count(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, model.getProviderId())
                .eq(AiModel::getModelId, model.getModelId()));
        if (count > 0) {
            throw new BusinessException("同一供应商下模型标识已存在: " + model.getModelId());
        }

        // 如果设为默认模型，先清除该供应商下其他默认
        if (AiConstants.IS_DEFAULT_YES.equals(model.getIsDefault())) {
            update(new LambdaUpdateWrapper<AiModel>()
                    .set(AiModel::getIsDefault, AiConstants.IS_DEFAULT_NO)
                    .eq(AiModel::getProviderId, model.getProviderId())
                    .eq(AiModel::getIsDefault, AiConstants.IS_DEFAULT_YES));
        }

        save(model);
        log.info("[AI模型] 新增模型, providerId={}, modelId={}", model.getProviderId(), model.getModelId());
    }

    /**
     * 修改模型 + 双写同步
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateModel(AiModel model) {
        AiModel existing = getById(model.getId());
        if (existing == null) {
            throw new BusinessException("模型不存在: " + model.getId());
        }

        // 如果修改了 modelId，校验同一供应商下唯一
        if (model.getModelId() != null && !model.getModelId().equals(existing.getModelId())) {
            long count = count(new LambdaQueryWrapper<AiModel>()
                    .eq(AiModel::getProviderId, existing.getProviderId())
                    .eq(AiModel::getModelId, model.getModelId()));
            if (count > 0) {
                throw new BusinessException("同一供应商下模型标识已存在: " + model.getModelId());
            }
        }

        // 如果设为默认模型，先清除该供应商下其他默认
        if (AiConstants.IS_DEFAULT_YES.equals(model.getIsDefault())) {
            update(new LambdaUpdateWrapper<AiModel>()
                    .set(AiModel::getIsDefault, AiConstants.IS_DEFAULT_NO)
                    .eq(AiModel::getProviderId, existing.getProviderId())
                    .eq(AiModel::getIsDefault, AiConstants.IS_DEFAULT_YES));
        }

        updateById(model);
        log.info("[AI模型] 修改模型, id={}", model.getId());
    }

    /**
     * 删除模型 + 双写同步
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        AiModel existing = getById(id);
        if (existing == null) {
            throw new BusinessException("模型不存在: " + id);
        }

        removeById(id);
        log.info("[AI模型] 删除模型, id={}, providerId={}, modelId={}", id, existing.getProviderId(), existing.getModelId());
    }

    /**
     * 按供应商查询模型列表
     */
    public List<AiModel> listByProviderId(Long providerId) {
        return list(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, providerId)
                .eq(AiModel::getStatus, AiConstants.STATUS_NORMAL)
                .orderByAsc(AiModel::getSortOrder));
    }

    /**
     * 查询供应商下所有模型（含停用），用于双写同步
     */
    public List<AiModel> listAllByProviderId(Long providerId) {
        return list(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, providerId)
                .orderByAsc(AiModel::getSortOrder));
    }

    /**
     * 统计供应商下的模型数量
     */
    public long countByProviderId(Long providerId) {
        return count(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, providerId));
    }

    /**
     * 聚合供应商下的模型ID列表（用于双写同步）
     */
    public List<String> getModelIdListByProviderId(Long providerId) {
        return listAllByProviderId(providerId).stream()
                .map(AiModel::getModelId)
                .collect(Collectors.toList());
    }

    /**
     * 获取供应商下的默认模型ID（用于双写同步）
     */
    public String getDefaultModelId(Long providerId) {
        return listAllByProviderId(providerId).stream()
                .filter(m -> AiConstants.IS_DEFAULT_YES.equals(m.getIsDefault()))
                .map(AiModel::getModelId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取运行时权威默认模型。
     *
     * <p>运行时只接受 ai_model 中启用且标记为默认的记录，不读取 ai_provider 的兼容双写字段。</p>
     *
     * @param providerId 供应商 ID
     * @return 默认模型标识
     */
    public String requireEnabledDefaultModelId(Long providerId) {
        if (providerId == null) {
            throw new BusinessException("AI供应商ID不能为空");
        }
        String modelId = baseMapper.selectEnabledDefaultModelId(providerId);
        if (!StringUtils.hasText(modelId)) {
            throw new BusinessException("请为供应商设置默认模型");
        }
        return modelId.trim();
    }

    private void validateGovernance(AiModelSaveDTO dto) {
        if (dto == null) throw new BusinessException("模型配置不能为空");
        if (dto.getContextWindow() != null && dto.getContextWindow() < 0) throw new BusinessException("上下文窗口不能为负数");
        if (dto.getInputPricePerMillionCent() != null && dto.getInputPricePerMillionCent() < 0) throw new BusinessException("输入价格不能为负数");
        if (dto.getOutputPricePerMillionCent() != null && dto.getOutputPricePerMillionCent() < 0) throw new BusinessException("输出价格不能为负数");
        if (dto.getCapabilityCodes() != null) dto.getCapabilityCodes().forEach(AiModelCapabilityCode::require);
    }

    private AiModel fromDto(AiModelSaveDTO dto) {
        AiModel model = new AiModel();
        org.springframework.beans.BeanUtils.copyProperties(dto, model, "capabilityCodes");
        return model;
    }

    private void replaceCapabilities(Long modelId, List<String> codes) {
        capabilityMapper.logicallyDeleteByModelId(modelId);
        if (codes == null) return;
        for (String code : new HashSet<>(codes)) {
            AiModelCapability capability = new AiModelCapability();
            capability.setModelId(modelId); capability.setCapabilityCode(AiModelCapabilityCode.require(code)); capability.setStatus("0"); capabilityMapper.insert(capability);
        }
    }

    private void resetHealthAfterCommit(AiModelHealthKey key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    healthRegistry.reset(key);
                }
            });
            return;
        }
        healthRegistry.reset(key);
    }
}
