package com.mdframe.forge.plugin.data.support;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class DataDatasetRowScopeCondition {

    private boolean enabled;

    private String conditionSql;

    private Map<String, Object> params = new LinkedHashMap<>();

    public static DataDatasetRowScopeCondition disabled() {
        return new DataDatasetRowScopeCondition();
    }

    public static DataDatasetRowScopeCondition of(String conditionSql, Map<String, Object> params) {
        DataDatasetRowScopeCondition condition = new DataDatasetRowScopeCondition();
        condition.setEnabled(true);
        condition.setConditionSql(conditionSql);
        condition.setParams(params != null ? params : new LinkedHashMap<>());
        return condition;
    }
}
