# CSDN Document Reader

CSDN Document Reader 是一个用于读取 CSDN 公开单篇文章的文档读取器。它会提取正文并转换为 Spring AI 可处理的 `Document`。

CSDN Document Reader is a document reader for public single CSDN articles. It extracts article content and converts it to Spring AI `Document`.

## 功能特点 | Features

- 支持输入单篇 CSDN 文章 URL（`/article/details/{id}`）
- 先提取正文主块，再进行 HTML 到文本解析
- 自动清理常见广告与噪声节点（脚本、推荐区等）
- 返回单个 `Document`（便于直接进入 RAG 流程）
- 提供简洁元数据：`source`、`article_id`、`title`、`author`
- 支持解析失败时自动降级到 Jsoup 纯文本提取

- Support single CSDN article URL (`/article/details/{id}`)
- Extract main article block first, then parse HTML into text
- Remove common ads and noisy nodes (scripts/recommend sections)
- Return one `Document` for direct RAG ingestion
- Provide minimal metadata: `source`, `article_id`, `title`, `author`
- Fallback to Jsoup plain-text extraction when parser fails

## 使用方法 | Usage

### Maven 依赖 | Maven Dependency

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-document-reader-csdn</artifactId>
    <version>${version}</version>
</dependency>
```

### 基础示例 | Basic Example

```java
String url = "https://blog.csdn.net/username/article/details/123456789";

CsdnDocumentReader reader = new CsdnDocumentReader(url);
List<Document> documents = reader.get();

Document doc = documents.get(0);
String text = doc.getText();
Map<String, Object> metadata = doc.getMetadata();
```

### 高级示例（注入 WebClient）| Advanced Example (Inject WebClient)

```java
WebClient webClient = WebClient.builder()
    .defaultHeader(HttpHeaders.ACCEPT, "text/html")
    .build();

CsdnDocumentReader reader = new CsdnDocumentReader(url, webClient);
List<Document> documents = reader.get();
```

## 输出格式 | Output Format

当前实现每篇文章返回一个 `Document`：

Current implementation returns one `Document` per article:

- `text`:
  - `Article Title: {title}`
  - `Content: {body}`
- `metadata`:
  - `source`: 原始文章 URL
  - `article_id`: 从 URL 提取的文章 ID
  - `title`: 文章标题
  - `author`: 作者

## 设计说明 | Design Notes

- 仅支持公开文章页面，不需要登录态
- 默认使用 `BsHtmlDocumentParser` 解析正文 HTML
- 若解析器异常，会自动使用 Jsoup 纯文本降级，保证可用性
- 当前为“纯文本优先”策略，更贴合常见文本 RAG

- Only public article pages are supported, no authentication needed
- Uses `BsHtmlDocumentParser` by default for article HTML parsing
- Falls back to Jsoup plain text on parser failure for reliability
- Current strategy is text-first, suitable for common text RAG pipelines

## 注意事项 | Notes

1. 当前不支持批量 URL 输入；如需批量，请在业务侧循环调用
2. CSDN 页面结构可能变化，建议保留回归测试
3. 文章中的复杂样式（代码块高亮、布局）会被转换为纯文本语义
4. 对于登录可见、VIP 或付费文章，页面可能只返回部分可见正文，提取结果可能出现截断

1. Batch URL input is not supported; loop in your application if needed
2. CSDN page structure may change, keep regression tests in place
3. Complex page styles (highlight/layout) are flattened into plain text semantics
4. For login-only, VIP, or paid articles, the page may expose only partial visible content, so extracted text can be truncated

## 测试 | Testing

### 单元测试 | Unit Tests

```bash
mvn -pl document-readers/spring-ai-alibaba-starter-document-reader-csdn test -DskipITs
```

### 集成测试 | Integration Tests

设置环境变量后运行：

Run with environment variables:

```bash
set CSDN_IT_ENABLED=true
set CSDN_ARTICLE_URL=https://blog.csdn.net/username/article/details/123456789
mvn -pl document-readers/spring-ai-alibaba-starter-document-reader-csdn -Dtest=CsdnDocumentReaderIT test
```

## 许可证 | License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
