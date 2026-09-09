# 单测 Spec — 统一企业协同集成与企业微信全能力接入
> status: propose
> created: 2026-07-28

## 0. 测试原则

- **Red/Green TDD**：安全策略、同步规划器、Token 并发、消息部分失败、待办状态机和回调去重必须先有失败用例，再实现 Green。
- **First Run the Tests**：开始每个模块前先运行现有相关测试，记录工作区基线，不把用户其他未提交改动造成的失败归因到本变更。
- **展示工作**：所有结论必须在 `execution-log.md` 记录实际命令、测试数量、关键输出、警告、跳过和服务清理。
- **增量复用**：每轮先读取本变更四份文档和 `code-copilot/rules/automated-testing-standard.md`，只增加本轮风险对应验证。
- **真实平台与 Mock 分层**：MockWebServer 用于稳定覆盖错误、限流和重放分支；真实企微测试企业 UAT 是上线门禁，二者不能互相替代。
- **敏感数据最小化**：测试只用合成 corpId/userid/Secret/手机号/邮箱；失败输出和快照不得包含原始 Secret、Token、回调正文或真实个人资料。
- **状态与权限优先**：涉及账号停用、组织归属、流程动作和跨租户查询时，除成功路径外必须覆盖越权、过期、重复和并发路径。

## 1. 测试框架与基线

| 项目 | 值 |
|------|-----|
| Java | Java 17 |
| 单元测试 | JUnit 5、AssertJ |
| Mock | Mockito |
| HTTP 合同 | OkHttp MockWebServer，沿用 `forge-starter-outbound` 现有依赖/风格 |
| Controller | Spring MockMvc，真实用户链路不使用 `X-Inner-Call` 代替 |
| 前端 | Vitest（目标逻辑/组件）+ Vite production build，Node 20.19.0 |
| 数据库 | MySQL 8 临时库执行 Flyway；无数据库时只可作为中间检查，不可完成上线门禁 |
| Redis | 单元测试使用 Fake/Mock；Token/state/分布式锁并发由真实 Redis 集成测试补充 |
| 当前 Social 测试 | `forge-starter-social` 当前未发现测试，属于本变更必须补齐的高风险空白 |
| 当前 Message 测试 | `forge-plugin-message` 当前未发现测试，需补发送幂等、部分失败和回归合同 |
| 当前 Flow 测试 | 当前发现 6 个 Flow 测试类，含 `FlowTaskEventListenerTest` 和 `FlowTaskActionAuthorizationTest` |

## 2. 覆盖范围

### P0 — Provider SPI 与装配

#### 类：`CollaborationProviderRegistry`

| 场景 | 输入/前置 | 预期 |
|------|-----------|------|
| 注册完整 Provider | Fake Provider + 5 个 Connector | 能按平台/能力解析正确 Connector |
| 部分能力 Provider | 仅 DIRECTORY/MESSAGE | 已实现能力可用，TODO 返回 `CAPABILITY_NOT_SUPPORTED` |
| 重复 Provider | 两个相同 platform Bean | 应用启动失败，错误不包含配置 Secret |
| Connector 类型不匹配 | 能力声明与 Bean 类型不一致 | 启动失败关闭，不运行时强转 |
| 平台无关编排 | Fake Provider 执行全链路 | 编排结果正确且无需企微常量 |

### P0 — 凭据和迁移

#### 类：`SocialAppCredentialService`、`CollaborationCredentialMigrationService`

| 场景 | 输入/前置 | 预期 |
|------|-----------|------|
| 新 Secret | 合成 Secret + 当前 activeKeyId | 输出版本化认证密文，不包含原文 |
| 相同明文多次加密 | 相同 Secret | IV 不同，密文不同，均可正确解密 |
| 篡改密文 | 修改 IV/ciphertext/tag/keyId | 失败关闭，不回退明文 |
| 空值更新 | 已有密文 + 空请求 | 存储零写并保留原密文 |
| 显式轮换 | 新 Secret | CAS 替换，旧 Secret 立即不可用，缓存失效 |
| 并发轮换 | 旧密文已被其他请求替换 | CAS 返回 0，事务失败，不恢复旧密文 |
| 旧明文 dry-run | 可唯一归属的旧连接 | 只报告 MIGRATABLE，不写库、不输出原文 |
| 歧义旧绑定 | 同平台同租户多个连接 | BLOCKED，不猜测 connectionId |
| 批次冲突 | 任一 CAS 返回 0 | 当前批次全部回滚，不能部分计为 MIGRATED |
| 缺失 key ring | 持久化加密关闭/未知 keyId | 启动或操作失败关闭，禁止写明文 |
| 外部 Secret 引用 | `secretMode=EXTERNAL_REF` 且安装匹配 Resolver | 运行时按引用读取，数据库/API 不出现明文 |
| 缺失外部 Resolver | `secretMode=EXTERNAL_REF` 但无实现 | 保存测试/运行失败关闭，不回退空 Secret |

