package com.mdframe.forge.plugin.data.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.data.dto.DataDatasetCategorySaveDTO;
import com.mdframe.forge.plugin.data.entity.DataDatasetCategory;
import com.mdframe.forge.plugin.data.mapper.DataDatasetCategoryMapper;
import com.mdframe.forge.plugin.data.mapper.DataDatasetMapper;
import com.mdframe.forge.plugin.data.service.DataDatasetCategoryService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.mdframe.forge.starter.core.enums.EnableStatus;

@Service
@RequiredArgsConstructor
public class DataDatasetCategoryServiceImpl extends ServiceImpl<DataDatasetCategoryMapper, DataDatasetCategory>
    implements DataDatasetCategoryService {

    private static final String ROOT_ANCESTORS = "0/";

    private final DataDatasetCategoryMapper categoryMapper;
    private final DataDatasetMapper datasetMapper;

    @Override
    public List<DataDatasetCategory> listTree() {
        List<DataDatasetCategory> categories = categoryMapper.selectCategoryList(SessionHelper.getTenantId(), null, null);
        return buildTree(categories, null);
    }

    @Override
    public DataDatasetCategory getByCode(String categoryCode) {
        return categoryMapper.selectCategoryByCode(SessionHelper.getTenantId(), categoryCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDatasetCategory saveCategory(DataDatasetCategorySaveDTO dto) {
        validateCategory(dto, false);
        DataDatasetCategory entity = convertToEntity(dto);
        entity.setTenantId(SessionHelper.getTenantId());
        if (entity.getStatus() == null) {
            entity.setStatus(EnableStatus.ENABLED.getCode());
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        fillHierarchy(entity, null);
        save(entity);
        return getById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDatasetCategory updateCategory(DataDatasetCategorySaveDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("分类ID不能为空");
        }
        DataDatasetCategory existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException("分类不存在或已删除");
        }
        validateCategory(dto, true);
        DataDatasetCategory entity = convertToEntity(dto);
        entity.setId(dto.getId());
        entity.setTenantId(existing.getTenantId());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : existing.getStatus());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : existing.getSortOrder());
        fillHierarchy(entity, existing);
        updateById(entity);
        refreshDescendants(entity.getId());
        return getById(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        DataDatasetCategory category = getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在或已删除");
        }
        Long tenantId = SessionHelper.getTenantId();
        if (categoryMapper.selectChildCount(tenantId, id) > 0) {
            throw new BusinessException("当前分类下存在子分类，无法删除");
        }
        if (datasetMapper.selectDatasetCountByCategoryId(id, tenantId) > 0) {
            throw new BusinessException("当前分类下已关联数据集，无法删除");
        }
        removeById(id);
    }

    private void validateCategory(DataDatasetCategorySaveDTO dto, boolean update) {
        if (!StringUtils.hasText(dto.getCategoryCode())) {
            throw new BusinessException("分类编码不能为空");
        }
        if (!StringUtils.hasText(dto.getCategoryName())) {
            throw new BusinessException("分类名称不能为空");
        }

        DataDatasetCategory existingByCode = getByCode(dto.getCategoryCode().trim());
        if (existingByCode != null && !Objects.equals(existingByCode.getId(), dto.getId())) {
            throw new BusinessException("分类编码已存在");
        }

        if (dto.getParentId() == null) {
            return;
        }

        DataDatasetCategory parent = getById(dto.getParentId());
        if (parent == null) {
            throw new BusinessException("父分类不存在或已删除");
        }

        if (!update) {
            return;
        }

        if (Objects.equals(dto.getId(), dto.getParentId())) {
            throw new BusinessException("父分类不能选择自身");
        }

        if (parent.getAncestors() != null && parent.getAncestors().contains("/" + dto.getId() + "/")) {
            throw new BusinessException("父分类不能选择当前分类的下级节点");
        }
    }

    private DataDatasetCategory convertToEntity(DataDatasetCategorySaveDTO dto) {
        DataDatasetCategory entity = new DataDatasetCategory();
        entity.setParentId(dto.getParentId());
        entity.setCategoryCode(dto.getCategoryCode() != null ? dto.getCategoryCode().trim() : null);
        entity.setCategoryName(dto.getCategoryName() != null ? dto.getCategoryName().trim() : null);
        entity.setSortOrder(dto.getSortOrder());
        entity.setStatus(dto.getStatus());
        entity.setDescription(dto.getDescription());
        return entity;
    }

    private void fillHierarchy(DataDatasetCategory entity, DataDatasetCategory existing) {
        if (entity.getParentId() == null) {
            entity.setAncestors(ROOT_ANCESTORS);
            entity.setLevel(1);
            return;
        }

        DataDatasetCategory parent = getById(entity.getParentId());
        if (parent == null) {
            throw new BusinessException("父分类不存在或已删除");
        }

        entity.setAncestors(parent.getAncestors() + parent.getId() + "/");
        entity.setLevel(parent.getLevel() + 1);

        if (existing != null && !Objects.equals(existing.getParentId(), entity.getParentId())
            && existing.getAncestors() != null && existing.getAncestors().contains("/" + entity.getId() + "/")) {
            throw new BusinessException("父分类不能选择当前分类的下级节点");
        }
    }

    private void refreshDescendants(Long categoryId) {
        Long tenantId = SessionHelper.getTenantId();
        List<DataDatasetCategory> categories = categoryMapper.selectCategoryList(tenantId, null, null);
        Map<Long, DataDatasetCategory> categoryMap = categories.stream()
            .collect(Collectors.toMap(DataDatasetCategory::getId, Function.identity(), (left, right) -> left));
        Map<Long, List<DataDatasetCategory>> childrenMap = categories.stream()
            .filter(item -> item.getParentId() != null)
            .collect(Collectors.groupingBy(DataDatasetCategory::getParentId));

        DataDatasetCategory current = categoryMap.get(categoryId);
        if (current == null) {
            return;
        }
        updateChildrenHierarchy(current, childrenMap);
    }

    private void updateChildrenHierarchy(DataDatasetCategory parent, Map<Long, List<DataDatasetCategory>> childrenMap) {
        List<DataDatasetCategory> children = childrenMap.getOrDefault(parent.getId(), new ArrayList<>());
        for (DataDatasetCategory child : children) {
            child.setAncestors(parent.getAncestors() + parent.getId() + "/");
            child.setLevel(parent.getLevel() + 1);
            updateById(child);
            updateChildrenHierarchy(child, childrenMap);
        }
    }

    private List<DataDatasetCategory> buildTree(List<DataDatasetCategory> categories, Long parentId) {
        return categories.stream()
            .filter(item -> Objects.equals(item.getParentId(), parentId))
            .map(item -> {
                item.setChildren(buildTree(categories, item.getId()));
                return item;
            })
            .collect(Collectors.toList());
    }
}
