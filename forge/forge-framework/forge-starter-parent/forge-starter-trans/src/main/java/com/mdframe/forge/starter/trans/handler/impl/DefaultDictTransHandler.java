package com.mdframe.forge.starter.trans.handler.impl;

import com.mdframe.forge.starter.trans.handler.TransHandler;
import com.mdframe.forge.starter.trans.model.TransContext;
import com.mdframe.forge.starter.trans.spi.DictValueProvider;
import lombok.RequiredArgsConstructor;

/**
 * 默认字典处理器：基于系统字典表
 * 通过 DictValueProvider SPI 接口获取字典值
 */
@RequiredArgsConstructor
public class DefaultDictTransHandler implements TransHandler {

    /**
     * 字典值提供者（由业务侧实现并注入）
     */
    private final DictValueProvider dictValueProvider;

    @Override
    public Object translate(TransContext context) {
        if (dictValueProvider == null) {
            return context.getFieldValue();
        }
        if (context.getFieldValue() == null) {
            return null;
        }
        String dictType = context.getTransField().dictType();
        String key = String.valueOf(context.getFieldValue());
        String label = dictValueProvider.getLabel(dictType, key);
        return label != null ? label : key;
    }
}
