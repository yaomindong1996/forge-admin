# 低代码应用集成规则引擎（LiteFlow）

> status: propose
> created: 2026-10-22（Asia/Shanghai）
> baseline: main / 8d4031cb1 / Forge 1.2.1 / Java 17 / Spring Boot 4.1.1
> complexity: 复杂（引擎接入较轻，平台治理与交付闭环为主要工作量）

## 0. 当前授权与阅读入口

- 用户先要求“先分析，不用动”，随后明确授权生成完整 Spec、Tasks、测试计划和执行记录。
- **本轮仅新增本目录文档**；不修改业务代码、POM、SQL、菜单或配置，不安装依赖，不启动服务，不操作数据库。文档生成后用户已授权本地提交，未授权 push 或部署。
- 本文是待评审的实施方案，不代表下面列出的模块、接口、表和功能已经存在。
- 任务拆分见 [tasks.md](tasks.md)，验收矩阵见 [test-spec.md](test-spec.md)，真实执行情况见 [execution-log.md](execution-log.md)。
- 当前版本保持 1.2.1；未来实施的日常变更记 Unreleased，正式发版和目标版本另行确认。

## 1. 背景与目标

目标不是再造审批引擎，而是为低代码应用提供可配置、可复用、可测试的业务决策与一次性逻辑计算能力。

例如：订单金额与运费计算、额度校验、规则评分、是否需要复核。规则返回结构化结果，业务动作决定是否继续、持久化或启动审批。

职责保持分离：

| 层 | 保留的职责 | 不应承担的职责 |
|---|---|---|
| 表单与字段规则 | 必填、显隐、简单联动、现有字段公式 | 替代后端业务裁决 |
| Forge 规则能力 | 所有权、协议、权限、版本、发布、试算、追踪 | 接受任意脚本/SQL/内部 chainId |
| LiteFlow | 执行短生命周期的组件化逻辑 | 人工审批、跨天等待、持久化恢复 |
| 现有业务动作/触发器 | 调用入口、幂等、持久化副作用、事件/定时 | 再复制一套规则编译器 |
| Flowable/应用业务流程 | 审批、等待、恢复、业务运行状态 | 被 LiteFlow 整体替换 |

### 1.1 阶段边界

| 阶段 | 目标 | 完成不代表什么 |
|---|---|---|
| T0 | 验证 Boot 4/JDK 17、依赖、上下文、编译/试算隔离 | 不代表已接入业务或可上线 |
| T1 | 单机最小闭环：定义/草稿/版本、纯计算、试算、动作桥接、基础 UI | 不代表多节点/应用分发闭环完成 |
| T2 | 平台上线闭环：Rule-DB、固定版本、权限、受控只读查询、应用发布/回退/导出、监控与回归 | 未经真实环境验收不能称上线 |
| T3（后续） | 应用业务流程节点调用、直接 BPMN 适配、复杂并行、进一步效果类型 | 不纳入本轮首版必做清单 |

首个生产可交付版本必须完成 T0～T2；不可将 T1 演示描述成完整集成。

### 1.2 非目标

- 不迁移或删除现有审批、触发器、业务动作、Aviator 公式和运行实例。
- 不将整张现有应用业务流程/BPMN 画布直接编译成一条 LiteFlow 链。
- 不开放 Groovy/Java/JS 等任意脚本、Java 类名、Spring Bean 名、SQL、自由 URL、Agent 节点。
- 不在规则内直接写业务记录、资金/库存、发通知或启动审批；由已有业务动作承接。
- 不支持无限循环、任意回边、动态规则引用、并行写共享上下文或跨库全局事务。
- 不给 AI/MCP/开放平台自动增加新调用权限，不扩大现有受控动作白名单。
- 不在本轮实现保存前全局拦截器；前置校验先通过明确的提交业务动作组合。所有保存入口强制校验属于独立扩展，不能承诺已覆盖。

## 2. 当前代码现状（Research Findings）

以下相对路径均以仓库根目录为基准。归档提案仅提供历史背景，**现状以 main 源码为准**。

