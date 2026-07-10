# 变更执行日志 — Spring AI Alibaba 供应商适配层与 DashScope 原生接入

> 本文件持续记录提案、实现、验证和审查证据。当前处于 review 阶段，尚未归档。

## 时间线

| 时间 | 阶段 | 事件 | 备注 |
|------|------|------|------|
| 2026-07-10 | Research | 读取根 `AGENTS.md`、`code-copilot/AGENTS.md`、全部规则、相关知识和变更模板 | 以根规则和实际 POM 为最高事实来源 |
| 2026-07-10 | Research | 核对 Forge AI 插件调用链 | 当前固定使用 `OpenAiApi/OpenAiChatModel/OpenAiChatOptions` |
| 2026-07-10 | Research | 核对 Spring AI Alibaba 主仓库 `1.1.2.3` | Spring AI `1.1.2`、Boot `3.5.8`；Agent Framework 建立在 Spring AI 上 |
| 2026-07-10 | Research | 核对 Spring AI Extensions `1.1.2.3` | Spring AI `1.1.2`、Boot `3.5.10`；确认 DashScope Core/Starter 分层及动态 Builder API |
| 2026-07-10 | Decision | 选择核心 `spring-ai-alibaba-dashscope`，不使用 Starter | 避免默认自动配置读取全局 API Key，与租户数据库动态配置冲突 |
| 2026-07-10 | Decision | 新增显式 `adapter_code` | 不再依据 `providerType` 或 URL 猜协议；历史数据保持 Compatible |
| 2026-07-10 | Proposal | 创建 Spec、Tasks、Test Spec 和本日志 | 等待 HARD-GATE 最终确认 |
| 2026-07-10 | Proposal Check | 检查模板章节、8 个 Task、引用路径、占位符、尾随空格和未跟踪文件空白错误 | 修正路径缩写、更新缺失 Adapter 保留语义、tenantId 来源和 EOF 多余空行 |
| 2026-07-10 | Reader Test 1 | 独立读者检查 Spec/Task/Test 一致性 | 发现连接测试 one-of、URL 双向校验、强制离线调用测试、after-commit 缓存和迁移/回退 5 项缺口，暂不 PASS |
| 2026-07-10 | Proposal Fix 1 | 按读者意见修订四份文档 | 五项缺口均已补充业务规则、任务、测试场景和回退前置检查，等待复审 |
| 2026-07-10 | Reader Test 2 | 复审第一轮修订 | 原 5 项关闭；发现 Registry 校验顺序和 DTO null/blank 语义 2 项高风险缺口 |
| 2026-07-10 | Proposal Fix 2 | 固定 Registry 唯一入口与 null/blank 分层语义 | 增加选择→校验→创建测试，以及新增默认/更新保留/blank 拒绝三类测试 |
| 2026-07-10 | Reader Test 3 | 最终只读复审 | PASS，无剩余阻断或高风险项 |
| 2026-07-10 | HARD-GATE | 用户执行 `/apply spring-ai-alibaba-provider-adapter` | Spec/Test Spec 状态切换为 `apply`，进入实现阶段 |
| 2026-07-10 | Git 环境检查 | 尝试创建 `feature/spring-ai-alibaba-provider-adapter` | 沙箱禁止写入 `.git/refs`，保留当前 `main` 工作树继续实现；Task 级 commit 将记录为环境跳过项 |
| 2026-07-10 | Git 环境恢复 | 权限放开后创建 `feature/spring-ai-alibaba-provider-adapter` | 已切换到独立 feature 分支；前述 Git 跳过限制解除 |
| 2026-07-10 | Apply 基线 | 运行 AI 插件 dependency tree 与 `AiInvocationResolverTest` | 变更前仅 Spring AI `1.1.2`；测试 2/2 通过，确认 `-Penable-tests` 生效 |
| 2026-07-10 | Task 1 | 导入 Spring AI/Extensions/Alibaba 三个 BOM并增加 DashScope Core | dependency tree 收敛：Spring AI `1.1.2`、DashScope `1.1.2.3`；AI 插件 reactor compile SUCCESS，未引入 Starter |
| 2026-07-10 | Task 2 Red | 仅新增 `AiProviderAdapterCodeTest` 后执行定向测试 | testCompile 按预期失败：`AiProviderAdapterCode` 尚不存在，Red 证据成立 |
| 2026-07-10 | Task 2 Green | 实现 Adapter Code、实体字段和 `V1.0.17` 迁移 | 定向测试 2/2 通过；全迁移 placeholder 扫描无输出；字段/字典防重复静态检查通过 |
| 2026-07-10 | Task 3 Red | 新增 Registry、URL Policy、Compatible/Native Adapter 测试后执行定向测试 | testCompile 按预期失败：SPI/Registry/两个 Adapter 尚不存在，Red 证据成立 |
| 2026-07-10 | Task 3 Green | 实现通用运行参数、Adapter SPI/Registry、URL Policy 和两类 ChatModel | 4 个测试类共 12 tests 全部通过；Registry 顺序、失败关闭、DashScope `maxToken` 和 ToolCalling Options 已覆盖 |
| 2026-07-10 | Task 4 Red | 新增 Cache、事务后失效及 Native Resolver 回归测试 | testCompile 按预期失败：旧 Cache 签名仍依赖 `OpenAiChatOptions`，Scheduler 尚不存在，Red 证据成立 |
| 2026-07-10 | Task 4 Green | Cache 改用租户安全结构化键并统一委托 Registry；同步/流式调用使用通用运行参数 | 首次补充分支测试发现 Mockito 严格模式的无效桩，调整测试桩作用域后定向 12 tests 全部通过 |
| 2026-07-10 | Task 4 Commit | 提交 `ebaaac44` | 代码提交成功；提交后统计上传 Hook 因网络超时失败，不影响本地提交 |
| 2026-07-10 | Task 5 Red | 新增 SecretMasker 和供应商生命周期服务测试 | testCompile 按预期失败：DTO、安全 VO 和密钥工具尚不存在，Red 证据成立 |
| 2026-07-10 | Task 5 Green | 引入 Save/Test DTO、安全 VO、密钥脱敏、严格 one-of 测试和 Registry 连接测试 | 补齐 blank Adapter、DB 失败、未保存配置测试后共 16 tests 全部通过；分页/默认查询迁入 Mapper XML |
| 2026-07-10 | Task 5 Commit | 提交 `d46c7e3c` | 代码提交成功；提交后统计上传 Hook 再次因网络超时失败，不影响本地提交 |
| 2026-07-10 | Task 6 | 改造活动供应商页面与 API 请求 | 增加字典驱动连接协议、受控 Native URL 联动、仅 ID 测试请求；新增/测试 POST 与更新 PUT 均启用请求加密 |
| 2026-07-10 | Task 6 Commit | 提交 `f82a24ae` | 代码提交成功；提交后统计上传 Hook 第三次因网络超时失败，不影响本地提交 |
| 2026-07-10 | Task 7 Native 离线回归 | 新增 `AiClientImplTest`，真实 Cache 经 Mock Registry/Fake ChatModel 运行 | 首轮同步通过、流式输出通过，但异步 `doFinally` 落库断言发生竞态；改为 Mockito timeout 后 2 tests 全部通过 |
| 2026-07-10 | Task 7 全量验证 | AI 插件测试、AI/Admin package、Flyway 静态检查、Node 20 前端构建 | AI 44 tests 全过；AI 24 模块与 Admin 35 模块 package 成功；前端生产构建成功 |
| 2026-07-10 | Spec Compliance Review | 对照 Spec、Tasks、Test Spec 与实现逐项检查 | PASS；功能、接口、数据、安全和范围边界均符合 Spec |
| 2026-07-10 | Code Quality Review | 检查 Adapter、生命周期、缓存、事务与密钥边界 | 发现活动事务未启用同步时可能提前清缓存；改为 fail-closed 并增加回归测试，同时修正 Controller 既有大括号格式 |
| 2026-07-10 | Review 修复复验 | 运行 AI 插件完整测试 | 45 tests 全过，24 模块 reactor BUILD SUCCESS；未启动服务 |
| 2026-07-10 | Task 8 | 回填战略方案、阶段路线图、Spec/Task/Test 和长期记忆 | 明确 Alibaba 增强层、依赖基线、核心模块、显式协议路由、阶段边界与旧应用回退前置条件；状态进入 review |
| 2026-07-11 | `/review` 独立复审 | 不采信既有 PASS 报告，从 `origin/main..HEAD` 重新检查 Spec 与真实代码 | Spec Compliance PASS；Code Quality PASS；无 Critical/Important，记录 2 项非阻断加固建议 |

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|------------|------|
| 框架主干 | 保留 Spring AI，叠加 Alibaba | 删除 Spring AI 全量重写 | Alibaba 复用 Spring AI `ChatModel/ChatClient` 抽象 |
| DashScope 接入 | 核心模型模块 + 动态 Adapter | Starter 全局自动配置 | Forge 是多租户数据库动态凭据模式 |
| 路由键 | `adapter_code` | `providerType`、URL 推断 | 历史存在 `alibaba/dashscope` 和 Compatible/Native 差异 |
| 历史行为 | 全部回填 `openai_compatible` | 自动升级为 Native | 避免上线即改变协议、请求地址和计费路径 |
| 失败策略 | Fail-closed | 跨 Adapter 自动重试 | 避免重复请求、重复计费和隐藏配置错误 |
| API Key | 脱敏回显 + 同值保留 | 明文返回、固定值覆盖 | 满足安全规则并保护更新链路 |
| 缓存事务边界 | after-commit；事务同步异常 fail-closed | 活动事务中立即失效 | 避免提交前重新缓存旧配置或回滚后误清理 |
| 旧应用回退 | 先清理 Native 配置再部署旧版本 | 直接回退应用 | 旧代码无法识别 `dashscope_native`，会误用原生 URL |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|
| 官方根 README 示例版本落后于 release | README 仍出现 `1.1.2.0/1.1.2.1` | 以 release POM/BOM `1.1.2.3` 为依赖事实 | 已沉淀到 pitfalls 104 |
| DashScope Starter 可能无配置即装配 | 两层条件均 `matchIfMissing=true` | Forge 只依赖 core model，不引入 Starter | 已沉淀到 decisions 16 / pitfalls 104 |
| 阿里供应商类型存在 `alibaba/dashscope` 两种历史值 | 多份初始化 SQL 不一致 | 路由独立为 `adapter_code`，不依赖品牌类型 | 已沉淀到 decisions 16 |
| Compatible 与 Native 地址不可混用 | API Path 协议不同 | Adapter 显式校验，历史配置不自动切换 | 已沉淀到 decisions 16 / pitfalls 104 |
| 缓存失效方法无调用者 | 更新/删除链路未接入 Cache | 事务成功后按 tenant/provider 主动清理 | 已由实现和测试固化 |
| ID 测试与临时配置混合会形成密钥边界歧义 | DTO 未定义互斥模式时可能加载持久密钥后接受请求字段覆盖 | 定义严格 one-of；混合请求在模型构建前拒绝 | 已由实现和测试固化 |
| 旧应用无法识别 Native Adapter | 旧代码会把原生 Base URL 当 OpenAI Compatible 使用 | 回退前检查 Native 记录并先切回 Compatible URL | 已沉淀到 decisions 16 / pitfalls 104 |

