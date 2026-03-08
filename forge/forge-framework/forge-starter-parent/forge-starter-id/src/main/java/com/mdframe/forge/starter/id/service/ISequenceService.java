package com.mdframe.forge.starter.id.service;

/**
 * 基于业务类型的序列生成服务
 */
public interface ISequenceService {
    
    /**
     * 获取下一个序列ID（纯数字）
     * 
     * @param bizKey 业务键（如：order、user、product）
     * @return 序列ID
     */
    long nextId(String bizKey);
    
    /**
     * 批量获取序列ID
     * 
     * @param bizKey 业务键
     * @param size 批量大小
     * @return ID数组
     */
    long[] nextBatch(String bizKey, int size);
    
    /**
     * 获取格式化的序列号（支持前缀、日期、补零）
     * 例如：ORD20251202000001
     * 
     * @param bizKey 业务键
     * @return 格式化序列号
     */
    String nextFormatted(String bizKey);
    
    /**
     * 批量获取格式化序列号
     * 
     * @param bizKey 业务键
     * @param size 批量大小
     * @return 格式化序列号数组
     */
    String[] nextFormattedBatch(String bizKey, int size);
}
