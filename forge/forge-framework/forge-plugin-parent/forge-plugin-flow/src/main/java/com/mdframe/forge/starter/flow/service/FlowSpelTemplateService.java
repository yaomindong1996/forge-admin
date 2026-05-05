package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.starter.flow.entity.FlowSpelTemplate;

import java.util.List;

/**
 * SPEL表达式模板服务接口
 *
 * @author forge
 */
public interface FlowSpelTemplateService extends IService<FlowSpelTemplate> {

    /**
     * 分页查询模板
     *
     * @param templateName 模板名称
     * @param category     分类
     * @param status       状态
     * @param page         页码
     * @param pageSize     每页大小
     * @return 分页结果
     */
    Page<FlowSpelTemplate> getPage(String templateName, String category, Integer status, Integer page, Integer pageSize);

    /**
     * 获取启用状态的模板列表
     *
     * @return 模板列表
     */
    List<FlowSpelTemplate> getEnabledList();

    /**
     * 创建模板
     *
     * @param template 模板
     * @return 是否成功
     */
    boolean createTemplate(FlowSpelTemplate template);

    /**
     * 更新模板
     *
     * @param template 模板
     * @return 是否成功
     */
    boolean updateTemplate(FlowSpelTemplate template);

    /**
     * 删除模板
     *
     * @param id 模板ID
     * @return 是否成功
     */
    boolean deleteTemplate(Long id);

    /**
     * 启用模板
     *
     * @param id 模板ID
     * @return 是否成功
     */
    boolean enableTemplate(Long id);

    /**
     * 禁用模板
     *
     * @param id 模板ID
     * @return 是否成功
     */
    boolean disableTemplate(Long id);
}