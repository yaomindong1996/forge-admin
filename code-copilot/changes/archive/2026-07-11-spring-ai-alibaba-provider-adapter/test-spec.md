# 单测 Spec — Spring AI Alibaba 供应商适配层与 DashScope 原生接入

> status: done
> created: 2026-07-10

## 0. 测试原则

- **Red/Green TDD**：Adapter 路由、参数映射、密钥保护和缓存失效必须先有失败测试，再实现最小代码使其通过；
- **First Run the Tests**：开始编码前先跑 `AiInvocationResolverTest`，记录当前测试工具链和基线；
- **展示工作**：所有命令、关键输出、失败原因和跳过项追加到 `execution-log.md`，禁止只写“测试通过”；
- **增量复用**：后续 `/test` 先读取本文件、`execution-log.md`、`spec.md`、`tasks.md`，按 `automated-testing-standard.md` 追加本轮差异；
- **无真实密钥**：单元测试使用格式明显的假值，真实 DashScope 仅作为人工可选集成验证；
- **不污染环境**：只停止本轮启动的服务，不清理用户已有 Maven、数据库、前端或其他进程。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit Jupiter 5（由 Spring Boot `3.5.13` 的 `spring-boot-starter-test` 管理） |
| Mock 框架 | Mockito + `MockitoExtension` |
| 断言 | JUnit Assertions；需要类型/集合表达时可使用 AssertJ |
| 已有测试数量 | `forge-plugin-ai` 当前 1 个测试类、2 个测试方法：`AiInvocationResolverTest` |
| 已有测试风格 | 纯单元测试，Mock Service，直接构造被测对象，不启动 Spring Context |
| 默认测试开关 | 根 POM 默认跳过测试，执行时必须启用 `-Penable-tests` |

## 2. 覆盖范围

### P0 — 核心业务逻辑（必须覆盖）

#### 类名: `AiProviderAdapterCode`

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `require` | Compatible 合法值 | `openai_compatible` | 返回对应枚举 |
| `require` | DashScope 合法值 | `dashscope_native` | 返回对应枚举 |
| `require` | 非法值 | `null`、`""`、纯空白、`unknown` | 抛 `BusinessException`，不回退默认 Adapter |

#### 类名: `AiProviderAdapterRegistry`

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|----------|
| 构造器 | 正常注册 | 两个不同 code Adapter | 返回各自 code | Registry 创建成功 |
| 构造器 | 重复注册 | 两个相同 code Adapter | 返回相同 code | 启动期失败，错误指出重复 code |
| `getRequired` | 命中 | `dashscope_native` | Registry 含对应 Adapter | 返回 DashScope Adapter |
| `getRequired` | 未命中 | `unknown` | Registry 无对应 Adapter | `BusinessException`，不调用其他 Adapter |
| `createChatModel` | 合法配置 | provider + options | Adapter validate 通过 | 按顺序执行 getRequired、validate、create，返回模型 |
| `createChatModel` | 校验失败 | 错误 URL/API Key/model | Adapter validate 抛错 | 不调用 Adapter#createChatModel，不产生网络请求 |

#### 类名: `AiProviderBaseUrlPolicy`

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `normalizeAndValidate` | URI 安全校验 | 非 HTTP scheme、query、fragment、userInfo | 明确业务异常 |
| `normalizeAndValidate` | Native 官方根地址 | `https://dashscope.aliyuncs.com/` | 归一化为无尾斜杠根地址 |
| `normalizeAndValidate` | Native 误用 Compatible | 官方域名 `/compatible-mode` | 拒绝 |
| `normalizeAndValidate` | Compatible 官方地址 | 官方域名 `/compatible-mode/` | 接受并归一化尾斜杠 |
| `normalizeAndValidate` | Compatible 误用 Native | 官方域名根地址 | 拒绝 |
| `normalizeAndValidate` | Compatible 错误版本路径 | 官方域名 `/compatible-mode/v1` | 拒绝，避免 SDK 再追加 `/v1` |
| `normalizeAndValidate` | 自定义代理 | 合法 HTTP/HTTPS 自定义域名 | 仅做通用校验，不追加或替换路径 |

#### 类名: `OpenAiCompatibleProviderAdapter`

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `validate` | 完整配置 | API Key、Base URL、model | 通过 |
| `validate` | Base URL 缺失 | 空 Base URL | 明确业务异常 |
| `validate` | 阿里官方原生根地址误配 | `https://dashscope.aliyuncs.com` | 明确业务异常，不发请求 |
| `createChatModel` | 参数映射 | model、temperature、maxTokens | 返回 `OpenAiChatModel`，默认 Options 值一致 |

