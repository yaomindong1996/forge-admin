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

        assertEquals("字典字段必须配置字典类型", error.getMessage());
    }

    private BusinessFieldDTO baseDictField() {
        BusinessFieldDTO dto = new BusinessFieldDTO();
        dto.setFieldName("跟进方式");
        dto.setFieldCode("type");
        dto.setColumnName("type");
        dto.setFieldType("DICT");
        dto.setComponentType("select");
        dto.setDataType("varchar");
        dto.setLength(32);
        dto.setRequired(false);
        dto.setListVisible(true);
        dto.setFormVisible(true);
        dto.setImportable(true);
        dto.setExportable(true);
        dto.setDictType("");
        dto.getFieldBinding().put("mode", "field");
        dto.getFieldBinding().put("fieldCode", "type");
        dto.getFieldBinding().put("columnName", "type");
        dto.getFieldBinding().put("source", "field_asset");
        dto.getFieldBinding().put("createIfMissing", false);
        return dto;
    }
}
