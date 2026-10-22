# 测试计划 — 低代码应用集成规则引擎

> status: propose
> created: 2026-10-22（Asia/Shanghai）
> 本文件是未来实施的验证计划；本轮只有文档检查，不代表下面测试已经运行。

## 0. 基线与执行原则

- 先读取 [spec.md](spec.md)、[tasks.md](tasks.md)、[execution-log.md](execution-log.md) 和仓库自动化测试标准，再按任务差异增量验证，不每轮从零规划。
- 当前基线 main / 8d4031cb1，Java 17、Boot 4.1.1、Flowable 8；LiteFlow 依赖尚未加入，确切版本和 API 由 T0 锁定。
- 历史流程列表变更有 2026-10-20 的 59 文件/485 项前端及 18 项后端记录，见 `../flow-model-catalog-polish/execution-log.md`。这些只覆盖当时范围，不是当前规则引擎或整个最新 main 的通过证明。
- 本轮不运行 Maven/Vitest/真实服务，不安装依赖或执行迁移。T0 开始时补现有相关测试的实际基线和版本；不预填通过数、覆盖率或运行时长。
- 实施时沿用项目实际 JUnit Jupiter/Mockito、Vitest/Vue Test Utils；版本以锁定 POM/package/lockfile 为准，不凭旧模板填写。
- 新行为先有效 Red，再 Green；不得删除测试、放宽安全断言或仅制造语法错误来充当 Red。
- 纯策略/编译/状态机新增分支目标覆盖率不少于 90%，所有 P0 场景必测；覆盖率数字必须用实际报告证明，不能用整体平均值掩盖安全分支。

## 1. 验证层级

| 层级 | 必须证明的内容 | 不能替代什么 |
|---|---|---|
| 单元/纯函数 | 结构、类型、计算、图、摘要、映射、策略、状态转换 | 不替代 Boot 装配、数据库与真正鉴权 |
| Mapper/内存库 | XML/参数/租户过滤、DDL 基本语法、幂等结构 | 不替代 MySQL 8 方言、锁和 Flyway 历史 |
| 单机集成 | 实际引擎注册/编译/运行/试算隔离、接口绑定/加密适配 | 不替代多实例一致性与生产数据源 |
| UI 真实组件 | 布局、滚动、交互、错误反馈、真实接口适配 | 合成请求适配器不替代后端 E2E |
| 用户真实环境 | MySQL/Flyway、Sa-Token/加密、多实例、应用回退/生成工程、审批回归 | 无用户授权不得自行执行 |

## 2. P0：兼容、协议、身份与运行