| 编号 | 已核对事实与源码 | 对本方案的约束 |
|---|---|---|
| F01 | `forge-server/pom.xml`：revision=1.2.1、Java 17、Boot 4.1.1、Flowable 8.0.0 | 专用 Boot 4 Starter 与完整依赖组合仍需 T0 实测 |
| F02 | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessActionExecutionService.java`：`executeInternal/buildContext/executePublished/executeSteps` 已有发布快照、权限、幂等、输入投影、日志、两种事务模式 | 保留统一入口，只接新的步骤适配器；类已 999 行，须先按领域拆分再扩展 |
| F03 | 同包 `BusinessActionStepExecutor.java`：`supportType/execute` | 新适配器可以注册，但不代表发布校验、事务策略、导出也已自动支持 |
| F04 | 同包 `BusinessActionCommandPolicy.java`：本地事务类型白名单及危险配置键检查 | 规则不能通过跳过策略或伪装普通类型来执行 |
| F05 | 同包 `BusinessTriggerExecutor.java#executeBusinessAction` 已转入业务动作执行器 | 事件/定时先复用此路，不新增裸链执行 HTTP 入口 |
| F06 | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessprocess/BusinessProcessOrchestrator.java#executeNode/resumeApprovalResult` | 外层已有等待/CAS 恢复；但条件节点当前直接走 OTHERWISE，SUB_PROCESS 未在该分支接入，不能假定全部节点可执行 |
| F07 | 同目录 `BusinessProcessActionExecutor.java#execute` 不是通用步骤注册器，动作另有受控记录实现 | T3 接入要走可审查适配，不随意塞新 switch 或复制动作运行时 |
| F08 | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/manager/DynamicCrudCreateManager.java#create` 在 `afterCommit` 发布新增事件 | 保存后事件不能阻止原保存；不得用它伪装前置校验 |
| F09 | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-capability-parent/forge-plugin-capability-actions/src/main/java/com/mdframe/forge/plugin/capability/secureaction/publish/SecureActionStepValidator.java` 只允许指定受控步骤 | EXECUTE_RULE 首版不自动进入开放平台/AI 授权面 |
| F10 | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessApplicationSnapshotService.java`、`BusinessApplicationPublishService.java` 管理应用快照和分步发布 | 需新增通用贡献器，不能让 generator 反向依赖规则插件 |
| F11 | 同包 `BusinessApplicationBundleExportService.java`、`BusinessApplicationBundleImportService.java` 及 `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/codegen/LowcodeStaticCodegenContributor.java` | 导入导出和静态源码下载必须处理规则依赖或明确阻断 |
| F12 | `forge-admin-ui/src/components/business-process-designer/BusinessProcessCanvas.vue` 已复用 `FlowCanvas/EdgeLayer`；设计规范在 `forge-admin-ui/DESIGN.md` | 可复用画布底座，不复用审批协议，不引入第二套 UI 组件库 |
| F13 | 当前未发现 LiteFlow 依赖或本项目现成 RuleRuntime/规则插件 | 本文后续类名与表名均为拟新增，不是可直接调用的现有 API |
| F14 | `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/dto/businessapp/BusinessActionExecuteDTO.java`、`service/businessapp/BusinessActionExecutionContext.java`、`BusinessEvent.java`（后二者在同一 generator 包根）尚无完整应用发布上下文 | 不能按 objectCode 猜当前应用；规则桥接需显式补充并验证应用 scope，旧无规则动作保持兼容 |

## 3. 可验证需求清单

| ID | 需求与验收结果 | 阶段 |
|---|---|---|
| R01 | 规则归属当前租户和一个应用，应用合法且访问者有权限；不把租户表数据设为 tenant=0 | T1 |
| R02 | 草稿 JSON 是唯一编辑源；名称/编码、输入输出协议、图、策略整体 CAS 保存 | T1 |
| R03 | 首版支持串行、条件、决策表、计算、输出；非法/不支持图失败关闭 | T1 |
| R04 | Java 节点由受信任插件注册，具有稳定类型、协议版本、输入输出、效果类型及资源限制 | T1 |
| R05 | 字段类型、空值、金额分、Long 字符串、映射路径和超额输入服务端校验 | T1 |
| R06 | 以 EXECUTE_RULE 接入原业务动作；未装适配器时不可发布含该步骤的新动作 | T1 |
| R07 | 规则结果与业务拒绝分开；显式 guard 才能中止后续动作，不把任意 false 当执行错误 | T1 |
| R08 | 身份/租户/组织/数据范围由可信入口构建，伪造 scope/版本/字段访问必须拒绝 | T1/T2 |
| R09 | 草稿试算隔离、限流、无业务副作用；校验报告定位 nodeId/fieldPath | T1 |
| R10 | 发布经结构/图/类型/组件/依赖/冷编译/样例验证，生成不可变版本与摘要 | T1/T2 |
| R11 | 应用、对象动作和后续引用固定规则版本；重试不偷偷换最新版，依赖传递闭包固定 | T2 |
| R12 | Rule-DB 发布有幂等和失败恢复；逐请求验证精确版本，缺失不得降级到旧链 | T2 |
| R13 | 幂等键与请求指纹冲突明确报错，多次触发可追溯，不自动重放失败副作用 | T1/T2 |
| R14 | 有界节点/图深/执行耗时/载荷/队列/日志，超时和资源超限不返回业务成功 | T1/T2 |
| R15 | 运行和节点日志只存安全摘要，真实执行版本、结果、耗时可查，无虚构成功率 | T1/T2 |
| R16 | 应用内管理、编辑、版本、校验/试算、发布状态、运行记录采用系统组件；跨面板走 Pinia | T1/T2 |
| R17 | 应用包携带规则版本与依赖，跨环境导入重新映射 ID 并校验，拒绝缺失插件/组件 | T2 |
| R18 | 静态源码下载通过统一贡献器携带规则/运行依赖/说明；不能静默丢弃 | T2 |
| R19 | 插件可选、未安装不影响原宿主；规则存储异常不伪装成功或让全部页面失效 | T0/T2 |
| R20 | 停用、引用保护、逻辑删除、版本回退语义明确，历史和活跃引用不被清除 | T2 |
| R21 | 原动作、流程审批、公式、无规则应用发布和下载行为保持兼容 | T0/T2 |
| R22 | 开放平台/MCP 权限和受控步骤白名单不变，新能力默认不开放 | T1/T2 |
| R23 | T2 只增加已发布的受控只读查询；不含写库/通知/自由网络/脚本 | T2 |
| R24 | 测试证据、阶段门禁与用户真实环境验收分别记录，不以示例等同上线 | 全阶段 |

## 4. 模块边界与执行链路

### 4.1 拟新增模块（本轮不创建）

