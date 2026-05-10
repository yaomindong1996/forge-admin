package com.mdframe.forge.plugin.external.adapter;

public interface DataAdapter {

    String getAdapterType();

    Object transform(Object originalData, String adapterConfig);

    boolean validateConfig(String adapterConfig);
}