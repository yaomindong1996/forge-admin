package com.mdframe.forge.plugin.generator.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.generator.domain.entity.GenTemplate;

import java.util.List;

/**
 * 代码生成模板配置Service接口
 */
public interface IGenTemplateService extends IService<GenTemplate> {

    /**
     * 根据模板引擎查询可用模板列表
     *
     * @param engine 模板引擎类型
     * @return 模板列表
     */
    List<GenTemplate> listByEngine(String engine);

    /**
     * 预览模板渲染结果
     *
     * @param templateId 模板ID
     * @param tableId    表ID
     * @return 渲染后的代码内容
     */
    String previewTemplate(Long templateId, Long tableId);

    /**
     * 获取模板类型枚举列表
     *
     * @return 模板类型列表
     */
    List<String> listTemplateTypes();
}
