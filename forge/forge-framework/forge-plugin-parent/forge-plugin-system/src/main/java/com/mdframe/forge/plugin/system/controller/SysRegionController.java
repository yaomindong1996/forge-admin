package com.mdframe.forge.plugin.system.controller;

import com.mdframe.forge.plugin.system.dto.SysRegionDTO;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.service.ISysRegionService;
import com.mdframe.forge.plugin.system.vo.SysRegionTreeVO;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行政区划管理Controller
 */
@RestController
@RequestMapping("/system/region")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysRegionController {

    private final ISysRegionService regionService;

    /**
     * 获取行政区划树形结构
     */
    @GetMapping("/tree")
    public RespInfo<List<SysRegionTreeVO>> tree() {
        List<SysRegionTreeVO> tree = regionService.selectRegionTree();
        return RespInfo.success(tree);
    }

    /**
     * 根据code获取详情
     */
    @GetMapping("/{code}")
    public RespInfo<SysRegion> getByCode(@PathVariable String code) {
        SysRegion region = regionService.selectRegionByCode(code);
        return RespInfo.success(region);
    }

    /**
     * 新增行政区划
     */
    @PostMapping("/")
    public RespInfo<Void> add(@RequestBody SysRegionDTO dto) {
        boolean result = regionService.insertRegion(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 更新行政区划
     */
    @PutMapping("/")
    public RespInfo<Void> edit(@RequestBody SysRegionDTO dto) {
        boolean result = regionService.updateRegion(dto);
        return result ? RespInfo.success() : RespInfo.error("更新失败");
    }

    /**
     * 删除行政区划
     */
    @DeleteMapping("/{code}")
    public RespInfo<Void> remove(@PathVariable String code) {
        boolean result = regionService.deleteRegionByCode(code);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 获取子级列表
     */
    @GetMapping("/children/{parentCode}")
    public RespInfo<List<SysRegion>> getChildren(@PathVariable String parentCode) {
        List<SysRegion> children = regionService.selectChildrenByParentCode(parentCode);
        return RespInfo.success(children);
    }

    /**
     * 搜索行政区划
     */
    @GetMapping("/search")
    public RespInfo<List<SysRegion>> search(@RequestParam String name) {
        List<SysRegion> regions = regionService.searchRegionByName(name);
        return RespInfo.success(regions);
    }
}