package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintContext;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintSource;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintBindingSelection;
import com.mdframe.forge.plugin.print.spi.PrintBusinessDataProvider;
import com.mdframe.forge.plugin.print.spi.PrintData;
import com.mdframe.forge.plugin.print.spi.PrintDataProvider;
import com.mdframe.forge.plugin.print.spi.PrintRecordRequest;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.plugin.print.vo.PrintFieldCatalogVO;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SERVICE 来源统一适配器：先解析服务端来源注册，再分发到明确编码的业务 Provider。
 */
@Component
@RequiredArgsConstructor
public class ServicePrintDataProvider implements PrintDataProvider {

    private final PrintBusinessSourceMapper sources;

    private final List<PrintBusinessDataProvider> providers;

    private final PrintParameterValidator parameters;

    @Override
    public PrintSourceType sourceType() {
        return PrintSourceType.SERVICE;
    }

    @Override
    public boolean supports(PrintSourceRequest source) {
        return source != null && source.sourceType() == PrintSourceType.SERVICE
                && source.businessSourceId() != null;
    }

    @Override
    public void authorizeDesignSource(PrintActor actor, PrintSourceRequest request,
                                      PrintDesignAction action) {
        var resolved = resolve(actor, request, false);
        resolved.provider().authorizeDesignSource(actor, resolved.source(), action);
    }

    @Override
    public PrintFieldCatalogVO catalog(AuthorizedPrintSource authorized) {
        if (authorized == null) {
            throw PrintFailure.denied();
        }
        var resolved = resolve(authorized.actor(), authorized.source(), false);
        return resolved.provider().catalog(authorized.actor(), resolved.source());
    }

    @Override
    public void validateDesignResources(AuthorizedPrintSource authorized, Set<String> fileIds) {
        if (authorized == null) {
            throw PrintFailure.denied();
        }
        var resolved = resolve(authorized.actor(), authorized.source(), false);
        resolved.provider().validateDesignResources(
                authorized.actor(), resolved.source(), fileIds);
    }

    @Override
    public AuthorizedPrintContext authorize(PrintActor actor, PrintRecordRequest request) {
        var resolved = resolve(actor, request.source(), true);
        Map<String, Object> params = parameters.validate(
                resolved.source().getParameterSchemaJson(), request.params());
        resolved.provider().authorizeRecord(actor, resolved.source(), request, params);
        var catalog = resolved.provider().catalog(actor, resolved.source());
        return new AuthorizedPrintContext(actor, request, null, List.of(), catalog);
    }

    @Override
    public PrintData load(AuthorizedPrintContext context, PrintBindingSelection selection) {
        var resolved = resolve(context.actor(), context.record().source(), true);
        Map<String, Object> params = parameters.validate(
                resolved.source().getParameterSchemaJson(), context.record().params());
        resolved.provider().authorizeRecord(
                context.actor(), resolved.source(), context.record(), params);
        return resolved.provider().load(
                context.actor(), resolved.source(), context.record(), selection, params);
    }

    @Override
    public void validateRuntimeResources(AuthorizedPrintContext context, Set<String> fileIds) {
        var resolved = resolve(context.actor(), context.record().source(), true);
        resolved.provider().validateRuntimeResources(
                context.actor(), resolved.source(), context.record(), fileIds);
    }

    private Resolution resolve(PrintActor actor, PrintSourceRequest requested, boolean enabled) {
        if (actor == null || !supports(requested)) {
            throw PrintFailure.denied();
        }
        PrintBusinessSource source = sources.selectScoped(
                actor.tenantId(), requested.businessSourceId());
        if (source == null || !PrintSourceType.SERVICE.matches(source.getSourceType())
                || !requested.equals(PrintSourceRequest.from(source))) {
            throw PrintFailure.denied();
        }
        if (enabled && !EnableStatus.ENABLED.matches(source.getStatus())) {
            throw PrintFailure.missing();
        }
        return new Resolution(source, provider(source.getProviderCode()));
    }

    private PrintBusinessDataProvider provider(String code) {
        var selected = providers.stream()
                .filter(provider -> provider.code() != null && provider.code().equals(code))
                .toList();
        if (selected.size() != 1) {
            throw PrintFailure.of(
                    503, "PRINT_PROVIDER_UNAVAILABLE", "业务打印 Provider 未接入或存在重复编码");
        }
        return selected.get(0);
    }

    private record Resolution(PrintBusinessSource source, PrintBusinessDataProvider provider) {
    }
}
