package com.mdframe.forge.starter.crypto.crypto;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mdframe.forge.starter.crypto.serializer.CryptoFieldDeserializer;
import com.mdframe.forge.starter.crypto.serializer.CryptoFieldSerializer;

import java.lang.annotation.*;

/**
 * 字段级加解密注解
 * <p>
 * 标注在实体类字段上，该字段在序列化时加密，反序列化时解密
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = CryptoFieldSerializer.class)
@JsonDeserialize(using = CryptoFieldDeserializer.class)
public @interface CryptoField {

    /**
     * 加密算法，默认使用配置文件中的算法
     */
    String algorithm() default "";

    /**
     * 是否加密（序列化时）
     */
    boolean encrypt() default true;

    /**
     * 是否解密（反序列化时）
     */
    boolean decrypt() default true;
}
