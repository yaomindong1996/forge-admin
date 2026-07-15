package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Lowcode database table import")
class LowcodeModelImportServiceTest {

    @Test
    @DisplayName("keeps decimal precision and required state from database column")
    void keepsDecimalPrecisionAndRequiredState() throws Exception {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName("total_amount");
        column.setColumnComment("订单金额");
        column.setColumnType("decimal(18,2)");
        column.setJavaType("BigDecimal");
        column.setJavaField("totalAmount");
        column.setIsRequired(1);
        column.setIsPk(0);
        column.setIsIncrement(0);

        LowcodeModelImportService service = new LowcodeModelImportService(null, null, null, null);
        Method method = LowcodeModelImportService.class.getDeclaredMethod("toFieldSchema", GenTableColumn.class);
        method.setAccessible(true);
        LowcodeFieldSchema field = (LowcodeFieldSchema) method.invoke(service, column);

        assertEquals("MONEY", field.getBusinessFieldType());
        assertEquals("decimal", field.getDataType());
        assertEquals(18, field.getLength());
        assertEquals(2, field.getPrecision());
        assertTrue(field.getRequired());
    }

    @Test
    @DisplayName("keeps varchar length and nullable state from database column")
    void keepsVarcharLengthAndNullableState() throws Exception {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName("order_no");
        column.setColumnComment("订单编号");
        column.setColumnType("varchar(64)");
        column.setJavaType("String");
        column.setJavaField("orderNo");
        column.setIsRequired(0);
        column.setIsPk(0);
        column.setIsIncrement(0);

        LowcodeFieldSchema field = toFieldSchema(column);

        assertEquals("TEXT", field.getBusinessFieldType());
        assertEquals("varchar", field.getDataType());
        assertEquals(64, field.getLength());
        assertFalse(field.getRequired());
    }

    private LowcodeFieldSchema toFieldSchema(GenTableColumn column) throws Exception {
        LowcodeModelImportService service = new LowcodeModelImportService(null, null, null, null);
        Method method = LowcodeModelImportService.class.getDeclaredMethod("toFieldSchema", GenTableColumn.class);
        method.setAccessible(true);
        return (LowcodeFieldSchema) method.invoke(service, column);
    }
}