## 知识发现

- [x] **Spring AI Alibaba 依赖关系**：Alibaba Agent Framework 与 Extensions 都建立在 Spring AI `1.1.2` 上，迁移应采用增强叠加而不是替换 Spring AI。
- [x] **动态供应商配置**：多租户数据库凭据场景优先直接构建模型核心类，避免默认全局 Starter Bean。
- [x] **协议与品牌解耦**：供应商品牌不能稳定表达 API 协议，尤其同一厂商同时提供原生和 OpenAI Compatible 接口。

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| 事务同步异常分支 | 更新/删除在事务成功后失效缓存，回滚不清理 | 审查发现实际事务已激活但同步未启用时原实现会立即清理 | 改为抛 `BusinessException` 失败关闭，并增加第 45 个测试 |
| 公网 DashScope 验证 | 有环境密钥时可选执行 | `AI_DASHSCOPE_API_KEY` 未设置 | 按 Test Spec 跳过；离线同步/流式/reasoningContent 必跑项已通过 |

## 验证记录

| 时间 | 变更范围 | 命令 | 结果 | 警告/跳过 | 服务清理 |
|------|----------|------|------|-----------|----------|
| 2026-07-10 | Proposal 文档 | `rg` 占位符/尾随空格扫描；`test -f` 关键路径；`git diff --no-index --check /dev/null <file>` | 本地静态检查通过 | 文件未跟踪，使用 `--no-index --check` 逐文件检查 | 无服务启动 |
| 2026-07-10 | Reader Test 1 | 新上下文 Agent 只读检查四份提案文档 | 未通过；5 项意见已全部修订，复审中 | 无业务代码或服务验证 | 无服务启动 |
| 2026-07-10 | Reader Test 2-3 | 新上下文 Agent 复审修订文档 | 第二轮补 2 项高风险语义；第三轮 PASS | 本轮仅文档检查 | 无服务启动 |
| 2026-07-10 | Proposal 最终静态复验 | `git status --short`、`git diff --name-only`、`git diff --check`、四文件逐一 `git diff --no-index --check`、尾随空格扫描、关键源码/vendor/文档路径检查、Flyway 版本序列检查 | 全部通过；当前最高迁移为 `V1.0.16`，计划使用 `V1.0.17` 无版本冲突 | 仅文档变更，未执行 Maven、前端构建或公网模型调用 | 无服务启动 |
| 2026-07-10 | Task 4 定向回归 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test -Dtest=ChatClientCacheTest,AiProviderCacheEvictionSchedulerTest,AiInvocationResolverTest -Dsurefire.failIfNoSpecifiedTests=false` | 12 tests，Failures/Errors/Skipped 均为 0；reactor BUILD SUCCESS | 首轮补充用例因 Mockito `UnnecessaryStubbingException` 失败，修正为 lenient 公共桩后复跑通过 | 无服务启动 |
| 2026-07-10 | Task 5 定向回归 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test -Dtest=AiProviderSecretMaskerTest,AiProviderServiceTest -Dsurefire.failIfNoSpecifiedTests=false`；`xmllint --noout .../AiProviderMapper.xml` | 16 tests，Failures/Errors/Skipped 均为 0；Mapper XML 语法通过 | JVM CDS 与 commons-logging classpath 警告待全量验证统一记录；测试构造器适配和 URL fixture 修正后复跑通过 | 无服务启动 |
| 2026-07-10 | Task 6 前端静态检查 | `pnpm exec eslint --fix src/api/ai.js src/views/ai/provider-model.vue`；`git diff --check` | ESLint 退出码 0，diff whitespace 检查通过；旧 `provider.vue` 无改动 | 当前系统 Node 为 `v26.0.0`；规定的 Node `v20.19.0` 生产构建留到 Task 7 | 无服务启动 |
| 2026-07-10 | Native 统一调用链 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test -Dtest=AiClientImplTest -Dsurefire.failIfNoSpecifiedTests=false` | 2 tests，Failures/Errors/Skipped 均为 0；验证 Native 同步、流式、`reasoningContent` 和最终会话内容 | 首轮因 Reactor `doFinally` 异步时序导致验证过早，改为 `timeout(2000)` 等待后复跑通过 | 无服务启动 |
| 2026-07-10 | AI 插件全量测试 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test` | 44 tests，Failures/Errors/Skipped 均为 0；24 模块 reactor BUILD SUCCESS | JVM CDS、spring-jcl/commons-logging 既有 classpath 警告 | 无服务启动 |
| 2026-07-10 | AI 插件打包 | `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am package -DskipTests` | 24 模块 reactor BUILD SUCCESS，生成 AI 插件 Jar | 按命令显式跳过测试；部分既有模块有 deprecated/unchecked 编译警告 | 无服务启动 |
| 2026-07-10 | Admin 主应用装配 | `mvn -pl forge-admin-server -am package -DskipTests` | 35 模块 reactor BUILD SUCCESS，Spring Boot repackage 成功 | 按命令显式跳过测试；既有模块有 deprecated/unchecked 编译警告 | 无服务启动 |
| 2026-07-10 | Flyway 静态验收 | `rg -n '\$\{[^}]+\}' forge-server/db/migration`；`rg` 检查 V1.0.17 字段/字典/tenant；`xmllint --noout AiProviderMapper.xml` | placeholder 扫描无输出；迁移含 `information_schema`、`NOT EXISTS` 语义、tenant_id=`1` 和仅 NULL/blank 回填；XML 合法 | 未提供隔离 dev 数据库，未对用户本地库执行迁移；`forge_schema_history` 实库验证跳过 | 无服务启动 |
| 2026-07-10 | 前端生产构建 | `source ~/.nvm/nvm.sh`、`nvm use 20.19.0`、`pnpm build` | Node `v20.19.0`；8485 modules transformed；Vite build SUCCESS，耗时 1m23s | 既有 UserSelectModal 重名、动态/静态 import、CSS `//` 注释和 bundle 体积警告 | 无服务启动 |
| 2026-07-10 | 可选公网 DashScope | 检查 `AI_DASHSCOPE_API_KEY` 是否存在 | 环境变量未设置，按 Test Spec 明确跳过真实同步/流式调用 | 不把无凭据/未调用写成通过；无费用产生 | 无服务启动 |
| 2026-07-10 | Review 修复复验 | `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am test` | 45 tests，Failures/Errors/Skipped 均为 0；24 模块 reactor BUILD SUCCESS | JVM CDS 与 spring-jcl/commons-logging 既有警告；无新增阻断 | 无服务启动 |
| 2026-07-10 | Task 8 静态收尾 | `git diff --check`；未跟踪路线图 `git diff --no-index --check`；`test -f` 路径检查；`rg` 状态/陈旧描述/未完成项检查 | 已跟踪差异无 whitespace 错误；路线图检查仅返回 no-index 差异码且无错误输出；关键路径存在；Spec/Test 均为 review，Task/Test 无未完成项 | 未执行外部写入或服务启动 | 无服务启动 |
| 2026-07-11 | `/review` 独立验证 | AI 插件 `-Penable-tests -am test`；AI `dependency:tree`；Node `20.19.0` `pnpm build`；`xmllint`；Flyway placeholder/Starter/diff 扫描 | 45 tests、24 模块 reactor BUILD SUCCESS；Spring AI `1.1.2`、DashScope Core `1.1.2.3`；8485 modules、Vite build SUCCESS；静态检查通过 | 保留 JVM CDS、commons-logging、UserSelectModal、import/CSS/bundle 既有警告；公网 API Key/隔离 dev 库缺失项未扩展执行 | 无服务启动 |

