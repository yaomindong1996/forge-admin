package com.mdframe.forge.plugin.ai.provider.support;

import com.mdframe.forge.plugin.ai.provider.adapter.AiProviderAdapterCode;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderTemplateVO;
import com.mdframe.forge.plugin.ai.provider.vo.AiProviderTypeCountVO;

import java.util.List;
import java.util.Locale;

/**
 * 内置 19 家供应商接入预设目录（统一采用 OpenAI Compatible 协议）。
 */
public final class AiProviderTemplateCatalog {

    private static final List<AiProviderTemplateVO> TEMPLATES = List.of(
            entry("alibaba", "阿里百炼", "https://dashscope.aliyuncs.com/compatible-mode", "qwen-plus",
                    "使用百炼 OpenAI 兼容接口（/compatible-mode），支持通义千问全系列模型。",
                    "https://help.aliyun.com/zh/model-studio/compatibility-of-openai-with-dashscope", List.of("dashscope")),
            entry("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat",
                    "填写 DeepSeek 开放平台 API Key。",
                    "https://api-docs.deepseek.com/", List.of()),
            entry("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini",
                    "使用 OpenAI API Key；当前调用 Chat Completions。",
                    "https://platform.openai.com/docs/api-reference/chat", List.of()),
            entry("zhipu", "智谱 AI", "https://open.bigmodel.cn/api/paas/v4", "glm-4",
                    "使用智谱开放平台 API Key，模型以账号已开通清单为准。",
                    "https://docs.bigmodel.cn/cn/guide/develop/openai/introduction", List.of()),
            entry("moonshot", "Kimi / Moonshot", "https://api.moonshot.cn/v1", "moonshot-v1-8k",
                    "使用 Moonshot 开放平台 API Key，国内与国际地址可按账号修改。",
                    "https://platform.moonshot.cn/docs/guide/start-using-kimi-api", List.of("kimi")),
            entry("minimax", "MiniMax", "https://api.minimax.io/v1", "",
                    "预填国际 OpenAI 兼容地址；国内账号请按控制台填写对应地址与密钥。",
                    "https://platform.minimax.io/docs/api-reference/text-openai-api", List.of()),
            entry("volcengine", "火山方舟", "https://ark.cn-beijing.volces.com/api/v3", "",
                    "填写方舟 API Key；模型标识使用已开通的模型 ID 或推理接入点 ID。",
                    "https://docs.volcengine.com/docs/ark/base-url-and-authentication?lang=zh", List.of("volcanic_engine")),
            entry("siliconflow", "硅基流动", "https://api.siliconflow.cn/v1", "",
                    "填写硅基流动 API Key，模型权限与地域以当前账号为准。",
                    "https://docs.siliconflow.cn/docs/userguide/quickstart", List.of("silicon_flow")),
            entry("baidu", "百度千帆", "https://qianfan.baidubce.com/v2", "",
                    "使用千帆 V2 的 API Key，旧版 AK/SK 接口不是此预设。",
                    "https://ai.baidu.com/ai-doc/WENXINWORKSHOP/2m3fihw8s", List.of("wenxin")),
            entry("tencent", "腾讯混元", "https://api.hunyuan.cloud.tencent.com/v1", "",
                    "使用混元 OpenAI 兼容 API Key；新账号的 TokenHub 地址以控制台为准。",
                    "https://cloud.tencent.com/document/product/1729/111007", List.of("hunyuan")),
            entry("xunfei", "讯飞星火", "https://spark-api-open.xf-yun.com/v1", "",
                    "访问密钥填写 HTTP APIpassword；不同星火模型可能使用不同接口前缀，请核对控制台。",
                    "https://www.xfyun.cn/doc/spark/产品使用说明.html", List.of("spark")),
            entry("google", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "",
                    "使用 Gemini 的 OpenAI 兼容接口；不包含 Gemini 专有文件、搜索等原生接口。",
                    "https://ai.google.dev/gemini-api/docs/openai", List.of("gemini")),
            entry("anthropic", "Anthropic / Claude", "https://api.anthropic.com/v1", "",
                    "使用 Claude 的 OpenAI SDK 兼容接口；其功能与参数支持有限，非 Messages 原生适配。",
                    "https://platform.claude.com/docs/en/cli-sdks-libraries/libraries/openai-sdk", List.of("claude")),
            entry("azure", "Azure OpenAI", "", "",
                    "填写账号提供的 OpenAI 兼容网关地址及密钥；传统 Azure 部署接口、api-key 专用认证和 Entra 自动续期尚未集成。",
                    "https://learn.microsoft.com/en-us/azure/ai-services/openai/", List.of()),
            entry("ollama", "Ollama", "http://localhost:11434/v1", "llama3",
                    "地址应能从后端访问；localhost 指后端所在主机。无认证部署仍需填写非空占位密钥。",
                    "https://docs.ollama.com/api/openai-compatibility", List.of()),
            entry("xinference", "Xinference", "http://localhost:9997/v1", "",
                    "填写实际部署地址；模型标识使用已启动模型的 UID，认证按部署配置填写。",
                    "https://inference.readthedocs.io/en/latest/user_guide/client_api.html", List.of()),
            entry("localai", "LocalAI", "http://localhost:8080/v1", "",
                    "填写后端可访问的 LocalAI 服务地址和部署密钥。",
                    "https://localai.io/basics/getting_started/", List.of("local_ai")),
            entry("vllm", "vLLM", "http://localhost:8000/v1", "",
                    "填写 vLLM OpenAI 服务地址，模型名对应部署的 served-model-name。",
                    "https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html", List.of()),
            entry("custom", "自定义接入", "", "",
                    "填写 OpenAI 兼容服务的基础地址与密钥；不要填写完整 chat/completions 路径。",
                    "", List.of())
    );

    private AiProviderTemplateCatalog() {
    }

    private static AiProviderTemplateVO entry(String type, String name, String baseUrl, String defaultModel,
                                              String hint, String docs, List<String> aliases) {
        return new AiProviderTemplateVO(type, name, type, baseUrl, defaultModel,
                AiProviderAdapterCode.OPENAI_COMPATIBLE.getCode(), hint, docs, true, aliases, null);
    }

    public static List<AiProviderTemplateVO> templates() {
        return TEMPLATES;
    }

    public static String defaultBaseUrl(String providerType) {
        if (providerType == null) {
            return null;
        }
        String normalized = providerType.trim().toLowerCase(Locale.ROOT);
        return TEMPLATES.stream()
                .filter(t -> t.featured() && matches(t, normalized))
                .map(AiProviderTemplateVO::baseUrl)
                .filter(url -> !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    public static boolean matches(AiProviderTemplateVO template, String type) {
        return template.providerType().equals(type) || template.providerTypeAliases().contains(type);
    }

    public static List<AiProviderTemplateVO> withCounts(List<AiProviderTypeCountVO> counts) {
        List<AiProviderTypeCountVO> safeCounts = counts == null ? List.of() : counts;
        return TEMPLATES.stream()
                .map(t -> new AiProviderTemplateVO(
                        t.templateKey(), t.name(), t.providerType(),
                        t.baseUrl(), t.defaultModel(), t.adapterCode(), t.connectionHint(), t.documentationUrl(),
                        t.featured(), t.providerTypeAliases(),
                        safeCounts.stream()
                                .filter(c -> matches(t, c.getProviderType()))
                                .mapToLong(c -> c.getConfiguredCount() == null ? 0L : c.getConfiguredCount())
                                .sum()))
                .toList();
    }
}
