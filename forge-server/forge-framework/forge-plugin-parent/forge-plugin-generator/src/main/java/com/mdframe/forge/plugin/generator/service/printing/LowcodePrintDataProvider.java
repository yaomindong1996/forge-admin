package com.mdframe.forge.plugin.generator.service.printing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationRuntimeService;
import com.mdframe.forge.plugin.print.entity.PrintBinding;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.mapper.PrintBindingMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.service.PrintDocumentAccess;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/** 低代码正式入口：应用门户、页面、对象操作与记录范围逐层检查。 */
@Component
@RequiredArgsConstructor
public class LowcodePrintDataProvider implements PrintDataProvider {
    private final PrintIdentity identity;
    private final PrintApplicationAccessAdapter applicationAccess;
    private final BusinessApplicationRuntimeService runtime;
    private final BusinessApplicationVersionMapper versions;
    private final PrintApplicationSnapshotCodec snapshots;
    private final PrintMetadataResolver metadata;
    private final LowcodePrintSourceResolver sources;
    private final LowcodePrintCatalogBuilder catalogs;
    private final LowcodePrintRecordReader records;
    private final LowcodePrintResourceAccess resources;
    private final PrintDocumentAccess documentAccess;
    private final ObjectMapper json;
    private final com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper templateVersions;
    private final PrintTemplateMapper templates;
    private final PrintBindingMapper bindings;
    private final FlowPrintContextResolver flowContexts;
    private final FlowPrintAccessPolicy flowAccess;
    private final FlowPrintHistoryAdapter flowHistory;

    @Override
    public PrintSourceType sourceType() {
        return PrintSourceType.LOWCODE;
    }

    @Override
    public boolean supports(PrintSourceRequest source) {
        return source != null && source.sourceType() == PrintSourceType.LOWCODE;
    }

    @Override
    public void authorizeDesignSource(PrintActor actor, PrintSourceRequest source, PrintDesignAction action) {
        applicationAccess.authorize(actor, source.applicationId(), action);
        metadata.draft(actor, source);
    }

    @Override
    public PrintFieldCatalogVO catalog(AuthorizedPrintSource source) {
        if (!source.actor().equals(identity.current())) {
            throw PrintFailure.denied();
        }
        List<PrintFieldCatalogVO.Field> fields = new ArrayList<>(
                catalogs.build(metadata.draft(source.actor(), source.source())).fields());
        fields.addAll(flowHistory.catalog());
        PrintFieldCatalogVO catalog = new PrintFieldCatalogVO(fields);
        documentAccess.catalog(catalog);
        return catalog;
    }

    @Override
    public void validateDesignResources(AuthorizedPrintSource source, Set<String> fileIds) {
        resources.validate(source.actor(), fileIds);
    }

    private record Resolution(Long versionId,
                              List<AuthorizedPrintContext.VersionRef> versions,
                              PrintMetadataResolver.Metadata metadata,
                              PrintRecordRequest record,
                              FlowPrintContextResolver.Context flow,
                              PrintFieldCatalogVO catalog) { }

