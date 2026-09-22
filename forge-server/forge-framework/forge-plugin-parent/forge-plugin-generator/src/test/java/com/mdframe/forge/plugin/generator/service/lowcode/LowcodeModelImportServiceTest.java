package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.domain.entity.GenTableColumn;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeAuditStrategy;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    @DisplayName("enables audit strategy when only partial forge audit columns exist")
    void enablesAuditStrategyForPartialAuditColumns() throws Exception {
        LowcodeAuditStrategy strategy = buildAuditStrategy(List.of(
                column("id"),
                column("create_by"),
                column("create_time"),
                column("update_time"),
                column("biz_name")));

        assertEquals("FORGE_COLUMNS", strategy.getMode());
        assertEquals("create_by", strategy.getCreateByColumn());
        assertEquals("create_time", strategy.getCreateTimeColumn());
        assertNull(strategy.getCreateDeptColumn());
        assertNull(strategy.getUpdateByColumn());
        assertEquals("update_time", strategy.getUpdateTimeColumn());
    }

    @Test
    @DisplayName("maps database column defaults into field defaults")
    void mapsDatabaseColumnDefaults() throws Exception {
        GenTableColumn switchColumn = column("enabled");
        switchColumn.setColumnComment("是否启用");
        switchColumn.setColumnType("tinyint(1)");
        switchColumn.setJavaType("Integer");
        switchColumn.setJavaField("enabled");
        switchColumn.setColumnDefault("1");
        switchColumn.setIsRequired(0);
        switchColumn.setIsPk(0);
        switchColumn.setIsIncrement(0);

        LowcodeFieldSchema switchField = toFieldSchema(switchColumn);
        assertEquals("switch", switchField.getComponentType());
        assertEquals(1, switchField.getDefaultValue());

        GenTableColumn timeColumn = column("signed_at");
        timeColumn.setColumnComment("签约时间");
        timeColumn.setColumnType("datetime");
        timeColumn.setJavaType("LocalDateTime");
        timeColumn.setJavaField("signedAt");
        timeColumn.setColumnDefault("CURRENT_TIMESTAMP");
        timeColumn.setIsRequired(0);
        timeColumn.setIsPk(0);
        timeColumn.setIsIncrement(0);

        LowcodeFieldSchema timeField = toFieldSchema(timeColumn);
        assertEquals("datetime", timeField.getComponentType());
        assertEquals("$forge:now", timeField.getDefaultValue());
    }

    @Test
    @DisplayName("disables audit strategy when no forge audit columns exist")
    void disablesAuditStrategyWithoutAuditColumns() throws Exception {
        LowcodeAuditStrategy strategy = buildAuditStrategy(List.of(column("id"), column("biz_name")));
        assertEquals("NONE", strategy.getMode());
    }

    private LowcodeFieldSchema toFieldSchema(GenTableColumn column) throws Exception {
        LowcodeModelImportService service = new LowcodeModelImportService(null, null, null, null);
        Method method = LowcodeModelImportService.class.getDeclaredMethod("toFieldSchema", GenTableColumn.class);
        method.setAccessible(true);
        return (LowcodeFieldSchema) method.invoke(service, column);
    }

    @SuppressWarnings("unchecked")
    private LowcodeAuditStrategy buildAuditStrategy(List<GenTableColumn> columns) throws Exception {
        LowcodeModelImportService service = new LowcodeModelImportService(null, null, null, null);
        Method method = LowcodeModelImportService.class.getDeclaredMethod("buildAuditStrategy", List.class);
        method.setAccessible(true);
        return (LowcodeAuditStrategy) method.invoke(service, columns);
    }

    private GenTableColumn column(String name) {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(name);
        return column;
    }
}
