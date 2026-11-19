package com.mdframe.forge.plugin.system.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SysTenantPackageQuery implements Serializable {

    private String packageName;

    private String packageCode;

    private Integer packageStatus;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
