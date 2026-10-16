package com.mdframe.forge.starter.flow.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.starter.flow.dto.FormFieldCatalogItemDTO;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.entity.FlowFormVersion;

import java.util.List;

/**
 * 流程表单定义服务接口
 * 
 * @author forge
 */
public interface FlowFormService extends IService<FlowForm> {

    /**
     * 分页查询表单定义
     *
     * @param formName 表单名称
     * @param status   状态
     * @param page     页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<FlowForm> getPage(String formName, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 获取所有启用的表单定义
     *
     * @return 表单定义列表
     */
    List<FlowForm> getEnabledForms();

    /**
     * 根据表单Key获取表单定义
     *
     * @param formKey 表单Key
     * @return 表单定义
     */
    FlowForm getByFormKey(String formKey);

    /**
     * 创建表单定义
     *
     * @param form 表单定义
     * @return 是否成功
     */
    boolean createForm(FlowForm form);

    /**
     * 更新表单定义
     *
     * @param form 表单定义
     * @return 是否成功
     */
    boolean updateForm(FlowForm form);

    /**
     * 删除表单定义
     *
     * @param id 表单ID
     * @return 是否成功
     */
    boolean deleteForm(Long id);

    /**
     * 启用表单
     *
     * @param id 表单ID
     * @return 是否成功
     */
    boolean enableForm(Long id);

    /**
     * 禁用表单
     *
     * @param id 表单ID
     * @return 是否成功
     */
    boolean disableForm(Long id);

    /**
     * 复制表单
     *
     * @param id      原表单ID
     * @param newName 新表单名称
     * @return 新表单ID
     */
    Long copyForm(Long id, String newName);

    /**
     * 检查表单Key是否存在
     *
     * @param formKey   表单Key
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean checkFormKeyExists(String formKey, Long excludeId);

    /**
     * 更新表单Schema
     *
     * @param id         表单ID
     * @param formSchema 表单Schema（JSON）
     * @return 是否成功
     */
    boolean updateFormSchema(Long id, String formSchema);

    /**
     * 获取表单Schema
     *
     * @param formKey 表单Key
     * @return 表单Schema
     */
    String getFormSchema(String formKey);

    /**
     * 发布表单不可变版本。
     */
    FlowFormVersion publishVersion(Long formId);

    /**
     * 查询表单发布版本列表。
     */
    List<FlowFormVersion> listVersions(Long formId);

    /**
     * 查询表单字段目录。
     */
    List<FormFieldCatalogItemDTO> resolveFieldCatalog(String formKey, Long versionId);

    /**
     * 解析表单 Schema 得到字段目录 JSON。
     */
    String buildFieldRegistryJson(String formSchema);
}
