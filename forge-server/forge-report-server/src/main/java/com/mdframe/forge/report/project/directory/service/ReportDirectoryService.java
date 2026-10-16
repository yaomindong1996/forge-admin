package com.mdframe.forge.report.project.directory.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.report.project.directory.domain.ReportDirectory;
import com.mdframe.forge.report.project.directory.dto.ReportDirectoryMoveDTO;
import com.mdframe.forge.report.project.directory.mapper.ReportDirectoryMapper;
import com.mdframe.forge.report.project.mapper.ReportProjectMapper;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报表目录 Service
 */
@Service
@RequiredArgsConstructor
public class ReportDirectoryService extends ServiceImpl<ReportDirectoryMapper, ReportDirectory> {

    private static final Long ROOT_PARENT_ID = 0L;
    private static final String ROOT_ANCESTORS = "0";

    private final ReportProjectMapper projectMapper;

    /**
     * 查询目录树
     */
    public List<ReportDirectory> listTree() {
        return buildTree(baseMapper.selectDirectoryList());
    }

    /**
     * 创建目录
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportDirectory createDirectory(ReportDirectory directory) {
        if (directory == null || !StringUtils.hasText(directory.getDirectoryName())) {
            throw new BusinessException("目录名称不能为空");
        }
        Long parentId = normalizeParentId(directory.getParentId());
        directory.setParentId(parentId);
        directory.setAncestors(buildAncestors(parentId));
        if (directory.getSort() == null) {
            directory.setSort(0);
        }
        save(directory);
        return directory;
    }

    /**
     * 更新目录
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDirectory(ReportDirectory directory) {
        if (directory == null || directory.getId() == null) {
            throw new BusinessException("目录ID不能为空");
        }
        if (!StringUtils.hasText(directory.getDirectoryName())) {
            throw new BusinessException("目录名称不能为空");
        }
        ReportDirectory exists = getById(directory.getId());
        if (exists == null) {
            throw new BusinessException("目录不存在");
        }
        exists.setDirectoryName(directory.getDirectoryName().trim());
        exists.setRemark(directory.getRemark());
        exists.setSort(directory.getSort() == null ? 0 : directory.getSort());
        updateById(exists);
    }

    /**
     * 移动目录
     */
    @Transactional(rollbackFor = Exception.class)
    public void moveDirectory(ReportDirectoryMoveDTO moveDTO) {
        if (moveDTO == null || moveDTO.getId() == null) {
            throw new BusinessException("目录ID不能为空");
        }
        ReportDirectory current = getById(moveDTO.getId());
        if (current == null) {
            throw new BusinessException("目录不存在");
        }

        Long targetParentId = normalizeParentId(moveDTO.getTargetParentId());
        if (Objects.equals(current.getId(), targetParentId)) {
            throw new BusinessException("目录不能移动到自身下");
        }

        if (targetParentId > 0) {
            ReportDirectory targetParent = getById(targetParentId);
            if (targetParent == null) {
                throw new BusinessException("目标父目录不存在");
            }
            List<Long> childIds = selectDirectoryAndChildrenIds(current.getId());
            if (childIds.contains(targetParentId)) {
                throw new BusinessException("目录不能移动到自己的子目录下");
            }
        }

        String oldNodePath = buildNodePath(current.getAncestors(), current.getId());
        String newAncestors = buildAncestors(targetParentId);
        String newNodePath = buildNodePath(newAncestors, current.getId());

        current.setParentId(targetParentId);
        current.setAncestors(newAncestors);
        updateById(current);

        List<ReportDirectory> descendants = baseMapper.selectDescendants(current.getId());
        if (!CollectionUtils.isEmpty(descendants)) {
            descendants.forEach(descendant -> {
                String ancestors = descendant.getAncestors();
                if (StringUtils.hasText(ancestors) && ancestors.startsWith(oldNodePath)) {
                    descendant.setAncestors(newNodePath + ancestors.substring(oldNodePath.length()));
                }
            });
            updateBatchById(descendants);
        }
    }

    /**
     * 删除目录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDirectory(Long id) {
        if (id == null) {
            throw new BusinessException("目录ID不能为空");
        }
        ReportDirectory current = getById(id);
        if (current == null) {
            throw new BusinessException("目录不存在");
        }
        Long childCount = baseMapper.countByParentId(id);
        if (childCount != null && childCount > 0) {
            throw new BusinessException("当前目录存在子目录，请先移动或删除子目录");
        }

        List<Long> directoryIds = selectDirectoryAndChildrenIds(id);
        Long projectCount = projectMapper.countByDirectoryIds(directoryIds);
        if (projectCount != null && projectCount > 0) {
            throw new BusinessException("当前目录下存在大屏项目，请先调整项目归属");
        }
        removeById(id);
    }

    /**
     * 校验目录存在
     */
    public void validateDirectoryExists(Long directoryId) {
        if (directoryId == null || directoryId <= 0) {
            throw new BusinessException("请选择所属目录");
        }
        if (getById(directoryId) == null) {
            throw new BusinessException("所属目录不存在");
        }
    }

    /**
     * 查询目录及子目录 ID
     */
    public List<Long> selectDirectoryAndChildrenIds(Long directoryId) {
        if (directoryId == null || directoryId <= 0) {
            return Collections.emptyList();
        }
        List<Long> ids = baseMapper.selectDirectoryAndChildrenIds(directoryId);
        return ids == null ? Collections.emptyList() : ids;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId <= 0 ? ROOT_PARENT_ID : parentId;
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId <= 0) {
            return ROOT_ANCESTORS;
        }
        ReportDirectory parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException("父目录不存在");
        }
        if (!StringUtils.hasText(parent.getAncestors())) {
            return ROOT_ANCESTORS + "," + parentId;
        }
        return parent.getAncestors() + "," + parentId;
    }

    private String buildNodePath(String ancestors, Long id) {
        if (!StringUtils.hasText(ancestors)) {
            return ROOT_ANCESTORS + "," + id;
        }
        return ancestors + "," + id;
    }

    private List<ReportDirectory> buildTree(List<ReportDirectory> directories) {
        if (CollectionUtils.isEmpty(directories)) {
            return Collections.emptyList();
        }
        Set<Long> allIds = directories.stream()
                .map(ReportDirectory::getId)
                .collect(Collectors.toSet());
        return directories.stream()
                .filter(directory -> directory.getParentId() == null
                        || Objects.equals(directory.getParentId(), ROOT_PARENT_ID)
                        || !allIds.contains(directory.getParentId()))
                .peek(directory -> {
                    List<ReportDirectory> children = buildChildren(directories, directory.getId());
                    directory.setChildren(children.isEmpty() ? null : children);
                })
                .toList();
    }

    private List<ReportDirectory> buildChildren(List<ReportDirectory> directories, Long parentId) {
        List<ReportDirectory> children = new ArrayList<>();
        for (ReportDirectory directory : directories) {
            if (Objects.equals(directory.getParentId(), parentId)) {
                List<ReportDirectory> subChildren = buildChildren(directories, directory.getId());
                directory.setChildren(subChildren.isEmpty() ? null : subChildren);
                children.add(directory);
            }
        }
        return children;
    }
}
