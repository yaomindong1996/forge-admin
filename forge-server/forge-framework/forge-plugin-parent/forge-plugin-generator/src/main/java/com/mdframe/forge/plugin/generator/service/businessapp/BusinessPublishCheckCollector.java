package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.constant.BusinessPublishCheckLevel;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckItemVO;

import java.util.List;
import java.util.Set;

/**
 * 发布检查结果收集器，统一维护不可降级的阻断项白名单。
 */
final class BusinessPublishCheckCollector {

    private static final Set<String> REQUIRED_BLOCK_ITEM_CODES = Set.of(
            "FIELD_EMPTY", "FIELD_LABEL_EMPTY", "FIELD_CODE_EMPTY", "FIELD_DUPLICATE",
            "PAGE_EMPTY", "PAGE_REF_MISSING", "PAGE_SCHEMA_INVALID", "PAGE_TARGET_MISSING",
            "FORM_TARGET_MISSING", "FORM_COMPONENT_EMPTY", "FORM_COMPONENT_DUPLICATE",
            "FORM_FIELD_BINDING_EMPTY", "FORM_FIELD_MISSING", "FORM_FIELD_COMPONENT_EMPTY",
            "FORM_FIELD_EVENT_INVALID",
            "COMMAND_CODE_INVALID", "COMMAND_CODE_DUPLICATE", "COMMAND_PROTOCOL_INVALID",
            "VIEW_FIELD_MISSING", "RUNTIME_INVALID", "APP_ENTRY_PAGE_MISSING", "APP_ENTRY_FORM_MISSING",
            "DATASOURCE_UNAVAILABLE", "TABLE_NAME_EMPTY", "TABLE_MISSING", "TABLE_PK_MISSING",
            "TABLE_COLUMN_MISSING", "TABLE_COLUMN_CHANGED", "TABLE_INDEX_MISSING",
            "FORMULA_ERROR"
    );

    private BusinessPublishCheckCollector() {
    }

    static void add(List<BusinessPublishCheckItemVO> items, String code, String category, String level,
                    String title, String message, String fieldCode, String zoneKey, String fixAction,
                    String fixActionLabel, String fixTarget, Integer sortOrder) {
        BusinessPublishCheckItemVO item = new BusinessPublishCheckItemVO();
        item.setItemCode(code);
        item.setCategory(category);
        item.setLevel(normalizeLevel(code, level));
        item.setTitle(title);
        item.setMessage(message);
        item.setFieldCode(fieldCode);
        item.setZoneKey(zoneKey);
        item.setFixAction(fixAction);
        item.setFixActionLabel(fixActionLabel);
        item.setFixTarget(fixTarget);
        item.setSortOrder(sortOrder);
        items.add(item);
    }

    private static String normalizeLevel(String code, String level) {
        if (!BusinessPublishCheckLevel.BLOCK.equals(level)) {
            return level;
        }
        return REQUIRED_BLOCK_ITEM_CODES.contains(code) ? level : BusinessPublishCheckLevel.WARN;
    }
}