#### 类名: `DashScopeNativeProviderAdapter`

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `validate` | 官方原生地址 | `https://dashscope.aliyuncs.com` | 通过 |
| `validate` | Base URL 为空 | 空值 | 使用官方默认地址 |
| `validate` | Compatible 地址误配 | 包含 `/compatible-mode` | 明确业务异常，不发请求 |
| `createChatModel` | 参数映射 | `qwen-plus`、0.7、512 | 返回 `DashScopeChatModel`；Options 为 `DashScopeChatOptions`；`maxTokens=512` |
| `createChatModel` | Tool Calling 基础类型 | 默认 Options | Options 实现 `ToolCallingChatOptions` |

#### 类名: `AiProviderSecretMasker`

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `mask` | 长密钥 | `abcd12345678wxyz` | `abcd****wxyz` |
| `mask` | 短密钥 | 长度小于等于 8 | 固定掩码，不泄露字符 |
| `mask` | 空值 | `null/blank` | 空安全结果 |
| `isUnchangedMask` | 未修改 | 提交值等于持久值的 mask | `true` |
| `isUnchangedMask` | 新密钥 | 提交新的明文 | `false` |

#### 类名: `AiProviderService`

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|----------|
| `updateProvider` | 脱敏值回写 | ID + masked key | Mapper 返回原实体 | 保留原密钥；成功后 evict |
| `updateProvider` | 替换密钥 | ID + 新明文 | Mapper 返回原实体 | 保存新密钥；成功后 evict |
| `createProvider` | Adapter 未提供/null | adapterCode=null | 无 | 写入 `openai_compatible` |
| `updateProvider` | 旧页面未传 Adapter | ID + adapterCode=null | Mapper 返回原生 Adapter 实体 | 保留原 `dashscope_native`，不得回退 Compatible |
| `createProvider/updateProvider` | 显式 blank Adapter | adapterCode=`""` 或纯空白 | 无 | 业务校验失败，不写数据库 |
| `updateProvider` | DB 更新失败 | 合法请求 | update 返回 false/抛错 | 抛异常；不得把失败写成成功 |
| `testConnection` | 已保存供应商 | 仅 ID | 按租户加载含真实 key 的实体；Adapter 返回 Mock ChatModel | Prompt 通过指定 Adapter 调用；不依赖浏览器 key |
| `testConnection` | 未保存配置 | 无 ID + 完整配置 | Registry 返回 Mock ChatModel | 可测试；密钥不进入日志/结果 |
| `testConnection` | one-of 混合请求 | ID + 任一 adapter/baseUrl/apiKey/model 字段 | 无 | 请求校验失败，不加载或调用模型 |
| `testConnection` | 敏感 SDK 异常 | 无 ID 完整配置 | ChatModel 抛出包含假密钥的异常 | 返回固定安全错误，不包含假密钥、Header 或请求体 |
| `toSafeView` | 列表/详情 | 含真实 key 实体 | 无 | VO 只含脱敏 key |
| `deleteProvider` | 删除成功 | providerId | 无关联模型、remove 成功 | 持久化成功后调度 after-commit evict |

#### 类名: `ChatClientCache`

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|----------|
| `getOrCreateBase` | 相同配置 | 同 provider 实体/adapter/options | Registry 每次可创建模型 | 实际只创建一次 |
| `getOrCreateBase` | 租户不同 | 两个实体 tenantId 不同、providerId 相同 | Registry 创建模型 | 两个缓存项，不串租户 |
| `getOrCreateBase` | Adapter 不同 | 同 providerId、不同 adapter | Registry 创建模型 | 两个缓存项 |
| `evictByProvider` | 配置更新 | tenantId + providerId | 已有缓存 | 清除所有模型/参数组合；下次重新创建 |

#### 类名: `AiProviderCacheEvictionScheduler`

| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `scheduleAfterCommit` | 活动事务未提交 | provider 实体 | 提交前不调用 Cache |
| `scheduleAfterCommit` | 事务提交 | provider 实体 | afterCommit 只清理实体 tenantId/providerId |
| `scheduleAfterCommit` | 事务回滚 | provider 实体 | 不清理 Cache |
| `scheduleAfterCommit` | 无活动事务 | provider 实体 | 立即清理 |
| `scheduleAfterCommit` | 实际事务已激活但同步未启用 | provider 实体 | 抛 `BusinessException`，不提前清理 Cache |

