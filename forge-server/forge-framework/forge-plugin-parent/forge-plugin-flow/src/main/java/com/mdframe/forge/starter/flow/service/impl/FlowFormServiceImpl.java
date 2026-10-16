package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.dto.FormFieldCatalogItemDTO;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.enums.FlowEnableStatus;
import com.mdframe.forge.starter.flow.enums.FlowFormPublishStatus;
import com.mdframe.forge.starter.flow.entity.FlowFormVersion;
import com.mdframe.forge.starter.flow.mapper.FlowFormMapper;
import com.mdframe.forge.starter.flow.mapper.FlowFormVersionMapper;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 流程表单定义服务实现。
 *
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowFormServiceImpl extends ServiceImpl<FlowFormMapper, FlowForm> implements FlowFormService {

    private static final String DATA_MODE_PROCESS_ONLY = "PROCESS_ONLY";
    private static final Long DEFAULT_TENANT_ID = 1L;

    private final FlowFormMapper flowFormMapper;
    private final FlowFormVersionMapper flowFormVersionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Page<FlowForm> getPage(String formName, Integer status, Integer pageNum, Integer pageSize) {
        return flowFormMapper.selectFormPage(new Page<>(pageNum, pageSize), formName, status);
    }

    @Override
    public List<FlowForm> getEnabledForms() {
        return flowFormMapper.selectEnabledForms();
    }

    @Override
    public FlowForm getByFormKey(String formKey) {
        if (!StringUtils.hasText(formKey)) {
            return null;
        }
        return flowFormMapper.selectByFormKey(formKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createForm(FlowForm form) {
        if (checkFormKeyExists(form.getFormKey(), null)) {
            throw new RuntimeException("表单Key已存在: " + form.getFormKey());
        }

        form.setVersion(1);
        form.setStatus(form.getStatus() == null ? FlowEnableStatus.ENABLED.getCode() : form.getStatus());
        form.setPublishStatus(FlowFormPublishStatus.DRAFT.getCode());
        if (form.getTenantId() == null) {
            form.setTenantId(resolveTenantId());
        }
        if (!StringUtils.hasText(form.getDefaultDataMode())) {
            form.setDefaultDataMode(DATA_MODE_PROCESS_ONLY);
        }
        form.setFieldRegistry(buildFieldRegistryJson(form.getFormSchema()));

        return save(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateForm(FlowForm form) {
        FlowForm existing = getById(form.getId());
        if (existing == null) {
            throw new RuntimeException("表单不存在");
        }

        form.setFormKey(existing.getFormKey());
        if (!StringUtils.hasText(form.getDefaultDataMode())) {
            form.setDefaultDataMode(existing.getDefaultDataMode() != null
                    ? existing.getDefaultDataMode() : DATA_MODE_PROCESS_ONLY);
        }

        if (form.getFormSchema() != null && !Objects.equals(form.getFormSchema(), existing.getFormSchema())) {
            form.setVersion(existing.getVersion() == null ? 1 : existing.getVersion() + 1);
            form.setPublishStatus(FlowFormPublishStatus.DRAFT.getCode());
            form.setFieldRegistry(buildFieldRegistryJson(form.getFormSchema()));
        } else {
            form.setVersion(existing.getVersion());
            if (form.getFieldRegistry() == null) {
                form.setFieldRegistry(existing.getFieldRegistry());
            }
            if (form.getPublishStatus() == null) {
                form.setPublishStatus(existing.getPublishStatus());
            }
        }

        return updateById(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteForm(Long id) {
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableForm(Long id) {
        FlowForm form = new FlowForm();
        form.setId(id);
        form.setStatus(FlowEnableStatus.ENABLED.getCode());
        return updateById(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableForm(Long id) {
        FlowForm form = new FlowForm();
        form.setId(id);
        form.setStatus(FlowEnableStatus.DISABLED.getCode());
        return updateById(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyForm(Long id, String newName) {
        FlowForm source = getById(id);
        if (source == null) {
            throw new RuntimeException("源表单不存在");
        }

        String newFormKey = source.getFormKey() + "_copy_" + System.currentTimeMillis();

        FlowForm newForm = new FlowForm();
        newForm.setFormKey(newFormKey);
        newForm.setFormName(newName);
        newForm.setFormCategory(source.getFormCategory());
        newForm.setFormType(source.getFormType());
        newForm.setFormSchema(source.getFormSchema());
        newForm.setFieldRegistry(source.getFieldRegistry());
        newForm.setFormUrl(source.getFormUrl());
        newForm.setComponentPath(source.getComponentPath());
        newForm.setFormConfig(source.getFormConfig());
        newForm.setDefaultDataMode(source.getDefaultDataMode());
        newForm.setTenantId(resolveTenantId());
        newForm.setVersion(1);
        newForm.setStatus(FlowEnableStatus.ENABLED.getCode());
        newForm.setPublishStatus(FlowFormPublishStatus.DRAFT.getCode());
        newForm.setDescription(source.getDescription());

        save(newForm);

        return newForm.getId();
    }

    @Override
    public boolean checkFormKeyExists(String formKey, Long excludeId) {
        if (!StringUtils.hasText(formKey)) {
            return false;
        }
        Long count = flowFormMapper.countByFormKey(formKey, excludeId);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFormSchema(Long id, String formSchema) {
        FlowForm existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("表单不存在");
        }

        FlowForm form = new FlowForm();
        form.setId(id);
        form.setFormSchema(formSchema);
        form.setFieldRegistry(buildFieldRegistryJson(formSchema));
        form.setVersion(existing.getVersion() == null ? 1 : existing.getVersion() + 1);
        form.setPublishStatus(FlowFormPublishStatus.DRAFT.getCode());

        return updateById(form);
    }

    @Override
    public String getFormSchema(String formKey) {
        FlowForm form = getByFormKey(formKey);
        return form != null ? form.getFormSchema() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FlowFormVersion publishVersion(Long formId) {
        FlowForm form = getById(formId);
        if (form == null) {
            throw new RuntimeException("表单不存在");
        }
        if (!StringUtils.hasText(form.getFormSchema())) {
            throw new RuntimeException("表单Schema不能为空，不能发布");
        }

        FlowFormVersion latest = flowFormVersionMapper.selectLatestByFormId(formId);
        int nextVersion = latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
        String fieldRegistry = StringUtils.hasText(form.getFieldRegistry())
                ? form.getFieldRegistry() : buildFieldRegistryJson(form.getFormSchema());

        FlowFormVersion version = new FlowFormVersion();
        version.setTenantId(form.getTenantId() == null ? resolveTenantId() : form.getTenantId());
        version.setFormId(form.getId());
        version.setFormKey(form.getFormKey());
        version.setFormName(form.getFormName());
        version.setFormCategory(form.getFormCategory());
        version.setFormType(form.getFormType());
        version.setVersion(nextVersion);
        version.setFormSchema(form.getFormSchema());
        version.setFieldRegistry(fieldRegistry);
        version.setFormConfig(form.getFormConfig());
        version.setDefaultDataMode(StringUtils.hasText(form.getDefaultDataMode())
                ? form.getDefaultDataMode() : DATA_MODE_PROCESS_ONLY);
        version.setPublishTime(LocalDateTime.now());
        version.setPublishBy(resolveUserId());
        flowFormVersionMapper.insert(version);

        FlowForm update = new FlowForm();
        update.setId(form.getId());
        update.setVersion(nextVersion);
        update.setFieldRegistry(fieldRegistry);
        update.setPublishStatus(FlowFormPublishStatus.PUBLISHED.getCode());
        update.setCurrentVersionId(version.getId());
        updateById(update);

        return version;
    }

    @Override
    public List<FlowFormVersion> listVersions(Long formId) {
        return flowFormVersionMapper.selectVersionsByFormId(formId);
    }

    @Override
    public List<FormFieldCatalogItemDTO> resolveFieldCatalog(String formKey, Long versionId) {
        String fieldRegistry = null;
        String formSchema = null;
        if (versionId != null) {
            FlowFormVersion version = flowFormVersionMapper.selectByIdForRuntime(versionId);
            if (version != null) {
                fieldRegistry = version.getFieldRegistry();
                formSchema = version.getFormSchema();
            }
        }
        if (!StringUtils.hasText(fieldRegistry) && StringUtils.hasText(formKey)) {
            FlowForm form = getByFormKey(formKey);
            if (form != null) {
                fieldRegistry = form.getFieldRegistry();
                formSchema = form.getFormSchema();
            }
        }
        if (StringUtils.hasText(fieldRegistry)) {
            try {
                return objectMapper.readValue(fieldRegistry, new TypeReference<List<FormFieldCatalogItemDTO>>() {});
            } catch (Exception e) {
                log.warn("字段目录JSON解析失败，尝试从Schema重新解析: formKey={}, versionId={}", formKey, versionId, e);
            }
        }
        return parseFieldCatalog(formSchema);
    }

    @Override
    public String buildFieldRegistryJson(String formSchema) {
        List<FormFieldCatalogItemDTO> catalog = parseFieldCatalog(formSchema);
        try {
            return objectMapper.writeValueAsString(catalog);
        } catch (Exception e) {
            log.warn("字段目录序列化失败", e);
            return "[]";
        }
    }

    private List<FormFieldCatalogItemDTO> parseFieldCatalog(String formSchema) {
        if (!StringUtils.hasText(formSchema)) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(formSchema);
            List<FormFieldCatalogItemDTO> fields = new ArrayList<>();
            collectFields(root, fields);
            return mergeByField(fields);
        } catch (Exception e) {
            log.warn("解析表单字段目录失败", e);
            return Collections.emptyList();
        }
    }

    private void collectFields(JsonNode node, List<FormFieldCatalogItemDTO> fields) {
        collectFields(node, fields, null, null);
    }

    private void collectFields(JsonNode node, List<FormFieldCatalogItemDTO> fields,
                               String arrayKey, String arrayLabel) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectFields(child, fields, arrayKey, arrayLabel));
            return;
        }
        if (!node.isObject()) {
            return;
        }

        String field = textValue(node, "field");
        if (!StringUtils.hasText(field)) {
            field = textValue(node, "fieldCode");
        }
        if (!StringUtils.hasText(field) && node.has("props")) {
            JsonNode props = node.get("props");
            field = firstText(props, "field", "fieldCode", "prop");
        }
        if (!StringUtils.hasText(field) && node.has("fieldBinding")) {
            field = textValue(node.get("fieldBinding"), "fieldCode");
        }
        if (!StringUtils.hasText(field) && node.has("_forge")) {
            JsonNode binding = node.path("_forge").path("fieldBinding");
            field = textValue(binding, "fieldCode");
        }

        if (StringUtils.hasText(field) && !field.startsWith("ref_")) {
            boolean arrayField = arrayKey == null && isArrayFormRule(node);
            FormFieldCatalogItemDTO item = toCatalogItem(node, field);
            if (arrayField) {
                item.setDataType("array");
            } else if (arrayKey != null) {
                item.setScope("array");
                item.setArrayKey(arrayKey);
                item.setArrayLabel(arrayLabel);
                item.setItemField(field);
            }
            fields.add(item);
            if (arrayField) {
                collectArrayItemFields(node, fields, field,
                        StringUtils.hasText(item.getLabel()) ? item.getLabel() : field);
                return;
            }
        }

        node.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            if ("props".equals(name) || "_fc_drag_tag".equals(name)) {
                return;
            }
            collectFields(entry.getValue(), fields, arrayKey, arrayLabel);
        });
    }

    private void collectArrayItemFields(JsonNode node, List<FormFieldCatalogItemDTO> fields,
                                        String arrayKey, String arrayLabel) {
        String type = normalizeComponentType(firstText(node, "type", "component", "componentKey"));
        JsonNode props = node.path("props");
        if ("tableform".equals(type)) {
            JsonNode columns = props.path("columns");
            if (!columns.isArray()) {
                columns = node.path("columns");
            }
            if (columns.isArray()) {
                for (JsonNode column : columns) {
                    collectFields(column.path("rule"), fields, arrayKey, arrayLabel);
                }
            }
            return;
        }
        JsonNode rules = props.path("rule");
        if (!rules.isArray()) {
            rules = node.path("rule");
        }
        if (!rules.isArray()) {
            rules = node.path("children");
        }
        collectFields(rules, fields, arrayKey, arrayLabel);
    }

    private boolean isArrayFormRule(JsonNode node) {
        String type = normalizeComponentType(firstText(node, "type", "component", "componentKey"));
        return Set.of("group", "tableform", "subform", "array").contains(type);
    }

    private String normalizeComponentType(String value) {
        return value == null ? "" : value.replaceAll("[-_\\s]", "").toLowerCase(Locale.ROOT);
    }

    private FormFieldCatalogItemDTO toCatalogItem(JsonNode node, String field) {
        FormFieldCatalogItemDTO item = new FormFieldCatalogItemDTO();
        item.setField(field);
        item.setLabel(firstText(node, "label", "title", "name"));
        if (!StringUtils.hasText(item.getLabel()) && node.has("props")) {
            item.setLabel(firstText(node.get("props"), "label", "title", "fieldName"));
        }
        item.setComponentType(firstText(node, "type", "component", "componentKey"));
        item.setDataType(inferDataType(item.getComponentType()));
        item.setRequired(resolveRequired(node));
        item.setOptionSource(resolveOptionSource(node));
        item.setSource("FORM");
        return item;
    }

    private List<FormFieldCatalogItemDTO> mergeByField(List<FormFieldCatalogItemDTO> fields) {
        Map<String, FormFieldCatalogItemDTO> merged = new LinkedHashMap<>();
        for (FormFieldCatalogItemDTO field : fields) {
            if (!StringUtils.hasText(field.getField())) {
                continue;
            }
            String key = "array".equals(field.getScope())
                    ? "array:" + field.getArrayKey() + ":" + field.getItemField()
                    : "main:" + field.getField();
            merged.putIfAbsent(key, field);
        }
        return new ArrayList<>(merged.values());
    }

    private Boolean resolveRequired(JsonNode node) {
        if (node.has("required")) {
            return node.get("required").asBoolean(false);
        }
        JsonNode validate = node.get("validate");
        if (validate != null && validate.isArray()) {
            for (JsonNode rule : validate) {
                if (rule.has("required") && rule.get("required").asBoolean(false)) {
                    return true;
                }
            }
        }
        JsonNode rules = node.get("rules");
        if (rules != null && rules.isArray()) {
            for (JsonNode rule : rules) {
                if (rule.has("required") && rule.get("required").asBoolean(false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String resolveOptionSource(JsonNode node) {
        if (node.has("dictType")) {
            return "DICT:" + node.get("dictType").asText();
        }
        if (node.has("props") && node.get("props").has("dictType")) {
            return "DICT:" + node.get("props").get("dictType").asText();
        }
        if (node.has("options") || node.path("props").has("options")) {
            return "STATIC";
        }
        return null;
    }

    private String inferDataType(String componentType) {
        if (!StringUtils.hasText(componentType)) {
            return "string";
        }
        String type = componentType.toLowerCase();
        if (type.contains("number") || type.contains("rate") || type.contains("slider")) {
            return "number";
        }
        if (type.contains("date") || type.contains("time")) {
            return "datetime";
        }
        if (type.contains("switch")) {
            return "boolean";
        }
        if (type.contains("checkbox") || type.contains("upload")) {
            return "array";
        }
        return "string";
    }

    private String firstText(JsonNode node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            String value = textValue(node, key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String textValue(JsonNode node, String key) {
        if (node == null || !node.has(key) || node.get(key).isNull()) {
            return null;
        }
        JsonNode value = node.get(key);
        return value.isValueNode() ? value.asText() : null;
    }

    private Long resolveTenantId() {
        try {
            Long tenantId = SessionHelper.getTenantId();
            return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
        } catch (Exception e) {
            return DEFAULT_TENANT_ID;
        }
    }

    private Long resolveUserId() {
        try {
            return SessionHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