### P0 — 连接、物理应用与能力绑定

#### 类：`SocialConfigServiceImpl`、`SocialAppConfigServiceImpl`

| 场景 | 输入/前置 | 预期 |
|------|-----------|------|
| 同平台多连接 | 同租户两个不同 enterpriseId | 均可保存并按 connectionCode 精确读取 |
| 同应用多能力 | 一个 app 绑定 DIRECTORY/MESSAGE/TODO | Secret 只保存一份，每个能力解析到同一 appId |
| 能力重新绑定 | MESSAGE 从 appA 切到 appB | 原绑定失效，新请求只使用 appB，相关缓存清理 |
| 并发重复绑定 | 同连接同能力同时绑定不同应用 | 只有一个活动绑定，另一事务冲突失败 |
| 旧平台读取 | 同租户同平台只有一个连接 | 兼容读取成功并产生弃用指标 |
| 旧平台读取歧义 | 同租户同平台多个连接 | 失败关闭，禁止 `limit 1` 随机选择 |
| 查询详情 | 已配置 inline/external Secret | VO 只返回状态、固定掩码、keyId/轮换时间，不返回密文或引用全文 |

### P0 — OAuth state、票据和登录

#### 类：`SocialOAuthStateService`、`SocialController`、`SocialAuthStrategyImpl`

| 场景 | 输入/前置 | 预期 |
|------|-----------|------|
| 企业应用授权地址 | 连接旧 `clientSecret` 已清空，LOGIN 应用有 `secretCipher` | 解密应用 Secret 后构造授权请求，使用完成即清零，不把密文或明文请求放入缓存 |
| state 正常消费 | 有效 state + 正确连接/客户端 | 一次成功，第二次失败 |
| state 篡改/过期 | 非法、过期或连接不匹配 | 回调拒绝，不调用 Provider 登录 |
| 回调成功 | Provider 返回合成身份 | 响应只有一次性 socialTicket，不含 AuthUser/Token/uuid |
| 票据正常消费 | 正确 client + 已同步映射 | 加载映射 Forge 用户并登录 |
| 客户端伪造 uuid | socialUuid/socialNickname 自报 | 字段被忽略，不能改变身份 |
| 跨租户/跨连接 | 票据与登录请求不一致 | 失败关闭 |
| 未同步用户 | 无 `sys_user_social` 映射 | 不自动注册，生成/复用问题单 |
| 离职/停用 | 连接、应用、映射或 Forge 用户任一停用 | 登录拒绝 |
| 同平台同 userid | 两租户/两连接均有相同 userid | 各自只登录对应 Forge 用户 |
| 日志合同 | 成功/失败回调与登录 | 不包含 AuthUser JSON、Secret、Token、邮箱或手机号 |

### P0 — Token 与企微 HTTP 合同

#### 类：`WeComAccessTokenProvider`、`WeComApiClient`、`WeComErrorClassifier`

| 场景 | Mock 行为 | 预期 |
|------|-----------|------|
| Token 首次获取 | 200 + access_token/expires_in | 缓存键含 tenant/connection/app/tokenType，日志无 Token |
| 缓存命中 | 未过刷新窗口 | 不调用远端 |
| 提前刷新 | 进入安全窗口 | 刷新并替换缓存 |
| 并发刷新 | 20 个并发请求 | 分布式锁下远端最多一次，其他请求读取新值 |
| Token 失效 | 首次返回 Token 错误，刷新后成功 | 只强制刷新一次并重放一次 |
| 连续 Token 失效 | 两次均失败 | 返回分类错误，不无限重放 |
| HTTP 429/企微限流码 | 限流响应 | 生成带 retryAfter 的临时错误 |
| 永久参数错误 | 非法 AgentId/userid | 标记不可自动重试 |
| 非白名单目标 | 非企微固定域名 | 出站策略拒绝 |
| 异常日志 | URL 带敏感参数、响应含 Token | 只记录场景、错误码、请求 ID 和耗时 |