    private Resolution resolve(PrintActor actor, PrintRecordRequest request, Long expectedVersion) {
        if (!actor.equals(identity.require("print:execute")) || !request.isSceneValid()) {
            throw PrintFailure.denied();
        }
        boolean flowScene = request.scene() == PrintScene.FLOW_TODO
                || request.scene() == PrintScene.FLOW_DONE
                || request.scene() == PrintScene.FLOW_STARTED;
        if (!flowScene && request.scene() != PrintScene.LIST && request.scene() != PrintScene.DETAIL) {
            throw PrintFailure.denied();
        }
        FlowPrintContextResolver.Context flow = null;
        if (flowScene) {
            flow = flowContexts.resolve(actor, request);
            flowAccess.authorize(actor, request.scene(), flow);
            if (flow.processRunId() == null) {
                throw PrintFailure.denied();
            }
        } else {
            // 列表/详情也可绑定审批记录：有流程实例则回填，没有也不拦字段目录。
            flow = flowContexts.resolveForRecord(actor, request);
        }
        var source = request.source();
        var portal = runtime.runtimeById(source.applicationId());
        // 门户已经剔除没有页面权限的节点；来源检查只能在这个过滤后的页面树上进行。
        var allowed = json.createObjectNode();
        allowed.putObject("application").set("options", metadata.parse(portal.getApplication().getOptions()));
        allowed.set("objects", json.valueToTree(portal.getObjects()));
        sources.object(allowed, source, false);
        if (!SessionHelper.hasPermission("ai:business:" + source.objectCode() + ":query")
                && !SessionHelper.hasPermission("ai:business:" + source.objectCode() + ":list")) {
            throw PrintFailure.denied();
        }
        var version = versions.selectVersion(actor.tenantId(), source.applicationId(), portal.getVersionNo());
        if (version == null || (expectedVersion != null && !expectedVersion.equals(version.getId()))) {
            throw PrintFailure.of(409, "PRINT_APPLICATION_CHANGED", "应用发布版本已变化，请重新打开打印");
        }
        var pinned = snapshots.read(version.getSnapshotJson(), source.applicationId());
        var matched = pinned.stream()
                .filter(binding -> binding.source().equals(source) && binding.scene() == request.scene())
                .sorted(Comparator.comparing(PrintApplicationSnapshotCodec.Binding::isDefault).reversed()
                        .thenComparingInt(PrintApplicationSnapshotCodec.Binding::sortOrder)
                        .thenComparing(PrintApplicationSnapshotCodec.Binding::templateId))
                .toList();
        // 设计人员试打：绑定可能尚未进入应用发布快照，业务对象也可能没有固定设计版本。
        boolean designer = SessionHelper.hasPermission(PrintDesignAction.MANAGE.permission())
                || SessionHelper.hasPermission(PrintDesignAction.VIEW.permission());
        List<AuthorizedPrintContext.VersionRef> refs;
        PrintMetadataResolver.Metadata resolved;
        if (designer) {
            refs = pinnedRefs(actor.tenantId(), matched);
            if (refs.isEmpty()) {
                refs = liveRefs(actor.tenantId(), source, request.scene());
            }
            if (refs.isEmpty()) {
                throw PrintFailure.of(409, "PRINT_TEMPLATE_UNPUBLISHED",
                        "没有可用的已发布打印模板：请先发布打印模板，并确认已绑定到当前场景");
            }
            resolved = metadata.draft(actor, source);
        } else {
            if (matched.isEmpty()) {
                throw PrintFailure.of(404, "PRINT_BINDING_NOT_PUBLISHED",
                        "当前应用版本尚未包含打印绑定，请重新发布应用后再打印");
            }
            refs = pinnedRefs(actor.tenantId(), matched);
            if (refs.isEmpty()) {
                throw PrintFailure.of(409, "PRINT_APPLICATION_VERSION_INVALID", "应用引用的打印版本校验失败");
            }
            resolved = metadata.published(actor, source, metadata.parse(version.getSnapshotJson()));
            metadata.assertRuntimeEnabled(actor, resolved);
        }
        if (flowScene && flow != null) {
            refs = flowAccess.templates(refs, flow);
        }
        PrintFieldCatalogVO catalog = catalogs.build(resolved);
        PrintRecordRequest normalized = request;
        List<PrintFieldCatalogVO.Field> fields = new ArrayList<>(catalog.fields());
        fields.addAll(flowHistory.catalog());
        catalog = new PrintFieldCatalogVO(fields);
        if (flow != null) {
            catalog = flowAccess.fields(catalog, flow);
            if (flowScene) {
                normalized = new PrintRecordRequest(source, flow.recordId(), request.scene(), flow.taskId(),
                        flow.processInstanceId(), flow.processRunId());
            }
        }
        documentAccess.catalog(catalog);
        return new Resolution(version.getId(), refs, resolved, normalized, flow, catalog);
    }

