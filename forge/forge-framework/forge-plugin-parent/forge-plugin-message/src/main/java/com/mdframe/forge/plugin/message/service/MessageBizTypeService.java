package com.mdframe.forge.plugin.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageBizType;

import java.util.List;

public interface MessageBizTypeService extends IService<SysMessageBizType> {
    
    /**
     * 获取所有启用的业务类型
     */
    List<SysMessageBizType> listEnabled();
}