package com.mdframe.forge.plugin.print.dto;

import com.mdframe.forge.plugin.print.spi.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record PrintAvailableTemplatesDTO(@Valid @NotNull PrintRecordRequest record) {
}
