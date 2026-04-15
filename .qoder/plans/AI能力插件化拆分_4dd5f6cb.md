# AI能力插件化拆分

## 目标
- 新建 `forge/forge-framework/forge-plugin-parent/forge-plugin-ai/`
- 将 `forge-report` 中的 `ai/` 目录全部代码迁移至插件，包名从 `com.mdframe.forge.report.ai.*` 改为 `com.mdframe.forge.plugin.ai.*`
- `forge-report` 的 pom.xml 移除 Spring AI 依赖，改为依赖 `forge-plugin-ai`
- Spring AI 相关 Maven 依赖和版本属性保留在 `forge/pom.xml` 层级（已存在，无需改动）

---

## Task 1 - 创建 forge-plugin-ai 模块

### 1.1 新建 `forge-plugin-ai/pom.xml`
路径：`forge/forge-framework/forge-plugin-parent/forge-plugin-ai/pom.xml`

```xml
<parent>forge-plugin-parent</parent>
<artifactId>forge-plugin-ai</artifactId>

<dependencies>
    <!-- forge 基础 starter -->
    <dependency>forge-starter-core</dependency>
    <dependency>forge-starter-orm</dependency>
    <dependency>forge-starter-auth</dependency>
    <dependency>forge-starter-tenant</dependency>
    <dependency>forge-plugin-system</dependency>  <!-- 可能依赖用户体系 -->
    <dependency>spring-boot-starter-webflux</dependency>  <!-- Flux<String> 流式响应 -->
    <!-- Spring AI OpenAI（核心库，非 starter） -->
    <dependency>spring-ai-openai version=${spring-ai.version}</dependency>
    <!-- Spring AI core（ChatMemory 接口） -->
    <dependency>spring-ai-core version=${spring-ai.version}</dependency>
    <dependency>lombok provided</dependency>
</dependencies>
```

### 1.2 注册到 forge-plugin-parent/pom.xml
在 `<modules>` 中添加 `<module>forge-plugin-ai</module>`

---

## Task 2 - 迁移 AI 代码到新插件

将 `forge-report` 中 `ai/` 下的全部 19 个 Java 文件复制到插件，修改包名：

| 原包名 | 新包名 |
|---|---|
| `com.mdframe.forge.report.ai.*` | `com.mdframe.forge.plugin.ai.*` |

新模块目录结构：
```
forge-plugin-ai/src/main/java/com/mdframe/forge/plugin/ai/
├── provider/
│   ├── domain/AiProvider.java
│   ├── mapper/AiProviderMapper.java
│   ├── service/AiProviderService.java
│   └── controller/AiProviderController.java
├── agent/
│   ├── domain/AiAgent.java
│   ├── mapper/AiAgentMapper.java
│   ├── service/AiAgentService.java
│   └── controller/AiAgentController.java
├── chat/
│   ├── domain/AiChatRecord.java
│   ├── mapper/AiChatRecordMapper.java
│   ├── service/AiChatService.java
│   ├── service/AiChatRecordService.java
│   ├── memory/DbChatMemory.java
│   ├── controller/AiChatController.java
│   └── dto/{ChatRequest,AIGenerateRequest}.java
└── session/
    ├── domain/AiChatSession.java
    ├── mapper/AiChatSessionMapper.java
    └── service/AiChatSessionService.java
```

需同步修改每个文件内部的 `import` 引用（将 `com.mdframe.forge.report.ai` 替换为 `com.mdframe.forge.plugin.ai`）。

**MyBatis Mapper XML**：如果有 XML 映射文件，同步移动到 `src/main/resources/mapper/ai/`，并更新 namespace。

---

## Task 3 - 改造 forge-report 模块

### 3.1 修改 `forge-report/pom.xml`
- 删除 Spring AI OpenAI 依赖（`spring-ai-openai`）
- 删除 Spring AI Alibaba Agent Framework 依赖（`spring-ai-alibaba-agent-framework`，若仍存在）
- 添加 `forge-plugin-ai` 依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-plugin-ai</artifactId>
</dependency>
```

### 3.2 删除 `forge-report` 中的 `ai/` 目录
删除 `forge-report/src/main/java/com/mdframe/forge/report/ai/` 整个目录

---

## Task 4 - 验证编译

```bash
cd forge && mvn clean compile -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am -DskipTests
cd forge && mvn clean compile -pl forge-report -am -DskipTests
```
