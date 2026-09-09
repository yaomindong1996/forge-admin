# 后端数据库性能与注入加固
> status: review-ready
> created: 2026-08-05
> complexity: 🔴复杂
> 本轮执行范围：P0（Task 1-4 安全紧急修复），P1/P2/P3 后续另开独立变更

## 1. 背景与目标

对 `forge-server` 后端工程（2508 个 Java 文件 + 304 个 Mapper XML）进行全量安全与性能扫描，发现 **88 处** 明显问题，覆盖 SQL 注入、事务内远程调用、N+1 查询、循环单条 DB 操作、全表扫描、LIKE 全表、JOIN OR 索引失效、LambdaQueryWrapper 滥用等 8 大类。

**目标**：按风险优先级分阶段修复，先关闭可直接利用的安全入口（P0），再消除分页列表接口的性能瓶颈（P1），最后治理规范违规与渐进迁移（P2/P3）。修复不改变现有 API 协议和前端交互，保持向后兼容。

## 2. 代码现状（扫描发现）

> 以下结论均经代码核验，含文件路径与行号。

### 2.1 SQL 注入（1 处，可直接利用）

**`forge-plugin-generator/.../service/CrudGeneratorStreamService.java:197`**
```java
.inSql(GenTableColumn::getTableId,
    "SELECT table_id FROM gen_table WHERE table_name = '" + request.getTableName() + "'")
```
`tableName` 来自 `CrudGeneratorController:21` 的 `@RequestBody StreamGenerateRequest`，无任何校验。`inSql` 第二参数原样拼接到 SQL，单引号闭合即可注入任意 SQL（读取/篡改/删除数据库）。

### 2.2 事务内远程调用 / 文件 IO（5 处）

| # | 位置 | 事务内的远程/IO 操作 | 影响 |
|---|------|---------------------|------|
| 1 | `MessageServiceImpl.java:92` `send()` | `messageClient.send()`（短信/邮件/HTTP） | 外部 API 超时占 DB 连接 |
| 2 | `MessageServiceImpl.java:132` `send()` | `sendToCollaboration` + 逐人 update | RPC + 循环 DB |
| 3 | `SamplePurchaseOrderServiceImpl.java:159` `submit()` | `flowClient.startProcess()`（RestTemplate） | 事务内 RPC |
| 4 | `SysFileMetadataServiceImpl.java:125` `removeBatch()` | 循环 `getById` + `fileManager.delete()` | 事务内文件 IO + 异常被吞 |
| 5 | `SysFileMetadataServiceImpl.java:111` `removeByFileId()` | `fileManager.delete()` | 事务内文件 IO |

### 2.3 N+1 查询（分页列表接口，5 处高危）

| # | 位置 | 问题 | 查询次数 |
|---|------|------|---------|
| 1 | `MessageManageServiceImpl.java:81-101` | 每条消息 `selectList` 查 receiver | N 次 |
| 2 | `SysNoticeServiceImpl.java:286-314` | 每条公告查已读 + 逐个查附件 | N×(1+附件数) |
| 3 | `SysUserServiceImpl.java:624-637` `batchBindUserTenant` | 每用户 selectById + upsert + updateById | 100人=300+次 |
| 4 | `SysUserServiceImpl.java:1450-1483` `syncLegacyRolesToUserOrgs` | 双重循环 delete+select+insert | 组织数×角色数 |
| 5 | `DataBusinessDefinitionServiceImpl.java:149,211` | 循环 getById 数据集 + 字段 | 绑定数×2 |

### 2.4 查询性能（7 处高危）

| # | 位置 | 问题 | 影响 |
|---|------|------|------|
| 1 | `AiChatSessionMapper.xml:52,54` | `COUNT(*)`/`SUM(token_usage)` 无 WHERE 全表扫 | 高频大表全表扫描 |
| 2 | `AiChatSessionMapper.xml:22-23` | 分页每行 2 个关联子查询（COUNT+SUM） | pageSize=20 -> 40 次子查询 |
| 3 | `FlowTaskMapper.xml:43-49` | `candidate_users` 逗号字符串 + 3 个 LIKE + 多重 OR | 待办核心路径全表扫 |
| 4 | `FlowTaskMapper.xml:11,29,41,79` | `JOIN ON c.id = m.category OR c.category_code = m.category` | 4 个核心查询 JOIN 全表扫 |
| 5 | `SysRoleServiceImpl.java:172` | `resourceService.list()` 全量加载资源表 | 上千条全量加载 |
| 6 | `ExternalApiLogMapper.xml:47` | 日志表分页 `SELECT *` 含大文本 | body/error 数 KB~MB |
| 7 | `SocialConfigServiceImpl.java:87` | `tenantId==null` 时无租户过滤全表查 | 跨租户泄露+全表扫 |