### P0 — 回调安全与收件箱

#### 类：`WeComCallbackCrypto`、`CollaborationCallbackInboxService`、`CollaborationCallbackController`

| 场景 | 输入 | 预期 |
|------|------|------|
| URL 验证 | 正确签名/echostr | 返回解密明文挑战值 |
| 多应用回调 | 同一连接下目录 app 和待办 app 使用不同密钥 | `connectionCode + appCode` 各自加载正确凭据，事件归属正确应用 |
| 应用码错误 | 连接存在但 appCode 不存在、停用或不属于该连接 | 拒绝且不尝试其他应用凭据 |
| 签名篡改 | 错误 msg_signature | 401/业务拒绝，零落库 |
| CorpId 错误 | 可解密但接收方错误 | 拒绝，零业务处理 |
| 时间窗过期 | 超出允许窗口 | 拒绝，不进入重试队列 |
| 重复 eventId | 相同事件两次 | 收件箱一条，第二次幂等应答 |
| 无 eventId 重放 | 相同规范负载/时间/nonce | dedupHash 阻止重复处理 |
| 超大正文 | 超过配置上限 | 在解密前拒绝 |
| 快速应答 | 后续处理器阻塞/失败 | Controller 仍在企微要求窗口内应答，事件留待补偿 |
| 负载存储 | 合成敏感字段 | 只保存认证密文；查询 VO 不返回解密正文 |

### P1（阻断用例）— 目录快照与停用安全

#### 类：`DirectorySnapshotValidator`、`DirectorySyncPlanner`、`DirectorySyncOrchestrator`

| 场景 | 快照/当前状态 | 预期 |
|------|---------------|------|
| 首次全量 | 合法部门树、成员、标签 | 按父级顺序创建并建立映射 |
| 重复全量 | sourceHash 全部相同 | 业务表零更新，批次记 UNCHANGED |
| 部门循环 | A -> B -> A | 校验失败，零业务写入/停用 |
| 缺失父级 | 子部门父 ID 不存在 | 生成阻塞问题，零停用 |
| 分页重复/缺页 | 重复 ID 或游标异常 | 批次失败，不处理 unseen |
| 中途网络失败 | 成员页拉取失败 | 已存在用户/组织不被停用 |
| 成功快照少一人 | 上批有，本批完整且无该用户 | 只按已确认离职策略停用映射 |
| 外部字段未变化 | 只有抓取时间变化 | 不更新 Forge 审计字段 |
| 并发全量 | 同连接两个命令 | 只有一个获得锁，另一个返回 ALREADY_RUNNING |
| 跨租户数据 | Mapper 返回/传入其他租户 ID | Service 拒绝，SQL 合同显式带 tenantId |

### P1（阻断用例）— 人员匹配与组织写入

#### 类：`UserIdentityMatchPolicy`、`ForgeDirectoryWriter`、`DirectorySyncIssueService`

| 场景 | 输入 | 预期 |
|------|------|------|
| 已有稳定映射 | connection + userid 命中 | 更新允许字段和组织关系 |
| 员工编码唯一 | 客户已启用该策略且唯一命中 | 建立映射，不创建新用户 |
| 手机/邮箱相同 | 无稳定员工编码 | 不合并，创建 MATCH_REQUIRED 问题单 |
| 员工编码重复 | 多个 Forge 用户命中 | 创建冲突，零绑定 |
| 手工组织/角色/岗位 | 外部快照未包含 | 不删除、不覆盖 |
| 转部门 | 映射用户主部门变化 | 更新被连接管理的用户组织关系，保留审计 |
| 多连接用户离职 | 另一活动连接仍绑定 | 只停用当前映射，不停用 Forge 用户 |
| 人工绑定越权 | 无 resolve 权限或跨租户 issueId | 拒绝且零写入 |

