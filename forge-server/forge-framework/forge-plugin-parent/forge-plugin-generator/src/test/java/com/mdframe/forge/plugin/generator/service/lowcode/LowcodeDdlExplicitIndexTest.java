package com.mdframe.forge.plugin.generator.service.lowcode;

import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeIndexSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.MySqlRuntimeDatabaseDialect;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.RuntimeDatabaseDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Lowcode explicit index DDL")
class LowcodeDdlExplicitIndexTest {

    @Test
    @DisplayName("does not infer indexes from searchable fields")
    void doesNotInferIndexesFromSearchableFields() throws Exception {
        LowcodeModelSchema schema = schema();

        List<?> definitions = buildIndexDefinitions(schema);

        assertTrue(definitions.isEmpty());
    }

    @Test
    @DisplayName("uses only explicit non-auto index definitions")
    void usesOnlyExplicitIndexes() throws Exception {
        LowcodeModelSchema schema = schema();
        LowcodeIndexSchema legacyAuto = index("idx_auto_order_no", true);
        LowcodeIndexSchema explicit = index("idx_order_no", false);
        schema.setIndexes(List.of(legacyAuto, explicit));

        List<?> definitions = buildIndexDefinitions(schema);

        assertEquals(1, definitions.size());
    }

    @Test
    @DisplayName("keeps imported not null column without requiring a default value")
    void keepsImportedNotNullColumn() throws Exception {
        LowcodeModelSchema schema = schema();
        schema.getFields().get(0).setRequired(true);
        LowcodeDdlRepository.ColumnMetadata metadata = new LowcodeDdlRepository.ColumnMetadata(
                "order_no", "varchar(64)", "NO", null, "", "订单编号", "");
        List<String> ddl = new ArrayList<>();
        LowcodeDdlService service = new LowcodeDdlService(null, null, null, null, null);
        Method method = LowcodeDdlService.class.getDeclaredMethod(
                "appendExistingColumnChanges", String.class, LowcodeModelSchema.class,
                Map.class, List.class, List.class, RuntimeDatabaseDialect.class);
        method.setAccessible(true);

        method.invoke(service, "purchase_order", schema, Map.of("order_no", metadata),
                ddl, new ArrayList<String>(), new MySqlRuntimeDatabaseDialect());

        assertTrue(ddl.isEmpty());
    }

    private List<?> buildIndexDefinitions(LowcodeModelSchema schema) throws Exception {
        LowcodeDdlService service = new LowcodeDdlService(null, null, null, null, null);
        Method method = LowcodeDdlService.class.getDeclaredMethod(
                "buildIndexDefinitions", LowcodeModelSchema.class, List.class, RuntimeDatabaseDialect.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(service, schema, new ArrayList<String>(), new MySqlRuntimeDatabaseDialect());
    }

    private LowcodeModelSchema schema() {
        LowcodeFieldSchema field = new LowcodeFieldSchema();
        field.setField("orderNo");
        field.setColumnName("order_no");
        field.setLabel("订单编号");
        field.setDataType("varchar");
        field.setLength(64);
        field.setSearchable(true);
        field.setSystemField(false);
        LowcodeModelSchema schema = new LowcodeModelSchema();
        schema.setFields(List.of(field));
        return schema;
    }

    private LowcodeIndexSchema index(String name, boolean auto) {
        LowcodeIndexSchema index = new LowcodeIndexSchema();
        index.setIndexName(name);
        index.setFields(List.of("orderNo"));
        index.setAuto(auto);
        return index;
    }
}
