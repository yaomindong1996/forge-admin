package com.mdframe.forge.starter.trans.handler.impl;

import com.mdframe.forge.starter.trans.annotation.TransField;
import com.mdframe.forge.starter.trans.handler.TransHandler;
import com.mdframe.forge.starter.trans.model.TransContext;

import java.lang.reflect.Method;

/**
 * 枚举翻译处理器
 * 根据枚举类型进行翻译
 */
public class EnumTransHandler implements TransHandler {

    @Override
    public Object translate(TransContext context) {
        TransField transField = context.getTransField();
        Class<? extends Enum<?>> enumClass = transField.enumClass();
        if (enumClass == TransField.DefaultEnum.class) {
            // 未配置枚举类，直接返回原值
            return context.getFieldValue();
        }

        Object value = context.getFieldValue();
        if (value == null) {
            return null;
        }

        // 尝试通过约定方法进行映射：如 getLabel(), getDesc(), getNameByCode()
        Enum<?>[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return value;
        }

        for (Enum<?> e : constants) {
            try {
                // 优先按照 name() 匹配
                if (e.name().equals(String.valueOf(value))) {
                    Method getLabel = findMethod(enumClass, "getLabel", "getDesc", "getName");
                    if (getLabel != null) {
                        return getLabel.invoke(e);
                    }
                    return e.name();
                }

                // 尝试 code 字段匹配：getCode()
                Method getCode = findMethod(enumClass, "getCode");
                if (getCode != null) {
                    Object code = getCode.invoke(e);
                    if (code != null && String.valueOf(code).equals(String.valueOf(value))) {
                        Method getLabel = findMethod(enumClass, "getLabel", "getDesc", "getName");
                        if (getLabel != null) {
                            return getLabel.invoke(e);
                        }
                        return e.name();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return value;
    }

    private Method findMethod(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                return clazz.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
