package com.mdframe.forge.plugin.generator.service.businessapp;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessAppQueryDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessAppVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationObjectVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessApplicationWorkspaceVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessExtensionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BusinessApplicationWorkspaceService")
class BusinessApplicationWorkspaceServiceTest {

    @Test
    @DisplayName("workspace returns seven lazy section summaries")
    void workspaceReturnsSevenSections() {
        BusinessApplicationWorkspaceService service = new BusinessApplicationWorkspaceService(
                new StubApplicationService(application(2L, 1L, 1L, 0L)),
                new StubApplicationObjectService(List.of(
                        object(201L, "PRIMARY", "PUBLISHED", 1),
                        object(202L, "SHARED", "PUBLISHED", 1))),
                new StubBusinessAppService(List.of(entry(301L, 1))),
                new StubBusinessExtensionService(List.of()),
                null);

        BusinessApplicationWorkspaceVO workspace = service.workspace(101L);

        assertEquals(7, workspace.getSections().size());
        assertEquals(List.of("overview", "objects", "entries", "automation", "enhancements", "permissions", "releases"),
                workspace.getSections().stream().map(item -> item.getSectionKey()).toList());
        assertEquals(0L, workspace.getSections().get(0).getAssetCount());
        assertEquals(2L, workspace.getSections().get(1).getAssetCount());
        assertEquals(2, workspace.getObjects().size());
        assertEquals(1, workspace.getEntries().size());
        assertEquals(0L, workspace.getBlockingCount());
    }

    @Test
    @DisplayName("missing primary blocks while missing entry is only a warning")
    void missingPrimaryBlocksAndEntryWarns() {
        BusinessApplicationWorkspaceService service = new BusinessApplicationWorkspaceService(
                new StubApplicationService(application(1L, 0L, 0L, 0L)),
                new StubApplicationObjectService(List.of(object(201L, "SHARED", "DRAFT", 1))),
                new StubBusinessAppService(List.of()),
                new StubBusinessExtensionService(List.of()),
                null);

        BusinessApplicationWorkspaceVO workspace = service.workspace(101L);

        assertFalse(workspace.getReadiness().getReady());
        assertEquals(1L, workspace.getBlockingCount());
        assertEquals(2L, workspace.getWarningCount());
        assertTrue(workspace.getIssues().stream().anyMatch(item -> "PRIMARY_OBJECT_MISSING".equals(item.getIssueCode())));
        assertTrue(workspace.getIssues().stream().anyMatch(item -> "ACTIVE_ENTRY_MISSING".equals(item.getIssueCode())
                && "WARN".equals(item.getLevel())));
    }

    @Test
    @DisplayName("disabled and unpublished objects are warnings rather than blockers")
    void objectStateIssuesAreWarnings() {
        BusinessApplicationWorkspaceService service = new BusinessApplicationWorkspaceService(
                new StubApplicationService(application(1L, 1L, 0L, 0L)),
                new StubApplicationObjectService(List.of(object(201L, "PRIMARY", "DRAFT", 0))),
                new StubBusinessAppService(List.of(entry(301L, 1))),
                new StubBusinessExtensionService(List.of()),
                null);

        BusinessApplicationWorkspaceVO workspace = service.workspace(101L);

        assertTrue(workspace.getReadiness().getReady());
        assertEquals(0L, workspace.getBlockingCount());
        assertEquals(2L, workspace.getWarningCount());
    }

    private static BusinessApplicationVO application(Long objectCount, Long entryCount,
                                                     Long flowCount, Long extensionCount) {
        BusinessApplicationVO application = new BusinessApplicationVO();
        application.setId(101L);
        application.setApplicationCode("crm_center");
        application.setApplicationName("客户经营");
        application.setSuiteCode("crm");
        application.setSuiteName("客户管理");
        application.setStatus(1);
        application.setDesignStatus("DRAFT");
        application.setObjectCount(objectCount);
        application.setEntryCount(entryCount);
        application.setFlowCount(flowCount);
        application.setExtensionCount(extensionCount);
        return application;
    }

    private static BusinessApplicationObjectVO object(Long id, String role, String designStatus, int status) {
        BusinessApplicationObjectVO object = new BusinessApplicationObjectVO();
        object.setObjectId(id);
        object.setObjectCode("customer_" + id);
        object.setObjectName("客户" + id);
        object.setObjectRole(role);
        object.setDesignStatus(designStatus);
        object.setObjectStatus(status);
        return object;
    }

    private static BusinessAppVO entry(Long id, int status) {
        BusinessAppVO entry = new BusinessAppVO();
        entry.setId(id);
        entry.setAppCode("CRM_CUSTOMER_RUNTIME");
        entry.setAppName("客户业务页面");
        entry.setEntryMode("RUNTIME");
        entry.setStatus(status);
        return entry;
    }

    private static class StubApplicationService extends BusinessApplicationService {

        private final BusinessApplicationVO application;

        StubApplicationService(BusinessApplicationVO application) {
            super(null, null, null);
            this.application = application;
        }

        @Override
        public BusinessApplicationVO detail(Long id) {
            return application;
        }
    }

    private static class StubApplicationObjectService extends BusinessApplicationObjectService {

        private final List<BusinessApplicationObjectVO> objects;

        StubApplicationObjectService(List<BusinessApplicationObjectVO> objects) {
            super(null, null);
            this.objects = objects;
        }

        @Override
        public List<BusinessApplicationObjectVO> list(Long applicationId) {
            return objects;
        }
    }

    private static class StubBusinessAppService extends BusinessAppService {

        private final List<BusinessAppVO> entries;

        StubBusinessAppService(List<BusinessAppVO> entries) {
            super(null, null, null, null, null);
            this.entries = entries;
        }

        @Override
        public List<BusinessAppVO> list(BusinessAppQueryDTO query) {
            return entries;
        }
    }

    private static class StubBusinessExtensionService extends BusinessExtensionService {

        private final List<BusinessExtensionVO> extensions;

        StubBusinessExtensionService(List<BusinessExtensionVO> extensions) {
            super(null, null, null);
            this.extensions = extensions;
        }

        @Override
        public List<BusinessExtensionVO> listWorkspaceSummaries(Long applicationId) {
            return extensions;
        }
    }
}
