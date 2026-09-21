package com.mdframe.forge.business.core.purchase.printing;

import com.mdframe.forge.business.core.purchase.service.SamplePurchaseOrderService;
import com.mdframe.forge.business.core.purchase.support.SamplePurchaseOrderFlowDefinition;
import com.mdframe.forge.business.core.purchase.vo.SamplePurchaseOrderVO;
import com.mdframe.forge.plugin.generator.mapper.BusinessApplicationVersionMapper;
import com.mdframe.forge.plugin.generator.service.businessapp.BusinessApplicationRuntimeService;
import com.mdframe.forge.plugin.generator.service.printing.FlowPrintAccessPolicy;
import com.mdframe.forge.plugin.generator.service.printing.FlowPrintContextResolver;
import com.mdframe.forge.plugin.generator.service.printing.FlowPrintHistoryAdapter;
import com.mdframe.forge.plugin.generator.service.printing.LowcodePrintResourceAccess;
import com.mdframe.forge.plugin.generator.service.printing.PrintApplicationAccessAdapter;
import com.mdframe.forge.plugin.generator.service.printing.PrintApplicationSnapshotCodec;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.service.PrintDocumentAccess;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintContext;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintSource;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintBindingSelection;
import com.mdframe.forge.plugin.print.spi.PrintData;
import com.mdframe.forge.plugin.print.spi.PrintDataProvider;
import com.mdframe.forge.plugin.print.spi.PrintRecordRequest;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 采购代码业务的打印 Provider；只从应用发布快照和业务 Service 读取数据。 */
@Component
@RequiredArgsConstructor
public class SamplePurchaseOrderPrintDataProvider implements PrintDataProvider {

    private final PrintIdentity identity;
    private final PrintApplicationAccessAdapter applicationAccess;
    private final BusinessApplicationRuntimeService runtime;
    private final BusinessApplicationVersionMapper applicationVersions;
    private final PrintApplicationSnapshotCodec snapshots;
    private final PrintTemplateVersionMapper templateVersions;
    private final SamplePurchaseOrderService purchaseOrders;
    private final FlowPrintContextResolver flowContexts;
    private final FlowPrintAccessPolicy flowAccess;
    private final FlowPrintHistoryAdapter flowHistory;
    private final LowcodePrintResourceAccess resources;
    private final PrintDocumentAccess documents;

    @Override
    public PrintSourceType sourceType() {
        return PrintSourceType.CODE;
    }

    @Override
    public boolean supports(PrintSourceRequest source) {
        return source != null
                && source.sourceType() == PrintSourceType.CODE
                && SamplePurchaseOrderFlowDefinition.BUSINESS_TYPE.equals(source.objectCode())
                && SamplePurchaseOrderFlowDefinition.FORM_KEY.equals(source.formKey());
    }

    @Override
    public void authorizeDesignSource(PrintActor actor, PrintSourceRequest source, PrintDesignAction action) {
        requireSource(source);
        applicationAccess.authorize(actor, source.applicationId(), action);
    }

    @Override
    public PrintFieldCatalogVO catalog(AuthorizedPrintSource source) {
        if (source == null || !source.actor().equals(identity.current())) {
            throw PrintFailure.denied();
        }
        requireSource(source.source());
        return buildCatalog(null);
    }

    @Override
    public void validateDesignResources(AuthorizedPrintSource source, Set<String> fileIds) {
        resources.validate(source.actor(), fileIds);
    }

    private record Resolution(Long applicationVersionId,
                              List<AuthorizedPrintContext.VersionRef> versions,
                              PrintRecordRequest record,
                              FlowPrintContextResolver.Context flow,
                              PrintFieldCatalogVO catalog,
                              SamplePurchaseOrderVO detail) {
    }

    @Override
    public AuthorizedPrintContext authorize(PrintActor actor, PrintRecordRequest request) {
        Resolution resolved = resolve(actor, request, null);
        return new AuthorizedPrintContext(actor, resolved.record(), resolved.applicationVersionId(),
                resolved.versions(), resolved.catalog());
    }

    @Override
    public PrintData load(AuthorizedPrintContext context, PrintBindingSelection selection) {
        Resolution resolved = resolve(context.actor(), context.record(), context.applicationVersionId());
        Map<String, String> types = documents.catalog(resolved.catalog());
        if (!resolved.versions().contains(selection.version())
                || selection.fields().stream().anyMatch(path -> !types.containsKey(path)
                || "COLLECTION".equals(types.get(path)))
                || selection.collections().stream().anyMatch(path -> !"COLLECTION".equals(types.get(path)))) {
            throw PrintFailure.denied();
        }
        Map<String, Object> main = scalarRecord(SamplePurchaseOrderFlowDefinition.recordData(resolved.detail()));
        Map<String, Object> flow = resolved.flow() == null ? Map.of() : flowHistory.load(resolved.flow());
        return new PrintData(main, Map.of(), flow);
    }

    @Override
    public void validateRuntimeResources(AuthorizedPrintContext context, Set<String> fileIds) {
        resources.validate(context.actor(), fileIds);
    }

