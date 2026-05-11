package com.mdframe.forge.plugin.external.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mdframe.forge.plugin.external.adapter.DataAdapter;
import com.mdframe.forge.plugin.external.adapter.DataAdapterFactory;
import com.mdframe.forge.plugin.external.entity.ExternalApi;
import com.mdframe.forge.plugin.external.entity.ExternalApiLog;
import com.mdframe.forge.plugin.external.entity.ExternalSystem;
import com.mdframe.forge.plugin.external.service.ExternalApiLogService;
import com.mdframe.forge.plugin.external.service.ExternalApiService;
import com.mdframe.forge.plugin.external.service.ExternalProxyService;
import com.mdframe.forge.plugin.external.service.ExternalSystemService;
import com.mdframe.forge.plugin.external.strategy.ExternalAuthStrategy;
import com.mdframe.forge.plugin.external.strategy.ExternalAuthStrategyFactory;
import com.mdframe.forge.plugin.external.vo.ExternalApiDebugResult;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.session.SessionHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalProxyServiceImpl implements ExternalProxyService {

    private final ExternalApiService apiService;
    private final ExternalSystemService systemService;
    private final ExternalApiLogService logService;
    private final ExternalAuthStrategyFactory authFactory;
    private final DataAdapterFactory adapterFactory;

    @Override
    public Object proxyRequest(Long apiId, Map<String, Object> params) {
        ExternalApiDebugResult result = executeRequest(apiId, params, false);
        return result.getResponseData();
    }

    @Override
    public ExternalApiDebugResult debugRequest(Long apiId, Map<String, Object> params) {
        return executeRequest(apiId, params, true);
    }

    private ExternalApiDebugResult executeRequest(Long apiId, Map<String, Object> params, boolean debugFlag) {
        Map<String, Object> safeParams = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        ExternalApi api = null;
        ExternalSystem system = null;
        String fullUrl = null;
        String requestParams = JSON.toJSONString(params == null ? Collections.emptyMap() : params);
        String requestBody = requestParams;
        ExternalApiDebugResult result = new ExternalApiDebugResult();

        try {
            api = apiService.getById(apiId);
            if (api == null || api.getApiStatus() != 1) {
                throw new BusinessException("接口不存在或已停用");
            }

            system = systemService.getById(api.getSystemId());
            if (system == null || system.getSystemStatus() != 1) {
                throw new BusinessException("系统不存在或已停用");
            }

            Map<String, Object> runtimeParams = params == null ? Collections.emptyMap() : new LinkedHashMap<>(params);
            safeParams.putAll(parseJsonObject(api.getRequestParams()));
            safeParams.putAll(applyParamMappings(api, runtimeParams));
            applyApiKeyBodyAuth(system, safeParams);
            requestParams = JSON.toJSONString(safeParams);
            requestBody = buildRequestBody(api, safeParams);

            fullUrl = buildFullUrl(system.getBaseUrl(), api.getApiPath());
            fullUrl = appendApiKeyQueryAuth(fullUrl, system);
            if ("GET".equalsIgnoreCase(api.getApiMethod())) {
                fullUrl = appendQuery(fullUrl, safeParams);
                requestBody = null;
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofMillis(resolveTimeout(system.getReadTimeout(), 30000)));

            ExternalAuthStrategy externalAuthStrategy = authFactory.getStrategy(resolveAuthStrategyType(system.getAuthType()));
            externalAuthStrategy.applyAuth(requestBuilder, buildAuthConfig(system));
            applyConfiguredHeaders(requestBuilder, api);

            applyRequestMethod(requestBuilder, api, requestBody);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(resolveTimeout(system.getConnectTimeout(), 30000)))
                    .build();
            HttpResponse<String> response = sendRequest(client, requestBuilder.build());
            long durationMs = System.currentTimeMillis() - startTime;

            Object responseData = parseResponse(api, response.body());
            Object extractedData = extractResponseData(api, responseData);
            Object finalData = transformResponse(api, extractedData);
            finalData = buildResponsePayload(api, responseData, finalData);
            boolean callSuccess = determineCallSuccess(api, response.statusCode(), responseData);
            String responseErrorMessage = callSuccess ? null : resolveResponseErrorMessage(api, response.statusCode(), responseData);

            result.setSuccess(callSuccess);
            result.setHttpStatusCode(response.statusCode());
            result.setDurationMs(durationMs);
            result.setResponseData(finalData);
            result.setResponseBody(response.body());
            result.setErrorMessage(responseErrorMessage);

            recordRuntimeLog(api, system, fullUrl, response.statusCode(), durationMs, result.getSuccess(), debugFlag, responseErrorMessage);
            saveLog(api, system, fullUrl, requestParams, requestBody, response.body(), response.statusCode(), result.getSuccess(), responseErrorMessage, durationMs, debugFlag);
            return result;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setDurationMs(durationMs);
            result.setErrorMessage(e.getMessage());
            recordRuntimeLog(api, system, fullUrl, null, durationMs, false, debugFlag, e.getMessage());
            saveLog(api, system, fullUrl, requestParams, requestBody, null, null, false, e.getMessage(), durationMs, debugFlag);
            if (!debugFlag) {
                throw e instanceof BusinessException ? (BusinessException) e : new BusinessException("请求外部接口失败: " + e.getMessage());
            }
            return result;
        }
    }

    private Object transformResponse(ExternalApi api, Object originalData) {
        if (api.getResponseTransformEnabled() != null && api.getResponseTransformEnabled()
                && api.getResponseTransformScript() != null && !api.getResponseTransformScript().isEmpty()) {
            DataAdapter adapter = adapterFactory.getAdapter("Script");
            return adapter.transform(originalData, api.getResponseTransformScript());
        }
        return originalData;
    }

    private Object extractResponseData(ExternalApi api, Object responseData) {
        if (api.getResponseDataPath() == null || api.getResponseDataPath().isEmpty()) {
            return responseData;
        }
        Object value = getPathValue(responseData, api.getResponseDataPath());
        return value == null ? responseData : value;
    }

    private boolean determineCallSuccess(ExternalApi api, Integer httpStatusCode, Object responseData) {
        if (api.getErrorCodePath() != null && !api.getErrorCodePath().isEmpty()) {
            Object code = getPathValue(responseData, api.getErrorCodePath());
            if (code == null) {
                return httpStatusCode != null && httpStatusCode >= 200 && httpStatusCode < 300;
            }
            return isSuccessCode(api, String.valueOf(code));
        }
        return httpStatusCode != null && httpStatusCode >= 200 && httpStatusCode < 300;
    }

    private boolean isSuccessCode(ExternalApi api, String code) {
        String successCodes = api.getSuccessCodes();
        if (successCodes == null || successCodes.isEmpty()) {
            successCodes = "0,200";
        }
        for (String successCode : successCodes.split(",")) {
            if (successCode.trim().equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String resolveResponseErrorMessage(ExternalApi api, Integer httpStatusCode, Object responseData) {
        if (api.getErrorMsgPath() != null && !api.getErrorMsgPath().isEmpty()) {
            Object message = getPathValue(responseData, api.getErrorMsgPath());
            if (message != null) {
                return String.valueOf(message);
            }
        }
        return "外部接口返回失败状态，HTTP状态码: " + httpStatusCode;
    }

    private Object getPathValue(Object data, String path) {
        if (data == null || path == null || path.isEmpty()) {
            return null;
        }
        path = normalizeJsonPath(path);
        Object current = data;
        for (String part : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof java.util.List<?> list) {
                try {
                    int index = Integer.parseInt(part);
                    current = index >= 0 && index < list.size() ? list.get(index) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }

    private String normalizeJsonPath(String path) {
        String normalizedPath = path.trim();
        if (normalizedPath.startsWith("$.")) {
            normalizedPath = normalizedPath.substring(2);
        } else if (normalizedPath.startsWith("$")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return normalizedPath
                .replace("[", ".")
                .replace("]", "")
                .replace("'", "")
                .replace("\"", "");
    }

    private Map<String, Object> applyParamMappings(ExternalApi api, Map<String, Object> runtimeParams) {
        if (!Boolean.TRUE.equals(api.getParamMappingEnabled()) || api.getParamMappings() == null || api.getParamMappings().isEmpty()) {
            return runtimeParams;
        }
        Map<String, Object> mappings = parseJsonObject(api.getParamMappings());
        if (mappings.isEmpty()) {
            return runtimeParams;
        }
        Map<String, Object> mappedParams = new LinkedHashMap<>();
        java.util.Set<String> mappedSourceKeys = new java.util.HashSet<>();
        for (Map.Entry<String, Object> entry : mappings.entrySet()) {
            String sourceKey = entry.getKey();
            String targetKey = null;
            Object defaultValue = null;
            Object mappingValue = entry.getValue();
            if (mappingValue instanceof JSONObject config) {
                sourceKey = defaultString(config.getString("source"), sourceKey);
                targetKey = defaultString(config.getString("target"), entry.getKey());
                defaultValue = config.get("defaultValue");
                if (defaultValue == null) {
                    defaultValue = config.get("default");
                }
            } else if (mappingValue != null) {
                targetKey = String.valueOf(mappingValue);
            }
            if (targetKey == null || targetKey.isEmpty()) {
                targetKey = sourceKey;
            }
            if (runtimeParams.containsKey(sourceKey)) {
                mappedParams.put(targetKey, runtimeParams.get(sourceKey));
                mappedSourceKeys.add(sourceKey);
            } else if (defaultValue != null) {
                mappedParams.put(targetKey, defaultValue);
            }
        }
        runtimeParams.forEach((key, value) -> {
            if (!mappedSourceKeys.contains(key) && !mappedParams.containsKey(key)) {
                mappedParams.put(key, value);
            }
        });
        return mappedParams;
    }

    private Object buildResponsePayload(ExternalApi api, Object responseData, Object finalData) {
        if (api.getResponseTotalPath() == null || api.getResponseTotalPath().isEmpty()) {
            return finalData;
        }
        Object total = getPathValue(responseData, api.getResponseTotalPath());
        if (total == null) {
            return finalData;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", finalData);
        payload.put("total", total);
        return payload;
    }

    private Object parseResponse(ExternalApi api, String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        String responseContentType = api.getResponseContentType();
        if (responseContentType != null && responseContentType.toLowerCase().contains("text/plain")) {
            return responseBody;
        }
        try {
            return JSON.parse(responseBody);
        } catch (Exception e) {
            return responseBody;
        }
    }

    private void saveLog(ExternalApi api, ExternalSystem system, String requestUrl, String requestParams, String requestBody,
                         String responseBody, Integer httpStatusCode, Boolean success,
                         String errorMessage, Long durationMs, boolean debugFlag) {
        if (api == null || (!debugFlag && system != null && Boolean.FALSE.equals(system.getRequestLoggingEnabled()))) {
            return;
        }
        ExternalApiLog logRecord = new ExternalApiLog();
        logRecord.setTenantId(SessionHelper.getTenantId());
        logRecord.setSystemId(api.getSystemId());
        logRecord.setApiId(api.getId());
        logRecord.setSystemName(system == null ? null : system.getSystemName());
        logRecord.setApiName(api.getApiName());
        logRecord.setApiCode(api.getApiCode());
        logRecord.setRequestMethod(api.getApiMethod());
        logRecord.setRequestUrl(truncate(requestUrl, 1000));
        logRecord.setRequestParams(truncate(maskSensitiveJson(requestParams), 4000));
        logRecord.setRequestBody(truncate(maskSensitiveJson(requestBody), 4000));
        logRecord.setResponseBody(truncate(responseBody, 4000));
        logRecord.setHttpStatusCode(httpStatusCode);
        logRecord.setCallStatus(Boolean.TRUE.equals(success) ? 1 : 0);
        logRecord.setErrorMessage(truncate(errorMessage, 1000));
        logRecord.setDurationMs(durationMs);
        logRecord.setDebugFlag(debugFlag);
        try {
            logService.save(logRecord);
        } catch (Exception e) {
            log.warn("保存外部接口调用日志失败，apiId={}, requestUrl={}", api.getId(), requestUrl, e);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String maskSensitiveJson(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            Object parsed = JSON.parse(value);
            if (parsed instanceof JSONObject jsonObject) {
                maskSensitiveObject(jsonObject);
                return jsonObject.toJSONString();
            }
        } catch (Exception e) {
            return value;
        }
        return value;
    }

    private void maskSensitiveObject(JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (isSensitiveKey(key)) {
                jsonObject.put(key, "******");
            } else if (value instanceof JSONObject child) {
                maskSensitiveObject(child);
            }
        }
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("token")
                || lowerKey.contains("password")
                || lowerKey.contains("secret")
                || lowerKey.contains("apikey")
                || lowerKey.contains("api_key")
                || lowerKey.contains("authorization");
    }

    private void recordRuntimeLog(ExternalApi api, ExternalSystem system, String requestUrl, Integer httpStatusCode,
                                  Long durationMs, Boolean success, boolean debugFlag, String errorMessage) {
        if (api == null) {
            return;
        }
        if (!Boolean.TRUE.equals(success)) {
            log.warn("外部接口调用失败，system={}, api={}, url={}, status={}, durationMs={}, debug={}, error={}",
                    system == null ? null : system.getSystemCode(), api.getApiCode(), requestUrl,
                    httpStatusCode, durationMs, debugFlag, errorMessage);
            return;
        }
        if (durationMs != null && durationMs > 3000) {
            log.warn("外部接口调用耗时较高，system={}, api={}, url={}, status={}, durationMs={}, debug={}",
                    system == null ? null : system.getSystemCode(), api.getApiCode(), requestUrl,
                    httpStatusCode, durationMs, debugFlag);
        }
    }

    private String appendQuery(String url, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        String query = params.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&"));
        if (query.isEmpty()) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + query;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String buildAuthConfig(ExternalSystem system) {
        if (system.getAuthType() == null || "none".equalsIgnoreCase(system.getAuthType())) {
            return null;
        }
        if ("basic".equalsIgnoreCase(system.getAuthType())) {
            JSONObject config = new JSONObject();
            config.put("username", system.getBasicUsername());
            config.put("password", system.getBasicPassword());
            return config.toJSONString();
        }
        if ("token".equalsIgnoreCase(system.getAuthType()) || "BearerToken".equals(system.getAuthType())) {
            JSONObject config = new JSONObject();
            config.put("token", system.getTokenValue());
            config.put("tokenHeader", system.getTokenHeaderName());
            config.put("tokenPrefix", system.getTokenPrefix());
            return config.toJSONString();
        }
        if ("current_token".equalsIgnoreCase(system.getAuthType())) {
            JSONObject config = new JSONObject();
            config.put("tokenHeader", system.getTokenHeaderName());
            config.put("tokenPrefix", system.getTokenPrefix());
            return config.toJSONString();
        }
        if ("api_key".equalsIgnoreCase(system.getAuthType())) {
            JSONObject config = new JSONObject();
            config.put("name", system.getApiKeyName());
            config.put("value", system.getApiKeyValue());
            config.put("position", system.getApiKeyPosition());
            return config.toJSONString();
        }
        if ("oauth2".equalsIgnoreCase(system.getAuthType())) {
            JSONObject config = new JSONObject();
            config.put("tokenUrl", system.getOauth2TokenUrl());
            config.put("clientId", system.getOauth2ClientId());
            config.put("clientSecret", system.getOauth2ClientSecret());
            config.put("grantType", system.getOauth2GrantType());
            config.put("scope", system.getOauth2Scope());
            return config.toJSONString();
        }
        if ("custom".equalsIgnoreCase(system.getAuthType())) {
            JSONObject config = new JSONObject();
            config.put("adapter", system.getCustomAuthAdapter());
            config.put("config", parseJsonObject(system.getCustomAuthConfig()));
            return config.toJSONString();
        }
        return null;
    }

    private String buildFullUrl(String baseUrl, String apiPath) {
        String url = baseUrl;
        if (!baseUrl.endsWith("/")) {
            url += "/";
        }
        url += apiPath.startsWith("/") ? apiPath.substring(1) : apiPath;
        return url;
    }

    private void applyRequestMethod(HttpRequest.Builder builder, ExternalApi api, String requestBody) {
        String method = api.getApiMethod() == null ? "GET" : api.getApiMethod().toUpperCase();
        String contentType = api.getRequestContentType() == null || api.getRequestContentType().isEmpty()
                ? "application/json" : api.getRequestContentType();
        switch (method) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(requestBody == null ? "" : requestBody));
                builder.header("Content-Type", contentType);
                break;
            case "PUT":
                builder.PUT(HttpRequest.BodyPublishers.ofString(requestBody == null ? "" : requestBody));
                builder.header("Content-Type", contentType);
                break;
            case "PATCH":
                builder.method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody == null ? "" : requestBody));
                builder.header("Content-Type", contentType);
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                builder.GET();
        }
    }

    private HttpResponse<String> sendRequest(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("请求外部接口失败: " + e.getMessage());
        }
    }

    private String resolveAuthStrategyType(String authType) {
        if (authType == null) {
            return "None";
        }
        if ("token".equalsIgnoreCase(authType)) {
            return "BearerToken";
        }
        if ("current_token".equalsIgnoreCase(authType)) {
            return "CurrentToken";
        }
        if ("none".equalsIgnoreCase(authType)) {
            return "None";
        }
        if ("basic".equalsIgnoreCase(authType)) {
            return "Basic";
        }
        if ("api_key".equalsIgnoreCase(authType)) {
            return "ApiKey";
        }
        if ("oauth2".equalsIgnoreCase(authType)) {
            return "OAuth2";
        }
        if ("custom".equalsIgnoreCase(authType)) {
            return "Custom";
        }
        return authType;
    }

    private Map<String, Object> parseJsonObject(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            JSONObject jsonObject = JSON.parseObject(value);
            return jsonObject == null ? Collections.emptyMap() : jsonObject;
        } catch (Exception e) {
            throw new BusinessException("JSON配置格式错误: " + e.getMessage());
        }
    }

    private void applyConfiguredHeaders(HttpRequest.Builder builder, ExternalApi api) {
        Map<String, Object> headers = parseJsonObject(api.getRequestHeaders());
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                builder.header(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
    }

    private void applyApiKeyBodyAuth(ExternalSystem system, Map<String, Object> params) {
        if (!"api_key".equalsIgnoreCase(system.getAuthType()) || !"body".equalsIgnoreCase(system.getApiKeyPosition())) {
            return;
        }
        if (system.getApiKeyName() != null && system.getApiKeyValue() != null) {
            params.put(system.getApiKeyName(), system.getApiKeyValue());
        }
    }

    private String appendApiKeyQueryAuth(String url, ExternalSystem system) {
        if (!"api_key".equalsIgnoreCase(system.getAuthType()) || !"query".equalsIgnoreCase(system.getApiKeyPosition())) {
            return url;
        }
        if (system.getApiKeyName() == null || system.getApiKeyValue() == null) {
            return url;
        }
        return appendQuery(url, Collections.singletonMap(system.getApiKeyName(), system.getApiKeyValue()));
    }

    private String buildRequestBody(ExternalApi api, Map<String, Object> params) {
        if (api.getRequestBodyTemplate() != null && !api.getRequestBodyTemplate().isEmpty()) {
            String body = api.getRequestBodyTemplate();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                body = body.replace("{" + entry.getKey() + "}", value);
            }
            return body;
        }
        if ("application/x-www-form-urlencoded".equalsIgnoreCase(api.getRequestContentType())) {
            return params.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                    .collect(Collectors.joining("&"));
        }
        if ("text/plain".equalsIgnoreCase(api.getRequestContentType())) {
            return params.values().stream().findFirst().map(String::valueOf).orElse("");
        }
        return JSON.toJSONString(params);
    }

    private long resolveTimeout(Integer timeout, long defaultValue) {
        return timeout == null || timeout <= 0 ? defaultValue : timeout;
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
