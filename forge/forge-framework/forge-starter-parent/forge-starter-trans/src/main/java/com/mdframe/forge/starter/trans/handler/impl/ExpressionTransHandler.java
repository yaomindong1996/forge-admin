package com.mdframe.forge.starter.trans.handler.impl;

import com.mdframe.forge.starter.trans.annotation.TransField;
import com.mdframe.forge.starter.trans.handler.TransHandler;
import com.mdframe.forge.starter.trans.model.TransContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 表达式翻译处理器
 * 使用简单表达式进行键值映射，例如："1:正常,2:失败"
 */
public class ExpressionTransHandler implements TransHandler {

    @Override
    public Object translate(TransContext context) {
        TransField transField = context.getTransField();
        String expression = transField.expression();
        if (expression == null || expression.isEmpty()) {
            return context.getFieldValue();
        }

        Map<String, String> mapping = parseExpression(expression);
        Object value = context.getFieldValue();
        if (value == null) {
            return null;
        }
        return mapping.getOrDefault(String.valueOf(value), String.valueOf(value));
    }

    private Map<String, String> parseExpression(String expression) {
        Map<String, String> map = new HashMap<>();
        String[] items = expression.split(",");
        for (String item : items) {
            String[] kv = item.split(":");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }
}
