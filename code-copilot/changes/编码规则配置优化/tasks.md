# 任务拆分 — 编码规则配置优化
> status: reviewed_passed
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

## Task 10: VARIABLE 自定义变量与低代码映射双来源

- [x] 已完成
- **目标**：解除 VARIABLE 对低代码业务对象的全局强绑定，同时保留 LOWCODE 字段的设计态和运行时边界。
- **数据协议**：
  ```text
  variableSource = CUSTOM | LOWCODE
  CUSTOM  -> segmentValue=调用方变量名，生成时从 fields 取值
  LOWCODE -> segmentValue=低代码字段编码，规则主表保存来源对象
  ```
- **Red**：
  - [x] `code-rule-utils.spec.js` 先覆盖 CUSTOM 无对象合法、LOWCODE 无对象失败、切换来源清理旧值。
  - [x] `SystemCodeRuleControllerContractTest` 先覆盖只收集 LOWCODE 字段和 CUSTOM 安全变量名。
  - [x] `LegacyCodeRuleParserTest` 与 `CodeRuleMigrationContractTest` 先覆盖缺省 CUSTOM、字段迁移及回填。
- **Green 后端**：
  - [x] 修改 `CodeRuleSegmentDTO`、`AiCodeRuleSegment`、`CodeRuleSegmentMapper.xml`、`CodeRuleService`，完成 `variableSource` 持久化、详情回显、等价指纹和 legacy 默认。
  - [x] 修改 `SystemCodeRuleController`，只校验 LOWCODE 字段目录；纯 CUSTOM 规则清空来源对象。
  - [x] 修改 `V1.0.36__add_structured_code_rule_segments.sql`，用 `information_schema` 防重增加 `variable_source`，已绑定对象的存量 VARIABLE 回填 LOWCODE。
- **Green 前端**：
  - [x] 修改 `code-rule-utils.js`，VARIABLE 默认 CUSTOM，分类校验并在切换来源时清空不兼容值。
  - [x] 修改 `CodeRuleSegmentEditor.vue`，在 VARIABLE 行内选择“自定义变量 / 低代码字段”；只存在 LOWCODE 分段时在分段区展示来源对象。
  - [x] 修改 `CodeRuleEditorWorkspace.vue`，基础信息不再固定展示低代码来源，切换对象只清理 LOWCODE 字段。
- **验证**：Generator 定向测试、相关 Vue/JS ESLint、Vitest、前端生产构建、Admin 聚合编译、Mapper XML/Flyway/差异格式静态检查。

## Review 发现（2026-07-16）

- [x] 完成阶段一 Spec Compliance Review；结论 FAIL，按流程未进入正式 Code Quality Review。
- [x] R1：迁移或兼容续接旧 `code-rule:*` 计数器，确保升级后的新 key 不从历史已用区间重新取号。
- [x] R2：号段分配基于本次乐观锁更新前的快照计算本实例区间，禁止 UPDATE 后重查最新 `max_id` 决定返回值。
- [x] R3：号段数据库分配使用独立提交边界，业务事务回滚只能产生空洞，不能回退已缓存号段的数据库水位。
- [x] R4：分类回填改为按当前数据状态幂等执行，不依赖“本轮刚新增字段”的会话变量。
- [x] 补充旧计数器续接、多实例竞争、`REQUIRES_NEW` 事务契约和前端异步过期响应测试。
- [x] 用户验收复修：移除编码规则页面错误的 Naive 组件内部背景变量，统一使用 Forge 系统主题 Token，并通过定向 ESLint、Vitest 和生产构建。
- [x] 用户验收复修：新增/编辑由抽屉改为同路由全屏工作台，分段操作列固定在右侧；无需新增菜单路由或放开权限守卫。
- [x] 重新执行 `/review 编码规则配置优化`；阶段一 PASS，阶段二 PASS_WITH_COMMENTS，P0/P1 为零。

## Task 11: LOWCODE 分段弹窗映射

