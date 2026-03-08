package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.SysOrgDTO;
import com.mdframe.forge.plugin.system.dto.SysOrgQuery;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.service.ISysOrgService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织管理Controller
 */
@RestController
@RequestMapping("/system/org")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysOrgController {

    private final ISysOrgService orgService;

    /**
     * 分页查询组织列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysOrg>> page(SysOrgQuery query) {
        IPage<SysOrg> page = orgService.selectOrgPage(query);
        return RespInfo.success(page);
    }

    /**
     * 查询组织树形列表
     */
    @GetMapping("/tree")
    public RespInfo<List<SysOrg>> tree(SysOrgQuery query) {
        List<SysOrg> list = orgService.selectOrgTree(query);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询组织详情
     */
    @PostMapping("/getById")
    public RespInfo<SysOrg> getById(@RequestParam Long id) {
        SysOrg org = orgService.selectOrgById(id);
        return RespInfo.success(org);
    }

    /**
     * 新增组织
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysOrgDTO dto) {
        boolean result = orgService.insertOrg(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改组织
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysOrgDTO dto) {
        boolean result = orgService.updateOrg(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除组织
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = orgService.deleteOrgById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }
}