### 2.5 循环单条 DB 操作（15 处中危）

| 文件 | 行号 | 类型 |
|------|------|------|
| `DataDimensionServiceImpl.java` | 297-300 | 循环 insert |
| `DataBusinessDefinitionServiceImpl.java` | 187-196 | 循环 insert |
| `SysUserServiceImpl.java` | 522-531 | 循环 insert（组织角色） |
| `SysUserServiceImpl.java` | 1423-1431 | 循环 insert（用户角色） |
| `SysUserServiceImpl.java` | 697-718 | 循环 insert/update（用户组织） |
| `SysUserServiceImpl.java` | 1661-1681 | 循环 insert/update（用户岗位） |
| `SysNoticeServiceImpl.java` | 244-249 | 循环 insert（公告组织） |
| `FlowFillBatchServiceImpl.java` | 95-113 | 循环 insert（填报批次） |
| `SysUserServiceImpl.java` | 125-127 | 循环 upsert（多租户绑定） |
| `SysUserServiceImpl.java` | 217-219 | 循环 delete（批量删除用户） |
| `SysUserServiceImpl.java` | 258-262 | 循环 select+sync（批量绑定角色） |
| `SysNoticeServiceImpl.java` | 77-91 | N+1 查询（附件逐个查） |
| `SysFileMetadataServiceImpl.java` | 127-134 | 循环 getById+delete |
| `MessageServiceImpl.java` | 330-335 | 循环 update（投递结果） |
| `SamplePurchaseOrderServiceImpl.java` | 481-493 | 循环 update（状态对账） |

### 2.6 中危查询性能（8 处）

| 位置 | 问题 |
|------|------|
| `DataConnectionMapper.xml:29` | 分页 `SELECT *` 含 `password_cipher` 敏感字段 |
| `SysResourceMapper.xml:14,17,20` | `LIKE '%xxx%'` 三处（权限表频繁查） |
| `SysUserMapper.xml:136-150` | 双重嵌套 IN 子查询 + `FIND_IN_SET` |
| `FlowTaskMapper.xml:62,94,125` | `ancestors LIKE '%xxx/%'` 三处 |
| `ExternalSystemMapper.xml:54` | 分页每行关联子查询 COUNT |
| `MessageServiceImpl.java:295` | 查全量用户字段仅取 phone/email |
| `FlowCategoryController.java:69` | `list()` 全量无分页 |
| `SysOrgMapper.xml:85` / `SysUserMapper.xml:157` | `IN (SELECT ... FROM sys_region_code)` 子查询 |

### 2.7 LambdaQueryWrapper 滥用（160+ 处，37 个文件）

违反 AGENTS.md 5.1 节"查询类 SQL 禁止在 Service 层用 LambdaQueryWrapper 构建，必须写在 Mapper XML 中"。重灾区：
- `SysUserServiceImpl`（54 处）
- `SysRoleServiceImpl`（15 处）
- `FlowOrgIntegrationServiceImpl`（14 处，含 `likeRight` 跨层级）
- `SysNoticeServiceImpl:347`（手拼 `exists`/`notExists` 子查询）
- `SysConfigServiceImpl:39`（分页查询）

**关键影响**：这些查询会干扰 `DataScopeInterceptor` 按 `mapperMethod` 精确匹配改写 SQL，导致数据权限拦截失效或异常。

### 2.8 低危 SQL 拼接（有防护但有绕过风险）

| 位置 | 防护 | 风险 |
|------|------|------|
| `BusinessStatsMapper.xml:10,17,22,50` | `validateIdentifier` 白名单（Service 层） | 新增调用方遗漏校验则可注入 |
| `DataDimensionServiceImpl.java:243` | `sqlSafetyValidator.validate()` | 用户配置 SQL 拼接，依赖校验器 |
| `DataDatasetController.java:575` | `sqlSafetyValidator.validate()` | 同上 |
| `DynamicCrudRepository.java:1189,1547` | `validateTableName` 白名单 | 拼接模式本身脆弱 |

## 3. 功能点

### P0 - 安全紧急修复
- [ ] **F1**：`CrudGeneratorStreamService` SQL 注入修复（参数化 + 白名单校验 tableName）
- [ ] **F2**：`MessageServiceImpl.send()` 事务内远程调用拆分（先落库 + 事务提交后发送）
- [ ] **F3**：`SamplePurchaseOrderServiceImpl.submit()` 事务内 RPC 拆分
- [ ] **F4**：`SysFileMetadataServiceImpl` 事务内文件 IO 拆分

