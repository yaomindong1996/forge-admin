package com.mdframe.forge.flow.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 动态加签/减签请求。动作只允许当前任务参与人发起，目标用户必须属于当前租户。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowTaskSignDTO extends FlowTaskActionDTO {

    private String targetUserId;

    /** BEFORE/AFTER/PARALLEL；未传时按 PARALLEL 记录。 */
    private String signMode;
}
