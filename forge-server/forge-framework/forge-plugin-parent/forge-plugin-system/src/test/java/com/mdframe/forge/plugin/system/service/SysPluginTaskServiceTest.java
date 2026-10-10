package com.mdframe.forge.plugin.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.system.dto.SysPluginTaskCommandDTO;
import com.mdframe.forge.plugin.system.dto.SysPluginUploadDTO;
import com.mdframe.forge.plugin.system.entity.SysPluginTask;
import com.mdframe.forge.plugin.system.enums.PluginTaskStatus;
import com.mdframe.forge.plugin.system.mapper.SysPluginTaskMapper;
import com.mdframe.forge.plugin.system.service.plugin.*;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.plugin.catalog.PluginOrigin;
import com.mdframe.forge.starter.plugin.catalog.RuntimePlugin;
import com.mdframe.forge.starter.plugin.catalog.RuntimePluginCatalog;
import com.mdframe.forge.starter.plugin.delivery.PackageDigests;
import com.mdframe.forge.starter.plugin.delivery.SourcePluginPackage;
import com.mdframe.forge.starter.plugin.descriptor.PluginDescriptor;
import com.mdframe.forge.starter.plugin.descriptor.PluginEdition;
import com.mdframe.forge.starter.plugin.feature.CommunityFeatureGate;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SysPluginTaskServiceTest {
    private static final byte[] ZIP = "fixture archive".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private final PluginTaskActor actor = new PluginTaskActor(1L, 9L, 1L);
    private final SysPluginTaskMapper mapper = mock(SysPluginTaskMapper.class);
    private final PluginTaskRepository repository = new PluginTaskRepository(mapper);
    private final PluginPackagePreflight preflight = mock(PluginPackagePreflight.class);
    private final RuntimePluginCatalog catalog = mock(RuntimePluginCatalog.class);
    private final PluginPreviewPlanner planner = new PluginPreviewPlanner(catalog, new CommunityFeatureGate());
    private final PluginTaskViews views = new PluginTaskViews(new ObjectMapper().findAndRegisterModules());

    @Test
    void upload_freezes_preview_and_private_bytes_not_vo() throws Exception {
        sourceFixture();
        SysPluginUploadDTO dto = uploadDTO();
        when(preflight.fileName(dto.getFile())).thenReturn("demo.zip");
        when(preflight.read(dto.getFile())).thenReturn(ZIP);
        var service = new PluginTaskUploadService(repository, preflight, planner, views);
        var result = service.upload(dto, actor);
        assertThat(result.status()).isEqualTo(PluginTaskStatus.AWAIT_CONFIRMATION.getCode());
        assertThat(result.preview().warnings()).isNotEmpty();
        var captor = org.mockito.ArgumentCaptor.forClass(SysPluginTask.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getArchiveData()).isEqualTo(ZIP);
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getCreateBy()).isEqualTo(9L);
        assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(result))
                .doesNotContain("archiveData", "previewJson", "fixture archive");
    }

    @Test
    void same_request_same_bytes_returns_existing_but_changed_bytes_rejected() {
        sourceFixture();
        SysPluginTask task = task();
        var dto = uploadDTO();
        when(mapper.selectByRequest(1L, 9L, dto.getRequestId())).thenReturn(task);
        when(preflight.fileName(dto.getFile())).thenReturn("demo.zip");
        when(preflight.read(dto.getFile())).thenReturn(ZIP);
        var service = new PluginTaskUploadService(repository, preflight, planner, views);
        assertThat(service.upload(dto, actor).id()).isEqualTo("task");
        verify(preflight, never()).inspect(any());
        when(preflight.read(dto.getFile())).thenReturn(new byte[]{1});
        assertThatThrownBy(() -> service.upload(dto, actor)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能替换包内容");
        verify(mapper, never()).insert(any(SysPluginTask.class));
    }

    @Test
    void concurrent_same_request_recovers_existing_without_duplicate_task() {
        sourceFixture();
        var dto = uploadDTO();
        when(preflight.fileName(dto.getFile())).thenReturn("demo.zip");
        when(preflight.read(dto.getFile())).thenReturn(ZIP);
        SysPluginTask existing = task();
        when(mapper.selectByRequest(1L, 9L, dto.getRequestId())).thenReturn(null, existing);
        doThrow(new DuplicateKeyException("fixture request race")).when(mapper).insert(any(SysPluginTask.class));
        var service = new PluginTaskUploadService(repository, preflight, planner, views);
        assertThat(service.upload(dto, actor).id()).isEqualTo("task");
    }

    @Test
    void confirm_only_queues_and_repeated_confirm_does_not_transition_twice() {
        sourceFixture();
        SysPluginTask task = task();
        when(mapper.selectTask(1L, "task")).thenReturn(task);
        when(mapper.selectArchive(1L, "task")).thenReturn(archive(ZIP));
        when(mapper.transition(any(), anyString(), anyInt())).thenReturn(1);
        var service = commands();
        var result = service.confirm("task", command(0), actor);
        assertThat(result.status()).isEqualTo(PluginTaskStatus.QUEUED.getCode());
        assertThat(result.revision()).isEqualTo(1);
        assertThat(task.getConfirmedBy()).isEqualTo(9L);
        assertThat(service.confirm("task", command(0), actor).revision()).isEqualTo(1);
        verify(mapper, times(1)).transition(any(), anyString(), anyInt());
    }

    @Test
    void reject_stale_revision_wrong_hash_changed_runtime_and_corrupted_blob() {
        sourceFixture();
        SysPluginTask task = task();
        when(mapper.selectTask(1L, "task")).thenReturn(task);
        assertThatThrownBy(() -> commands().confirm("task", command(10), actor))
                .isInstanceOf(BusinessException.class).hasMessageContaining("过期");
        var wrong = command(0);
        wrong.setSha256("0".repeat(64));
        assertThatThrownBy(() -> commands().confirm("task", wrong, actor)).hasMessageContaining("摘要");
        task.setRuntimeSnapshot("changed");
        assertThatThrownBy(() -> commands().confirm("task", command(0), actor)).hasMessageContaining("运行清单已变化");
        task.setRuntimeSnapshot(planner.snapshot());
        when(mapper.selectArchive(1L, "task")).thenReturn(archive(new byte[]{1}));
        assertThatThrownBy(() -> commands().confirm("task", command(0), actor)).hasMessageContaining("摘要异常");
        verify(mapper, never()).transition(any(), anyString(), anyInt());
    }

    @Test
    void cas_rejects_concurrent_update_and_cancel_releases_active_slot() {
        sourceFixture();
        var task = task();
        when(mapper.selectTask(1L, "task")).thenReturn(task);
        assertThatThrownBy(() -> commands().cancel("task", command(0), actor)).hasMessageContaining("其它请求修改");
        task = task();
        when(mapper.selectTask(1L, "task")).thenReturn(task);
        when(mapper.transition(any(), anyString(), anyInt())).thenReturn(1);
        assertThat(commands().cancel("task", command(0), actor).status()).isEqualTo("cancelled");
        assertThat(task.getActivePluginId()).isNull();
        assertThat(task.getCancelledBy()).isEqualTo(9L);
    }

    @Test
    void tenant_context_does_not_read_another_tenants_task() {
        assertThatThrownBy(() -> commands().cancel("task", command(0), new PluginTaskActor(2L, 9L, 1L)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不存在或不属于当前租户");
        verify(mapper).selectTask(2L, "task");
        verify(mapper, never()).transition(any(), anyString(), anyInt());
    }

    @Test
    void planner_blocks_core_replacement_downgrade_and_enterprise() {
        sourceFixture();
        var builtin = new RuntimePlugin("demo", "内置", "2.0.0", PluginOrigin.BUILTIN,
                PluginEdition.COMMUNITY, null, "forge-plugin-demo", false, List.of());
        when(catalog.findById("demo")).thenReturn(java.util.Optional.of(builtin));
        assertThat(planner.preview(source()).blockers()).hasSize(2);
        var metadata = new PluginDescriptor("demo", "商业", "1.0.0", PluginEdition.ENTERPRISE,
                ">=1.2.0", List.of("ee.demo"), null, new PluginDescriptor.Ui("ui"));
        var enterprise = new SourcePluginPackage(metadata, "hash", 1, 1, 1, List.of());
        assertThat(planner.preview(enterprise).blockers()).anyMatch(value -> value.contains("独立 Pro"));
    }

    private void sourceFixture() {
        when(catalog.getPlugins()).thenReturn(List.of());
        when(catalog.findById(anyString())).thenReturn(java.util.Optional.empty());
        when(preflight.inspect(ZIP)).thenReturn(source());
    }

    private SourcePluginPackage source() {
        var metadata = new PluginDescriptor("demo", "测试", "1.0.0", PluginEdition.COMMUNITY,
                ">=1.2.0 <2.0.0", List.of("community.demo"), null, new PluginDescriptor.Ui("ui"));
        return new SourcePluginPackage(metadata, PackageDigests.sha256(ZIP), ZIP.length, 1, 1, List.of());
    }

    private SysPluginTask task() {
        SysPluginTask task = new SysPluginTask();
        task.setId("task");
        task.setTenantId(1L);
        task.setTaskStatus(PluginTaskStatus.AWAIT_CONFIRMATION.getCode());
        task.setActivePluginId("demo");
        task.setRevision(0);
        task.setArchiveSha256(PackageDigests.sha256(ZIP));
        task.setArchiveBytes(ZIP.length);
        task.setPreviewJson(views.encode(planner.preview(source())));
        task.setRuntimeSnapshot(planner.snapshot());
        return task;
    }

    private SysPluginTaskCommandDTO command(int revision) {
        var dto = new SysPluginTaskCommandDTO();
        dto.setRevision(revision);
        dto.setSha256(PackageDigests.sha256(ZIP));
        return dto;
    }

    private SysPluginUploadDTO uploadDTO() {
        var dto = new SysPluginUploadDTO();
        dto.setRequestId("00000000-0000-4000-8000-000000000000");
        dto.setFile(new MockMultipartFile("file", "demo.zip", "application/zip", ZIP));
        return dto;
    }

    private PluginTaskCommandService commands() {
        return new PluginTaskCommandService(repository, mapper, views, preflight, planner);
    }

    private SysPluginTask archive(byte[] bytes) {
        var archive = new SysPluginTask();
        archive.setArchiveData(bytes);
        return archive;
    }
}
