package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageDTO;
import com.mdframe.forge.plugin.system.dto.SysTenantPackageQuery;
import com.mdframe.forge.plugin.system.entity.SysTenantPackage;
import com.mdframe.forge.plugin.system.service.ISysTenantPackageService;
import com.mdframe.forge.plugin.system.vo.SysTenantPackageVO;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system/tenant-package")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
public class SysTenantPackageController {

    private final ISysTenantPackageService packageService;

    @GetMapping("/page")
    public RespInfo<IPage<SysTenantPackage>> page(SysTenantPackageQuery query) {
        return RespInfo.success(packageService.selectPage(query));
    }

    @GetMapping("/{id}")
    public RespInfo<SysTenantPackageVO> detail(@PathVariable Long id) {
        return RespInfo.success(packageService.detail(id));
    }

    @GetMapping("/options")
    public RespInfo<List<SysTenantPackage>> options() {
        return RespInfo.success(packageService.options());
    }

    @PostMapping("/add")
    public RespInfo<Void> add(@Valid @RequestBody SysTenantPackageDTO dto) {
        return packageService.insertPackage(dto) ? RespInfo.success() : RespInfo.error("新增失败");
    }

    @PostMapping("/edit")
    public RespInfo<Void> edit(@Valid @RequestBody SysTenantPackageDTO dto) {
        return packageService.updatePackage(dto) ? RespInfo.success() : RespInfo.error("修改失败");
    }

    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        return packageService.deleteById(id) ? RespInfo.success() : RespInfo.error("删除失败");
    }

    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        return packageService.deleteByIds(ids) ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