### P1（阻断用例）— 消息核心和企微渠道

#### 类：`MessageServiceImpl`、`CollaborationMessageChannel`、`WeComMessageConnector`

| 场景 | 输入/Mock | 预期 |
|------|-----------|------|
| WEB/SMS/EMAIL 回归 | 现有渠道请求 | 行为和记录兼容 |
| 原子幂等 | 相同 idempotencyKey 并发 20 次 | 只有一份逻辑消息/投递 |
| 全部成功 | 3 个已映射用户 | 三人成功，发送记录计数一致 |
| 部分失败 | 企微返回一个 invaliduser | 仅该接收人失败并进入可处理状态 |
| 无映射用户 | Forge user 无连接映射 | 不调用远端，记录 IDENTITY_NOT_MAPPED |
| 重试部分失败 | 一成功一失败 | 只发送失败接收人，成功人不重复收到 |
| 模板超长/非法 URL | 不满足企微限制 | 发送前拒绝，错误可读但不含内容全文 |
| 停用连接/应用 | status=0 | 不调用远端，记录永久失败 |

### P2（阻断用例）— 待办投影与受控动作

#### 类：`CollaborationTodoProjectionService`、`CollaborationTodoEntryService`、`CollaborationTodoActionService`

| 场景 | 任务事件 | 预期 |
|------|----------|------|
| 创建/重复创建 | 相同 taskId/assignee/version | 一条活动投影，一次外部发送 |
| 转派 | A -> B | A 期望 CLOSED，B 新建 PENDING，版本递增 |
| 签收 | 候选人 -> assignee | 无权候选人投影关闭，处理人投影保持活动 |
| 完成/驳回/撤回/退回/终结 | 终态事件 | 所有活动投影关闭，重复事件幂等 |
| 外网失败 | Connector 抛临时错误 | Forge 任务不回滚，投影进入 RETRY_WAIT |
| 版本并发 | 旧 worker 回写成功状态 | CAS 失败，不能覆盖新转派状态 |
| 入口票据篡改/重放 | 非法/已消费 ticket | 拒绝且不创建 Forge 会话 |
| 身份不匹配 | 卡片收件人与 OAuth userid 不同 | 拒绝并审计安全事件 |
| 任务已完成/转派 | 点击旧卡片 | 不执行动作，返回已失效并关闭卡片 |
| 默认直接审批 | 连接未启用 directAction | 只允许 OPEN，APPROVE/REJECT 拒绝 |
| 受控同意/驳回 | 已启用 + 当前用户有权 | 复用 Forge 权限、状态和 actionIdempotencyKey 后只执行一次 |
| 跨租户/超级管理员 | 票据和 task 不同租户 | 显式租户查询拒绝，不依赖拦截器旁路 |

### P1 — Mapper XML、Flyway 和静态合同

- 所有 Collaboration 查询 XML 显式包含 `tenant_id`、`connection_id` 和逻辑删除条件。
- 不在 Service/Controller 新增复杂 `LambdaQueryWrapper`；同步/列表/冲突/迁移 SQL 均位于 Mapper XML。
- 所有逻辑删除实体显式 `@TableLogic(value = "0", delval = "id")`，字段类型与数据库一致。
- 三个迁移脚本版本唯一，结构和数据有防重复保护，业务 `${...}` 使用安全拼接，不被 Flyway 解析。
- 旧 `sys_social_config` 和 `sys_user_social` 歧义数据预检失败，不被 SQL 自动错误归属。
- `sys_social_tag_member` 物理替换仅用于连接内关系重建，其他主数据删除均走逻辑删除。
- `sys_resource`/字典/Job/出站白名单 `tenant_id=1`，权限插入和角色授权有 `NOT EXISTS`。
- 静态扫描确认业务编排包不包含按企微/飞书/钉钉的 switch/if；平台判断只允许存在于 Provider 注册或适配器内部。

### P1 — Controller、权限和 API 安全

