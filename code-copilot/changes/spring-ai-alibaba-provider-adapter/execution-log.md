# 变更执行日志 — Spring AI Alibaba 供应商适配层与 DashScope 原生接入

> 本文件持续记录提案、实现、验证、审查和归档证据。当前仅处于 propose 阶段。

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

## 技术决策

| 决策 | 选择 | 放弃的方案 | 原因 |
|------|------|------------|------|
| 框架主干 | 保留 Spring AI，叠加 Alibaba | 删除 Spring AI 全量重写 | Alibaba 复用 Spring AI `ChatModel/ChatClient` 抽象 |
| DashScope 接入 | 核心模型模块 + 动态 Adapter | Starter 全局自动配置 | Forge 是多租户数据库动态凭据模式 |
| 路由键 | `adapter_code` | `providerType`、URL 推断 | 历史存在 `alibaba/dashscope` 和 Compatible/Native 差异 |
| 历史行为 | 全部回填 `openai_compatible` | 自动升级为 Native | 避免上线即改变协议、请求地址和计费路径 |
| 失败策略 | Fail-closed | 跨 Adapter 自动重试 | 避免重复请求、重复计费和隐藏配置错误 |
| API Key | 脱敏回显 + 同值保留 | 明文返回、固定值覆盖 | 满足安全规则并保护更新链路 |

## 踩坑记录

| 问题 | 原因 | 解决方案 | 沉淀？ |
|------|------|----------|--------|
| 官方根 README 示例版本落后于 release | README 仍出现 `1.1.2.0/1.1.2.1` | 以 release POM/BOM `1.1.2.3` 为依赖事实 | 实现验证后决定是否写入 pitfalls |
| DashScope Starter 可能无配置即装配 | 两层条件均 `matchIfMissing=true` | Forge 只依赖 core model，不引入 Starter | 实现验证后沉淀 |
| 阿里供应商类型存在 `alibaba/dashscope` 两种历史值 | 多份初始化 SQL 不一致 | 路由独立为 `adapter_code`，不依赖品牌类型 | 实现验证后沉淀 |
| Compatible 与 Native 地址不可混用 | API Path 协议不同 | Adapter 显式校验，历史配置不自动切换 | 实现验证后沉淀 |
| 缓存失效方法无调用者 | 更新/删除链路未接入 Cache | 事务成功后按 tenant/provider 主动清理 | 实现验证后沉淀 |
| ID 测试与临时配置混合会形成密钥边界歧义 | DTO 未定义互斥模式时可能加载持久密钥后接受请求字段覆盖 | 定义严格 one-of；混合请求在模型构建前拒绝 | 实现验证后沉淀 |
| 旧应用无法识别 Native Adapter | 旧代码会把原生 Base URL 当 OpenAI Compatible 使用 | 回退前检查 Native 记录并先切回 Compatible URL | 实现验证后沉淀 |

## 知识发现

- [ ] **Spring AI Alibaba 依赖关系**：Alibaba Agent Framework 与 Extensions 都建立在 Spring AI `1.1.2` 上，迁移应采用增强叠加而不是替换 Spring AI。
- [ ] **动态供应商配置**：多租户数据库凭据场景优先直接构建模型核心类，避免默认全局 Starter Bean。
- [ ] **协议与品牌解耦**：供应商品牌不能稳定表达 API 协议，尤其同一厂商同时提供原生和 OpenAI Compatible 接口。

## Spec-Code 偏差记录

| 偏差点 | Spec 预期 | 实际情况 | 处理方式 |
|--------|-----------|----------|----------|
| 尚未编码 | HARD-GATE 后才允许 `/apply` | 当前仅文档 | 无偏差 |

## 验证记录

| 时间 | 变更范围 | 命令 | 结果 | 警告/跳过 | 服务清理 |
|------|----------|------|------|-----------|----------|
| 2026-07-10 | Proposal 文档 | `rg` 占位符/尾随空格扫描；`test -f` 关键路径；`git diff --no-index --check /dev/null <file>` | 本地静态检查通过 | 文件未跟踪，使用 `--no-index --check` 逐文件检查 | 无服务启动 |
| 2026-07-10 | Reader Test 1 | 新上下文 Agent 只读检查四份提案文档 | 未通过；5 项意见已全部修订，复审中 | 无业务代码或服务验证 | 无服务启动 |
| 2026-07-10 | Reader Test 2-3 | 新上下文 Agent 复审修订文档 | 第二轮补 2 项高风险语义；第三轮 PASS | 本轮仅文档检查 | 无服务启动 |
| 2026-07-10 | Proposal 最终静态复验 | `git status --short`、`git diff --name-only`、`git diff --check`、四文件逐一 `git diff --no-index --check`、尾随空格扫描、关键源码/vendor/文档路径检查、Flyway 版本序列检查 | 全部通过；当前最高迁移为 `V1.0.16`，计划使用 `V1.0.17` 无版本冲突 | 仅文档变更，未执行 Maven、前端构建或公网模型调用 | 无服务启动 |

## 代码质量备忘

- Adapter Registry 必须检测重复 code，避免 Spring Bean 增加后静默覆盖；
- `AiClientImpl`、`ChatClientCache`、`AiProviderService` 最终不应包含具体供应商模型构造；
- 缓存键和移除日志禁止出现 API Key 或可逆凭据；
- 连接测试异常必须清理供应商 SDK 可能携带的敏感请求信息后再返回；
- 活动供应商页面是 `provider-model.vue`，旧 `provider.vue` 不在本次扩展范围；
- Maven 发布依赖与本地 vendor 源码必须保持相同 release，不使用 vendor 目录作为生产源码依赖。
