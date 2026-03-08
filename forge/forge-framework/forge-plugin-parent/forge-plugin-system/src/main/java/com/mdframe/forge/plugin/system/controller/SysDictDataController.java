package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.plugin.system.dto.SysDictDataDTO;
import com.mdframe.forge.plugin.system.dto.SysDictDataQuery;
import com.mdframe.forge.plugin.system.entity.SysDictData;
import com.mdframe.forge.plugin.system.service.ISysDictDataService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典数据Controller
 */
@RestController
@RequestMapping("/system/dict/data")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysDictDataController {

    private final ISysDictDataService dictDataService;

    /**
     * 分页查询字典数据列表
     */
    @GetMapping("/page")
    public RespInfo<Page<SysDictData>> page(PageQuery pageQuery, SysDictDataQuery query) {
        Page<SysDictData> page = dictDataService.selectDictDataPage(pageQuery, query);
        return RespInfo.success(page);
    }

    /**
     * 查询字典数据列表
     */
    @GetMapping("/list")
    public RespInfo<List<SysDictData>> list(SysDictDataQuery query) {
        List<SysDictData> list = dictDataService.selectDictDataList(query);
        return RespInfo.success(list);
    }

    /**
     * 根据字典类型查询字典数据
     */
    @GetMapping("/type/{dictType}")
    public RespInfo<List<SysDictData>> getByType(@PathVariable String dictType) {
        List<SysDictData> list = dictDataService.selectDictDataByType(dictType);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询字典数据详情
     */
    @PostMapping("/getById")
    public RespInfo<SysDictData> getById(@RequestParam Long dictCode) {
        SysDictData dictData = dictDataService.selectDictDataById(dictCode);
        return RespInfo.success(dictData);
    }

    /**
     * 新增字典数据
     */
    @PostMapping("/add")
    public RespInfo<Void> add(@RequestBody SysDictDataDTO dto) {
        boolean result = dictDataService.insertDictData(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改字典数据
     */
    @PostMapping("/edit")
    public RespInfo<Void> edit(@RequestBody SysDictDataDTO dto) {
        boolean result = dictDataService.updateDictData(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除字典数据
     */
    @PostMapping("/remove")
    public RespInfo<Void> remove(@RequestParam Long dictCode) {
        boolean result = dictDataService.deleteDictDataById(dictCode);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

    /**
     * 批量删除字典数据
     */
    @PostMapping("/removeBatch")
    public RespInfo<Void> removeBatch(@RequestBody Long[] dictCodes) {
        boolean result = dictDataService.deleteDictDataByIds(dictCodes);
        return result ? RespInfo.success() : RespInfo.error("批量删除失败");
    }
}
