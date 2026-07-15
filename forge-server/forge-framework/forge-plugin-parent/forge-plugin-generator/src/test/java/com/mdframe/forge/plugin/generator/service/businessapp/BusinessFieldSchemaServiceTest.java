package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessFieldDTO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeModelSchemaNormalizer;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("BusinessFieldSchemaService")
@Tag("dev")
class BusinessFieldSchemaServiceTest {

    private final BusinessFieldSchemaService service = new BusinessFieldSchemaService(
            new LowcodeModelSchemaNormalizer(),
            new BusinessNamingService()
    );

    @Test
    @DisplayName("inline options select does not require system dict type")
    void inlineOptionsSelectDoesNotRequireDictType() {
        BusinessFieldDTO dto = baseDictField();
        List<Map<String, Object>> options = List.of(
                Map.of("label", "电话", "value", "1"),
                Map.of("label", "微信", "value", "2")
        );
        dto.getBasicProps().put("options", options);

        LowcodeFieldSchema schema = service.buildFieldSchema(dto);

        assertEquals("DICT", schema.getBusinessFieldType());
        assertEquals("select", schema.getComponentType());
        assertEquals("", schema.getDictType());
        assertSame(options, schema.getBasicProps().get("options"));
    }

    @Test
    @DisplayName("dict field without dict type or inline options is rejected")
    void dictFieldWithoutDictTypeOrInlineOptionsIsRejected() {
        BusinessFieldDTO dto = baseDictField();

        BusinessException error = assertThrows(BusinessException.class, () -> service.buildFieldSchema(dto));

        assertEquals("字段「跟进方式」使用字典组件时必须配置字典类型或静态选项", error.getMessage());
    }

    @Test
    @DisplayName("explicit text type is not overridden by status field name")
    void explicitTextTypeIsNotOverriddenByStatusFieldName() {
        BusinessFieldDTO dto = baseField("订单状态", "orderStatus", "TEXT");

        LowcodeFieldSchema schema = service.buildFieldSchema(dto);

        assertEquals("TEXT", schema.getBusinessFieldType());
    }

    @Test
    @DisplayName("explicit switch type is not overridden by status field name")
    void explicitSwitchTypeIsNotOverriddenByStatusFieldName() {
        BusinessFieldDTO dto = baseField("启用状态", "enabled", "SWITCH");

        LowcodeFieldSchema schema = service.buildFieldSchema(dto);

        assertEquals("SWITCH", schema.getBusinessFieldType());
    }

    @Test
    @DisplayName("unknown explicit type falls back to text instead of field name inference")
    void unknownExplicitTypeFallsBackToText() {
        BusinessFieldDTO dto = baseField("订单类型", "orderType", "UNKNOWN");

        LowcodeFieldSchema schema = service.buildFieldSchema(dto);

        assertEquals("TEXT", schema.getBusinessFieldType());
    }

    @Test
    @DisplayName("dict type can be resolved from basic props")
    void dictTypeCanBeResolvedFromBasicProps() {
        BusinessFieldDTO dto = baseDictField();
        dto.setDictType("");
        dto.getBasicProps().put("dictType", "pw_common_status");

        LowcodeFieldSchema schema = service.buildFieldSchema(dto);

        assertEquals("pw_common_status", schema.getDictType());
        assertEquals("DICT", schema.getBusinessFieldType());
    }

    private BusinessFieldDTO baseDictField() {
        BusinessFieldDTO dto = baseField("跟进方式", "type", "DICT");
        dto.setComponentType("select");
        dto.setLength(32);
        return dto;
    }

    private BusinessFieldDTO baseField(String fieldName, String fieldCode, String fieldType) {
        BusinessFieldDTO dto = new BusinessFieldDTO();
        dto.setFieldName(fieldName);
        dto.setFieldCode(fieldCode);
        dto.setColumnName(fieldCode);
        dto.setFieldType(fieldType);
        dto.setDataType("varchar");
        dto.setLength(128);
        dto.setRequired(false);
        dto.setListVisible(true);
        dto.setFormVisible(true);
        dto.setImportable(true);
        dto.setExportable(true);
        dto.setDictType("");
        dto.getFieldBinding().put("mode", "field");
        dto.getFieldBinding().put("fieldCode", fieldCode);
        dto.getFieldBinding().put("columnName", fieldCode);
        dto.getFieldBinding().put("source", "field_asset");
        dto.getFieldBinding().put("createIfMissing", false);
        return dto;
    }
}
