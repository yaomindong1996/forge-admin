package com.mdframe.forge.starter.flow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 当前租户内批量调整流程模型排序。 */
@Data
public class FlowModelSortDTO {

    @Valid
    @NotEmpty(message = "排序项不能为空")
    private List<FlowModelSortItemDTO> items;
}
