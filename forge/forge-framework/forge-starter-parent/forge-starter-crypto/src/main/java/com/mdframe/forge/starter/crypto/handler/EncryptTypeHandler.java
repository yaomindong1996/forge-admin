package com.mdframe.forge.starter.crypto.handler;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.MappedTypes;

import java.nio.charset.StandardCharsets;

@Slf4j
@MappedTypes({String.class})
public class EncryptTypeHandler extends AbstractJsonTypeHandler<String> {
    
    private static final String SECRET_KEY = "forge_client_secret_key_16b";
    private static final AES AES = SecureUtil.aes(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    
    public EncryptTypeHandler(Class<?> clazz) {
        super(clazz);
    }
    
    @Override
    public String parse(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return null;
            }
            return AES.decryptStr(json);
        } catch (Exception e) {
            log.warn("字段解密失败: {}", json, e);
            return json;
        }
    }
    
    @Override
    public String toJson(String obj) {
        try {
            if (obj == null || obj.isEmpty()) {
                return null;
            }
            return AES.encryptBase64(obj.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("字段加密失败: {}", obj, e);
            return obj;
        }
    }
}