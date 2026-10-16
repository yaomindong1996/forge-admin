package com.mdframe.forge.plugin.print.dto;

import com.mdframe.forge.plugin.print.spi.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record PrintCatalogQueryDTO(@Valid PrintSourceRequest source, @Valid PrintRecordRequest record) {

    @JsonIgnore
    @AssertTrue(message = "catalog 请求只能传 source 或 record 其一")
    public boolean isExclusive() {
        return (source == null) != (record == null);
    }
}
