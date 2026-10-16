package com.mdframe.forge.plugin.generator.service.formula;

import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObjectRelation;
import com.mdframe.forge.plugin.generator.domain.entity.AiLowcodeModel;
import com.mdframe.forge.plugin.generator.domain.formula.AggregateDataException;
import com.mdframe.forge.plugin.generator.domain.formula.FormulaRuntimeContext;
import com.mdframe.forge.plugin.generator.mapper.AiLowcodeModelMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectRelationMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@DisplayName("DbAggregateDataProvider")
@Tag("dev")
class DbAggregateDataProviderTest {

    private BusinessObjectRelationMapper relationMapper;
    private BusinessObjectMapper businessObjectMapper;
    private AiLowcodeModelMapper lowcodeModelMapper;
    private DetailRecordFetcher detailRecordFetcher;
    private DbAggregateDataProvider provider;

    @BeforeEach
    void setUp() {
        relationMapper = Mockito.mock(BusinessObjectRelationMapper.class);
        businessObjectMapper = Mockito.mock(BusinessObjectMapper.class);
        lowcodeModelMapper = Mockito.mock(AiLowcodeModelMapper.class);
        detailRecordFetcher = Mockito.mock(DetailRecordFetcher.class);
        provider = new DbAggregateDataProvider(relationMapper, businessObjectMapper,
            lowcodeModelMapper, detailRecordFetcher, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    // ---- helpers ----
    private Map<String, Object> ctx(Long tenantId, String suiteCode, String sourceObjectCode) {
        return ctx(tenantId, suiteCode, sourceObjectCode, "masterId", 100L);
    }

    private Map<String, Object> ctx(Long tenantId, String suiteCode, String sourceObjectCode,
                                     String joinField, Object joinValue) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 100L);
        row.put(joinField, joinValue);
        FormulaRuntimeContext rc = new FormulaRuntimeContext(tenantId, suiteCode, sourceObjectCode, row);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("__formulaRuntimeContext__", rc);
        map.put("tenantId", tenantId);
        map.put("suiteCode", suiteCode);
        map.put("sourceObjectCode", sourceObjectCode);
        map.put("id", 100L);
        map.put(joinField, joinValue);
        return map;
    }

    private AiBusinessObjectRelation relation(Long id, String src, String tgt, String srcField, String tgtField) {
        AiBusinessObjectRelation r = new AiBusinessObjectRelation();
        r.setId(id);
        r.setSuiteCode("default");
        r.setSourceObjectCode(src);
        r.setTargetObjectCode(tgt);
        r.setSourceFieldCode(srcField);
        r.setTargetFieldCode(tgtField);
        r.setRelationType("DETAIL");
        return r;
    }

    private AiBusinessObject targetObject(String objectCode, String modelCode, Long modelId) {
        AiBusinessObject obj = new AiBusinessObject();
        obj.setId(200L);
        obj.setSuiteCode("default");
        obj.setObjectCode(objectCode);
        obj.setObjectName("Target");
        obj.setModelCode(modelCode);
        obj.setModelId(modelId);
        return obj;
    }

    private AiLowcodeModel model(Long id, String modelCode, String tableName) {
        AiLowcodeModel m = new AiLowcodeModel();
        m.setId(id);
        m.setModelCode(modelCode);
        m.setModelSchema("{\"tableName\":\"" + tableName + "\"}");
        return m;
    }

    // ---- tests ----

    @Nested
    @DisplayName("invalid inputs")
    class InvalidInputs {
        @Test
        @DisplayName("blank relationCode returns empty")
        void blankRelationCode() {
            assertThrows(AggregateDataException.class,
                () -> provider.getDetailRecords("  ", ctx(1L, "s", "master")));
        }

        @Test
        @DisplayName("null relationCode returns empty")
        void nullRelationCode() {
            assertThrows(AggregateDataException.class,
                () -> provider.getDetailRecords(null, ctx(1L, "s", "master")));
        }

        @Test
        @DisplayName("non-numeric relationCode returns empty")
        void nonNumericRelationCode() {
            assertThrows(AggregateDataException.class,
                () -> provider.getDetailRecords("abc", ctx(1L, "s", "master")));
        }

        @Test
        @DisplayName("null context throws")
        void nullContext() {
            assertThrows(NullPointerException.class,
                () -> provider.getDetailRecords("123", null));
        }
    }

    @Nested
    @DisplayName("relationCode -> relation mapping")
    class RelationMapping {
        @Test
        @DisplayName("relation not found returns empty")
        void relationNotFound() {
            assertThrows(AggregateDataException.class,
                () -> provider.getDetailRecords("999", ctx(1L, "default", "master")));
        }

        @Test
        @DisplayName("sourceObjectCode mismatch returns empty")
        void sourceMismatch() {
            assertThrows(AggregateDataException.class,
                () -> provider.getDetailRecords("10", ctx(1L, "default", "master")));
        }

        @Test
        @DisplayName("target object not found returns empty")
        void targetNotFound() {
            assertThrows(AggregateDataException.class,
                () -> provider.getDetailRecords("10", ctx(1L, "default", "master")));
        }
    }

