package com.mdframe.forge.plugin.generator.service.businessapp;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationPublishStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplicationVersion;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVersionVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用不可变版本查询和原子提交服务，不暴露历史 update 能力。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationVersionService
        extends ServiceImpl<BusinessApplicationVersionMapper, AiBusinessApplicationVersion> {

    private final BusinessApplicationService applicationService;
    private final BusinessApplicationMapper applicationMapper;
    private final BusinessApplicationSnapshotService snapshotService;

    public List<BusinessApplicationVersionVO> list(Long applicationId) {
        return baseMapper.selectVersions(resolveTenantId(), applicationId).stream()
                .map(version -> toVO(version, false)).toList();
    }

    public BusinessApplicationVersionVO detail(Long applicationId, Integer versionNo) {
        applicationService.requireEntity(applicationId);
        return toVO(requireVersion(applicationId, versionNo), true);
    }

    public AiBusinessApplicationVersion requireVersion(Long applicationId, Integer versionNo) {
        if (versionNo == null || versionNo < 1) {
            throw new BusinessException("应用版本号不正确");
        }
        AiBusinessApplicationVersion version = baseMapper.selectVersion(resolveTenantId(), applicationId, versionNo);
        if (version == null) {
            throw new BusinessException("应用发布版本不存在: v" + versionNo);
        }
        return version;
    }

    @Transactional(rollbackFor = Exception.class)
    public AiBusinessApplicationVersion commitImmutable(Long applicationId,
                                                        Integer versionNo,
                                                        BusinessApplicationSnapshotService.SnapshotBundle snapshot,
                                                        String publishStatus,
                                                        Integer sourceVersionNo,
                                                        String summary) {
        if (!BusinessApplicationPublishStatus.versionStatuses().contains(publishStatus)) {
            throw new BusinessException("应用发布版本状态不正确");
        }
        AiBusinessApplicationVersion existing = baseMapper.selectVersion(resolveTenantId(), applicationId, versionNo);
        if (existing != null) {
            if (!StringUtils.equals(existing.getSnapshotHash(), snapshot.hash())) {
                throw new BusinessException("目标应用版本已被其他发布占用");
            }
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        AiBusinessApplicationVersion version = new AiBusinessApplicationVersion();
        version.setTenantId(resolveTenantId());
        version.setApplicationId(applicationId);
        version.setVersionNo(versionNo);
        version.setSnapshotJson(snapshot.json());
        version.setSnapshotHash(snapshot.hash());
        version.setPublishStatus(publishStatus);
        version.setPublishSummary(StringUtils.abbreviate(StringUtils.trimToNull(summary), 1000));
        version.setSourceVersionNo(sourceVersionNo);
        version.setPublishedBy(resolveUserId());
        version.setPublishedTime(now);
        save(version);
        if (applicationMapper.markPublished(resolveTenantId(), applicationId, versionNo, now) == 0) {
            throw new BusinessException("应用发布状态提交失败");
        }
        return version;
    }

    private BusinessApplicationVersionVO toVO(AiBusinessApplicationVersion version, boolean includeSnapshot) {
        BusinessApplicationVersionVO vo = new BusinessApplicationVersionVO();
        vo.setId(version.getId());
        vo.setApplicationId(version.getApplicationId());
        vo.setVersionNo(version.getVersionNo());
        vo.setSnapshotHash(version.getSnapshotHash());
        vo.setPublishStatus(version.getPublishStatus());
        vo.setPublishSummary(version.getPublishSummary());
        vo.setSourceVersionNo(version.getSourceVersionNo());
        vo.setPublishedBy(version.getPublishedBy());
        vo.setPublishedTime(version.getPublishedTime());
        vo.setCreateTime(version.getCreateTime());
        if (includeSnapshot) {
            vo.setSnapshot(snapshotService.parse(version.getSnapshotJson()));
        }
        return vo;
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        } catch (Exception e) {
            return 1L;
        }
    }

    private Long resolveUserId() {
        try {
            Long userId = SessionHelper.getUserId();
            return userId == null ? 1L : userId;
        } catch (Exception e) {
            return 1L;
        }
    }
}
