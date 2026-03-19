package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowModel;

import java.util.List;
import java.util.Map;

/**
 * 流程模型服务接口
 */
public interface FlowModelService extends IService<FlowModel> {

    /**
     * 分页查询流程模型
     */
    IPage<FlowModel> pageFlowModel(Page<FlowModel> page, String modelName, String category, Integer status);

    /**
     * 获取启用的模型列表
     */
    List<FlowModel> getEnabledModels(String category);

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
     * 挂起流程模型
     */
    void suspendModel(String id);

    /**
     * 激活流程模型
     */
    void activateModel(String id);

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

    /**
     * 根据ModelKey获取模型
     */
    FlowModel getModelByKey(String modelKey);

    /**
     * 获取模型版本历史
     */
    List<Map<String, Object>> getModelVersions(String modelKey);

    /**
     * 导入BPMN模型
     */
    FlowModel importModel(String bpmnXml, String modelName, String category);

    /**
     * 导出BPMN XML
     */
    String exportModel(String id);

    /**
     * 复制模型
     */
    FlowModel copyModel(String id, String newName);

    /**
     * 检查模型Key是否存在
     */
    boolean checkModelKeyExists(String modelKey, String excludeId);
}