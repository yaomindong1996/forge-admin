package com.mdframe.forge.plugin.system.vo;

import com.mdframe.forge.plugin.system.entity.SysTenantPackage;
import com.mdframe.forge.plugin.system.entity.SysTenantPackageItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysTenantPackageVO extends SysTenantPackage {

    private List<SysTenantPackageItem> items = new ArrayList<>();
}
