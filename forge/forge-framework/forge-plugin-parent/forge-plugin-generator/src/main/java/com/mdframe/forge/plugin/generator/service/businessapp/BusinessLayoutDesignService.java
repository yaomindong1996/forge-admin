package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessObjectDesignStatus;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.dto.AiCrudConfigRenderVO;
import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessLayoutDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageZone;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessLayoutVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * 业务对象表单、列表和详情布局设计服务。
 */
@Service
@RequiredArgsConstructor
public class BusinessLayoutDesignService {

    private static final Set<String> FORM_ZONES = Set.of("edit");
    private static final Set<String> LIST_ZONES = Set.of("search", "table", "toolbar");
    private static final Set<String> DETAIL_ZONES = Set.of("detail");

    private final BusinessObjectDesignerService designerService;
    private final LowcodeSchemaValidator schemaValidator;
    private final AiCrudConfigService crudConfigService;

    @Transactional(rollbackFor = Exception.class)
    public void saveFormLayout(Long objectId, BusinessLayoutDTO dto) {
        saveLayout(objectId, dto, FORM_ZONES);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveListLayout(Long objectId, BusinessLayoutDTO dto) {
        saveLayout(objectId, dto, LIST_ZONES);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDetailLayout(Long objectId, BusinessLayoutDTO dto) {
        saveLayout(objectId, dto, DETAIL_ZONES);
    }

    public AiCrudConfigRenderVO previewLayout(Long objectId, BusinessLayoutDTO dto) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        LowcodePageSchema pageSchema = resolvePageSchema(context.getPageSchema(), dto, Set.of());
        validatePage(context.getModelSchema(), pageSchema);

        AiCrudConfig config = context.getConfig() == null ? new AiCrudConfig() : context.getConfig();
        config.setConfigKey(StringUtils.defaultIfBlank(config.getConfigKey(),
                context.getObject().getSuiteCode() + "_" + context.getObject().getObjectCode()));
        config.setTableName(context.getModelSchema().getTableName());
        config.setTableComment(context.getModelSchema().getBusinessName());
        config.setAppName(context.getObject().getObjectName());
        config.setMenuName(StringUtils.defaultIfBlank(config.getMenuName(), context.getObject().getObjectName()));
        config.setObjectCode(context.getObject().getObjectCode());
        config.setObjectName(context.getObject().getObjectName());
        config.setMode("CONFIG");
        config.setBuildMode("LOWCODE");
        config.setStatus("0");
        config.setPublishStatus("DRAFT");
        config.setLayoutType(StringUtils.defaultIfBlank(pageSchema.getLayoutType(), "simple-crud"));
        config.setModelSchema(designerService.writeJson(context.getModelSchema(), "modelSchema"));
        config.setPageSchema(designerService.writeJson(pageSchema, "pageSchema"));
        return crudConfigService.buildRenderConfig(config);
    }

    public BusinessLayoutVO getLayout(Long objectId, String layoutKey) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        BusinessLayoutVO vo = new BusinessLayoutVO();
        vo.setLayoutKey(layoutKey);
        vo.setLayoutName(resolveLayoutName(layoutKey));
        vo.setLayoutType(context.getPageSchema() == null ? null : context.getPageSchema().getLayoutType());
        vo.setPageSchema(context.getPageSchema());
        vo.setZones(filterZones(context.getPageSchema(), resolveZones(layoutKey)));
        return vo;
    }

    private void saveLayout(Long objectId, BusinessLayoutDTO dto, Set<String> zoneKeys) {
        BusinessObjectDesignerService.DesignerContext context = designerService.loadContext(objectId);
        LowcodePageSchema pageSchema = resolvePageSchema(context.getPageSchema(), dto, zoneKeys);
        validatePage(context.getModelSchema(), pageSchema);
        context.setPageSchema(pageSchema);
        designerService.saveDraft(context, BusinessObjectDesignStatus.CHANGED);
    }