#### 类名: `AiClientImpl`

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|----------|
| `call` | Native 供应商同步调用 | Resolver 返回 `dashscope_native` provider | 真实 Cache 经 Mock Registry 获得 Fake ChatModel | 统一 ChatClient 链路返回内容，Registry 收到 Native provider 和通用 Options |
| `stream` | Native 供应商流式调用 | Resolver 返回 `dashscope_native` provider | Fake ChatModel 依次返回 reasoning/content ChatResponse | 输出包含思考和回答，完整内容按现有规则持久化 |
| `stream` | reasoningContent | AssistantMessage metadata 含该键 | 无公网调用 | 现有解析逻辑读取推理内容，不依赖 OpenAI 类型 |

#### 类名: `AiInvocationResolver`

保留现有两个用例，并增加：DashScope Native 的 Base URL 可由 Adapter 默认，不在 Resolver 统一拒绝；API Key 缺失仍在 Adapter 校验阶段失败。

#### 2026-07-11 默认模型与错误诊断增量

| 类名 | 场景 | 预期结果 |
|------|------|----------|
| `AiModelService` | 存在启用且默认模型 | 返回 Mapper XML 查询到的 `model_id` |
| `AiModelService` | 无启用默认模型 | 抛 `BusinessException("请为供应商设置默认模型")` |
| `AiProviderService` | 已保存供应商的双写默认字段过期 | 忽略 `ai_provider.default_model`，使用 `ai_model` 权威结果 |
| `AiProviderService` | 已保存供应商无权威默认模型 | 在 Registry/ChatModel 调用前失败，不产生网络请求 |
| `AiProviderService` | 未保存供应商测试 | 使用请求内 `defaultModel`，不查询 `ai_model` |
| `AiInvocationResolver` | 无显式模型且 Agent 无模型 | 使用 `ai_model` 权威默认模型，不读取供应商双写字段 |
| `AiInvocationResolver` | 无权威默认模型 | 明确提示配置默认模型，不回退 `gpt-3.5-turbo` |
| `AiProviderFailureDiagnostics` | `400 - {error.code/type}` | 仅提取 `httpStatus=400` 与白名单化错误码 |
| `AiProviderFailureDiagnostics` | 异常含密钥/任意正文 | 诊断对象和日志字段不包含原始 message、密钥或响应正文 |

### P1 — 数据与迁移

| 对象 | 场景 | 验证 |
|------|------|------|
| `V1.0.17` | 首次执行 | `ai_provider.adapter_code` 存在、非空、默认 Compatible |
| `V1.0.17` | 历史回填 | 现有记录全部为 `openai_compatible` |
| `V1.0.17` | 部分部署/重跑 | 预置一条 `dashscope_native` 后重跑保护逻辑 | Native 保持不变，仅 NULL/blank 补 Compatible |
| `V1.0.17` | 字典写入 | tenant_id=`1`，类型及两条数据存在且不重复 |
| `V1.0.17` | 防重复静态检查 | 字段检查使用 `information_schema`，字典使用 `NOT EXISTS` |
| Flyway placeholder | 所有迁移 | `rg -n '\$\{[^}]+\}' forge-server/db/migration` 无输出 |

### P2 — 入口层和前端

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 供应商分页 | `GET /ai/provider/page` | 包含 adapterCode，API Key 只有脱敏值 |
| 供应商详情 | `GET /ai/provider/{id}` | 不返回真实 API Key |
| 旧供应商更新 | 不修改密钥直接保存 | 数据库 API Key 保持原值，缓存失效 |
| 新建 Compatible | adapterCode=`openai_compatible` | 保存成功，既有模型调用正常 |
| 新建 Native | adapterCode=`dashscope_native` + 原生 URL | 保存成功，可选择 qwen 模型 |
| 错误 Native URL | adapterCode=`dashscope_native` + compatible-mode | 保存或测试返回明确错误 |
| 错误 Compatible URL | adapterCode=`openai_compatible` + DashScope 官方根地址 | 保存或测试返回明确错误 |
| 已保存连接测试 | 点击“测试连接” | 加密 Network Payload 只有 ID，不含 API Key |
| 混合连接测试 | ID 与配置字段同时提交 | 后端拒绝，不加载数据库密钥后再接受请求覆盖 |
| 敏感写请求 | 新增、更新、测试 | POST 使用 `postEncrypt`，PUT 带 `encrypt:true`，后端保持 `@ApiDecrypt` |
| 前端构建 | Node 20.19.0 + pnpm build | 构建成功，无字典或模板错误 |

