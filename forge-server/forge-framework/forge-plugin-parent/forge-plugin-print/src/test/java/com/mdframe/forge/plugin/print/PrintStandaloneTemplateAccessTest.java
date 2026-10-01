package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.enums.PrintDesignAction;
import com.mdframe.forge.plugin.print.enums.PrintSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.mapper.PrintTemplateMapper;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.service.PrintProviderRegistry;
import com.mdframe.forge.plugin.print.service.PrintTemplateAccess;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.plugin.print.spi.PrintDataProvider;
import com.mdframe.forge.plugin.print.spi.PrintSourceRequest;
import com.mdframe.forge.starter.core.exception.BusinessException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintStandaloneTemplateAccessTest {

    @Test
    void shouldAuthorizeOnlyCanonicalTenantScopedBusinessSource() {
        try (var validation = Validation.buildDefaultValidatorFactory()) {
            var actor = new PrintActor(1L, 9L, 2L);
            var row = businessSource();
            var canonical = PrintSourceRequest.from(row);
            var sources = mock(PrintBusinessSourceMapper.class);
            var registry = mock(PrintProviderRegistry.class);
            var provider = mock(PrintDataProvider.class);
            when(sources.selectScoped(1L, 7L)).thenReturn(row);
            when(registry.provider(canonical)).thenReturn(provider);
            var access = new PrintTemplateAccess(
                    mock(PrintTemplateMapper.class), sources, registry,
                    new PrintIdentity(validation.getValidator()));

            var authorized = access.source(actor, canonical, PrintDesignAction.MANAGE, false);

            assertThat(authorized.source()).isEqualTo(canonical);
            verify(provider).authorizeDesignSource(actor, canonical, PrintDesignAction.MANAGE);

            var spoofed = new PrintSourceRequest(
                    7L, "another_source", null, PrintSourceType.SERVICE,
                    null, null, "purchase_order");
            assertThatThrownBy(() -> access.source(actor, spoofed, PrintDesignAction.MANAGE, false))
                    .isInstanceOf(BusinessException.class);
        }
    }

    private PrintBusinessSource businessSource() {
        var row = new PrintBusinessSource();
        row.setId(7L);
        row.setTenantId(1L);
        row.setSourceCode("purchase_order");
        row.setSourceType(PrintSourceType.SERVICE.getCode());
        row.setObjectCode("purchase_order");
        row.setSourceRevision(1L);
        row.setStatus(1);
        row.setDelFlag(0L);
        return row;
    }
}
