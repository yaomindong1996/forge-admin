# 低代码企业协同容器与扫码测试规格

| ID | 场景 | 预期 |
|---|---|---|
| RUNTIME-01 | UA 识别企微、钉钉、飞书、H5、普通浏览器 | 返回稳定 platform，不以 platform 作为身份 |
| RUNTIME-02 | 字符串、resultStr、codeString、value/type | 归一化为 value/type/platform |
| RUNTIME-03 | 空值、非字符串、超长结果 | 返回 `SCAN_INVALID_RESULT` |
| RUNTIME-04 | 宿主 scanner 成功 | 优先使用注入 scanner，仅完成一次 |
| RUNTIME-05 | 企业微信 `wx.scanQRCode` 成功/失败 | 正确映射 success/fail 回调 |
| RUNTIME-06 | 用户取消、超时、不支持 | 分别返回 `SCAN_CANCELLED`、`SCAN_TIMEOUT`、`SCAN_UNSUPPORTED` |
| RUNTIME-07 | 晚到回调 | 不重复 resolve/reject |
| EVENT-01 | dispatch `SCAN_COMPLETE` 带上下文 | 只传 `scan.value/type/platform` 和规则声明参数 |
| UI-01 | 有扫码规则的可编辑字段 | 显示扫码按钮并回填字段 |
| UI-02 | 无扫码规则、只读或禁用字段 | 不显示或不可点击扫码入口 |
| UI-03 | 扫码失败 | 显示受控错误文案，不展示原始 SDK 响应 |
| UI-04 | 扫码枪 Enter | 继续分发 `SCAN_COMPLETE`，无 SDK 依赖 |
| SECURITY-01 | 浏览器 context 自报 userId/tenantId/activeOrgId | 不改变服务端可信身份，未映射字段不自动转发 |

验证分层：运行时纯函数/Vitest → AiFormItem 组件定向测试 → 目标 ESLint → 前端生产构建 → `git diff --check`。

