# Bilibili Document Reader

Bilibili Document Reader 是一个用于加载和处理 Bilibili 视频信息和字幕的文档读取器。它可以将 Bilibili 视频的元数据和字幕内容转换为 Spring AI 可以处理的 Document 对象。

Bilibili Document Reader is a document reader for loading and processing Bilibili video information and subtitles. It converts Bilibili video metadata and subtitle content into Document objects that can be processed by Spring AI.

## 功能特点 | Features

- 支持通过 BV 号或完整 URL 读取 Bilibili 视频信息
- 自动获取视频标题、描述和字幕内容
- 支持多分片视频（多P视频），自动合并所有分片的字幕
- 支持批量处理多个视频
- 提供结构化的元数据（bvid、document_type、title）便于识别和过滤
- 支持 Bilibili 账号凭证认证（SESSDATA、bili_jct、buvid3）
- 与 Spring AI 框架无缝集成

- Support reading Bilibili video information via BV ID or full URL
- Automatically fetch video title, description, and subtitle content
- Support multi-part videos, automatically merge subtitles from all parts
- Support batch processing of multiple videos
- Provide structured metadata (bvid, document_type, title) for easy identification and filtering
- Support Bilibili account credential authentication (SESSDATA, bili_jct, buvid3)
- Seamless integration with Spring AI framework

## 使用方法 | Usage

### Maven 依赖 | Maven Dependency

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-document-reader-bilibili</artifactId>
    <version>${version}</version>
</dependency>
```

### 代码示例 | Code Example

#### 单个视频 | Single Video

```java
// 创建凭证
// Create credentials
BilibiliCredentials credentials = BilibiliCredentials.builder()
    .sessdata("your_sessdata")
    .biliJct("your_bili_jct")
    .buvid3("your_buvid3")
    .build();

// 创建资源（支持 BV 号或完整 URL）
// Create resource (supports BV ID or full URL)
BilibiliResource resource = new BilibiliResource(
    "https://www.bilibili.com/video/BV1xx411c7mD/",
    credentials
);

// 或使用 BV 号
// Or use BV ID
BilibiliResource resource = new BilibiliResource("BV1xx411c7mD", credentials);

// 创建 reader 并获取文档
// Create reader and get documents
BilibiliDocumentReader reader = new BilibiliDocumentReader(resource);
List<Document> documents = reader.get();
```

#### 批量处理多个视频 | Batch Processing Multiple Videos

```java
// 创建多个资源
// Create multiple resources
List<BilibiliResource> resources = List.of(
    new BilibiliResource("BV1xx411c7mD", credentials),
    new BilibiliResource("BV1yy422c8mE", credentials)
);

// 使用资源列表创建 reader
// Create reader with resource list
BilibiliDocumentReader reader = new BilibiliDocumentReader(resources);
List<Document> documents = reader.get();
```

### 输出格式 | Output Format

每个视频会生成两个 Document：

Each video generates two Documents:

1. **元数据文档 | Metadata Document**
   - 内容：`"Video information"`
   - Metadata：`bvid`、`document_type`（值为 "metadata"）、`title`、`description`

2. **内容文档 | Content Document**
   - 内容：格式化的字符串，包含视频标题、描述和完整字幕（所有分片合并）
   - Metadata：`bvid`、`document_type`（值为 "content"）、`title`
   - 格式：`Video Title: xxx, Description: xxx\nTranscript: [merged subtitles from all parts]`

### 通过 Metadata 过滤文档 | Filter Documents by Metadata

```java
// 获取特定视频的内容文档
// Get content document for a specific video
Document contentDoc = documents.stream()
    .filter(doc -> "BV1xx411c7mD".equals(doc.getMetadata().get("bvid")))
    .filter(doc -> "content".equals(doc.getMetadata().get("document_type")))
    .findFirst()
    .orElse(null);
```

## 获取 Bilibili 凭证 | Obtaining Bilibili Credentials

1. 登录 Bilibili 网站
2. 打开浏览器开发者工具（F12）
3. 切换到 "Application" 或 "存储" 标签
4. 在 Cookies 中找到以下值：
   - `SESSDATA`
   - `bili_jct`
   - `buvid3`（可选）

1. Log in to Bilibili website
2. Open browser developer tools (F12)
3. Switch to "Application" or "Storage" tab
4. Find the following values in Cookies:
   - `SESSDATA`
   - `bili_jct`
   - `buvid3` (optional)

## 注意事项 | Notes

1. 需要有效的 Bilibili 账号凭证才能访问字幕 | Valid Bilibili account credentials are required to access subtitles
2. 仅支持有字幕的视频，默认获取第一个字幕（通常为AI生成字幕或作者上传字幕） | Only videos with subtitles are supported, fetches the first subtitle by default (usually AI-generated or author-uploaded subtitles)
3. 多分片视频会自动合并所有分片的字幕 | Multi-part videos will automatically merge subtitles from all parts
4. 凭证信息敏感，请妥善保管，不要提交到代码仓库 | Credentials are sensitive, please keep them safe and do not commit to code repository
5. 建议使用环境变量存储凭证信息 | Recommended to store credentials in environment variables

## 测试 | Testing

### 单元测试 | Unit Tests

```bash
mvn test
```

### 集成测试 | Integration Tests

需要设置环境变量：

Environment variables required:

```bash
export BILIBILI_SESSDATA=your_sessdata
export BILIBILI_BILI_JCT=your_bili_jct
export BILIBILI_BUVID3=your_buvid3

mvn test -Dtest=BilibiliDocumentReaderIT
```

## License | 许可证

本项目采用 Apache License 2.0 协议。详情请参见 LICENSE 文件。

This project is licensed under the Apache License 2.0. See the LICENSE file for details.
