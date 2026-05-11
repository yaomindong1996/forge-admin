package com.mdframe.forge.plugin.external.service;

import com.mdframe.forge.plugin.external.vo.ExternalApiDebugResult;

import java.util.Map;

public interface ExternalProxyService {

    Object proxyRequest(Long apiId, Map<String, Object> params);

    ExternalApiDebugResult debugRequest(Long apiId, Map<String, Object> params);
}
