package com.mdframe.forge.starter.file.spi;

import com.mdframe.forge.starter.file.model.FileMetadata;

/**
 * 文件元数据持久化SPI
 * 由业务模块实现，负责文件元数据的存储
 */
public interface FileMetadataPersistence {
    
    /**
     * 保存文件元数据
     */
    void save(FileMetadata metadata);
    
    /**
     * 根据文件ID查询元数据
     */
    FileMetadata getById(String fileId);
    
    /**
     * 根据MD5查询元数据（秒传）
     */
    FileMetadata getByMd5(String md5);
    
    /**
     * 更新下载次数
     */
    void incrementDownloadCount(String fileId);
    
    /**
     * 删除文件元数据
     */
    void delete(String fileId);
    
    /**
     * 检查文件权限
     */
    boolean checkPermission(String fileId, Long userId);
}
