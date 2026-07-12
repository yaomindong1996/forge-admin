此文件记录了 Tool Calling 插件的开发规范，请贡献者遵循以下规范编写插件实现。

## 插件开发规范

1. 命名规范

* 插件命名为：`spring-ai-alibaba-starter-tool-calling-${name}`，例如：`spring-ai-alibaba-starter-tool-calling-baidusearch`
* 包名前缀命名为：`com.alibaba.cloud.ai.toolcalling.${name}`，例如：`com.alibaba.cloud.ai.toolcalling.baidusearch`
* AutoConfiguration 配置类命名为：`${name}AutoConfiguration`，例如：`BaiduSearchAutoConfiguration`
* Properties 类命名为：`${name}Properties`，例如：`BaiduSearchProperties`。
* ToolFunction Impl name 命名为：`${name}Service`，通常是由声明 Bean 注解的方法名确定，如 `baiduSearchService`（建议，请根据插件实际情况确定）
* ToolFunction bean name 命名为：`${name}`。
* Constants 类命名为：`${name}Constants`，且为`final`。
* 单元测试类的名称为：`${name}Test`。

2. 使用 **org.springframework.context.annotation.Description** `@Description("xxx")` 注解描述插件的功能，应提供对插件功能清晰明确的描述，例如：`@Description("百度搜索插件，用于查询百度上的新闻事件等信息")`
3. 如果 Function Impl 实现较为复杂，需要使用一些自定义函数，方法命名规范为：`${name}Tool`，例如：`BaiduSearchTool`, 目录层级和实现类保持一致
4. 请在根目录 pom.xml 中添加 module 配置，如 `<module>community/tool-calls/spring-ai-alibaba-starter-tool-calling-baidusearch</module>`
5. 请在插件 pom.xml 文件中只保留必须的传递依赖，插件版本应与 Spring AI Alibaba 统一，日志依赖统一使用 `org.slf4j`，其余依赖尽可能少引用或不引用

6. Properties配置属性类应并继承来自`spring-ai-alibaba-starter-tool-calling-common`模块的`CommonToolCallProperties`，并且需要写上`@ConfigurationProperties(prefix = ${name}Properties.${NAME}_PREFIX)`。
即使这个配置类不需要额外的属性字段，也需要定义一个`CommonToolCallProperties`的子类。在Properties类中定义一个属性前缀常量，构造方法中初始化该服务的基础URL，以及部分重要属性用户没有给出时可以根据哪个环境变量读取。之后该类可以定义自己独有的属性。一个Properties类的实例如下：

```java
@ConfigurationProperties(prefix = BaiDuTranslatePrefix)
public class BaiduTranslateProperties extends CommonToolCallProperties {
   protected static final String BaiDuTranslatePrefix = TOOL_CALLING_CONFIG_PREFIX + ".baidu.translate";
   private static final String TRANSLATE_HOST_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate/";

   public BaiduTranslateProperties() {
      super(TRANSLATE_HOST_URL);
      this.setPropertiesFromEnv(null, "BAIDU_TRANSLATE_SECRET_KEY", "BAIDU_TRANSLATE_APP_ID", null);
   }
}
```

7. 在Function Impl中，JSON的序列化与反序列化统一使用`spring-ai-alibaba-starter-tool-calling-common`模块的`JsonParseTool`对象，common模块自动注入了一个默认的Bean，如果有特殊需求也可以自定义`objectMapper`，在`AutoConfiguration`中覆盖原有的`JsonParseTool`的Bean。
HTTP请求统一使用common模块的`RestClientTool`或者`WebClientTool`的对象，该类有`builder`方法，必要`CommonToolCallProperties`和`JsonParseTool`对象，根据需要也可以自定义其他对象。
8. Auto Configuration 类中，应该声明一个Function Impl的Bean，供用户使用，且 Bean 的名称应该在对应的 Constants 类给出，常量的名称应为`TOOL_NAME`。例如：

