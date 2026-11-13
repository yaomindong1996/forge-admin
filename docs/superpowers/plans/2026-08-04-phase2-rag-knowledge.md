# 二期：RAG 知识库 · 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建企业级 RAG 知识库——存储实例管理、知识库 CRUD、文档处理流水线（解析→分块→向量化）、混合检索（向量+BM25+融合+Rerank）、Agent 绑定（Forced 模式）。

**Architecture:** 在 `forge-plugin-ai` 内新增 `knowledge` 和 `rag` 子包。多后端向量存储工厂（首期 Milvus），文档处理用策略模式（解析器/分块器），检索用责任链（向量→BM25→融合→Rerank→Finalize）。参考 snail-ai 的 RAG 架构。

**Tech Stack:** Java 17、Spring Boot 3、Spring AI 2.0.0、milvus-sdk-java（直连）、Tika、PDFBox、POI、Reactor、Vue 3 + Naive UI

**Spec:** `docs/superpowers/specs/2026-08-04-ai-upgrade-master-design.md`

## 决策记录（2026-08-04 评审确认）

- **前置**：依赖 Phase 0（Spring AI 2.0.0 升级冻结）+ 一期（模型类型细分、`AiSecretCrypto` legacy 加密、`AiModelAdapterRegistry.getEmbedding/getRerank`、`forge-plugin-ai` 加 `crypto`/`file` 依赖）。
- **向量存储**：首期 Milvus，**直连 milvus-sdk-java**（不用 Spring AI `MilvusVectorStore` 组件）——只有直连 SDK 才能管理 collection 的 sparse 向量/BM25 全文本索引（三期 `Bm25SearchHandler` 依赖）。`VectorStore` 接口 add/search/delete 直接操作 Milvus client（float 向量 insert + 全文索引）。
- **逻辑删除**：四张新表统一 `del_flag bigint NOT NULL DEFAULT 0` + `@TableLogic(value="0", delval="id")`，**无 `logic_delete_active` 生成列**，唯一键直接建在 `del_flag` 上（与 `ai_model`/`ai_agent` 一致）。
- **文档解析**：精准 parser（PDFBox/POI/Markdown）+ Tika 兜底未知类型。
- **分块器**：Spring AI 2.0.0 自带 `TokenTextSplitter`，**不新增 LangChain4j 依赖**。
- **文件依赖**：`forge-plugin-ai` 新增 `forge-starter-file`（文档上传/预览/结果图存储走 sys_file）。
- **异步线程池**：`forge-plugin-ai` 新增 `AiAsyncConfig`（`@EnableAsync` + `aiDocProcessExecutor`），流水线 `@Async("aiDocProcessExecutor")`，不依赖其他模块是否带 `@EnableAsync`。
- **SSE 通道**：沿用现有 WebFlux `Flux<ServerSentEvent>`（与 `AiClientController` 一致），不用 MVC `SseEmitter`。
- **去重策略**：Service 层按 `dedup_strategy` 先行查重（name 比对 `knowledge_id+doc_name`，content 比对 `content_hash`），命中按 `dedup_action` 处理；DB 唯一键作兜底，不作策略载体。
- **向量去重**：同内容 chunk **各自存向量**（不共享 `vector_id`），`content_hash` 仅用于重复导入判定（skip/reject）。
- **QA 链路**：二期简化链路——纯向量检索 + 拼 `<documents>` XML + 单轮 ChatModel 回答（Forced 式），SSE 流式；三期引擎上线后自然切换。
- **Agent 绑定**：`ai_agent` 新增列（`knowledge_ids`/`rag_mode`）**统一收拢到三期 V1.0.88**，本期 V1.0.87 只建 RAG 四表 + 字典 + 菜单。Task 9 依赖三期列，**本期不实现 Agent 绑定**。

## Global Constraints

- 查询 SQL 必须写 Mapper XML（`DataScopeInterceptor`），禁止 Service 层用 `LambdaQueryWrapper`
- 业务数据 `tenant_id` 必须为 `1`
- 分页参数：`pageNum`/`pageSize`
- 逻辑删除默认：`del_flag bigint NOT NULL DEFAULT 0`，数值主键表 `@TableLogic(value = "0", delval = "id")`，唯一键直接建在 `del_flag` 上（无生成列）
- API Key 脱敏（前4后4），禁止日志打印；Embedding/Rerank Key 用一期 `AiSecretCrypto`（复用 `PersistentCryptoService`）加密存储
- `forge-plugin-ai` 需新增依赖：`forge-starter-file`（文档解析/存储）、milvus-sdk-java（向量）、PDFBox/POI/Tika（解析）、`forge-starter-crypto`（一期已加）
- Flyway 迁移单调递增（> 1.0.82），SQL 幂等，`NOT EXISTS` 防重复
- 字典不硬编码；菜单 `sys_resource` 带 `NOT EXISTS`
- 禁止 Service 互相注入，跨 Service 协调上提 Controller
- 基础包：`com.mdframe.forge`

---

### Task 1: RAG 数据库迁移（表结构 + 字典 + 菜单）

**Files:**
- Create: `forge-server/db/mysql/migration/V1.0.87__add_ai_knowledge_rag.sql`

**Interfaces:**
- Produces: 表 `ai_store_instance`、`ai_knowledge`、`ai_knowledge_document`、`ai_knowledge_chunk`；字典 `ai_store_instance_category`、`ai_vector_store_type`、`ai_knowledge_status`；菜单"知识库"分组

- [ ] **Step 1: 创建向量存储实例表**

```sql
CREATE TABLE IF NOT EXISTS `ai_store_instance` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `instance_name` varchar(100) NOT NULL COMMENT '实例名称',
  `category` varchar(32) NOT NULL COMMENT '类别(vector_store/search_engine)',
  `store_type` varchar(32) NOT NULL COMMENT '类型(MILVUS/PG_VECTOR/ELASTICSEARCH)',
  `config_json` longtext NOT NULL COMMENT '连接配置JSON(host/port/user/token/database等)',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI向量存储/搜索引擎实例';
```

