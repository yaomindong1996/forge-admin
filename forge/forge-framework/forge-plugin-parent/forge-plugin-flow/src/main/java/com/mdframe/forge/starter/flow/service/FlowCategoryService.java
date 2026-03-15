package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowCategory;

import java.util.List;

/**
 * 流程分类服务接口
 */
public interface FlowCategoryService extends IService<FlowCategory> {

    /**
     * 获取所有启用的分类
     */
    List<FlowCategory> listEnabled();

    /**
     * 根据编码获取分类
     */
    FlowCategory getByCode(String categoryCode);

    /**
     * 创建分类
     */
    FlowCategory createCategory(FlowCategory category);

    /**
     * 更新分类
     */
    FlowCategory updateCategory(FlowCategory category);

    /**
     * 删除分类
     */
    void deleteCategory(String id);

    /**
     * 启用分类
     */
    void enableCategory(String id);

    /**
     * 禁用分类
     */
    void disableCategory(String id);
}