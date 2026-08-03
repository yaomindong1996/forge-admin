package com.mdframe.forge.plugin.capability.flowaction.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.capability.flowaction.mapper.FlowActionSourceMapper;
import com.mdframe.forge.plugin.capability.secureaction.exception.SecureActionUnavailableException;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class FlowActionSourceService {

    private static final Set<String> PLATFORM_MANAGED_FIELDS = Set.of(
            "id", "tenantid", "createby", "createtime", "createdept", "updateby", "updatetime",
            "delflag", "businesskey", "processinstanceid");

    private final FlowActionSourceMapper sourceMapper;
    private final ObjectMapper objectMapper;

    public ResolvedFlowActionSource requirePublished(
            Long tenantId,
            String suiteCode,
            String objectCode) {
        FlowActionSourceRow row;
        try {
            row = sourceMapper.selectPublishedFlowSource(
                    requireTenant(tenantId), suiteCode, objectCode);
        }
        catch (RuntimeException exception) {
            throw new SecureActionUnavailableException("FLOW_CATALOG_UNAVAILABLE", exception);
        }
        if (row == null) {
            throw new BusinessException("业务对象未发布或未配置启用的主流程");
        }
        String flowModelKey = resolveFlowModelKey(row);
        if (StringUtils.isBlank(flowModelKey)) {
            throw new BusinessException("业务对象主流程绑定缺少 flowModelKey");
        }
        return new ResolvedFlowActionSource(row, flowModelKey);
    }

    public FlowActionRegistrationSource resolveRegistrationSource(
            Long tenantId,
            String suiteCode,
            String objectCode) {
        ResolvedFlowActionSource source = requirePublished(tenantId, suiteCode, objectCode);
        FlowActionSourceRow row = source.row();
        Map<String, LowcodeFieldSchema> fields = submissionFields(row, false);
        boolean runtimeConfigAvailable = runtimeConfigAvailable(row);
        String submissionUnavailableReason = submissionUnavailableReason(
                row, runtimeConfigAvailable, fields);
        return new FlowActionRegistrationSource(
                row.getObjectId(),
                row.getSuiteCode(),
                row.getObjectCode(),
                row.getObjectName(),
                source.flowModelKey(),
                row.getPublishedObjectVersion(),
                runtimeConfigAvailable,
                submissionUnavailableReason == null,
                submissionUnavailableReason,
                fields.values().stream().map(this::toRegistrationField).toList());
    }

    public Map<String, LowcodeFieldSchema> requireSubmissionFields(
            ResolvedFlowActionSource source) {
        if (source == null || !runtimeConfigAvailable(source.row())) {
            throw new BusinessException("当前对象不是平台托管的低代码运行对象，不能提交业务申请");
        }
        requireMasterRuntimeDatasource(source.row());
        return submissionFields(source.row(), true);
    }

    public ResolvedFlowActionSource requireMatching(
            Long tenantId,
            String suiteCode,
            String objectCode,
            Integer publishedObjectVersion,
            Long bindingId,
            String flowModelKey) {
        ResolvedFlowActionSource source = requirePublished(tenantId, suiteCode, objectCode);
        if (!source.row().getPublishedObjectVersion().equals(publishedObjectVersion)
                || !source.row().getBindingId().equals(bindingId)
                || !source.flowModelKey().equals(flowModelKey)) {
            throw new BusinessException(409, "FLOW_BINDING_MISMATCH");
        }
        return source;
    }

    private String resolveFlowModelKey(FlowActionSourceRow row) {
        if (StringUtils.isNotBlank(row.getBindingConfig())) {
            try {
                JsonNode config = objectMapper.readTree(row.getBindingConfig());
                String configured = StringUtils.trimToNull(config.path("flowModelKey").asText());
                if (configured != null) {
                    return configured;
                }
            }
            catch (Exception exception) {
                throw new SecureActionUnavailableException("FLOW_CATALOG_UNAVAILABLE", exception);
            }
        }
        return StringUtils.trimToNull(row.getBindingKey());
    }

    private Map<String, LowcodeFieldSchema> submissionFields(
            FlowActionSourceRow row,
            boolean required) {
        LowcodeModelSchema model;
        try {
            model = objectMapper.readValue(row.getModelSnapshot(), LowcodeModelSchema.class);
        }
        catch (Exception exception) {
            if (required) {
                throw new BusinessException("业务对象发布版本缺少有效的低代码字段快照");
            }
            return Map.of();
        }
        Map<String, LowcodeFieldSchema> result = new LinkedHashMap<>();
        List<LowcodeFieldSchema> fields = model.getFields() == null ? List.of() : model.getFields();
        for (LowcodeFieldSchema field : fields) {
            if (!isSubmissionField(row, field)) {
                continue;
            }
            result.put(field.getField(), field);
        }
        if (required && result.isEmpty()) {
            throw new BusinessException("当前业务对象没有可开放的申请输入字段");
        }
        return Collections.unmodifiableMap(result);
    }

    private boolean isSubmissionField(FlowActionSourceRow row, LowcodeFieldSchema field) {
        if (field == null || StringUtils.isBlank(field.getField())
                || Boolean.TRUE.equals(field.getSystemField())
                || Boolean.TRUE.equals(field.getReadonly())
                || Boolean.TRUE.equals(field.getPrimaryKey())
                || Boolean.TRUE.equals(field.getAutoIncrement())
                || Boolean.FALSE.equals(field.getFormVisible())
                || field.getFormulaConfig() != null) {
            return false;
        }
        String status = StringUtils.defaultString(field.getFieldStatus());
        if ("DISABLED".equalsIgnoreCase(status) || "HIDDEN".equalsIgnoreCase(status)) {
            return false;
        }
        String name = field.getField();
        String column = field.getColumnName();
        return !isPlatformManaged(row, name) && !isPlatformManaged(row, column);
    }

    private boolean isPlatformManaged(FlowActionSourceRow row, String field) {
        if (StringUtils.isBlank(field)) {
            return false;
        }
        String normalized = normalizeManagedField(field);
        return PLATFORM_MANAGED_FIELDS.contains(normalized)
                || normalized.equals(normalizeManagedField(row.getStatusField()))
                || normalized.equals(normalizeManagedField(row.getStarterField()))
                || normalized.equals(normalizeManagedField(row.getOwnerField()));
    }

    private String normalizeManagedField(String field) {
        return StringUtils.defaultString(field)
                .replace("_", "")
                .toLowerCase(java.util.Locale.ROOT);
    }

    private FlowActionSubmissionField toRegistrationField(LowcodeFieldSchema field) {
        String description = StringUtils.firstNonBlank(
                field.getRemark(), basicText(field, "helpText"), basicText(field, "help"),
                basicText(field, "placeholder"), field.getLabel(), field.getField());
        return new FlowActionSubmissionField(
                field.getField(), StringUtils.defaultIfBlank(field.getLabel(), field.getField()),
                field.getDataType(), field.getLength(), field.getPrecision(),
                Boolean.TRUE.equals(field.getRequired()), field.getDictType(), description,
                field.getDefaultValue());
    }

    private String basicText(LowcodeFieldSchema field, String key) {
        if (field.getBasicProps() == null || field.getBasicProps().get(key) == null) {
            return null;
        }
        return StringUtils.trimToNull(String.valueOf(field.getBasicProps().get(key)));
    }

    private boolean runtimeConfigAvailable(FlowActionSourceRow row) {
        return row != null
                && row.getRuntimeConfigId() != null
                && StringUtils.isNotBlank(row.getConfigKey());
    }

    private String submissionUnavailableReason(
            FlowActionSourceRow row,
            boolean runtimeConfigAvailable,
            Map<String, LowcodeFieldSchema> fields) {
        if (!runtimeConfigAvailable) {
            return "当前对象没有可用的低代码运行配置，不能由平台自动创建申请记录";
        }
        if (hasExternalRuntimeDatasource(row)) {
            return "当前对象使用外部运行数据源，暂不支持保证建单与幂等检查点原子提交；可使用已有记录 START";
        }
        if (fields.isEmpty()) {
            return "当前发布模型没有可开放的申请输入字段";
        }
        return null;
    }

    private void requireMasterRuntimeDatasource(FlowActionSourceRow row) {
        if (hasExternalRuntimeDatasource(row)) {
            throw new BusinessException(409,
                    "当前业务对象使用外部运行数据源，暂不支持原子提交申请；请改用已有记录 START 或配置可靠消息扩展");
        }
    }

    private boolean hasExternalRuntimeDatasource(FlowActionSourceRow row) {
        if (row.getRuntimeDatasourceId() != null
                || StringUtils.isNotBlank(row.getRuntimeDatasourceCode())) {
            return true;
        }
        if (StringUtils.isBlank(row.getRuntimeDatasourceSnapshot())) {
            return false;
        }
        try {
            JsonNode snapshot = objectMapper.readTree(row.getRuntimeDatasourceSnapshot());
            return !snapshot.isNull()
                    && (snapshot.path("datasourceId").canConvertToLong()
                    || StringUtils.isNotBlank(snapshot.path("datasourceCode").asText()));
        }
        catch (Exception exception) {
            throw new SecureActionUnavailableException("FLOW_CATALOG_UNAVAILABLE", exception);
        }
    }

    private Long requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new BusinessException("未获取到有效租户上下文");
        }
        return tenantId;
    }

    public record ResolvedFlowActionSource(
            FlowActionSourceRow row,
            String flowModelKey) {
    }
}
