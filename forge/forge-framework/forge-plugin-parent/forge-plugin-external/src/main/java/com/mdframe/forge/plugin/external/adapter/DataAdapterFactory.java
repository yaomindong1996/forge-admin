package com.mdframe.forge.plugin.external.adapter;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DataAdapterFactory {

    private final Map<String, DataAdapter> adapters;

    public DataAdapterFactory(List<DataAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(DataAdapter::getAdapterType, Function.identity()));
    }

    public DataAdapter getAdapter(String adapterType) {
        return adapters.getOrDefault(adapterType, adapters.get("None"));
    }

    public List<String> getSupportedTypes() {
        return new ArrayList<>(adapters.keySet());
    }
}