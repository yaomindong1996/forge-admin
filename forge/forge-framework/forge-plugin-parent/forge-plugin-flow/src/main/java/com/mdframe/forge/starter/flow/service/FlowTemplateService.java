package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowTemplate;

import java.util.List;

/**
 * 流程模板服务接口
 */
public interface FlowTemplateService extends IService<FlowTemplate> {

    /**
     * 分页查询流程模板
     *
     * @param page       分页参数
     * @param templateName 模板名称
     * @param category   分类
     * @param status     状态
     * @return 分页结果
     */
    IPage<FlowTemplate> pageTemplate(Page<FlowTemplate> page, String templateName, String category, Integer status);

    /**
     * 获取启用的模板列表
     *
     * @param category 分类（可选）
     * @return 模板列表
     */
    List<FlowTemplate> getEnabledTemplates(String category);

    /**
     * 获取模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    FlowTemplate getTemplateDetail(String id);

    /**
     * 根据模板Key获取详情
     *
     * @param templateKey 模板Key
     * @return 模板详情
     */
    FlowTemplate getTemplateByKey(String templateKey);

    /**
     * 创建模板
     *
     * @param template 模板信息
     * @return 创建后的模板
     */
    FlowTemplate createTemplate(FlowTemplate template);

    /**
     * 更新模板
     *
     * @param template 模板信息
     * @return 更新后的模板
     */
    FlowTemplate updateTemplate(FlowTemplate template);

    /**
     * 删除模板
     *
     * @param id 模板ID
     */
    void deleteTemplate(String id);

    /**
     * 启用模板
     *
     * @param id 模板ID
     */
    void enableTemplate(String id);

    /**
     * 禁用模板
     *
     * @param id 模板ID
     */
    void disableTemplate(String id);

    /**
     * 从模板创建流程模型
     *
     * @param templateKey 模板Key
     * @param modelName   新模型名称
     * @param modelKey    新模型Key（可选，不填自动生成）
     * @return 创建的模型ID
     */
    String createModelFromTemplate(String templateKey, String modelName, String modelKey);

    /**
     * 增加使用次数
     *
     * @param templateKey 模板Key
     */
    void incrementUsageCount(String templateKey);

    /**
     * 复制模板
     *
     * @param id 源模板ID
     * @param newName 新模板名称
     * @return 新模板
     */
    FlowTemplate copyTemplate(String id, String newName);
}