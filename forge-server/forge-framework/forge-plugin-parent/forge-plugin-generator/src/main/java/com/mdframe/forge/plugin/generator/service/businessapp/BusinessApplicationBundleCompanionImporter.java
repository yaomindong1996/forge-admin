package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessTrigger;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessBindingDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentConfigDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessExtensionDTO;
import com.mdframe.forge.plugin.generator.dto.businessprocess.BusinessProcessDTO;
import com.mdframe.forge.plugin.generator.dto.businessprocess.BusinessProcessSchemaDTO;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessService;
import com.mdframe.forge.plugin.generator.vo.businessprocess.BusinessProcessVO;
import com.mdframe.forge.plugin.print.dto.PrintBindingSaveDTO;
import com.mdframe.forge.plugin.print.dto.PrintTemplateCreateDTO;
import com.mdframe.forge.plugin.print.dto.PrintTemplatePublishDTO;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.service.PrintBindingService;
import com.mdframe.forge.plugin.print.service.PrintTemplateService;
import com.mdframe.forge.plugin.print.service.PrintTemplateVersionService;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.plugin.print.vo.PrintTemplateVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 调试包伴随资产导入：尽量还原完整设计态；单项失败记入 warnings，不阻断主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessApplicationBundleCompanionImporter {

    private final ObjectMapper objectMapper;
    private final BusinessAppService businessAppService;
    private final BusinessBindingService bindingService;
    private final BusinessExtensionService extensionService;
    private final BusinessProcessService processService;
    private final BusinessTriggerService triggerService;
    private final BusinessDocumentConfigService documentConfigService;
    private final PrintTemplateService printTemplateService;
    private final PrintTemplateVersionService printTemplateVersionService;
    private final PrintBindingService printBindingService;

    public void restoreDocumentConfig(Long objectId,
                                      Map<String, Object> objectSnapshot,
                                      List<String> warnings) {
        if (objectId == null || objectSnapshot == null) {
            return;
        }
        Object raw = objectSnapshot.get("documentConfig");
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            return;
        }
        try {
            BusinessDocumentConfigDTO dto = objectMapper.convertValue(raw, BusinessDocumentConfigDTO.class);
            documentConfigService.saveConfig(objectId, dto);
        }
        catch (Exception e) {
            warnings.add("单据配置还原失败 objectId=" + objectId + ": " + e.getMessage());
            log.warn("[调试包导入] documentConfig 失败 objectId={}: {}", objectId, e.getMessage());
        }
    }

    public void restoreCompanions(Long applicationId,
                                  String applicationCode,
                                  Map<String, Object> bundle,
                                  Map<String, String> objectCodeMap,
                                  Map<String, Long> objectIdMap,
                                  List<String> warnings) {
        if (bundle == null) {
            return;
        }
        restoreEntries(applicationId, listOfMap(bundle.get("entries")), objectCodeMap, warnings);
        restoreBindings(applicationId, applicationCode, listOfMap(bundle.get("bindings")),
                objectCodeMap, warnings);
        restoreProcesses(applicationId, listOfMap(bundle.get("processes")),
                objectCodeMap, objectIdMap, warnings);
        restoreExtensions(applicationId, listOfMap(bundle.get("extensions")),
                objectCodeMap, objectIdMap, warnings);
        restoreTriggers(listOfMap(bundle.get("triggers")), objectCodeMap, warnings);
        restorePrinting(applicationId, map(bundle.get("printing")), objectCodeMap, warnings);
    }

    private void restoreEntries(Long applicationId,
                                List<Map<String, Object>> entries,
                                Map<String, String> objectCodeMap,
                                List<String> warnings) {
        for (Map<String, Object> entry : entries) {
            try {
                BusinessAppDTO dto = new BusinessAppDTO();
                String oldObjectCode = text(entry.get("objectCode"));
                String newObjectCode = remapCode(oldObjectCode, objectCodeMap);
                String appCode = StringUtils.defaultIfBlank(text(entry.get("appCode")), "entry")
                        + "_dbg";
                dto.setAppCode(appCode.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_"));
                dto.setAppName(StringUtils.defaultIfBlank(text(entry.get("appName")), "调试入口"));
                dto.setAppType(text(entry.get("appType")));
                dto.setApplicationId(applicationId);
                dto.setSuiteCode(text(entry.get("suiteCode")));
                dto.setObjectCode(newObjectCode);
                dto.setEntryMode(text(entry.get("entryMode")));
                dto.setEntryUrl(text(entry.get("entryUrl")));
                dto.setConfigKey(null);
                dto.setIcon(text(entry.get("icon")));
                dto.setDescription(text(entry.get("description")));
                dto.setStatus(integer(entry.get("status"), EnableStatus.ENABLED.getCode()));
                dto.setSortOrder(integer(entry.get("sortOrder"), 0));
                if (entry.get("options") != null) {
                    dto.setOptions(JSON.toJSONString(entry.get("options")));
                }
                businessAppService.create(dto);
            }
            catch (Exception e) {
                warnings.add("访问入口还原失败: " + e.getMessage());
                log.warn("[调试包导入] entry 失败: {}", e.getMessage());
            }
        }
    }

    private void restoreBindings(Long applicationId,
                                 String applicationCode,
                                 List<Map<String, Object>> bindings,
                                 Map<String, String> objectCodeMap,
                                 List<String> warnings) {
        for (Map<String, Object> binding : bindings) {
            try {
                BusinessBindingDTO dto = new BusinessBindingDTO();
                dto.setTargetType("APPLICATION");
                dto.setTargetId(applicationId);
                dto.setTargetCode(applicationCode);
                dto.setBindingType(text(binding.get("bindingType")));
                dto.setBindingKey(text(binding.get("bindingKey")));
                dto.setBindingName(text(binding.get("bindingName")));
                Object config = remapDeep(binding.get("bindingConfig"), objectCodeMap, Map.of());
                if (config != null) {
                    dto.setBindingConfig(JSON.toJSONString(config));
                }
                dto.setStatus(integer(binding.get("status"), EnableStatus.ENABLED.getCode()));
                dto.setSortOrder(integer(binding.get("sortOrder"), 0));
                bindingService.create(dto);
            }
            catch (Exception e) {
                warnings.add("能力挂接还原失败: " + e.getMessage());
                log.warn("[调试包导入] binding 失败: {}", e.getMessage());
            }
        }
    }

    private void restoreProcesses(Long applicationId,
                                  List<Map<String, Object>> processes,
                                  Map<String, String> objectCodeMap,
                                  Map<String, Long> objectIdMap,
                                  List<String> warnings) {
        for (Map<String, Object> process : processes) {
            try {
                String oldSubjectCode = text(process.get("subjectObjectCode"));
                String newSubjectCode = remapCode(oldSubjectCode, objectCodeMap);
                Long subjectObjectId = objectIdMap.get(oldSubjectCode);
                if (subjectObjectId == null && newSubjectCode != null) {
                    subjectObjectId = objectIdMap.entrySet().stream()
                            .filter(e -> newSubjectCode.equals(objectCodeMap.get(e.getKey())))
                            .map(Map.Entry::getValue)
                            .findFirst()
                            .orElse(null);
                }
                if (subjectObjectId == null) {
                    warnings.add("流程跳过（主对象缺失）: " + text(process.get("processCode")));
                    continue;
                }
                BusinessProcessDTO createDto = new BusinessProcessDTO();
                createDto.setApplicationId(String.valueOf(applicationId));
                createDto.setProcessName(StringUtils.defaultIfBlank(
                        text(process.get("processName")), "调试流程"));
                createDto.setProcessCode(null);
                createDto.setProcessDescription(text(process.get("processDescription")));
                createDto.setSubjectObjectId(String.valueOf(subjectObjectId));
                createDto.setStatus(integer(process.get("status"), EnableStatus.ENABLED.getCode()));
                BusinessProcessVO created = processService.create(createDto);

                Object schema = process.get("businessProcessJson");
                if (schema == null) {
                    continue;
                }
                Object remapped = remapDeep(schema, objectCodeMap, objectIdMap);
                // 强制主体对象指向新 ID/编码
                if (remapped instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> schemaMap = new LinkedHashMap<>((Map<String, Object>) rawMap);
                    Object subject = schemaMap.get("subject");
                    Map<String, Object> subjectMap = subject instanceof Map<?, ?> s
                            ? new LinkedHashMap<>((Map<String, Object>) s)
                            : new LinkedHashMap<>();
                    subjectMap.put("objectId", String.valueOf(subjectObjectId));
                    subjectMap.put("objectCode", newSubjectCode);
                    schemaMap.put("subject", subjectMap);
                    remapped = schemaMap;
                }
                BusinessProcessSchemaDTO schemaDto = new BusinessProcessSchemaDTO();
                schemaDto.setBusinessProcessJson(objectMapper.valueToTree(remapped));
                schemaDto.setExpectedSchemaHash(created.getDraftSchemaHash());
                processService.saveSchema(Long.valueOf(created.getId()), schemaDto);
            }
            catch (Exception e) {
                warnings.add("流程还原失败 " + text(process.get("processCode")) + ": " + e.getMessage());
                log.warn("[调试包导入] process 失败: {}", e.getMessage());
            }
        }
    }

    private void restoreExtensions(Long applicationId,
                                   List<Map<String, Object>> extensions,
                                   Map<String, String> objectCodeMap,
                                   Map<String, Long> objectIdMap,
                                   List<String> warnings) {
        for (Map<String, Object> extension : extensions) {
            try {
                BusinessExtensionDTO dto = new BusinessExtensionDTO();
                dto.setApplicationId(applicationId);
                String oldObjectCode = text(extension.get("objectCode"));
                if (oldObjectCode != null) {
                    dto.setObjectId(objectIdMap.get(oldObjectCode));
                }
                String code = StringUtils.defaultIfBlank(text(extension.get("extensionCode")), "ext")
                        + "_dbg";
                dto.setExtensionCode(code.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_"));
                dto.setExtensionName(StringUtils.defaultIfBlank(
                        text(extension.get("extensionName")), "调试扩展"));
                dto.setExtensionType(text(extension.get("extensionType")));
                dto.setHookCode(text(extension.get("hookCode")));
                dto.setScopeType(text(extension.get("scopeType")));
                dto.setScopeKey(remapCode(text(extension.get("scopeKey")), objectCodeMap));
                dto.setSortOrder(integer(extension.get("sortOrder"), 0));
                dto.setFailurePolicy(text(extension.get("failurePolicy")));
                dto.setContent(text(extension.get("content")));
                dto.setProcessedContent(text(extension.get("processedContent")));
                if (extension.get("configJson") != null) {
                    Object config = remapDeep(extension.get("configJson"), objectCodeMap, objectIdMap);
                    dto.setConfigJson(config instanceof String s ? s : JSON.toJSONString(config));
                }
                extensionService.create(dto);
            }
            catch (Exception e) {
                warnings.add("扩展还原失败: " + e.getMessage());
                log.warn("[调试包导入] extension 失败: {}", e.getMessage());
            }
        }
    }

    private void restoreTriggers(List<Map<String, Object>> triggers,
                                 Map<String, String> objectCodeMap,
                                 List<String> warnings) {
        for (Map<String, Object> trigger : triggers) {
            try {
                AiBusinessTrigger entity = new AiBusinessTrigger();
                String oldCode = text(trigger.get("objectCode"));
                entity.setObjectCode(remapCode(oldCode, objectCodeMap));
                entity.setSuiteCode(text(trigger.get("suiteCode")));
                entity.setTriggerName(StringUtils.defaultIfBlank(
                        text(trigger.get("triggerName")), "调试触发器"));
                entity.setTriggerDesc(text(trigger.get("triggerDesc")));
                entity.setTriggerType(text(trigger.get("triggerType")));
                entity.setScenarioType(text(trigger.get("scenarioType")));
                entity.setBlockingMode(text(trigger.get("blockingMode")));
                entity.setDeveloperMode(integer(trigger.get("developerMode"), 0));
                entity.setEventType(text(trigger.get("eventType")));
                if (trigger.get("eventCondition") != null) {
                    entity.setEventCondition(JSON.toJSONString(
                            remapDeep(trigger.get("eventCondition"), objectCodeMap, Map.of())));
                }
                entity.setActionType(text(trigger.get("actionType")));
                if (trigger.get("actionConfig") != null) {
                    entity.setActionConfig(JSON.toJSONString(
                            remapDeep(trigger.get("actionConfig"), objectCodeMap, Map.of())));
                }
                entity.setStatus(integer(trigger.get("status"), EnableStatus.ENABLED.getCode()));
                triggerService.insert(entity);
            }
            catch (Exception e) {
                warnings.add("触发器还原失败: " + e.getMessage());
                log.warn("[调试包导入] trigger 失败: {}", e.getMessage());
            }
        }
    }

    private void restorePrinting(Long applicationId,
                                 Map<String, Object> printing,
                                 Map<String, String> objectCodeMap,
                                 List<String> warnings) {
        if (printing.isEmpty()) {
            return;
        }
        Map<String, Long> templateCodeToId = new LinkedHashMap<>();
        for (Map<String, Object> template : listOfMap(printing.get("templates"))) {
            try {
                String schemaJson = text(template.get("schemaJson"));
                if (StringUtils.isBlank(schemaJson)) {
                    warnings.add("打印模板缺少 schema，已跳过: " + text(template.get("templateCode")));
                    continue;
                }
                String objectCode = remapCode(text(template.get("objectCode")), objectCodeMap);
                if (StringUtils.isBlank(objectCode)) {
                    warnings.add("打印模板缺少 objectCode，已跳过: " + text(template.get("templateCode")));
                    continue;
                }
                String sourceType = StringUtils.defaultIfBlank(text(template.get("sourceType")), "LOWCODE");
                String templateCode = StringUtils.defaultIfBlank(text(template.get("templateCode")), "tpl")
                        + "_dbg";
                templateCode = templateCode.replaceAll("[^A-Za-z0-9_-]", "_");
                PrintTemplateCreateDTO createDto = new PrintTemplateCreateDTO(
                        applicationId,
                        templateCode,
                        StringUtils.defaultIfBlank(text(template.get("templateName")), "调试打印模板"),
                        PrintSourceType.valueOf(sourceType),
                        text(template.get("pageId")),
                        text(template.get("formKey")),
                        objectCode,
                        schemaJson
                );
                PrintTemplateVO created = printTemplateService.create(createDto);
                printTemplateVersionService.publish(
                        created.id(), new PrintTemplatePublishDTO(created.draftRevision()));
                templateCodeToId.put(text(template.get("templateCode")), created.id());
                templateCodeToId.put(templateCode, created.id());
            }
            catch (Exception e) {
                warnings.add("打印模板还原失败: " + e.getMessage());
                log.warn("[调试包导入] print template 失败: {}", e.getMessage());
            }
        }
        for (Map<String, Object> binding : listOfMap(printing.get("bindings"))) {
            try {
                String templateCode = text(binding.get("templateCode"));
                Long templateId = templateCodeToId.get(templateCode);
                if (templateId == null) {
                    warnings.add("打印绑定跳过（模板未导入）: " + templateCode);
                    continue;
                }
                String objectCode = remapCode(text(binding.get("objectCode")), objectCodeMap);
                PrintSourceType sourceType = PrintSourceType.valueOf(
                        StringUtils.defaultIfBlank(text(binding.get("sourceType")), "LOWCODE"));
                PrintSourceRequest source = new PrintSourceRequest(
                        applicationId,
                        sourceType,
                        text(binding.get("pageId")),
                        text(binding.get("formKey")),
                        objectCode
                );
                PrintScene scene = PrintScene.valueOf(
                        StringUtils.defaultIfBlank(text(binding.get("scene")), "LIST"));
                PrintBindingSaveDTO dto = new PrintBindingSaveDTO(
                        null,
                        null,
                        source,
                        templateId,
                        scene,
                        Boolean.TRUE.equals(binding.get("isDefault")),
                        integer(binding.get("sortOrder"), 0),
                        integer(binding.get("status"), EnableStatus.ENABLED.getCode())
                );
                printBindingService.save(dto);
            }
            catch (Exception e) {
                warnings.add("打印绑定还原失败: " + e.getMessage());
                log.warn("[调试包导入] print binding 失败: {}", e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object remapDeep(Object node,
                             Map<String, String> objectCodeMap,
                             Map<String, Long> objectIdMap) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                String name = String.valueOf(key);
                if ("objectCode".equals(name) || "subjectObjectCode".equals(name)
                        || "referenceObjectCode".equals(name) || "relatedObjectCode".equals(name)
                        || "sourceObjectCode".equals(name) || "targetObjectCode".equals(name)) {
                    result.put(name, remapCode(text(value), objectCodeMap));
                    return;
                }
                if (("objectId".equals(name) || "subjectObjectId".equals(name)) && value != null) {
                    result.put(name, value);
                    return;
                }
                if ("objectRef".equals(name) || "businessObjectRef".equals(name)) {
                    result.put(name, remapDeep(value, objectCodeMap, objectIdMap));
                    return;
                }
                result.put(name, remapDeep(value, objectCodeMap, objectIdMap));
            });
            // 若同时有 objectCode，用新 id 回填 objectId
            String code = text(result.get("objectCode"));
            if (code != null) {
                for (Map.Entry<String, String> entry : objectCodeMap.entrySet()) {
                    if (code.equals(entry.getValue()) || code.equals(entry.getKey())) {
                        Long id = objectIdMap.get(entry.getKey());
                        if (id != null && result.containsKey("objectId")) {
                            result.put("objectId", String.valueOf(id));
                        }
                        break;
                    }
                }
                // 若 objectCode 仍是旧码，换成新码
                String remapped = objectCodeMap.get(code);
                if (remapped != null) {
                    result.put("objectCode", remapped);
                    Long id = objectIdMap.get(code);
                    if (id != null && result.containsKey("objectId")) {
                        result.put("objectId", String.valueOf(id));
                    }
                }
            }
            return result;
        }
        if (node instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(remapDeep(item, objectCodeMap, objectIdMap));
            }
            return result;
        }
        if (node instanceof String text) {
            return remapCode(text, objectCodeMap);
        }
        return node;
    }

    private String remapCode(String value, Map<String, String> objectCodeMap) {
        if (StringUtils.isBlank(value) || objectCodeMap == null) {
            return value;
        }
        return objectCodeMap.getOrDefault(value, value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> raw) {
            return new LinkedHashMap<>((Map<String, Object>) raw);
        }
        return new LinkedHashMap<>();
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?>) {
                result.add(map(item));
            }
        }
        return result;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private Integer integer(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        }
        catch (Exception e) {
            return defaultValue;
        }
    }
}
