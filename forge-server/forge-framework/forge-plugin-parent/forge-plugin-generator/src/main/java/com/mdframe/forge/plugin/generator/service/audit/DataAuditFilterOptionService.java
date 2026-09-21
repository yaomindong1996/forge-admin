package com.mdframe.forge.plugin.generator.service.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.enums.DataAuditErrorCode;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.vo.audit.DataAuditFilterOptionsVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DataAuditFilterOptionService {

    private final DataAuditScopeService scopeService;
    private final BusinessApplicationMapper applicationMapper;
    private final BusinessApplicationObjectMapper applicationObjectMapper;
    private final BusinessObjectMapper businessObjectMapper;
    private final AiCrudConfigMapper crudConfigMapper;
    private final DataAuditValueNormalizer valueNormalizer;
    private final ObjectMapper objectMapper;

    public DataAuditFilterOptionsVO listCurrentUserOptions() {
        Long tenantId = DataAuditTenantSupport.currentTenantId();
        Set<Long> authorizedObjectIds = scopeService.currentUserObjectIds();
        DataAuditFilterOptionsVO result = new DataAuditFilterOptionsVO();
        if (authorizedObjectIds.isEmpty()) {
            return result;
        }
        List<Long> applicationIds = applicationObjectMapper.selectApplicationIdsByObjectIds(
                tenantId, new ArrayList<>(authorizedObjectIds));
        List<DataAuditFilterOptionsVO.ApplicationOption> applications = new ArrayList<>();
        for (Long applicationId : applicationIds) {
            AiBusinessApplication application = applicationMapper.selectEntityById(tenantId, applicationId);
            if (application == null) {
                continue;
            }
            List<BusinessApplicationObjectVO> objects = applicationObjectMapper.selectByApplicationId(
                    tenantId, applicationId).stream()
                    .filter(item -> item.getObjectId() != null && authorizedObjectIds.contains(item.getObjectId()))
                    .toList();
            DataAuditFilterOptionsVO.ApplicationOption option = toApplicationOption(application, objects);
            if (!option.getPages().isEmpty()) {
                applications.add(option);
            }
        }
        applications.sort(Comparator.comparing(
                DataAuditFilterOptionsVO.ApplicationOption::getApplicationName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        result.setApplications(applications);
        return result;
    }

    public List<DataAuditFilterOptionsVO.FieldOption> listObjectFields(Long objectId) {
        Long tenantId = DataAuditTenantSupport.currentTenantId();
        AiBusinessObject object = businessObjectMapper.selectByIdForTenant(tenantId, objectId);
        if (object == null || StringUtils.isBlank(object.getConfigKey())) {
            throw DataAuditErrorCode.AUDIT_FORBIDDEN.exception();
        }
        AiCrudConfig config = crudConfigMapper.selectByConfigKey(tenantId, object.getConfigKey());
        return config == null ? List.of() : parseFields(config.getModelSchema());
    }

    DataAuditFilterOptionsVO.ApplicationOption toApplicationOption(
            AiBusinessApplication application,
            List<BusinessApplicationObjectVO> objects) {
        DataAuditFilterOptionsVO.ApplicationOption option = new DataAuditFilterOptionsVO.ApplicationOption();
        option.setApplicationId(String.valueOf(application.getId()));
        option.setApplicationName(application.getApplicationName());
        option.setApplicationCode(application.getApplicationCode());
        option.setPages(parsePages(application, objects));
        return option;
    }

    private List<DataAuditFilterOptionsVO.PageOption> parsePages(
            AiBusinessApplication application,
            List<BusinessApplicationObjectVO> objects) {
        Map<Long, BusinessApplicationObjectVO> byId = new LinkedHashMap<>();
        Map<String, BusinessApplicationObjectVO> byCode = new LinkedHashMap<>();
        for (BusinessApplicationObjectVO object : objects) {
            if (object.getObjectId() != null) {
                byId.put(object.getObjectId(), object);
            }
            if (StringUtils.isNotBlank(object.getObjectCode())) {
                byCode.put(object.getObjectCode(), object);
            }
        }
        List<DataAuditFilterOptionsVO.PageOption> pages = new ArrayList<>();
        Set<Long> representedObjectIds = new LinkedHashSet<>();
        try {
            JsonNode root = objectMapper.readTree(StringUtils.defaultIfBlank(application.getOptions(), "{}"));
            JsonNode nodes = root.path("inAppBuilder").path("nodes");
            if (nodes.isArray()) {
                for (JsonNode node : nodes) {
                    if (!"page".equals(node.path("type").asText())) {
                        continue;
                    }
                    JsonNode objectRef = node.path("objectRef");
                    BusinessApplicationObjectVO object = resolveObject(objectRef, byId, byCode);
                    if (object == null) {
                        continue;
                    }
                    String pageId = StringUtils.trimToNull(node.path("id").asText(null));
                    if (pageId == null) {
                        continue;
                    }
                    pages.add(toPageOption(
                            pageId,
                            StringUtils.defaultIfBlank(node.path("title").asText(null), object.getObjectName()),
                            object));
                    representedObjectIds.add(object.getObjectId());
                }
            }
        } catch (Exception ignored) {
            // 旧应用或异常草稿继续通过对象关系生成兼容页面选项。
        }
        for (BusinessApplicationObjectVO object : objects) {
            if (object.getObjectId() == null || representedObjectIds.contains(object.getObjectId())) {
                continue;
            }
            pages.add(toPageOption(
                    "object:" + object.getObjectId(),
                    StringUtils.defaultIfBlank(object.getObjectName(), object.getObjectCode()),
                    object));
        }
        return pages;
    }

    private BusinessApplicationObjectVO resolveObject(
            JsonNode objectRef,
            Map<Long, BusinessApplicationObjectVO> byId,
            Map<String, BusinessApplicationObjectVO> byCode) {
        if (objectRef == null || objectRef.isMissingNode() || objectRef.isNull()) {
            return null;
        }
        JsonNode idNode = objectRef.get("objectId");
        if (idNode != null && !idNode.isNull()) {
            try {
                BusinessApplicationObjectVO matched = byId.get(Long.valueOf(idNode.asText()));
                if (matched != null) {
                    return matched;
                }
            } catch (NumberFormatException ignored) {
                // 继续使用对象编码兼容旧页面。
            }
        }
        return byCode.get(objectRef.path("objectCode").asText());
    }

    private DataAuditFilterOptionsVO.PageOption toPageOption(
            String pageId,
            String pageName,
            BusinessApplicationObjectVO object) {
        DataAuditFilterOptionsVO.PageOption option = new DataAuditFilterOptionsVO.PageOption();
        option.setPageId(pageId);
        option.setPageName(pageName);
        option.setObjectId(String.valueOf(object.getObjectId()));
        option.setObjectName(object.getObjectName());
        option.setFields(parseFields(object.getModelSchema()));
        return option;
    }

    private List<DataAuditFilterOptionsVO.FieldOption> parseFields(String modelSchema) {
        if (StringUtils.isBlank(modelSchema)) {
            return List.of();
        }
        try {
            LowcodeModelSchema schema = objectMapper.readValue(modelSchema, LowcodeModelSchema.class);
            List<DataAuditFilterOptionsVO.FieldOption> result = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (LowcodeFieldSchema field : schema.getFields() == null ? List.<LowcodeFieldSchema>of() : schema.getFields()) {
                if (field == null || StringUtils.isBlank(field.getField())
                        || valueNormalizer.isSystemColumn(field.getColumnName(), field.getField())
                        || !seen.add(field.getField())) {
                    continue;
                }
                DataAuditFilterOptionsVO.FieldOption option = new DataAuditFilterOptionsVO.FieldOption();
                option.setFieldCode(field.getField());
                option.setFieldLabel(StringUtils.defaultIfBlank(field.getLabel(), field.getField()));
                option.setColumnName(field.getColumnName());
                result.add(option);
            }
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
