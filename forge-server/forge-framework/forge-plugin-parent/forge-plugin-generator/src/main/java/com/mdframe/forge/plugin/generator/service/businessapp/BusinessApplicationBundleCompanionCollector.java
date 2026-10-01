package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApp;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessBinding;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtension;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessExtensionVersion;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessProcess;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTrigger;
import com.mdframe.forge.plugin.generator.mapper.BusinessBindingMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessExtensionVersionMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessProcessMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessTriggerMapper;
import com.mdframe.forge.plugin.print.entity.PrintBinding;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.mapper.PrintBindingMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 调试包伴随资产采集：流程、绑定、扩展、入口、触发器、打印（设计态软采集，不强制已发布）。
 */
@Service
@RequiredArgsConstructor
public class BusinessApplicationBundleCompanionCollector {

    private final ObjectMapper objectMapper;
    private final BusinessAppService businessAppService;
    private final BusinessBindingMapper bindingMapper;
    private final BusinessExtensionMapper extensionMapper;
    private final BusinessExtensionVersionMapper extensionVersionMapper;
    private final BusinessProcessMapper processMapper;
    private final BusinessTriggerMapper triggerMapper;
    private final BusinessDocumentConfigService documentConfigService;
    private final PrintBindingMapper printBindingMapper;
    private final PrintTemplateMapper printTemplateMapper;
    private final PrintTemplateVersionMapper printTemplateVersionMapper;

    public Map<String, Object> collect(Long applicationId,
                                       Collection<String> includedObjectCodes,
                                       Map<Long, String> objectIdToCode) {
        Long tenantId = resolveTenantId();
        Set<String> objectCodes = includedObjectCodes == null
                ? Set.of()
                : new LinkedHashSet<>(includedObjectCodes);
        Map<Long, String> idToCode = objectIdToCode == null ? Map.of() : objectIdToCode;

        Map<String, Object> companions = new LinkedHashMap<>();
        companions.put("entries", exportEntries(applicationId, objectCodes));
        companions.put("bindings", exportBindings(tenantId, applicationId));
        companions.put("processes", exportProcesses(tenantId, applicationId, objectCodes));
        companions.put("extensions", exportExtensions(tenantId, applicationId, idToCode));
        companions.put("triggers", exportTriggers(tenantId, objectCodes));
        companions.put("printing", exportPrinting(tenantId, applicationId, objectCodes));
        return companions;
    }

