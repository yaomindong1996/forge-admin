package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintPrepareAuthorizationTest extends PrintServiceFixture {

    @Test
    void flowProviderMustResolveTheAuthorizedBusinessRun() {
        published();
        var unresolved = new PrintRecordRequest(source, "synthetic_record", PrintScene.FLOW_TODO, "task", "instance", null);
        fails(403, () -> runtime.available(new PrintAvailableTemplatesDTO(unresolved)));
        var resolved = new PrintRecordRequest(source, "synthetic_record", PrintScene.FLOW_TODO, "task", "instance", 12L);
        assertThat(runtime.available(new PrintAvailableTemplatesDTO(resolved))).hasSize(1);
    }

    @Test
    void runtimeNeedsNoDesignPermissionAndPinsPublishedApplicationVersion() throws Exception {
        var row = published();
        var changed = service.update(row.id(), new PrintTemplateUpdateDTO(row.draftRevision(), "最新草稿", schema.replace("合成采购单", "新发布版本")));
        publish(changed);
        permissions.clear();
        permissions.add("print:execute");
        adapter.designDenied = true;
        var result = runtime.prepare(new PrintPrepareDTO(record(), row.id()));
        assertThat(result.templateVersionId()).isEqualTo(row.publishedVersionId());
        assertThat(json.writeValueAsString(result)).contains("合成采购单", "SYN-001").doesNotContain("新发布版本", "MUST_NOT_LEAK");
        assertThat(result.applicationVersionId()).isEqualTo(11L);
        assertThat(result.dataMode()).isEqualTo("CURRENT");
        assertThat(jdbc.queryForObject("SELECT result FROM sys_print_execution WHERE id=?", String.class, result.executionId())).isEqualTo("PREPARED");
    }

    @Test
    void recordSourceActorAndTemplateSpoofingAreDeniedBeforeDataReturns() {
        var row = published();
        var other = create("other");
        fails(403, () -> runtime.prepare(new PrintPrepareDTO(record(), other.id())));
        adapter.denied = true;
        fails(403, () -> runtime.prepare(new PrintPrepareDTO(record(), row.id())));
        adapter.denied = false;
        adapter.spoofed = true;
        fails(403, () -> runtime.prepare(new PrintPrepareDTO(record(), row.id())));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_print_execution", Integer.class)).isZero();
    }

    @Test
    void stoppedTemplateImmediatelyDisappearsAndOldVersionsCannotPrepare() {
        var row = published();
        service.status(row.id(), new PrintTemplateStatusDTO(row.draftRevision(), 0));
        assertThat(runtime.available(new PrintAvailableTemplatesDTO(record()))).isEmpty();
        fails(404, () -> runtime.prepare(new PrintPrepareDTO(record(), row.id())));
    }

    @Test
    void resourcesAndOversizedDataRejectWithoutExecutionAudit() {
        var row = published();
        adapter.resourceDenied = true;
        fails(403, () -> runtime.prepare(new PrintPrepareDTO(record(), row.id())));
        adapter.resourceDenied = false;
        adapter.data = new PrintData(Map.of("remark", "x".repeat(100001)), Map.of(), Map.of());
        fails(400, () -> runtime.prepare(new PrintPrepareDTO(record(), row.id())));
        adapter.data = new PrintData(Map.of(), Map.of("items", Collections.nCopies(501, Map.of("name", "合成"))), Map.of());
        fails(400, () -> runtime.prepare(new PrintPrepareDTO(record(), row.id())));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_print_execution", Integer.class)).isZero();
    }

    @Test
    void catalogRejectsMixedDesignRuntimeAndFlowContextIsExplicit() {
        fails(400, () -> runtime.catalog(new PrintCatalogQueryDTO(source, record())));
        var invalid = new PrintRecordRequest(source, "synthetic_record", PrintScene.FLOW_TODO, null, "instance", null);
        fails(400, () -> runtime.available(new PrintAvailableTemplatesDTO(invalid)));
        permissions.clear();
        permissions.add("print:execute");
        fails(403, () -> runtime.catalog(new PrintCatalogQueryDTO(source, null)));
        assertThat(runtime.catalog(new PrintCatalogQueryDTO(null, record())).fields()).hasSize(6);
    }

    @Test
    void corruptVersionHashFailsClosed() {
        var row = published();
        jdbc.update("UPDATE sys_print_template_version SET schema_hash=? WHERE id=?", "0".repeat(64), row.publishedVersionId());
        fails(409, () -> runtime.prepare(new PrintPrepareDTO(record(), row.id())));
    }
}
