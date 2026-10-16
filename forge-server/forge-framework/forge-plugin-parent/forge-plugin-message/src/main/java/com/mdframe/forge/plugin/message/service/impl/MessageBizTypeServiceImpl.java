package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageBizType;
import com.mdframe.forge.plugin.message.mapper.SysMessageBizTypeMapper;
import com.mdframe.forge.plugin.message.service.MessageBizTypeService;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

@Service
public class MessageBizTypeServiceImpl extends ServiceImpl<SysMessageBizTypeMapper, SysMessageBizType>
        implements MessageBizTypeService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SysMessageBizType entity) {
        // 字段非空由 Controller @Valid + 实体注解保证
        return super.save(entity);
    }

    @Override
    public Page<SysMessageBizType> selectBizTypePage(Integer pageNum, Integer pageSize,
        String bizType, String bizName, Integer enabled) {
        return baseMapper.selectBizTypePage(
            new Page<>(pageNum, pageSize), bizType, bizName, enabled);
    }

    @Override
    public List<SysMessageBizType> listEnabled() {
        return baseMapper.selectEnabledBizTypes();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SysMessageBizType entity) {
        if (entity == null || entity.getId() == null) {
            throw new BusinessException("业务类型不存在");
        }
        SysMessageBizType existing = getById(entity.getId());
        if (existing == null) {
            throw new BusinessException("业务类型不存在");
        }
        MessageBizTypeAccessPolicy.assertMutable(existing, entity.getBizType());
        return super.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        SysMessageBizType existing = getById(id);
        if (existing == null) {
            throw new BusinessException("业务类型不存在");
        }
        MessageBizTypeAccessPolicy.assertDeletable(existing);
        return super.removeById(id);
    }
}
