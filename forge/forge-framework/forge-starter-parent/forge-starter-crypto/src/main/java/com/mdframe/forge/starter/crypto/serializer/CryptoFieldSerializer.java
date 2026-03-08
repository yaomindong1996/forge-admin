package com.mdframe.forge.starter.crypto.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.mdframe.forge.starter.crypto.crypto.CryptoField;
import com.mdframe.forge.starter.crypto.crypto.Encryptor;
import com.mdframe.forge.starter.crypto.crypto.EncryptorFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 字段加密序列化器
 */
@Slf4j
public class CryptoFieldSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private EncryptorFactory encryptorFactory;
    private CryptoField annotation;

    public CryptoFieldSerializer() {
    }

    public CryptoFieldSerializer(EncryptorFactory encryptorFactory, CryptoField annotation) {
        this.encryptorFactory = encryptorFactory;
        this.annotation = annotation;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (annotation == null || !annotation.encrypt() || encryptorFactory == null) {
            gen.writeString(value);
            return;
        }

        try {
            Encryptor encryptor = encryptorFactory.getEncryptor(annotation.algorithm());
            String encryptedValue = encryptor.encrypt(value);
            gen.writeString(encryptedValue);
        } catch (Exception e) {
            log.error("字段加密失败", e);
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            CryptoField annotation = property.getAnnotation(CryptoField.class);
            if (annotation != null && annotation.encrypt()) {
                EncryptorFactory factory = (EncryptorFactory) prov.getAttribute(EncryptorFactory.class);
                if (factory != null) {
                    return new CryptoFieldSerializer(factory, annotation);
                }
            }
        }
        return this;
    }
}
