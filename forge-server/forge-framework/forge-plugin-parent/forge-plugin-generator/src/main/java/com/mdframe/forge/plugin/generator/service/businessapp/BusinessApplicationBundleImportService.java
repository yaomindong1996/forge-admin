package com.mdframe.forge.plugin.generator.service.businessapp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessApplicationObjectRole;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectDesignerDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessObjectRelationDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.FormDesignerSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.LinkageSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.businessapp.ViewSchemaDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeObjectSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationBundleImportResultVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationBundleImportStepVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationCreateVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 导入应用调试包：新建应用与对象、还原设计态、安全 DDL 建表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessApplicationBundleImportService {

    private static final String MANAGED_BY_DEBUG_BUNDLE = "DEBUG_BUNDLE";

    private final ObjectMapper objectMapper;
    private final BusinessApplicationService applicationService;
    private final BusinessApplicationObjectService applicationObjectService;
    private final BusinessObjectCreateService objectCreateService;
    private final BusinessObjectDesignerService designerService;
    private final BusinessObjectTableMappingService tableMappingService;
    private final BusinessNamingService namingService;
    private final BusinessApplicationBundleCompanionImporter companionImporter;
    private final BusinessApplicationPublishService publishService;

    public BusinessApplicationBundleImportResultVO importBundle(MultipartFile file) {
        return importBundle(file, false);
    }

    public BusinessApplicationBundleImportResultVO importBundle(MultipartFile file, boolean autoPublish) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传应用调试包文件");
        }
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            Map<String, Object> bundle = objectMapper.readValue(text, new TypeReference<>() {
            });
            return importBundle(bundle, autoPublish);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("解析应用调试包失败: " + e.getMessage(), e);
        }
    }

    public BusinessApplicationBundleImportResultVO importBundle(Map<String, Object> bundle) {
        return importBundle(bundle, false);
    }

    public BusinessApplicationBundleImportResultVO importBundle(Map<String, Object> bundle, boolean autoPublish) {
        BusinessApplicationBundleImportResultVO result = new BusinessApplicationBundleImportResultVO();
        List<BusinessApplicationBundleImportStepVO> steps = result.getSteps();

        mark(steps, "parse", "解析配置包", "RUNNING", null);
        validateBundle(bundle);
        mark(steps, "parse", "解析配置包", "SUCCESS", "schemaVersion="
                + bundle.get("schemaVersion"));

        Map<String, Object> applicationSnapshot = map(bundle.get("application"));
        List<Map<String, Object>> objectSnapshots = listOfMap(bundle.get("objects"));
        if (objectSnapshots.isEmpty()) {
            throw new BusinessException("调试包未包含任何业务对象");
        }

        String suffix = debugSuffix();
        Map<String, String> objectCodeMap = new LinkedHashMap<>();
        for (Map<String, Object> objectSnapshot : objectSnapshots) {
            String oldCode = text(objectSnapshot.get("objectCode"));
            if (StringUtils.isBlank(oldCode)) {
                continue;
            }
            objectCodeMap.put(oldCode, allocateObjectCode(oldCode, suffix));
        }

        mark(steps, "application", "创建应用", "RUNNING", null);
        BusinessApplicationCreateVO created = createApplication(applicationSnapshot, suffix);
        result.setApplicationId(created.getId());
        result.setApplicationCode(created.getApplicationCode());
        result.setApplicationName(text(applicationSnapshot.get("applicationName")));
        mark(steps, "application", "创建应用", "SUCCESS",
                "applicationCode=" + created.getApplicationCode());

        Map<String, Long> objectIdMap = new LinkedHashMap<>();
        mark(steps, "objects", "创建业务对象", "RUNNING", null);
        int objectIndex = 0;
        for (Map<String, Object> objectSnapshot : objectSnapshots) {
            objectIndex++;
            String oldCode = text(objectSnapshot.get("objectCode"));
            String newCode = objectCodeMap.getOrDefault(oldCode, allocateObjectCode(oldCode, suffix));
            try {
                Long objectId = createObjectShell(created.getId(), applicationSnapshot, objectSnapshot, newCode);
                objectIdMap.put(oldCode, objectId);
                objectCodeMap.put(oldCode, newCode);
            } catch (RuntimeException e) {
                mark(steps, "objects", "创建业务对象", "FAILED",
                        "第 " + objectIndex + " 个对象失败: " + e.getMessage());
                throw e;
            }
        }
        mark(steps, "objects", "创建业务对象", "SUCCESS",
                "已创建 " + objectIdMap.size() + " 个对象");

        mark(steps, "design", "还原字段与表单设计", "RUNNING", null);
        for (Map<String, Object> objectSnapshot : objectSnapshots) {
            String oldCode = text(objectSnapshot.get("objectCode"));
            Long objectId = objectIdMap.get(oldCode);
            if (objectId == null) {
                continue;
            }
            String newCode = objectCodeMap.get(oldCode);
            try {
                saveObjectDesign(objectId, objectSnapshot, newCode, objectCodeMap);
                companionImporter.restoreDocumentConfig(objectId, objectSnapshot, result.getWarnings());
            } catch (RuntimeException e) {
                mark(steps, "design", "还原字段与表单设计", "FAILED",
                        newCode + ": " + e.getMessage());
                throw e;
            }
        }
        mark(steps, "design", "还原字段与表单设计", "SUCCESS", null);

        mark(steps, "ddl", "同步数据表结构", "RUNNING", null);
        for (Map.Entry<String, Long> entry : objectIdMap.entrySet()) {
            try {
                tableMappingService.syncForDebugBundleImport(entry.getValue());
            } catch (RuntimeException e) {
                String message = entry.getKey() + ": " + e.getMessage();
                result.getWarnings().add("表结构同步警告 — " + message);
                log.warn("[调试包导入] DDL 同步失败 objectCode={}: {}", entry.getKey(), e.getMessage());
            }
        }
        mark(steps, "ddl", "同步数据表结构",
                result.getWarnings().isEmpty() ? "SUCCESS" : "SUCCESS",
                result.getWarnings().isEmpty() ? "全部完成" : "完成（含警告）");

        mark(steps, "builder", "还原页面与导航", "RUNNING", null);
        try {
            restoreApplicationBuilder(created.getId(), applicationSnapshot, objectCodeMap, objectIdMap);
            bindApplicationObjects(created.getId(), objectSnapshots, objectCodeMap, objectIdMap);
            mark(steps, "builder", "还原页面与导航", "SUCCESS", null);
        } catch (RuntimeException e) {
            mark(steps, "builder", "还原页面与导航", "FAILED", e.getMessage());
            throw e;
        }

        mark(steps, "companions", "还原流程/打印/扩展等配置", "RUNNING", null);
        try {
            companionImporter.restoreCompanions(
                    created.getId(),
                    created.getApplicationCode(),
                    bundle,
                    objectCodeMap,
                    objectIdMap,
                    result.getWarnings());
            mark(steps, "companions", "还原流程/打印/扩展等配置", "SUCCESS",
                    result.getWarnings().isEmpty() ? "完成" : "完成（含警告）");
        }
        catch (RuntimeException e) {
            result.getWarnings().add("伴随资产还原异常: " + e.getMessage());
            log.warn("[调试包导入] companions 失败: {}", e.getMessage());
            mark(steps, "companions", "还原流程/打印/扩展等配置", "SUCCESS", "部分失败，已记录警告");
        }

        if (autoPublish) {
            mark(steps, "publish", "导入后发布应用", "RUNNING", null);
            try {
                var publishResult = publishService.publish(
                        created.getId(),
                        new com.mdframe.forge.plugin.generator.dto.businessapp.BusinessApplicationPublishDTO(),
                        "debug-bundle-" + created.getId() + "-" + System.currentTimeMillis());
                result.setPublished(true);
                result.setPublishedVersion(publishResult == null ? null : publishResult.getTargetVersionNo());
                mark(steps, "publish", "导入后发布应用", "SUCCESS",
                        "version=" + result.getPublishedVersion());
            }
            catch (Exception e) {
                result.setPublished(false);
                result.getWarnings().add("自动发布失败: " + e.getMessage());
                mark(steps, "publish", "导入后发布应用", "FAILED", e.getMessage());
                log.warn("[调试包导入] autoPublish 失败: {}", e.getMessage());
            }
        }

        mark(steps, "done", "导入完成", "SUCCESS",
                "可打开应用 " + created.getApplicationCode() + " 进行调试");
        return result;
    }

    private void validateBundle(Map<String, Object> bundle) {
        if (bundle == null || bundle.isEmpty()) {
            throw new BusinessException("调试包内容为空");
        }
        if (!BusinessApplicationBundleExportService.FORMAT.equals(text(bundle.get("format")))) {
            throw new BusinessException("不是有效的 forge-application-bundle 文件");
        }
        Integer version = integerValue(bundle.get("schemaVersion"));
        if (version == null || version < 1) {
            throw new BusinessException("调试包 schemaVersion 无效");
        }
        if (version > BusinessApplicationBundleExportService.SCHEMA_VERSION) {
            throw new BusinessException("调试包版本过高，请升级本地 Forge 后再导入");
        }
    }

    private BusinessApplicationCreateVO createApplication(Map<String, Object> snapshot, String suffix) {
        BusinessApplicationDTO dto = new BusinessApplicationDTO();
        String name = StringUtils.defaultIfBlank(text(snapshot.get("applicationName")), "调试应用");
        String suiteCode = text(snapshot.get("suiteCode"));
        if (StringUtils.isBlank(suiteCode)) {
            throw new BusinessException("调试包缺少 suiteCode，无法创建应用");
        }
        dto.setSuiteCode(suiteCode);
        dto.setApplicationName(name + "（调试导入）");
        dto.setApplicationCode(allocateApplicationCode(suiteCode, name, suffix));
        dto.setPortalSlug(dto.getApplicationCode());
        dto.setIcon(text(snapshot.get("icon")));
        dto.setDescription(StringUtils.defaultIfBlank(text(snapshot.get("description")),
                "由应用调试包导入，便于本地复现问题"));
        dto.setStatus(EnableStatus.ENABLED.getCode());
        dto.setOptions("{}");
        return applicationService.create(dto);
    }

    private Long createObjectShell(Long applicationId,
                                   Map<String, Object> applicationSnapshot,
                                   Map<String, Object> objectSnapshot,
                                   String newObjectCode) {
        BusinessObjectDTO dto = new BusinessObjectDTO();
        dto.setSuiteCode(text(applicationSnapshot.get("suiteCode")));
        dto.setObjectCode(newObjectCode);
        dto.setObjectName(StringUtils.defaultIfBlank(text(objectSnapshot.get("objectName")), newObjectCode));
        dto.setObjectType(StringUtils.defaultIfBlank(text(objectSnapshot.get("objectType")), "MASTER"));
        dto.setCreateMode("BLANK");
        dto.setModelCode(newObjectCode);
        dto.setDisplayField(text(objectSnapshot.get("displayField")));
        dto.setIcon(text(objectSnapshot.get("icon")));
        dto.setDescription(text(objectSnapshot.get("description")));
        dto.setStatus(EnableStatus.ENABLED.getCode());
        JSONObject options = new JSONObject();
        options.put("managedBy", MANAGED_BY_DEBUG_BUNDLE);
        options.put("sourceApplicationId", applicationId);
        options.put("importedFromDebugBundle", true);
        dto.setOptions(options.toJSONString());
        return objectCreateService.create(dto);
    }

    private void saveObjectDesign(Long objectId,
                                  Map<String, Object> objectSnapshot,
                                  String newObjectCode,
                                  Map<String, String> objectCodeMap) {
        BusinessObjectDesignerDTO dto = new BusinessObjectDesignerDTO();
        dto.setObjectId(objectId);
        dto.setObjectName(text(objectSnapshot.get("objectName")));
        dto.setDescription(text(objectSnapshot.get("description")));
        dto.setIcon(text(objectSnapshot.get("icon")));
        dto.setDisplayField(text(objectSnapshot.get("displayField")));
        dto.setStatus(EnableStatus.ENABLED.getCode());

        LowcodeModelSchema modelSchema = convert(objectSnapshot.get("modelSchema"), LowcodeModelSchema.class);
        if (modelSchema == null) {
            modelSchema = new LowcodeModelSchema();
        }
        if (modelSchema.getObject() == null) {
            modelSchema.setObject(new LowcodeObjectSchema());
        }
        modelSchema.getObject().setCode(newObjectCode);
        modelSchema.setTableName(namingService.normalizeObjectCode(newObjectCode, newObjectCode));
        modelSchema.setBusinessName(StringUtils.defaultIfBlank(modelSchema.getBusinessName(),
                text(objectSnapshot.get("objectName"))));
        modelSchema.setRuntimeDatasource(null);
        dto.setModelSchema(modelSchema);

        List<BusinessFieldDTO> fields = convertList(objectSnapshot.get("fields"), BusinessFieldDTO.class);
        if (!fields.isEmpty()) {
            dto.setFields(fields);
        }
        dto.setFormDesignerSchema(convert(remapJsonTree(objectSnapshot.get("formDesignerSchema"), objectCodeMap),
                FormDesignerSchemaDTO.class));
        dto.setPageSchema(convert(remapJsonTree(objectSnapshot.get("pageSchema"), objectCodeMap),
                LowcodePageSchema.class));
        dto.setViewSchema(convert(remapJsonTree(objectSnapshot.get("viewSchema"), objectCodeMap),
                ViewSchemaDTO.class));
        dto.setLinkageSchema(convert(remapJsonTree(objectSnapshot.get("linkageSchema"), objectCodeMap),
                LinkageSchemaDTO.class));
        Map<String, Object> designerOptions = map(remapJsonTree(objectSnapshot.get("designerOptions"), objectCodeMap));
        if (!designerOptions.isEmpty()) {
            dto.setDesignerOptions(designerOptions);
        }
        dto.setRelations(remapRelations(convertList(objectSnapshot.get("relations"), BusinessObjectRelationDTO.class),
                objectCodeMap));
        designerService.saveDesigner(objectId, dto);
    }

    private void restoreApplicationBuilder(Long applicationId,
                                           Map<String, Object> applicationSnapshot,
                                           Map<String, String> objectCodeMap,
                                           Map<String, Long> objectIdMap) {
        Map<String, Object> options = map(remapJsonTree(applicationSnapshot.get("options"), objectCodeMap));
        remapObjectRefsInBuilder(options, objectCodeMap, objectIdMap);
        BusinessApplicationDTO dto = new BusinessApplicationDTO();
        dto.setId(applicationId);
        com.mdframe.forge.plugin.generator.domain.entity.AiBusinessApplication existing =
                applicationService.requireEntity(applicationId);
        dto.setApplicationCode(existing.getApplicationCode());
        dto.setApplicationName(existing.getApplicationName());
        dto.setSuiteCode(existing.getSuiteCode());
        dto.setPortalSlug(existing.getPortalSlug());
        dto.setIcon(existing.getIcon());
        dto.setDescription(existing.getDescription());
        dto.setStatus(existing.getStatus());
        dto.setOptions(JSON.toJSONString(options));
        Object portalConfig = remapJsonTree(applicationSnapshot.get("portalConfig"), objectCodeMap);
        if (portalConfig instanceof Map<?, ?> portalMap && !portalMap.isEmpty()) {
            dto.setPortalConfig(JSON.toJSONString(portalMap));
        }
        Object aiAssistant = applicationSnapshot.get("aiAssistantConfig");
        if (aiAssistant instanceof Map<?, ?> aiMap && !aiMap.isEmpty()) {
            dto.setAiAssistantConfig(JSON.toJSONString(aiMap));
        }
        applicationService.update(dto);
    }

    private void bindApplicationObjects(Long applicationId,
                                        List<Map<String, Object>> objectSnapshots,
                                        Map<String, String> objectCodeMap,
                                        Map<String, Long> objectIdMap) {
        List<BusinessApplicationObjectDTO> associations = new ArrayList<>();
        int sort = 0;
        boolean hasPrimary = false;
        for (Map<String, Object> objectSnapshot : objectSnapshots) {
            String oldCode = text(objectSnapshot.get("objectCode"));
            Long objectId = objectIdMap.get(oldCode);
            if (objectId == null) {
                continue;
            }
            BusinessApplicationObjectDTO item = new BusinessApplicationObjectDTO();
            item.setObjectId(objectId);
            String role = StringUtils.defaultIfBlank(text(objectSnapshot.get("role")),
                    BusinessApplicationObjectRole.SHARED);
            if (!BusinessApplicationObjectRole.supportedRoles().contains(role)) {
                role = BusinessApplicationObjectRole.SHARED;
            }
            if (BusinessApplicationObjectRole.PRIMARY.equals(role)) {
                if (hasPrimary) {
                    role = BusinessApplicationObjectRole.SHARED;
                } else {
                    hasPrimary = true;
                }
            }
            item.setObjectRole(role);
            item.setSortOrder(integerValue(objectSnapshot.get("sortOrder")) == null
                    ? sort : integerValue(objectSnapshot.get("sortOrder")));
            associations.add(item);
            sort++;
        }
        if (!hasPrimary && !associations.isEmpty()) {
            associations.get(0).setObjectRole(BusinessApplicationObjectRole.PRIMARY);
        }
        applicationObjectService.replace(applicationId, associations);
    }

    private List<BusinessObjectRelationDTO> remapRelations(List<BusinessObjectRelationDTO> relations,
                                                           Map<String, String> objectCodeMap) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        List<BusinessObjectRelationDTO> remapped = new ArrayList<>();
        for (BusinessObjectRelationDTO relation : relations) {
            if (relation == null) {
                continue;
            }
            relation.setId(null);
            relation.setSourceObjectCode(remapCode(relation.getSourceObjectCode(), objectCodeMap));
            relation.setTargetObjectCode(remapCode(relation.getTargetObjectCode(), objectCodeMap));
            if (StringUtils.isNotBlank(relation.getRelationConfig())) {
                Object config = remapJsonTree(readJson(relation.getRelationConfig()), objectCodeMap);
                relation.setRelationConfig(JSON.toJSONString(config));
            }
            remapped.add(relation);
        }
        return remapped;
    }

    @SuppressWarnings("unchecked")
    private void remapObjectRefsInBuilder(Map<String, Object> options,
                                          Map<String, String> objectCodeMap,
                                          Map<String, Long> objectIdMap) {
        Object builder = options.get("inAppBuilder");
        if (!(builder instanceof Map<?, ?>)) {
            return;
        }
        Map<String, Object> inAppBuilder = (Map<String, Object>) builder;
        remapObjectRefsDeep(inAppBuilder, objectCodeMap, objectIdMap);
    }

    @SuppressWarnings("unchecked")
    private void remapObjectRefsDeep(Object node,
                                     Map<String, String> objectCodeMap,
                                     Map<String, Long> objectIdMap) {
        if (node instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Object objectRef = map.get("objectRef");
            if (objectRef instanceof Map<?, ?> refMap) {
                Map<String, Object> ref = (Map<String, Object>) refMap;
                String oldCode = text(ref.get("objectCode"));
                if (StringUtils.isNotBlank(oldCode) && objectCodeMap.containsKey(oldCode)) {
                    ref.put("objectCode", objectCodeMap.get(oldCode));
                    Long newId = objectIdMap.get(oldCode);
                    if (newId != null) {
                        ref.put("objectId", String.valueOf(newId));
                        ref.put("id", String.valueOf(newId));
                    }
                }
            }
            String objectCode = text(map.get("objectCode"));
            if (StringUtils.isNotBlank(objectCode) && objectCodeMap.containsKey(objectCode)) {
                map.put("objectCode", objectCodeMap.get(objectCode));
                Long newId = objectIdMap.get(objectCode);
                if (newId != null && map.containsKey("objectId")) {
                    map.put("objectId", String.valueOf(newId));
                }
            }
            for (Object value : map.values()) {
                remapObjectRefsDeep(value, objectCodeMap, objectIdMap);
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                remapObjectRefsDeep(item, objectCodeMap, objectIdMap);
            }
        }
    }

    private Object remapJsonTree(Object node, Map<String, String> objectCodeMap) {
        if (node == null || objectCodeMap.isEmpty()) {
            return node;
        }
        Object converted = objectMapper.convertValue(node, Object.class);
        return remapStringCodes(converted, objectCodeMap);
    }

    @SuppressWarnings("unchecked")
    private Object remapStringCodes(Object node, Map<String, String> objectCodeMap) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), remapStringCodes(value, objectCodeMap)));
            return result;
        }
        if (node instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(remapStringCodes(item, objectCodeMap));
            }
            return result;
        }
        if (node instanceof String text) {
            return remapCode(text, objectCodeMap);
        }
        return node;
    }

    private String remapCode(String value, Map<String, String> objectCodeMap) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String mapped = objectCodeMap.get(value);
        return mapped == null ? value : mapped;
    }

    private String allocateApplicationCode(String suiteCode, String applicationName, String suffix) {
        String base = namingService.buildApplicationCode(suiteCode, applicationName);
        String candidate = StringUtils.left(base, 48) + "_dbg_" + suffix;
        return candidate.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("_+", "_");
    }

    private String allocateObjectCode(String oldCode, String suffix) {
        String base = StringUtils.defaultIfBlank(oldCode, "object");
        String candidate = StringUtils.left(base, 40) + "_dbg_" + suffix;
        return namingService.normalizeObjectCode(candidate, candidate);
    }

    private String debugSuffix() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder suffix = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            suffix.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return suffix.toString();
    }

    private void mark(List<BusinessApplicationBundleImportStepVO> steps,
                      String key, String label, String status, String message) {
        for (BusinessApplicationBundleImportStepVO step : steps) {
            if (key.equals(step.getKey())) {
                step.setStatus(status);
                step.setMessage(message);
                return;
            }
        }
        steps.add(new BusinessApplicationBundleImportStepVO(key, label, status, message));
    }

    private <T> T convert(Object source, Class<T> type) {
        if (source == null) {
            return null;
        }
        return objectMapper.convertValue(source, type);
    }

    private <T> List<T> convertList(Object source, Class<T> type) {
        if (!(source instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>(list.size());
        for (Object item : list) {
            result.add(objectMapper.convertValue(item, type));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        if (value instanceof String json && StringUtils.isNotBlank(json)) {
            return readJson(json);
        }
        return new LinkedHashMap<>();
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = map(item);
            if (!map.isEmpty()) {
                result.add(map);
            }
        }
        return result;
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String text(Object value) {
        return value == null ? null : StringUtils.trimToNull(String.valueOf(value));
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
