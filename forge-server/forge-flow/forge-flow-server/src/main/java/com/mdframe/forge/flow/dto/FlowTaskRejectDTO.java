package com.mdframe.forge.flow.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批驳回请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FlowTaskRejectDTO extends FlowTaskActionDTO {

    private Long tenantId;

}
