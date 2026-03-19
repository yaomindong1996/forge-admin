package com.mdframe.forge.starter.flow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.starter.flow.entity.FlowForm;
import com.mdframe.forge.starter.flow.mapper.FlowFormMapper;
import com.mdframe.forge.starter.flow.service.FlowFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 流程表单定义服务实现
 * 
 * @author forge
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowFormServiceImpl extends ServiceImpl<FlowFormMapper, FlowForm> implements FlowFormService {

    @Override
    public Page<FlowForm> getPage(String formName, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<FlowForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(formName), FlowForm::getFormName, formName)
                .eq(status != null, FlowForm::getStatus, status)
                .orderByDesc(FlowForm::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<FlowForm> getEnabledForms() {
        LambdaQueryWrapper<FlowForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowForm::getStatus, 1)
                .orderByAsc(FlowForm::getFormName);
        return list(wrapper);
    }

    @Override
    public FlowForm getByFormKey(String formKey) {
        LambdaQueryWrapper<FlowForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowForm::getFormKey, formKey);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createForm(FlowForm form) {
        // 检查Key是否存在
        if (checkFormKeyExists(form.getFormKey(), null)) {
            throw new RuntimeException("表单Key已存在: " + form.getFormKey());
        }
        
        // 设置初始值
        form.setVersion(1);
        form.setStatus(1);
        
        return save(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateForm(FlowForm form) {
        FlowForm existing = getById(form.getId());
        if (existing == null) {
            throw new RuntimeException("表单不存在");
        }
        
        // 不允许修改formKey
        form.setFormKey(existing.getFormKey());
        
        // 如果更新了Schema，增加版本号
        if (form.getFormSchema() != null && !form.getFormSchema().equals(existing.getFormSchema())) {
            form.setVersion(existing.getVersion() + 1);
        } else {
            form.setVersion(existing.getVersion());
        }
        
        return updateById(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteForm(Long id) {
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableForm(Long id) {
        FlowForm form = new FlowForm();
        form.setId(id);
        form.setStatus(1);
        return updateById(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableForm(Long id) {
        FlowForm form = new FlowForm();
        form.setId(id);
        form.setStatus(0);
        return updateById(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyForm(Long id, String newName) {
        FlowForm source = getById(id);
        if (source == null) {
            throw new RuntimeException("源表单不存在");
        }
        
        // 生成新的formKey
        String newFormKey = source.getFormKey() + "_copy_" + System.currentTimeMillis();
        
        FlowForm newForm = new FlowForm();
        newForm.setFormKey(newFormKey);
        newForm.setFormName(newName);
        newForm.setFormType(source.getFormType());
        newForm.setFormSchema(source.getFormSchema());
        newForm.setFormUrl(source.getFormUrl());
        newForm.setComponentPath(source.getComponentPath());
        newForm.setFormConfig(source.getFormConfig());
        newForm.setVersion(1);
        newForm.setStatus(1);
        newForm.setDescription(source.getDescription());
        
        save(newForm);
        
        return newForm.getId();
    }

    @Override
    public boolean checkFormKeyExists(String formKey, Long excludeId) {
        LambdaQueryWrapper<FlowForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowForm::getFormKey, formKey);
        if (excludeId != null) {
            wrapper.ne(FlowForm::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFormSchema(Long id, String formSchema) {
        FlowForm existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("表单不存在");
        }
        
        FlowForm form = new FlowForm();
        form.setId(id);
        form.setFormSchema(formSchema);
        form.setVersion(existing.getVersion() + 1);
        
        return updateById(form);
    }

    @Override
    public String getFormSchema(String formKey) {
        FlowForm form = getByFormKey(formKey);
        return form != null ? form.getFormSchema() : null;
    }
}