| 模块 | 包根 | 职责/依赖 |
|---|---|---|
| `forge-server/forge-framework/forge-starter-parent/forge-starter-rule` | `com.mdframe.forge.starter.rule` | 规则协议、编译/执行端口、LiteFlow 适配、纯节点；不依赖 generator/Flowable/AI |
| `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule` | `com.mdframe.forge.plugin.rule` | 定义/版本/发布/权限/日志，依赖 starter 和平台基础能力，不依赖 generator |
| `forge-server/forge-framework/forge-plugin-parent/forge-plugin-rule-lowcode` | `com.mdframe.forge.plugin.rule.lowcode` | 应用所有权、动作、发布快照、只读查询、包与源码适配；依赖 rule 与 generator |

generator 不得依赖 rule/rule-lowcode；新增中性扩展 SPI 由 generator 声明、桥接插件实现。不存在的 SPI 必须真实新增并回归，不能把提案名称当现成类。

规则插件在宿主/预设中按安装选择聚合；本轮不决定社区/Pro 的商业归属或修改 ForgePro 工程。

### 4.2 执行路径

1. 按钮或触发器进入现有业务动作执行器，执行现有输入投影、权限、幂等与日志。
2. EXECUTE_RULE 适配器从**发布态配置**解析规则版本及映射，从可信业务上下文构建输入。
3. RuleInvocationService 校验 tenant/application/definition/enabled/pinnedVersion/hash/节点协议，保留调用者关联 ID。
4. RuleRuntime 只执行选定的不可变产物；解析成功不代表业务允许，结果仍按输出 Schema 校验。
5. 适配器先在局部结果中完成映射和 guard 校验，成功后原子合入步骤结果上下文；不直接持久化记录。
6. 后续原有写步骤可显式引用规则结果，仍由原事务、字段权限和幂等机制处理。

现有动作请求没有应用版本字段：需增量增加 `applicationCode/applicationVersionId` 作为**待验证的定位信息**，服务端核对已发布应用、动作投影、对象归属和调用权限后才写入可信上下文。没有规则的旧请求可继续缺省；含规则而缺少明确发布上下文时拒绝，不能从 ruleVersionId、对象主应用或“第一个应用”反推。事件/定时从已发布触发绑定解析应用与版本，不依赖事件载荷自报；旧绑定缺此信息必须显式补配置并重新发布。

T1/T2 的 EXECUTE_RULE 不进入 LOCAL_TRANSACTION 白名单，包含它的动作使用 ORCHESTRATION；显式要求本地事务时发布检查拒绝，不能静默降级。纯计算与受控只读查询本身不写业务数据，但这不等于组合动作整体可回滚。

## 5. 编辑协议与运行契约

### 5.1 基础规则

- `schemaVersion="1.0"`；根包含 entryNodeId、inputSchema、outputSchema、nodes、edges、policies。
- inputSchema/outputSchema 为 Forge 强类型字段列表，不接受任意外部 JSON Schema 引用；类型与业务动作对齐：TEXT、INTEGER、NUMBER、MONEY、BOOLEAN、DATE、DATETIME、SELECT。
- MONEY 为 Java long 分，边界传输使用十进制整数字符串；ID 和超过 JS 安全整数范围的整数同样无损传递。NUMBER 用有明确精度/舍入规则的十进制类型，不用浮点算资金。
- null 与缺失区分；可空必须显式声明。除非协议声明，不做字符串真假、日期时区或小数到整数的隐式转换。
- 输入源仅限声明输入、经授权的当前记录/父记录快照、前步骤结果、常量及允许的系统字段；不能读取整个 Session/BeanFactory/数据源连接。
- 同类型节点可在一条规则出现多次，配置通过 nodeInstanceId 解析，禁止存进单例组件可变字段。
- 首版图为有界结构化串行/IF 图；不支持的 DAG 合流、循环、动态路由、并行与未知节点在保存校验/发布均报明确诊断，不猜测转换。

### 5.2 节点范围

| 类型 | 行为 | 关键约束 |
|---|---|---|
| CONDITION | AND/OR 条件组，TRUE/FALSE 两个出口 | 运算符白名单、字段类型匹配、空值语义明确 |
| DECISION_TABLE | 按优先顺序首条命中，返回声明结果 | 必须有默认行；T1 不支持任意冲突策略/脚本单元格 |
| CALCULATE | 白名单类型化运算 AST 赋值到局部变量 | 无任意表达式字符串；溢出/除零报错，舍入显式 |
| OUTPUT | 组装声明输出并结束本路径 | 所有可达路径必须抵达输出；只输出声明字段 |
| QUERY_SOURCE（T2） | 调用明确可只读、已发布的查询源 | 继续检查数据范围、字段、行数/耗时；首版不含 EXTERNAL_API 自由网络或写接口 |
| CALL_RULE（T2） | 固定版本调用已发布子规则 | 静态依赖、环检查、调用深度、租户/应用/版本闭包 |

纯规则只暴露 PURE 节点；QUERY_SOURCE 的 READ 效果声明必须由受信任适配器给出，不能由客户端自报。DB_WRITE/REMOTE_WRITE 节点即使由插件注册，也在本首版发布器拒绝。

### 5.2.1 节点 config 形态

下列字段由节点类型专用 DTO 接收并拒绝未知键，不能用任意 Map 绕过校验：

