package com.mdframe.forge.plugin.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.system.dto.SysResourceDTO;
import com.mdframe.forge.plugin.system.dto.SysResourceQuery;
import com.mdframe.forge.plugin.system.entity.SysResource;
import com.mdframe.forge.plugin.system.service.ISysResourceService;
import com.mdframe.forge.starter.core.annotation.api.ApiPermissionIgnore;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源管理Controller
 */
@RestController
@RequestMapping("/system/resource")
@RequiredArgsConstructor
@ApiDecrypt
@ApiEncrypt
@ApiPermissionIgnore
public class SysResourceController {

    private final ISysResourceService resourceService;

    /**
     * 分页查询资源列表
     */
    @GetMapping("/page")
    public RespInfo<IPage<SysResource>> page(SysResourceQuery query) {
        IPage<SysResource> page = resourceService.selectResourcePage(query);
        return RespInfo.success(page);
    }

    /**
     * 查询资源树形列表
     */
    @GetMapping("/tree")
    public RespInfo<List<SysResource>> tree(SysResourceQuery query) {
        List<SysResource> list = resourceService.selectResourceTree(query);
        return RespInfo.success(list);
    }

    /**
     * 根据ID查询资源详情
     */
    @PostMapping("/getById")
    public RespInfo<SysResource> getById(@RequestParam Long id) {
        SysResource resource = resourceService.selectResourceById(id);
        return RespInfo.success(resource);
    }

    /**
     * 新增资源
     */
    @PostMapping("/add")
    @OperationLog(module = "资源管理", type = OperationType.ADD, desc = "新增资源")
    public RespInfo<Void> add(@RequestBody SysResourceDTO dto) {
        boolean result = resourceService.insertResource(dto);
        return result ? RespInfo.success() : RespInfo.error("新增失败");
    }

    /**
     * 修改资源
     */
    @PostMapping("/edit")
    @OperationLog(module = "资源管理", type = OperationType.UPDATE, desc = "修改资源")
    public RespInfo<Void> edit(@RequestBody SysResourceDTO dto) {
        boolean result = resourceService.updateResource(dto);
        return result ? RespInfo.success() : RespInfo.error("修改失败");
    }

    /**
     * 删除资源
     */
    @PostMapping("/remove")
    @OperationLog(module = "资源管理", type = OperationType.DELETE, desc = "删除资源")
    public RespInfo<Void> remove(@RequestParam Long id) {
        boolean result = resourceService.deleteResourceById(id);
        return result ? RespInfo.success() : RespInfo.error("删除失败");
    }

}