```java
@Configuration
@EnableConfigurationProperties(BaiduTranslateProperties.class)
@ConditionalOnProperty(prefix = BaiduTranslateConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true",
  matchIfMissing = true)
public class BaiduTranslateAutoConfiguration {
 @Bean(name = BaiduTranslateConstants.TOOL_NAME)
 @ConditionalOnMissingBean
 @Description("Baidu translation function for general text translation")
 public BaiduTranslateService baiduTranslate(BaiduTranslateProperties properties, JsonParseTool jsonParseTool) {

  return new BaiduTranslateService(properties, RestClientTool.builder(jsonParseTool, properties).build(),
    jsonParseTool);
 }
}
```

如果该插件有多个 ToolFunction Bean，Constants 类可以分别定义每个 Bean 名称的常量，但必须以 `_TOOL_NAME` 结尾，比如`GET_ADDRESS_TOOL_NAME`。

9. 对于多个模块可能共用的方法，应该写到common模块的`CommonToolCallUtils`类中。对于多个模块可能共用的常量，应该写到common模块的`CommonToolCallConstants`中。
10. 对于 Properties 类中需要用户隐私的信息（比如 API Key），需要在 Constants 类定义一个环境变量名词，并在 Properties 类的构造方法中读取系统环境变量的值，例如：

```java
// BaiduTranslateConstants
public static final String SECRET_KEY_ENV = "BAIDU_TRANSLATE_SECRET_KEY";

// BaiduTranslateProperties
public BaiduTranslateProperties() {
 this.secretKey = System.getenv(SECRET_KEY_ENV);
}
```

11. 每一个插件都需要编写单元测试类。对于需要用户隐私的信息（比如 API Key）的插件，可以在测试方法上标注 `@EnabledIfEnvironmentVariable(named = XXXConstants.API_KEY_ENV, matches = CommonToolCallConstants.NOT_BLANK_REGEX)` 注解，保证自己本地能够通过单元测试。

当前已提供的工具列表信息

本项目提供了 **44 个工具模块**，按功能分为 10 大类别，为 AI Agent 提供丰富的工具调用能力。

### 1. 搜索工具

| 模块名称           | 功能描述                     | 备注            |
|----------------|--------------------------|---------------|
| serpapi        | 使用 SerpApi 搜索引擎查询最新新闻和信息 | 需要 API Key    |
| baidusearch    | 使用百度搜索引擎查询最新新闻和信息        | 支持百度搜索和百度AI搜索 |
| duckduckgo     | 使用 DuckDuckGo 搜索引擎查询最新新闻 | 注重隐私保护        |
| bravesearch    | 使用 Brave 搜索引擎进行查询        | 需要 API Key    |
| tavilysearch   | 使用 Tavily 搜索引擎执行搜索任务     | 专为 AI 优化      |
| aliyunaisearch | 阿里云 AI 网络搜索服务            | 需要阿里云账号       |
| metaso         | 秘塔 AI 网络搜索服务             | 需要 API Key    |
| googlescholar  | Google Scholar 学术搜索服务    | 学术论文检索        |
| openalex       | OpenAlex 学术搜索服务          | 开放学术数据库       |
| searches       | 搜索工具聚合模块                 | 集成多个搜索插件      |

### 2. 翻译工具

| 模块名称               | 功能描述         | 备注               |
|--------------------|--------------|------------------|
| googletranslate    | 实现自然语言翻译功能   | 使用 Google 翻译 API |
| microsofttranslate | 实现自然语言翻译功能   | 使用微软翻译 API       |
| alitranslate       | 实现自然语言翻译功能   | 使用阿里云翻译          |
| baidutranslate     | 百度通用文本翻译功能   | 需要百度翻译 API Key   |
| youdaotranslate    | 使用有道翻译实现翻译功能 | 需要有道 API Key     |

### 3. 地图与位置服务

| 模块名称        | 功能描述                            | 备注             |
|-------------|---------------------------------|----------------|
| baidumap    | 百度地图服务：地点搜索、地址查询、详情获取、天气查询      | 多功能集成          |
| amap        | 高德地图：根据地址获取天气信息                 | 需要高德 API Key   |
| tencentmap  | 腾讯地图：查询指定位置的天气状况                | 需要腾讯地图 API Key |
| opentripmap | 使用 OpenTripMap API 搜索地点、获取详情和坐标 | 旅游景点数据         |
| tripadvisor | TripAdvisor Content API：位置详情和搜索 | 旅游评论数据         |

### 4. 数据与信息查询

