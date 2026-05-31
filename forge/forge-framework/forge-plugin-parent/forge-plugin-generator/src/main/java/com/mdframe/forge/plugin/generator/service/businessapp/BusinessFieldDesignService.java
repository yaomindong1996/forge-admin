package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeIndexSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePolicySchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeRelationSchema;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelSchemaNormalizer;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessFieldVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessObjectRelationVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务对象字段设计服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessFieldDesignService {

    private static final Map<String, String> ZONE_COMPONENTS = Map.of(
            "search", "search-form",
            "table", "data-table",
            "edit", "edit-form",
            "detail", "detail-view",
            "toolbar", "table-toolbar"
    );

    private final BusinessObjectDesignerService designerService;
    private final BusinessFieldSchemaService fieldSchemaService;
    private final LowcodeDdlService ddlService;
    private final LowcodeModelSchemaNormalizer schemaNormalizer;

    public List<BusinessFieldVO> listFields(Long objectId) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        return decorateReferences(fieldSchemaService.toFieldVOList(context.getModelSchema()), context);
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessFieldVO addField(Long objectId, BusinessFieldDTO dto) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        LowcodeModelSchema modelSchema = context.getModelSchema();
        Set<String> existingFields = collectFieldCodes(modelSchema);
        BusinessFieldDTO normalized = dto == null ? new BusinessFieldDTO() : dto;
        if (normalized.getSortOrder() == null) {
            normalized.setSortOrder(nextSortOrder(modelSchema));
        }
        modelSchema = fieldSchemaService.appendField(modelSchema, normalized);
        LowcodeFieldSchema newField = findNewField(modelSchema, existingFields);
        if (newField == null) {
            throw new BusinessException("新增字段失败，请检查字段配置");
        }
        context.setModelSchema(modelSchema);
        context.setPageSchema(syncFieldVisibility(context.getPageSchema(), newField));
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED);
        return findFieldVO(objectId, newField.getField());
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessFieldVO updateField(Long objectId, String fieldCode, BusinessFieldDTO dto) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        LowcodeFieldSchema existing = requireBusinessField(context.getModelSchema(), fieldCode);
        if (Boolean.TRUE.equals(existing.getSystemField())) {
            throw new BusinessException("系统字段不可修改");
        }
        String nextFieldCode = resolveRequestedFieldCode(existing, dto);
        String nextColumnName = resolveRequestedColumnName(existing, dto, nextFieldCode);
        boolean identityChanged = !StringUtils.equals(nextFieldCode, existing.getField())
                || !StringUtils.equals(nextColumnName, existing.getColumnName());
        if (identityChanged) {
            assertFieldIdentityEditable(context, existing, nextFieldCode, nextColumnName);
        }

        BusinessFieldDTO merged = mergeField(existing, dto);
        merged.setFieldCode(nextFieldCode);
        merged.setColumnName(nextColumnName);
        LowcodeFieldSchema updated = fieldSchemaService.buildFieldSchema(merged);
        updated.setPrimaryKey(false);
        updated.setSystemField(false);
        updated.setReadonly(Boolean.TRUE.equals(merged.getReadonly()));
        replaceField(context.getModelSchema(), existing.getField(), updated);
        if (identityChanged) {
            replaceModelFieldReferences(context.getModelSchema(), existing.getField(), updated.getField(),
                    existing.getColumnName(), updated.getColumnName());
            context.setPageSchema(replaceFieldRefs(context.getPageSchema(), existing.getField(), updated.getField(),
                    existing.getColumnName(), updated.getColumnName()));
        }
        context.setModelSchema(schemaNormalizer.normalizeModelFields(context.getModelSchema(), true));
        LowcodeFieldSchema normalized = requireField(context.getModelSchema(), updated.getField());
        context.setPageSchema(syncFieldVisibility(context.getPageSchema(), normalized));
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED);
        return findFieldVO(objectId, normalized.getField());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long objectId, String fieldCode) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        LowcodeFieldSchema field = requireBusinessField(context.getModelSchema(), fieldCode);
        if (Boolean.TRUE.equals(field.getSystemField())) {
            throw new BusinessException("系统字段不可删除");
        }
        List<String> blockingReferences = collectBlockingReferences(context, field.getField());
        if (!blockingReferences.isEmpty()) {
            throw new BusinessException("字段已被对象关系引用，删除前请先移除引用: " + String.join("、", blockingReferences));
        }
        field.setFieldStatus("HIDDEN");
        field.setSearchable(false);
        field.setListVisible(false);
        field.setFormVisible(false);
        field.setImportable(false);
        field.setExportable(false);
        context.setPageSchema(removeFieldRefs(context.getPageSchema(), field.getField()));
        context.setModelSchema(schemaNormalizer.normalizeModelFields(context.getModelSchema(), true));
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<BusinessFieldVO> sortFields(Long objectId, List<String> fieldCodes) {
        if (fieldCodes == null || fieldCodes.isEmpty()) {
            throw new BusinessException("字段排序不能为空");
        }
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        Map<String, Integer> orderMap = new LinkedHashMap<>();
        for (int i = 0; i < fieldCodes.size(); i++) {
            orderMap.put(fieldCodes.get(i), (i + 1) * 10);
        }
        for (LowcodeFieldSchema field : businessFields(context.getModelSchema())) {
            Integer order = orderMap.get(field.getField());
            if (order != null) {
                field.setSortOrder(order);
            }
        }
        context.getModelSchema().getFields().sort(Comparator.comparing(field ->
                field == null || field.getSortOrder() == null ? Integer.MAX_VALUE : field.getSortOrder()));
        reorderPageRefs(context.getPageSchema(), fieldCodes);
        context.setModelSchema(schemaNormalizer.normalizeModelFields(context.getModelSchema(), true));
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED);
        return listFields(objectId);
    }

    private BusinessFieldDTO mergeField(LowcodeFieldSchema existing, BusinessFieldDTO dto) {
        BusinessFieldDTO merged = new BusinessFieldDTO();
        merged.setFieldName(StringUtils.defaultIfBlank(dto == null ? null : dto.getFieldName(), existing.getLabel()));
        merged.setFieldCode(existing.getField());
        merged.setColumnName(existing.getColumnName());
        merged.setFieldType(StringUtils.defaultIfBlank(dto == null ? null : dto.getFieldType(), existing.getBusinessFieldType()));
        merged.setDataType(StringUtils.defaultIfBlank(dto == null ? null : dto.getDataType(), existing.getDataType()));
        merged.setLength(dto != null && dto.getLength() != null ? dto.getLength() : existing.getLength());
        merged.setPrecision(dto != null && dto.getPrecision() != null ? dto.getPrecision() : existing.getPrecision());
        merged.setRequired(dto != null && dto.getRequired() != null ? dto.getRequired() : existing.getRequired());
        merged.setDefaultValue(dto != null && dto.getDefaultValue() != null ? dto.getDefaultValue() : existing.getDefaultValue());
        merged.setSearchable(dto != null && dto.getSearchable() != null ? dto.getSearchable() : existing.getSearchable());
        merged.setListVisible(dto != null && dto.getListVisible() != null ? dto.getListVisible() : existing.getListVisible());
        merged.setFormVisible(dto != null && dto.getFormVisible() != null ? dto.getFormVisible() : existing.getFormVisible());
        merged.setImportable(dto != null && dto.getImportable() != null ? dto.getImportable() : existing.getImportable());
        merged.setExportable(dto != null && dto.getExportable() != null ? dto.getExportable() : existing.getExportable());
        merged.setComponentType(StringUtils.defaultIfBlank(dto == null ? null : dto.getComponentType(), existing.getComponentType()));
        merged.setQueryType(StringUtils.defaultIfBlank(dto == null ? null : dto.getQueryType(), existing.getQueryType()));
        merged.setDictType(StringUtils.defaultString(StringUtils.defaultIfBlank(dto == null ? null : dto.getDictType(), existing.getDictType())));
        merged.setSensitiveType(StringUtils.defaultIfBlank(dto == null ? null : dto.getSensitiveType(), existing.getSensitiveType()));
        merged.setEncryptAlgorithm(StringUtils.defaultIfBlank(dto == null ? null : dto.getEncryptAlgorithm(), existing.getEncryptAlgorithm()));
        merged.setSortable(dto != null && dto.getSortable() != null ? dto.getSortable() : existing.getSortable());
        merged.setSystemField(false);
        merged.setReadonly(dto != null && dto.getReadonly() != null ? dto.getReadonly() : existing.getReadonly());
        merged.setFieldStatus(StringUtils.defaultIfBlank(dto == null ? null : dto.getFieldStatus(), existing.getFieldStatus()));
        merged.setReferenceObjectCode(StringUtils.defaultIfBlank(dto == null ? null : dto.getReferenceObjectCode(), existing.getReferenceObjectCode()));
        merged.setReferenceDisplayField(StringUtils.defaultIfBlank(dto == null ? null : dto.getReferenceDisplayField(), existing.getReferenceDisplayField()));
        merged.setRemark(StringUtils.defaultIfBlank(dto == null ? null : dto.getRemark(), existing.getRemark()));
        merged.setSortOrder(dto != null && dto.getSortOrder() != null ? dto.getSortOrder() : existing.getSortOrder());
        merged.setBasicProps(mergeProps(existing.getBasicProps(), dto == null ? null : dto.getBasicProps()));
        merged.setAdvancedProps(mergeProps(existing.getAdvancedProps(), dto == null ? null : dto.getAdvancedProps()));
        return merged;
    }

    private Map<String, Object> mergeProps(Map<String, Object> existing, Map<String, Object> requested) {
        if (requested == null) {
            return existing == null ? new LinkedHashMap<>() : new LinkedHashMap<>(existing);
        }
        return new LinkedHashMap<>(requested);
    }

    private String resolveRequestedFieldCode(LowcodeFieldSchema existing, BusinessFieldDTO dto) {
        String requested = dto == null ? null : dto.getFieldCode();
        if (StringUtils.isBlank(requested)) {
            return existing.getField();
        }
        return fieldSchemaService.normalizeBusinessFieldCode(requested);
    }

    private String resolveRequestedColumnName(LowcodeFieldSchema existing, BusinessFieldDTO dto, String nextFieldCode) {
        String requested = dto == null ? null : dto.getColumnName();
        if (StringUtils.isBlank(requested)) {
            String defaultExistingColumn = fieldSchemaService.camelToSnake(existing.getField());
            if (!StringUtils.equals(nextFieldCode, existing.getField())
                    && StringUtils.equals(existing.getColumnName(), defaultExistingColumn)) {
                return fieldSchemaService.camelToSnake(nextFieldCode);
            }
            return existing.getColumnName();
        }
        String normalized = fieldSchemaService.normalizeBusinessColumnName(requested);
        String defaultExistingColumn = fieldSchemaService.camelToSnake(existing.getField());
        if (!StringUtils.equals(nextFieldCode, existing.getField())
                && StringUtils.equals(normalized, existing.getColumnName())
                && StringUtils.equals(existing.getColumnName(), defaultExistingColumn)) {
            return fieldSchemaService.camelToSnake(nextFieldCode);
        }
        return normalized;
    }

    private void assertFieldIdentityEditable(BusinessObjectDesignerService.DesignerContext context,
                                             LowcodeFieldSchema existing,
                                             String nextFieldCode,
                                             String nextColumnName) {
        if (StringUtils.isBlank(nextFieldCode)) {
            throw new BusinessException("字段英文名不能为空");
        }
        if (StringUtils.isBlank(nextColumnName)) {
            throw new BusinessException("数据库列名不能为空");
        }
        assertUniqueFieldIdentity(context.getModelSchema(), existing.getField(), nextFieldCode, nextColumnName);
        List<String> blockingReferences = collectBlockingReferences(context, existing.getField());
        if (!blockingReferences.isEmpty()) {
            throw new BusinessException("字段已被对象关系引用，修改英文名前请先移除引用: "
                    + String.join("、", blockingReferences));
        }
        if (isStorageColumnSynced(context.getModelSchema(), existing)) {
            throw new BusinessException("字段已同步到数据表，不能直接修改英文名或列名。请新增规范英文名的新字段，迁移数据后停用旧字段。");
        }
    }

    private void assertUniqueFieldIdentity(LowcodeModelSchema modelSchema, String currentFieldCode,
                                           String nextFieldCode, String nextColumnName) {
        if (modelSchema == null || modelSchema.getFields() == null) {
            return;
        }
        for (LowcodeFieldSchema field : modelSchema.getFields()) {
            if (field == null || StringUtils.equals(field.getField(), currentFieldCode)) {
                continue;
            }
            if (StringUtils.equals(field.getField(), nextFieldCode)) {
                throw new BusinessException("字段英文名已存在: " + nextFieldCode);
            }
            if (StringUtils.equals(field.getColumnName(), nextColumnName)) {
                throw new BusinessException("数据库列名已存在: " + nextColumnName);
            }
        }
    }

    private boolean isStorageColumnSynced(LowcodeModelSchema modelSchema, LowcodeFieldSchema existing) {
        if (modelSchema == null || StringUtils.isBlank(modelSchema.getTableName())
                || StringUtils.isBlank(existing.getColumnName())) {
            return false;
        }
        try {
            return ddlService.tableExists(modelSchema.getTableName())
                    && ddlService.listColumns(modelSchema.getTableName()).contains(existing.getColumnName());
        } catch (Exception e) {
            throw new BusinessException("无法确认数据表列状态，暂不能修改字段英文名或列名");
        }
    }

    private LowcodePageSchema syncFieldVisibility(LowcodePageSchema pageSchema, LowcodeFieldSchema field) {
        LowcodePageSchema target = pageSchema == null ? new LowcodePageSchema() : pageSchema;
        ensureZone(target, "search");
        ensureZone(target, "table");
        ensureZone(target, "edit");
        ensureZone(target, "detail");
        ensureZone(target, "toolbar");
        boolean enabled = !"DISABLED".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus()))
                && !"HIDDEN".equalsIgnoreCase(StringUtils.defaultString(field.getFieldStatus()));
        syncZoneRef(target, "search", field.getField(), enabled && Boolean.TRUE.equals(field.getSearchable()));
        syncZoneRef(target, "table", field.getField(), enabled && (field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible())));
        syncZoneRef(target, "edit", field.getField(), enabled && (field.getFormVisible() == null || Boolean.TRUE.equals(field.getFormVisible())));
        syncZoneRef(target, "detail", field.getField(), enabled && (field.getListVisible() == null || Boolean.TRUE.equals(field.getListVisible())));
        return target;
    }

    private void syncZoneRef(LowcodePageSchema pageSchema, String zoneKey, String fieldCode, boolean present) {
        LowcodePageZone zone = ensureZone(pageSchema, zoneKey);
        List<String> refs = withoutFieldRef(zone.getFieldRefs(), fieldCode);
        if (present) {
            refs.add(fieldCode);
        }
        zone.setFieldRefs(refs);
    }

    private LowcodePageZone ensureZone(LowcodePageSchema pageSchema, String zoneKey) {
        if (pageSchema.getZones() == null) {
            pageSchema.setZones(new ArrayList<>());
        }
        return pageSchema.getZones().stream()
                .filter(zone -> zone != null && StringUtils.equals(zoneKey, zone.getZoneKey()))
                .findFirst()
                .orElseGet(() -> {
                    LowcodePageZone zone = new LowcodePageZone();
                    zone.setZoneKey(zoneKey);
                    zone.setComponentKey(ZONE_COMPONENTS.get(zoneKey));
                    zone.setEnabled(true);
                    zone.setFieldRefs(new ArrayList<>());
                    zone.setProps(new LinkedHashMap<>());
                    pageSchema.getZones().add(zone);
                    return zone;
                });
    }

    private LowcodePageSchema removeFieldRefs(LowcodePageSchema pageSchema, String fieldCode) {
        if (pageSchema == null || pageSchema.getZones() == null) {
            return pageSchema;
        }
        for (LowcodePageZone zone : pageSchema.getZones()) {
            if (zone != null && zone.getFieldRefs() != null) {
                zone.setFieldRefs(withoutFieldRef(zone.getFieldRefs(), fieldCode));
            }
            if (zone != null && zone.getProps() != null) {
                removeFieldFromNestedValue(zone.getProps(), fieldCode);
            }
        }
        if (pageSchema.getListGridLayout() != null) {
            removeFieldFromNestedValue(pageSchema.getListGridLayout(), fieldCode);
        }
        return pageSchema;
    }

    private LowcodePageSchema replaceFieldRefs(LowcodePageSchema pageSchema, String oldFieldCode, String newFieldCode,
                                               String oldColumnName, String newColumnName) {
        if (pageSchema == null) {
            return null;
        }
        if (pageSchema.getZones() != null) {
            for (LowcodePageZone zone : pageSchema.getZones()) {
                if (zone == null) {
                    continue;
                }
                zone.setFieldRefs(replaceStringValue(zone.getFieldRefs(), oldFieldCode, newFieldCode));
                replaceFieldInNestedValue(zone.getProps(), oldFieldCode, newFieldCode, oldColumnName, newColumnName);
            }
        }
        replaceFieldInNestedValue(pageSchema.getListGridLayout(), oldFieldCode, newFieldCode, oldColumnName, newColumnName);
        if (pageSchema.getModelRefs() != null) {
            for (var modelRef : pageSchema.getModelRefs()) {
                replaceFieldInNestedValue(modelRef.getFields(), oldFieldCode, newFieldCode, oldColumnName, newColumnName);
                replaceFieldInNestedValue(modelRef.getProps(), oldFieldCode, newFieldCode, oldColumnName, newColumnName);
            }
        }
        return pageSchema;
    }

    private void replaceModelFieldReferences(LowcodeModelSchema modelSchema, String oldFieldCode, String newFieldCode,
                                             String oldColumnName, String newColumnName) {
        if (modelSchema == null) {
            return;
        }
        if (modelSchema.getIndexes() != null) {
            for (LowcodeIndexSchema index : modelSchema.getIndexes()) {
                if (index != null) {
                    index.setFields(replaceStringValue(index.getFields(), oldFieldCode, newFieldCode));
                }
            }
        }
        if (modelSchema.getRelations() != null) {
            for (LowcodeRelationSchema relation : modelSchema.getRelations()) {
                if (relation == null) {
                    continue;
                }
                if (StringUtils.equals(relation.getSourceField(), oldFieldCode)) {
                    relation.setSourceField(newFieldCode);
                }
                if (StringUtils.equals(relation.getTargetField(), oldFieldCode)) {
                    relation.setTargetField(newFieldCode);
                }
                if (StringUtils.equals(relation.getDisplayField(), oldFieldCode)) {
                    relation.setDisplayField(newFieldCode);
                }
            }
        }
        LowcodePolicySchema policies = modelSchema.getPolicies();
        if (policies != null) {
            if (StringUtils.equals(policies.getUserField(), oldFieldCode)) {
                policies.setUserField(newFieldCode);
            }
            if (StringUtils.equals(policies.getOrgField(), oldFieldCode)) {
                policies.setOrgField(newFieldCode);
            }
            if (StringUtils.equals(policies.getRegionField(), oldFieldCode)) {
                policies.setRegionField(newFieldCode);
            }
            if (StringUtils.equals(policies.getUserColumn(), oldColumnName)) {
                policies.setUserColumn(newColumnName);
            }
            if (StringUtils.equals(policies.getOrgColumn(), oldColumnName)) {
                policies.setOrgColumn(newColumnName);
            }
            if (StringUtils.equals(policies.getRegionColumn(), oldColumnName)) {
                policies.setRegionColumn(newColumnName);
            }
        }
    }

    private void reorderPageRefs(LowcodePageSchema pageSchema, List<String> fieldCodes) {
        if (pageSchema == null || pageSchema.getZones() == null) {
            return;
        }
        Map<String, Integer> order = new LinkedHashMap<>();
        for (int i = 0; i < fieldCodes.size(); i++) {
            order.put(fieldCodes.get(i), i);
        }
        for (LowcodePageZone zone : pageSchema.getZones()) {
            if (zone == null || zone.getFieldRefs() == null) {
                continue;
            }
            List<String> sortedRefs = new ArrayList<>(zone.getFieldRefs());
            sortedRefs.sort(Comparator.comparingInt(ref -> order.getOrDefault(ref, Integer.MAX_VALUE)));
            zone.setFieldRefs(sortedRefs);
        }
    }

    private List<String> withoutFieldRef(List<String> refs, String fieldCode) {
        List<String> next = refs == null ? new ArrayList<>() : new ArrayList<>(refs);
        next.removeIf(ref -> StringUtils.equals(ref, fieldCode));
        return next;
    }

    private List<String> replaceStringValue(List<String> refs, String oldValue, String newValue) {
        if (refs == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(refs.stream()
                .map(ref -> StringUtils.equals(ref, oldValue) ? newValue : ref)
                .toList());
    }

    private List<BusinessFieldVO> decorateReferences(List<BusinessFieldVO> fields,
                                                     BusinessObjectDesignerService.DesignerContext context) {
        for (BusinessFieldVO field : fields) {
            List<String> refs = collectReferences(context, field.getFieldCode());
            List<String> blockingRefs = collectBlockingReferences(context, field.getFieldCode());
            field.setReferencedBy(refs);
            field.setCanDelete(Boolean.TRUE.equals(field.getCanDelete()) && blockingRefs.isEmpty());
        }
        return fields;
    }

    private List<String> collectReferences(BusinessObjectDesignerService.DesignerContext context, String fieldCode) {
        Set<String> references = new LinkedHashSet<>();
        references.addAll(collectPageReferences(context, fieldCode));
        references.addAll(collectBlockingReferences(context, fieldCode));
        return new ArrayList<>(references);
    }

    private List<String> collectPageReferences(BusinessObjectDesignerService.DesignerContext context, String fieldCode) {
        Set<String> references = new LinkedHashSet<>();
        LowcodePageSchema pageSchema = context.getPageSchema();
        if (pageSchema != null && pageSchema.getZones() != null) {
            for (LowcodePageZone zone : pageSchema.getZones()) {
                if (zone == null) {
                    continue;
                }
                boolean referenced = zone.getFieldRefs() != null && zone.getFieldRefs().contains(fieldCode);
                if (!referenced) {
                    referenced = containsFieldReference(zone.getProps(), fieldCode);
                }
                if (referenced) {
                    references.add("页面区域:" + pageZoneLabel(zone.getZoneKey()));
                }
            }
            if (containsFieldReference(pageSchema.getListGridLayout(), fieldCode)) {
                references.add("页面区域:列表自由布局");
            }
        }
        return new ArrayList<>(references);
    }

    private List<String> collectBlockingReferences(BusinessObjectDesignerService.DesignerContext context, String fieldCode) {
        Set<String> references = new LinkedHashSet<>();
        if (context.getRelations() != null && context.getObject() != null) {
            for (BusinessObjectRelationVO relation : context.getRelations()) {
                if (relation == null) {
                    continue;
                }
                if (StringUtils.equals(context.getObject().getObjectCode(), relation.getSourceObjectCode())
                        && StringUtils.equals(fieldCode, relation.getSourceFieldCode())) {
                    references.add("对象关系:" + relation.getRelationName());
                }
                if (StringUtils.equals(context.getObject().getObjectCode(), relation.getTargetObjectCode())
                        && StringUtils.equals(fieldCode, relation.getTargetFieldCode())) {
                    references.add("对象关系:" + relation.getRelationName());
                }
            }
        }
        return new ArrayList<>(references);
    }

    @SuppressWarnings("unchecked")
    private void removeFieldFromNestedValue(Object value, String fieldCode) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<Object, Object> map = (Map<Object, Object>) rawMap;
            Object fieldRefs = map.get("fieldRefs");
            if (fieldRefs instanceof List<?> refs) {
                map.put("fieldRefs", withoutStringValue(refs, fieldCode));
            }
            Object fieldSettings = map.get("fieldSettings");
            if (fieldSettings instanceof Map<?, ?> settings) {
                ((Map<Object, Object>) settings).remove(fieldCode);
            }
            if (StringUtils.equals(String.valueOf(map.get("fieldRef")), fieldCode)) {
                map.remove("fieldRef");
            }
            for (Object child : new ArrayList<>(map.values())) {
                removeFieldFromNestedValue(child, fieldCode);
            }
            return;
        }
        if (value instanceof List<?> list) {
            list.removeIf(item -> isFieldReferenceItem(item, fieldCode));
            for (Object child : new ArrayList<>(list)) {
            removeFieldFromNestedValue(child, fieldCode);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void replaceFieldInNestedValue(Object value, String oldFieldCode, String newFieldCode,
                                           String oldColumnName, String newColumnName) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<Object, Object> map = (Map<Object, Object>) rawMap;
            Object fieldRefs = map.get("fieldRefs");
            if (fieldRefs instanceof List<?> refs) {
                map.put("fieldRefs", replaceObjectStringValue(refs, oldFieldCode, newFieldCode));
            }
            Object fieldSettings = map.get("fieldSettings");
            if (fieldSettings instanceof Map<?, ?> rawSettings) {
                Map<Object, Object> settings = (Map<Object, Object>) rawSettings;
                if (settings.containsKey(oldFieldCode)) {
                    Object oldValue = settings.remove(oldFieldCode);
                    settings.putIfAbsent(newFieldCode, oldValue);
                }
            }
            replaceMapStringValue(map, "fieldRef", oldFieldCode, newFieldCode);
            replaceMapStringValue(map, "field", oldFieldCode, newFieldCode);
            replaceMapStringValue(map, "sourceField", oldFieldCode, newFieldCode);
            replaceMapStringValue(map, "displayField", oldFieldCode, newFieldCode);
            replaceMapStringValue(map, "columnName", oldColumnName, newColumnName);
            for (Object child : new ArrayList<>(map.values())) {
                replaceFieldInNestedValue(child, oldFieldCode, newFieldCode, oldColumnName, newColumnName);
            }
            return;
        }
        if (value instanceof List<?> rawList) {
            List<Object> list = (List<Object>) rawList;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (StringUtils.equals(String.valueOf(item), oldFieldCode)) {
                    list.set(i, newFieldCode);
                } else if (StringUtils.equals(String.valueOf(item), oldColumnName)) {
                    list.set(i, newColumnName);
                } else {
                    replaceFieldInNestedValue(item, oldFieldCode, newFieldCode, oldColumnName, newColumnName);
                }
            }
        }
    }

    private void replaceMapStringValue(Map<Object, Object> map, String key, String oldValue, String newValue) {
        if (StringUtils.isBlank(oldValue) || !map.containsKey(key)) {
            return;
        }
        Object value = map.get(key);
        if (StringUtils.equals(String.valueOf(value), oldValue)) {
            map.put(key, newValue);
        }
    }

    private List<Object> withoutStringValue(List<?> values, String fieldCode) {
        List<Object> next = new ArrayList<>(values);
        next.removeIf(item -> StringUtils.equals(String.valueOf(item), fieldCode));
        return next;
    }

    private List<Object> replaceObjectStringValue(List<?> values, String oldValue, String newValue) {
        List<Object> next = new ArrayList<>(values);
        for (int i = 0; i < next.size(); i++) {
            if (StringUtils.equals(String.valueOf(next.get(i)), oldValue)) {
                next.set(i, newValue);
            }
        }
        return next;
    }

    private boolean isFieldReferenceItem(Object item, String fieldCode) {
        if (!(item instanceof Map<?, ?> map)) {
            return false;
        }
        return StringUtils.equals(String.valueOf(map.get("fieldRef")), fieldCode)
                || StringUtils.equals(String.valueOf(map.get("field")), fieldCode);
    }

    private boolean containsFieldReference(Object value, String fieldCode) {
        if (value instanceof Map<?, ?> map) {
            if (StringUtils.equals(String.valueOf(map.get("fieldRef")), fieldCode)
                    || StringUtils.equals(String.valueOf(map.get("field")), fieldCode)) {
                return true;
            }
            Object fieldRefs = map.get("fieldRefs");
            if (fieldRefs instanceof List<?> refs
                    && refs.stream().anyMatch(ref -> StringUtils.equals(String.valueOf(ref), fieldCode))) {
                return true;
            }
            Object fieldSettings = map.get("fieldSettings");
            if (fieldSettings instanceof Map<?, ?> settings && settings.containsKey(fieldCode)) {
                return true;
            }
            return map.values().stream().anyMatch(child -> containsFieldReference(child, fieldCode));
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(item -> containsFieldReference(item, fieldCode));
        }
        return false;
    }

    private String pageZoneLabel(String zoneKey) {
        return switch (StringUtils.defaultString(zoneKey)) {
            case "search" -> "查询条件";
            case "table" -> "列表";
            case "edit" -> "表单";
            case "detail" -> "详情";
            case "toolbar" -> "工具栏";
            default -> zoneKey;
        };
    }

    private BusinessFieldVO findFieldVO(Long objectId, String fieldCode) {
        return listFields(objectId).stream()
                .filter(field -> StringUtils.equals(fieldCode, field.getFieldCode()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("字段保存后未找到: " + fieldCode));
    }

    private LowcodeFieldSchema requireBusinessField(LowcodeModelSchema modelSchema, String fieldCode) {
        LowcodeFieldSchema field = requireField(modelSchema, fieldCode);
        if (Boolean.TRUE.equals(field.getSystemField())) {
            throw new BusinessException("系统字段不允许执行该操作");
        }
        return field;
    }

    private LowcodeFieldSchema requireField(LowcodeModelSchema modelSchema, String fieldCode) {
        if (modelSchema == null || modelSchema.getFields() == null || StringUtils.isBlank(fieldCode)) {
            throw new BusinessException("字段不存在: " + fieldCode);
        }
        return modelSchema.getFields().stream()
                .filter(field -> field != null && StringUtils.equals(fieldCode, field.getField()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("字段不存在: " + fieldCode));
    }

    private void replaceField(LowcodeModelSchema modelSchema, String fieldCode, LowcodeFieldSchema updated) {
        for (int i = 0; i < modelSchema.getFields().size(); i++) {
            LowcodeFieldSchema field = modelSchema.getFields().get(i);
            if (field != null && StringUtils.equals(fieldCode, field.getField())) {
                modelSchema.getFields().set(i, updated);
                return;
            }
        }
        throw new BusinessException("字段不存在: " + fieldCode);
    }

    private LowcodeFieldSchema findNewField(LowcodeModelSchema modelSchema, Set<String> existingFields) {
        return businessFields(modelSchema).stream()
                .filter(field -> !existingFields.contains(field.getField()))
                .findFirst()
                .orElse(null);
    }

    private List<LowcodeFieldSchema> businessFields(LowcodeModelSchema modelSchema) {
        if (modelSchema == null || modelSchema.getFields() == null) {
            return List.of();
        }
        return modelSchema.getFields().stream()
                .filter(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()))
                .toList();
    }

    private Set<String> collectFieldCodes(LowcodeModelSchema modelSchema) {
        Set<String> fields = new LinkedHashSet<>();
        if (modelSchema != null && modelSchema.getFields() != null) {
            modelSchema.getFields().stream()
                    .filter(field -> field != null && StringUtils.isNotBlank(field.getField()))
                    .forEach(field -> fields.add(field.getField()));
        }
        return fields;
    }

    private Integer nextSortOrder(LowcodeModelSchema modelSchema) {
        return businessFields(modelSchema).stream()
                .map(LowcodeFieldSchema::getSortOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(0) + 10;
    }

}
