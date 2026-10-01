package com.mdframe.forge.plugin.print.spi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mdframe.forge.plugin.print.enums.PrintScene;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PrintRecordRequest(
        @Valid @NotNull PrintSourceRequest source,
        @NotBlank @Size(max = 128) String recordId,
        @NotNull PrintScene scene,
        @Size(max = 128) String taskId,
        @Size(max = 128) String processInstanceId,
        @Positive Long processRunId,
        @Size(max = 50) Map<@Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,79}") String, Object> params) {

    public PrintRecordRequest {
        params = params == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    public PrintRecordRequest(PrintSourceRequest source, String recordId, PrintScene scene,
                              String taskId, String processInstanceId, Long processRunId) {
        this(source, recordId, scene, taskId, processInstanceId, processRunId, Map.of());
    }

    @JsonIgnore
    @AssertTrue(message = "打印场景与流程身份不匹配")
    public boolean isSceneValid() {
        if (scene == null) {
            return false;
        }
        return switch(scene) {
            case LIST, DETAIL ->
                taskId == null && processInstanceId == null && processRunId == null;
            case FLOW_TODO, FLOW_DONE ->
                taskId != null && !taskId.isBlank() && processInstanceId != null && !processInstanceId.isBlank();
            case FLOW_STARTED ->
                taskId == null && processInstanceId != null && !processInstanceId.isBlank();
        };
    }
}
