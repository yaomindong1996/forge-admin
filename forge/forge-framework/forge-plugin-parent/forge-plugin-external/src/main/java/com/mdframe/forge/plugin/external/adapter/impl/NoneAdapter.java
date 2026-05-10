package com.mdframe.forge.plugin.external.adapter.impl;

import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import org.springframework.stereotype.Component;

@Component
public class NoneAdapter implements DataAdapter {

    @Override
    public String getAdapterType() {
        return "None";
    }

    @Override
    public Object transform(Object originalData, String adapterConfig) {
        return originalData;
    }

    @Override
    public boolean validateConfig(String adapterConfig) {
        return true;
    }
}