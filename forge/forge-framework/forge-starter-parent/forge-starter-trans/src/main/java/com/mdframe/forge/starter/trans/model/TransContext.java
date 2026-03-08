package com.mdframe.forge.starter.trans.model;

import com.mdframe.forge.starter.trans.annotation.TransField;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;

/**
 * 翻译上下文
 */
@Data
@Builder
public class TransContext {

    /**
     * 当前实体对象
     */
    private Object rootObject;

    /**
     * 当前字段所在对象（通常与rootObject相同）
     */
    private Object targetObject;

    /**
     * 当前字段
     */
    private Field field;

    /**
     * 字段原始值
     */
    private Object fieldValue;

    /**
     * 字段上的注解
     */
    private TransField transField;
}
