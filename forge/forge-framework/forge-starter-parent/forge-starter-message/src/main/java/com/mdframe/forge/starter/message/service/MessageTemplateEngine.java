package com.mdframe.forge.starter.message.service;

import java.util.Map;

public class MessageTemplateEngine {
    
    /**
     * 简单模板替换：将 ${key} 替换为 params 中的值
     */
    public String render(String template, Map<String, Object> params) {
        if (template == null || template.isEmpty() || params == null || params.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> e : params.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            result = result.replace("${" + key + "}", val == null ? "" : String.valueOf(val));
        }
        return result;
    }
}