| ID | 场景/输入 | 预期与拒绝行为 | 需求 | 阶段 |
|---|---|---|---|---|
| C01 | 同一 Forge Boot/JDK，插件未装、已装关闭、启用、连接失败 | 无旧 Boot/Agent 强制依赖；关闭不初始化第三方；启用失败不伪造可用 | R19/R21/R24 | T0/T2 |
| C02 | 两个请求、两条同类型不同参数节点、同名草稿与已发布规则 | 输入/配置/输出不串；临时注册/卸载不影响正式链；作用域 finally 清理 | R04/R09/R14 | T0/T1 |
| C03 | 实际 RulePublisher 创建/CAS 更新/重复创建/错误 EL | 记录确切异常/API/DDL；存储成功不冒充语义校验；不关调用方 DataSource | R10/R12/R19 | T0/T2 |
| C04 | 未知 schema/节点、缺入口、重复 ID、多余出口、未知配置键 | 在编译前失败关闭，错误含字段与节点位置，不回退默认实现 | R02/R03/R04 | T1 |
| C05 | 缺失/null、文本真假、非法日期、超过 JS 精度整数 | 严格按声明类型校验，无隐式转换或 Long 精度丢失 | R05 | T1 |
| C06 | MONEY 大数/负数边界/溢出/除零、NUMBER 舍入 | 分单位精确；错误明确；JSON→Java→JSON 无浮点/精度丢失 | R03/R05 | T1 |
| C07 | 回边、不可达节点、无输出路径、动态子规则、深度超限 | 结构/依赖环与资源超限被拒，未知图不能自动改语义 | R03/R10/R11/R14 | T1/T2 |
| C08 | 插件伪装 PURE 但未受信任、DB_WRITE/REMOTE_WRITE/script/sql/url | 效果按服务端注册与实际适配判断，拒绝不支持能力，不执行 | R04/R08/R23 | T1/T2 |
| C09 | 同一规则字段顺序变化、真实配置变化、手工提交 EL | 规范化稳定；实际变化改 hash；用户 EL 不被作为权威源执行 | R02/R10 | T1 |
| C10 | 条件边界与 AND/OR/null；决策表多命中/无命中/默认行 | 规则顺序与首条命中确定，缺默认行发布失败 | R03/R05 | T1 |
| C11 | OUTPUT 缺字段/多字段/类型错；QUERY_SOURCE 类型与依赖 | 输出必须符合 Schema，不把未声明字段/敏感查询结果外传 | R03/R05/R23 | T1/T2 |
| C12 | 草稿校验/试算，在正式链同名情况下反复调用 | 无业务写入；正式产物/hash/状态不变；临时资源有界回收 | R09/R14 | T1 |
| C13 | 总 3s 预算、查询慢、线程未响应中断、超大结果 | 返回超时/限流；迟到结果不写输出/终态；占用额度不提早释放 | R14/R23 | T1/T2 |
| C14 | 并发过载、队列满、重复超时、注册节点数持续增长 | 有界拒绝，无无限排队/全局组件泄漏；指标低基数 | R14/R19 | T0/T2 |
| C15 | 新/旧库执行迁移、重复执行、逻辑删除/唯一索引 | 所有 Forge 字段一致；原生表 DDL 与锁定版本一致；迁移重号/占位符为空 | R01/R10/R13/R20 | T1/T2/UAT |
| C16 | 同租户两个应用同编码、跨租户 ID、缺会话/停用应用 | 业务唯一键隔离；读/改/发布/试算/日志全部范围校验，不默认租户/管理员 | R01/R08 | T1 |
| C17 | 双人保存同 revision/hash；删除再建同编码；版本删除后复用 | CAS 冲突不覆盖；有效定义可按墓碑策略重建；历史版本号不复用 | R02/R10/R20 | T1/T2 |
| C18 | 同键同指纹/不同输入/不同版本；两个并发预留 | 仅一个运行/发布认领；同内容复用，不同内容 409；RUNNING 明确处理中 | R10/R12/R13 | T1/T2 |
| C19 | 已成功结果有效/过期/密文不可读、运行记录超期清理 | 只复用原授权结果；过期明确拒绝、不重算；去重窗口与清理行为一致 | R13/R15 | T1/T2 |
| C20 | 只有规则编辑权、无记录权；假 tenant/user/org/数据源字段 | 身份来自可信入口；受保护字段被拒/隔离；查询继续受字段/行级权限控制 | R01/R08/R23 | T1/T2 |
| C21 | 客户端伪造应用/规则版本/chainId、共享对象同名规则、缺当前应用、改最新指针 | 服务端查应用发布投影再固定引用；缺 scope 的规则动作拒绝，不能从对象/规则猜应用；旧无规则请求兼容 | R01/R06/R08/R11/R21 | T1/T2 |
| C22 | DTO 空/超长/超额、无权限、page 参数、加密请求 | 注解校验生效；pageNum/pageSize 正确；原响应/加密协议不改 | R05/R08/R16 | T1 |
| C23 | 输出/错误中夹 PII、Token、JDBC URL、EL 和异常堆栈 | 日志与异常只展示安全摘要/错误码；运行结果缓存是受限密文，不从日志泄露 | R08/R15 | T1/T2 |
| C24 | 单机 LOCAL_PREVIEW 尚未完成正式 Rule-DB/应用固定版本 | 试算和测试可验证；正式应用发布明确阻断，不显示生产已就绪 | R09/R10/R11/R19 | T1 |
| C25 | 已超时运行稍后成功、重复完成回调、guard 之后失败 | CAS 不可覆盖终态；引擎成功不因 guard 改为失败，父步骤正确记录拒绝 | R07/R13/R14/R15 | T1/T2 |
| C26 | 有/无步骤贡献器，混用旧步骤，显式 LOCAL_TRANSACTION | 未装规则时不认新步骤；旧模式不变；规则步骤不能偷进本地事务白名单 | R06/R19/R21 | T1 |
| C27 | 扩展为空、多个贡献器重复标识、原步骤失败策略 | 无规则原行为/hash 不变；重复注册报错；不静默跳过未知步骤 | R06/R19/R21 | T1/T2 |
| C28 | 正常输出 false；配置/不配置 guard；映射半途失败 | 无 guard 则正常；guard 拒绝无后续步骤；映射成功前不部分污染上下文 | R05/R06/R07 | T1 |
| C29 | 试图把 EXECUTE_RULE 发布成开放平台/AI/MCP 受控动作 | 现有白名单仍拒绝；无新客户端授权、代用户提权或裸链接口 | R08/R22 | T1/T2 |

