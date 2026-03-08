package com.mdframe.forge.starter.trans.util;

import com.mdframe.forge.starter.trans.manager.TransManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 字典翻译静态工具类
 * 提供静态方法快速调用字典翻译
 */
@Component
public class TransUtils implements ApplicationContextAware {

    private static TransManager transManager;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        transManager = applicationContext.getBean(TransManager.class);
    }

    /**
     * 翻译单个对象
     * 
     * @param entity 实体对象
     * @param <T> 泛型类型
     * @return 翻译后的对象（原对象，已修改）
     */
    public static <T> T translate(T entity) {
        if (transManager != null && entity != null) {
            transManager.translate(entity);
        }
        return entity;
    }

    /**
     * 翻译列表
     * 
     * @param list 实体列表
     * @param <T> 泛型类型
     * @return 翻译后的列表（原列表，已修改）
     */
    public static <T extends Iterable<?>> T translate(T list) {
        if (transManager != null && list != null) {
            transManager.translate(list);
        }
        return list;
    }

    /**
     * 翻译单个对象（别名方法）
     */
    public static <T> T trans(T entity) {
        return translate(entity);
    }

    /**
     * 翻译列表（别名方法）
     */
    public static <T extends Iterable<?>> T trans(T list) {
        return translate(list);
    }
}
