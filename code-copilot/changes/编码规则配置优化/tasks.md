# 任务拆分 — 编码规则配置优化
> status: fixed_pending_review
> created: 2026-07-16
> 拆分顺序：SDD 基线 → 序列底座 → 数据模型 → 生成引擎 → API → 前端 → 低代码兼容 → 验证

## 前置条件

- [x] 已读取根 `AGENTS.md`、项目记忆、编码规范、自动化测试标准和适用 Skill。
- [x] 已从 `main` 切换到 `feature/code-rule-config-optimization-20260716`。
- [x] 已确认保留 `ai_code_rule` 主表和旧 `/ai/code-rule` 兼容入口。
- [x] 已取得用户 HARD-GATE 确认。

## Task 0: 建立 SDD 变更基线

- [x] 已完成
- **目标**：把需求分析固化为可执行、可验证的 Spec。
- **涉及文件**：
  - `code-copilot/changes/编码规则配置优化/spec.md`
  - `code-copilot/changes/编码规则配置优化/tasks.md`
  - `code-copilot/changes/编码规则配置优化/test-spec.md`
  - `code-copilot/changes/编码规则配置优化/execution-log.md`
- **验收**：13 章 Spec 完整、无待澄清项、测试策略和权限风险明确。

## Task 1: 修复数据库号段切换并支持起始值

- [x] 已完成
- **目标**：消除低余量重复预取造成的跳号，支持新计数 key 从指定 `startValue` 开始。
- **涉及文件**：
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-id/src/main/java/com/mdframe/forge/starter/id/service/ISequenceService.java` — 增加带起始值的默认契约。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-id/src/main/java/com/mdframe/forge/starter/id/service/impl/SequenceServiceImpl.java` — 转发起始值。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-id/src/main/java/com/mdframe/forge/starter/id/generator/SegmentSequenceGenerator.java` — 使用原子状态切换号段，删除重复 next 覆盖。
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-id/src/test/java/com/mdframe/forge/starter/id/generator/SegmentSequenceGeneratorTest.java` — 覆盖首次起始值、999/1000/1001 边界和并发唯一性。
- **关键签名**：
  ```java
  long nextId(String bizKey, long startValue);
  public long nextId(String bizKey, long startValue);
  ```
- **验收**：单 key 连续获取 1200 个值时严格为 `startValue..startValue+1199`；并发结果无重复。

## Task 2: 新增结构化分段数据模型与正式迁移

