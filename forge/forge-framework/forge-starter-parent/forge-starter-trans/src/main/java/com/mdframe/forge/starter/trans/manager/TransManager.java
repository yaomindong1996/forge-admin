package com.mdframe.forge.starter.trans.manager;

import com.mdframe.forge.starter.trans.annotation.DictTrans;
import com.mdframe.forge.starter.trans.annotation.TransField;
import com.mdframe.forge.starter.trans.handler.TransHandler;
import com.mdframe.forge.starter.trans.handler.impl.DefaultDictTransHandler;
import com.mdframe.forge.starter.trans.handler.impl.EnumTransHandler;
import com.mdframe.forge.starter.trans.handler.impl.ExpressionTransHandler;
import com.mdframe.forge.starter.trans.model.TransContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 翻译管理器
 * 对外暴露统一的翻译入口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransManager implements ApplicationContextAware {

    private final DefaultDictTransHandler defaultDictTransHandler;
    private final EnumTransHandler enumTransHandler;
    private final ExpressionTransHandler expressionTransHandler;

    private ApplicationContext applicationContext;

    /**
     * 手动对单个实体进行翻译
     */
    public void translate(Object entity) {
        if (entity == null) {
            return;
        }
        Class<?> clazz = entity.getClass();
        DictTrans dictTrans = clazz.getAnnotation(DictTrans.class);
        if (dictTrans == null || !dictTrans.enabled()) {
            return;
        }

        for (Field field : clazz.getDeclaredFields()) {
            TransField transField = field.getAnnotation(TransField.class);
            if (transField == null) {
                continue;
            }
            doTranslateField(entity, field, transField);
        }
    }

    /**
     * 手动对列表进行翻译
     */
    public void translate(Iterable<?> list) {
        if (list == null) {
            return;
        }
        for (Object obj : list) {
            translate(obj);
        }
    }

    private void doTranslateField(Object entity, Field field, TransField transField) {
        try {
            field.setAccessible(true);
            Object value = field.get(entity);

            TransHandler handler = resolveHandler(transField);
            if (handler == null) {
                return;
            }

            TransContext context = TransContext.builder()
                    .rootObject(entity)
                    .targetObject(entity)
                    .field(field)
                    .fieldValue(value)
                    .transField(transField)
                    .build();

            if (!handler.supports(context)) {
                return;
            }

            Object translated = handler.translate(context);
            if (translated == null) {
                return;
            }

            // 计算目标字段名
            String targetName = transField.target();
            if (targetName == null || targetName.isEmpty()) {
                targetName = field.getName() + "Name";
            }

            // 反射设置目标字段值
            Field targetField = findField(entity.getClass(), targetName);
            if (targetField == null) {
                log.debug("未找到目标字段: {}.{}", entity.getClass().getSimpleName(), targetName);
                return;
            }
            targetField.setAccessible(true);
            targetField.set(entity, translated);
        } catch (Exception e) {
            log.warn("字段翻译失败: {}.{}", entity.getClass().getSimpleName(), field.getName(), e);
        }
    }

    private Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 自动解析处理器，优先级：
     * 1. 手动指定的 handler（非默认值）
     * 2. dictType（字典表翻译）- 最高优先级
     * 3. enumClass（枚举翻译）
     * 4. expression（表达式翻译）
     */
    private TransHandler resolveHandler(TransField transField) {
        Class<? extends TransHandler> handlerClass = transField.handler();
        
        // 1. 如果手动指定了非默认 handler，使用指定的
        if (handlerClass != TransHandler.class) {
            if (handlerClass == DefaultDictTransHandler.class) {
                return defaultDictTransHandler;
            }
            if (handlerClass == EnumTransHandler.class) {
                return enumTransHandler;
            }
            if (handlerClass == ExpressionTransHandler.class) {
                return expressionTransHandler;
            }
            // 自定义处理器
            try {
                return applicationContext.getBean(handlerClass);
            } catch (BeansException e) {
                log.warn("未找到自定义处理器Bean: {}，将尝试直接实例化", handlerClass.getName());
                try {
                    return handlerClass.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    log.error("实例化处理器失败: {}", handlerClass.getName(), ex);
                    return null;
                }
            }
        }
        
        // 2. 自动适配：优先级 dictType > enumClass > expression
        String dictType = transField.dictType();
        if (dictType != null && !dictType.isEmpty()) {
            return defaultDictTransHandler;
        }
        
        Class<? extends Enum<?>> enumClass = transField.enumClass();
        if (enumClass != TransField.DefaultEnum.class) {
            return enumTransHandler;
        }
        
        String expression = transField.expression();
        if (expression != null && !expression.isEmpty()) {
            return expressionTransHandler;
        }
        
        // 3. 都没配置，默认使用字典表翻译
        return defaultDictTransHandler;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
