package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class SysTenantPackageItemDTO implements Serializable {

    private Long id;

    @NotBlank(message = "项类型不能为空")
    private String itemType;

    @NotBlank(message = "项编码不能为空")
    private String itemCode;

    private String itemValue;

    private Integer sort;
}
