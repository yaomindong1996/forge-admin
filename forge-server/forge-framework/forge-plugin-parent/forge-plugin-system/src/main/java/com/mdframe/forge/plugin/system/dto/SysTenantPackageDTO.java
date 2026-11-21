package com.mdframe.forge.plugin.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SysTenantPackageDTO implements Serializable {

    private Long id;

    @NotBlank(message = "套餐编码不能为空")
    private String packageCode;

    @NotBlank(message = "套餐名称不能为空")
    private String packageName;

    private Integer packageStatus;

    private Integer isDefault;

    private String description;

    private Integer sort;

    private String remark;

    @Valid
    private List<SysTenantPackageItemDTO> items = new ArrayList<>();
}
