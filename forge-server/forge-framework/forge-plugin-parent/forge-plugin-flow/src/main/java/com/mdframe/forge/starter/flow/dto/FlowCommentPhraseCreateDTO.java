package com.mdframe.forge.starter.flow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增常用审批意见。 */
@Data
public class FlowCommentPhraseCreateDTO {

    @NotBlank(message = "审批意见不能为空")
    @Size(max = 200, message = "审批意见不能超过200字")
    private String content;

    @NotBlank(message = "请选择适用场景")
    private String scene;

    /** 0 企业常用，1 我的常用；缺省按个人意见处理。 */
    private Integer ownerType;

    private Integer sortOrder;

    private Integer status;
}