## 3. P0/P1：UI、发布、多节点和交付

下面 P1 主要为呈现体验；版本/权限/交付正确性项仍是 P0，不因放在此表降低优先级。

| ID | 场景/输入 | 预期与证据 | 需求 | 阶段 |
|---|---|---|---|---|
| C30 | 应用规则入口、列表、筛选、分页、空/无匹配/错误/重试 | 当前应用清晰，只有真实状态；插件未装不露死入口；请求竞态不回写 | R01/R16/R19 | T1/T2 |
| C31 | 编辑/试算/版本多个面板，修改后取消、保存冲突 | Pinia 一份草稿，取消不写入、dirty 提示、冲突不覆盖 | R02/R09/R16 | T1/T2 |
| C32 | 只装底座、加规则插件、再加低代码桥接；卸载有引用插件 | 依赖无环，Flow 不引入 generator；缺桥接拒绝相应功能，活跃引用阻断卸载 | R19/R20/R21 | T0/T2 |
| C33 | 明/暗、桌面/390/320px，长字段、日志、图与属性滚动 | 系统组件/样式统一，无按钮遮挡与横向溢出；截图为真实组件并标模拟边界 | R16/R24 | T1/T2 |
| C34 | 两实例分别冷/热缓存、落后目录、丢缓存、规则库断连 | 每次命中精确版本/hash/组件协议；无法满足时 RULE_NOT_READY/不可用，不旧链兜底 | R11/R12/R19 | T2/UAT |
| C35 | 发布每一步崩溃、Publisher 超时但实际成功、租约过期 | 恢复原 release/manifest；相同内容幂等继续；冲突不覆盖；原应用仍能运行 | R10/R12/R13 | T2 |
| C36 | 子规则先/后发布、依赖只发布一半、删除依赖 | 未形成闭包不能激活；精确 manifest 可冷编译；无跨 bundle 原子假承诺 | R10/R11/R12 | T2 |
| C37 | 应用 A 固定 v1，规则出 v2，应用 B/新版本固定 v2，组件协议升级 | A 仍跑 v1；不兼容组件拒绝，不“同 EL 就永远兼容”；重试不换新版 | R04/R11/R21 | T2 |
| C38 | 应用对象动作与快照引用不一致；发布失败再恢复 | 一致性检查阻断；恢复固定原选中版本；不悄悄替换新草稿 | R10/R11/R21 | T2 |
| C39 | 页面按钮/事件/定时重投，原 actor 缺失、绑定无应用版本、表单未保存字段 | 从已发布绑定而非事件载荷解析应用版本；复用父身份/幂等；缺 scope/身份失败关闭；form 与 record 不混淆 | R06/R08/R11/R13 | T1/T2 |
| C40 | 受控只读源跨应用、越权字段/行、已停用/未发布、超过 100 行 | 通过平台只读查询与剩余预算，结果限流；自由 SQL/URL/写查询拒绝 | R05/R08/R14/R23 | T2 |
| C41 | 导出应用及子规则、源环境 chain/tenant/secret | 闭包完整且无内部运行 ID/凭据/源权限；已有无规则包不变 | R11/R17/R21 | T2 |
| C42 | 导入新租户/应用、ID 冲突、缺节点/引擎、篡改 EL | 先预检再重映射/编译；不能信任包内 EL；半成功可追溯，不自动激活 | R08/R10/R17 | T2 |
| C43 | 应用级、访问入口级、configKey 级源码下载 | 共用贡献器；包含可构建依赖、规则/调用/说明，不覆盖用户扩展；普通 Java 后端保留 | R18/R21 | T2 |
| C44 | 下载含规则应用到干净目录，实际构建和运行示例 | 编译成功且结果与在线固定规则一致；未支持模式明确拒绝，不能只测 ZIP 名称 | R05/R18/R24 | T2/UAT |
| C45 | 停用影响确认过期、活跃应用/过程引用、并发新调用 | 权限/引用/CAS 审查；新调用拒绝，历史保留，不声称撤回已完成副作用 | R08/R20 | T2 |
| C46 | 选旧默认版本、回退应用发布、逻辑删定义/清理运行记录 | 默认值只影响新引用；回退建新应用版本；旧正文/序号不倒退，窗口语义明确 | R11/R13/R20 | T2 |
| C47 | 指标/健康/节点加载信息，未执行过的规则或未采集实例 | 不伪造 100%/全部 ready；维度低基数，无 runId/tenant/输入作为无限指标标签 | R14/R15/R19/R24 | T2 |

