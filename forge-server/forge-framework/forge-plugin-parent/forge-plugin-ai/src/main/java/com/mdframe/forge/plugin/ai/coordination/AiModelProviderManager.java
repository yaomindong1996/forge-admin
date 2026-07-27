package com.mdframe.forge.plugin.ai.coordination;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.dto.AiModelSaveDTO;
import com.mdframe.forge.plugin.ai.model.service.AiModelService;
import com.mdframe.forge.plugin.ai.provider.dto.AiProviderSaveDTO;
import com.mdframe.forge.plugin.ai.provider.service.AiProviderService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelProviderManager {

    private final AiModelService modelService;
    private final AiProviderService providerService;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public void createModel(AiModelSaveDTO model) {
        lockProvider(model.getProviderId());
        modelService.addModel(model);
        syncProvider(model.getProviderId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateModel(AiModelSaveDTO model) {
        AiModel snapshot = requireModelSnapshot(model.getId());
        Set<Long> providerIds = new TreeSet<>();
        if (snapshot.getProviderId() != null) {
            providerIds.add(snapshot.getProviderId());
        }
        if (model.getProviderId() != null) {
            providerIds.add(model.getProviderId());
        }
        lockProviders(providerIds);
        AiModel existing = modelService.getByIdForUpdate(model.getId());
        verifyProviderSnapshot(snapshot, existing);
        modelService.updateModel(model);
        AiModel updated = requireModelSnapshot(model.getId());

        if (updated.getProviderId() == null || !providerIds.contains(updated.getProviderId())) {
            throw new BusinessException("AI模型供应商已变更，请重试");
        }
        providerIds.forEach(this::syncProvider);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        AiModel snapshot = requireModelSnapshot(id);
        lockProvider(snapshot.getProviderId());
        AiModel existing = modelService.getByIdForUpdate(id);
        verifyProviderSnapshot(snapshot, existing);
        modelService.deleteModel(id);
        syncProvider(existing.getProviderId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProvider(AiProviderSaveDTO provider) {
        lockProvider(provider.getId());
        providerService.updateProvider(provider);
        syncProvider(provider.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(Long providerId) {
        lockProvider(providerId);
        long modelCount = modelService.countByProviderId(providerId);
        if (modelCount > 0) {
            throw new BusinessException("该供应商下存在 " + modelCount + " 个关联模型，请先删除关联模型");
        }
        providerService.deleteProvider(providerId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefaultProvider(Long providerId) {
        if (providerId == null) {
            throw new BusinessException("AI供应商ID不能为空");
        }
        Long tenantId = requireTenantId();
        List<Long> lockedProviderIds = providerService.lockAllForDefaultSwitch(tenantId);
        if (!lockedProviderIds.contains(providerId)) {
            throw new BusinessException("AI供应商不存在");
        }
        providerService.switchDefaultProvider(tenantId, providerId);
    }

    private Long requireTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("无法确定当前租户");
        }
        return tenantId;
    }

    private void lockProviders(Set<Long> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) {
            return;
        }
        new TreeSet<>(providerIds).forEach(providerService::lockForModelSummary);
    }

    private void lockProvider(Long providerId) {
        if (providerId == null) {
            throw new BusinessException("AI供应商ID不能为空");
        }
        providerService.lockForModelSummary(providerId);
    }

    private AiModel requireModelSnapshot(Long modelId) {
        if (modelId == null) {
            throw new BusinessException("模型ID不能为空");
        }
        AiModel model = modelService.getById(modelId);
        if (model == null) {
            throw new BusinessException("模型不存在: " + modelId);
        }
        return model;
    }

    private void verifyProviderSnapshot(AiModel snapshot, AiModel lockedModel) {
        if (!Objects.equals(snapshot.getProviderId(), lockedModel.getProviderId())) {
            throw new BusinessException("AI模型供应商已变更，请重试");
        }
    }

    private void syncProvider(Long providerId) {
        List<String> modelIds = modelService.getModelIdListByProviderId(providerId);
        String defaultModel = modelService.getDefaultModelId(providerId);
        try {
            providerService.updateModelSummary(
                    providerId, objectMapper.writeValueAsString(modelIds), defaultModel);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("AI供应商模型摘要同步失败", exception);
        }
        log.info("[AI模型同步] 已同步, providerId={}, modelCount={}",
                providerId, modelIds.size());
    }
}