- [x] 已完成
- **目标**：从分段顶部移除全局映射区，用当前 VARIABLE 行触发的弹窗完成业务对象和字段选择。
- **文件**：
  - `forge-admin-ui/src/views/app-center/code-rule-utils.js`：提供可测试的映射原子应用函数。
  - `forge-admin-ui/src/views/app-center/__tests__/code-rule-utils.spec.js`：覆盖同对象保留、切换对象清理和 CUSTOM 不受影响。
  - `forge-admin-ui/src/views/app-center/components/CodeRuleSegmentEditor.vue`：移除顶部映射区，LOWCODE 行显示摘要/重新映射入口。
  - `forge-admin-ui/src/views/app-center/components/CodeRuleEditorWorkspace.vue`：管理 Modal 映射草稿、对象字段加载和确认提交。
- **核心契约**：
  ```js
  applyLowCodeVariableMapping(segments, targetSegmentKey, {
    sourceObjectId,
    fieldCode,
  }, currentSourceObjectId)
  // => { segments, sourceObjectId, clearedSegmentKeys, objectChanged }
  ```
- **Red/Green**：
  - [x] 先增加纯函数用例并确认旧实现因缺少契约而失败。
  - [x] 实现最小原子应用函数，同对象时只更新目标分段，切换对象时清空其它 LOWCODE 映射。
  - [x] 选择 LOWCODE 时只打开弹窗，取消时不修改 `variableSource/segmentValue/sourceObjectId`。
  - [x] 确认后一次性应用对象与字段，对象改变时在弹窗中显示其它映射将被清理的提示。
- **验证**：Vitest、两个 Vue 组件与工具函数 ESLint、前端生产构建、`git diff --check`。

## Task 12: 旧号段水位与固定宽度组合兼容

- [x] 已完成
- **目标**：修复归档前 Review 发现的“旧号段已预分配到 1000，但历史三位流水迁移后从 1001 续接立即溢出”问题。
- **Red**：
  - [x] `CodeRuleEngineTest` 增加旧安全起点 1001 + DECIMAL 三位的组合用例；旧实现因缺少只读旧起点契约而测试编译失败。
- **Green**：
  - [x] `ISequenceService` 增加不消耗序列的 `resolveLegacyStartValue` 默认契约，`SequenceServiceImpl` 委托号段生成器实现。
  - [x] `CodeRuleEngine` 只在严格宽度实际溢出时解析旧起点，并按旧起点所需的最小进制位数重试。
  - [x] 没有旧水位的新规则继续溢出失败；旧起点 1001 只兼容到四位，实际值达到 10000 时仍失败。
- **验证**：Starter ID 7/7、Generator 编码规则 21/21、Admin reactor 42/42、`git diff --check`。

## Task 13: 归档前两阶段复审安全与质量修复

- [x] 已完成
- **目标**：关闭最终 Review 发现的计数器重置、多实例可见性、缓存增长和旧协议兼容风险。
- **Red/Green**：
  - [x] 兼容宽度缓存：连续两次历史扩宽/新规则溢出从查询 2 次降为 1 次，并缓存负结果。
  - [x] 高基数号段缓存：由永久 Map 改为 Caffeine `maximumSize=10000 + expireAfterAccess=60m`；淘汰后继续取号为 1001，不重复。
  - [x] 多实例事务：号段分配显式 `REQUIRES_NEW + READ_COMMITTED`，测试记录并断言隔离级别。
  - [x] legacy 语义：AUTO + 单独 `HHmmss` 保持旧 NONE/all，旧 all 水位从 1001 续接。
  - [x] 字段别名：VARIABLE 恢复 exact → snake/camel 别名，SYS_VAR 不使用业务别名。
  - [x] 计数器身份：ruleCode 跨删除记录永久唯一；已有 SEQ 更新必须保留同一 `segmentKey`。
- **最终 Review**：Spec Compliance PASS；Code Quality PASS_WITH_COMMENTS，P0/P1 为零。
- **验证**：Starter ID 9/9、Generator 编码规则 25/25、Admin reactor 42/42、XML/Flyway/差异静态检查。

## Task 14: SDD 归档与知识沉淀

- [x] 已完成
- **完成日期**：2026-07-17
- [x] Spec、Tasks、Test Spec、Execution Log 状态统一为 `done`。
- [x] 最终 Review 结论和环境验收保留项已回填。
- [x] 可复用的计数器身份决策及旧水位/缓存踩坑已写入长期记忆。
- [x] 四份 SDD 文档、`功能需求文档.md` 和 `参考UI设计.png` 一并归档到 `code-copilot/changes/archive/2026-07-17-编码规则配置优化/`。

