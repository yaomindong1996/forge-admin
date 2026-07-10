package com.mdframe.forge.plugin.ai.provider.adapter;

import com.mdframe.forge.starter.core.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * AI 供应商 Base URL 归一化与协议边界校验。
 */
public final class AiProviderBaseUrlPolicy {

    public static final String DASHSCOPE_NATIVE_BASE_URL = "https://dashscope.aliyuncs.com";

    private static final String DASHSCOPE_HOST = "dashscope.aliyuncs.com";
    private static final String DASHSCOPE_COMPATIBLE_PATH = "/compatible-mode";

    private AiProviderBaseUrlPolicy() {
    }

    /**
     * 归一化 Base URL，并校验官方 DashScope 地址与适配器协议匹配。
     *
     * @param adapterCode 适配器代码
     * @param baseUrl Base URL
     * @return 去除尾斜杠后的 Base URL
     */
    public static String normalizeAndValidate(String adapterCode, String baseUrl) {
        AiProviderAdapterCode adapter = AiProviderAdapterCode.require(adapterCode);
        if (!StringUtils.hasText(baseUrl)) {
            if (adapter == AiProviderAdapterCode.DASHSCOPE_NATIVE) {
                return DASHSCOPE_NATIVE_BASE_URL;
            }
            throw new BusinessException("OpenAI Compatible Base URL不能为空");
        }

        String trimmedBaseUrl = baseUrl.trim();
        URI uri = parseUri(trimmedBaseUrl);
        validateGenericUri(uri);

        String normalizedBaseUrl = removeTrailingSlashes(trimmedBaseUrl);
        String normalizedPath = removeTrailingSlashes(uri.getPath() == null ? "" : uri.getPath());
        if (DASHSCOPE_HOST.equalsIgnoreCase(uri.getHost())) {
            validateOfficialDashScopePath(adapter, normalizedPath);
        }
        return normalizedBaseUrl;
    }

    private static URI parseUri(String baseUrl) {
        try {
            return new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new BusinessException("Base URL格式不正确", e);
        }
    }

    private static void validateGenericUri(URI uri) {
        String scheme = uri.getScheme();
        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new BusinessException("Base URL仅支持HTTP或HTTPS协议");
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new BusinessException("Base URL缺少有效域名");
        }
        if (uri.getUserInfo() != null) {
            throw new BusinessException("Base URL禁止包含用户信息");
        }
        if (uri.getQuery() != null) {
            throw new BusinessException("Base URL禁止包含查询参数");
        }
        if (uri.getFragment() != null) {
            throw new BusinessException("Base URL禁止包含片段标识");
        }
    }

    private static void validateOfficialDashScopePath(AiProviderAdapterCode adapter, String path) {
        if (adapter == AiProviderAdapterCode.DASHSCOPE_NATIVE && StringUtils.hasText(path)) {
            throw new BusinessException("DashScope原生协议必须使用官方根地址");
        }
        if (adapter == AiProviderAdapterCode.OPENAI_COMPATIBLE
                && !DASHSCOPE_COMPATIBLE_PATH.equals(path)) {
            throw new BusinessException("DashScope OpenAI Compatible协议必须使用/compatible-mode地址");
        }
    }

    private static String removeTrailingSlashes(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
