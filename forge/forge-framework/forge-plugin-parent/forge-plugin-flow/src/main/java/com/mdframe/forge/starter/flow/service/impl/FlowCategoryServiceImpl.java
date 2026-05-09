package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowCategory;
import com.mdframe.forge.starter.flow.mapper.FlowCategoryMapper;
import com.mdframe.forge.starter.flow.service.FlowCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程分类服务实现（树形结构）
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
    public List<FlowCategory> listTree() {
        // 查询所有启用的分类
        List<FlowCategory> allCategories = listEnabled();
        // 构建树形结构
        return buildTree(allCategories, null);
    }

    @Override
    public List<FlowCategory> listTreeSelect(boolean onlyLeaf) {
        List<FlowCategory> tree = listTree();
        if (onlyLeaf) {
            // 只返回叶子节点（用于分类选择器）
            return flattenLeafNodes(tree);
        }
        return tree;
    }

    /**
     * 递归构建树形结构
     */
    private List<FlowCategory> buildTree(List<FlowCategory> allCategories, String parentId) {
        return allCategories.stream()
                .filter(c -> {
                    if (parentId == null) {
                        return c.getParentId() == null || !StringUtils.hasText(c.getParentId());
                    }
                    return parentId.equals(c.getParentId());
                })
                .peek(c -> {
                    List<FlowCategory> children = buildTree(allCategories, c.getId());
                    c.setChildren(children);
                })
                .collect(Collectors.toList());
    }

    /**
     * 扁平化叶子节点
     */
    private List<FlowCategory> flattenLeafNodes(List<FlowCategory> tree) {
        List<FlowCategory> result = new ArrayList<>();
        for (FlowCategory category : tree) {
            if (category.getChildren() == null || category.getChildren().isEmpty()) {
                result.add(category);
            } else {
                result.addAll(flattenLeafNodes(category.getChildren()));
            }
        }
        return result;
    }

    @Override
    public boolean hasChildren(String parentId) {
        LambdaQueryWrapper<FlowCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowCategory::getParentId, parentId);
        return count(wrapper) > 0;
    }

    /**
     * 更新祖先路径
     */
    private void updateAncestors(FlowCategory category) {
        if (StringUtils.hasText(category.getParentId())) {
            FlowCategory parent = getById(category.getParentId());
            if (parent != null) {
                String parentAncestors = parent.getAncestors();
                if (!StringUtils.hasText(parentAncestors)) {
                    parentAncestors = "0";
                }
                category.setAncestors(parentAncestors + "/" + category.getParentId() + "/");
                category.setLevel(parent.getLevel() + 1);
            } else {
                // 父分类不存在，设为根节点
                category.setAncestors("0/");
                category.setLevel(1);
            }
        } else {
            // 根节点
            category.setAncestors("0/");
            category.setLevel(1);
        }
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

        // 更新祖先路径
        updateAncestors(category);

        save(category);
        log.info("创建流程分类：{}, 层级：{}", category.getCategoryCode(), category.getLevel());
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowCategory updateCategory(FlowCategory category) {
        // 如果修改了父分类，需要更新祖先路径
        updateAncestors(category);

        updateById(category);
        log.info("更新流程分类：{}", category.getCategoryCode());
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        // 检查是否有子分类
        if (hasChildren(id)) {
            throw new RuntimeException("该分类下存在子分类，无法删除");
        }
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