# 低代码企业协同容器与扫码能力

> status: completed
> created: 2026-08-10
> complexity: 🟠中高

## 1. 背景与目标

企业微信/H5 页面需要在不编写业务页面代码的情况下打开扫码能力，并把扫码结果接入已有 `SCAN_COMPLETE` 字段查询事件。平台只负责容器能力探测、扫码结果归一化、超时/取消/不支持反馈和最小上下文传递；手机号查会员、条码查商品、静态码查收款等具体业务继续由受管 `EXTERNAL_API` / `DATASET` 查询源配置完成。

完成后应满足：

- 统一识别企业微信内置浏览器、钉钉、飞书、H5 和普通浏览器；
- 优先调用宿主注入的扫码 SDK，支持企业微信 `wx.scanQRCode` 的回调式接口；
- 统一归一化字符串、`resultStr`、`codeString`、`value/type` 等返回形态；
- 扫码成功只写入当前业务字段并分发 `SCAN_COMPLETE`，不提交完整表单；
- 不支持、取消、超时和失败返回稳定错误码，调用方可以展示受控文案；
- 扫码结果只作为业务查询参数，浏览器自报的 `userId`、`tenantId`、`activeOrgId`、门店或权限字段不得成为可信身份；
- 不配置扫码事件的字段不展示扫码入口，手持扫码枪按 Enter 的旧链路保持兼容。

## 2. 架构归属

新增通用运行时工具 `src/utils/collaboration-runtime.js`，不新增预售、会员、商品、支付或企业微信专用业务类。`AiFormItem` 通过现有 `context` 接收可选扫码入口，`field-event-runtime` 接收显式扫码上下文并继续使用受管查询源。

运行链路：

```text
字段扫码按钮/宿主事件
  -> collaboration-runtime.scan()
  -> normalizeScanResult()
  -> AiFormItem 写入 sourceField
  -> AiForm 统一 dispatchFieldEvent('SCAN_COMPLETE', field)
  -> 受管 EXTERNAL_API / DATASET 查询源
  -> 显式结果映射回填
```

运行时不得从浏览器 UA、扫码值或 query 参数推导 Forge 身份、租户或数据权限；这些仍来自服务端可信 Session、租户拦截器和查询源 ACL。

## 3. 容器识别与扫码协议

### 3.1 平台识别

`detectCollaborationPlatform(userAgent, globals)` 返回以下之一：

- `WECHAT_ENTERPRISE`：UA 含 `wxwork`；
- `DINGTALK`：UA 含 `dingtalk`；
- `FEISHU`：UA 含 `lark` 或 `feishu`；
- `H5`：移动端浏览器但不是已知协同容器；
- `BROWSER`：桌面/普通浏览器。

识别仅用于能力选择和展示，不用于授权。

### 3.2 扫码入口

```js
scan({
  platform?,
  timeoutMs?: 30000,
  scanner?: function,
  globals?: object,
}) => Promise<{
  value: string,
  type: string,
  platform: string,
}>
```

- `scanner` 为宿主显式注入的优先适配器，签名为 `({ platform, timeoutMs }) => Promise<unknown> | unknown`；
- 未注入时，企业微信尝试 `globals.wx.scanQRCode({ needResult: 1, success, fail, complete })`；
- 其它平台只允许宿主注入适配器或浏览器原生能力；本阶段不动态加载第三方 SDK、不拼接授权凭据；
- `timeoutMs` 限制为 1000～60000，默认 30000；
- 同一调用只完成一次，晚到回调不得再次 resolve/reject。

### 3.3 结果归一化

接受以下常见形态：

- 非空字符串：作为 `value`；
- `{ resultStr: '...' }`；
- `{ codeString: '...' }`；
- `{ value: '...', type: '...' }`；
- 企业微信回调 `{ resultStr, scanType }`，其中 `scanType` 映射为 `type`。

结果必须是去除首尾空白的字符串，长度限制为 1～2048；空值或超长返回 `SCAN_INVALID_RESULT`。不得解析扫码值中的身份字段并写入可信上下文。

### 3.4 错误码

| code | 语义 |
|---|---|
| `SCAN_UNSUPPORTED` | 当前平台没有可用扫码能力 |
| `SCAN_TIMEOUT` | 在超时时间内没有结果 |
| `SCAN_CANCELLED` | 用户主动取消扫码 |
| `SCAN_INVALID_RESULT` | 回调结果缺少有效业务值或超过长度上限 |
| `SCAN_FAILED` | 宿主 SDK 返回其它失败 |

错误对象只包含 `code`、可选 `message` 和 `platform`，不得包含 URL、凭据、原始响应或表单快照。

## 4. 表单集成

- 仅当字段存在启用的 `SCAN_COMPLETE` 规则时，`AiFormItem` 才显示紧凑“扫码”入口；
- 点击入口后调用 `context.scanField?.(field)`，成功时由组件通过 `update:value` 写入扫码 `value`，再调用 `dispatchFieldEvent('SCAN_COMPLETE', field, scanContext)`；
- `scanContext` 只包含 `{ scan: { value, type, platform } }`，字段事件参数映射仍受设计态白名单限制；
- 扫码值写入后沿用现有字段变化和结果回填机制；扫码失败显示 `SCAN_*` 对应受控文案，不自动重试；
- disabled/readonly 字段不允许发起扫码；查询中字段可以继续由运行时取消旧请求并隔离过期响应；
- Enter 触发的 `SCAN_COMPLETE` 保持不变，context 中没有 SDK 信息时仍可正常查询。

## 5. 安全与兼容性

- 容器识别、扫码结果和路由参数只是非可信输入；服务端不接受客户端传入的 `userId`、`tenantId`、`activeOrgId` 作为身份覆盖；
- 不新增数据库表、外部业务接口或 Secret 配置；
- 旧表单没有 `SCAN_COMPLETE` 时行为不变；没有宿主扫码适配器的普通浏览器仍可使用手持扫码枪 Enter；
- 任何非法规则在现有字段事件运行时中失败关闭；平台错误不暴露底层 SDK 错误详情。

## 6. 非本阶段范围

- 手机号查询会员、条码查询商品、静态码单号查询收款的具体接口；
- 企业微信通讯录、userid 映射或登录授权协议改造（现有 collaboration 模块继续复用）；
- 服务端接收和解析完整表单快照；
- 离线草稿、冲突重放和业务写入命令。

## 7. 完成结论

- 2026-08-10：新增通用协同容器运行时，支持企业微信 `wx.scanQRCode`、宿主注入 scanner、平台识别、结果归一化和 `SCAN_*` 错误码；未动态加载 SDK 或新增凭据入口。
- 2026-08-10：`AiForm`/`AiFormItem` 在存在 `SCAN_COMPLETE` 规则时提供扫码入口，成功仅回填源字段并把最小扫码上下文交给既有字段查询运行时；扫码枪 Enter 继续兼容。
- 2026-08-10：19 个前端定向测试通过，目标 ESLint 0 errors（保留既有 warning），前端生产构建成功，`git diff --check` 通过。
