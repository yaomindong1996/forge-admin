package com.mdframe.forge.plugin.print.service;

import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.entity.PrintTemplate;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PrintTemplateAccess {

    private final PrintTemplateMapper templates;

    private final PrintProviderRegistry registry;

    private final PrintIdentity identity;

    public record Access(PrintTemplate row, AuthorizedPrintSource source, PrintDataProvider provider) {
    }

    public AuthorizedPrintSource source(PrintActor actor, PrintSourceRequest source, PrintDesignAction action, boolean lock) {
        identity.validate(source);
        var application = registry.application();
        application.authorize(actor, source.applicationId(), action);
        if (lock) {
            if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                throw new IllegalStateException("打印写操作需要事务");
            }
            application.lockApplication(actor, source.applicationId());
        }
        registry.provider(source).authorizeDesignSource(actor, source, action);
        return new AuthorizedPrintSource(actor, source);
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
        if (revision == null || revision < 1 || revision == Long.MAX_VALUE || !Objects.equals(row.getDraftRevision(), revision)) {
            throw PrintFailure.conflict();
        }
    }

    public void changed(int count) {
        if (count != 1) {
            throw PrintFailure.conflict();
        }
    }
}
