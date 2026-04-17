package com.mdframe.forge.plugin.ai.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.constant.AiConstants;
import com.mdframe.forge.plugin.ai.model.domain.AiModel;
import com.mdframe.forge.plugin.ai.model.mapper.AiModelMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService extends ServiceImpl<AiModelMapper, AiModel> {

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
}
