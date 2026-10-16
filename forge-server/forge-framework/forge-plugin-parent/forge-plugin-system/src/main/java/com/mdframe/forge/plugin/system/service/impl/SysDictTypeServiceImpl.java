package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.SysDictTypeDTO;
import com.mdframe.forge.plugin.system.dto.SysDictTypeQuery;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.plugin.system.listener.DictChangeEvent;
import com.mdframe.forge.plugin.system.mapper.SysDictDataMapper;
import com.mdframe.forge.plugin.system.mapper.SysDictTypeMapper;
import com.mdframe.forge.plugin.system.service.ISysDictTypeService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 字典类型Service实现类
 */
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements ISysDictTypeService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<SysDictType> selectDictTypePage(PageQuery pageQuery, SysDictTypeQuery query) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getTenantId() != null, SysDictType::getTenantId, query.getTenantId())
                .like(StringUtils.isNotBlank(query.getDictName()), SysDictType::getDictName, query.getDictName())
                .eq(StringUtils.isNotBlank(query.getDictType()), SysDictType::getDictType, query.getDictType())
                .eq(query.getDictStatus() != null, SysDictType::getDictStatus, query.getDictStatus())
                .orderByDesc(SysDictType::getCreateTime);
        return dictTypeMapper.selectPage(pageQuery.toPage(), wrapper);
    }

    @Override
    public List<SysDictType> selectDictTypeList(SysDictTypeQuery query) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(query.getTenantId() != null, SysDictType::getTenantId, query.getTenantId())
                    .like(StringUtils.isNotBlank(query.getDictName()), SysDictType::getDictName, query.getDictName())
                    .eq(StringUtils.isNotBlank(query.getDictType()), SysDictType::getDictType, query.getDictType())
                    .eq(query.getDictStatus() != null, SysDictType::getDictStatus, query.getDictStatus());
        }
        wrapper.orderByDesc(SysDictType::getCreateTime);
        return dictTypeMapper.selectList(wrapper);
    }

    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        return dictTypeMapper.selectById(dictId);
    }

    @Override
    public boolean insertDictType(SysDictTypeDTO dto) {
        assertDictTypeUnique(dto == null ? null : dto.getDictType(), null);
        SysDictType dictType = new SysDictType();
        BeanUtil.copyProperties(dto, dictType);
        return dictTypeMapper.insert(dictType) > 0;
    }

    @Override
    public boolean updateDictType(SysDictTypeDTO dto) {
        if (dto == null || dto.getDictId() == null) {
            throw new BusinessException("字典类型ID不能为空");
        }
        SysDictType existing = dictTypeMapper.selectById(dto.getDictId());
        if (existing == null) {
            return false;
        }
        SysDictTypeAccessPolicy.assertDictTypeCodeMutable(existing, dto.getDictType());
        SysDictType dictType = new SysDictType();
        BeanUtil.copyProperties(dto, dictType);
        dictType.setDictType(existing.getDictType());
        return dictTypeMapper.updateById(dictType) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictTypeById(Long dictId) {
        SysDictType existing = dictTypeMapper.selectById(dictId);
        if (existing == null) {
            return false;
        }
        SysDictTypeAccessPolicy.assertDeletable(existing);
        cascadeDeleteDictData(existing.getDictType());
        boolean deleted = dictTypeMapper.deleteById(dictId) > 0;
        if (deleted && StringUtils.isNotBlank(existing.getDictType())) {
            eventPublisher.publishEvent(new DictChangeEvent(this, existing.getDictType()));
        }
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictTypeByIds(Long[] dictIds) {
        if (dictIds == null || dictIds.length == 0) {
            throw new BusinessException("请选择要删除的字典类型");
        }
        Set<String> changedTypes = new HashSet<>();
        for (Long dictId : dictIds) {
            SysDictType existing = dictTypeMapper.selectById(dictId);
            if (existing == null) {
                continue;
            }
            SysDictTypeAccessPolicy.assertDeletable(existing);
            cascadeDeleteDictData(existing.getDictType());
            if (StringUtils.isNotBlank(existing.getDictType())) {
                changedTypes.add(existing.getDictType());
            }
        }
        boolean deleted = dictTypeMapper.deleteBatchIds(Arrays.asList(dictIds)) > 0;
        if (deleted && !changedTypes.isEmpty()) {
            eventPublisher.publishEvent(new DictChangeEvent(this, changedTypes));
        }
        return deleted;
    }

    private void cascadeDeleteDictData(String dictType) {
        if (StringUtils.isBlank(dictType)) {
            return;
        }
        dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType));
    }

    private void assertDictTypeUnique(String dictType, Long excludeId) {
        if (StringUtils.isBlank(dictType)) {
            throw new BusinessException("字典类型不能为空");
        }
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType.trim());
        if (excludeId != null) {
            wrapper.ne(SysDictType::getDictId, excludeId);
        }
        Long count = dictTypeMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("字典类型已存在：" + dictType.trim());
        }
    }
}
