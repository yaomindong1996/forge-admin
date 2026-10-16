package com.mdframe.forge.plugin.ai.dashboard.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.ai.dashboard.domain.AiDashboardComponentLineage;
import com.mdframe.forge.plugin.ai.dashboard.domain.AiDashboardGenerateRecord;
import com.mdframe.forge.plugin.ai.dashboard.dto.AiDashboardComponentLineageDTO;
import com.mdframe.forge.plugin.ai.dashboard.dto.AiDashboardGenerateRecordQuery;
import com.mdframe.forge.plugin.ai.dashboard.dto.AiDashboardGenerateRecordSaveDTO;
import com.mdframe.forge.plugin.ai.dashboard.mapper.AiDashboardComponentLineageMapper;
import com.mdframe.forge.plugin.ai.dashboard.mapper.AiDashboardGenerateRecordMapper;
import com.mdframe.forge.plugin.ai.dashboard.vo.AiDashboardDatasetImpactVO;
import com.mdframe.forge.plugin.ai.dashboard.vo.AiDashboardGenerateRecordAuditVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiDashboardGenerateRecordService
        extends ServiceImpl<AiDashboardGenerateRecordMapper, AiDashboardGenerateRecord> {

    private static final int DEFAULT_RECENT_LIMIT = 20;
    private static final int MAX_RECENT_LIMIT = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_LINEAGE_ITEMS = 200;

    private final AiDashboardGenerateRecordMapper recordMapper;
    private final AiDashboardComponentLineageMapper lineageMapper;

    @Transactional(rollbackFor = Exception.class)
    public AiDashboardGenerateRecord saveGenerateRecord(AiDashboardGenerateRecordSaveDTO dto) {
        if (dto == null) {
            throw new BusinessException("生成记录不能为空");
        }
        AiDashboardGenerateRecord record = new AiDashboardGenerateRecord();
        record.setTenantId(SessionHelper.getTenantId());
        record.setUserId(StpUtil.getLoginIdAsLong());
        record.setSessionId(trim(dto.getSessionId(), 80));
        record.setProjectId(normalizeId(dto.getProjectId()));
        record.setProjectName(trim(dto.getProjectName(), 128));
        record.setBusinessDefinitionId(normalizeId(dto.getBusinessDefinitionId()));
        record.setBusinessName(trim(dto.getBusinessName(), 128));
        record.setProviderId(normalizeId(dto.getProviderId()));
        record.setProviderName(trim(dto.getProviderName(), 128));
        record.setModelName(trim(dto.getModelName(), 128));
        record.setStyle(trim(dto.getStyle(), 20));
        record.setCanvasWidth(dto.getCanvasWidth());
        record.setCanvasHeight(dto.getCanvasHeight());
        record.setPrompt(longText(dto.getPrompt()));
        record.setRequestJson(longText(dto.getRequestJson()));
        record.setGeneratedTitle(trim(dto.getGeneratedTitle(), 200));
        record.setResponseJson(longText(dto.getResponseJson()));
        record.setValidationSummaryJson(longText(dto.getValidationSummaryJson()));
        record.setStatus(StringUtils.hasText(dto.getStatus()) ? trim(dto.getStatus(), 32) : "success");
        record.setComponentCount(nonNegative(dto.getComponentCount()));
        record.setBoundCount(nonNegative(dto.getBoundCount()));
        record.setStaticCount(nonNegative(dto.getStaticCount()));
        record.setStaticFallbackCount(nonNegative(dto.getStaticFallbackCount()));
        record.setRepairedCount(nonNegative(dto.getRepairedCount()));
        record.setElapsedMs(dto.getElapsedMs() != null && dto.getElapsedMs() >= 0 ? dto.getElapsedMs() : null);
        record.setErrorMessage(trim(dto.getErrorMessage(), 1000));
        save(record);
        saveLineageItems(record, dto.getLineageItems());
        return getById(record.getId());
    }

    public List<AiDashboardGenerateRecord> listRecent(Long businessDefinitionId, Long projectId, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        return recordMapper.selectRecent(
                SessionHelper.getTenantId(),
                StpUtil.getLoginIdAsLong(),
                normalizeId(businessDefinitionId),
                normalizeId(projectId),
                safeLimit);
    }

    public Page<AiDashboardGenerateRecordAuditVO> pageForAdmin(Integer pageNum, Integer pageSize,
                                                               AiDashboardGenerateRecordQuery query) {
        Page<AiDashboardGenerateRecordAuditVO> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        return recordMapper.selectAdminPage(page, SessionHelper.getTenantId(), query == null ? new AiDashboardGenerateRecordQuery() : query);
    }

    public AiDashboardGenerateRecordAuditVO getAdminDetail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("生成记录ID不能为空");
        }
        AiDashboardGenerateRecordAuditVO detail = recordMapper.selectAdminDetail(SessionHelper.getTenantId(), id);
        if (detail == null) {
            throw new BusinessException("生成记录不存在");
        }
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOwn(Long id) {
        if (id == null) {
            throw new BusinessException("生成记录ID不能为空");
        }
        Long tenantId = SessionHelper.getTenantId();
        Long userId = StpUtil.getLoginIdAsLong();
        int deleted = recordMapper.deleteOwn(tenantId, userId, id);
        if (deleted <= 0) {
            throw new BusinessException("生成记录不存在或无权删除");
        }
        lineageMapper.deleteByRecordId(tenantId, userId, id);
    }

    public List<AiDashboardDatasetImpactVO> listDatasetImpact(Long datasetId, Integer limit) {
        if (datasetId == null || datasetId <= 0) {
            throw new BusinessException("数据集ID不能为空");
        }
        return lineageMapper.selectDatasetImpact(SessionHelper.getTenantId(), datasetId, normalizeLimit(limit));
    }

    private void saveLineageItems(AiDashboardGenerateRecord record, List<AiDashboardComponentLineageDTO> lineageItems) {
        if (record == null || record.getId() == null || lineageItems == null || lineageItems.isEmpty()) {
            return;
        }
        Long tenantId = record.getTenantId();
        Long userId = record.getUserId();
        List<AiDashboardComponentLineage> lineages = lineageItems.stream()
                .filter(item -> item != null && item.getDatasetId() != null && item.getDatasetId() > 0)
                .limit(MAX_LINEAGE_ITEMS)
                .map(item -> toLineage(record, tenantId, userId, item))
                .collect(Collectors.toList());
        if (lineages.isEmpty()) {
            return;
        }
        lineages.forEach(lineageMapper::insert);
    }

    private AiDashboardComponentLineage toLineage(AiDashboardGenerateRecord record, Long tenantId, Long userId,
                                                  AiDashboardComponentLineageDTO item) {
        AiDashboardComponentLineage lineage = new AiDashboardComponentLineage();
        lineage.setTenantId(tenantId);
        lineage.setRecordId(record.getId());
        lineage.setUserId(userId);
        lineage.setSessionId(record.getSessionId());
        lineage.setProjectId(record.getProjectId());
        lineage.setProjectName(record.getProjectName());
        lineage.setBusinessDefinitionId(record.getBusinessDefinitionId());
        lineage.setBusinessName(record.getBusinessName());
        lineage.setComponentIndex(item.getComponentIndex());
        lineage.setComponentKey(trim(item.getComponentKey(), 100));
        lineage.setComponentTitle(trim(item.getComponentTitle(), 200));
        lineage.setDatasetId(item.getDatasetId());
        lineage.setDatasetName(trim(item.getDatasetName(), 128));
        String fieldNames = item.getFieldNames() != null
                ? item.getFieldNames().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .collect(Collectors.joining(","))
                : "";
        lineage.setFieldNames(trim(fieldNames, 2000));
        lineage.setBindingStatus(trim(item.getBindingStatus(), 32));
        return lineage;
    }

    private Integer normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_RECENT_LIMIT;
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
    }

    private Long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? 1L : pageNum.longValue();
    }

    private Long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return (long) DEFAULT_PAGE_SIZE;
        }
        return (long) Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private Long normalizeId(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private Integer nonNegative(Integer value) {
        return value != null && value >= 0 ? value : 0;
    }

    private String longText(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String trim(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
