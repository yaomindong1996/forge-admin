package com.mdframe.forge.starter.outbound.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OutboundWhitelistSaveRequest {

    private Long id;

    @NotBlank(message = "出站场景不能为空")
    private String scene;

    @NotBlank(message = "协议不能为空")
    private String protocol;

    @NotBlank(message = "主机不能为空")
    @Size(max = 253, message = "主机长度不能超过253个字符")
    private String host;

    @NotNull(message = "起始端口不能为空")
    @Min(value = 1, message = "起始端口必须大于0")
    @Max(value = 65535, message = "起始端口不能超过65535")
    private Integer portStart;

    @NotNull(message = "结束端口不能为空")
    @Min(value = 1, message = "结束端口必须大于0")
    @Max(value = 65535, message = "结束端口不能超过65535")
    private Integer portEnd;

    @NotNull(message = "私网例外标志不能为空")
    @Min(value = 0, message = "私网例外标志不合法")
    @Max(value = 1, message = "私网例外标志不合法")
    private Integer allowPrivate;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "状态不合法")
    private Integer status;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;
}
