package com.mdframe.forge.flow.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 撤回抄送请求。 */
@Data
public class FlowCcRevokeDTO {
    @Size(max = 500, message = "撤回原因不能超过500个字符")
    private String reason;
}