> 注：`del_flag` 统一 `bigint`，`@TableLogic(value="0", delval="id")`，唯一键直接建在 `del_flag` 上，**无 `logic_delete_active` 生成列**（与 `ai_model`/`ai_agent` 一致）。

- [ ] **Step 2: 创建知识库表**

```sql
CREATE TABLE IF NOT EXISTS `ai_knowledge` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `knowledge_name` varchar(100) NOT NULL COMMENT '知识库名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `vector_store_instance_id` bigint DEFAULT NULL COMMENT '向量存储实例ID',
  `embedding_model_id` bigint DEFAULT NULL COMMENT 'Embedding模型ID',
  `rerank_model_id` bigint DEFAULT NULL COMMENT 'Rerank模型ID',
  `dimension_of_vector_model` int DEFAULT NULL COMMENT '向量维度(显式覆盖)',
  `chunk_strategy` varchar(32) DEFAULT 'length' COMMENT '分块策略(length/delimiter/regex/smart/qa)',
  `chunk_config_json` longtext DEFAULT NULL COMMENT '分块参数JSON(max_tokens/overlap/delimiters/regex)',
  `search_config_json` longtext DEFAULT NULL COMMENT '检索参数JSON(topK/threshold/fusion/rerank_enable/nearby_count)',
  `dedup_strategy` varchar(32) DEFAULT 'none' COMMENT '去重策略(none/name/content/name_or_content)',
  `dedup_action` varchar(32) DEFAULT 'reject' COMMENT '冲突处理(reject/skip/overwrite)',
  `upload_confirm` char(1) DEFAULT '0' COMMENT '两步上传(0否 1是)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_knowledge_name_active` (`tenant_id`, `knowledge_name`, `del_flag`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库';
```

- [ ] **Step 3: 创建文档表**

```sql
CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `file_id` bigint DEFAULT NULL COMMENT '文件ID(sys_file)',
  `doc_name` varchar(255) NOT NULL COMMENT '文档名称',
  `doc_type` varchar(32) DEFAULT NULL COMMENT '文档类型(pdf/word/excel/markdown/txt/html/url/manual)',
  `source_type` varchar(32) DEFAULT 'upload' COMMENT '来源(upload/url/manual/db)',
  `source_url` varchar(1000) DEFAULT NULL COMMENT 'URL来源',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容SHA-256(去重)',
  `chunk_count` int DEFAULT 0 COMMENT '分块数',
  `process_status` varchar(32) DEFAULT 'pending' COMMENT '处理状态(pending/processing/success/failed)',
  `process_error` varchar(1000) DEFAULT NULL COMMENT '处理错误信息',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_knowledge_doc_name_active` (`knowledge_id`, `doc_name`, `del_flag`),
  KEY `idx_knowledge` (`knowledge_id`),
  KEY `idx_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档';
```

- [ ] **Step 4: 创建分块表**

```sql
CREATE TABLE IF NOT EXISTS `ai_knowledge_chunk` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `knowledge_id` bigint NOT NULL COMMENT '知识库ID',
  `document_id` bigint NOT NULL COMMENT '文档ID',
  `chunk_index` int NOT NULL COMMENT '分块序号',
  `content` longtext NOT NULL COMMENT '分块内容',
  `title` varchar(500) DEFAULT NULL COMMENT '分块标题(可选)',
  `token_count` int DEFAULT 0 COMMENT 'token数',
  `vector_id` varchar(200) DEFAULT NULL COMMENT '向量ID(每分块独立，不跨文档共享)',
  `ref_count` int DEFAULT 1 COMMENT '保留列(各自向量方案恒为1，不参与逻辑)',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '内容哈希',
  `retrieval_count` int DEFAULT 0 COMMENT '被检索次数',
  `create_by` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_dept` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志(0正常，删除后写主键)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chunk_active` (`document_id`, `chunk_index`, `del_flag`),
  KEY `idx_document` (`document_id`),
  KEY `idx_knowledge` (`knowledge_id`),
  KEY `idx_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库分块';
```

- [ ] **Step 5: 新增字典与菜单（NOT EXISTS 防重复）**

新增字典：`ai_store_instance_category`（vector_store/search_engine）、`ai_vector_store_type`（MILVUS/PG_VECTOR/ELASTICSEARCH）、`ai_knowledge_process_status`（pending/processing/success/failed）。
新增菜单：`知识库`（分组）→ `知识库管理` / `存储实例` / `检索调试`，`INSERT INTO sys_resource ... SELECT ... WHERE NOT EXISTS`，`tenant_id = 1`。参照 `V1.0.18__add_ai_model_routing_governance.sql` 的 `sys_resource` 写法。

> 注：V1.0.87 **只建 RAG 四表 + 字典 + 菜单，不动 `ai_agent`**（`ai_agent` 新增列统一在 V1.0.88）。

- [ ] **Step 6: 提交**

```bash
git add forge-server/db/mysql/migration/V1.0.87__add_ai_knowledge_rag.sql
git commit -m "feat(db): RAG 知识库表结构、字典与菜单"
```

---

### Task 2: 实体类与 Mapper

**Files:**
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/domain/AiStoreInstance.java`
- Create: `.../knowledge/domain/AiKnowledge.java`
- Create: `.../knowledge/domain/AiKnowledgeDocument.java`
- Create: `.../knowledge/domain/AiKnowledgeChunk.java`
- Create: `.../knowledge/mapper/AiStoreInstanceMapper.java` + `AiStoreInstanceMapper.xml`
- Create: `.../knowledge/mapper/AiKnowledgeMapper.java` + `AiKnowledgeMapper.xml`
- Create: `.../knowledge/mapper/AiKnowledgeDocumentMapper.java` + `AiKnowledgeDocumentMapper.xml`
- Create: `.../knowledge/mapper/AiKnowledgeChunkMapper.java` + `AiKnowledgeChunkMapper.xml`

**Interfaces:**
- Consumes: Task 1 的表结构
- Produces: 4 个实体（继承 `TenantEntity`）+ 4 个 Mapper。Mapper XML 含：分页查询（`selectPage`）、按 knowledgeId 查文档、按 documentId 查分块、按 contentHash 查分块（防重复导入）、`retrieval_count` 累加（检索命中次数）

> 注：**无引用计数（ref_count）**——各自向量方案下每个分块独立，`content_hash` 仅用于防重复导入判定，不共享向量、不做 `ref_count++`。`ai_knowledge_chunk.ref_count` 列保留但恒为 1，不参与逻辑。

- [ ] **Step 1: 创建实体类**

每个实体继承 `TenantEntity`（含 `tenantId`），`@TableName("ai_xxx")`。**不重复写** `createBy`/`createTime`/`createDept`/`updateBy`/`updateTime`（由 `BaseEntity` 提供，`@TableField(fill=...)` 自动填充）。`delFlag` 用 `@TableLogic(value = "0", delval = "id")`（逻辑删除写主键）。参照 `AiProvider.java` / `AiModel.java` 的写法。

- [ ] **Step 2: 创建 Mapper 接口 + XML**

Mapper 接口继承 `BaseMapper<T>`，XML 里定义 `<sql id="...Columns">` 列片段（含 `del_flag`），查询显式 `del_flag = 0`（bigint 比较）。租户条件由 `TenantLineInnerInterceptor` 自动追加，XML 不写。

`AiKnowledgeMapper.xml` 分页查询：

```xml
<select id="selectKnowledgePage" resultType="com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledge">
    SELECT <include refid="knowledgeColumns" />
    FROM ai_knowledge
    <where>
        del_flag = '0'
        <if test="knowledgeName != null and knowledgeName != ''">
            AND knowledge_name LIKE CONCAT('%', #{knowledgeName}, '%')
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
    </where>
    ORDER BY create_time DESC
</select>
```

`AiKnowledgeChunkMapper.xml` 内容哈希去重查询：

```xml
<select id="selectByContentHash" resultType="com.mdframe.forge.plugin.ai.knowledge.domain.AiKnowledgeChunk">
    SELECT <include refid="chunkColumns" />
    FROM ai_knowledge_chunk
    WHERE del_flag = 0 AND content_hash = #{contentHash}
    LIMIT 1
</select>
```

其余 Mapper 按同样风格（查询必须显式 `del_flag = '0'`，参照 `AiModelMapper.xml`）。

- [ ] **Step 3: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/
git commit -m "feat(ai): 知识库实体与Mapper"
```

---

### Task 3: 向量存储工厂（Milvus）

**Files:**
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/store/VectorStore.java`（接口）
- Create: `.../rag/store/MilvusVectorStore.java`
- Create: `.../rag/store/VectorStoreFactory.java`
- Create: `.../rag/store/VectorStoreConfig.java`（Milvus 连接配置）
- Test: `.../src/test/java/com/mdframe/forge/plugin/ai/rag/store/VectorStoreFactoryTest.java`

**Interfaces:**
- Consumes: `AiStoreInstance`（store_type = MILVUS + config_json）、一期 `AiEmbeddingModelAdapter`（`embed(baseUrl, apiKey, model, texts) -> List<List<Float>>`，provider 解析在二期本 Task 闭环）
- Produces:
  - `VectorStore.add(String indexName, String content, String vectorId, List<Float> vector, String metadataJson)`
  - `VectorStore.search(String indexName, List<Float> queryVector, int topK, Map<String,String> filter) -> List<VectorHit>`（`VectorHit` 含 `vectorId`/`score`）
  - `VectorStore.delete(String indexName, String vectorId)`
  - `VectorStoreFactory.create(AiStoreInstance) -> VectorStore`（缓存）

> 注 1：`VectorStoreFactory`（向量存储）用"工厂"而非"注册表"——因为向量存储实例是**运行时按知识库配置动态创建**的（Milvus client + embedding model），不是固定的 Spring `@Component`。这与一期模型适配器的注册表（固定实现集合）不同，两类工厂解决不同问题。
>
> 注 2：**直连 milvus-sdk**，不用 Spring AI `MilvusVectorStore` 组件。Milvus collection 需同时建 **dense float 向量字段** + **sparse 全文本索引字段**（三期 BM25 依赖，`Bm25SearchHandler`）。向量写入走一期 adapter `embed()` 得 `List<List<Float>>`，直接 `MilvusClient.insert`（float 数组），不经过 `EmbeddingModel` 抽象。

- [ ] **Step 1: 写失败测试（工厂解析配置 + 类型路由）**

```java
package com.mdframe.forge.plugin.ai.rag.store;

import com.mdframe.forge.plugin.ai.knowledge.domain.AiStoreInstance;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VectorStoreFactoryTest {

    @Test
    void factoryCreatesMilvusFromConfig() {
        AiStoreInstance instance = new AiStoreInstance();
        instance.setStoreType("MILVUS");
        instance.setConfigJson("{\"host\":\"localhost\",\"port\":19530,\"database\":\"forge_ai\"}");
        VectorStoreFactory factory = new VectorStoreFactory();
        VectorStore store = factory.create(instance);
        assertNotNull(store);
        assertTrue(store instanceof MilvusVectorStore);
    }

    @Test
    void unsupportedStoreTypeThrows() {
        AiStoreInstance instance = new AiStoreInstance();
        instance.setStoreType("UNKNOWN");
        VectorStoreFactory factory = new VectorStoreFactory();
        assertThrows(IllegalArgumentException.class, () -> factory.create(instance));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=VectorStoreFactoryTest -DfailIfNoTests=false`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 VectorStore 接口与 Milvus 实现（直连 SDK）**

`VectorStore` 接口定义 add/search/delete 三方法（`add` 接收 float 向量，`search` 接收 query 向量）。
`MilvusVectorStore` 用 **milvus-sdk-java** `MilvusClientV2` 封装：
- collection schema：`id`（varchar，对应 `vector_id`）+ `content`（varchar，BM25 全文索引字段）+ `dense`（FLOAT_VECTOR）+ `metadata`（JSON）+ `sparse`（SPARSE_FLOAT_VECTOR，配合 SPARSE_INVERTED_INDEX，三期 BM25 用）
- indexName 对应知识库集合（`IndexNameBuilder.KNOWLEDGE.build(Map.of("knowledgeId", id))`，名称需符合 Milvus 集合命名规则）
- 连接**延迟**：`create` 只构建 `MilvusClientV2`（`MilvusServiceClient` 配置），不实际 `connect()`；首个 add/search/delete 时才触发连接
`VectorStoreFactory` 按 `storeType` 分派，实例按 `(instanceId, embeddingModelId)` 缓存（Caffeine，参照 `ChatClientCache` 的写法）。

> provider 解析闭环（一期 `AiEmbeddingModelAdapter` 只按 modelKey 匹配，无 provider 信息）：`VectorStoreFactory.create` 额外接收知识库的 `embeddingModelId`，通过 `AiModelService` 查 `AiModel.providerId` → `AiProvider`（`AiSecretCrypto.decrypt` 解密 apiKey）→ `adapter.embed(baseUrl, apiKey, modelId, texts)`。

- [ ] **Step 4: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=VectorStoreFactoryTest -DfailIfNoTests=false`
Expected: PASS（无 Milvus 时工厂应延迟连接——`create` 只构建客户端不实际连接）

- [ ] **Step 5: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/store/
git commit -m "feat(ai): 向量存储工厂与Milvus直连实现"
```

---

### Task 4: 文档解析器（策略模式）

**Files:**
- Create: `.../rag/parse/DocumentParser.java`（接口）
- Create: `.../rag/parse/PdfParser.java`、`WordParser.java`、`ExcelParser.java`、`MarkdownParser.java`、`TikaParser.java`
- Create: `.../rag/parse/DocumentParserFactory.java`
- Test: `.../rag/parse/DocumentParserFactoryTest.java`

**Interfaces:**
- Consumes: 无
- Produces: `DocumentParser.parse(InputStream, String filename) -> ParsedDocument`（`ParsedDocument` 含 `title`/`content`）；`DocumentParserFactory.getParser(String docType) -> DocumentParser`

- [ ] **Step 1: 写失败测试（按类型路由解析器）**

```java
package com.mdframe.forge.plugin.ai.rag.parse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentParserFactoryTest {

    @Test
    void factoryRoutesByDocType() {
        DocumentParserFactory factory = new DocumentParserFactory();
        assertTrue(factory.getParser("pdf") instanceof PdfParser);
        assertTrue(factory.getParser("word") instanceof WordParser);
        assertTrue(factory.getParser("excel") instanceof ExcelParser);
        assertTrue(factory.getParser("markdown") instanceof MarkdownParser);
        assertTrue(factory.getParser("txt") instanceof TikaParser);
    }

    @Test
    void markdownParserExtractsText() {
        DocumentParser parser = new MarkdownParser();
        ParsedDocument doc = parser.parse(
            new java.io.ByteArrayInputStream("# 标题\n\n正文内容".getBytes()),
            "test.md");
        assertEquals("标题", doc.getTitle());
        assertTrue(doc.getContent().contains("正文内容"));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=DocumentParserFactoryTest -DfailIfNoTests=false`
Expected: FAIL

- [ ] **Step 3: 实现解析器**

- `PdfParser`：PDFBox，按页提取文本
- `WordParser`：POI XWPFDocument（docx）
- `ExcelParser`：POI WorkbookFactory（xlsx/xls），转为 `表头:值` 形式
- `MarkdownParser`：剥离 Markdown 语法，`#` 标题映射为 `title`，保留正文结构
- `TikaParser`：Apache Tika 兜底（txt/html/rtf/ppt 等）
- `DocumentParserFactory`：`Map<String, DocumentParser>` 路由，未知类型返回 `TikaParser`

- [ ] **Step 4: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=DocumentParserFactoryTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/parse/
git commit -m "feat(ai): 文档解析器策略模式"
```

---

### Task 5: 分块器（4种策略 + TokenAware）

**Files:**
- Create: `.../rag/chunk/ChunkStrategy.java`（接口）
- Create: `.../rag/chunk/LengthChunkStrategy.java`、`DelimiterChunkStrategy.java`、`RegexChunkStrategy.java`、`SmartChunkStrategy.java`、`QaChunkStrategy.java`
- Create: `.../rag/chunk/TokenAwareChunker.java`
- Create: `.../rag/chunk/ChunkStrategyFactory.java`
- Test: `.../rag/chunk/ChunkStrategyFactoryTest.java`

**Interfaces:**
- Consumes: 一期 `AiEmbeddingModelAdapter` 或其 Chat 模型（Smart 分块用 LLM）
- Produces: `ChunkStrategy.chunk(String content, ChunkConfig config) -> List<TextChunk>`（`TextChunk` 含 `content`/`index`）；`ChunkStrategyFactory.getStrategy(String code) -> ChunkStrategy`；`TokenAwareChunker.chunk(String, int maxTokens, int overlap) -> List<String>`

- [ ] **Step 1: 写失败测试**

```java
package com.mdframe.forge.plugin.ai.rag.chunk;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChunkStrategyFactoryTest {

    @Test
    void lengthChunkSplitsByTokenBudget() {
        LengthChunkStrategy strategy = new LengthChunkStrategy();
        // maxTokens=10, 中文每字约1.5 token，30字应拆成多块
        List<TextChunk> chunks = strategy.chunk(
            "一二三四五六七八九十一二三四五六七八九十一二三四五六七八九十",
            new ChunkConfig("length", 10, 2, null, null));
        assertTrue(chunks.size() > 1);
        assertEquals(0, chunks.get(0).getIndex());
        // 有重叠
        assertEquals(chunks.get(0).getContent().charAt(0),
                     chunks.get(1).getContent().charAt(0));
    }

    @Test
    void delimiterChunkSplitsByCustomDelimiter() {
        DelimiterChunkStrategy strategy = new DelimiterChunkStrategy();
        List<TextChunk> chunks = strategy.chunk(
            "第一句。第二句。第三句。",
            new ChunkConfig("delimiter", 100, 0, new String[]{"。"}, null));
        assertEquals(3, chunks.size());
    }

    @Test
    void qaChunkKeepsQuestionWithAnswer() {
        QaChunkStrategy strategy = new QaChunkStrategy();
        List<TextChunk> chunks = strategy.chunk(
            "## 什么是高血压\n\n高血压是指...\n\n## 怎么预防\n\n预防措施...",
            new ChunkConfig("qa", 1000, 0, null, null));
        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).getContent().contains("什么是高血压"));
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=ChunkStrategyFactoryTest -DfailIfNoTests=false`
Expected: FAIL

- [ ] **Step 3: 实现分块器**

- `TokenAwareChunker`：Spring AI 2.0.0 自带 `TokenTextSplitter(maxTokens, overlap)`（**不引 LangChain4j**），CJK token 估算（`cjkCount*1.5 + asciiWordCount`）
- `LengthChunkStrategy`：整文档交给 `TokenAwareChunker`
- `DelimiterChunkStrategy`：按自定义分隔符先切，再 `TokenAwareChunker` 二次切
- `RegexChunkStrategy`：正则切分，再 `TokenAwareChunker`
- `SmartChunkStrategy`：LLM 语义分块（system prompt 要求输出 JSON 数组），大文档分段多轮调用
- `QaChunkStrategy`：`##` 标题视为问题，问题+答案保持一个 chunk，答案过长时带问题上下文切分（54Doctor 的 `MarkdownQaSplitter`）
- `ChunkConfig`：`(code, maxTokens, overlap, delimiters, regex)`

> ⚠️ Phase 0 升级到 Spring AI 2.0.0 后，`TokenTextSplitter` 的构造与 `split` 返回类型以 2.0.0 实际 API 为准（实施前探针验证）。分块结果统一为 `List<TextChunk>`（`content`/`index`），内部实现可换。

- [ ] **Step 4: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=ChunkStrategyFactoryTest -DfailIfNoTests=false`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/chunk/
git commit -m "feat(ai): 4种分块策略与TokenAware分块器"
```

---

### Task 6: 文档处理流水线（异步 + SSE 进度）

**Files:**
- Create: `.../rag/pipeline/DocumentPipeline.java`
- Create: `.../rag/pipeline/DocumentProcessService.java`
- Create: `.../rag/pipeline/DocumentPreviewService.java`（两步上传：解析+分块预览，不向量化）
- Create: `.../rag/pipeline/DocumentProgressPublisher.java`（WebFlux SSE 进度）
- Create: `.../rag/config/AiAsyncConfig.java`（`@EnableAsync` + `aiDocProcessExecutor` 线程池）
- Create: `.../rag/controller/AiKnowledgeDocumentController.java`（上传 + 进度订阅 + 确认入库）
- Test: `.../rag/pipeline/DocumentPipelineTest.java`

**Interfaces:**
- Consumes: Task 2-5（Mapper、解析器、分块器、VectorStore）、一期 Embedding
- Produces: `DocumentPipeline.process(documentId) -> ProcessResult`；`DocumentPreviewService.preview(documentId) -> List<TextChunk>`（两步上传预览，不向量化）；`DocumentProgressPublisher.subscribe(documentId) -> Flux<ServerSentEvent<String>>`；上传接口 `POST /ai/knowledge/document`（multipart，返回 documentId）、`POST /ai/knowledge/document/confirm`（两步上传确认入库）

> **异步线程池**：`AiAsyncConfig` 新增 `@EnableAsync` + 命名线程池 `aiDocProcessExecutor`（`ThreadPoolTaskExecutor`，参照 `LogThreadPoolConfig`/`FlowAutoConfiguration` 的写法），流水线 `@Async("aiDocProcessExecutor")`。不依赖其他模块是否带 `@EnableAsync`，`forge-plugin-ai` 单模块测试也确定生效。
> **SSE**：沿用 WebFlux `Flux<ServerSentEvent<String>>`（与 `AiClientController` 一致），不用 MVC `SseEmitter`。
> **两步上传分流**：按知识库 `upload_confirm` 开关（0/1）：
> - `0`（直接入库）：上传 → `@Async` 自动 `DocumentPipeline.process`（解析→分块→向量化）
> - `1`（两步上传）：上传 → 同步 `DocumentPreviewService.preview`（仅解析+分块，**不向量化**）→ 返回预览分块列表 → 用户确认后 `POST /ai/knowledge/document/confirm` 再 `@Async` 向量化

- [ ] **Step 1: 写失败测试（pipeline 状态机）**

```java
package com.mdframe.forge.plugin.ai.rag.pipeline;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentPipelineTest {

    @Test
    void unsupportedDocTypeMarksFailed() {
        // 使用 mock Mapper/解析器工厂，一个 .xyz 类型文档
        // pipeline.process 后 process_status 应为 failed
        // (具体断言依赖 DocumentProcessService 设计，此处为契约测试骨架)
    }
}
```

> 注：pipeline 强依赖 DB + Milvus，单元测试用 mock（Mockito）注入 `AiKnowledgeDocumentMapper`、`ChunkStrategyFactory`、`VectorStoreFactory`，验证状态流转（pending→processing→success/failed）。

- [ ] **Step 2: 实现 DocumentPipeline**

`process(documentId)` 步骤：
1. 查文档，`CAS` 状态 `pending→processing`（防并发重复处理）
2. `DocumentParserFactory.getParser(docType).parse()` 解析内容
3. 内容哈希查重（`selectByContentHash`）——**仅用于防重复导入判定**（skip/reject，按知识库 `dedup_strategy`），**不做向量复用**（各自存向量）
4. `ChunkStrategyFactory.getStrategy(knowledge.chunkStrategy).chunk()` 分块
5. 每 10 块一批，`VectorStore.add()` 向量化（一期 `AiEmbeddingModelAdapter.embed()` 返回 float 向量 → milvus-sdk insert）
6. 三期 BM25：二期先落 `vector_id` 与全文索引预留（collection 建 sparse 字段，正文写入 `content` 字段）
7. `DocumentProgressPublisher.publish(documentId, stage, count)` 推送进度
8. 更新文档 `chunk_count`、`process_status=success`；失败写 `process_error`、`process_status=failed`

- [ ] **Step 3: 实现异步触发 + 两步上传分流 + WebFlux SSE 进度**

`AiKnowledgeDocumentController`：
- `upload_confirm = 0`（直接入库）：上传后 `@Async("aiDocProcessExecutor")` 调 `DocumentPipeline.process`
- `upload_confirm = 1`（两步上传）：上传后同步 `DocumentPreviewService.preview`（仅解析+分块，不向量化）返回预览分块列表；用户确认后 `POST /ai/knowledge/document/confirm` 再 `@Async` 向量化入库
- `DocumentProgressPublisher` 用 `Flux<ServerSentEvent<String>>` 推送 `解析完成/分块完成/向量化完成(N块)` 事件（与现有 `AiClientController` 的 WebFlux SSE 一致）

- [ ] **Step 4: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/
git commit -m "feat(ai): 文档处理流水线与SSE进度"
```

---

### Task 7: 向量检索服务（二期基础版）

> 二期只做纯向量检索（Embedding 相似度）+ 阈值过滤 + 相邻分块扩展 + Lost-in-Middle 重排。
> **BM25 / 混合融合 / Rerank / 对话历史补全查询 顺延到三期**（三期 Task 2，依赖 Agent 引擎 + Milvus 内置 BM25）。

**Files:**
- Create: `.../rag/search/RagSearchContext.java`
- Create: `.../rag/search/RagSearchService.java`
- Create: `.../rag/search/VectorSearch.java`
- Create: `.../rag/search/LostInMiddleReorder.java`
- Create: `.../rag/search/RagSearchHit.java`
- Test: `.../rag/search/RagSearchServiceTest.java`

**Interfaces:**
- Consumes: Task 3（VectorStore）、Task 2（Mapper）
- Produces:
  - `RagSearchService.search(RagSearchRequest) -> List<RagSearchHit>`（`RagSearchHit` 含 `chunkId`/`content`/`documentId`/`score`）
  - `RagSearchRequest` 含 `knowledgeId`/`query`/`topK`/`thresholdEnabled`/`threshold`/`nearbySliceCount`
  - `LostInMiddleReorder.reorder(List<RagSearchHit>) -> List<RagSearchHit>`（最相关置首尾，避免中间位置被忽略）

- [ ] **Step 1: 写失败测试（Lost-in-Middle 重排）**

```java
package com.mdframe.forge.plugin.ai.rag.search;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class RagSearchServiceTest {

    @Test
    void lostInMiddlePutsTopToFrontAndSecondToEnd() {
        List<RagSearchHit> hits = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            hits.add(new RagSearchHit((long) i, "chunk" + i, "doc" + i, 1.0 - i * 0.1));
        }
        // score: hit0=1.0, hit1=0.9, hit2=0.8, hit3=0.7, hit4=0.6
        List<RagSearchHit> reordered = LostInMiddleReorder.reorder(hits);
        assertEquals(0L, reordered.get(0).getChunkId());      // 最强在首位
        assertEquals(1L, reordered.get(reordered.size() - 1).getChunkId()); // 次强在末位
    }

    @Test
    void thresholdFiltersLowScores() {
        RagSearchService service = new RagSearchService(null, null, null);
        RagSearchRequest req = RagSearchRequest.builder()
                .thresholdEnabled(true).threshold(0.7).topK(10).build();
        // 依赖 mock VectorStore 返回高低分混合结果，验证 <0.7 的被过滤
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -Dtest=RagSearchServiceTest -DfailIfNoTests=false`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 LostInMiddleReorder**

```java
package com.mdframe.forge.plugin.ai.rag.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Lost-in-the-Middle 重排：最相关置首、次相关置尾，避免 LLM 忽略中间内容。
 */
public final class LostInMiddleReorder {

    private LostInMiddleReorder() {
    }

    public static List<RagSearchHit> reorder(List<RagSearchHit> hits) {
        if (hits == null || hits.size() <= 2) {
            return hits == null ? new ArrayList<>() : new ArrayList<>(hits);
        }
        List<RagSearchHit> result = new ArrayList<>(hits.size());
        result.add(hits.get(0));                          // 最强置首
        result.add(hits.get(hits.size() - 1));            // 次强置尾
        for (int i = 1; i < hits.size() - 1; i++) {       // 其余按序填中间
            result.add(hits.get(i));
        }
        return result;
    }
}
```

- [ ] **Step 4: 实现 RagSearchService**

`search(request)`：
1. 用知识库 Embedding 模型对 `query` 向量化（一期 `AiEmbeddingModelAdapter.embed(query)`），得 `List<Float> queryVector`
2. `VectorStore.search(indexName, queryVector, topK*2)` 向量检索（按相似度返回）
3. 阈值过滤（`thresholdEnabled` 时过滤 `score < threshold`）
4. `nearbySliceCount > 0` 时按 `chunk_index` 扩展相邻分块（查 `ai_knowledge_chunk`）
5. `LostInMiddleReorder.reorder`
6. 截取 `topK`
7. 累加 `retrieval_count`（`ai_knowledge_chunk`）

- [ ] **Step 5: 运行测试验证通过**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/search/
git commit -m "feat(ai): 向量检索服务与Lost-in-Middle重排"
```

---

### Task 8: 知识库 CRUD 服务与控制器

**Files:**
- Create: `.../knowledge/service/AiKnowledgeService.java`
- Create: `.../knowledge/service/AiStoreInstanceService.java`
- Create: `.../knowledge/controller/AiKnowledgeController.java`
- Create: `.../knowledge/controller/AiStoreInstanceController.java`
- Create: `.../rag/controller/RagSearchController.java`
- Test: `.../knowledge/service/AiKnowledgeServiceTest.java`

**Interfaces:**
- Consumes: Task 2 Mapper、Task 3 工厂、Task 7 检索
- Produces: REST 接口 `GET/POST/PUT/DELETE /ai/knowledge`、`/ai/knowledge/:id/page`、`/ai/store-instance`、`POST /ai/knowledge/search`（检索调试）、`POST /ai/knowledge/qa`（QA 对话）

- [ ] **Step 1: 实现知识库 CRUD**

`AiKnowledgeService`：分页查询（Mapper XML）、新增（校验 embeddingModelId 存在）、更新（变更后清 `VectorStoreFactory` 缓存）、删除（连带删文档/分块，逻辑删除）。遵循"查询走 Mapper XML"。

- [ ] **Step 2: 实现存储实例 CRUD + 连接测试**

`AiStoreInstanceService`：CRUD + `testConnection`（用配置建 Milvus client，`listCollections()` 验证连通）。前端存储实例页可测。

- [ ] **Step 3: 实现检索调试 + QA 接口**

`POST /ai/knowledge/search`：接收 `RagSearchRequest`，返回 `RagSearchHit` 列表（含耗时分解：embedding/vector/total）。BM25/fusion/rerank 耗时段三期加入。
`POST /ai/knowledge/qa`：**二期简化链路**——纯向量检索（`RagSearchService`）+ 拼接 `<documents>` XML + 单轮 ChatModel 回答（Forced 式，不带工具调用/多轮上下文补全），WebFlux SSE 流式。三期 Agent 引擎上线后，QA 自然切换到引擎的检索增强工具。

- [ ] **Step 4: 运行测试**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/knowledge/ forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/
git commit -m "feat(ai): 知识库/存储实例CRUD与检索调试接口"
```

---

### Task 9: Agent 绑定知识库（Forced 模式）——依赖三期

> **范围调整**：`ai_agent` 新增列（`knowledge_ids`/`rag_mode`/`greeting`/`preset_questions`/`max_iters`/`tool_group_mode`）**统一收拢到三期 V1.0.88**，本期的 V1.0.87 只建 RAG 四表 + 字典 + 菜单，**不补 ai_agent 列**。
> 因此本 Task 的 Agent 绑定逻辑依赖三期列，**二期不实现**。保留本 Task 作为三期落地时的设计参考（RagForcedInjector 逻辑），前置的 Forced 检索可先在二期用 `RagSearchService` 直接验证。

**Files（三期落地）:**
- Modify: `.../ai/agent/domain/AiAgent.java`（三期加 `knowledgeIds`、`ragMode` 字段）
- Create: `.../rag/service/RagForcedInjector.java`
- Modify: `.../ai/chat/service/AiChatService.java`（三期对话前注入 RAG）
- Modify: `.../ai/agent/controller/AiAgentController.java`（三期 Agent 保存知识库绑定）
- Modify: `forge-server/db/mysql/migration/V1.0.88__add_agent_engine_event_skill.sql`（三期补 ai_agent 列）
- Test: `.../rag/service/RagForcedInjectorTest.java`

**Interfaces:**
- Consumes: 二期 Task 7 检索、现有 `AiAgent`
- Produces: `RagForcedInjector.inject(systemPrompt, agent, query) -> String`（检索结果拼入 `<documents>` XML）；`AiAgent` 新增 `knowledgeIds`（JSON 数组）/`ragMode`（`none`/`forced`/`smart`）

- [ ] **Step 1（三期）: Agent 实体新增字段**

三期 V1.0.88 给 `AiAgent` 补列（`knowledge_ids`/`rag_mode`/`greeting`/`preset_questions`/`max_iters`/`tool_group_mode`），幂等（MySQL 8 无 `ADD COLUMN IF NOT EXISTS`，用 `information_schema` + 动态 SQL，参考 V1.0.18 模板）：

```sql
-- 幂等补列（MySQL 8 无 ADD COLUMN IF NOT EXISTS，用 information_schema 判断）
SET @col1 = (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_agent' AND column_name = 'knowledge_ids');
SET @sql1 = IF(@col1 = 0, 'ALTER TABLE ai_agent ADD COLUMN knowledge_ids longtext DEFAULT NULL COMMENT ''绑定知识库ID列表(JSON)''', 'SELECT 1');
PREPARE s1 FROM @sql1; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @col2 = (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ai_agent' AND column_name = 'rag_mode');
SET @sql2 = IF(@col2 = 0, 'ALTER TABLE ai_agent ADD COLUMN rag_mode varchar(16) DEFAULT ''none'' COMMENT ''RAG模式(none/forced/smart)''', 'SELECT 1');
PREPARE s2 FROM @sql2; EXECUTE s2; DEALLOCATE PREPARE s2;
```

> ⚠️ MySQL 8 无 `ADD COLUMN IF NOT EXISTS`，需用 `information_schema` 判断（遵循 CLAUDE.md SQL 幂等规则）。

- [ ] **Step 2（三期）: 实现 Forced 注入**

`RagForcedInjector.inject`：
1. `ragMode != forced` 直接返回原 prompt
2. 按 `knowledgeIds` 并行检索各知识库（Reactor 并行）
3. 结果拼成 `<documents><document id='n'>...</document></documents>` 追加到 systemPrompt
4. 返回

- [ ] **Step 3（三期）: 接入 AiChatService**

在 `AiChatService` / `AiClientImpl` 构建 systemPrompt 处，若 `agent.ragMode == forced` 则调 `RagForcedInjector.inject`。

- [ ] **Step 4（三期）: Agent 保存绑定**

`AiAgentController` 新增/更新 Agent 时接收 `knowledgeIds`、`ragMode`，落库。

- [ ] **Step 5（三期）: 运行测试**

Run: `cd forge-server && mvn test -P enable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai`
Expected: PASS

- [ ] **Step 6（三期）: 提交**

```bash
git add forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/rag/service/ forge-server/forge-framework/forge-plugin-parent/forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/agent/ forge-server/db/mysql/migration/
git commit -m "feat(ai): Agent绑定知识库与Forced模式注入"
```

---

### Task 10: 前端知识库页面（Snail AI 方案）

**Files:**
- Create: `forge-admin-ui/src/views/ai/knowledge/index.vue`（知识库列表）
- Create: `forge-admin-ui/src/views/ai/knowledge/detail.vue`（详情 4Tab：文档/分块/搜索/Q&A）
- Create: `forge-admin-ui/src/views/ai/knowledge/components/DocumentsTab.vue`、`SlicesTab.vue`、`SearchTab.vue`、`QaTab.vue`
- Create: `forge-admin-ui/src/views/ai/store-instance/index.vue`（存储实例管理）
- Modify: `forge-admin-ui/src/api/ai.js`（知识库/文档/分块/检索/QA/存储实例接口）

**Interfaces:**
- Consumes: Task 8 接口、Task 9 接口
- Produces: 完整知识库管理 UI

- [ ] **Step 1: 知识库列表页**

卡片网格（参照 snail-ai-admin）：知识库名/描述/文档数/分块数/状态，新建/编辑/删除。新建抽屉含：名称/描述/存储实例选择/Embedding模型选择/Rerank模型选择/分块策略（4种配置面板）/去重策略/两步上传开关。

- [ ] **Step 2: 文档 Tab**

文档列表 + 上传（文件/URL/手动录入）、两步上传预览（解析→分块→确认入库）、重新解析、在线预览、删除。上传时显示处理进度（SSE）。

- [ ] **Step 3: 分块 Tab**

分块列表（内容/token数/向量ID/检索次数），可编辑内容、删除（引用计数校验）。搜索框按内容过滤。

- [ ] **Step 4: 搜索 Tab（调试）**

左：检索参数（topK/阈值/融合策略/rerank开关/相邻分块/补全查询开关）；右：搜索结果（相似度/文档标签/耗时分解）。参照 snail-ai-admin 的 `SearchTab`。

- [ ] **Step 5: Q&A Tab（内嵌）**

左：检索参数 + LLM 回答参数（Chat 模型/临近分块数/提示词模板）；右：流式 Q&A 对话 + 引用来源展示。

- [ ] **Step 6: 存储实例页**

实例列表（名称/类型/状态）+ 新建（MILVUS 连接配置表单）+ 连接测试 + 维度校验。

- [ ] **Step 7: 本地验证**

Run: `cd forge-admin-ui && pnpm dev`
Expected: 知识库 CRUD、文档上传带进度、分块编辑、搜索调试、Q&A 对话、存储实例管理均可用

- [ ] **Step 8: 提交**

```bash
git add forge-admin-ui/src/views/ai/knowledge/ forge-admin-ui/src/views/ai/store-instance/ forge-admin-ui/src/api/ai.js
git commit -m "feat(ui): 知识库管理页面(文档/分块/搜索/QA)"
```

---

## Self-Review 记录

- **Spec 覆盖**：设计文档二期内容全覆盖（存储实例 ✓、RAG 核心 ✓、前端 ✓、Agent 绑定 Forced 依赖三期列）
- **范围调整**：Agent 绑定（Task 9）因 `ai_agent` 列收拢三期 V1.0.88，**二期不实现**，保留为三期设计参考；二期 QA 走简化链路
- **类型一致性**：`VectorStore.add/search/delete`（直连 milvus-sdk，`add` 接 float 向量、`search` 接 query 向量）、`ChunkStrategy.chunk`、`DocumentParser.parse`、`RagSearchService.search` 全篇签名一致
- **依赖顺序**：Task 3 依赖一期 `AiModelAdapterRegistry`（`getEmbedding` 获取 Embedding 适配器）+ provider 解析闭环（本 Task 内）；Task 9 依赖三期列
- **范围裁剪**：BM25 / 混合融合 / Rerank / 对话历史补全查询**顺延到三期**（三期新增"RAG 检索增强"Task）。二期只做纯向量检索 + 阈值过滤 + 相邻分块 + Lost-in-Middle
- **逻辑删除**：四张新表统一 `del_flag bigint NOT NULL DEFAULT 0` + 唯一键直接建在 `del_flag` 上（无 `logic_delete_active` 生成列），与现有 `ai_model`/`ai_agent` 一致
- **实体基类**：`create_by` 等由 `BaseEntity` 自动填充，实体不重复声明
- **菜单脚本**：按 V1.0.18 完整模板（`client_code`/`menu_status`/`visible`/`sys_role_resource` 授权 admin），不以简化版交付
- **依赖**：`forge-plugin-ai` 新增 `forge-starter-file`、milvus-sdk-java、PDFBox/POI/Tika；分块用 Spring AI 2.0.0 自带 `TokenTextSplitter`（不引 LangChain4j）
- **异步**：`AiAsyncConfig`（`@EnableAsync` + `aiDocProcessExecutor`）；SSE 用 WebFlux `Flux<ServerSentEvent>`（与现有 `AiClientController` 一致，非 MVC SseEmitter）
- **向量去重**：同内容 chunk 各自存向量，content_hash 仅防重复导入
