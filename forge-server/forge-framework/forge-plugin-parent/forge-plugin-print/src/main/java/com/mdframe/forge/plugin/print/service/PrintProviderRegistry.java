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
            throw PrintFailure.of(503, "PRINT_PROVIDER_UNAVAILABLE", "应用授权适配器尚未接入或存在重复配置");
        }
        return applications.get(0);
    }

    public PrintDataProvider provider(PrintSourceRequest source) {
        var selected = providers.stream().filter(p -> p.sourceType() == source.sourceType() && p.supports(source)).toList();
        if (selected.size() != 1) {
            throw PrintFailure.of(503, "PRINT_PROVIDER_UNAVAILABLE", "当前表单的打印数据适配器尚未接入或存在重复配置");
        }
        return selected.get(0);
    }

    public AuthorizedPrintContext authorize(PrintActor actor, PrintRecordRequest request) {
        var context = provider(request.source()).authorize(actor, request);
        if (context == null || !actor.equals(context.actor()) || context.record() == null || !request.source().equals(context.record().source()) || !request.recordId().equals(context.record().recordId()) || request.scene() != context.record().scene() || !Objects.equals(request.taskId(), context.record().taskId()) || !Objects.equals(request.processInstanceId(), context.record().processInstanceId()) || (request.processRunId() != null && !Objects.equals(request.processRunId(), context.record().processRunId())) || context.applicationVersionId() == null || context.applicationVersionId() <= 0 || context.catalog() == null) {
            throw PrintFailure.denied();
        }
        if (context.record().scene().name().startsWith("FLOW_") && context.record().processRunId() == null) {
            throw PrintFailure.denied();
        }
        var ids = new HashSet<Long>();
        if (context.versions().size() > 100 || context.versions().stream().filter(AuthorizedPrintContext.VersionRef::isDefault).count() > 1) {
            throw PrintFailure.denied();
        }
        for (var ref : context.versions()) {
            if (ref.templateId() == null || ref.templateId() <= 0 || ref.templateVersionId() == null || ref.templateVersionId() <= 0 || !ids.add(ref.templateId())) {
                throw PrintFailure.denied();
            }
        }
        return context;
    }
}
