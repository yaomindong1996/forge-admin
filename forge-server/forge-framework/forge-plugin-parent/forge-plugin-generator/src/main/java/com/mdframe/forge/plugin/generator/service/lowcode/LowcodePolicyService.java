package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePolicySchema;
import com.mdframe.forge.plugin.generator.util.DynamicQueryGenerator;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 低代码模型策略默认值和发布校验。
 */
@Service
public class LowcodePolicyService {

    public static final String DATA_SCOPE_TENANT = "TENANT";
    public static final String DATA_SCOPE_FOLLOW_SYSTEM = "FOLLOW_SYSTEM";
    public static final String DATA_SCOPE_SYSTEM_DATA_SCOPE = "SYSTEM_DATA_SCOPE";
    public static final String DATA_SCOPE_REGION = "REGION";

    public LowcodePolicySchema normalizeModelSchema(LowcodeModelSchema modelSchema) {
        if (modelSchema == null) {
            return new LowcodePolicySchema();
        }
        LowcodePolicySchema policies = modelSchema.getPolicies() == null
                ? new LowcodePolicySchema()
                : modelSchema.getPolicies();
        modelSchema.setPolicies(normalizePolicies(modelSchema, policies));
        return modelSchema.getPolicies();
    }

    public LowcodePolicySchema normalizePolicies(LowcodeModelSchema modelSchema, LowcodePolicySchema policies) {
        LowcodePolicySchema result = policies == null ? new LowcodePolicySchema() : policies;
        result.setDataScope(normalizeDataScope(result.getDataScope()));
        result.setAuditEnabled(true);
        result.setPrimaryKeyStrategy("AUTO_INCREMENT");
        result.setPrimaryKeyField("id");

        Map<String, FieldRef> fieldRefs = buildFieldRefs(modelSchema);
        FieldRef userRef = resolveRef(fieldRefs, result.getUserField(), result.getUserColumn(),
                List.of("createBy", "create_by", "userId", "user_id"), "createBy", "create_by");
        FieldRef orgRef = resolveRef(fieldRefs, result.getOrgField(), result.getOrgColumn(),
                List.of("orgId", "org_id", "deptId", "dept_id", "createDept", "create_dept"),
                "createDept", "create_dept");
        FieldRef regionRef = resolveRef(fieldRefs, result.getRegionField(), result.getRegionColumn(),
                List.of("regionCode", "region_code", "areaCode", "area_code"), null, null);
        FieldRef tenantRef = resolveRef(fieldRefs, result.getTenantField(), result.getTenantColumn(),
                List.of("tenantId", "tenant_id"), "tenantId", "tenant_id");
        FieldRef logicDeleteRef = resolveRef(fieldRefs, result.getLogicDeleteField(), result.getLogicDeleteColumn(),
                List.of("delFlag", "del_flag"), "delFlag", "del_flag");

        applyUserRef(result, userRef);
        applyOrgRef(result, orgRef);
        applyRegionRef(result, regionRef);
        applyTenantRef(result, tenantRef);
        applyLogicDeleteRef(result, logicDeleteRef);
        return result;
    }

