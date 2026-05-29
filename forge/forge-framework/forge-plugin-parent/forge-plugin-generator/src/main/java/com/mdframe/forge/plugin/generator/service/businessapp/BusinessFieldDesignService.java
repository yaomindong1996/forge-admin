package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
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
        if (dto != null && StringUtils.isNotBlank(dto.getFieldCode())
                && !StringUtils.equals(dto.getFieldCode(), existing.getField())) {
            throw new BusinessException("字段编码创建后不允许直接修改");
        }
        if (dto != null && StringUtils.isNotBlank(dto.getColumnName())
                && !StringUtils.equals(dto.getColumnName(), existing.getColumnName())) {
            throw new BusinessException("数据库列名创建后不允许直接修改");
        }

        BusinessFieldDTO merged = mergeField(existing, dto);
        LowcodeFieldSchema updated = fieldSchemaService.buildFieldSchema(merged);
        updated.setPrimaryKey(false);
        updated.setSystemField(false);
        updated.setReadonly(Boolean.TRUE.equals(merged.getReadonly()));
        replaceField(context.getModelSchema(), existing.getField(), updated);
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
        List<String> references = collectReferences(context, field.getField());
        if (!references.isEmpty()) {
            throw new BusinessException("字段已被引用，删除前请先移除引用: " + String.join("、", references));
        }
        if (isPublished(context.getConfig())) {
            field.setFieldStatus("DISABLED");
            field.setSearchable(false);
            field.setListVisible(false);
            field.setFormVisible(false);
            context.setPageSchema(removeFieldRefs(context.getPageSchema(), field.getField()));
        } else {
            removeField(context.getModelSchema(), field.getField());
            context.setPageSchema(removeFieldRefs(context.getPageSchema(), field.getField()));
        }
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
        if (dto != null) {
            merged.setBasicProps(dto.getBasicProps());
            merged.setAdvancedProps(dto.getAdvancedProps());
        }
        return merged;
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
        if (zone.getFieldRefs() == null) {
            zone.setFieldRefs(new ArrayList<>());
        }
        zone.getFieldRefs().removeIf(ref -> StringUtils.equals(ref, fieldCode));
        if (present) {
            zone.getFieldRefs().add(fieldCode);
        }
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
                zone.getFieldRefs().removeIf(ref -> StringUtils.equals(ref, fieldCode));
            }
        }
        return pageSchema;
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
            zone.getFieldRefs().sort(Comparator.comparingInt(ref -> order.getOrDefault(ref, Integer.MAX_VALUE)));
        }
    }

    private List<BusinessFieldVO> decorateReferences(List<BusinessFieldVO> fields,
                                                     BusinessObjectDesignerService.DesignerContext context) {
        for (BusinessFieldVO field : fields) {
            List<String> refs = collectReferences(context, field.getFieldCode());
            field.setReferencedBy(refs);
            field.setCanDelete(Boolean.TRUE.equals(field.getCanDelete()) && refs.isEmpty());
        }
        return fields;
    }

    private List<String> collectReferences(BusinessObjectDesignerService.DesignerContext context, String fieldCode) {
        Set<String> references = new LinkedHashSet<>();
        LowcodePageSchema pageSchema = context.getPageSchema();
        if (pageSchema != null && pageSchema.getZones() != null) {
            for (LowcodePageZone zone : pageSchema.getZones()) {
                if (zone != null && zone.getFieldRefs() != null && zone.getFieldRefs().contains(fieldCode)) {
                    references.add("页面区域:" + zone.getZoneKey());
                }
            }
        }
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

    private void removeField(LowcodeModelSchema modelSchema, String fieldCode) {
        if (modelSchema == null || modelSchema.getFields() == null) {
            return;
        }
        modelSchema.getFields().removeIf(field -> field != null && StringUtils.equals(fieldCode, field.getField()));
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

    private boolean isPublished(AiCrudConfig config) {
        return config != null && "PUBLISHED".equals(config.getPublishStatus());
    }
}
