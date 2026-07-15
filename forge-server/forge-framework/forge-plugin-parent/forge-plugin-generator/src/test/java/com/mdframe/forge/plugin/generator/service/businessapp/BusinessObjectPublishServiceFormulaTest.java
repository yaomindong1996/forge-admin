package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.constant.BusinessPublishCheckLevel;
import com.mdframe.forge.plugin.generator.domain.entity.AiBusinessObject;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPermissionSummaryVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentConfigVO;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeFieldSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodeModelSchema;
import com.mdframe.forge.plugin.generator.dto.lowcode.LowcodePageSchema;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessAppMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessTriggerMapper;
import com.mdframe.forge.plugin.generator.service.formula.CrossObjectRecomputeTaskService;
import com.mdframe.forge.plugin.generator.service.formula.FormulaPublishValidator;
import com.mdframe.forge.plugin.generator.service.formula.FormulaValidationService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeDdlService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodePublishService;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeRuntimeConfigBuilder;
import com.mdframe.forge.plugin.generator.service.lowcode.LowcodeSchemaValidator;
import com.mdframe.forge.plugin.generator.service.lowcode.runtime.LowcodeRuntimeDataSourceResolver;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckItemVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessPublishCheckVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@DisplayName("BusinessObjectPublishService - Formula Integration")
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class BusinessObjectPublishServiceFormulaTest {

    @Mock private BusinessObjectDesignerService designerService;
    @Mock private BusinessObjectDesignVersionService designVersionService;
    @Mock private LowcodePublishService lowcodePublishService;
    @Mock private LowcodeRuntimeConfigBuilder runtimeConfigBuilder;
    @Mock private LowcodeSchemaValidator schemaValidator;
    @Mock private LowcodeDdlService ddlService;
    @Mock private LowcodeRuntimeDataSourceResolver runtimeDataSourceResolver;
    @Mock private AiCrudConfigMapper crudConfigMapper;
    @Mock private BusinessAppMapper businessAppMapper;
    @Mock private BusinessObjectMapper businessObjectMapper;
    @Mock private BusinessTriggerMapper triggerMapper;
    @Mock private BusinessDocumentConfigService documentConfigService;
    @Mock private BusinessPermissionService permissionService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FormulaPublishValidator realValidator;
    private BusinessObjectPublishService service;

    @BeforeEach
    void setUp() {
        realValidator = new FormulaPublishValidator(new FormulaValidationService(), objectMapper);
        service = new BusinessObjectPublishService(
                designerService, designVersionService, lowcodePublishService,
                runtimeConfigBuilder, schemaValidator, realValidator,
                new CrossObjectRecomputeTaskService(), ddlService,
                runtimeDataSourceResolver,
                crudConfigMapper, businessAppMapper, businessObjectMapper,
                triggerMapper, documentConfigService, permissionService, objectMapper);
        // Stub document config to disabled so checkDocumentConfig skips early
        BusinessDocumentConfigVO docConfig = new BusinessDocumentConfigVO();
        docConfig.setDocumentEnabled(false);
        when(documentConfigService.getConfig(anyLong())).thenReturn(docConfig);
        // Stub permission summary to return empty (skip permission checks)
        BusinessPermissionSummaryVO permSummary = new BusinessPermissionSummaryVO();
        permSummary.setActionPermissions(java.util.Collections.emptyList());
        when(permissionService.documentActionSummary(any(AiBusinessObject.class))).thenReturn(permSummary);
    }

    private LowcodeFieldSchema plainField(String name) {
        LowcodeFieldSchema f = new LowcodeFieldSchema();
        f.setField(name);
        f.setLabel(name);
        return f;
    }

    private LowcodeFieldSchema formulaField(String name, Map<String, Object> formulaConfig) {
        LowcodeFieldSchema f = new LowcodeFieldSchema();
        f.setField(name);
        f.setLabel(name);
        f.setFormulaConfig(formulaConfig);
        return f;
    }

    private BusinessObjectDesignerService.DesignerContext mockContext(LowcodeModelSchema modelSchema) {
        BusinessObjectDesignerService.DesignerContext ctx =
                new BusinessObjectDesignerService.DesignerContext();
        ctx.setModelSchema(modelSchema);
        ctx.setPageSchema(new LowcodePageSchema());
        AiBusinessObject obj = new AiBusinessObject();
        obj.setId(1L);
        obj.setSuiteCode("test");
        obj.setObjectCode("test_obj");
        ctx.setObject(obj);
        return ctx;
    }

    @Nested
    @DisplayName("checkFormula integration")
    class CheckFormula {

        @Test
        @DisplayName("no formula fields: FORMULA_PASS item present")
        void noFormulaFields() {
            LowcodeModelSchema schema = new LowcodeModelSchema();
            schema.setFields(List.of(plainField("name"), plainField("price")));

            when(designerService.loadContext(anyLong())).thenReturn(mockContext(schema));
            when(runtimeConfigBuilder.buildRuntimeConfig(any(), any(), any())).thenReturn(null);

            BusinessPublishCheckVO result = service.publishCheck(1L);
            assertNotNull(result);

            List<BusinessPublishCheckItemVO> formulaItems = result.getPassItems().stream()
                    .filter(i -> "FORMULA".equals(i.getCategory()))
                    .toList();
            assertFalse(formulaItems.isEmpty(), "Should have FORMULA_PASS item");
            assertTrue(formulaItems.stream().anyMatch(i -> "FORMULA_PASS".equals(i.getItemCode())),
                    "Should have FORMULA_PASS when no formula fields");
        }

        @Test
        @DisplayName("valid CALC formula: FORMULA_PASS item present")
        void validCalcFormula() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC");
            fc.put("mode", "STORED");
            fc.put("expression", "price * quantity");
            fc.put("dependsOn", List.of("price", "quantity"));

            LowcodeModelSchema schema = new LowcodeModelSchema();
            schema.setFields(List.of(
                    plainField("price"), plainField("quantity"),
                    formulaField("total", fc)));

            when(designerService.loadContext(anyLong())).thenReturn(mockContext(schema));
            when(runtimeConfigBuilder.buildRuntimeConfig(any(), any(), any())).thenReturn(null);

            BusinessPublishCheckVO result = service.publishCheck(1L);
            assertNotNull(result);
            assertTrue(result.getPublishable() || result.getBlockItems().stream()
                    .noneMatch(i -> "FORMULA".equals(i.getCategory())),
                    "Valid formula should not produce BLOCK items");
        }

        @Test
        @DisplayName("syntax error in formula: FORMULA_ERROR BLOCK item present")
        void syntaxErrorFormula() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC");
            fc.put("mode", "STORED");
            fc.put("expression", "price * ");
            fc.put("dependsOn", List.of("price"));

            LowcodeModelSchema schema = new LowcodeModelSchema();
            schema.setFields(List.of(plainField("price"), formulaField("total", fc)));

            when(designerService.loadContext(anyLong())).thenReturn(mockContext(schema));
            when(runtimeConfigBuilder.buildRuntimeConfig(any(), any(), any())).thenReturn(null);

            BusinessPublishCheckVO result = service.publishCheck(1L);
            assertNotNull(result);

            List<BusinessPublishCheckItemVO> blockFormulaItems = result.getBlockItems().stream()
                    .filter(i -> "FORMULA".equals(i.getCategory()))
                    .toList();
            assertFalse(blockFormulaItems.isEmpty(),
                    "Syntax error should produce FORMULA BLOCK items");
            assertTrue(blockFormulaItems.stream().anyMatch(i -> "FORMULA_ERROR".equals(i.getItemCode())),
                    "Should have FORMULA_ERROR item");
        }

        @Test
        @DisplayName("missing dependency field: FORMULA_ERROR BLOCK item present")
        void missingDependencyField() {
            Map<String, Object> fc = new LinkedHashMap<>();
            fc.put("type", "CALC");
            fc.put("mode", "STORED");
            fc.put("expression", "price * quantity");
            fc.put("dependsOn", List.of("price", "quantity"));

            LowcodeModelSchema schema = new LowcodeModelSchema();
            schema.setFields(List.of(plainField("price"), formulaField("total", fc)));

            when(designerService.loadContext(anyLong())).thenReturn(mockContext(schema));
            when(runtimeConfigBuilder.buildRuntimeConfig(any(), any(), any())).thenReturn(null);

            BusinessPublishCheckVO result = service.publishCheck(1L);
            assertNotNull(result);

            List<BusinessPublishCheckItemVO> blockFormulaItems = result.getBlockItems().stream()
                    .filter(i -> "FORMULA".equals(i.getCategory()))
                    .toList();
            assertFalse(blockFormulaItems.isEmpty(),
                    "Missing dependency should produce FORMULA BLOCK items");
        }
    }
}
