package com.mdframe.forge.starter.crypto.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.mdframe.forge.starter.crypto.crypto.CryptoField;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 字段解密反序列化器
 */
@Slf4j
public class CryptoFieldDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {

    private EncryptorFactory encryptorFactory;
    private CryptoField annotation;

    public CryptoFieldDeserializer() {
    }

    public CryptoFieldDeserializer(EncryptorFactory encryptorFactory, CryptoField annotation) {
        this.encryptorFactory = encryptorFactory;
        this.annotation = annotation;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        if (value == null) {
            return null;
        }

        if (annotation == null || !annotation.decrypt() || encryptorFactory == null) {
            return value;
        }

        try {
            Encryptor encryptor = encryptorFactory.getEncryptor(annotation.algorithm());
            return encryptor.decrypt(value);
        } catch (Exception e) {
            log.error("字段解密失败", e);
            return value;
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            CryptoField annotation = property.getAnnotation(CryptoField.class);
            if (annotation != null && annotation.decrypt()) {
                EncryptorFactory factory = (EncryptorFactory) ctxt.getAttribute(EncryptorFactory.class);
                if (factory != null) {
                    return new CryptoFieldDeserializer(factory, annotation);
                }
            }
        }
        return this;
    }
}
