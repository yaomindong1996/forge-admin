package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysDictDataDTO;
import com.mdframe.forge.plugin.system.dto.SysDictDataQuery;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.listener.DictChangeEvent;
import com.mdframe.forge.plugin.system.mapper.SysDictDataMapper;
import com.mdframe.forge.plugin.system.service.ISysDictDataService;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheConfig;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheEvict;
import com.mdframe.forge.starter.cache.managed.annotation.ForgeCacheable;
import com.mdframe.forge.starter.cache.managed.enums.CacheMode;
import com.mdframe.forge.starter.cache.managed.enums.CacheScope;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 字典数据Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ForgeCacheConfig(
        name = "system:dict-data",
        description = "系统字典数据",
        mode = CacheMode.MULTI,
        scope = CacheScope.TENANT,
        localTtlSeconds = 60,
        redisTtlSeconds = 1800,
        localMaxSize = 2000
)
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements ISysDictDataService {

    private static final String DICT_VALUE_DUPLICATED_MESSAGE = "同一字典下的字典键值不能重复";
    
    private final SysDictDataMapper dictDataMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    public Page<SysDictData> selectDictDataPage(PageQuery pageQuery, SysDictDataQuery query) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getTenantId() != null, SysDictData::getTenantId, query.getTenantId())
                .like(StringUtils.isNotBlank(query.getDictLabel()), SysDictData::getDictLabel, query.getDictLabel())
                .eq(StringUtils.isNotBlank(query.getDictType()), SysDictData::getDictType, query.getDictType())
                .eq(query.getDictStatus() != null, SysDictData::getDictStatus, query.getDictStatus())
                .orderByAsc(SysDictData::getDictSort);
        return dictDataMapper.selectPage(pageQuery.toPage(), wrapper);
    }
    
    @Override
    public List<SysDictData> selectDictDataList(SysDictDataQuery query) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        // 添加空值检查,防止NPE
        if (query != null) {
            wrapper.eq(query.getTenantId() != null, SysDictData::getTenantId, query.getTenantId())
                    .like(StringUtils.isNotBlank(query.getDictLabel()), SysDictData::getDictLabel, query.getDictLabel())
                    .eq(StringUtils.isNotBlank(query.getDictType()), SysDictData::getDictType, query.getDictType())
                    .eq(query.getDictStatus() != null, SysDictData::getDictStatus, query.getDictStatus());
        }
        wrapper.orderByAsc(SysDictData::getDictSort);
        return dictDataMapper.selectList(wrapper);
    }
    
    @Override
    @ForgeCacheable(cacheName = "system:dict-data", key = "#dictType")
    public List<SysDictData> selectDictDataByType(String dictType) {
        if (StringUtils.isBlank(dictType)) {
            return List.of();
        }
        return List.copyOf(dictDataMapper.selectEnabledByType(dictType));
    }

    @Override
    @ForgeCacheEvict(cacheName = "system:dict-data", allEntries = true)
    public void clearDictDataCache() {
        log.debug("请求清空全部受管字典缓存");
    }

    @Override
    @ForgeCacheEvict(cacheName = "system:dict-data", key = "#dictType")
    public void clearDictDataCache(String dictType) {
        if (StringUtils.isBlank(dictType)) {
            return;
        }
        log.debug("请求清空受管字典缓存: dictType={}", dictType);
    }
    
    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return dictDataMapper.selectById(dictCode);
    }
    
    @Override
    public boolean insertDictData(SysDictDataDTO dto) {
        validateDictValueUnique(dto);
        SysDictData dictData = new SysDictData();
        BeanUtil.copyProperties(dto, dictData);
        boolean result = dictDataMapper.insert(dictData) > 0;
        if (result && dto.getDictType() != null) {
            eventPublisher.publishEvent(new DictChangeEvent(this, dto.getDictType()));
        }
        return result;
    }
    
    @Override
    public boolean updateDictData(SysDictDataDTO dto) {
        SysDictData existing = dto.getDictCode() == null ? null : dictDataMapper.selectById(dto.getDictCode());
        validateDictValueUnique(dto);
        SysDictData dictData = new SysDictData();
        BeanUtil.copyProperties(dto, dictData);
        boolean result = dictDataMapper.updateById(dictData) > 0;
        if (result) {
            Set<String> dictTypes = new HashSet<>();
            if (existing != null && existing.getDictType() != null) {
                dictTypes.add(existing.getDictType());
            }
            if (dto.getDictType() != null) {
                dictTypes.add(dto.getDictType());
            }
            if (!dictTypes.isEmpty()) {
                eventPublisher.publishEvent(new DictChangeEvent(this, dictTypes));
            }
        }
        return result;
    }

    private void validateDictValueUnique(SysDictDataDTO dto) {
        int duplicateCount = dictDataMapper.countByDictTypeAndValue(
                dto.getDictType(), dto.getDictValue(), dto.getDictCode());
        if (duplicateCount > 0) {
            throw new BusinessException(DICT_VALUE_DUPLICATED_MESSAGE);
        }
    }
    
    @Override
    public boolean deleteDictDataById(Long dictCode) {
        SysDictData existing = dictDataMapper.selectById(dictCode);
        boolean result = dictDataMapper.deleteById(dictCode) > 0;
        if (result && existing != null && existing.getDictType() != null) {
            eventPublisher.publishEvent(new DictChangeEvent(this, existing.getDictType()));
        }
        return result;
    }
    
    @Override
    public boolean deleteDictDataByIds(Long[] dictCodes) {
        Set<String> dictTypes = new HashSet<>();
        for (Long dictCode : dictCodes) {
            SysDictData existing = dictDataMapper.selectById(dictCode);
            if (existing != null && existing.getDictType() != null) {
                dictTypes.add(existing.getDictType());
            }
        }
        boolean result = dictDataMapper.deleteBatchIds(Arrays.asList(dictCodes)) > 0;
        if (result && !dictTypes.isEmpty()) {
            eventPublisher.publishEvent(new DictChangeEvent(this, dictTypes));
        }
        return result;
    }
}
