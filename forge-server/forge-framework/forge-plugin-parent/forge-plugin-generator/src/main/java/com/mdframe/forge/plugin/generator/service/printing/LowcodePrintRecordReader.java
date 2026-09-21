package com.mdframe.forge.plugin.generator.service.printing;

import com.mdframe.forge.plugin.generator.service.DynamicCrudService;
import com.mdframe.forge.plugin.print.service.PrintFailure;
import com.mdframe.forge.plugin.print.spi.PrintBindingSelection;
import com.mdframe.forge.plugin.print.spi.PrintData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class LowcodePrintRecordReader {
    private final DynamicCrudService crud;
    private final LowcodePrintValueAdapter values;

    public void assertReadable(PrintMetadataResolver.Metadata metadata, String recordId) {
        if (crud.selectPrintById(metadata.main().config(), recordId) == null) {
            throw PrintFailure.of(404, "PRINT_RECORD_UNAVAILABLE", "单据不存在或无权打印此记录");
        }
    }

    public PrintData read(PrintMetadataResolver.Metadata metadata, String recordId, PrintBindingSelection selection) {
        var record = crud.selectPrintById(metadata.main().config(), recordId);
        if (record == null) {
            throw PrintFailure.of(404, "PRINT_RECORD_UNAVAILABLE", "单据不存在或无权打印此记录");
        }
        Map<String, Object> children = new LinkedHashMap<>();
        for (var child : metadata.children()) {
            String path = "children." + child.key();
            if (!selection.collections().contains(path)) {
                continue;
            }
            Object relationValue = record.columns().get(child.mainColumn());
            if (!record.columns().containsKey(child.mainColumn())) {
                throw PrintFailure.field(path, "已发布主子表关联字段不存在");
            }
            var rows = crud.selectPrintChildren(child.model().config(), child.childColumn(), relationValue);
            children.put(child.key(), rows.stream().map(row -> values.adapt(
                    child.model(), row, path, selection.fields())).toList());
        }
        return new PrintData(values.adapt(metadata.main(), record.values(), "main", selection.fields()), children, Map.of());
    }
}
