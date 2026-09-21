# 移动端低代码多端运行时增量测试计划

> 变更：`mobile-lowcode-multiplatform-runtime`
> 验证范围：仅 `forge-h5-ui` 与本变更文档；后端协议和数据库未修改。

## P0 必须通过

1. 组件注册表覆盖管理端新版低代码设计器、页面画布和旧版表单设计器公开的组件键。
2. 未知组件、动态 Vue 和小程序 iframe 不得回退成可编辑输入框。
3. 审批字段权限覆盖数组、JSON 字符串、新旧权限键优先级、主子表作用域和动态数组字段。
4. H5 与微信小程序生产构建通过。
5. `git diff --check` 通过，工作分支保持 `codex/mobile-lowcode-runtime-refactor`。
6. 移动端 API 与后端 Controller 的低代码、选择器和审批路由契约保持一致，不引入 `/mobile` 或 `/h5` 专用协议。
7. 审批主子表必须展示全部可读子表，并按节点新增、修改、删除权限构造保存载荷；Long ID 保持字符串。
8. 流程写操作必须携带成对的 `idempotencyKey` 与 `requestDigest`，相同载荷摘要稳定、不同动作摘要不同。

## P1 交互与兼容验证

1. 本地 H5 开发页可打开；Wot 输入适配层可输入、清空，按钮和表单样式无明显破坏。
2. 小程序构建产物使用 `uni.request` 适配器，并要求通过 `VITE_MP_API_BASE_URL` 提供绝对网关地址。
3. 上传、图片和签名在 H5 使用 Fetch/FormData，在小程序使用 `uni.uploadFile`。
4. 独立入口参数 `applicationId/appId/pageId/pageCode/configKey` 能透传到统一运行页。
5. 登录、首页、消息、待办、我的、审批详情和低代码运行页使用同一蓝白黑灰视觉体系，无渐变、装饰光斑、毛玻璃或持续入场动画。
6. 自定义底部导航为标准白色固定导航，具备图标、文字、选中态和底部安全区，不使用悬浮胶囊布局。
7. 所有生产页面 Vue SFC 不超过 800 行；页面滚动区与固定顶部/底部操作区边界正确。

## 执行命令

```bash
cd forge-h5-ui
node --test \
  src/api/__tests__/runtime-api-contract.test.js \
  src/store/modules/__tests__/lowcodeRuntime.test.js \
  src/components/lowcode/__tests__/mobile-component-registry.test.js \
  src/utils/__tests__/business-task-form-adapter.test.js \
  src/utils/__tests__/flow-action-idempotency.test.js \
  src/utils/__tests__/lowcode-runtime.test.js \
  src/utils/__tests__/uni-adapter.test.js \
  src/utils/__tests__/mobile-selector-runtime.test.js \
  src/utils/__tests__/machine-code.test.js

./node_modules/.bin/uni build
./node_modules/.bin/uni build -p mp-weixin

cd ..
git diff --check
```

## 暂不执行

- 不启动 Admin、App、Flow 服务，不写数据库；后端协议未变，且用户偏好由用户完成真实服务联调。
- 不执行真实审批、上传和小程序合法域名请求；需要可用账号、已发布低代码配置、Flow 任务和 HTTPS 网关。
- 不删除 uview-plus；本阶段允许与 Wot Design Uni 并存，待全站页面回归后单独移除。
