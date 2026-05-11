package com.mdframe.forge.plugin.external.strategy;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExternalCustomAuthAdapterFactory {

    private final Map<String, ExternalCustomAuthAdapter> adapters;

    public ExternalCustomAuthAdapterFactory(List<ExternalCustomAuthAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(ExternalCustomAuthAdapter::getAdapterType, Function.identity()));
    }

    public ExternalCustomAuthAdapter getAdapter(String adapterType) {
        ExternalCustomAuthAdapter adapter = adapters.get(adapterType);
        if (adapter == null) {
            throw new BusinessException("未找到外部认证适配器: " + adapterType);
        }
        return adapter;
    }
}
