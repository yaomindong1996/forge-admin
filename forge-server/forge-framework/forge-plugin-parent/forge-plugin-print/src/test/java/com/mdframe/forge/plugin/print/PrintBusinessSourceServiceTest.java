package com.mdframe.forge.plugin.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceCreateDTO;
import com.mdframe.forge.plugin.print.dto.PrintBusinessSourceStatusDTO;
import com.mdframe.forge.plugin.print.entity.PrintBusinessSource;
import com.mdframe.forge.plugin.print.enums.PrintBusinessSourceType;
import com.mdframe.forge.plugin.print.mapper.PrintBusinessSourceMapper;
import com.mdframe.forge.plugin.print.service.PrintBusinessSourceService;
import com.mdframe.forge.plugin.print.service.PrintIdentity;
import com.mdframe.forge.plugin.print.service.PrintParameterValidator;
import com.mdframe.forge.plugin.print.service.PrintSourceConfigValidator;
import com.mdframe.forge.plugin.print.spi.PrintActor;
import com.mdframe.forge.starter.core.enums.EnableStatus;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrintBusinessSourceServiceTest {

    private final PrintIdentity identity = mock(PrintIdentity.class);

    private final PrintBusinessSourceMapper mapper = mock(PrintBusinessSourceMapper.class);

    private PrintBusinessSourceService service;

    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper();
        service = new PrintBusinessSourceService(identity, mapper,
                new PrintSourceConfigValidator(
                        objectMapper, new PrintParameterValidator(objectMapper)));
        when(identity.require("print:source:manage")).thenReturn(new PrintActor(1L, 9L, 2L));
        when(identity.require("print:execute")).thenReturn(new PrintActor(1L, 9L, 2L));
        doAnswer(invocation -> invocation.getArgument(0)).when(identity).validate(any());
    }

    @Test
    void shouldCreateDisabledDatasetSourceWithServerOwnedAudit() {
        doAnswer(invocation -> {
            PrintBusinessSource row = invocation.getArgument(0);
            row.setId(12L);
            return 1;
        }).when(mapper).insert(any());
        var dto = new PrintBusinessSourceCreateDTO(
                "purchase_order", "采购单", PrintBusinessSourceType.DATASET, null, 21L,
                "purchase_order", "{\"status\":{\"type\":\"string\"}}",
                "{\"recordIdParam\":\"id\",\"maxRows\":100}");

        var created = service.create(dto);

        assertThat(created.id()).isEqualTo(12L);
        assertThat(created.status()).isZero();
        verify(mapper).insert(any(PrintBusinessSource.class));
    }

    @Test
    void shouldRejectApiAndNonObjectJsonBeforeInsert() {
        var api = new PrintBusinessSourceCreateDTO(
                "external_order", "外部订单", PrintBusinessSourceType.API, null, null,
                "external_order", "{}", "{}");
        var badJson = new PrintBusinessSourceCreateDTO(
                "purchase_order", "采购单", PrintBusinessSourceType.DATASET, null, 21L,
                "purchase_order", "[]", "{\"recordIdParam\":\"id\"}");

        assertThatThrownBy(() -> service.create(api)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create(badJson)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectStaleRevisionBeforeChangingStatus() {
        PrintBusinessSource row = source();
        when(mapper.lockScoped(1L, 12L)).thenReturn(row);

        assertThatThrownBy(() -> service.status(12L, new PrintBusinessSourceStatusDTO(2L, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo(409));
    }

    @Test
    void shouldResolveEnabledRuntimeSourceByStableCode() {
        PrintBusinessSource row = source();
        row.setStatus(EnableStatus.ENABLED.getCode());
        when(mapper.selectByCode(1L, "purchase_order")).thenReturn(row);

        var result = service.resolveRuntime("purchase_order");

        assertThat(result.id()).isEqualTo(12L);
        assertThat(result.sourceCode()).isEqualTo("purchase_order");
        assertThat(result.objectCode()).isEqualTo("purchase_order");
    }

    @Test
    void shouldHideDisabledRuntimeSource() {
        when(mapper.selectByCode(1L, "purchase_order")).thenReturn(source());

        assertThatThrownBy(() -> service.resolveRuntime("purchase_order"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo(404));
    }

    private PrintBusinessSource source() {
        var row = new PrintBusinessSource();
        row.setId(12L);
        row.setTenantId(1L);
        row.setSourceCode("purchase_order");
        row.setSourceName("采购单");
        row.setSourceType(PrintBusinessSourceType.DATASET.getCode());
        row.setDatasetId(21L);
        row.setObjectCode("purchase_order");
        row.setSourceRevision(1L);
        row.setStatus(0);
        row.setDelFlag(0L);
        row.setCreateTime(LocalDateTime.now());
        row.setUpdateTime(LocalDateTime.now());
        return row;
    }
}
