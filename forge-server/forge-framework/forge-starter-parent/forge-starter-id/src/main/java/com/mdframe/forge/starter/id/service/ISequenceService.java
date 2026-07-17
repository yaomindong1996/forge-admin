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
     * 获取下一个序列ID，并在业务键首次初始化时使用指定起始值。
     *
     * @param bizKey 业务键
     * @param startValue 新业务键的首个返回值，必须大于等于0
     * @return 序列ID
     */
    long nextId(String bizKey, long startValue);

    /**
     * 获取下一个序列值，并在新 key 首次初始化时兼容旧编码规则已分配水位。
     *
     * @param bizKey 新序列键
     * @param startValue 配置的起始值
     * @param legacyKeyPrefix 旧序列键前缀；为空时不读取旧水位
     * @param legacyPeriod 旧序列周期；小时周期会兼容旧分钟/秒周期并取最大水位
     * @return 下一个序列值
     */
    default long nextId(String bizKey,
                        long startValue,
                        String legacyKeyPrefix,
                        String legacyPeriod) {
        return nextId(bizKey, startValue);
    }

    /**
     * 获取有限容量的下一个序列值。
     *
     * @param bizKey 业务键
     * @param startValue 配置的起始值
     * @param legacyKeyPrefix 旧序列键前缀
     * @param legacyPeriod 旧序列周期
     * @param allocationStep 单次数据库预分配步长
     * @param maxValue 允许返回的最大值，分配不得越过该上限
     * @return 下一个序列值
     */
    default long nextId(String bizKey,
                        long startValue,
                        String legacyKeyPrefix,
                        String legacyPeriod,
                        int allocationStep,
                        long maxValue) {
        return nextId(bizKey, startValue, legacyKeyPrefix, legacyPeriod);
    }

    /**
     * 解析兼容旧编码规则已分配水位后的首个安全值，不消耗序列。
     *
     * @param startValue 配置的起始值
     * @param legacyKeyPrefix 旧序列键前缀
     * @param legacyPeriod 旧序列周期
     * @return 不小于配置起始值的兼容起始值
     */
    default long resolveLegacyStartValue(long startValue,
                                         String legacyKeyPrefix,
                                         String legacyPeriod) {
        return startValue;
    }
    
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
