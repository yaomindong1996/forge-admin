package com.mdframe.forge.plugin.print;

import com.mdframe.forge.plugin.print.dto.*;
import com.mdframe.forge.plugin.print.enums.*;
import com.mdframe.forge.plugin.print.spi.*;
import com.mdframe.forge.plugin.print.vo.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrintExecutionServiceTest extends PrintServiceFixture {

    @Test
    void eventIsOwnedTerminalAndIdempotentWithoutClaimingPaperOutput() {
        var row = published();
        var prepared = runtime.prepare(new PrintPrepareDTO(record(), row.id()));
        var event = new PrintExecutionEventDTO(PrintExecutionResult.DIALOG_OPENED, 2, null);
        actor = new PrintActor(1L, 10L, 1L);
        fails(404, () -> events.record(prepared.executionId(), event));
        actor = new PrintActor(1L, 9L, 1L);
        assertThat(events.record(prepared.executionId(), event).result()).isEqualTo("DIALOG_OPENED");
        assertThat(events.record(prepared.executionId(), event).pageCount()).isEqualTo(2);
        assertThat(events.record(prepared.executionId(), event).physicalOutputConfirmed()).isFalse();
        fails(409, () -> events.record(prepared.executionId(), new PrintExecutionEventDTO(PrintExecutionResult.FAILED, null, "PRINT_FAILED")));
        assertThat(jdbc.queryForObject("SELECT actor FROM sys_print_execution WHERE id=?", Long.class, prepared.executionId())).isEqualTo(9L);
    }

    @Test
    void acceptsPdfDownloadAsClientOutputWithoutClaimingPaper() {
        var row = published();
        var prepared = runtime.prepare(new PrintPrepareDTO(record(), row.id()));
        var event = new PrintExecutionEventDTO(PrintExecutionResult.PDF_DOWNLOADED, 1, null);
        assertThat(events.record(prepared.executionId(), event).result()).isEqualTo("PDF_DOWNLOADED");
        assertThat(events.record(prepared.executionId(), event).physicalOutputConfirmed()).isFalse();
        fails(409, () -> events.record(prepared.executionId(), new PrintExecutionEventDTO(PrintExecutionResult.DIALOG_OPENED, 1, null)));
    }

    @Test
    void refusesPreparedAsClientEventOversizedPageCountAndArbitraryErrorText() {
        fails(400, () -> events.record(1L, new PrintExecutionEventDTO(PrintExecutionResult.PREPARED, null, null)));
        fails(400, () -> events.record(1L, new PrintExecutionEventDTO(PrintExecutionResult.DIALOG_OPENED, 51, null)));
        fails(400, () -> events.record(1L, new PrintExecutionEventDTO(PrintExecutionResult.FAILED, null, "raw private record")));
        fails(400, () -> events.record(1L, new PrintExecutionEventDTO(PrintExecutionResult.FAILED, 1, "PRINT_FAILED")));
    }
}
