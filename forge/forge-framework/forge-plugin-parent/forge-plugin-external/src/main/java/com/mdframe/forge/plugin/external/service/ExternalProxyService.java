package com.mdframe.forge.plugin.external.service;

import java.util.Map;

public interface ExternalProxyService {

    Object proxyRequest(Long apiId, Map<String, Object> params);
}