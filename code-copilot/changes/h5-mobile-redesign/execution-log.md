# H5 移动办公风格改版 Execution Log

## 2026-10-06 提案

- 读取 H5 页面、主题令牌、底部导航、设计契约测试、归档变更 `h5-mobile-workbench-refactor`。
- 从用户提供的参考截图取色（`sips` 转 BMP 后逐像素采样），结果记录在 spec 2.5。
- 确认 `/system/user/page` 受接口权限和数据权限约束，不能作为通讯录接口。
- 用户已确认：五页签（消息 / 待办 / 工作台 / 通讯录 / 我的）；先做全部页面改版和通讯录，发起审批与公告放到后续变更；主色使用品牌蓝。
- 后端服务未启动，本轮没有截图现有页面；需要登录的页面只做代码审阅。
- 用户确认通讯录口径：登录即可见、仅当前租户、不写迁移；手机号和邮箱完整展示，支持拨打和复制。

## 2026-10-06 /apply

### 环境

- Node v20.19.0，`npx -y pnpm@9 install` 安装 H5 依赖。
- 本机没有 JDK 和 Maven，后端编译和后端测试未执行。

### 基线

- `node --test src/utils/__tests__ src/store/modules/__tests__`：89 通过，1 失败。失败项是改版前就存在的，不是本次引入的。

### 实施

- 阶段一：令牌改为新色板（主色 #0066FF、页面背景 #F2F1F6、卡片无边框圆角 16）；新增 6 组图标色板；新增 `store/modules/badge.js`；`AiTabBar` 改为五项悬浮胶囊并显示红点角标；新增 `AiTabHeader`；10 个公共组件换肤。
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

## 第二轮视觉修正（spec 第 10 章，T19–T24）

### 素材生成

- 生图接口：OpenAI 兼容的 `/v1/images/generations`，模型 `gpt-image-2-low`；限流或失败时最多重试 3 次，第 4 次改用 `gemini-2.5-flash-image`（走 `/v1/chat/completions`，返回 base64）。透明背景参数被接口限流拒绝，所以改为白底生成后本地去底。
- 密钥只通过环境变量 `IMG_API_KEY` 传给本地脚本，没有写入仓库。生成脚本和原图都放在 `/tmp`，不提交。
- 共生成 22 个应用图标和 3 张插画。`seal`（红色实心底）和 `more`（深灰）与整套浅色底不一致，加强提示词后重新生成。
- 后处理（Pillow）：从边缘泛洪去掉白底，边缘按与白色的距离做 alpha 过渡，裁到内容边界后补成正方形，再量化为 256 色。图标 144px，共 120KB；插画 360px，共 36KB。
- 已在页面底色和白色卡片上分别预览，没有白边。

### 代码

- `mobile-menu.js`：语义规则改为映射到 `/static/app-icons/<key>.png`。后台配置的 `i-*`、`ionicons5:*` 线性图标不再使用，只有图片地址原样保留。这是“测试”“12”等应用出现空白图标块的原因：外链图标加载失败。
- `AiAppIcon`：图片图标用 `<image>` 渲染。工作台、全部应用入口、承接页、通讯录入口、消息分类、公告入口都换成彩色图标。
- `AiEmpty`：新增 `type`（`empty` / `error` / `search`），使用透明底插画；删除不再引用的 `static/images/no-data.png`。
- 通讯录：顶栏 `:show-org="false"`，不再出现两个 logo；成员和搜索失败时显示“重新加载 / 重新搜索”按钮；概要接口失败时显示“人数暂不可用”，不再显示“共 0 人”。
- 审批表单：`LowcodeField` 只读值改为纯文本，空值用三级文字色；`FlowBusinessFormPanel` 内可编辑控件改为白底 + `#e3e4e6` 描边。
- 消息列表：已读标题和摘要降为二级/三级文字色；只有“未读”页签用红色角标，其它页签数量改为灰字。
- 顺带修复两个样式问题：
  - 消息页页签下划线不显示：`button::after { display: none }` 覆盖了激活态。
  - 工作台分组页签的下划线显示在文字上方：uni-app 按钮自带 `::after { top: 0 }`。

### 测试与构建

- 改了 `mobile-menu.test.js` 中锁定旧图标路径的断言，按新行为断言，没有放宽。另外新增 4 条：线性图标改走语义图标、外链图片原样保留、自定义 png 原样保留、兜底图标格式。
- `console-design-system.test.js` 新增 2 条契约：素材文件存在、通讯录顶栏、只读纯文本、可编辑白底。
- 第一次运行失败 1 条：既有断言锁定消息行 `min-height: 72px` 和分隔线 `left: 72px`。处理方式是保持 72px，消息图标沿用 44px，没有改动该测试。
- `node --test src/utils/__tests__ src/store/modules/__tests__`：175 通过，0 失败。
- `build:h5` 通过，只有已知的 2 条 `badge.js` 动态导入提示。
- `build:mp-weixin` 通过，有 4 条 Circular chunk 提示，与改动前相同；产物包含 22 个图标和 3 张插画。
- H5 工程没有 ESLint 配置和 lint 脚本，未执行 lint。

### 预览（390 宽，后端未启动，数据通过 Pinia / setupState 注入）

- 通讯录：顶栏只剩标题，失败态插画透明，“重新加载”按钮可见。
- 工作台：“测试”“12”“店铺”等原本空白或线性图标的应用都显示彩色图标；分组页签的下划线回到文字下方。
- 消息：未读行加粗并带红点，已读行变灰；“全部”页签下划线恢复。
- 审批详情：只读字段为纯文本，可编辑字段为白底描边输入框。
- 预览结束后已停止开发服务器，3009 端口已释放。

### 待用户确认

- 通讯录“成员加载失败”：前端页面和 SQL 字段已核对无误，H5 请求的是 `forge-app-server`（8583），该服务已依赖 `forge-plugin-system`。判断是后端没有用新代码重新构建和重启，需重新执行 `mvn install` 并重启 app-server 后验证。