    @Nested
    @DisplayName("table name resolution")
    class TableResolution {
        @Test
        @DisplayName("resolves table name via modelCode")
        void viaModelCode() {
            AiBusinessObjectRelation rel = relation(10L, "master", "detail", "masterId", "id");
            AiBusinessObject target = targetObject("detail", "M001", 300L);
            AiLowcodeModel mdl = model(300L, "M001", "ai_detail");

            Mockito.when(relationMapper.selectRelationById(eq(1L), eq(10L))).thenReturn(rel);
            Mockito.when(businessObjectMapper.selectByObjectCode(eq(1L), eq("default"), eq("detail")))
                .thenReturn(target);
            Mockito.when(lowcodeModelMapper.selectByModelCode(eq(1L), eq("M001"))).thenReturn(mdl);

            List<Map<String, Object>> fakeRows = List.of(Map.of("amount", 50L));
            Mockito.when(detailRecordFetcher.fetchDetailRecords(
                eq("ai_detail"), eq("id"), eq(100L), eq(1L)))
                .thenReturn(fakeRows);

            // relation sourceFieldCode="masterId", ctx row has masterId=100L
            List<Map<String, Object>> result = provider.getDetailRecords("10",
                ctx(1L, "default", "master", "masterId", 100L));
            assertEquals(1, result.size());
            assertEquals(50L, result.get(0).get("amount"));
        }

        @Test
        @DisplayName("resolves table name via modelId fallback")
        void viaModelIdFallback() {
            AiBusinessObjectRelation rel = relation(10L, "master", "detail", "masterId", "id");
            AiBusinessObject target = targetObject("detail", null, 300L);
            target.setModelCode(null);
            AiLowcodeModel mdl = model(300L, "M002", "ai_detail_v2");

            Mockito.when(relationMapper.selectRelationById(eq(1L), eq(10L))).thenReturn(rel);
            Mockito.when(businessObjectMapper.selectByObjectCode(eq(1L), eq("default"), eq("detail")))
                .thenReturn(target);
            Mockito.when(lowcodeModelMapper.selectById(eq(300L))).thenReturn(mdl);

            List<Map<String, Object>> fakeRows = List.of(Map.of("amount", 75L));
            Mockito.when(detailRecordFetcher.fetchDetailRecords(
                eq("ai_detail_v2"), eq("id"), eq(100L), eq(1L)))
                .thenReturn(fakeRows);

            List<Map<String, Object>> result = provider.getDetailRecords("10",
                ctx(1L, "default", "master", "masterId", 100L));
            assertEquals(1, result.size());
            assertEquals(75L, result.get(0).get("amount"));
        }

        @Test
        @DisplayName("no model found returns empty")
        void noModel() {
            assertThrows(AggregateDataException.class,
                () -> provider.getDetailRecords("10", ctx(1L, "default", "master")));
        }
    }

    @Nested
    @DisplayName("context extraction fallback")
    class ContextFallback {
        @Test
        @DisplayName("without FormulaRuntimeContext key, extracts from individual keys")
        void withoutContextKey() {
            AiBusinessObjectRelation rel = relation(10L, "master", "detail", "masterId", "id");
            AiBusinessObject target = targetObject("detail", "M001", 300L);
            AiLowcodeModel mdl = model(300L, "M001", "ai_detail");

            Mockito.when(relationMapper.selectRelationById(eq(1L), eq(10L))).thenReturn(rel);
            Mockito.when(businessObjectMapper.selectByObjectCode(eq(1L), eq("default"), eq("detail")))
                .thenReturn(target);
            Mockito.when(lowcodeModelMapper.selectByModelCode(eq(1L), eq("M001"))).thenReturn(mdl);

            List<Map<String, Object>> fakeRows = List.of(Map.of("amount", 30L));
            Mockito.when(detailRecordFetcher.fetchDetailRecords(
                eq("ai_detail"), any(), any(), eq(1L)))
                .thenReturn(fakeRows);

            // context without __formulaRuntimeContext__ key
            Map<String, Object> plainCtx = new LinkedHashMap<>();
            plainCtx.put("tenantId", 1);
            plainCtx.put("suiteCode", "default");
            plainCtx.put("sourceObjectCode", "master");
            plainCtx.put("masterId", 100L);

            List<Map<String, Object>> result = provider.getDetailRecords("10", plainCtx);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("end-to-end: relationCode -> targetObjectCode chain")
    class EndToEnd {
        @Test
        @DisplayName("correct chain: Long.parseLong -> selectRelationById -> selectByObjectCode -> fetch")
        void fullChain() {
            // Given: relationCode="42", relation maps order->orderItem via orderId=id
            AiBusinessObjectRelation rel = relation(42L, "order", "orderItem", "orderId", "id");
            AiBusinessObject target = targetObject("orderItem", "M_ITEM", 500L);
            AiLowcodeModel mdl = model(500L, "M_ITEM", "biz_order_item");

            Mockito.when(relationMapper.selectRelationById(eq(1L), eq(42L))).thenReturn(rel);
            Mockito.when(businessObjectMapper.selectByObjectCode(eq(1L), eq("default"), eq("orderItem")))
                .thenReturn(target);
            Mockito.when(lowcodeModelMapper.selectByModelCode(eq(1L), eq("M_ITEM"))).thenReturn(mdl);

            List<Map<String, Object>> detailRows = List.of(
                Map.of("id", 1L, "amount", 100L),
                Map.of("id", 2L, "amount", 200L)
            );
            Mockito.when(detailRecordFetcher.fetchDetailRecords(
                eq("biz_order_item"), eq("id"), eq(100L), eq(1L)))
                .thenReturn(detailRows);

            // When: ctx has orderId=100L (matches relation.sourceFieldCode)
            List<Map<String, Object>> result = provider.getDetailRecords("42",
                ctx(1L, "default", "order", "orderId", 100L));

            // Then
            assertEquals(2, result.size());
            assertEquals(100L, result.get(0).get("amount"));
            assertEquals(200L, result.get(1).get("amount"));
        }
    }
}