    private List<AuthorizedPrintContext.VersionRef> pinnedRefs(Long tenantId,
                                                               List<PrintApplicationSnapshotCodec.Binding> matched) {
        List<AuthorizedPrintContext.VersionRef> refs = new ArrayList<>();
        for (var binding : matched) {
            var pinnedVersion = templateVersions.selectScoped(tenantId, binding.templateId(), binding.templateVersionId());
            if (pinnedVersion == null || !binding.schemaHash().equals(pinnedVersion.getSchemaHash())) {
                continue;
            }
            refs.add(new AuthorizedPrintContext.VersionRef(binding.templateId(), binding.templateVersionId(),
                    binding.isDefault(), binding.sortOrder()));
        }
        return List.copyOf(refs);
    }

    private List<AuthorizedPrintContext.VersionRef> liveRefs(Long tenantId, PrintSourceRequest source, PrintScene scene) {
        List<AuthorizedPrintContext.VersionRef> refs = new ArrayList<>();
        for (PrintBinding row : bindings.selectApplicationEnabled(tenantId, source.applicationId())) {
            if (!Objects.equals(row.getSourceKey(), source.key()) || !scene.matches(row.getScene())) {
                continue;
            }
            var template = templates.selectScoped(tenantId, row.getTemplateId());
            if (template == null || !EnableStatus.ENABLED.matches(template.getStatus())
                    || template.getPublishedVersionId() == null
                    || !source.key().equals(template.getSourceKey())) {
                continue;
            }
            var published = templateVersions.selectScoped(tenantId, template.getId(), template.getPublishedVersionId());
            if (published == null) {
                continue;
            }
            refs.add(new AuthorizedPrintContext.VersionRef(template.getId(), published.getId(),
                    Boolean.TRUE.equals(row.getIsDefault()), row.getSortOrder() == null ? 0 : row.getSortOrder()));
        }
        refs.sort(Comparator.comparing(AuthorizedPrintContext.VersionRef::isDefault).reversed()
                .thenComparingInt(AuthorizedPrintContext.VersionRef::sortOrder)
                .thenComparing(AuthorizedPrintContext.VersionRef::templateId));
        return List.copyOf(refs);
    }

    @Override
    public AuthorizedPrintContext authorize(PrintActor actor, PrintRecordRequest request) {
        var resolved = resolve(actor, request, null);
        records.assertReadable(resolved.metadata(), resolved.record().recordId());
        return new AuthorizedPrintContext(actor, resolved.record(), resolved.versionId(),
                resolved.versions(), resolved.catalog());
    }

    @Override
    public PrintData load(AuthorizedPrintContext context, PrintBindingSelection selection) {
        var resolved = resolve(context.actor(), context.record(), context.applicationVersionId());
        var types = documentAccess.catalog(resolved.catalog());
        if (!resolved.versions().contains(selection.version())
                || selection.fields().stream().anyMatch(field -> !types.containsKey(field) || "COLLECTION".equals(types.get(field)))
                || selection.collections().stream().anyMatch(path -> !"COLLECTION".equals(types.get(path)))) {
            throw PrintFailure.denied();
        }
        PrintData data = records.read(resolved.metadata(), context.record().recordId(), selection);
        boolean needsFlow = selection.fields().stream().anyMatch(field -> field.startsWith("flow."))
                || selection.collections().stream().anyMatch(path -> path.startsWith("flow."));
        if (!needsFlow) {
            return data;
        }
        if (resolved.flow() == null) {
            return new PrintData(data.main(), data.children(), flowHistory.empty());
        }
        return new PrintData(data.main(), data.children(), flowHistory.load(resolved.flow()));
    }

    @Override
    public void validateRuntimeResources(AuthorizedPrintContext context, Set<String> fileIds) {
        resources.validate(context.actor(), fileIds);
    }
}
