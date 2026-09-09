package com.mdframe.forge.starter.flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改常用审批意见。 */
@Data
public class FlowCommentPhraseUpdateDTO {

    @NotNull(message = "意见ID不能为空")
    private Long id;

    @NotBlank(message = "审批意见不能为空")
    @Size(max = 200, message = "审批意见不能超过200字")
    private String content;

    @NotBlank(message = "请选择适用场景")
    private String scene;

    private Integer sortOrder;

    private Integer status;
}
