package com.mdframe.forge.plugin.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.starter.core.domain.PageQuery;

import java.util.List;

/**
 * 文件元数据Service
 */
public interface ISysFileMetadataService extends IService<SysFileMetadata> {
    
    /**
     * 分页查询
     */
    Page<SysFileMetadata> page(PageQuery query, SysFileMetadata condition);
    
    /**
     * 根据业务类型和业务ID查询
     */
    List<SysFileMetadata> listByBusiness(String businessType, String businessId);
    
    /**
     * 根据 fileId 字符串查询元数据
     */
    SysFileMetadata getByFileId(String fileId);

    /**
     * 根据 fileId 字符串删除（带所有权校验）
     */
    void removeByFileId(String fileId);

    /**
     * 批量删除
     */
    void removeBatch(String[] fileIds);

    /**
     * 重命名文件（仅修改 originalName）
     */
    void rename(String fileId, String originalName);
}
