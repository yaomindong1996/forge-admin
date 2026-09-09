# 低代码子表行动作与父子可信上下文
> status: completed
> created: 2026-08-11
> complexity: 🔴高

## 1. 背景与目标

Forge 已具备主子表编辑、不可变发布动作、结构化输入、幂等执行和本地事务命令，但子表运行时只有新增、选择记录和删除，没有通用的已落库子行按钮，也没有服务端可信的父记录/子记录上下文。客户要用低代码完成“对某一条明细执行登记、确认、调整、查询后写入”等业务时，仍会被迫写页面或专用接口。

本阶段继续增强通用 BusinessAction，不新增任何预售、会员、商品、支付或企业微信专用代码：

- 增加 `CHILD_ROW` 动作位置，并绑定稳定 `relationKey`；
- 子表行按钮继续执行现有 `COMMAND`，复用输入 Schema、权限、幂等、事务和日志；
- 浏览器只提交父记录 ID、子记录 ID、relationKey、声明输入和 routeQuery；
- 服务端从不可变发布快照解析动作和关系，再重新读取权威父子记录；
- `record` 表示当前子记录，`parentRecord` 表示父记录，`SYSTEM` 继续表示可信身份；
- 未保存的新子行禁止执行服务端命令，不能把浏览器草稿伪装为数据库记录。

## 2. 架构归属与边界

```text
ChildTableEditor 行按钮
  -> AiCrudPage 通用 COMMAND 输入/确认/幂等
  -> BusinessActionExecutionController
  -> 不可变对象发布快照中的 CHILD_ROW 动作与关系
  -> 动态 CRUD 按父 ID 读取主子详情（租户 + 数据权限）
  -> 按 relationKey 和 childRecordId 校验子行归属
  -> BusinessActionExecutionService / 原有步骤执行器
```

`CHILD_ROW` 首版只允许 `COMMAND`。直接 URL、任意 Header、浏览器行字段拼接 API、脚本和 SQL 不进入该协议，避免客户端把整行或敏感信息作为可信参数。页面跳转等纯前端行为仍可由现有页面操作配置承担，不混入服务端父子上下文。

## 3. 动作与请求协议

设计态动作：

```json
{
  "actionCode": "confirm_detail",
  "actionName": "确认明细",
  "actionPosition": "CHILD_ROW",
  "actionType": "COMMAND",
  "actionConfig": {
    "triggerScene": "MANUAL",
    "relationKey": "order_item",
    "inputSchema": [],
    "steps": []
  }
}
```

运行请求：

```json
{
  "objectCode": "order",
  "recordId": "2001",
  "parentRecordId": "1001",
  "childRecordId": "2001",
  "relationKey": "order_item",
  "actionCode": "confirm_detail",
  "formData": {},
  "context": { "routeQuery": {} },
  "idempotencyKey": "ui:..."
}
```

`recordId` 在子行动作中等于 `childRecordId`，用于兼容结果和执行日志；父记录必须使用独立 `parentRecordId`，禁止根据浏览器提交的父行字段推断。

## 4. relationKey 与发布快照

- 当前主子表运行时以子模型 `modelCode` 作为集合键，`relationKey` 默认取该稳定模型编码；
- 动作配置、关系运行时和请求必须三方一致；
- 发布检查确认 relationKey 指向当前对象的一条启用明细关系；
- 发布时把匹配的 CHILD_ROW 动作投影到对应 `masterDetailConfig.children[].rowActions`；
- 服务端执行时从动作所属发布版本的 `relationSnapshot` 再次解析关系，不信任当前草稿或客户端传入的目标对象/表名；
- 若发布关系、运行配置或父详情中不存在该 relationKey，失败关闭。

## 5. 可信上下文

| 路径 | 语义 | 来源 |
|---|---|---|
| `record.*` / `row.*` | 当前已落库子记录 | 服务端通过父详情和子 ID 匹配 |
| `parentRecord.*` / `parent.*` | 当前父记录主表数据 | 服务端按父 ID 读取 |
| `formData.*` | inputSchema 声明的动作输入 | 浏览器提交后服务端投影与转换 |
| `context.routeQuery.*` | 普通路由业务参数 | 浏览器最小上下文，安全过滤 |
| `SYSTEM.*` | 租户、用户、组织、父子 ID、relationKey | Session/执行身份与服务端解析 |

客户端提交的 `row`、`record`、`parentRecord`、对象编码、表名、字段名、租户和身份字段都不能覆盖上述可信值。

## 6. 运行时交互

- 子表操作列显示已发布且有权限的 CHILD_ROW 动作；
- 已落库行可执行，未保存行显示禁用态并提示“请先保存主记录和子表行”；
- 动作有 `inputSchema` 时复用 AiCrudPage 的通用输入弹窗；
- 确认、输入校验、按钮防重复、幂等键和结果提示复用主表动作实现；
- 动作完成默认刷新列表，不强制覆盖当前编辑表单中的未保存草稿；详情刷新可由成功行为后续扩展。

## 7. 发布与安全校验

- `CHILD_ROW` 仅允许 `COMMAND` 且 `triggerScene=MANUAL`；
- relationKey 必填、格式安全并指向启用的 DETAIL/CHILD_LIST/ONE_TO_MANY 关系；
- 动作、关系和目标模型在发布快照中固化；
- parentRecordId、childRecordId、relationKey 缺失或不一致时，在任何步骤副作用前拒绝；
- 子记录不存在、不属于父记录或父记录不可见时，统一返回安全业务错误；
- 幂等摘要包含父 ID、子 ID 和 relationKey，同键不能跨父子上下文复用；
- 执行日志不保存父子记录内容和动作输入值。

## 8. 兼容策略

- `TOOLBAR`、`ROW`、`DETAIL` 动作保持现有语义；
- 非 CHILD_ROW 请求无需新增字段；
- 存量主子表没有 rowActions 时行为不变；
- 草稿预览可读取草稿动作，但真实执行仍只读取发布快照；
- 当前运行时不支持同一父对象对同一 targetObjectCode 配置多条内嵌关系，relationKey 因此使用 target modelCode；未来放开多关系时需升级为独立持久化关系编码。

## 9. 测试策略

- 后端：发布关系解析、父子归属、可信上下文、路径映射、幂等摘要和失败关闭；
- 发布：CHILD_ROW 类型/场景/relationKey 校验和运行态投影；
- 运行配置：子表 relationKey、rowActions 和动作身份完整保留；
- 前端：父子最小载荷、未保存行禁用、输入弹窗复用和不提交整行；
- 执行 Java/Vitest/ESLint、generator 聚合构建、前端生产构建及静态安全检查。

## 10. 非本阶段范围

- 未保存子行的服务端命令或浏览器草稿脚本；
- 任意 API、SQL、JavaScript/Groovy/SpEL 子行动作；
- 企业微信/H5 容器、扫码 SDK（阶段 6）；
- 离线草稿、冲突重放和长期治理（阶段 7）。

## 11. 确认记录

- 2026-08-10：用户授权按既定七阶段路线连续实施，无需阶段间再次确认。
