package com.mdframe.forge.plugin.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageBizType;
import com.mdframe.forge.plugin.message.mapper.SysMessageBizTypeMapper;
import com.mdframe.forge.plugin.message.service.MessageBizTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageBizTypeServiceImpl extends ServiceImpl<SysMessageBizTypeMapper, SysMessageBizType> implements MessageBizTypeService {
    
    @Override
    public List<SysMessageBizType> listEnabled() {
        return list(new LambdaQueryWrapper<SysMessageBizType>()
            .eq(SysMessageBizType::getEnabled, 1)
            .orderByAsc(SysMessageBizType::getSort));
    }
}