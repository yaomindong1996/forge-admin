package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.spi.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class PrintProviderRegistry {

    private final List<PrintDataProvider> providers;

    private final List<PrintApplicationAccess> applications;

    public PrintApplicationAccess application() {
        if (applications.size() != 1) {
            throw PrintFailure.of(
                    503, "PRINT_PROVIDER_UNAVAILABLE", "应用授权适配器尚未接入或存在重复配置");
        }
        return applications.get(0);
    }

    public PrintDataProvider provider(PrintSourceRequest source) {
        var selected = providers.stream()
                .filter(provider -> provider.sourceType() == source.sourceType()
                        && provider.supports(source))
                .toList();
        if (selected.size() != 1) {
            throw PrintFailure.of(
                    503, "PRINT_PROVIDER_UNAVAILABLE",
                    "当前来源的打印数据适配器尚未接入或存在重复配置");
        }
        return selected.get(0);
    }

    public AuthorizedPrintContext authorize(PrintActor actor, PrintRecordRequest request) {
        var context = provider(request.source()).authorize(actor, request);
        if (!validContext(actor, request, context)) {
            throw PrintFailure.denied();
        }
        if (context.record().scene().name().startsWith("FLOW_") && context.record().processRunId() == null) {
            throw PrintFailure.denied();
        }
        validateVersionOwner(context);
        validateVersionRefs(context.versions());
        return context;
    }

    private boolean validContext(PrintActor actor, PrintRecordRequest request,
                                 AuthorizedPrintContext context) {
        return context != null
                && actor.equals(context.actor())
                && context.record() != null
                && request.source().equals(context.record().source())
                && request.recordId().equals(context.record().recordId())
                && request.scene() == context.record().scene()
                && Objects.equals(request.taskId(), context.record().taskId())
                && Objects.equals(request.processInstanceId(), context.record().processInstanceId())
                && (request.processRunId() == null
                || Objects.equals(request.processRunId(), context.record().processRunId()))
                && Objects.equals(request.params(), context.record().params())
                && context.catalog() != null;
    }

    private void validateVersionOwner(AuthorizedPrintContext context) {
        boolean standalone = context.record().source().sourceType().isStandalone();
        if (standalone && (context.applicationVersionId() != null || !context.versions().isEmpty())) {
            throw PrintFailure.denied();
        }
        if (!standalone
                && (context.applicationVersionId() == null || context.applicationVersionId() <= 0)) {
            throw PrintFailure.denied();
        }
        if (!standalone && !context.record().params().isEmpty()) {
            throw PrintFailure.denied();
        }
    }

    private void validateVersionRefs(List<AuthorizedPrintContext.VersionRef> versions) {
        var ids = new HashSet<Long>();
        if (versions.size() > 100
                || versions.stream().filter(AuthorizedPrintContext.VersionRef::isDefault).count() > 1) {
            throw PrintFailure.denied();
        }
        for (var ref : versions) {
            if (ref.templateId() == null || ref.templateId() <= 0
                    || ref.templateVersionId() == null || ref.templateVersionId() <= 0
                    || !ids.add(ref.templateId())) {
                throw PrintFailure.denied();
            }
        }
    }
}
