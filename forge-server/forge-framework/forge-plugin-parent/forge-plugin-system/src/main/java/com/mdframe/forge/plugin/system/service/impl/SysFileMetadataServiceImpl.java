package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.plugin.system.mapper.SysFileMetadataMapper;
import com.mdframe.forge.plugin.system.service.ISysFileMetadataService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.file.core.FileManager;
import com.mdframe.forge.starter.file.spi.FileMetadataPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件元数据Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileMetadataServiceImpl extends ServiceImpl<SysFileMetadataMapper, SysFileMetadata>
        implements ISysFileMetadataService {

    private final FileManager fileManager;
    private final FileMetadataPersistence metadataPersistence;

    @Override
    public Page<SysFileMetadata> page(PageQuery query, SysFileMetadata condition) {
        LambdaQueryWrapper<SysFileMetadata> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(condition.getOriginalName())) {
            wrapper.like(SysFileMetadata::getOriginalName, condition.getOriginalName());
        }

        if (StrUtil.isNotBlank(condition.getStorageType())) {
            wrapper.eq(SysFileMetadata::getStorageType, condition.getStorageType());
        }

        if (StrUtil.isNotBlank(condition.getBusinessType())) {
            wrapper.eq(SysFileMetadata::getBusinessType, condition.getBusinessType());
        }

        if (StrUtil.isNotBlank(condition.getBusinessId())) {
            wrapper.eq(SysFileMetadata::getBusinessId, condition.getBusinessId());
        }

        if (condition.getUploaderId() != null) {
            wrapper.eq(SysFileMetadata::getUploaderId, condition.getUploaderId());
        }

        if (condition.getGroupId() != null) {
            wrapper.eq(SysFileMetadata::getGroupId, condition.getGroupId());
        }

        if (StrUtil.isNotBlank(condition.getMimeType())) {
            wrapper.likeRight(SysFileMetadata::getMimeType, condition.getMimeType());
        }

        if (condition.getIsPrivate() != null) {
            wrapper.eq(SysFileMetadata::getIsPrivate, condition.getIsPrivate());
        }

        if (!StpUtil.hasPermission("*:*:*")) {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            wrapper.and(w -> w.eq(SysFileMetadata::getIsPrivate, false)
                    .or()
                    .eq(SysFileMetadata::getUploaderId, currentUserId));
        }

        wrapper.eq(SysFileMetadata::getStatus, "1");
        wrapper.orderByDesc(SysFileMetadata::getUploadTime);

        Page<SysFileMetadata> page = new Page<>(query.getPageNum(), query.getPageSize());
        return this.baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysFileMetadata> listByBusiness(String businessType, String businessId) {
        return findEnabledByBusiness(businessType, businessId).stream()
                .filter(this::canRead)
                .collect(Collectors.toList());
    }

    @Override
    public SysFileMetadata getByFileId(String fileId) {
        SysFileMetadata metadata = findEnabledByFileId(fileId);
        if (metadata == null) {
            return null;
        }
        assertReadable(metadata);
        return metadata;
    }

    @Override
    public SysFileMetadata getReadableById(Long id) {
        if (id == null) {
            return null;
        }
        SysFileMetadata metadata = this.getById(id);
        if (metadata == null || !isEnabledStatus(metadata.getStatus())) {
            return null;
        }
        assertReadable(metadata);
        return metadata;
    }

    private void assertReadable(SysFileMetadata metadata) {
        if (!canRead(metadata)) {
            throw new BusinessException(403, "无权访问该文件");
        }
    }

    private boolean canRead(SysFileMetadata metadata) {
        return metadata != null
                && metadataPersistence.checkPermission(metadata.getFileId(), null);
    }

    private void checkOwnership(SysFileMetadata metadata) {
        if (metadata == null) {
            throw new BusinessException("素材不存在");
        }
        // 与 FileManager.canModify 一致：uploaderId 为空时仅超管可改
        if (!metadataPersistence.canModify(metadata.getFileId(), null)) {
            throw new BusinessException(403, "无权操作他人素材");
        }
    }

    @Override
    public void removeByFileId(String fileId) {
        SysFileMetadata metadata = findEnabledByFileId(fileId);
        checkOwnership(metadata);
        fileManager.delete(metadata.getFileId());
    }

    @Override
    public void removeBatch(String[] fileIds) {
        for (String fileId : fileIds) {
            SysFileMetadata fileMetadata = resolveMetadata(fileId);
            if (fileMetadata == null) {
                continue;
            }
            checkOwnership(fileMetadata);
            try {
                fileManager.delete(fileMetadata.getFileId());
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("删除文件失败: {}", fileId, e);
            }
        }
    }

    @Override
    public boolean updateById(SysFileMetadata metadata) {
        if (metadata != null && metadata.getId() != null) {
            checkOwnership(this.getById(metadata.getId()));
        }
        return super.updateById(metadata);
    }

    @Override
    public void rename(String fileId, String originalName) {
        SysFileMetadata existing = findEnabledByFileId(fileId);
        checkOwnership(existing);
        this.lambdaUpdate()
                .eq(SysFileMetadata::getFileId, fileId)
                .set(SysFileMetadata::getOriginalName, originalName)
                .update();
    }

    /**
     * 包内可覆盖，便于权限单测不依赖 MyBatis 链式查询。
     */
    SysFileMetadata findEnabledByFileId(String fileId) {
        if (StrUtil.isBlank(fileId)) {
            return null;
        }
        return this.lambdaQuery()
                .eq(SysFileMetadata::getFileId, fileId)
                .eq(SysFileMetadata::getStatus, 1)
                .one();
    }

    List<SysFileMetadata> findEnabledByBusiness(String businessType, String businessId) {
        return this.lambdaQuery()
                .eq(SysFileMetadata::getBusinessType, businessType)
                .eq(SysFileMetadata::getBusinessId, businessId)
                .eq(SysFileMetadata::getStatus, 1)
                .orderByDesc(SysFileMetadata::getUploadTime)
                .list();
    }

    /**
     * 批量删除兼容主键 id 与业务 fileId。
     */
    private SysFileMetadata resolveMetadata(String idOrFileId) {
        if (StrUtil.isBlank(idOrFileId)) {
            return null;
        }
        if (idOrFileId.chars().allMatch(Character::isDigit)) {
            SysFileMetadata byPk = this.getById(Long.parseLong(idOrFileId));
            if (byPk != null && isEnabledStatus(byPk.getStatus())) {
                return byPk;
            }
        }
        return findEnabledByFileId(idOrFileId);
    }

    private static boolean isEnabledStatus(Object status) {
        if (status == null) {
            return false;
        }
        if (status instanceof Number number) {
            return number.intValue() == 1;
        }
        return "1".equals(String.valueOf(status));
    }
}
