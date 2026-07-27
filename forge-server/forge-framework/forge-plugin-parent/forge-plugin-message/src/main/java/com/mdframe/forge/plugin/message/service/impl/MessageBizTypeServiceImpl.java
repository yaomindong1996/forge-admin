package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageBizType;
import com.mdframe.forge.plugin.message.mapper.SysMessageBizTypeMapper;
import com.mdframe.forge.plugin.message.service.MessageBizTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageBizTypeServiceImpl extends ServiceImpl<SysMessageBizTypeMapper, SysMessageBizType> implements MessageBizTypeService {

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
}
