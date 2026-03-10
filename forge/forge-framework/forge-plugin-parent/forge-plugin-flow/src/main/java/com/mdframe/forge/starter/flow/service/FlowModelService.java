package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowModel;

/**
 * 流程模型服务接口
 */
public interface FlowModelService extends IService<FlowModel> {

    /**
     * 分页查询流程模型
     */
    IPage<FlowModel> pageFlowModel(Page<FlowModel> page, String modelName, String category, Integer status);

    /**
     * 创建流程模型
     */
    FlowModel createModel(FlowModel flowModel);

    /**
     * 更新流程模型
     */
    FlowModel updateModel(FlowModel flowModel);

    /**
     * 删除流程模型
     */
    void deleteModel(String id);

    /**
     * 部署流程模型
     */
    String deployModel(String id);

    /**
     * 禁用流程模型
     */
    void disableModel(String id);

    /**
     * 启用流程模型
     */
    void enableModel(String id);

    /**
     * 获取模型详情
     */
    FlowModel getModelDetail(String id);
}