    public Map<String, Object> documentConfigSnapshot(Long objectId) {
        if (objectId == null) {
            return Map.of();
        }
        try {
            return objectMapper.convertValue(documentConfigService.getConfig(objectId),
                    new com.fasterxml.jackson.core.type.TypeReference<LinkedHashMap<String, Object>>() { });
        }
        catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> exportEntries(Long applicationId, Set<String> objectCodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiBusinessApp entry : businessAppService.listByApplicationId(applicationId)) {
            if (entry == null) {
                continue;
            }
            if (StringUtils.isNotBlank(entry.getObjectCode())
                    && !objectCodes.isEmpty()
                    && !objectCodes.contains(entry.getObjectCode())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("appCode", entry.getAppCode());
            item.put("appName", entry.getAppName());
            item.put("appType", entry.getAppType());
            item.put("suiteCode", entry.getSuiteCode());
            item.put("objectCode", entry.getObjectCode());
            item.put("entryMode", entry.getEntryMode());
            item.put("entryUrl", entry.getEntryUrl());
            item.put("configKey", entry.getConfigKey());
            item.put("icon", entry.getIcon());
            item.put("description", entry.getDescription());
            item.put("status", entry.getStatus());
            item.put("sortOrder", entry.getSortOrder());
            item.put("options", parseJson(entry.getOptions()));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> exportBindings(Long tenantId, Long applicationId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiBusinessBinding binding : bindingMapper.selectByApplication(tenantId, applicationId)) {
            if (binding == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bindingType", binding.getBindingType());
            item.put("bindingKey", binding.getBindingKey());
            item.put("bindingName", binding.getBindingName());
            item.put("bindingConfig", parseJson(binding.getBindingConfig()));
            item.put("status", binding.getStatus());
            item.put("sortOrder", binding.getSortOrder());
            item.put("targetType", binding.getTargetType());
            item.put("targetCode", binding.getTargetCode());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> exportProcesses(Long tenantId,
                                                      Long applicationId,
                                                      Set<String> objectCodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<AiBusinessProcess> processes = processMapper.selectByApplicationId(tenantId, applicationId);
        for (AiBusinessProcess process : processes) {
            if (process == null) {
                continue;
            }
            if (StringUtils.isNotBlank(process.getSubjectObjectCode())
                    && !objectCodes.isEmpty()
                    && !objectCodes.contains(process.getSubjectObjectCode())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("processCode", process.getProcessCode());
            item.put("processName", process.getProcessName());
            item.put("processDescription", process.getProcessDescription());
            item.put("subjectObjectCode", process.getSubjectObjectCode());
            item.put("designStatus", process.getDesignStatus());
            item.put("status", process.getStatus());
            item.put("businessProcessJson", parseJson(process.getDraftSchemaJson()));
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> exportExtensions(Long tenantId,
                                                       Long applicationId,
                                                       Map<Long, String> objectIdToCode) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<AiBusinessExtension> extensions = extensionMapper.selectByApplicationId(tenantId, applicationId);
        for (AiBusinessExtension extension : extensions) {
            if (extension == null) {
                continue;
            }
            AiBusinessExtensionVersion version = resolveExtensionContent(tenantId, extension);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("extensionCode", extension.getExtensionCode());
            item.put("extensionName", extension.getExtensionName());
            item.put("extensionType", extension.getExtensionType());
            item.put("hookCode", extension.getHookCode());
            item.put("scopeType", extension.getScopeType());
            item.put("scopeKey", extension.getScopeKey());
            item.put("objectCode", extension.getObjectId() == null
                    ? null
                    : objectIdToCode.get(extension.getObjectId()));
            item.put("sortOrder", extension.getSortOrder());
            item.put("failurePolicy", extension.getFailurePolicy());
            item.put("status", extension.getStatus());
            item.put("content", version == null ? null : version.getContent());
            item.put("processedContent", version == null ? null : version.getProcessedContent());
            item.put("configJson", version == null ? null : version.getConfigJson());
            result.add(item);
        }
        return result;
    }

    private AiBusinessExtensionVersion resolveExtensionContent(Long tenantId, AiBusinessExtension extension) {
        Integer versionNo = extension.getDraftVersion() != null
                ? extension.getDraftVersion()
                : extension.getEnabledVersion();
        if (versionNo == null) {
            return null;
        }
        try {
            return extensionVersionMapper.selectVersion(tenantId, extension.getId(), versionNo);
        }
        catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> exportTriggers(Long tenantId, Set<String> objectCodes) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (objectCodes == null || objectCodes.isEmpty()) {
            return result;
        }
        for (String objectCode : objectCodes) {
            // 用分页接口的大 pageSize 拉全量（无专用 list-all API）
            var page = triggerMapper.selectTriggerPage(
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 500),
                    tenantId, objectCode, null);
            if (page == null || page.getRecords() == null) {
                continue;
            }
            for (AiBusinessTrigger trigger : page.getRecords()) {
                if (trigger == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("objectCode", trigger.getObjectCode());
                item.put("suiteCode", trigger.getSuiteCode());
                item.put("triggerName", trigger.getTriggerName());
                item.put("triggerDesc", trigger.getTriggerDesc());
                item.put("triggerType", trigger.getTriggerType());
                item.put("scenarioType", trigger.getScenarioType());
                item.put("blockingMode", trigger.getBlockingMode());
                item.put("developerMode", trigger.getDeveloperMode());
                item.put("eventType", trigger.getEventType());
                item.put("eventCondition", parseJson(trigger.getEventCondition()));
                item.put("actionType", trigger.getActionType());
                item.put("actionConfig", parseJson(trigger.getActionConfig()));
                item.put("status", trigger.getStatus());
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Object> exportPrinting(Long tenantId,
                                               Long applicationId,
                                               Set<String> objectCodes) {
        Map<String, Object> printing = new LinkedHashMap<>();
        List<Map<String, Object>> templates = new ArrayList<>();
        List<Map<String, Object>> bindings = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<Long> exportedTemplateIds = new LinkedHashSet<>();

        List<PrintBinding> rows;
        try {
            rows = printBindingMapper.selectApplication(tenantId, applicationId);
        }
        catch (Exception e) {
            printing.put("templates", templates);
            printing.put("bindings", bindings);
            printing.put("warnings", List.of("读取打印绑定失败: " + e.getMessage()));
            return printing;
        }
        if (rows == null) {
            rows = List.of();
        }
        for (PrintBinding row : rows) {
            if (row == null) {
                continue;
            }
            if (StringUtils.isNotBlank(row.getObjectCode())
                    && !objectCodes.isEmpty()
                    && !objectCodes.contains(row.getObjectCode())) {
                continue;
            }
            PrintTemplate template = null;
            try {
                template = printTemplateMapper.selectScoped(tenantId, row.getTemplateId());
            }
            catch (Exception e) {
                warnings.add("打印模板读取失败 templateId=" + row.getTemplateId());
            }
            if (template != null && exportedTemplateIds.add(template.getId())) {
                Map<String, Object> templateItem = new LinkedHashMap<>();
                templateItem.put("templateCode", template.getTemplateCode());
                templateItem.put("templateName", template.getTemplateName());
                templateItem.put("sourceType", template.getSourceType());
                templateItem.put("pageId", template.getPageId());
                templateItem.put("formKey", template.getFormKey());
                templateItem.put("objectCode", template.getObjectCode());
                templateItem.put("schemaJson", resolvePrintSchema(tenantId, template));
                templateItem.put("status", template.getStatus());
                templates.add(templateItem);
            }
            Map<String, Object> bindingItem = new LinkedHashMap<>();
            bindingItem.put("templateCode", template == null ? null : template.getTemplateCode());
            bindingItem.put("sourceType", row.getSourceType());
            bindingItem.put("pageId", row.getPageId());
            bindingItem.put("formKey", row.getFormKey());
            bindingItem.put("objectCode", row.getObjectCode());
            bindingItem.put("scene", row.getScene());
            bindingItem.put("isDefault", row.getIsDefault());
            bindingItem.put("sortOrder", row.getSortOrder());
            bindingItem.put("status", row.getStatus());
            bindings.add(bindingItem);
        }
        printing.put("templates", templates);
        printing.put("bindings", bindings);
        printing.put("warnings", warnings);
        return printing;
    }

    private String resolvePrintSchema(Long tenantId, PrintTemplate template) {
        if (template == null) {
            return null;
        }
        if (StringUtils.isNotBlank(template.getDraftSchema())) {
            return template.getDraftSchema();
        }
        if (template.getPublishedVersionId() == null) {
            return null;
        }
        try {
            var version = printTemplateVersionMapper.selectScoped(
                    tenantId, template.getId(), template.getPublishedVersionId());
            return version == null ? null : version.getSchemaJson();
        }
        catch (Exception e) {
            return null;
        }
    }

    private Object parseJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        }
        catch (Exception e) {
            return json;
        }
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? 1L : tenantId;
        }
        catch (Exception e) {
            return 1L;
        }
    }
}
