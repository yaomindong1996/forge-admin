# H5 钉钉风格改版 Execution Log

## 2026-10-06 提案

- 读取 H5 页面、主题令牌、底部导航、设计契约测试、归档变更 `h5-mobile-workbench-refactor`。
- 从用户提供的钉钉截图取色（`sips` 转 BMP 后逐像素采样），结果记录在 spec 2.5。
- 确认 `/system/user/page` 受接口权限和数据权限约束，不能作为通讯录接口。
- 用户已确认：五页签（消息 / 待办 / 工作台 / 通讯录 / 我的）；先做全部页面改版和通讯录，发起审批与公告放到后续变更；主色使用钉钉蓝。
- 后端服务未启动，本轮没有截图现有页面；需要登录的页面只做代码审阅。
- 用户确认通讯录口径：登录即可见、仅当前租户、不写迁移；手机号和邮箱完整展示，支持拨打和复制。

## 2026-10-06 /apply

### 环境

- Node v20.19.0，`npx -y pnpm@9 install` 安装 H5 依赖。
- 本机没有 JDK 和 Maven，后端编译和后端测试未执行。

### 基线

- `node --test src/utils/__tests__ src/store/modules/__tests__`：89 通过，1 失败。失败项是改版前就存在的，不是本次引入的。

### 实施

- 阶段一：令牌改为钉钉色板（主色 #0066FF、页面背景 #F2F1F6、卡片无边框圆角 16）；新增 6 组图标色板；新增 `store/modules/badge.js`；`AiTabBar` 改为五项悬浮胶囊并显示红点角标；新增 `AiTabHeader`；10 个公共组件换肤。
- 用脚本批量把页面和组件里的旧令牌、rpx 字号换成新令牌和 px，共 61 个文件，之后逐页人工校对。
- 阶段二：工作台、消息、待办列表、待办详情（只改 `TodoTaskSummary`、`TodoFlowTrace` 和 `todo-detail.scss`，`todo-detail.vue` 仍为 862 行）、我的、登录、应用承接页、低代码运行页、组件演示页。
- 消息改为页签页，全局不再 `navigateTo('/pages/message/index')`；`switchTab` 不能带参数，待办页签筛选通过 `utils/tab-handoff.js` 一次性存储传递。
- 阶段三：后端 `SysContactsController`、`ISysContactsService`、`SysContactsMapper(.xml)`、4 个 VO、`ContactMemberQuery`；H5 新增 4 个 API 方法、`utils/contacts.js`、`useContactMembers`、`ContactAvatar`、`ContactMemberList`，以及通讯录首页、组织架构页、成员详情页。

### 测试

- 契约测试按 spec 第 7 章改写：令牌、五页签导航、工作台布局、消息行样式、消息不再用 `navigateTo`、通讯录接口和拨打按钮守卫。`pages.json` 带 `//` 注释，测试里加了去注释的 `readPagesJson()`。
- 第一次运行：97 通过，6 失败。失败原因是旧断言还按原卡片样式写，以及 `message-flow-navigation.test.js` 要求 `onShow` 首行是 `await loadTasks({ reset: true })`。修复方式：把页签参数读取放到更早注册的另一个 `onShow`，该测试本身未改。
- 新增 `contacts.test.js`（6 条）。
- 最终：`node --test src/utils/__tests__ src/store/modules/__tests__` 105 通过，0 失败。
- `npx -y pnpm@9 build:h5`：构建成功。出现一条 Vite 提示：`badge.js` 动态导入 `@/api`，而它已被静态导入。这是有意为之，让 store 能在 Node 中单测，不影响产物。
- 后端新增 `SysContactsMapperContractTest`（租户、成员状态、禁止 `${}`）和 `SysContactsServiceImplTest`（`pageSize` 上限、关键字处理、VO 字段白名单）。**未执行**，需在有 JDK 的环境运行：`cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests -Dtest='SysContacts*' -Dsurefire.failIfNoSpecifiedTests=false`。
- 租户隔离、停用和已删除用户不返回，这两项需要数据库，目前只有 XML 契约断言覆盖，待用户联调确认。

### 预览

- `npx -y pnpm@9 dev:h5`（http://localhost:3009），浏览器模拟 390×844，`localStorage.default_auth` 写入假 token，后端未启动。
- 登录页、工作台、通讯录、我的、待办截图均按新样式渲染；需要接口的区块显示加载失败或空状态，这是预期结果（“安全通道初始化失败”“成员加载失败”）。
- 待办页第一次截图是空白，原因是按需编译还没完成；等 `.todo-page` 出现后重新截图正常。
- 预览结束后已停止开发服务器，3009 端口已释放。