- 值引用为 `{source,path}` 或 `{source:"STATIC",value}`，二者互斥。规则内部仅接受 INPUT、VARIABLE、STATIC 及发布时明确允许的 SYSTEM；RECORD/FORM 来源由外部动作映射成 INPUT，不能在节点里重新越权取整条记录。
- CONDITION：`{operator:"AND|OR",rules:[条件原子或嵌套条件组]}`；原子为 `{source,path,op,value}`。op 首版限 EQ/NE/GT/GTE/LT/LTE/IN/NOT_IN/IS_NULL/IS_NOT_NULL/CONTAINS，并逐类型验证；空值只参与显式空判断，不让 null 意外等于文本 "null"。
- CALCULATE：`{target,resultType,expression,scale?,roundingMode?}`。expression 是 `{op,operands}` 的 AST，叶子为值引用；首版 ADD/SUBTRACT/MULTIPLY/DIVIDE/MIN/MAX/ROUND 各有固定类型签名。MONEY 以分运算，产生非整数分时必须显式舍入或报错，不能自动换成元。
- DECISION_TABLE：`{columns:[{name,type}],rows:[{id,when,values}],defaultValues}`。按数组顺序取首个 when 命中；values/defaultValues 为列名到值引用的映射，必须覆盖全部列，写入同名局部变量；只有 NEXT 出口，默认行不可缺失。重复/保留列名拒绝。
- OUTPUT：`{values:{输出字段名:值引用}}`，无出口，投影结果必须恰好匹配 outputSchema。
- QUERY_SOURCE（T2）：`{queryRef:{sourceCode,publishedVersionId},mode,parameterMappings,resultMappings}`。mode 限 FIRST_ROW/EXISTS/COUNT，结果投影成声明的局部标量变量；FIRST_ROW 必须有发布态稳定排序/空结果策略。暂不支持把任意数组透传下游。
- CALL_RULE（T2）：`{ruleRef:{ruleCode,ruleVersionId},inputMappings,outputMappings}`；只有 NEXT，输出只合入声明的局部变量，依赖在发布时固定。

编译器建立局部变量类型表：必须在所有抵达使用点的路径上先赋值；首版禁止重复赋值、保留名、路径穿透及同名不同类型，避免分支未初始化或上下文覆盖。

### 5.3 规则 JSON 示例（协议提案，不是已实现配置）

```json
{
  "schemaVersion": "1.0",
  "entryNodeId": "calc_total",
  "inputSchema": [
    { "name": "amountCent", "type": "MONEY", "required": true },
    { "name": "shippingCent", "type": "MONEY", "required": true }
  ],
  "outputSchema": [
    { "name": "totalCent", "type": "MONEY", "required": true },
    { "name": "requiresReview", "type": "BOOLEAN", "required": true }
  ],
  "nodes": [
    {
      "id": "calc_total", "type": "CALCULATE", "config": {
        "target": "totalCent", "resultType": "MONEY",
        "expression": { "op": "ADD", "operands": [
          { "source": "INPUT", "path": "amountCent" },
          { "source": "INPUT", "path": "shippingCent" }
        ] }
      }
    },
    {
      "id": "check_total", "type": "CONDITION", "config": {
        "operator": "AND", "rules": [
          { "source": "VARIABLE", "path": "totalCent", "op": "GT", "value": "100000" }
        ]
      }
    },
    {
      "id": "review", "type": "OUTPUT", "config": { "values": {
        "totalCent": { "source": "VARIABLE", "path": "totalCent" },
        "requiresReview": { "source": "STATIC", "value": true }
      } }
    },
    {
      "id": "normal", "type": "OUTPUT", "config": { "values": {
        "totalCent": { "source": "VARIABLE", "path": "totalCent" },
        "requiresReview": { "source": "STATIC", "value": false }
      } }
    }
  ],
  "edges": [
    { "source": "calc_total", "sourcePort": "NEXT", "target": "check_total" },
    { "source": "check_total", "sourcePort": "TRUE", "target": "review" },
    { "source": "check_total", "sourcePort": "FALSE", "target": "normal" }
  ],
  "policies": { "timeoutMs": 3000 }
}
```

由服务器规范化排序并计算 draftHash。图和组件配置编译为受控 EL，用户不能在同一编辑模式独立修改 EL；不接受前端提交内部运行标识。

### 5.4 动作步骤协议

```json
{
  "stepCode": "price_check",
  "stepName": "计算订单总额",
  "stepType": "EXECUTE_RULE",
  "rollbackOnFailure": true,
  "stepConfig": {
    "ruleCode": "order_total",
    "ruleVersionId": "910000000000000001",
    "inputMappings": [
      { "target": "amountCent", "source": "RECORD", "path": "amountCent" },
      { "target": "shippingCent", "source": "FORM", "path": "shippingCent" }
    ],
    "resultKey": "priceCheck"
  }
}
```

- `ruleVersionId` 是示意字符串 ID；服务端仍必须校验它属于当前应用与规则，不能信任该 ID 或 ruleCode 自报身份。
- 输入和结果映射路径受白名单限制；resultKey 仅写入步骤专用命名空间，不能覆盖 system、tenant、actor、recordId、发布版本等保留字段。
- 可选 guard 是固定 DTO：`{ "outputField": "allowed", "expected": true, "messageField": "message" }`。只有声明的 BOOLEAN 输出可用于 guard，messageField 必须是声明的安全 TEXT。
- 引擎执行成功但 allowed=false 是正常规则结果；配置 guard 后才转换成受控业务拒绝并中止后续步骤。
- 运行时只认发布快照中的引用；设计时可建议某个已发布版本，但禁止存 `latest` 作为运行策略。

### 5.5 类型化 Java 端口（拟新增）

