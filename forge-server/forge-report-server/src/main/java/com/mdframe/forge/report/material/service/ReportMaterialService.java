package com.mdframe.forge.report.material.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.report.material.client.ReportMaterialFileClient;
import com.mdframe.forge.report.material.domain.ReportMaterial;
import com.mdframe.forge.report.material.dto.ReportMaterialCreateDTO;
import com.mdframe.forge.report.material.mapper.ReportMaterialMapper;
import com.mdframe.forge.report.material.vo.ReportMaterialVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.file.model.FileMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.mdframe.forge.starter.core.enums.EnableStatus;

/**
 * 报表素材 Service。
 */
@Service
@RequiredArgsConstructor
public class ReportMaterialService extends ServiceImpl<ReportMaterialMapper, ReportMaterial> {

    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String DEFAULT_CATEGORY = "background";
    private static final String REPORT_MATERIAL_BUSINESS_TYPE = "report_material";

    private final ReportMaterialMapper materialMapper;
    private final ReportMaterialFileClient fileClient;

    /**
     * 分页查询素材。
     */
    public Page<ReportMaterialVO> pageMaterials(Integer pageNum, Integer pageSize, String originalName,
                                                String businessId, Boolean isPrivate, String mimeType) {
        Page<ReportMaterialVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return materialMapper.selectMaterialPage(
                page,
                currentTenantId(),
                normalizeCategory(businessId, false),
                normalizeText(originalName),
                normalizeText(mimeType),
                isPrivate,
                SessionHelper.getUserId(),
                SessionHelper.hasPermission("*:*:*")
        );
    }

    /**
     * 素材入库。文件本体仍由通用文件接口上传，这里只维护素材和 fileId 的关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReportMaterialVO createMaterial(ReportMaterialCreateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getFileId())) {
            throw new BusinessException("文件ID不能为空");
        }
        String fileId = dto.getFileId().trim();
        String category = normalizeCategory(StringUtils.hasText(dto.getMaterialCategory())
                ? dto.getMaterialCategory() : dto.getBusinessId(), true);

        FileMetadata metadata = fileClient.getMetadata(fileId);
        if (metadata == null) {
            throw new BusinessException("文件不存在");
        }
        if (!StringUtils.hasText(metadata.getMimeType()) || !metadata.getMimeType().startsWith("image/")) {
            throw new BusinessException("素材库仅支持图片文件");
        }
        if (StringUtils.hasText(metadata.getBusinessType())
                && !REPORT_MATERIAL_BUSINESS_TYPE.equals(metadata.getBusinessType())) {
            throw new BusinessException("文件不是报表素材类型");
        }

        Long tenantId = currentTenantId();
        ReportMaterial existing = materialMapper.selectActiveByFileId(tenantId, fileId);
        if (existing != null) {
            checkOperatePermission(existing);
            existing.setMaterialCategory(category);
            if (dto.getIsPrivate() != null) {
                checkVisibilityPermission(dto.getIsPrivate());
                existing.setIsPrivate(dto.getIsPrivate());
            }
            updateById(existing);
            return getMaterialByFileId(fileId);
        }

        Boolean materialPrivate = dto.getIsPrivate() != null
                ? dto.getIsPrivate() : Boolean.TRUE.equals(metadata.getIsPrivate());
        checkCreatePermission(metadata, materialPrivate);

        ReportMaterial material = new ReportMaterial();
        material.setTenantId(tenantId);
        material.setFileId(fileId);
        material.setMaterialCategory(category);
        material.setIsPrivate(materialPrivate);
        material.setStatus(EnableStatus.ENABLED.getCode());
        Long ownerId = metadata.getUploaderId() != null ? metadata.getUploaderId() : SessionHelper.getUserId();
        material.setCreateBy(ownerId);
        material.setUpdateBy(ownerId);
        save(material);
        return getMaterialByFileId(fileId);
    }

    /**
     * 删除素材及其通用文件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaterial(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            throw new BusinessException("文件ID不能为空");
        }
        ReportMaterial material = requireActiveMaterial(fileId);
        checkOperatePermission(material);
        int deleted = materialMapper.markDeletedByFileId(currentTenantId(), fileId.trim());
        if (deleted <= 0) {
            throw new BusinessException("素材不存在");
        }
        fileClient.delete(fileId.trim());
    }

    /**
     * 重命名素材对应文件。
     */
    @Transactional(rollbackFor = Exception.class)
    public void renameMaterial(String fileId, String originalName) {
        if (!StringUtils.hasText(fileId)) {
            throw new BusinessException("文件ID不能为空");
        }
        if (!StringUtils.hasText(originalName)) {
            throw new BusinessException("素材名称不能为空");
        }
        ReportMaterial material = requireActiveMaterial(fileId);
        checkOperatePermission(material);
        requireFileMetadata(fileId);
        fileClient.rename(fileId.trim(), originalName.trim());
    }

    private ReportMaterialVO getMaterialByFileId(String fileId) {
        return materialMapper.selectMaterialByFileId(currentTenantId(), fileId);
    }

    private FileMetadata requireFileMetadata(String fileId) {
        FileMetadata metadata = fileClient.getMetadata(fileId.trim());
        if (metadata == null) {
            throw new BusinessException("素材文件不存在");
        }
        return metadata;
    }

    private ReportMaterial requireActiveMaterial(String fileId) {
        ReportMaterial material = materialMapper.selectActiveByFileId(currentTenantId(), fileId.trim());
        if (material == null) {
            throw new BusinessException("素材不存在");
        }
        return material;
    }

    private void checkCreatePermission(FileMetadata metadata, Boolean materialPrivate) {
        checkVisibilityPermission(materialPrivate);
        if (SessionHelper.hasPermission("*:*:*")) {
            return;
        }
        Long currentUserId = SessionHelper.getUserId();
        if (currentUserId == null || !currentUserId.equals(metadata.getUploaderId())) {
            throw new BusinessException("无权操作他人素材");
        }
    }

    private void checkVisibilityPermission(Boolean materialPrivate) {
        if (SessionHelper.hasPermission("*:*:*")) {
            return;
        }
        if (!Boolean.TRUE.equals(materialPrivate)) {
            throw new BusinessException("只有管理员才能维护公共素材");
        }
    }

    private void checkOperatePermission(ReportMaterial material) {
        if (SessionHelper.hasPermission("*:*:*")) {
            return;
        }
        Long currentUserId = SessionHelper.getUserId();
        if (!Boolean.TRUE.equals(material.getIsPrivate())) {
            throw new BusinessException("只有管理员才能维护公共素材");
        }
        if (currentUserId == null || !currentUserId.equals(material.getCreateBy())) {
            throw new BusinessException("无权操作他人素材");
        }
    }

    private Long currentTenantId() {
        Long tenantId = SessionHelper.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    private Integer normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 24;
        }
        return Math.min(pageSize, 200);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeCategory(String value, boolean defaultWhenBlank) {
        if (!StringUtils.hasText(value) || "all".equals(value)) {
            return defaultWhenBlank ? DEFAULT_CATEGORY : null;
        }
        return value.trim();
    }
}