| 模块名称          | 功能描述                          | 备注               |
|---------------|-------------------------------|------------------|
| weather       | 使用 WeatherAPI 获取天气信息          | 需要 API Key       |
| wikipedia     | 使用关键词搜索维基百科信息                 | 免费使用             |
| worldbankdata | 世界银行发展数据搜索服务                  | 经济发展数据           |
| tushare       | 根据股票代码或日期获取股票日行情数据（最多 6000 条） | 需要 Tushare Token |
| googletrends  | Google Trends 数据分析 API        | 趋势分析             |

### 5. 新闻资讯

| 模块名称        | 功能描述     | 备注    |
|-------------|----------|-------|
| toutiaonews | 获取今日头条新闻 | 国内新闻源 |
| sinanews    | 获取新浪新闻   | 国内新闻源 |

### 6. 通讯工具

| 模块名称      | 功能描述                               | 备注         |
|-----------|------------------------------------|------------|
| dingtalk  | 使用自定义机器人发送钉钉群聊消息                   | 需要 Webhook |
| larksuite | 飞书文档、消息、表格操作：创建文档、发送消息（群聊和单聊）、创建表格 | 多功能集成      |

### 7. 开发工具

| 模块名称          | 功能描述                                       | 备注              |
|---------------|--------------------------------------------|-----------------|
| githubtoolkit | GitHub 工具集：获取 Issue、创建 Pull Request、搜索仓库列表 | 需要 GitHub Token |
| jinacrawler   | Jina Reader 爬虫服务插件                         | 网页内容提取          |
| firecrawl     | Firecrawl 爬虫服务插件                           | 网页爬取服务          |

### 8. 内容处理工具

| 模块名称            | 功能描述                                 | 备注        |
|-----------------|--------------------------------------|-----------|
| jsonprocessor   | 使用 Gson 处理 JSON：插入属性、解析对象、删除属性、替换字段值 | JSON 数据操作 |
| regex           | 使用正则表达式查找内容                          | 文本模式匹配    |
| sensitivefilter | 过滤和替换文本中的敏感信息：手机号、身份证、银行卡等           | 隐私保护      |

### 9. 文档知识库

| 模块名称  | 功能描述                        | 备注         |
|-------|-----------------------------|------------|
| yuque | 语雀 API 操作：创建/查询/更新/删除知识库和文档 | 需要语雀 Token |

### 10. 基础设施与工具

| 模块名称              | 功能描述                                   | 备注               |
|-------------------|----------------------------------------|------------------|
| kuaidi100         | 查询快递物流跟踪信息                             | 需要快递 100 API Key |
| minio             | MinIO 对象存储操作：上传、下载、删除、检查对象是否存在         | 需要 MinIO 服务      |
| memcached         | Memcached 缓存客户端服务                      | 需要 Memcached 服务  |
| time              | 获取指定城市的时间                              | 时区查询             |
| ollamasearchmodel | 从 Ollama 搜索模型信息                        | 需要 Ollama 服务     |
| python            | 执行 Python 代码并返回结果（沙箱环境，带错误处理）          | 基于 GraalVM       |
| agentbay          | AgentBay 沙箱执行环境：创建沙箱、执行命令、文件操作、HTTP 服务 | 安全执行环境           |
| common            | 通用工具调用基础类和工具                           | 基础模块             |

### 统计信息

- **工具总数**：44 个
- **搜索工具**：10 个
- **翻译工具**：5 个
- **地图位置**：5 个
- **数据信息**：5 个
- **新闻资讯**：2 个
- **通讯工具**：2 个
- **开发工具**：3 个
- **内容处理**：3 个
- **文档知识**：1 个
- **基础设施**：8 个

### 使用说明

1. **模块命名**：所有工具模块统一使用 `spring-ai-alibaba-starter-tool-calling-{name}` 命名格式
2. **依赖引入**：在项目中添加对应模块的 Maven 依赖即可自动配置
3. **配置项**：大部分工具需要配置 API Key 或服务地址，可通过 `application.yml` 或环境变量配置
4. **环境变量**：敏感信息（如 API Key）建议使用环境变量配置，具体变量名请参考各模块的 Constants 类
5. **单元测试**：每个模块都提供了完整的单元测试，可作为使用示例参考