```java
RuleValidationReport validate(RuleDraftValidationCommand command);
RuleCompiledArtifact compile(RuleCompilationRequest request);
RuleExecutionResult evaluate(TrustedRuleInvocation invocation);
RulePublishResult publish(RulePublishCommand command);
RuleTestResult test(RuleTestCommand command);
```

固定字段用 DTO/record，动态数据仅放在命名的 arguments/output 字段；TrustedRuleInvocation 不是 Controller 反序列化类型。

## 6. 数据模型、版本与发布

### 6.1 Forge 控制面拟新增表

以下每张 Forge 表均包含 `id/tenant_id/create_by/create_time/create_dept/update_by/update_time`，时间用 LocalDateTime，utf8mb4/InnoDB。
定义、版本、发布、日志均用显式逻辑删除；存在“有效业务键删除后重建”语义的数值主键表采用 BIGINT del_flag 主键墓碑。

| 表 | 关键字段 | 约束/索引 |
|---|---|---|
| sys_rule_definition | application_id、rule_code、rule_name、draft_json、draft_hash、draft_revision、enabled、default_version_id、del_flag | 有效唯一 `(tenant_id, application_id, rule_code, del_flag)`；草稿 CAS |
| sys_rule_version | definition_id、version_no、schema_json、compiled_el、schema_hash、artifact_hash、dependency_manifest_json、engine_version、component_manifest_json、del_flag | 版本号永久唯一 `(tenant_id, definition_id, version_no)`；正文不可 UPDATE；删除不允许复用历史版本号 |
| sys_rule_release | rule_version_id、publish_key_hash、state、artifact_manifest_json、attempt_no、lease_until、error_code、del_flag | 每版本一个发布记录；发布键同范围唯一；状态 CAS/租约恢复 |
| sys_rule_run | application_id、rule_version_id、caller_scope_hash、idempotency_key_hash、request_digest、status、parent_correlation_id、duration_ms、output_summary、result_ciphertext、result_expires_at、error_code、del_flag | 保留记录期间调用范围+幂等键唯一；指纹覆盖版本/输入/可信主体；租户应用时间索引 |
| sys_rule_node_run | run_id、node_instance_id、attempt_no、node_type、status、duration_ms、summary、error_code、del_flag | `(tenant_id, run_id, node_instance_id, attempt_no)` 唯一；run_id 查询索引 |

不存明文凭据或完整原始输入。运行/节点日志只显示截断脱敏摘要；成功结果为幂等复用单独保存有界、按输出 Schema 投影的密文快照，默认有效 24 小时，日志 API 不返回该密文或明文。过期但幂等记录仍在时返回 RULE_RESULT_EXPIRED，不能用重新查询替换原结果。完整载荷审计不是本期能力。
幂等去重仅在运行记录保留期内承诺；到期清理策略与去重窗口一起公布，不宣称永久 exactly-once。业务副作用仍受父动作自身幂等保护。

### 6.2 Rule-DB 运行存储

- T0 使用隔离文件规则验证；T1 从 Forge 不可变版本恢复单机试用产物，模式明确标为 LOCAL_PREVIEW，不写假的 Rule-DB 成功记录；T2 采用 `liteflow-rule-db-sql`，与旧 SQL 配置源不可混用。
- Rule-DB 原生 `lf_chain/lf_script/lf_change_log/lf_change_lock` 是第三方运行基础表，不套 Forge 实体审计/逻辑删除；归本插件适配器独占，不能直接对外作为业务 CRUD。
- 原生 DDL 从 T0 锁定的实际依赖版本提取、核对后通过新 Flyway 迁移交付；不得猜字段、自动建生产表或改写第三方读写协议。
- 规则数据源明确指定平台元数据源，不随低代码业务数据源切换。运行读和发布写权限分离；不默认另建业务数据库。
- `applicationName` 隔离 Forge 安装/环境，内部 chain 标识由 tenant/application/definition/version/产物摘要生成且受长度限制；namespace 不是租户鉴权。
- 发布只走 RulePublisher，默认关闭 auto-init-table。框架轮询/缓存不会经过 MyBatis-Plus 租户插件，租户检查必须在 Forge 入口和产物命名处显式执行。

### 6.3 两种权威与固定版本

Forge 版本表是业务配置与历史权威；Rule-DB 是 LiteFlow 运行读取权威，缓存不作为历史事实。

- 每个 Forge 版本映射独立、不可变内部 chainId；依赖清单固定子规则版本、编译器协议、组件协议版本与输入输出摘要。
- LiteFlow CAS version/变更序号与 Forge 业务 version_no 分开维护，不能互相冒充。
- 新增规则版本不改变已经发布的对象动作、应用版本或已发起业务过程的引用。
- 应用快照与规则引用存在冲突时失败关闭，不能在运行时取任意一个“最新值”。
- T1 尚无应用级依赖贡献器时，规则只允许在隔离/开发试用路径消费；正式应用发布遇到 EXECUTE_RULE 必须明确阻断，直到 T2 完成闭包。

### 6.4 发布状态机与恢复

`PREPARING -> WRITING -> VERIFYING -> AVAILABLE`；任一步可转 FAILED。失败重试必须恢复相同 rule_version_id 和 manifest，不能重读草稿换内容。

