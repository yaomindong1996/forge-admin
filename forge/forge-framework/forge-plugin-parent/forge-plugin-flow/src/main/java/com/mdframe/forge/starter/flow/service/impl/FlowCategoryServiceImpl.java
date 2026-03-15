package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowCategory;
import com.mdframe.forge.starter.flow.mapper.FlowCategoryMapper;
import com.mdframe.forge.starter.flow.service.FlowCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程分类服务实现
 */
@Slf4j
@Service
public class FlowCategoryServiceImpl extends ServiceImpl<FlowCategoryMapper, FlowCategory> implements FlowCategoryService {

    @Override
    public List<FlowCategory> listEnabled() {
        LambdaQueryWrapper<FlowCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowCategory::getStatus, 1)
                .orderByAsc(FlowCategory::getSortOrder);
        return list(wrapper);
    }

    @Override
    public FlowCategory getByCode(String categoryCode) {
        LambdaQueryWrapper<FlowCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowCategory::getCategoryCode, categoryCode);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowCategory createCategory(FlowCategory category) {
        // 检查编码是否已存在
        FlowCategory existing = getByCode(category.getCategoryCode());
        if (existing != null) {
            throw new RuntimeException("分类编码已存在：" + category.getCategoryCode());
        }
        
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        
        save(category);
        log.info("创建流程分类：{}", category.getCategoryCode());
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowCategory updateCategory(FlowCategory category) {
        updateById(category);
        log.info("更新流程分类：{}", category.getCategoryCode());
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        removeById(id);
        log.info("删除流程分类：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableCategory(String id) {
        FlowCategory category = getById(id);
        if (category != null) {
            category.setStatus(1);
            updateById(category);
            log.info("启用流程分类：{}", category.getCategoryCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableCategory(String id) {
        FlowCategory category = getById(id);
        if (category != null) {
            category.setStatus(0);
            updateById(category);
            log.info("禁用流程分类：{}", category.getCategoryCode());
        }
    }
}