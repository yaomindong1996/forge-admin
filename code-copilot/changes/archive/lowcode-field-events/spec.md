# 低代码统一字段查询事件
> status: completed
> created: 2026-08-10
> complexity: 🔴高

## 1. 背景与目标

低代码表单需要在页面打开、字段变化、字段失焦、扫码完成或用户主动点击时，基于当前表单值调用受管只读查询源，并把结果安全、可预期地回填到一个或多个字段。典型验收场景包括当前人员/门店识别、手机号查会员、条码查商品和业务单号查收款，但平台协议和实现不得出现预售、会员、商品、支付或企业微信专用代码。

本阶段建设应用级、结构化的字段查询事件协议，并在表单设计器、发布检查和 `AiForm` 运行时形成闭环。完成后应满足：

- 触发器支持 `FORM_LOAD`、`CHANGE`、`BLUR`、`MANUAL`、`SCAN_COMPLETE`；
- 查询来源只允许上一阶段的 `EXTERNAL_API`、`DATASET`；
- 参数只允许从表单字段、受控运行上下文和可选路由查询参数映射；
- 查询结果按显式路径映射到目标字段，支持根对象与首行两种结果模式；
- change 防抖、同规则请求取消、过期响应隔离和字段级运行状态由统一运行时处理；
- 设计器只选择受管查询源和契约字段，不填写任意 URL、Header、认证、SQL 或脚本；
- 配置随应用表单不可变发布快照交付，设计态和发布态使用同一协议。

## 2. 架构归属

字段事件描述“应用如何使用数据”，归属 `formDesignerSchema.settings.governance.fieldEvents`，不放入业务对象字段定义，也不新增独立数据库表。对象继续定义字段和存储，应用表单定义事件、查询参数和回填行为。

运行链路：

```text
表单控件事件
  -> AiForm 统一事件调度器
  -> POST /ai/lowcode/query-source/execute
  -> EXTERNAL_API / DATASET 受管查询源
  -> 显式结果路径映射
  -> 表单目标字段 + 字段级运行状态
```

发布时由后端检查协议白名单、字段存在性和危险键；发布快照仍由现有低代码运行时编译链透传，不在浏览器端拼装访问边界。

## 3. 事件协议

单条事件结构：

```json
{
  "id": "query_contact",
  "name": "查询联系人",
  "enabled": true,
  "trigger": "BLUR",
  "sourceField": "mobile",
  "sourceType": "EXTERNAL_API",
  "sourceKey": "crm/contact_lookup",
  "debounceMs": 300,
  "skipWhenEmpty": true,
  "clearTargetsOnTrigger": true,
  "paramMappings": [
    {
      "param": "mobile",
      "source": "FORM_FIELD",
      "field": "mobile"
    }
  ],
  "resultMode": "ROOT",
  "resultMappings": [
    {
      "from": "contact.name",
      "to": "contactName",
      "whenMissing": "CLEAR"
    }
  ],
  "notFoundMessage": "未匹配到数据",
  "errorMessage": "查询失败，请重试",
  "errorMode": "MESSAGE"
}
```

### 3.1 触发器

| 触发器 | 语义 |
|---|---|
| `FORM_LOAD` | 新增/编辑表单数据与上下文就绪后执行一次 |
| `CHANGE` | 来源字段变化后按 `debounceMs` 防抖执行 |
| `BLUR` | 来源字段失去焦点时执行 |
| `MANUAL` | 来源字段旁的紧凑“查询”按钮触发 |
| `SCAN_COMPLETE` | 扫码值确认或桌面扫码枪 Enter 后触发 |

`sourceField` 对 `CHANGE`、`BLUR`、`MANUAL`、`SCAN_COMPLETE` 必填；`FORM_LOAD` 可不填。H5 扫码组件后续调用同一 `SCAN_COMPLETE` 分发入口，本阶段不硬编码任何厂商扫码 SDK。

### 3.2 参数映射

参数来源白名单：

- `FORM_FIELD`：当前表单字段；
- `CONTEXT_PATH`：表单运行时显式暴露的只读上下文路径；
- `ROUTE_QUERY`：当前路由查询参数，仅作查询条件。

浏览器提供的人员、租户、组织、门店或路由参数均不得作为授权依据。服务端 ACL、租户、数据权限和外部接口权限必须继续来自可信登录 Session 和查询源安全链。

### 3.3 结果映射

- `resultMode=ROOT`：从统一网关响应的 `data` 根节点取值；
- `resultMode=FIRST_ROW`：`data` 为数组或含 `records/list/rows` 数组时取首行；
- `from` 为只读点路径，不执行表达式；空路径表示选中结果本身；
- `to` 必须是当前表单已存在字段；
- `whenMissing=CLEAR` 清空目标字段，`KEEP` 保留旧值；
- 无选中结果时进入“未找到”分支，不把错误响应回填进表单。

## 4. 运行时治理