#### 2026-07-11 管理端增量

| 场景 | 操作 | 预期结果 |
|------|------|----------|
| 供应商分页 | 切换每页条数/快速跳页 | 请求携带响应式 `pageNum/pageSize`，总数来自 `itemCount` |
| 模型分页 | 供应商模型超过一页 | 服务端分页，无 `pageSize=100` 静默截断，数量显示总条数 |
| 搜索重置 | 筛选后点击重置 | 清空筛选、页码、当前供应商与模型工作区 |
| 行选择 | 重复点击已选中供应商 | 保持选中；仅显式关闭按钮收起详情 |
| 键盘选择 | 聚焦供应商行后 Enter/Space | 选择供应商并加载模型 |
| 上传过滤 | 打开文件选择器 | `accept=.png,.jpg,.jpeg,.svg,.webp` 生效 |
| API Key 编辑 | 编辑已保存供应商 | 展示脱敏值且明确“留空表示不修改”，提交不还原密钥 |
| 模型默认 | 点击“设为默认” | 更新模型默认状态并刷新供应商权威默认模型 |
| 连接测试结果 | 成功或失败 | 图标、标题、说明共同表达状态，非纯颜色反馈 |
| 响应式与主题 | 375/768/1024/1440、明暗色 | 无页面级横向溢出，边界和文字对比清晰 |

### P3 — 可选真实集成

仅当人工提供 `AI_DASHSCOPE_API_KEY`、网络可用且明确接受少量模型调用成本时执行：

1. `qwen-plus` 同步请求返回可见文本；
2. 流式请求持续输出且正常结束；
3. 推理模型返回 `reasoningContent` 时现有解析逻辑可读取；
4. 连接测试 `maxTokens` 受限；
5. 日志、异常和响应均不出现 API Key。

### 不测试（明确列出原因）

- Agent Framework、Graph、MCP、Nacos、Studio：不在本变更依赖和功能范围；
- Embedding/Image/Audio/Rerank：本阶段仅实现 Chat Adapter；
- API Key 数据库加密/KMS：属于独立安全变更；
- 公网真实模型作为 CI 必跑项：依赖外部网络、凭据和费用，不具备可重复性；
- 未绑定菜单的 `forge-admin-ui/src/views/ai/provider.vue`：后端“新增缺失则 Compatible、更新缺失则保留原 Adapter”保证旧请求兼容，本次只验证实际菜单页面。

## 3. 执行计划

- [x] Step 1: 运行 `AiInvocationResolverTest` 和目标模块 dependency tree，确认基线；
- [x] Step 2: 按 Task 2-5 逐类生成 P0 测试，并强制包含 `AiClientImplTest` 同步/流式用例，保存 Red 输出；
- [x] Step 3: 实现最小代码并保存 Green 输出；
- [x] Step 4: 运行 AI 插件完整测试、编译和主应用装配；
- [x] Step 5: 执行 Flyway 静态检查，有 dev 库时实跑 V1.0.17；
- [x] Step 6: 使用 Node `v20.19.0` 执行前端 build；
- [x] Step 7: 按环境条件决定是否执行真实 DashScope 集成验证并记录跳过原因；
- [x] Step 8: 回填执行证据、警告、服务清理和 Spec/Task 状态。

### 3.1 建议命令

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-ai dependency:tree \
  '-Dincludes=org.springframework.ai:*,com.alibaba.cloud.ai:*' -Dverbose
```

```bash
cd forge-server
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test \
  -Dtest=AiInvocationResolverTest,AiClientImplTest,AiProviderAdapterCodeTest,AiProviderAdapterRegistryTest,AiProviderBaseUrlPolicyTest,OpenAiCompatibleProviderAdapterTest,DashScopeNativeProviderAdapterTest,AiProviderSecretMaskerTest,AiProviderCacheEvictionSchedulerTest,AiProviderServiceTest,ChatClientCacheTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

```bash
cd forge-server
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am compile -DskipTests
```

```bash
cd forge-server
mvn -pl forge-admin-server -am package -DskipTests
```

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

