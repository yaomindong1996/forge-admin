package com.mdframe.forge.plugin.external.adapter.impl;

import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@Component
public class ScriptAdapter implements DataAdapter {

    private final ScriptEngineManager scriptEngineManager;

    public ScriptAdapter() {
        this.scriptEngineManager = new ScriptEngineManager();
    }

    @Override
    public String getAdapterType() {
        return "Script";
    }

    @Override
    public Object transform(Object originalData, String adapterConfig) {
        ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
        engine.put("response", originalData);

        try {
            engine.eval(adapterConfig);
            return engine.get("result");
        } catch (ScriptException e) {
            throw new RuntimeException("脚本执行失败: " + e.getMessage());
        }
    }

    @Override
    public boolean validateConfig(String adapterConfig) {
        if (adapterConfig == null || adapterConfig.isEmpty()) {
            return false;
        }
        ScriptEngine engine = scriptEngineManager.getEngineByName("javascript");
        try {
            engine.eval(adapterConfig);
            return true;
        } catch (ScriptException e) {
            return false;
        }
    }
}