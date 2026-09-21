package com.mdframe.forge.plugin.print.spi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mdframe.forge.plugin.print.enums.PrintScene;

public record PrintRecordRequest(@Valid @NotNull PrintSourceRequest source, @NotBlank @Size(max = 128) String recordId, @NotNull PrintScene scene, @Size(max = 128) String taskId, @Size(max = 128) String processInstanceId, @Positive Long processRunId) {

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
