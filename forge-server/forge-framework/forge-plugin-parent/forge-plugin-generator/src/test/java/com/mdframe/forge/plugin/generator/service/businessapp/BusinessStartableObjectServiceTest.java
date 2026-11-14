package com.mdframe.forge.plugin.generator.service.businessapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessStartableObjectMapper;
import com.mdframe.forge.plugin.generator.service.businessprocess.BusinessProcessRuntimeActionCompiler;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessStartableProcessRow;
import com.mdframe.forge.starter.core.session.SessionHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@DisplayName("BusinessStartableObjectService")
class BusinessStartableObjectServiceTest {

    private final BusinessStartableObjectMapper mapper = mock(BusinessStartableObjectMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BusinessStartableObjectService service = new BusinessStartableObjectService(
            mapper, new BusinessProcessRuntimeActionCompiler(objectMapper), objectMapper);

    @Test
    @DisplayName("catalog appends manual-start business processes after object main flows")
    void appendsManualStartProcesses() {
        BusinessStartableObjectVO mainFlow = new BusinessStartableObjectVO();
        mainFlow.setObjectCode("leave");
        mainFlow.setConfigKey("hr_leave");
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(1L);
            when(mapper.selectStartableObjects(1L)).thenReturn(List.of(mainFlow));
            when(mapper.selectStartableProcesses(1L)).thenReturn(List.of(
                    row("approval_qsxf", "报销审批", schema("START_MANUAL", "手动开始")),
                    row("auto_sync", "自动同步", schema("START_EVENT", "记录新增")),
                    row("broken", "损坏流程", "{not-json")));

            List<BusinessStartableObjectVO> catalog = service.listStartableObjects();

            assertThat(catalog).extracting(BusinessStartableObjectVO::getProcessCode)
                    .containsExactly(null, "approval_qsxf");
            BusinessStartableObjectVO process = catalog.get(1);
            assertThat(process.getProcessName()).isEqualTo("报销审批");
            assertThat(process.getConfigKey()).isEqualTo("hr_apply_kdcp");
            assertThat(process.getApplicationId()).isEqualTo(10L);
        }
    }

    @Test
    @DisplayName("custom start node name becomes the catalog label")
    void usesCustomStartLabel() {
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(1L);
            when(mapper.selectStartableObjects(1L)).thenReturn(null);
            when(mapper.selectStartableProcesses(1L)).thenReturn(List.of(
                    row("approval_qsxf", "报销审批", schema("START_MANUAL", "提交报销"))));

            assertThat(service.listStartableObjects())
                    .extracting(BusinessStartableObjectVO::getProcessName)
                    .containsExactly("提交报销");
        }
    }

    @Test
    @DisplayName("missing tenant returns an empty catalog")
    void missingTenantReturnsEmpty() {
        try (MockedStatic<SessionHelper> session = mockStatic(SessionHelper.class)) {
            session.when(SessionHelper::getTenantId).thenReturn(null);
            assertThat(service.listStartableObjects()).isEmpty();
        }
    }

    private BusinessStartableProcessRow row(String processCode, String processName, String schemaJson) {
        BusinessStartableProcessRow row = new BusinessStartableProcessRow();
        row.setObjectCode("apply_kdcp");
        row.setObjectName("报销申请");
        row.setConfigKey("hr_apply_kdcp");
        row.setApplicationId(10L);
        row.setApplicationCode("hr_e2_e");
        row.setApplicationName("费用报销E2E");
        row.setProcessCode(processCode);
        row.setProcessName(processName);
        row.setSchemaJson(schemaJson);
        return row;
    }

    private String schema(String startType, String startName) {
        return """
                {"schemaVersion":"1.0","processCode":"p",
                 "subject":{"objectCode":"apply_kdcp"},
                 "nodes":[{"id":"start","type":"%s","name":"%s","config":{"positions":["DETAIL"]}}]}
                """.formatted(startType, startName);
    }
}