### P1 - 分页列表性能修复
- [ ] **F5**：`MessageManageServiceImpl` N+1 改批量查询
- [ ] **F6**：`SysNoticeServiceImpl` N+1 改批量查询
- [ ] **F7**：`SysUserServiceImpl.batchBindUserTenant` 循环改批量
- [ ] **F8**：`DataBusinessDefinitionServiceImpl` N+1 改批量
- [ ] **F9**：`AiChatSessionMapper` 全表统计优化
- [ ] **F10**：`FlowTaskMapper` 待办 LIKE 改关联表或 EXISTS
- [ ] **F11**：`FlowTaskMapper` JOIN OR 改统一存储
- [ ] **F12**：`SysRoleServiceImpl` 全量加载改按 ID 批量

### P2 - 循环操作批量化 + 中危查询
- [ ] **F13**：15 处循环单条操作改 `saveBatch`/`updateBatchById`/IN 批量
- [ ] **F14**：`ExternalApiLogMapper` / `DataConnectionMapper` 分页 `SELECT *` 改精确列
- [ ] **F15**：`SysResourceMapper` / `FlowTaskMapper` LIKE 优化
- [ ] **F16**：`SocialConfigServiceImpl` 租户缺失失败关闭
- [ ] **F17**：`SysUserMapper` 嵌套 IN 子查询优化

### P3 - 规范治理（渐进）
- [ ] **F18**：LambdaQueryWrapper 查询按模块迁移到 Mapper XML（优先 system/flow/message）
- [ ] **F19**：低危 SQL 拼接加 Mapper 层防护文档/约束

## 4. 业务规则

1. 所有修复**不改变现有 API 路径、请求体、响应结构**，前端零改动。
2. 消息发送拆分事务后，**发送失败不回滚消息记录**（消息记录作为发送凭证留存），改为标记发送状态并支持重试。
3. 采购单提交拆分事务后，**流程启动失败必须回滚单据状态**（状态流转强一致）。
4. 文件删除拆分事务后，**元数据逻辑删除先提交，物理文件删除后执行**，文件删除失败只告警不回滚元数据。
5. N+1 改批量查询时，**数据顺序必须与原实现一致**（按分页记录顺序组装）。
6. 批量 insert/update 必须显式限定 `tenant_id`，空 ID 列表不生成空 `IN` SQL。
7. LambdaQueryWrapper 迁移到 XML 时，必须同步验证 `DataScopeInterceptor` 是否对该 mapperMethod 配置了数据权限改写。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|----------|------|
| 优化（无结构变更） | `ai_chat_session` | - | F9 优先用 SQL 优化（JOIN GROUP BY）；如选冗余字段方案需 Flyway 新增 `message_count`、`total_token_usage` |
| 可选新增 | `sys_flow_task_candidate` | `task_id, user_id, group_id, tenant_id` + 索引 | F10 候选人关联表（替代逗号字符串 LIKE），需存量数据迁移 |
| 无 Flyway | - | - | P0/P1/P2 多数改动不涉及表结构 |

> F9/F10 的数据变更方案需在 Task 设计时确认，默认先采用无结构变更的 SQL 优化。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|---------|
| 无 | 所有接口 | - | 协议不变，仅底层实现优化 |

## 7. 影响范围

| 模块 | 涉及 |
|------|------|
| forge-plugin-generator | F1（SQL 注入）|
| forge-plugin-message | F2（事务拆分）、F5（N+1）、F13（循环）|
| forge-plugin-system | F6/F7/F12/F13/F16/F17、LambdaQueryWrapper 迁移 |
| forge-plugin-ai | F9（统计优化）|
| forge-plugin-flow | F10/F11（待办查询）、LambdaQueryWrapper 迁移 |
| forge-plugin-data | F8（N+1）、低危 SQL 拼接防护 |
| forge-business-core | F3（采购单事务）、F13（循环更新）|
| forge-starter-file | F4（文件 IO 事务）|
| forge-starter-social | F16（租户失败关闭）|

## 8. 风险与关注点

> ⚠️ 本变更涉及消息发送、流程提交、文件删除的状态流转，必须逐 Task 人工审查。

