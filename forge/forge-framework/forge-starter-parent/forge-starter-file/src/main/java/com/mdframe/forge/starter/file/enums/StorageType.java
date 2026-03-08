package com.mdframe.forge.starter.file.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储策略枚举
 */
@Getter
@AllArgsConstructor
public enum StorageType {
    
    /**
     * 本地文件系统
     */
    LOCAL("local", "本地存储"),
    
    /**
     * MinIO对象存储
     */
    MINIO("minio", "MinIO存储"),
    
    /**
     * 阿里云OSS
     */
    ALIYUN_OSS("aliyun_oss", "阿里云OSS"),
    
    /**
     * 腾讯云COS
     */
    TENCENT_COS("tencent_cos", "腾讯云COS"),
    
    /**
     * 七牛云
     */
    QINIU("qiniu", "七牛云存储");
    
    private final String code;
    private final String desc;
    
    public static StorageType fromCode(String code) {
        for (StorageType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return LOCAL;
    }
}