1. 校验 expectedDraftHash/draftRevision、权限、类型/图/依赖和组件能力；发布同一幂等键且内容相同返回原发布记录，不同内容报冲突。
2. 短事务分配单调版本、保存不可变快照和 PREPARING 发布记录；分配号失败允许有空洞，不允许倒退/复用。
3. 事务外按依赖从叶子到根调用 Publisher；一个调用只发布一个对象，不假设跨多链原子事务。
4. 遇到超时但不确定写入结果时，按内部标识读取元数据/正文核对同版本 hash；匹配则继续，不匹配则冲突，禁止覆盖。
5. 从运行存储冷加载精确 manifest 编译、校验后置 AVAILABLE；平台事务通过 CAS 完成发布记录，现有应用激活指针在此之前不改变。
6. 发布工作器用持久记录+有界租约认领，崩溃恢复续做同一 release，不能仅用单机异步队列。已有业务副作用不由本工作器重放。

AVAILABLE 只说明产物已持久化且验证器通过，不宣称所有节点同时 ready。每个执行节点仍必须校验所需产物 hash 和组件协议并进行有界冷加载；缺失/滞后返回 RULE_NOT_READY，不回退旧版本。节点加载信息如展示，必须标明实例和采集时间，未采集不能显示“全部同步”。

### 6.5 停用、删除与回退

- 停用定义：需展示受影响应用/引用并确认；新调用在可信解析边界拒绝，已进入的 PURE/READ 调用可结束，不承诺强制撤销。
- 删除：有活跃应用/对象动作/过程引用时拒绝；无引用且停用后仅逻辑删除定义，历史版本/发布/运行记录保留，不复用版本号。
- 回退规则：为新引用选择先前已验证版本；现有应用通过创建新的应用发布版本重新绑定。不能改小 Rule-DB 的序号或覆写旧快照。
- 安装降级/卸载前检查活跃引用；有引用时拒绝卸载或要求先迁移，不自动把规则步骤当空操作。
- 无规则应用不因规则插件缺失而失败；规则引用应用缺引擎/产物时明确不可用。

## 7. 安全、效果与资源边界

### 7.1 身份矩阵

| 入口 | 可信来源 | 特殊约束 |
|---|---|---|
| 管理/草稿试算 | 登录会话、应用访问校验 | 试算权不等于发布权；不能借试算访问其他应用记录 |
| 页面业务动作 | 原动作授权+已发布版本+可信记录 | 未保存 formData 是用户输入，不冒充库内事实 |
| 事件/定时 | 原触发器服务身份与事件可信 scope | 无可信身份拒绝，不默认提权到管理员 |
| 原生 Java 调用 | 服务端构建的 Invocation 与显式身份 | 复用相同策略，不增加免鉴权后门 |
| AI/MCP/开放平台 | 首版不开放 | 继续由现有允许列表拒绝 EXECUTE_RULE，不扩授权 |

应用内对象归属、当前记录权限、查询源权限分别检查；能编辑规则不等于能读应用全部记录。

### 7.2 资源与上下文

- 默认总预算 3000ms，节点 100、条件/AST 深度 8、子规则深度 8、入/出字段各 100、载荷各 256KiB；管理员只能在服务器硬上限内调整，前端不可放宽。
- T2 只读查询最多 100 行且受剩余总预算约束，不允许无限分页；过大/过深输入在执行前拒绝。
- 使用 Spring 托管的有界、命名执行资源并显式传播/清理租户、组织、数据源、执行身份，不传播可变 Session 对象或共享 Map。
- 串行写入局部变量，OUTPUT 后结果封闭；暂不支持 WHEN 并行。ThreadLocal 传播不等于传播 Spring 数据库事务。
- 超时返回 TIMED_OUT，迟到结果不能覆盖终态或映射到后续业务上下文。Java 中断不是强杀；只允许运算量受控节点，查询必须有真实驱动超时；占用额度直到任务实际结束。
- 试算和正式执行有独立并发配额/有界队列与拒绝响应。对外只返回错误码和安全说明，不把 JDBC/EL/堆栈或输入原值拼进异常。

### 7.3 幂等和结果

- 父动作继续持有自身幂等。子规则键由父动作日志/步骤实例/固定规则版本派生，直接内部调用需给合法幂等键；前端不能指定其他用户的 caller_scope。
- 指纹含 scope、固定版本、规范化输入与必要的记录版本；同键不同指纹报 409，不返回其他请求的结果。
- 相同指纹已成功且结果快照未过期时可复用经权限校验的原结果；快照过期明确拒绝，不重新计算冒充命中。RUNNING 返回处理中，FAILED/TIMED_OUT 不自动重试；显式新尝试保持原版本。
- sys_rule_run 状态为 RUNNING/SUCCEEDED/FAILED/TIMED_OUT；引擎正常 false 输出仍是 SUCCEEDED。guard 拒绝记在父业务动作步骤及其安全错误码中，不把已成功的计算记录再次改为失败。
- 纯规则仅返回结构化输出；规则拒绝导致既有动作如何提示/中止由适配器控制，不宣称 ORCHESTRATION 已执行写步骤可整体回滚。

## 8. 接口和用户交互（拟新增）

### 8.1 管理 API

统一前缀 `/system/rule`，响应沿用 RespInfo；写接口固定 DTO+`@Valid`，管理查询也验证分页和 applicationId。ID 响应转字符串，分页参数 pageNum/pageSize。

