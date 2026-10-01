package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.entity.PrintBinding;
import com.mdframe.forge.plugin.print.mapper.PrintBindingMapper;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateVersionMapper;
import com.mdframe.forge.plugin.print.spi.AuthorizedPrintContext;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * 独立来源的运行时版本解析器。
 * 只接受绑定中固定的不可变发布版本，不回退到模板最新版本。
 */
@Component
@RequiredArgsConstructor
public class PrintRuntimeBindingResolver {

    private final PrintBusinessSourceMapper sources;

    private final PrintBindingMapper bindings;

    private final PrintTemplateMapper templates;

    private final PrintTemplateVersionMapper versions;

    public AuthorizedPrintContext resolve(AuthorizedPrintContext context) {
        PrintSourceRequest requested = context.record().source();
        if (!requested.sourceType().isStandalone()) {
            return context;
        }
        var source = sources.selectScoped(context.actor().tenantId(), requested.businessSourceId());
        if (source == null || !EnableStatus.ENABLED.matches(source.getStatus())
                || source.getSourceRevision() == null || source.getSourceRevision() <= 0) {
            throw PrintFailure.missing();
        }
        PrintSourceRequest canonical = PrintSourceRequest.from(source);
        if (!canonical.equals(requested)) {
            throw PrintFailure.denied();
        }
        var rows = bindings.selectStandaloneEnabled(
                context.actor().tenantId(), source.getId(), canonical.key(),
                context.record().scene().getCode());
        var refs = new ArrayList<AuthorizedPrintContext.VersionRef>();
        var templateIds = new HashSet<Long>();
        for (PrintBinding row : rows) {
            refs.add(resolveBinding(context, canonical, row, templateIds));
        }
        validateDefaults(refs);
        return new AuthorizedPrintContext(
                context.actor(), context.record(), null, source.getSourceRevision(), refs,
                context.catalog());
    }

    private AuthorizedPrintContext.VersionRef resolveBinding(
            AuthorizedPrintContext context, PrintSourceRequest source, PrintBinding binding,
            HashSet<Long> templateIds) {
        var template = templates.selectScoped(context.actor().tenantId(), binding.getTemplateId());
        if (template == null || !EnableStatus.ENABLED.matches(template.getStatus())
                || !source.equals(PrintSourceRequest.from(template))
                || !templateIds.add(template.getId())) {
            throw PrintFailure.denied();
        }
        var version = versions.selectScoped(
                context.actor().tenantId(), template.getId(), binding.getTemplateVersionId());
        if (version == null) {
            throw PrintFailure.of(409, "PRINT_VERSION_INVALID", "打印绑定引用的发布版本不存在");
        }
        return new AuthorizedPrintContext.VersionRef(
                template.getId(), version.getId(), Boolean.TRUE.equals(binding.getIsDefault()),
                binding.getSortOrder());
    }

    private void validateDefaults(ArrayList<AuthorizedPrintContext.VersionRef> refs) {
        if (refs.size() > 100
                || refs.stream().filter(AuthorizedPrintContext.VersionRef::isDefault).count() > 1) {
            throw PrintFailure.denied();
        }
    }
}
