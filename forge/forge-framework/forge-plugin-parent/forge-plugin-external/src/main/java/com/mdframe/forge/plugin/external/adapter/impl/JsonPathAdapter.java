package com.mdframe.forge.plugin.external.adapter.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import org.springframework.stereotype.Component;

@Component
public class JsonPathAdapter implements DataAdapter {

    @Override
    public String getAdapterType() {
        return "JsonPath";
    }

    @Override
    public Object transform(Object originalData, String adapterConfig) {
        JSONObject config = JSON.parseObject(adapterConfig);
        JSONObject sourceData = (JSONObject) originalData;

        String sourcePath = config.getString("sourcePath");
        Object extractedData = extractByPath(sourceData, sourcePath);

        JSONObject fieldMapping = config.getJSONObject("fieldMapping");
        Object transformedData = mapFields(extractedData, fieldMapping);

        String targetPath = config.getString("targetPath");
        return buildTargetStructure(transformedData, targetPath);
    }

    private Object extractByPath(JSONObject source, String path) {
        if (path == null || path.isEmpty()) {
            return source;
        }
        String[] parts = path.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (current instanceof JSONObject) {
                current = ((JSONObject) current).get(part);
            } else if (current instanceof JSONArray) {
                JSONArray arr = (JSONArray) current;
                current = arr;
            }
        }
        return current;
    }

    private Object mapFields(Object data, JSONObject mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return data;
        }
        if (data instanceof JSONArray) {
            JSONArray result = new JSONArray();
            for (Object item : (JSONArray) data) {
                if (item instanceof JSONObject) {
                    result.add(mapSingleObject((JSONObject) item, mapping));
                } else {
                    result.add(item);
                }
            }
            return result;
        }
        if (data instanceof JSONObject) {
            return mapSingleObject((JSONObject) data, mapping);
        }
        return data;
    }

    private JSONObject mapSingleObject(JSONObject source, JSONObject mapping) {
        JSONObject result = new JSONObject();
        for (String targetField : mapping.keySet()) {
            String sourceField = mapping.getString(targetField);
            result.put(targetField, source.get(sourceField));
        }
        return result;
    }

    private Object buildTargetStructure(Object data, String targetPath) {
        JSONObject result = new JSONObject();
        result.put("code", 0);

        if (targetPath == null || targetPath.isEmpty()) {
            result.put("data", data);
        } else {
            JSONObject container = new JSONObject();
            container.put(targetPath, data);
            result.put("data", container);
        }
        return result;
    }

    @Override
    public boolean validateConfig(String adapterConfig) {
        if (adapterConfig == null || adapterConfig.isEmpty()) {
            return false;
        }
        try {
            JSONObject config = JSON.parseObject(adapterConfig);
            return config != null;
        } catch (Exception e) {
            return false;
        }
    }
}