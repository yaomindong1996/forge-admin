package com.mdframe.forge.plugin.generator.service.formula;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.generator.domain.entity.AiFormulaFunctionInstall;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionInstallRequest;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionMarketQueryDTO;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionMarketResponse;
import com.mdframe.forge.plugin.generator.dto.formula.FormulaFunctionResponse;
import com.mdframe.forge.plugin.generator.mapper.FormulaFunctionMapper;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FormulaFunctionMarketService")
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class FormulaFunctionMarketServiceTest {

    @Mock
    private FormulaFunctionMapper formulaFunctionMapper;

    @Test
    @DisplayName("page delegates tenant scoped query")
    void pageDelegatesTenantScopedQuery() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
        FormulaFunctionMarketService service = new FormulaFunctionMarketService(formulaFunctionMapper, registry);
        FormulaFunctionMarketQueryDTO query = new FormulaFunctionMarketQueryDTO();
        query.setPageNum(2);
        query.setPageSize(20);
        query.setKeyword("math");
        query.setCategory("Math");

        try (MockedStatic<SessionHelper> sessionHelper = mockStatic(SessionHelper.class)) {
            sessionHelper.when(SessionHelper::getTenantId).thenReturn(7L);
            when(formulaFunctionMapper.selectFormulaFunctionMarketPage(any(), eq(7L), eq(query)))
                .thenAnswer(invocation -> {
                    Page<FormulaFunctionMarketResponse> page = invocation.getArgument(0);
                    page.setTotal(1);
                    page.setRecords(List.of(function("math.max", true)));
                    return page;
                });

            Page<FormulaFunctionMarketResponse> page = service.page(query);

            assertEquals(2, page.getCurrent());
            assertEquals(20, page.getSize());
            assertEquals(1, page.getTotal());
            assertEquals("math.max", page.getRecords().get(0).getFunctionCode());
        }
    }

    @Test
    @DisplayName("install upserts tenant function install state")
    void installUpsertsTenantFunctionInstallState() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
        FormulaFunctionMarketService service = new FormulaFunctionMarketService(formulaFunctionMapper, registry);
        FormulaFunctionInstallRequest request = new FormulaFunctionInstallRequest();
        request.setFunctionCode("math.max");
        request.setVersion("1.0.0");
        request.setEnabled(true);

        FormulaFunctionMarketResponse before = function("math.max", null);
        FormulaFunctionMarketResponse after = function("math.max", true);
        after.setInstallStatus("INSTALLED");
        after.setInstalledVersion("1.0.0");

        try (MockedStatic<SessionHelper> sessionHelper = mockStatic(SessionHelper.class)) {
            sessionHelper.when(SessionHelper::getTenantId).thenReturn(7L);
            sessionHelper.when(SessionHelper::getUserId).thenReturn(99L);
            sessionHelper.when(SessionHelper::getMainOrgId).thenReturn(88L);
            when(formulaFunctionMapper.selectFormulaFunctionMarketDetail(7L, "math.max"))
                .thenReturn(before, after);

            FormulaFunctionMarketResponse result = service.install(request);

            assertEquals("math.max", result.getFunctionCode());
            assertEquals(Boolean.TRUE, result.getEnabled());
        }

        ArgumentCaptor<AiFormulaFunctionInstall> captor =
            ArgumentCaptor.forClass(AiFormulaFunctionInstall.class);
        verify(formulaFunctionMapper).updateFunctionInstallByUk(captor.capture());

        AiFormulaFunctionInstall install = captor.getValue();
        assertNotNull(install.getId());
        assertEquals(7L, install.getTenantId());
        assertEquals("math.max", install.getFunctionCode());
        assertEquals("1.0.0", install.getInstalledVersion());
        assertEquals("INSTALLED", install.getInstallStatus());
        assertEquals(Boolean.TRUE, install.getEnabled());
        assertEquals(99L, install.getInstalledBy());
        assertEquals(99L, install.getCreateBy());
        assertEquals(88L, install.getCreateDept());
        assertNotNull(install.getInstalledTime());
    }

    @Test
    @DisplayName("availableFunctions returns enabled installed functions only")
    void availableFunctions() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
        FormulaFunctionMarketService service = new FormulaFunctionMarketService(formulaFunctionMapper, registry);
        FormulaFunctionMarketResponse response = function("string.contains", true);
        response.setDescription("判断字符串是否包含子串");
        response.setExample("string.contains('hello','ll') => true");

        try (MockedStatic<SessionHelper> sessionHelper = mockStatic(SessionHelper.class)) {
            sessionHelper.when(SessionHelper::getTenantId).thenReturn(7L);
            when(formulaFunctionMapper.selectEnabledInstalledFunctions(7L))
                .thenReturn(List.of(response));

            List<FormulaFunctionResponse> functions = service.availableFunctions();

            assertEquals(1, functions.size());
            assertEquals("string.contains", functions.get(0).getName());
            assertEquals("String", functions.get(0).getCategory());
            assertEquals("判断字符串是否包含子串", functions.get(0).getDescription());
        }
    }

    @Test
    @DisplayName("validateFunctionReferences rejects disabled market function")
    void validateFunctionReferencesRejectsDisabledMarketFunction() {
        FormulaFunctionRegistry registry = FormulaFunctionRegistry.builtin();
        FormulaFunctionMarketService service = new FormulaFunctionMarketService(formulaFunctionMapper, registry);
        FormulaFunctionMarketResponse disabled = function("math.max", false);
        disabled.setInstallStatus("INSTALLED");

        try (MockedStatic<SessionHelper> sessionHelper = mockStatic(SessionHelper.class)) {
            sessionHelper.when(SessionHelper::getTenantId).thenReturn(7L);
            when(formulaFunctionMapper.selectFormulaFunctionMarketDetail(7L, "math.max"))
                .thenReturn(disabled);

            List<String> errors = service.validateFunctionReferences(
                "math.max(left, right)", List.of("math.max"));

            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).contains("not enabled"));
        }
    }

    private FormulaFunctionMarketResponse function(String functionCode, Boolean enabled) {
        FormulaFunctionMarketResponse response = new FormulaFunctionMarketResponse();
        response.setTenantId(7L);
        response.setFunctionCode(functionCode);
        response.setDisplayName(functionCode);
        response.setCategory(functionCode.startsWith("string.") ? "String" : "Math");
        response.setDescription("description");
        response.setSourceType("BUILTIN");
        response.setReturnType("NUMBER");
        response.setExample(functionCode + "()");
        response.setStatus("ENABLED");
        response.setCurrentVersion("1.0.0");
        response.setLatestVersion("1.0.0");
        response.setBuiltin(true);
        response.setInstallStatus(enabled == null ? null : "INSTALLED");
        response.setInstalledVersion(enabled == null ? null : "1.0.0");
        response.setEnabled(enabled);
        return response;
    }
}