- 所有管理接口未登录、缺权限、跨租户 ID 均拒绝。
- Secret 写接口有 `@ApiDecrypt`；查询 VO 只有 `hasSecret/maskedSecret/keyId/rotatedAt`，没有密文/明文。
- 操作日志切面不会序列化 Secret、Callback Token、EncodingAESKey 或迁移请求中的敏感值。
- 回调公开端点只绕过登录，不绕过签名、时间窗、CorpId、重放和连接状态校验。
- 人工绑定、重试、直接审批开关和凭据迁移具有独立权限。

### P2 — 前端与浏览器验证

- 连接/应用表单枚举来自字典，Secret 编辑为空时保留，显式轮换时要求重新输入。
- 浏览器 Network、Vue 状态和详情弹窗中不出现 Secret、密文、Token、原始回调负载。
- 同步批次显示阶段、计数和问题，不通过可见文本暴露个人敏感资料。
- 问题单人工绑定、忽略、重试有权限和二次确认；重复点击不会提交两次。
- 消息/待办投递页可筛选连接、类型、状态、错误码和时间；只重试失败接收人。
- 1440x900、1024x768、390x844 视口下无横向页面溢出、文本遮挡或按钮重叠。
- 生产构建通过；新增页面的目标 Vitest 和 ESLint 通过。

### P1 — M1 真实企业微信 UAT

1. 连接测试：Token、部门、成员、标签和测试消息。
2. 首次全量：部门层级、员工数量、主部门、标签关系与企微测试数据一致。
3. 增量：新增员工、改名、转部门、离职、标签变化在 SLA 内同步；漏事件由下一次全量修复。
4. 登录：已同步用户成功，未同步/停用/冲突用户失败且管理员可处理。
5. 消息：文本和模板卡片到达正确人员，跳转链接正确，部分失败可补偿。
6. 目录回调：合法事件一次处理；篡改、重放和错误应用凭据被拒绝。
7. 故障：断网、限流、Token 失效和应用停用后状态可观测，恢复后只补偿未完成项。

M1 通过后可以独立上线，不要求客户先提供待办应用或完成下列 M2 UAT。

### P2 — M2 真实企业微信待办 UAT

1. 待办：创建、转派、签收、完成、撤回/终结与企微卡片状态一致。
2. 安全入口：合法用户打开正确 Forge 待办；篡改、重放、过期、身份不匹配和旧卡片均被拒绝。
3. 回调：合法事件一次处理；签名篡改、错误应用、重复事件和旧动作被拒绝。
4. 故障：断网、限流、Token 失效和待办应用停用后可观测，恢复后只补偿未完成投影。
5. 卡片直批默认关闭并验证拒绝；客户另行启用时才按流程白名单执行同意/驳回专项 UAT。

### 不测试（明确列出原因）

- 飞书、钉钉真实 API：本次只交付扩展合同，真实适配器在后续独立变更测试。
- 生产组织全量数据：只在客户测试企业/UAT 环境验证，禁止把真实全员数据复制到开发测试库。
- 移动端复杂表单渲染：不在本次交付边界；只验证安全跳转到现有 Forge 待办页面。
- 未经客户另行启用的卡片直接审批：通用网关和关闭状态开关做自动化验证，M2 基线 UAT 只验证拒绝行为。

## 3. 执行计划

- [ ] Step 1：记录 Social、Message、Flow、Job、Crypto 和 Admin UI 现有测试/构建基线。
- [ ] Step 2：创建 Provider Registry/Fake Provider Red 测试，确认通用编排不依赖企微。
- [ ] Step 3：创建 Secret、迁移、OAuth state/票据、Token 并发和回调安全 Red/Green 测试。
- [ ] Step 4：创建目录快照、停用保护、身份冲突和 Mapper XML Red/Green 测试。
- [ ] Step 5：创建消息幂等/部分失败和待办状态机/受控动作 Red/Green 测试。
- [ ] Step 6：执行相关 Reactor package、Admin Server 装配、前端目标测试和 production build。
- [ ] Step 7：在 MySQL 8/Redis 临时环境执行迁移、密文、租户和并发集成测试。
- [ ] Step 8A：使用真实企微测试企业执行 M1 目录/登录/消息 UAT，完成一期复审、回滚和上线门禁。
- [ ] Step 8B：Gate C 后执行 M2 待办/回调 UAT，完成二期复审、回滚和上线门禁。
- [ ] Step 9：每个里程碑分别执行 Spec 合规、代码质量、安全/权限复审，关闭 Critical/Important 后复测。
- [ ] Step 10：每个里程碑分别完成部署、回滚、监控和留存任务演练后更新状态。