```bash
source ~/.nvm/nvm.sh
nvm use v20.19.0
cd forge-admin-ui
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-10 | Research | 未执行构建 | 待 `/apply` 前建立 | 当前仅创建提案；禁止把静态阅读记为测试通过 |
| 2026-07-10 | Apply 基线 | `AiInvocationResolverTest` | 2 tests，0 failure/error/skip | Java 17，`-Penable-tests` 确认测试真实执行 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-10 | Spec/Tasks/Test Spec 文档 | 占位符/尾随空格、`--no-index --check`、路径/状态一致性、独立读者复审 | 文档静态检查通过；第三轮 Reader Test PASS | 本轮仅文档，不编译业务代码 | 无服务启动 |
| 2026-07-10 | Task 1 依赖基线 | 变更前后 dependency tree、AI 插件 compile | Spring AI 全部为 `1.1.2`；DashScope Core 为 `1.1.2.3`；compile SUCCESS | 无 DashScope Starter；无服务启动 |
| 2026-07-10 | Task 2-5 P0 增量 | Adapter Code/Registry/URL Policy/双 Adapter/Cache/Resolver/Secret/Service/Scheduler | 各 Task Red/Green 证据见 `execution-log.md`；最终并入 AI 插件全量 44 tests | JVM CDS 与 commons-logging 警告，不影响测试结论 |
| 2026-07-10 | Task 7 Native 离线调用 | `AiClientImplTest` 同步、流式、reasoningContent 和持久化 | 2 tests，0 failure/error/skip；真实 Cache 经 Mock Registry/Fake ChatModel 进入统一链路 | 首轮异步落库断言存在竞态，改用 timeout 等待后复跑通过 |
| 2026-07-10 | Task 7 全量验收 | AI 全量测试、AI package、Admin package、Flyway 静态检查、Node 20 前端 build | 44 tests 全过；24/35 模块 reactor package 成功；前端 8485 modules、build SUCCESS | 无环境 API Key，公网验证跳过；未启动服务；既有编译/Vite 警告已记录 |
| 2026-07-10 | Review 修复复验 | 事务同步异常 fail-closed、AI 插件完整测试 | 新增 Scheduler 边缘用例；45 tests，Failures/Errors/Skipped 均为 0；24 模块 reactor BUILD SUCCESS | JVM CDS 与 commons-logging 既有警告；未启动服务 |
| 2026-07-11 | `/review` 独立复审 | `origin/main..HEAD` 真实代码、AI 完整测试、依赖树、前端构建、XML/Flyway/diff 静态检查 | Spec Compliance PASS；Code Quality PASS；45 tests、24 模块 reactor、8485 modules 前端构建全部成功 | 未提供公网 API Key/隔离 dev 库；保留既有 JVM、commons-logging 与 Vite 警告；未启动服务 |
| 2026-07-11 | 默认模型与供应商页面增量 | 4 类后端定向测试、AI 插件完整测试、Mapper XML、目标 Vue ESLint、Node 20 build、Playwright 响应式与交互 | 定向 21 tests、AI 全量 51 tests 全过；前端 8485 modules build；1440/1024/375 无页面横向溢出，重复点击保持选中，控制台无错误 | 未再次调用真实供应商模型；复用用户现有 8580 服务；Playwright 启动的 5173 Vite 已停止 |

## 6. 执行证据

- `execution-log.md`：`code-copilot/changes/archive/2026-07-11-spring-ai-alibaba-provider-adapter/execution-log.md`
- 关键接口：`/ai/provider/page`、`/ai/provider/{id}`、`/ai/provider`、`/ai/provider/test`、`AiClient.call/stream`
- 关键数据库检查：`ai_provider.adapter_code`、`sys_dict_type.ai_provider_adapter_type`、对应 `sys_dict_data`、`forge_schema_history` V1.0.17
- 服务启动与停止：本变更未启动 `forge-admin-server`，无 PID 清理项
- 公网集成凭据：只从环境变量读取，不写入命令日志、Spec、测试源码或 SQL

## 7. 归档验收

- **状态**：done
- **归档时间**：2026-07-11
- **复用基线**：后端定向 21 tests、AI 插件完整 51 tests、Admin 35 模块 package、Node 20 前端生产构建和 1440/1024/375 三视口页面验证均已有成功证据。
- **本轮未重跑**：用户明确要求自行验收最后一轮页面间距、模型列表拆列和默认模型行内开关微调，因此未再次执行前端构建或浏览器测试，也未把中断构建写成通过。
- **环境跳过**：未提供 `AI_DASHSCOPE_API_KEY` 和隔离 dev 数据库，公网 DashScope 与 Flyway 实库验证继续跳过。
