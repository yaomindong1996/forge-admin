package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysDictTypeDTO;
import com.mdframe.forge.plugin.system.dto.SysDictTypeQuery;
import com.mdframe.forge.plugin.system.entity.SysDictType;
import com.mdframe.forge.plugin.system.service.ISysDictTypeService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典类型Controller
 */
@RestController
@RequestMapping("/system/dict/type")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysDictTypeController {

    private final ISysDictTypeService dictTypeService;

    /**
     * 分页查询字典类型列表
     */
    @GetMapping("/page")
    public RespInfo<Page<SysDictType>> page(PageQuery pageQuery, SysDictTypeQuery query) {
        Page<SysDictType> page = dictTypeService.selectDictTypePage(pageQuery, query);
        return RespInfo.success(page);
    }

    /**
     * 查询字典类型列表
     */
    @GetMapping("/list")
    public RespInfo<List<SysDictType>> list(SysDictTypeQuery query) {
        List<SysDictType> list = dictTypeService.selectDictTypeList(query);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询字典类型详情
     */
    @PostMapping("/getById")
    public RespInfo<SysDictType> getById(@RequestParam Long dictId) {
        SysDictType dictType = dictTypeService.selectDictTypeById(dictId);
        return RespInfo.success(dictType);
    }

    /**
     * 新增字典类型
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysDictTypeDTO dto) {
        boolean result = dictTypeService.insertDictType(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改字典类型
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysDictTypeDTO dto) {
        boolean result = dictTypeService.updateDictType(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除字典类型
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long dictId) {
        boolean result = dictTypeService.deleteDictTypeById(dictId);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除字典类型
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] dictIds) {
        boolean result = dictTypeService.deleteDictTypeByIds(dictIds);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