## 4. 测试组织与样例

路径缩写与 Tasks §0 一致，下表是拟新增测试，不是已存在文件：

| 文件/目录 | 主要覆盖 |
|---|---|
| `CHANGE/verification/compatibility/src/test/java/com/mdframe/forge/verification/` | C01～C03，实际依赖/引擎行为 |
| `ST/schema/RuleSchemaContractTest.java`、`ST/validation/RuleGraphValidatorTest.java` | C04～C08，错误图/DTO/协议 |
| `ST/compiler/RuleElCompilerTest.java`、`ST/node/RulePureNodesTest.java` | C05～C11，规范化/类型/计算 |
| `ST/runtime/LiteflowRuleRuntimeTest.java` | C02/C12～C14，隔离/预算/泄漏 |
| `PT/mapper/RuleMapperContractTest.java`、`PT/migration/RuleMigrationTest.java` | C15～C19，SQL 和内存库；MySQL 另列 |
| `PT/security/RuleAccessPolicyTest.java`、`PT/controller/RuleControllerContractTest.java` | C16/C20～C23，权限/DTO/响应 |
| `PT/service/RuleInvocationServiceTest.java`、`RuleLifecycleTest.java`（同目录） | C18/C19/C25/C45/C46，指纹/终态/生命周期 |
| `LT/action/ExecuteRuleActionStepExecutorTest.java`、`GT/service/businessapp/BusinessActionRuleExtensionTest.java` | C21/C24/C26～C29/C39，兼容和适配 |
| `PT/service/RulePublishRecoveryTest.java`、`PT/dependency/RulePinnedVersionTest.java` | C34～C38 发布/闭包；多进程实测另列 |
| `LT/query/LowcodeRuleQueryProviderTest.java`、`LT/bundle/RuleApplicationBundleTest.java`、`LT/codegen/RuleStaticCodegenTest.java` | C40～C44，读范围/包/生成 |
| `U/stores/rule/__tests__/ruleDesignerStore.spec.js`、`U/views/app-center/rules/__tests__/` | C30/C31/C33，响应式/交互/样式边界 |

必须准备以下合成样例，不提交真实业务数据：

1. Spec §5.3 的订单分单位计算：输入 amountCent="100000"、shippingCent="2000"，输出 totalCent="102000"、requiresReview=true。
2. 同输入但 shippingCent="0"，总额等于阈值，输出 requiresReview=false；覆盖 GT 的等号边界。
3. 两个应用同 ruleCode、两个租户同应用编码、两个不同 actor/组织/记录权限，全部用虚构 ID。
4. v1/v2 与子规则依赖清单；同节点类型多次出现且参数不同。
5. 无输出/环/未知节点/非法 AST/金额溢出/超大载荷/坏 Schema/恶意字段路径的拒绝样例。
6. 发布每个阶段的中断点、密文结果过期、迟到完成、查询源权限/超时样例。
7. 含规则与无规则应用包、被篡改包、缺组件包、普通静态生成工程。

样例 JSON 在 T1 对应任务中转成仓库测试资源；本轮只校验文档 JSON 的可解析性，不声称业务实现能解析或执行它。

## 5. 实施后的命令计划（本轮未执行）

### 5.1 T0

在 `CHANGE/verification/compatibility` 创建完兼容工程并获得实施授权后，使用机器上实际 Java 17/Maven：

```bash
mvn -B -ntp dependency:tree
mvn -B -ntp help:effective-pom
mvn -B -ntp test
```