## Task 15: 容量感知号段与无锁消费热路径

- [x] **Red**：在 `SegmentSequenceGeneratorTest` 增加“有限容量生成一次 → 缓存淘汰 → 下一次为 2”以及“达到上限不推进数据库水位”用例。
- [x] **接口**：扩展 `ISequenceService`、`SequenceServiceImpl` 和 `SegmentSequenceGenerator`，新增有限容量取号契约：
  ```java
  long nextId(String bizKey, long startValue, String legacyKeyPrefix,
              String legacyPeriod, int allocationStep, long maxValue);
  ```
- [x] **实现**：新规则根据进制容量计算 `allocationStep=clamp(capacity/1000, 1, 1000)`；分配前按剩余容量裁剪步长，容量耗尽时不再 UPDATE。
- [x] **并发**：`SegmentHolder` 使用 CAS 消费当前段，仅在当前段耗尽时同步加载新段。
- [x] **验证**：Starter ID 定向测试 Green，现有跨段、多实例、缓存淘汰和事务隔离用例保持通过。

## Task 16: 关闭原始序列接口默认暴露

- [x] **Red**：新增 `SequenceControllerContractTest`，断言 `matchIfMissing=false`、写操作使用 POST、四个入口均要求 `system:sequence:use`。
- [x] **实现**：修改 `SequenceController`，默认不注册 HTTP API；增加专用权限和 `bizKey` 长度/安全字符校验。
- [x] **验证**：Starter ID 编译与契约测试通过；不新增默认菜单或角色授权。

## Task 17: legacy 边界与索引友好查询

- [x] **Red**：扩展 Mapper/Flyway 契约测试，断言 legacy 查询使用转义后的左前缀 LIKE，不再对 `biz_key` 使用 `LEFT(...)`；新建规则不携带 legacy 前缀。
- [x] **迁移**：新增 `V1.0.37__optimize_code_rule_runtime.sql`，为 `ai_code_rule` 增加 `legacy_compat_enabled`；升级前既有规则默认兼容，新代码创建规则显式写 0；重建分段活跃查询索引为 `(tenant_id, rule_id, del_flag, segment_order, id)`。
- [x] **实现**：`AiCodeRule`、Mapper、`CodeRuleDefinition` 和 `CodeRuleService` 传递兼容标记；`SysIdSequenceMapper.xml` 使用 `LIKE ... ESCAPE '!'` 的参数化可索引前缀范围。
- [x] **验证**：Generator migration/Mapper/legacy/engine 契约测试 Green，V1.0.36/V1.0.37 placeholder 扫描无输出。

## Task 18: 规则定义缓存与生成快路径

- [x] **Red**：新增同一 `tenantId + ruleId + versionNo` 连续生成只加载一次分段、版本变化重新加载、缓存返回深拷贝的测试。
- [x] **缓存**：在 Generator 显式引入 Caffeine；缓存不可变分段快照，key 为 `tenantId/ruleId/versionNo`，最大 10000、无访问 30 分钟过期。
- [x] **生成快路径**：真实生成不构造 compatibility template、format expression、分段预览和 warning VO；仍保留字符、长度、租户和变量失败关闭，并在可预判时于取号前拒绝非法输出。
- [x] **输入上限**：一条规则最多 32 个分段，业务上下文字段最多 256 个，单个未声明长度变量最多 96 字符。
- [x] **验证**：Generator 既有回归及新增缓存/上限/快路径测试共 31/31 通过。

## Task 19: 预览请求降载与收尾验证

- [x] **后端**：移除两个实时预览入口的 `@OperationLog`，避免编辑输入产生高频审计写入；生成接口保留日志。
- [x] **前端**：`previewSystemCodeRule` 接收 Axios config，工作台用 `AbortController` 取消已过期预览请求，保留 latest-request guard；取消静默，真实错误仍提示。
- [x] **验证**：前端 Vitest、定向 ESLint、生产构建；Starter ID、Generator、Admin reactor；三个 Mapper XML、Flyway、`git diff --check`。
- [x] **文档**：回填 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`，重新执行两阶段 Review；结论为 PASS_WITH_COMMENTS，当前保持活动目录等待用户决定是否再次归档。
