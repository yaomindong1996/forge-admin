package com.mdframe.forge.starter.message.service;

import java.util.Map;

public class MessageTemplateEngine {
    
    /**
     * 简单模板替换：兼容 ${key} 与 {key}。
     */
    public String render(String template, Map<String, Object> params) {
        if (template == null || template.isEmpty() || params == null || params.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> e : params.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            String text = val == null ? "" : String.valueOf(val);
            result = result.replace("${" + key + "}", text)
                    .replace("{" + key + "}", text);
        }
        return result;
    }
}
