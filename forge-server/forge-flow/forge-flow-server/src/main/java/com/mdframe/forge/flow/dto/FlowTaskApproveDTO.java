package com.mdframe.forge.flow.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 审批通过请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowTaskApproveDTO extends FlowTaskActionDTO {

    /**
     * 额外流程变量，键值由流程模型动态决定。
     */
    private Map<String, Object> variables;

    private Long tenantId;

}
