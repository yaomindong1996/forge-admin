package com.mdframe.forge.starter.crypto.desensitize.serializer;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.mdframe.forge.starter.crypto.desensitize.annotation.Desensitize;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategy;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeStrategyFactory;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 字段脱敏序列化器
 *
 * @author forge
 */
@Slf4j
public class DesensitizeSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DesensitizeStrategyFactory strategyFactory;
    private Desensitize annotation;

    public DesensitizeSerializer() {
    }

    public DesensitizeSerializer(DesensitizeStrategyFactory strategyFactory, Desensitize annotation) {
        this.strategyFactory = strategyFactory;
        this.annotation = annotation;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (annotation == null || !annotation.enabled()) {
            gen.writeString(value);
            return;
        }

        try {
            String desensitizedValue = doDesensitize(value);
            gen.writeString(desensitizedValue);
        } catch (Exception e) {
            log.error("字段脱敏失败", e);
            gen.writeString(value);
        }
    }

    private String doDesensitize(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }

        DesensitizeType type = annotation.type();

        if (type == DesensitizeType.CUSTOM) {
            return customDesensitize(value);
        }

        if (strategyFactory != null && strategyFactory.hasStrategy(type)) {
            DesensitizeStrategy strategy = strategyFactory.getStrategy(type);
            return strategy.desensitize(value);
        }

        return value;
    }

    private String customDesensitize(String value) {
        int prefixKeep = annotation.prefixKeep();
        int suffixKeep = annotation.suffixKeep();
        char replaceChar = annotation.replaceChar();

        int length = value.length();

        if (prefixKeep + suffixKeep >= length) {
            return value;
        }

        String prefix = prefixKeep > 0 ? value.substring(0, prefixKeep) : "";
        String suffix = suffixKeep > 0 ? value.substring(length - suffixKeep) : "";
        int replaceLength = length - prefixKeep - suffixKeep;
        String replaceStr = StrUtil.repeat(replaceChar, replaceLength);

        return prefix + replaceStr + suffix;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Desensitize annotation = property.getAnnotation(Desensitize.class);
            if (annotation != null && annotation.enabled()) {
                DesensitizeStrategyFactory factory =
                        (DesensitizeStrategyFactory) prov.getAttribute(DesensitizeStrategyFactory.class);
                if (factory != null) {
                    return new DesensitizeSerializer(factory, annotation);
                }
            }
        }
        return this;
    }
}