## 4. 计划命令

> 以下命令在对应模块创建后执行；实际命令和输出必须写入 `execution-log.md`。

### 4.1 后端目标测试

```bash
cd forge-server
mvn -pl \
  forge-framework/forge-starter-parent/forge-starter-collaboration,\
  forge-framework/forge-starter-parent/forge-starter-social,\
  forge-framework/forge-plugin-parent/forge-plugin-collaboration,\
  forge-framework/forge-plugin-parent/forge-plugin-message,\
  forge-framework/forge-plugin-parent/forge-plugin-flow \
  -am test -Dforge.compiler.skip=false -Dforge.tests.skip=false -Dforge.test.groups=
```

### 4.2 后端装配构建

```bash
cd forge-server
mvn -pl forge-admin-server -am package -DskipTests
```

### 4.3 前端

```bash
source ~/.nvm/nvm.sh
nvm use v20.19.0
pnpm --dir forge-admin-ui vitest run src/views/system/collaboration
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

### 4.4 低成本静态检查

```bash
git diff --check -- code-copilot/changes/unified-enterprise-collaboration \
  forge-server/forge-framework/forge-starter-parent/forge-starter-collaboration \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration \
  forge-admin-ui/src/views/system/collaboration

rg -n '\$\{[^}]+\}' forge-server/db/migration
rg -n 'clientSecret|accessToken|refreshToken|encodingAESKey|callbackToken' \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-collaboration/src/main/java
```

静态敏感扫描只报告文件、行号和模式，不打印运行时 Secret/Token 值。第二条 Flyway 扫描应无本变更新增命中；第三条命中需要逐项确认仅为字段名、掩码或安全处理，不得直接以“有命中”判失败或通过。

## 5. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-28 | 现状调查 | 文件/类/DDL 静态读取 | 完成 | Social/Message 当前无目标测试；Flow 存在 6 个测试类；未执行构建或真实服务 |
| 2026-07-28 | 客户需求分析 | 三份需求文档与客户任务清单交叉核对 | 完成 | 确认通讯录早于业务流程联调，消息早于待办完整桥接 |

## 6. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-28 | 仅 SDD 文档 | 空白、占位符、旧命名、类型术语、回调路由、代码块闭合和 Git 状态检查 | `git diff --no-index --check /dev/null <file>`；`rg` 一致性扫描；`awk` 代码块计数；`git status --short` | 通过 | `--no-index` 内容差异退出码 1 按预期处理；未编译、未启动服务、未连接企微/MySQL/Redis |
| 2026-08-24 | Social OAuth LOGIN 应用凭据回归 | Red/Green 单测、Social 凭据相关定向测试、模块打包、静态检查 | `mvn -pl forge-framework/forge-starter-parent/forge-starter-social -am test -Dforge.compiler.skip=false -Dforge.tests.skip=false -Dforge.test.groups= -Dtest=SocialOAuthLoginServiceTest -Dsurefire.failIfNoSpecifiedTests=false`；同命令追加 `SocialAppCredentialServiceTest`；`mvn ... package -DskipTests`；`git diff --check` | Red 用例在旧实现按预期失败；修复后 1/1 通过，扩展定向测试 13/13 通过，模块打包成功 | 存在原有编译警告和篡改密文测试的预期错误日志；未启动服务、未连接真实企微/MySQL/Redis；真实企业授权登录由用户环境联调 |

## 7. 执行证据

- `execution-log.md`：每轮追加命令、数量、接口/数据库结论、警告、跳过和服务 PID。
- 关键接口：连接/应用、OAuth state/票据、同步、问题单、消息、待办、回调、重试。
- 关键数据库检查：Flyway history、活动唯一索引、逻辑删除、旧明文清零、歧义绑定阻塞、逐人投递和待办版本。
- 服务启动与停止：只停止本轮启动的 Admin/Flow/UI/Mock 服务，不处理用户已有进程。
- 真实平台证据：只记录 corpId/应用/用户的脱敏引用、企微错误码/请求 ID 和结果，不保存凭据或个人资料。