- [x] 已完成
- **目标**：为规则增加分类/版本，并以稳定分段键持久化五类段。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.36__add_structured_code_rule_segments.sql` — 增加主表字段、子表、字典、菜单/按钮/API 资源及内置规则分段回填。
  - `forge-server/.../generator/domain/entity/AiCodeRule.java` — 增加 `category/version/inCodeList`。
  - `forge-server/.../generator/domain/entity/AiCodeRuleSegment.java` — 新增分段实体和 `@TableLogic`。
  - `forge-server/.../generator/mapper/CodeRuleSegmentMapper.java` — 分段查询、批量插入、逻辑删除接口。
  - `forge-server/.../generator/resources/mapper/CodeRuleSegmentMapper.xml` — 显式 tenant/del_flag 查询和批量写入。
- **关键签名**：
  ```java
  List<AiCodeRuleSegment> selectByRuleId(Long tenantId, Long ruleId);
  int logicalDeleteByRuleId(Long tenantId, Long ruleId, Long updateBy);
  int insertBatch(List<AiCodeRuleSegment> segments);
  ```
- **验收**：SQL 具备 information_schema/NOT EXISTS 保护，内置数据 tenant_id=1，子表查询显式过滤逻辑删除。

## Task 3: 扩展主表查询与结构化 DTO/VO

- [x] 已完成
- **目标**：建立实体隔离的保存、查询、预览和生成协议。
- **涉及文件**：
  - `forge-server/.../generator/mapper/CodeRuleMapper.java`
  - `forge-server/.../generator/resources/mapper/CodeRuleMapper.xml`
  - `forge-server/.../generator/dto/businessapp/CodeRuleSaveDTO.java`
  - `forge-server/.../generator/dto/businessapp/CodeRuleSegmentDTO.java`
  - `forge-server/.../generator/vo/businessapp/CodeRuleDetailVO.java`
- **关键签名**：
  ```java
  public class CodeRuleSaveDTO { Long id; Integer version; String ruleCode; List<CodeRuleSegmentDTO> segments; }
  public class CodeRuleDetailVO { AiCodeRule rule; List<CodeRuleSegmentDTO> segments; List<String> warnings; }
  ```
- **验收**：不再由新管理接口直接接收 `AiCodeRule`；分页返回分类、分段数和兼容状态。

## Task 4: 实现结构化校验、预览、生成和旧模板解析

- [x] 已完成
- **目标**：完成五种段类型、五种进制、分组、周期和 legacy 兼容的无状态核心引擎。
- **涉及文件**：
  - `forge-server/.../generator/manager/coderule/CodeRuleEngine.java` — 校验、预览和真实生成。
  - `forge-server/.../generator/manager/coderule/CodeRuleRadixCodec.java` — 固定宽度进制转换和容量计算。
  - `forge-server/.../generator/manager/coderule/CodeRuleSequenceKeyFactory.java` — 周期和分组摘要 key。
  - `forge-server/.../generator/manager/coderule/LegacyCodeRuleParser.java` — 旧 `${...}` 模板转分段。
  - `forge-server/.../generator/src/test/java/com/mdframe/forge/plugin/generator/manager/coderule/CodeRuleEngineTest.java` — P0 规则测试。
- **关键签名**：
  ```java
  CodeRulePreviewVO preview(CodeRuleDefinition definition, Map<String, Object> fields, long sampleSequence);
  CodeRuleGenerateVO generate(CodeRuleDefinition definition, Map<String, Object> fields);
  List<CodeRuleSegmentDTO> parse(String template, String resetPolicy, Integer seqLength);
  ```
- **验收**：进制边界、变量缺失、系统变量防伪造、分组摘要、周期 key、溢出和 legacy 模板均有单测。

## Task 5: 重构服务编排并新增独立管理接口

- [x] 已完成
- **目标**：主表和分段事务一致，提供 POST-safe 管理接口及独立权限。
- **涉及文件**：
  - `forge-server/.../generator/service/businessapp/CodeRuleService.java` — CRUD、分段物化、预览和生成 Facade。
  - `forge-server/.../generator/controller/SystemCodeRuleController.java` — `/system/code-rule` 管理接口。
  - `forge-server/.../generator/controller/CodeRuleController.java` — 旧接口兼容委托。
  - `forge-server/.../generator/dto/businessapp/CodeRuleGenerateDTO.java` — 增加 `fields` 并保留 `context` alias。
  - `forge-server/.../generator/dto/businessapp/CodeRulePreviewDTO.java` — 支持未保存 segments。
- **关键签名**：
  ```java
  public CodeRuleDetailVO detail(Long id);
  public Long create(CodeRuleSaveDTO dto);
  public void update(CodeRuleSaveDTO dto);
  public CodeRuleGenerateVO generate(String ruleCode, Map<String, Object> fields);
  ```
- **验收**：内置规则字段白名单、乐观锁、事务回滚、权限和加解密注解符合 Spec。

## Task 6: 实现前端分段编辑器和规则工作台

- [x] 已完成
- **目标**：保留 AiCrudPage 列表能力，使用同路由全屏工作台完成结构化规则配置。
- **涉及文件**：
  - `forge-admin-ui/src/api/business-app.js` — 增加 `/system/code-rule` API 并保留旧函数。
  - `forge-admin-ui/src/views/app-center/code-rule-utils.js` — 分段默认值、排序、校验和预览 payload 纯函数。
  - `forge-admin-ui/src/views/app-center/__tests__/code-rule-utils.spec.js` — 前端规则联动测试。
  - `forge-admin-ui/src/views/app-center/components/CodeRuleSegmentEditor.vue` — 分段列表、拖拽、行内核心配置和高级配置。
  - `forge-admin-ui/src/views/app-center/components/CodeRuleEditorWorkspace.vue` — 全屏工作台、基础信息、分段设计和粘性预览。
  - `forge-admin-ui/src/views/app-center/code-rules.vue` — AiCrudPage 列表、自定义工具栏和操作接入。
- **关键签名**：
  ```js
  export function createCodeRuleSegment(type, order) {}
  export function normalizeCodeRuleSegments(segments) {}
  export function validateCodeRuleDraft(draft) {}
  ```
- **验收**：新增、编辑、排序、类型联动、预览防抖、内置规则限制和主题适配可用。

## Task 7: 对接低代码自动编号兼容链路

- [x] 已完成
- **目标**：低代码字段继续按 ruleCode 生成，并能向 VARIABLE/分组段传递当前业务字段。
- **涉及文件**：
  - `forge-server/.../generator/service/DynamicCrudService.java` — `buildCodeRuleContext` 改为可信元数据和业务 fields 分层。
  - `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue` — 展示结构化摘要并按新预览协议传 sample fields。
  - `forge-server/.../generator/src/test/java/com/mdframe/forge/plugin/generator/service/DynamicCrudServiceAutoGenerationTest.java` — 增加 VARIABLE 分组上下文回归。
- **验收**：现有 `generation.ruleCode` 无需迁移；新增业务记录自动编号、变量分组和旧规则预览保持可用。

## Task 8: 增量验证、文档回填与两阶段自审

- [x] 已完成
- **目标**：执行 `test-spec.md` 的最小验证矩阵并形成可追溯证据。
- **涉及文件**：
  - `code-copilot/changes/编码规则配置优化/spec.md`
  - `code-copilot/changes/编码规则配置优化/tasks.md`
  - `code-copilot/changes/编码规则配置优化/test-spec.md`
  - `code-copilot/changes/编码规则配置优化/execution-log.md`
- **命令**：
  ```bash
  JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
  PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
  mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-id,forge-framework/forge-plugin-parent/forge-plugin-generator -am test
  ```
  ```bash
  source ~/.nvm/nvm.sh && nvm use v20.19.0
  pnpm exec vitest run src/views/app-center/__tests__/code-rule-utils.spec.js
  NODE_OPTIONS=--max-old-space-size=8192 pnpm build
  ```
  ```bash
  xmllint --noout <本轮 Mapper XML>
  rg -n '\$\{[^}]+\}' forge-server/db/migration
  git diff --check
  ```
- **验收**：失败、警告、跳过项和未启动服务均记录；Spec 合规和代码质量审查均无阻塞项。

## 完成摘要

- 后端定向测试：`SegmentSequenceGeneratorTest` 6/6，Generator 编码规则相关测试 15/15。
- 前端纯函数：Vitest 9/9；生产构建成功，8691 modules。
- 聚合验证：Admin reactor 42/42 编译成功；三个 Mapper XML、Flyway placeholder 和 tracked/untracked 差异格式检查均通过。
- 环境验收：未启动 Admin/MySQL/Redis，未实跑 Flyway 和 HTTP；原因及后续检查项已记录在 `execution-log.md`。

## Task 9: 适用场景字典与低代码字段强绑定

- [x] 已完成
- **目标**：适用场景不再自由输入；VARIABLE 只能从指定低代码业务对象的启用业务字段选择；低代码字段配置和运行时均校验对象边界。
- **实现范围**：
  - `ai_code_rule` 增加 `source_object_id/source_object_code`，迁移 `sys_code_rule_scene` 字典。
  - capability 接口按 `sourceObjectId` 返回业务对象和字段选项。
  - 分段“顺序”由拖拽顺序表达，SEQ 名称明确为“流水号（顺序递增）”，不新增重复的顺序段类型。
  - ForgePropertyPanel 传当前 `objectCode`，列表只返回通用规则或当前对象绑定规则。
  - 运行时生成校验规则绑定对象与当前低代码对象一致。
- **验收**：Generator 定向测试、前端 ESLint/Vitest/build、Mapper/Flyway 静态检查和 Admin 聚合编译通过。

## Review 发现（2026-07-16）

- [x] 完成阶段一 Spec Compliance Review；结论 FAIL，按流程未进入正式 Code Quality Review。
- [x] R1：迁移或兼容续接旧 `code-rule:*` 计数器，确保升级后的新 key 不从历史已用区间重新取号。
- [x] R2：号段分配基于本次乐观锁更新前的快照计算本实例区间，禁止 UPDATE 后重查最新 `max_id` 决定返回值。
- [x] R3：号段数据库分配使用独立提交边界，业务事务回滚只能产生空洞，不能回退已缓存号段的数据库水位。
- [x] R4：分类回填改为按当前数据状态幂等执行，不依赖“本轮刚新增字段”的会话变量。
- [x] 补充旧计数器续接、多实例竞争、`REQUIRES_NEW` 事务契约和前端异步过期响应测试。
- [x] 用户验收复修：移除编码规则页面错误的 Naive 组件内部背景变量，统一使用 Forge 系统主题 Token，并通过定向 ESLint、Vitest 和生产构建。
- [x] 用户验收复修：新增/编辑由抽屉改为同路由全屏工作台，分段操作列固定在右侧；无需新增菜单路由或放开权限守卫。
- [ ] 重新执行 `/review 编码规则配置优化`；本项属于 Fix 后下一阶段，不在本轮 `/fix` 内提前标记通过。
