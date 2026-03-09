package com.mdframe.forge.starter.excel.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通用行数据类，用于 Excel 导入时接收动态数据
 * 解决 Map 接口无法被 EasyExcel 实例化的问题
 */
@Data
@NoArgsConstructor
public class GenericRowData {
    
    /**
     * 动态字段数据，保持插入顺序
     */
    private Map<String, Object> fields = new LinkedHashMap<>();
    
    /**
     * 设置字段值
     */
    public void setField(String key, Object value) {
        fields.put(key, value);
    }
    
    /**
     * 获取字段值
     */
    public Object getField(String key) {
        return fields.get(key);
    }
    
    /**
     * 获取所有字段
     */
    public Map<String, Object> getFields() {
        return fields;
    }
}
