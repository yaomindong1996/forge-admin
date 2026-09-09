package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.starter.flow.entity.FlowBusiness;
import com.mdframe.forge.starter.flow.entity.FlowRecordParticipant;
import com.mdframe.forge.starter.flow.mapper.FlowRecordParticipantMapper;
import com.mdframe.forge.starter.flow.service.FlowRecordParticipantService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowRecordParticipantServiceImpl implements FlowRecordParticipantService {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final FlowRecordParticipantMapper participantMapper;

    @Override
    public void record(FlowBusiness business, String userId, String relationType) {
        if (business == null) {
            return;
        }
        record(business.getTenantId(), business.getBusinessType(), business.getBusinessKey(),
                business.getProcessInstanceId(), userId, relationType);
    }

    @Override
    public void record(Long tenantId, String businessType, String businessKey, String processInstanceId,
                       String userId, String relationType) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(relationType) || StringUtils.isBlank(businessKey)) {
            return;
        }
        ParsedRef ref = parse(businessType, businessKey);
        if (ref == null) {
            log.debug("跳过经手索引：无法解析业务主键 businessType={}, businessKey={}", businessType, businessKey);
            return;
        }
        FlowRecordParticipant participant = new FlowRecordParticipant();
        participant.setTenantId(tenantId != null ? tenantId : resolveTenantId());
        participant.setBusinessType(ref.businessType());
        participant.setBusinessId(ref.businessId());
        participant.setUserId(userId.trim());
        participant.setRelationType(relationType);
        participant.setProcessInstanceId(processInstanceId);
        participant.setCreateTime(LocalDateTime.now());
        try {
            TenantContextHolder.executeIgnore(() -> participantMapper.insertIgnore(participant));
        } catch (Exception e) {
            log.warn("写入流程经手索引失败: type={}, id={}, userId={}, relation={}",
                    ref.businessType(), ref.businessId(), userId, relationType, e);
        }
    }

    @Override
    public List<String> listBusinessIds(Long tenantId, String userId, String businessType) {
        if (tenantId == null || StringUtils.isBlank(userId) || StringUtils.isBlank(businessType)) {
            return List.of();
        }
        List<String> ids = TenantContextHolder.executeIgnore(() ->
                participantMapper.selectBusinessIds(tenantId, userId, businessType));
        return ids == null ? List.of() : ids;
    }

    @Override
    public Map<String, List<String>> listRelationTypes(Long tenantId, String userId, String businessType,
                                                       Collection<String> businessIds) {
        if (tenantId == null || StringUtils.isBlank(userId) || StringUtils.isBlank(businessType)
                || businessIds == null || businessIds.isEmpty()) {
            return Map.of();
        }
        List<FlowRecordParticipant> rows = TenantContextHolder.executeIgnore(() ->
                participantMapper.selectRelations(tenantId, userId, businessType, businessIds));
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (FlowRecordParticipant row : rows) {
            if (row == null || StringUtils.isBlank(row.getBusinessId()) || StringUtils.isBlank(row.getRelationType())) {
                continue;
            }
            result.computeIfAbsent(row.getBusinessId(), key -> new java.util.ArrayList<>())
                    .add(row.getRelationType());
        }
        return result.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
    }

    static ParsedRef parse(String businessType, String businessKey) {
        String key = StringUtils.trimToNull(businessKey);
        if (key == null) {
            return null;
        }
        int colon = key.indexOf(':');
        if (colon > 0 && colon < key.length() - 1) {
            return new ParsedRef(key.substring(0, colon), key.substring(colon + 1));
        }
        String type = StringUtils.trimToNull(businessType);
        if (type == null) {
            return null;
        }
        return new ParsedRef(type, key);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

    record ParsedRef(String businessType, String businessId) {
    }
}