| 方法/路径 | DTO/结果 | 权限 |
|---|---|---|
| GET `/page` | RulePageQueryDTO / Page<RuleSummaryVO> | system:rule:list + 应用访问 |
| GET `/:id` | RuleDetailVO | system:rule:query + 应用访问 |
| GET `/components` | 受信任组件目录及协议版本，非任意 Bean 列表 | system:rule:query |
| POST `/` | RuleDefinitionCreateDTO；应用、名称、编码、草稿 | system:rule:add |
| PUT `/` | RuleDefinitionUpdateDTO；id、expectedDraftHash、expectedRevision、完整草稿 | system:rule:edit |
| DELETE `/:id` | 引用检查后逻辑删除 | system:rule:remove |
| POST `/:id/validate` | RuleValidateDTO；expectedDraftHash / RuleValidationReport | system:rule:edit |
| POST `/:id/test` | RuleTestDTO；expectedDraftHash、arguments / RuleTestResultVO | system:rule:test |
| POST `/:id/publish` | RulePublishDTO；expectedDraftHash、expectedRevision、publishKey、说明 | system:rule:publish |
| GET `/:id/versions`、GET `/version/:versionId` | 租户/应用范围内安全版本信息 | system:rule:query |
| GET `/release/:releaseId` | 发布步骤、状态、安全错误摘要，不承诺全节点 ready | system:rule:publish |
| POST `/:id/default-version` | RuleDefaultVersionDTO；expectedPointer、versionId；仅影响新引用默认值 | system:rule:publish |
| POST `/:id/availability` | RuleAvailabilityDTO；enabled、expectedRevision、影响确认凭据 | system:rule:enable |
| GET `/runs`、GET `/run/:runId` | RuleRunQueryDTO / 脱敏结果与节点时间线 | system:rule:log + 应用访问 |

不新增面向浏览器的 `execute(chainId, el, context)` 接口。正式调用继续用已有 `/ai/business/action/execute`；只允许服务端 facade 接受 TrustedRuleInvocation。

前端按现有加密请求工具对接，后端匹配 `@ApiDecrypt/@ApiEncrypt`；租户、actor、authority、内部运行 ID 均不从请求信任。敏感权限和停用状态迁移需人工审查后实施。

### 8.2 错误契约

| 内部诊断码 | 对外语义 | 响应要求 |
|---|---|---|
| RULE_SCHEMA_INVALID / RULE_INPUT_INVALID | 图/协议/输入无效 | 400，返回安全 fieldPath/nodeId，不返回输入原值 |
| RULE_DRAFT_CONFLICT / RULE_PUBLISH_CONFLICT | 草稿 CAS/发布幂等冲突 | 409，不能覆盖别人的保存或生成另一个版本冒充幂等 |
| RULE_SCOPE_DENIED | 身份/租户/应用/记录权限不符 | 本租户无权用 403，跨租户或不可见对象用不泄露存在性的 404 |
| RULE_ENGINE_UNAVAILABLE / RULE_NOT_READY | 插件/运行产物/组件不可用 | 503，明确不可执行，不回退、不显示业务成功 |
| RULE_TIMED_OUT | 预算耗尽 | 504 或项目统一超时语义，运行状态 TIMED_OUT，迟到结果不得覆盖 |
| RULE_DISABLED / RULE_REFERENCED | 已停用或有活跃引用 | 409，提示影响范围，不自动删引用 |
| RULE_RESULT_EXPIRED | 已有幂等结果不能再复用 | 409，要求显式新请求，不无声重算 |
| RULE_GUARD_REJECTED | 计算成功但调用方业务校验拒绝 | 由父动作沿用业务拒绝响应；安全文案、无后续步骤，规则运行仍 SUCCEEDED |

内部 errorCode 是诊断 VO 的独立字段；RespInfo.code 沿用现有数值约定，不为此变更通用错误协议或把字符串码强塞 HTTP status。

### 8.3 页面

- 应用工作台“业务逻辑/规则”入口，始终显示当前应用归属；规则管理页可按有权限应用筛选，不默认加载所有租户。
- 列表：名称/编码、真实默认发布版本、草稿关系、更新时间；最多两个常用操作，其余更多菜单。区分空数据/无匹配/加载失败。
- 编辑工作台：基础信息与输入输出、规则编辑、试算、版本记录；主操作集中顶部。T1 可用结构化节点/条件表单，T2 复用 FlowCanvas 构建受控可视化，不新引入图框架。
- 编辑状态入 Pinia，配置/画布/测试区不深度传参；T1/T2 禁止将巨型 SFC 或 Service 继续堆长。
- 发布前展示输入输出及依赖差异、校验结果；草稿改动不冒充发布状态，解析失败不显示成功。
- 试算显示 sample 输入、真实命中路径、结构化输出与耗时；“校验通过”“试算成功”“已写入运行库”“可用于新引用”是不同状态。
- 组件/样式遵循 DESIGN.md，Naive UI、主题变量、深浅色/窄屏、真实 loading，禁止营销化装饰。
- H5/其他端只复用规则动作协议，不建设一套独立移动规则编辑器。

## 9. 应用发布、包与静态源码

