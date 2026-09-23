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
9. 所有注册页面必须挂载统一 `AiFeedbackHost`；Toast、Notify、MessageBox、Prompt 和 ActionSheet 由 Wot Design Uni 实现，业务反馈适配器不得包含手写 DOM 或 uni 原生提示 API。
10. `FLOW_TODO` 消息必须从 `jumpUrl/taskId/task_id/bizKey` 稳定解析任务 ID；未读消息进入办理态，已读消息进入只读态，只有办理成功后才标记来源消息已读，普通消息仍可自动已读。
11. 待办页 `onShow` 必须重新请求当前作用域；办理完成后的回退使用统一兜底，不得依赖一定存在的上一页。

## P1 交互与兼容验证

1. 本地 H5 开发页可打开；Wot 输入适配层可输入、清空，按钮和表单样式无明显破坏。
2. 小程序构建产物使用 `uni.request` 适配器，并要求通过 `VITE_MP_API_BASE_URL` 提供绝对网关地址。
3. 上传、图片和签名在 H5 使用 Fetch/FormData，在小程序使用 `uni.uploadFile`。
4. 独立入口参数 `applicationId/appId/pageId/pageCode/configKey` 能透传到统一运行页。
5. 登录、首页、消息、待办、我的、审批详情和低代码运行页使用同一蓝白黑灰视觉体系，无渐变、装饰光斑、毛玻璃或持续入场动画。
6. 自定义底部导航为标准白色固定导航，具备图标、文字、选中态和底部安全区，不使用悬浮胶囊布局。
7. 所有生产页面 Vue SFC 不超过 800 行；页面滚动区与固定顶部/底部操作区边界正确。
8. 登录页账号、密码、验证码、提交按钮和工作区弹层使用 `Ai*`/Wot 组件；空表单提交能展示 Wot Toast，登录页在 390 × 844 视口下布局完整。
9. 审批意见、转办说明、低代码多行文本和数字输入使用 `Ai*`/Wot 适配组件；待办、消息和审批详情的视觉密度与底部操作区保持一致。

## 第八轮增量验证：统一反馈与登录页

1. 新增 `feedback-components.test.js`，静态锁定全部注册页面的反馈宿主、Wot 反馈组件集合、适配器禁用 API和登录页组件化结构。
2. 重新执行既有 38 项运行时测试与新增反馈测试，避免公共反馈宿主影响低代码及审批路径。
3. 重新执行 `pnpm build:h5` 与 `pnpm build:mp-weixin`，验证 Wot MessageBox/ActionSheet 在两端均可编译。
4. 使用 H5 开发页和 390 × 844 手机视口检查登录页，并触发一次空表单登录提示验证 Wot Toast 实际显示。
5. 执行反馈 API 残留扫描、生产页面行数检查和 `git diff --check`。

## 第九轮增量验证：消息与待办处理回路

1. 新增 `message-flow-navigation.test.js`，验证流程任务 ID 的多来源解析、Long ID 字符串语义、待办/只读模式和页面回退链路。
2. 验证 `FLOW_TODO` 详情打开时不会提前标记已读；普通消息仍按原逻辑自动已读，批量已读排除活动审批待办。
3. 验证待办页每次 `onShow` 刷新、审批提交后统一回退，并复用既有历史任务详情和只读表单协议。
4. 检查搜索、多行文本和数字输入均通过 `Ai*`/Wot 适配组件渲染，生产代码无原生 `input/textarea` 和 uni 原生反馈调用残留。
5. 重新执行定向单测、H5/微信小程序生产构建、390 × 844 组件交互冒烟、页面行数检查和 `git diff --check`。

## 第十轮增量验证：同步 main

1. 将最新 `origin/main` 合并到 `codex/mobile-lowcode-runtime-refactor`，确认无未解决冲突并执行暂存区空白检查。
2. 复跑 48 项 H5 运行时、审批、消息与统一反馈定向测试，确认主线更新未破坏移动端链路。
3. 重新执行 H5 与微信小程序生产构建；本轮不重复验证 main 自身已交付的管理端和后端功能。

## 第十一轮增量验证：火山方舟风格全站重构

1. 静态检查全局主色、灰阶、`6px` 圆角、`44px` 触屏高度、系统字体和无默认阴影约束。
2. 检查登录、首页、消息、待办、账户、审批详情、低代码运行页、独立入口与演示页仍挂载统一反馈宿主并复用 `Ai*` 组件。
3. 在 `390 × 844` 与 `1440 × 900` 视口验证移动单列、桌面分栏、固定操作区、滚动边界和登录页结构。
4. 复跑 53 项 H5 运行时/审批/消息/设计规范测试，执行 H5 与微信小程序生产构建及 `git diff --check`。

## 第十二轮增量验证：首页与待办工作台

1. 静态锁定 `AiField`、`AiSearchBar`、`AiSelect` 和 `AiDateTimePicker` 的垂直居中规则及 `44px` 触控高度。
2. 检查首页只有一个工作概览结构，常用应用与最新提醒层级明确，桌面端主区/侧栏和移动端单列断点保持稳定。
3. 检查待办列表不再挂载任务中转弹层；待签收任务点击调用既有签收接口后直接导航审批页，其他待办直接导航。
4. 复跑 56 项 H5 运行时、审批、消息和设计规范测试，执行 H5/微信小程序生产构建、移动视口输入控件验证及 `git diff --check`。

## 执行命令

```bash
cd forge-h5-ui
node --test \
  src/api/__tests__/runtime-api-contract.test.js \
  src/store/modules/__tests__/lowcodeRuntime.test.js \
  src/components/lowcode/__tests__/mobile-component-registry.test.js \
  src/utils/__tests__/business-task-form-adapter.test.js \
  src/utils/__tests__/flow-action-idempotency.test.js \
  src/utils/__tests__/feedback-components.test.js \
  src/utils/__tests__/message-flow-navigation.test.js \
  src/utils/__tests__/console-design-system.test.js \
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