- `debounceMs` 范围为 0～5000，默认 `CHANGE=300`、其它触发器 0；
- 同一规则发起新请求前中止旧请求；无法中止时仍通过递增序列号忽略过期响应；
- 组件卸载时清理全部计时器和请求；
- `skipWhenEmpty=true` 时，来源字段为空不发送请求，并按配置清理目标字段；
- `clearTargetsOnTrigger=true` 时在请求开始前清理目标字段，避免旧结果被误认作当前结果；
- 每个来源字段维护 `idle/loading/success/not_found/error` 状态；
- 查询中禁止同一规则重复手动触发，并在字段旁提供状态反馈；
- 未找到使用 `notFoundMessage`，网络/业务异常使用 `errorMessage`，不展示原始响应、URL、SQL、Header 或堆栈；
- 一个触发事件可匹配多条规则，按配置顺序独立执行，失败不阻塞其它规则。

## 5. 设计器

在现有“表单事件与生命周期”配置中增加“字段查询回填”结构化编辑器：

- 选择触发字段和触发时机；
- 通过阶段 2 目录选择受管查询源；
- 读取来源元数据配置参数来源；
- 选择返回路径与目标字段；
- 配置防抖、空值、未找到和错误提示；
- 支持新增、复制、启停、删除和排序；
- 配置入口保持紧凑，复杂明细在局部弹窗内编辑，确认后原子写入草稿。

现有表单生命周期高级能力继续保留，但不作为字段查询主入口；旧 linkage 的任意远程 URL 能力不扩展到新协议。

## 6. 后端发布检查

发布时必须失败关闭校验：

- `fieldEvents` 必须为数组，事件 `id` 唯一且符合安全编码格式；
- trigger、sourceType、resultMode、参数来源、缺失策略和错误模式必须命中白名单；
- `sourceField`、参数映射字段和结果目标字段必须存在于表单字段集合；
- `sourceKey`、参数名和结果目标不能为空；同一事件内参数名、结果目标不能重复；
- `debounceMs` 在 0～5000；提示文本有长度上限；
- 任意层级出现 `url`、`header`、`authorization`、`credential`、`secret`、`sql`、`script`、`handler` 等危险键时拒绝发布；
- 发布检查不通过时返回结构化问题路径，不让非法配置进入运行快照。

本阶段不在发布检查中远程解析查询源是否存在，以避免发布事务依赖外部运行资源；设计器元数据校验和运行时查询源 ACL 共同失败关闭。后续可增加发布前资源解析器扩展点。

## 7. 安全与兼容性

- 不新增任意 JavaScript 沙箱、URL/SQL 编辑器或浏览器凭据配置；
- 客户端字段事件只组织查询参数，不能覆盖租户、用户、组织或权限；
- 查询错误日志不记录参数值、表单快照或原始响应；
- 旧表单没有 `fieldEvents` 时行为不变；非法/未知规则在运行端也忽略并提示配置错误，不能降级执行；
- 现有 `linkageSchema` 和生命周期事件保持兼容，新能力不改变其存量协议。

## 8. 测试策略

- 纯函数：规则标准化、参数映射、路径读取、ROOT/FIRST_ROW、缺失策略和危险配置失败关闭；
- 调度：防抖、取消、过期响应、空值跳过、表单加载和卸载清理；
- 组件：blur、Enter/扫码、手动查询按钮、loading/error/not-found 状态；
- 设计器：来源目录、元数据、字段映射和保存协议；
- 后端：发布校验白名单、字段存在性、重复项、范围和危险键；
- 编译器：发布快照和运行时配置完整携带 `fieldEvents`；
- 前端生产构建、目标 Lint、Java 定向测试和相关模块聚合编译。

## 9. 非本阶段范围

- 事务写命令和跨表原子落库；
- 子表行内查询按钮和行上下文映射；
- 企业微信/H5 SDK 初始化、扫码授权和移动端容器适配；
- 断网表单暂存、重放、冲突检测；
- 任意浏览器或服务端脚本执行。

## 10. 确认记录

- 2026-08-10：用户授权按既定七阶段路线连续实施，无需阶段间再次确认。

## 11. 完成结论

- 2026-08-10：字段查询事件已形成设计器、发布检查、发布快照和 `AiForm` 运行时闭环。
- 实施人员只能选择受管 `EXTERNAL_API`、`DATASET`，通过结构化参数和结果映射完成页面打开、change、blur、手动查询和扫码/Enter 回填。
- 运行时已覆盖空值跳过、查询前清理、防抖、请求取消、过期响应隔离、卸载清理、ROOT/FIRST_ROW 和字段级状态反馈。
- 当前 Forge 用户的最小只读标识可通过 `CONTEXT_PATH` 参与 userid/工号/门店映射查询，但服务端权限仍只认可信 Session。
- 发布检查会阻止未知协议、缺失/重复字段、危险路径，以及 URL、Header、认证、凭据、SQL、脚本和 handler 等配置进入快照。
- 自动化测试、相关模块聚合编译、前端 Lint 和生产构建均已完成；真实查询源、多角色、多租户、扫码枪和弱网体验按既定分工留待部署环境补验。
