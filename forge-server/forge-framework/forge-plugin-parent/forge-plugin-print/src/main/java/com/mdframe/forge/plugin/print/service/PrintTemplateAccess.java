package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PrintTemplateAccess {

    private final PrintTemplateMapper templates;

    private final PrintBusinessSourceMapper businessSources;

    private final PrintProviderRegistry registry;

    private final PrintIdentity identity;

    public record Access(PrintTemplate row, AuthorizedPrintSource source, PrintDataProvider provider) {
    }

    public AuthorizedPrintSource source(PrintActor actor, PrintSourceRequest source,
                                        PrintDesignAction action, boolean lock) {
        identity.validate(source);
        PrintSourceRequest authorized = source.sourceType().isStandalone()
                ? standaloneSource(actor, source, lock)
                : applicationSource(actor, source, action, lock);
        registry.provider(authorized).authorizeDesignSource(actor, authorized, action);
        return new AuthorizedPrintSource(actor, authorized);
    }

    public AuthorizedPrintSource source(PrintActor actor, Long businessSourceId,
                                        PrintDesignAction action, boolean lock) {
        PrintSourceRequest source = standaloneSource(actor, businessSourceId, lock);
        registry.provider(source).authorizeDesignSource(actor, source, action);
        return new AuthorizedPrintSource(actor, source);
    }

    /** 独立来源是否存在（列表场景不强制走业务 Provider 鉴权）。 */
    public PrintSourceRequest requireBusinessSource(PrintActor actor, Long businessSourceId) {
        return standaloneSource(actor, businessSourceId, false);
    }

    private PrintSourceRequest applicationSource(PrintActor actor, PrintSourceRequest source,
                                                 PrintDesignAction action, boolean lock) {
        var application = registry.application();
        application.authorize(actor, source.applicationId(), action);
        if (lock) {
            requireTransaction();
            application.lockApplication(actor, source.applicationId());
        }
        return source;
    }

    private PrintSourceRequest standaloneSource(PrintActor actor, PrintSourceRequest requested, boolean lock) {
        PrintSourceRequest canonical = standaloneSource(actor, requested.businessSourceId(), lock);
        if (!canonical.equals(requested)) {
            throw PrintFailure.denied();
        }
        return canonical;
    }

    private PrintSourceRequest standaloneSource(PrintActor actor, Long id, boolean lock) {
        if (id == null || id <= 0) {
            throw PrintFailure.missing();
        }
        if (lock) {
            requireTransaction();
        }
        PrintBusinessSource row = lock
                ? businessSources.lockScoped(actor.tenantId(), id)
                : businessSources.selectScoped(actor.tenantId(), id);
        if (row == null) {
            throw PrintFailure.missing();
        }
        return PrintSourceRequest.from(row);
    }

    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("打印写操作需要事务");
        }
    }

    public Access open(PrintActor actor, Long id, PrintDesignAction action, boolean lock) {
        var row = templates.selectScoped(actor.tenantId(), id);
        if (row == null) {
            throw PrintFailure.missing();
        }
        var request = PrintSourceRequest.from(row);
        var source = source(actor, request, action, lock);
        if (lock) {
            row = templates.lockScoped(actor.tenantId(), id);
        }
        if (row == null || !request.equals(PrintSourceRequest.from(row)) || !request.key().equals(row.getSourceKey())) {
            throw PrintFailure.missing();
        }
        return new Access(row, source, registry.provider(request));
    }

    public void revision(PrintTemplate row, Long revision) {
        if (revision == null || revision < 1 || revision == Long.MAX_VALUE
                || !Objects.equals(row.getDraftRevision(), revision)) {
            throw PrintFailure.conflict();
        }
    }

    public void changed(int count) {
        if (count != 1) {
            throw PrintFailure.conflict();
        }
    }
}