    private Resolution resolve(PrintActor actor, PrintRecordRequest request, Long expectedVersionId) {
        if (actor == null || request == null || !actor.equals(identity.require("print:execute"))
                || !request.isSceneValid()) {
            throw PrintFailure.denied();
        }
        requireSource(request.source());
        var application = runtime.runtimeById(request.source().applicationId());
        boolean objectVisible = application.getObjects().stream().anyMatch(item ->
                SamplePurchaseOrderFlowDefinition.BUSINESS_TYPE.equals(item.getObjectCode()));
        if (!objectVisible) {
            throw PrintFailure.denied();
        }
        var version = applicationVersions.selectVersion(actor.tenantId(), request.source().applicationId(),
                application.getVersionNo());
        if (version == null || expectedVersionId != null && !expectedVersionId.equals(version.getId())) {
            throw PrintFailure.of(409, "PRINT_APPLICATION_CHANGED", "应用发布版本已变化，请重新打开打印");
        }

        FlowPrintContextResolver.Context flow = isFlowScene(request.scene())
                ? flowContexts.resolve(actor, request) : null;
        if (flow != null) {
            flowAccess.authorize(actor, request.scene(), flow);
        }

        List<PrintApplicationSnapshotCodec.Binding> bindings = snapshots.read(
                version.getSnapshotJson(), request.source().applicationId());
        List<AuthorizedPrintContext.VersionRef> refs = bindings.stream()
                .filter(binding -> binding.source().equals(request.source()) && binding.scene() == request.scene())
                .sorted(Comparator.comparing(PrintApplicationSnapshotCodec.Binding::isDefault).reversed()
                        .thenComparingInt(PrintApplicationSnapshotCodec.Binding::sortOrder)
                        .thenComparing(PrintApplicationSnapshotCodec.Binding::templateId))
                .map(binding -> new AuthorizedPrintContext.VersionRef(binding.templateId(), binding.templateVersionId(),
                        binding.isDefault(), binding.sortOrder()))
                .toList();
        if (flow != null) {
            refs = flowAccess.templates(refs, flow);
        }
        for (PrintApplicationSnapshotCodec.Binding binding : bindings) {
            if (!binding.source().equals(request.source()) || binding.scene() != request.scene()) {
                continue;
            }
            var pinned = templateVersions.selectScoped(actor.tenantId(), binding.templateId(),
                    binding.templateVersionId());
            if (pinned == null || !binding.schemaHash().equals(pinned.getSchemaHash())) {
                throw PrintFailure.of(409, "PRINT_APPLICATION_VERSION_INVALID", "应用引用的打印版本校验失败");
            }
        }

        Long recordId = parseRecordId(request.recordId());
        SamplePurchaseOrderVO detail;
        try {
            detail = purchaseOrders.detail(recordId);
        } catch (RuntimeException error) {
            throw PrintFailure.denied();
        }
        if (!Objects.equals(String.valueOf(detail.getId()), request.recordId())) {
            throw PrintFailure.denied();
        }
        if (flow != null && (!Objects.equals(detail.getProcessInstanceId(), flow.processInstanceId())
                || !Objects.equals(detail.getBusinessKey(), flow.businessKey()))) {
            throw PrintFailure.denied();
        }

        PrintFieldCatalogVO catalog = buildCatalog(flow);
        PrintRecordRequest normalized = flow == null ? request : new PrintRecordRequest(
                request.source(), flow.recordId(), request.scene(), flow.taskId(), flow.processInstanceId(),
                flow.processRunId());
        return new Resolution(version.getId(), refs, normalized, flow, catalog, detail);
    }

    private PrintFieldCatalogVO buildCatalog(FlowPrintContextResolver.Context flow) {
        List<PrintFieldCatalogVO.Field> fields = new ArrayList<>();
        for (Map<String, Object> field : SamplePurchaseOrderFlowDefinition.fields(null)) {
            if (Boolean.FALSE.equals(field.get("visible")) || "fileUpload".equals(field.get("componentType"))) {
                continue;
            }
            String code = String.valueOf(field.get("fieldCode"));
            fields.add(new PrintFieldCatalogVO.Field("main." + code, String.valueOf(field.get("label")),
                    type(code, String.valueOf(field.get("componentType")))));
        }
        fields.addAll(flowHistory.catalog());
        PrintFieldCatalogVO catalog = new PrintFieldCatalogVO(fields);
        if (flow != null) {
            catalog = flowAccess.fields(catalog, flow);
        }
        documents.catalog(catalog);
        return catalog;
    }

    private String type(String code, String componentType) {
        if (SamplePurchaseOrderFlowDefinition.FIELD_AMOUNT_CENT.equals(code)) {
            return "MONEY";
        }
        return switch (StringUtils.defaultString(componentType)) {
            case "number" -> "NUMBER";
            case "date", "datetime" -> "DATE";
            default -> "TEXT";
        };
    }

    private Map<String, Object> scalarRecord(Map<String, Object> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key,
                value instanceof TemporalAccessor ? String.valueOf(value) : value));
        return result;
    }

    private Long parseRecordId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException error) {
            throw PrintFailure.denied();
        }
    }

    private void requireSource(PrintSourceRequest source) {
        if (!supports(source) || !source.isSourceValid()) {
            throw PrintFailure.denied();
        }
    }

    private boolean isFlowScene(PrintScene scene) {
        return scene == PrintScene.FLOW_TODO || scene == PrintScene.FLOW_DONE
                || scene == PrintScene.FLOW_STARTED;
    }
}
