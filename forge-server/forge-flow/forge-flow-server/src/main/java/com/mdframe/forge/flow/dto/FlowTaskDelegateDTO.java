package com.mdframe.forge.flow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务转办请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowTaskDelegateDTO extends FlowTaskActionDTO {

    @NotBlank(message = "转办人ID不能为空")
    private String targetUserId;
}
