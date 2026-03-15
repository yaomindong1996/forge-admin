package com.mdframe.forge.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.core.domain.RespInfo;
import com.mdframe.forge.starter.flow.entity.FlowCategory;
import com.mdframe.forge.starter.flow.service.FlowCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程分类管理接口
 */
@RestController
@RequestMapping("/api/flow/category")
@RequiredArgsConstructor
public class FlowCategoryController {

    private final FlowCategoryService flowCategoryService;

    /**
     * 获取所有启用的分类（下拉选择用）
     */
    @GetMapping("/enabled")
    public RespInfo<List<FlowCategory>> listEnabled() {
        List<FlowCategory> categories = flowCategoryService.listEnabled();
        return RespInfo.success(categories);
    }

    /**
     * 分页查询分类
     */
    @GetMapping("/page")
    public RespInfo<IPage<FlowCategory>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Integer status) {
        
        Page<FlowCategory> page = new Page<>(pageNum, pageSize);
        List<FlowCategory> list = flowCategoryService.list();
        
        list = list.stream()
                .filter(c -> categoryName == null || c.getCategoryName().contains(categoryName))
                .filter(c -> status == null || c.getStatus().equals(status))
                .toList();
        
        int start = (int) ((pageNum - 1) * pageSize);
        int end = Math.min(start + (int) pageSize, list.size());
        List<FlowCategory> pageList = list.subList(start, end);
        
        page.setRecords(pageList);
        page.setTotal(list.size());
        
        return RespInfo.success(page);
    }

    /**
     * 获取分类详情
     */
    @GetMapping("/{id}")
    public RespInfo<FlowCategory> getById(@PathVariable String id) {
        FlowCategory category = flowCategoryService.getById(id);
        return RespInfo.success(category);
    }

    /**
     * 根据编码获取分类
     */
    @GetMapping("/code/{code}")
    public RespInfo<FlowCategory> getByCode(@PathVariable String code) {
        FlowCategory category = flowCategoryService.getByCode(code);
        return RespInfo.success(category);
    }

    /**
     * 创建分类
     */
    @PostMapping
    public RespInfo<FlowCategory> create(@RequestBody FlowCategory category) {
        FlowCategory result = flowCategoryService.createCategory(category);
        return RespInfo.success("创建成功", result);
    }

    /**
     * 更新分类
     */
    @PutMapping
    public RespInfo<FlowCategory> update(@RequestBody FlowCategory category) {
        FlowCategory result = flowCategoryService.updateCategory(category);
        return RespInfo.success("更新成功", result);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public RespInfo<Void> delete(@PathVariable String id) {
        flowCategoryService.deleteCategory(id);
        return RespInfo.success("删除成功", null);
    }

    /**
     * 启用分类
     */
    @PostMapping("/{id}/enable")
    public RespInfo<Void> enable(@PathVariable String id) {
        flowCategoryService.enableCategory(id);
        return RespInfo.success("启用成功", null);
    }

    /**
     * 禁用分类
     */
    @PostMapping("/{id}/disable")
    public RespInfo<Void> disable(@PathVariable String id) {
        flowCategoryService.disableCategory(id);
        return RespInfo.success("禁用成功", null);
    }
}