## 代码质量备忘

- Adapter Registry 必须检测重复 code，避免 Spring Bean 增加后静默覆盖；
- `AiClientImpl`、`ChatClientCache`、`AiProviderService` 最终不应包含具体供应商模型构造；
- 缓存键和移除日志禁止出现 API Key 或可逆凭据；
- 连接测试异常必须清理供应商 SDK 可能携带的敏感请求信息后再返回；
- 活动供应商页面是 `provider-model.vue`，旧 `provider.vue` 不在本次扩展范围；
- Maven 发布依赖与本地 vendor 源码必须保持相同 release，不使用 vendor 目录作为生产源码依赖。

## Review 结论

- Spec Compliance：PASS；
- Code Quality：PASS，事务同步异常边缘分支已修复并复验；
- 安全与依赖：PASS，未发现密钥泄漏，未引入 DashScope Starter；
- 保留警告：JVM CDS、commons-logging、既有 deprecated/unchecked Java 警告和既有 Vite 组件/import/CSS/bundle 警告均不阻断；
- 保留跳过项：未提供 `AI_DASHSCOPE_API_KEY`，未执行付费公网模型调用；未提供隔离 dev 数据库，未实跑 Flyway；
- 服务清理：本变更未启动后端或前端服务；
- 当前状态：review，未归档、未推送。

### 2026-07-11 独立复审补充

- 未发现 Critical 或 Important 问题；
- 非阻断建议 1：后续迁移可对已存在的 `adapter_code` 列继续校验类型、默认值和 nullability，覆盖非标准半迁移数据库；
- 非阻断建议 2：未来新增 Adapter 时，可把 `testConnection` 的配置校验和网络调用放到不同异常边界，确保任何自定义 Adapter 的网络异常都统一安全化；
- 最终结论仍为 Spec Compliance PASS / Code Quality PASS，可进入 `/archive`。