    public void validatePublishedPolicies(LowcodeModelSchema modelSchema, Set<String> tableColumns) {
        LowcodePolicySchema policies = normalizeModelSchema(modelSchema);
        Set<String> columns = tableColumns == null ? Set.of() : tableColumns.stream()
                .filter(StringUtils::isNotBlank)
                .map(column -> column.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (isFollowSystem(policies)) {
            requireColumn(columns, policies.getTenantColumn(), "租户字段");
            requireColumn(columns, policies.getUserColumn(), "本人数据权限字段");
            requireColumn(columns, policies.getOrgColumn(), "组织数据权限字段");
            if (StringUtils.isNotBlank(policies.getRegionColumn())) {
                requireColumn(columns, policies.getRegionColumn(), "行政区划数据权限字段");
            }
        } else if (DATA_SCOPE_REGION.equals(normalizeDataScope(policies.getDataScope()))) {
            requireColumn(columns, policies.getRegionColumn(), "行政区划数据权限字段");
        }
    }

    public boolean isFollowSystem(LowcodePolicySchema policies) {
        return policies != null && DATA_SCOPE_FOLLOW_SYSTEM.equals(normalizeDataScope(policies.getDataScope()));
    }

    public boolean isFlowRelatedVisible(LowcodePolicySchema policies) {
        return policies != null && !Boolean.FALSE.equals(policies.getFlowRelatedVisible());
    }

    public String normalizeDataScope(String dataScope) {
        String normalized = StringUtils.defaultIfBlank(dataScope, DATA_SCOPE_TENANT)
                .trim()
                .toUpperCase(Locale.ROOT);
        if (DATA_SCOPE_SYSTEM_DATA_SCOPE.equals(normalized)) {
            return DATA_SCOPE_FOLLOW_SYSTEM;
        }
        return normalized;
    }

    private void requireColumn(Set<String> tableColumns, String columnName, String label) {
        if (StringUtils.isBlank(columnName)) {
            throw new BusinessException("FOLLOW_SYSTEM 策略缺少" + label + "映射");
        }
        if (!tableColumns.contains(columnName.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("FOLLOW_SYSTEM 策略" + label + "不存在: " + columnName);
        }
    }

    private Map<String, FieldRef> buildFieldRefs(LowcodeModelSchema modelSchema) {
        Map<String, FieldRef> refs = new LinkedHashMap<>();
        if (modelSchema == null || modelSchema.getFields() == null) {
            return refs;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null) {
                continue;
            }
            String fieldName = StringUtils.trimToNull(field.getField());
            String columnName = StringUtils.defaultIfBlank(
                    StringUtils.trimToNull(field.getColumnName()),
                    fieldName == null ? null : DynamicQueryGenerator.camelToSnake(fieldName));
            if (StringUtils.isBlank(fieldName) && StringUtils.isBlank(columnName)) {
                continue;
            }
            FieldRef ref = new FieldRef(fieldName, columnName);
            putRef(refs, fieldName, ref);
            putRef(refs, columnName, ref);
        }
        return refs;
    }

    private void putRef(Map<String, FieldRef> refs, String key, FieldRef ref) {
        if (StringUtils.isNotBlank(key)) {
            refs.putIfAbsent(key.toLowerCase(Locale.ROOT), ref);
        }
    }

    private FieldRef resolveRef(Map<String, FieldRef> fieldRefs,
                                String configuredField,
                                String configuredColumn,
                                List<String> candidates,
                                String fallbackField,
                                String fallbackColumn) {
        FieldRef configured = findRef(fieldRefs, configuredField);
        if (configured == null) {
            configured = findRef(fieldRefs, configuredColumn);
        }
        if (configured != null) {
            return configured;
        }
        for (String candidate : candidates) {
            FieldRef matched = findRef(fieldRefs, candidate);
            if (matched != null) {
                return matched;
            }
        }
        if (StringUtils.isBlank(fallbackField) && StringUtils.isBlank(fallbackColumn)) {
            return new FieldRef(StringUtils.trimToNull(configuredField), StringUtils.trimToNull(configuredColumn));
        }
        return new FieldRef(
                StringUtils.defaultIfBlank(StringUtils.trimToNull(configuredField), fallbackField),
                StringUtils.defaultIfBlank(StringUtils.trimToNull(configuredColumn), fallbackColumn)
        );
    }

    private FieldRef findRef(Map<String, FieldRef> fieldRefs, String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return fieldRefs.get(key.toLowerCase(Locale.ROOT));
    }

    private void applyUserRef(LowcodePolicySchema policies, FieldRef ref) {
        policies.setUserField(ref.field());
        policies.setUserColumn(ref.column());
    }

    private void applyOrgRef(LowcodePolicySchema policies, FieldRef ref) {
        policies.setOrgField(ref.field());
        policies.setOrgColumn(ref.column());
    }

    private void applyRegionRef(LowcodePolicySchema policies, FieldRef ref) {
        policies.setRegionField(ref.field());
        policies.setRegionColumn(ref.column());
    }

    private void applyTenantRef(LowcodePolicySchema policies, FieldRef ref) {
        policies.setTenantField(StringUtils.defaultIfBlank(ref.field(), "tenantId"));
        policies.setTenantColumn(StringUtils.defaultIfBlank(ref.column(), "tenant_id"));
    }

    private void applyLogicDeleteRef(LowcodePolicySchema policies, FieldRef ref) {
        policies.setLogicDeleteField(StringUtils.defaultIfBlank(ref.field(), "delFlag"));
        policies.setLogicDeleteColumn(StringUtils.defaultIfBlank(ref.column(), "del_flag"));
    }

    private record FieldRef(String field, String column) {
    }
}