    private LowcodePageSchema resolvePageSchema(LowcodePageSchema current, BusinessLayoutDTO dto, Set<String> zoneKeys) {
        if (dto == null) {
            throw new BusinessException("布局配置不能为空");
        }
        LowcodePageSchema target = current == null ? new LowcodePageSchema() : current;
        if (dto.getPageSchema() != null && (zoneKeys == null || zoneKeys.isEmpty())) {
            target = dto.getPageSchema();
        } else if (dto.getPageSchema() != null) {
            mergeZones(target, dto.getPageSchema().getZones(), zoneKeys);
            if (StringUtils.isNotBlank(dto.getPageSchema().getLayoutType())) {
                target.setLayoutType(dto.getPageSchema().getLayoutType());
            }
            if (StringUtils.isNotBlank(dto.getPageSchema().getListLayoutMode())) {
                target.setListLayoutMode(dto.getPageSchema().getListLayoutMode());
            }
            if (dto.getPageSchema().getListGridLayout() != null && !dto.getPageSchema().getListGridLayout().isEmpty()) {
                target.setListGridLayout(dto.getPageSchema().getListGridLayout());
            }
            if (dto.getPageSchema().getModelRefs() != null && !dto.getPageSchema().getModelRefs().isEmpty()) {
                target.setModelRefs(dto.getPageSchema().getModelRefs());
                target.setPrimaryModelId(dto.getPageSchema().getPrimaryModelId());
                target.setPrimaryModelCode(dto.getPageSchema().getPrimaryModelCode());
            }
        }
        mergeZones(target, dto.getZones(), zoneKeys);
        if (StringUtils.isNotBlank(dto.getLayoutType())) {
            target.setLayoutType(dto.getLayoutType());
        }
        if (StringUtils.isBlank(target.getLayoutType())) {
            target.setLayoutType("simple-crud");
        }
        if (target.getZones() == null) {
            target.setZones(new ArrayList<>());
        }
        return target;
    }

    private void mergeZones(LowcodePageSchema target, List<LowcodePageZone> zones, Set<String> zoneKeys) {
        if (zones == null || zones.isEmpty()) {
            return;
        }
        if (target.getZones() == null) {
            target.setZones(new ArrayList<>());
        }
        for (LowcodePageZone zone : zones) {
            if (zone == null || StringUtils.isBlank(zone.getZoneKey())) {
                continue;
            }
            if (zoneKeys != null && !zoneKeys.isEmpty() && !zoneKeys.contains(zone.getZoneKey())) {
                continue;
            }
            target.getZones().removeIf(item -> item != null && StringUtils.equals(item.getZoneKey(), zone.getZoneKey()));
            target.getZones().add(zone);
        }
    }

    private void validatePage(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
        if (modelSchema == null || modelSchema.getFields() == null || modelSchema.getFields().stream()
                .noneMatch(field -> field != null && !Boolean.TRUE.equals(field.getSystemField()))) {
            return;
        }
        schemaValidator.validatePage(pageSchema, modelSchema);
    }

    private List<LowcodePageZone> filterZones(LowcodePageSchema pageSchema, Set<String> zoneKeys) {
        if (pageSchema == null || pageSchema.getZones() == null) {
            return new ArrayList<>();
        }
        return pageSchema.getZones().stream()
                .filter(zone -> zone != null && zoneKeys.contains(zone.getZoneKey()))
                .toList();
    }

    private Set<String> resolveZones(String layoutKey) {
        return switch (StringUtils.defaultString(layoutKey)) {
            case "form" -> FORM_ZONES;
            case "list" -> LIST_ZONES;
            case "detail" -> DETAIL_ZONES;
            default -> Set.of("search", "table", "edit", "detail", "toolbar");
        };
    }

    private String resolveLayoutName(String layoutKey) {
        return switch (StringUtils.defaultString(layoutKey)) {
            case "form" -> "表单布局";
            case "list" -> "列表布局";
            case "detail" -> "详情布局";
            default -> "对象布局";
        };
    }
}
