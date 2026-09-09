package com.mdframe.forge.starter.flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 单个流程模型的排序项。排序值越小越靠前。 */
@Data
public class FlowModelSortItemDTO {

    @NotBlank(message = "模型ID不能为空")
    private String modelId;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;
}