兼容工程不得配置默认连接用户现有 dev/test/生产数据库。Rule-DB 实测使用用户明确允许的独立测试 schema/容器，DDL 和清理目标先核验。

### 5.2 后端

新模块真实创建后，在 `forge-server` 运行；profile 必须显式开启测试：

```bash
mvn -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-rule-lowcode -am compile -DskipTests
mvn -B -ntp -pl forge-framework/forge-plugin-parent/forge-plugin-rule-lowcode -am test -Penable-tests \
  '-Dtest=Rule*Test,Liteflow*Test,ExecuteRule*Test,BusinessAction*Test,BusinessProcess*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false
```

测试后核对目标模块实际执行数量大于 0，不能因为上游无指定测试而把空运行当成功。共享动作/应用发布 SPI 改动还需回归原动作/流程/导入导出/打印/发布测试。
按实际选择安装后的宿主 POM，在独立验证目录构建 `forge-admin-server`；Flow 主应用也需确认没有管理端桥接依赖。发布 package 默认跳过测试不替代此前证据。

### 5.3 前端

在 `forge-admin-ui`，使用当次已安装且符合工程要求的 Node（当前偏好 v20.19.0；如现有构建工具明确要求更高版本，记录实际版本与原因）：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm --ignore-workspace exec vitest run src/stores/rule src/views/app-center/rules \
  --maxWorkers=2 --minWorkers=1
pnpm --ignore-workspace exec eslint src/api/rule.js src/stores/rule src/views/app-center/rules
NODE_OPTIONS=--max-old-space-size=8192 pnpm --ignore-workspace build
```

如包管理器/锁文件工具版本与命令不符，复用工程已装工具并记录，不修改无关 lockfile 或全局安装来凑通过。
UI 至少桌面与 390/320px、深浅色、真实组件交互截图；任何合成请求明确标记为模拟，不把其返回成功当真实发布。

### 5.4 迁移/静态检查

```bash
git diff --check
ls forge-server/db/migration/V*.sql | sed 's/__.*//' | sort | uniq -d
rg -n '\$\{[^}]+\}' forge-server/db/migration
```

修改 SQL 时核对全部目录输出与本轮脚本，不能因为已有问题就忽略新增错误。正式迁移编号根据目录与真实 schema history 再确定，不复用本文基线编号。
MySQL 8 验证含新/旧库、重复执行、JSqlParser 改写后的锁/分页、结果密文/TTL 与权限数据；H2 不替代这些测试。

## 6. 真实环境门禁与跳过项

只有用户明确允许后才执行：

- 指定测试库迁移、真实 Admin/Flow 启动、Sa-Token/正常加密请求；不能仅靠 MockMVC 或 X-Inner-Call 宣称真实鉴权已覆盖。
- 至少两个独立执行实例、冷/热缓存、发布中断、服务重启和版本固定；不能用同一 JVM 两个 Bean 冒充多实例测试。
- 从按钮/事件/定时实际触发动作，核对结果/运行表；若在 ORCHESTRATION 中后步骤失败，不宣称前步骤全回滚。
- 原有采购/低代码审批：发起、字段可写权限、审批、驳回、重提、只读历史、取消/重复回调，保持现有行为。
- 普通生成工程构建/执行及应用包新环境导入；不自动部署服务器或替换既有应用。

当前全部未执行，原因是本轮只授权生成文档，不是环境缺失或功能测试失败。后续执行记录必须给出命令、输出、版本、跳过项及本轮进程清理信息。

## 7. 追溯、证据与完成标准

- 每个 Tasks 条目完成时标注对应 C 用例、实际测试文件、命令和报告路径；不只写“完成”。
- 一个 R 的相关 P0 用例全通过才可标实现验收通过；所有 R01～R24 至少对应本计划一个 C 用例。
- 应用关键版本/权限/金额/停用变更需人工复核，覆盖率不能代替评审。
- T1 只报告单机最小闭环；首个生产交付必须满足 T0/T1/T2 与用户环境门禁。T3 不自动纳入。
- 本次 D00 只运行文档级检查：四文件齐全、Markdown JSON、R/C/任务引用、相对文档链接、whitespace、只新增本目录且原本地 diff 不变。
- 执行证据写 [execution-log.md](execution-log.md)；本轮不填造 JUnit/Vitest/SQL/端到端通过结果。
