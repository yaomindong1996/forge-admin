package com.mdframe.forge.plugin.message.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mdframe.forge.plugin.message.domain.entity.SysMessageTemplate;

/**
 * 消息模板服务接口
 */
public interface MessageTemplateService {
    
    /**
     * 创建模板
     */
    void create(SysMessageTemplate template);
    
    /**
     * 更新模板
     */
    void update(SysMessageTemplate template);
    
    /**
     * 删除模板
     */
    void delete(Long id);
    
    /**
     * 根据ID查询
     */
    SysMessageTemplate getById(Long id);
    
    /**
     * 根据模板编码查询
     */
    SysMessageTemplate getByCode(String templateCode);
    
    /**
     * 分页查询模板列表
     */
    IPage<SysMessageTemplate> page(String type, String keyword, Integer pageNum, Integer pageSize);
}
