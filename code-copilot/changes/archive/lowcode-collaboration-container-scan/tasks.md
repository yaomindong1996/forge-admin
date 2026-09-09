# 任务清单

## 阶段 A：协议与运行时

- [x] A1 新增 `collaboration-runtime.js`，实现平台识别、扫码适配器选择、结果归一化、超时、取消和错误码。
- [x] A2 为企业微信 `wx.scanQRCode` 提供回调适配，并支持宿主注入 scanner；不动态加载 SDK。
- [x] A3 扩展 `field-event-runtime` 的 dispatch 入参，传递受控 `scan.value/type/platform` 上下文且保持旧调用兼容。

## 阶段 B：表单入口

- [x] B1 在 `AiFormItem` 为配置了 `SCAN_COMPLETE` 的字段显示扫码按钮，复用现有反馈和禁用规则。
- [x] B2 成功扫码后写入源字段并分发 `SCAN_COMPLETE`；失败展示稳定错误码映射；Enter 链路保持兼容。
- [x] B3 在 `AiForm` context 暴露统一 `scanField`，调用方可注入宿主 scanner，不向后端发送身份字段。

## 阶段 C：测试与文档

- [x] C1 补充运行时和组件 Vitest：识别、归一化、企微回调、取消/超时/不支持、扫码上下文和过期回调。
- [x] C2 更新 `spec.md`、`test-spec.md`、`execution-log.md` 和长期决策记录。
- [x] C3 执行目标 ESLint、Vitest、前端生产构建、`git diff --check`；不启动真实服务或外部系统。