- 新增中性应用资产贡献 SPI：收集、校验、固定依赖、序列化、导入重映射。只有装有适配器才处理规则资产；无规则应用返回空贡献，既有快照 hash 不被无意义改变。
- 当前对象动作和应用快照都应保留固定版本引用，并在发布时核对一致；不能只有按钮拿到版本而导出/恢复仍取最新版。
- 应用发布预检完整 dependencyManifest：同租户/同应用（首版禁止跨应用共享）、目标 AVAILABLE、组件协议匹配、图无环、没有不支持效果。
- 恢复应用发布读取原发布任务快照，不重新选择规则；活跃的不可变规则版本不能因为默认指针改变而消失。
- 包导出携带规则 JSON、不可变版本、组件/引擎要求、依赖闭包与摘要，不携带环境凭据、内部 chainId 或源租户授权。
- 导入先预检再重映射到目标租户/应用，重新生成内部 ID、重新编译并发布本地运行产物；不能直接信任导入包的 EL 或执行源码。缺插件/版本不兼容时给出明确诊断。
- 内容市场包复用同一应用包协议；不另造商店专用规则导入通道。
- 静态下载通过 LowcodeStaticCodegenContributor 增加规则资源、适配调用、构建依赖和运行说明；禁止覆盖用户业务扩展。生成工程仍是普通后端工程，不退回统一 DynamicCrud 入口。
- T2 未完成某种分发/导出模式前，此模式必须显式拒绝含规则的应用，不能仅在文档提示后生成不可运行产物。

## 10. 验收与兼容

1. 原无规则应用、动作、触发器、字段公式、审批状态/表单权限/回调保持原行为。
2. 同一规则试算与固定版本执行对相同输入返回同类型结果，边界值/异常有确定行为。
3. 旧应用固定 v1，新应用固定 v2，多节点冷缓存/滞后不能串版本；缺依赖时不回退。
4. 跨租户/跨应用、缺权限、伪造系统字段/内部 ID、未知协议/效果全部拒绝。
5. 重复发布、重复调用、发布中断、并发保存、超时迟到、停用/删除保护有自动化证据。
6. 导出/导入/静态源码至少各一条真实可运行验证；未覆盖模式显示不支持，不伪造全覆盖。
7. 文档静态检查、Java 目标模块/宿主构建、前端 lint/build/UI、SQL/MySQL/多实例分别记录。
8. 内存数据库、Mock 或合成页面不等同真实 Flyway/Flowable/网络鉴权验收。

测试用例和命令详见 test-spec.md。本次文档生成不执行任何未来功能测试。

## 11. 风险、回滚与实施门禁

### 11.1 高风险清单

- **权限**：规则执行、查询、试算、发布、停用及包导入引入新的资源面，需人工审查。
- **状态流转**：发布状态、可用性、应用激活、运行终态 CAS；不能修改旧 SQL 或关闭 Flyway 掩盖错误。
- **金额**：只做分单位计算，不直接变动资金；精度/溢出/舍入需要用例与人工确认。
- **并发/热更**：持久产物不等于全部执行节点已加载；版本闭包和组件协议不匹配时拒绝执行。
- **本机存量**：现有未提交部署/图标/memory 文件不属于本变更，不清理/覆盖。

### 11.2 回滚

- 未安装插件的宿主路径不受影响；不得为回滚删除规则定义、历史或运行数据。
- 停用新引用并将应用发布成绑定旧版本的新应用版本，是业务回退路径；不修改既有发布正文或缩小序号。
- 代码降级前先检查活跃规则依赖，无法解释的引用应阻断而非跳过；保留数据库迁移与第三方运行表。
- 新迁移编号在实施时按目录/已执行历史确定，**本 Spec 不预占 V1.0.229 等编号**。当前基线最大文件号为 1.0.228，未来不可据此硬编码。

### 11.3 HARD-GATE

| 门禁 | 当前状态 | 通过条件 |
|---|---|---|
| G0 文档生成授权 | 已取得 | 用户本轮“生成吧”，仅文档 |
| G1 实施授权与范围 | 未取得 | 用户确认按此提案开始 T0，注明分支/允许操作 |
| G2 引擎版本与装配 | 待 T0 | 实际存在且一致的 core/Boot4/Rule-DB 版本、启动/组件/隔离/超时证据；不用 Agent 模块 |
| G3 安全与协议评审 | 待人工确认 | 身份/权限、金额、停用影响、固定版本、输入输出协议审查 |
| G4 首个生产版本交付 | 未开始 | T0～T2 + test-spec 的 P0/真实环境门禁通过，或由用户明确接受尚未覆盖的上线风险 |

T0 需要核实的技术问题不是已通过事实：

- 官方专用 Starter 与 Forge Boot 4.1.1 的实际依赖树、Jackson/日志/表达式包兼容性。
- LiteFlow 是否使用全局注册表，以及如何保证草稿验证、试算不覆盖发布规则；不可假定 new FlowExecutor 就隔离。
- 未安装/已安装但关闭/存储异常三种装配行为，必须实测第三方自动装配控制，不只加一个无效 feature flag。
- RulePublisher 的确切 API/DDL/CAS/关闭连接语义和冷加载行为，按锁定版本记录，不能把官网示例直接当本工程已验证实现。

## 12. 资料与确认记录

事实来源为本轮前的用户调研报告、当前 main 源码及分析轮核对的官方文档；下列链接是外部事实来源，不作为实施指令：

- [LiteFlow Spring Boot 安装](https://liteflow.cc/pages/9bf6be/)：Boot 4 专用 Starter/JDK 要求。
- [LiteFlow FAQ](https://liteflow.cc/pages/845dff/)：审批、无状态、上下文和事务边界。
- [Rule-DB 发布 API](https://liteflow.cc/pages/ruledb-publisher/)：单目标写入、CAS、依赖顺序与非语义校验。
- [Rule-DB 存储结构](https://liteflow.cc/pages/ruledb-storage/)：运行表与 application_name/chain_id。
- [Spring 事务说明](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-decl-explained.html)：线程事务边界。

审查结论：方案文档已生成，业务实现/依赖兼容/真实环境验证均未开始。本轮生成文档不等同用户批准 G1～G4。
