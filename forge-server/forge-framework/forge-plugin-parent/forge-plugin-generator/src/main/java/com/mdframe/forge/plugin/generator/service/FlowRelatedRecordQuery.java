package com.mdframe.forge.plugin.generator.service;

import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从平台主库读取流程经手索引，供低代码数据权限附加可见。
 */
@Repository
@RequiredArgsConstructor
public class FlowRelatedRecordQuery {

    static final int MAX_RELATED_IDS = 2000;

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public List<String> listBusinessIds(Long tenantId, Long userId, String objectCode) {
        if (tenantId == null || userId == null || StringUtils.isBlank(objectCode)) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("userId", String.valueOf(userId))
                .addValue("businessType", objectCode);
        List<String> ids = TenantContextHolder.executeIgnore(() -> namedJdbcTemplate.queryForList(
                """
                        SELECT business_id
                        FROM sys_flow_record_participant
                        WHERE tenant_id = :tenantId
                          AND user_id = :userId
                          AND business_type = :businessType
                        LIMIT :limit
                        """,
                params.addValue("limit", MAX_RELATED_IDS),
                String.class));
        return ids == null ? List.of() : ids;
    }

    public Map<String, List<String>> listRelationTypes(Long tenantId, Long userId, String objectCode,
                                                       Collection<String> businessIds) {
        if (tenantId == null || userId == null || StringUtils.isBlank(objectCode)
                || businessIds == null || businessIds.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("userId", String.valueOf(userId))
                .addValue("businessType", objectCode)
                .addValue("businessIds", businessIds);
        List<Map<String, Object>> rows = TenantContextHolder.executeIgnore(() -> namedJdbcTemplate.queryForList(
                """
                        SELECT business_id, relation_type
                        FROM sys_flow_record_participant
                        WHERE tenant_id = :tenantId
                          AND user_id = :userId
                          AND business_type = :businessType
                          AND business_id IN (:businessIds)
                        """,
                params));
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object businessId = row.get("business_id");
            Object relationType = row.get("relation_type");
            if (businessId == null || relationType == null) {
                continue;
            }
            result.computeIfAbsent(String.valueOf(businessId), key -> new java.util.ArrayList<>())
                    .add(String.valueOf(relationType));
        }
        return result;
    }
}