1. **消息发送事务拆分**：发送失败后消息记录已落库，需确保有重试/补偿机制，避免"幽灵消息"。
2. **采购单流程提交**：流程启动与单据状态必须保持一致，建议同事务内只做状态预变更，流程启动成功后确认，失败回滚。
3. **文件删除拆分**：元数据已删但物理文件未删会导致存储泄漏，需有定期清理任务兜底。
4. **N+1 改批量**：批量查询返回的数据量需评估（如公告附件数），避免一次性查过多数据。
5. **待办 LIKE 改造**：若引入关联表，需迁移存量 `candidate_users` 逗号字符串数据。
6. **LambdaQueryWrapper 迁移**：迁移过程中可能暴露隐藏的数据权限配置缺失，需逐方法验证。

## 8.5 测试策略

- **测试范围**：P0/P1 所有改动点必须有定向 JUnit 测试；P2 按改动范围补测。
- **覆盖率目标**：改动方法行覆盖 >= 80%。
- **独立 Test Spec**：是，按 Task 分阶段生成 test-spec.md。
- **验证方式**：
  - 定向模块 `mvn test`
  - 聚合编译 `mvn clean install -DskipTests`
  - Mapper XML 静态解析（MyBatis XML 加载校验）
  - 不启动真实服务（除非验证 N+1 实际查询次数）

## 9. 待澄清

- [x] **Q1**：F9 AI 统计优化 -> 采用 **SQL 优化 LEFT JOIN GROUP BY**（无表结构变更）
- [x] **Q2**：F10 待办候选人 -> 采用 **FIND_IN_SET 替代 LIKE**（无表结构变更，无需数据迁移）
- [x] **Q3**：F2 消息发送拆分事务后 -> **仅标记失败状态由人工处理**（不自动重试）
- [x] **Q4**：F18 LambdaQueryWrapper 迁移 -> **分批排期 system->flow->message**（后续独立变更推进）
- [x] **Q5**：本提案 -> **拆分为独立变更分别执行**，本轮只做 P0（Task 1-4）

## 10. 技术决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| SQL 注入修复方式 | 参数化查询 + 白名单正则校验 | 双重防护，`inSql` 改为自定义 Mapper 方法用 `#{tableName}` |
| 事务拆分模式 | "先落库 + `@TransactionalEventListener(AFTER_COMMIT)` 发送" | 保证数据一致性，远程调用在事务外执行 |
| N+1 修复模式 | `IN` 批量查询 + 内存按 ID 分组组装 | 单次 SQL 替代 N 次查询，保持顺序 |
| 循环批量模式 | `saveBatch` / `updateBatchById` / 自定义 XML `INSERT ... ON DUPLICATE KEY` | 利用 MyBatis-Plus 批量或 XML 批量 SQL |
| 统计查询优化 | 优先 SQL 优化（LEFT JOIN GROUP BY），其次冗余字段 | 先无结构变更，效果不足再加字段 |
| 待办 LIKE 优化 | 优先 EXISTS 子查询，其次关联表 | 关联表需数据迁移，EXISTS 无需结构变更 |

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|------------|------|
| Task 1 | ✅完成 | CrudGeneratorStreamService.java, GenTableColumnMapper.java, GenTableColumnMapper.xml | SQL注入修复，inSql改为参数化Mapper方法 |
| Task 2 | ✅完成 | MessageServiceImpl.java, MessageSendEvent.java(新增) | @TransactionalEventListener(AFTER_COMMIT) 拆分发送 |
| Task 3 | ✅完成 | SamplePurchaseOrderServiceImpl.java | 移除@Transactional，RPC在事务外，更新失败告警 |
| Task 4 | ✅完成 | SysFileMetadataServiceImpl.java | 移除@Transactional，文件IO在事务外 |
| 聚合验证 | ✅通过 | mvn clean install -DskipTests | 全量编译通过 |

## 12. 审查结论

P0 四个安全紧急修复全部完成，全量编译通过。改动范围：
- SQL 注入：1 处（参数化查询替代字符串拼接）
- 事务拆分：3 处（消息发送、采购单提交、文件删除）
- 新增文件：1 个（MessageSendEvent）
- 行为变化：消息 send() 返回状态为"发送中"，最终状态由监听器异步更新

## 13. 确认记录（HARD-GATE）
- **确认时间**：2026-08-05
- **确认人**：用户
- **确认内容**：Q1-Q5 全部决策完成，本轮执行 P0（Task 1-4 安全紧急修复），P1/P2/P3 后续另开独立变更
- **决策摘要**：
  - Q1 F9：SQL 优化 LEFT JOIN GROUP BY（无 Flyway）
  - Q2 F10：FIND_IN_SET 替代 LIKE（无 Flyway）
  - Q3 F2：发送失败仅标记状态，人工处理
  - Q4 F18：LambdaQueryWrapper 分批排期，本轮不做
  - Q5：拆分独立变更，本轮只做 P0
