# H5 移动办公风格改版 Spec

> 变更名：`h5-mobile-redesign`
> 状态：`confirmed`（第 8 章待澄清项已全部确认，可进入 /apply）
> 创建日期：2026-10-06
> 涉及：H5 设计契约测试改写（有意推翻归档变更 `h5-mobile-workbench-refactor` 的部分约定）、新增通讯录只读接口（成员信息可见范围需人工审查）

## 1. 背景与目标

### 1.1 背景

用户反馈 H5 端界面仍有问题，要求按主流移动办公应用的风格改造全部页面，并提供了 4 张参考截图（工作台、应用分组、通讯录、应用市场）作为参照。

上一轮 `h5-mobile-workbench-refactor`（已归档）把 H5 收敛成“单一蓝色 + 灰白卡片 + 三个页签”，这些约定写进了 `forge-h5-ui/src/utils/__tests__/console-design-system.test.js`。新风格要求彩色应用图标、五个页签、红点角标、页签页自绘顶栏，和现有约定直接冲突。本变更有意改写这些约定，不是绕过测试。

### 1.2 已确认的决策

| 决策 | 结论 |
|------|------|
| 底部导航 | 消息 / 待办 / 工作台（居中）/ 通讯录 / 我的，待办和消息显示红点数字 |
| 范围拆分 | 本变更：全部页面改版 + 导航重构 + 通讯录；后续变更 `h5-office-essentials`：发起审批 + 公告 |
| 主色 | 从参考截图取色得到的品牌蓝作为默认主色，保留租户品牌覆盖能力 |

### 1.3 目标

1. 用截图取色结果重建 H5 设计令牌，所有页面和公共组件只引用令牌。
2. 底部导航改为悬浮胶囊式五页签，待办和消息显示未读数。
3. 消息从二级页面升级为页签页。
4. 新增通讯录：组织架构浏览、成员搜索、成员详情。
5. 全部 10 个现有页面按新风格改版，业务逻辑和接口协议不变。
6. 改写设计契约测试，新约定可被自动检查。

### 1.4 不做

- 发起审批、公告：放到后续变更 `h5-office-essentials`。
- 考勤打卡、请假、出差、日报等业务应用：属于业务范畴，建议以后用低代码应用 + 流程模板提供，不写进框架。
- 不引入新的图标库或 UI 依赖；继续用 `static/icons/ai-icon/*.svg`（291 个线性图标）+ wot-design-uni。
- 不改审批动作、低代码运行时、登录认证的业务逻辑。
- 1024px 以上桌面布局只同步令牌，不重新设计。

## 2. 代码现状（Research Findings）

### 2.1 设计令牌与契约测试

- 令牌在 `forge-h5-ui/src/styles/theme.css`：主色 `#3b82f6`、页面底色 `#f4f5f7`、控件圆角 12px、卡片圆角 20px、弹层圆角 24px、控件高 44px。
- `console-design-system.test.js` 中与本变更冲突的断言：
  - 第 28–44 行：锁定整套色值和圆角；
  - 第 79–100 行：`pages.json` 不允许任何 `"navigationStyle": "custom"`；
  - 第 117–135 行：首页结构（`allMenuItems.value.slice(0, 3)`、`overview-list`、`feed-section`、`"overview apps" / "feed apps"` 网格）；
  - 第 213–232 行：三页签，禁止 `key: 'message'`、禁止 `ai-tabbar__badge`，禁止消息页进入 `tabBar`；
  - 第 320–355 行：消息卡片、我的页面的部分结构。
- 其余断言（44px 触控高度、单次登录、审批意见滚动、业务表单只渲染接口返回字段、`AiAuthImage` 重试等）与风格无关，**全部保留**。

### 2.2 导航

- `src/pages.json` 原生 `tabBar` 三项：`pages/index/index`、`pages/todo`、`pages/mine/index`；`custom: true` 由 `AiTabBar.vue` 自绘。
- `src/components/AiTabBar.vue`：固定白底 + 顶部分隔线，图标用遮罩着色，`uni.switchTab` 切换，没有角标。
- `src/utils/mobile-menu.js`：
  - `TAB_ROUTES` 三项，`STACK_ROUTES` 只有 `/pages/message/index`；
  - `MENU_ACCENTS` 四色循环（`#f43f5e`、`#10b981`、`#6366f1`、`#3b82f6`），和图标语义无关；
  - `MENU_ICON_RULES` 按菜单名称匹配语义图标。
- 消息页现在通过 `navigateTo` 打开。升级为页签页后，所有跳转到 `/pages/message/index` 的地方必须改为 `switchTab`，否则 uni-app 会报错。

### 2.3 页面清单（行数）

| 页面 | 文件 | 行数 | 样式文件 |
|------|------|------|----------|
| 登录 | `pages/login/index.vue` | 315 | `styles/login.scss` 301 |
| 首页（工作台） | `pages/index/index.vue` | 453 | `styles/home.scss` 498 |
| 消息列表 | `pages/message/index.vue` | 470 | `styles/message.scss` 329 |
| 消息详情 | `pages/message/detail.vue` | — | 页内样式 |
| 待办列表 | `pages/todo.vue` | 339 | `styles/todo.scss` 306 |
| 待办详情 | `pages/todo-detail.vue` | 862 | `styles/todo-detail.scss` 194 |
| 我的 | `pages/mine/index.vue` | 740 | `styles/mine.scss` 449 |
| 应用承接页 | `pages/app-entry.vue` | — | 页内样式 |
| 低代码运行页 | `pages/lowcode-runtime.vue` | 576 | `styles/lowcode-runtime.scss` |
| 组件演示页 | `pages/demo/loading/index.vue` | 632 | 页内样式 |

所有页面都低于 1000 行。`todo-detail.vue`（862 行）只改样式和子组件，禁止继续堆逻辑。

### 2.4 通讯录可用的后端能力

- H5 已对接 `getOrgTree`（`/system/org/tree`）和 `getUserPage`（`/system/user/page`），但只用在待办详情的转办选人上。
- **不能直接复用 `/system/user/page`**：
  - 这是管理端接口，会被 `ApiPermissionInterceptor` 按 `sys_resource` 接口权限拦截，没有用户管理权限的员工会收到 403；
  - 会叠加数据权限，员工可能只看到本部门；
  - 返回完整 `SysUser`，手机号、邮箱、身份证经 `SensitiveDataUtil` 脱敏，无法拨号。
- 用户与组织、岗位关系：`sys_user`（`real_name`、`avatar`、`phone`、`email`、`user_status`、`del_flag`）、`sys_user_tenant`（多租户成员关系，`status = 1` 有效）、`sys_user_org`、`sys_user_post` + `sys_post`。
- 鉴权方式：全局 Sa-Token 登录校验 + `ApiPermissionInterceptor`；接口**未登记**到 `sys_resource` 时只校验登录，登记后按角色授权。项目内 Controller 基本不使用 `@SaCheckPermission`。
- 最新迁移 `V1.0.208`；`V1.0.209` 已被 `plugin-foundation` 预留。

### 2.5 截图取色结果

用 `sips` 把截图转成 BMP 后逐像素采样（`/tmp/c.bmp`、`/tmp/w.bmp`）：

| 用途 | 采样值 | 采用值 |
|------|--------|--------|
| 主色（实心图标、组织头像） | `#0066ff`、`#036ffd` | `#0066FF` |
| 浅主色底（邀请成员条） | `#e8f1ff` | `#E8F1FF` |
| 页面底色 | `#f2f1f6` | `#F2F1F6` |
| 卡片 / 底栏 | `#ffffff` | `#FFFFFF` |
| 一级文字、分组页签下划线 | `#121315`、`#171b1e` | `#171A1D` |
| 二级文字（页签标签） | `#7e7e7e` | `#747677` |
| 三级文字（说明文字） | `#9b9b9b` | `#A2A3A5` |
| 分隔线 | `#f2f2f2` | `#F0F1F2` |
| 箭头 | `#c1c1c1` | `#C1C3C6` |
| 底栏选中底块、次按钮底 | `#eaebef`、`#f2f3f5` | `#EBECF0` |
| 红点 | `#f54d07` | `#FF5219` |
| 图标浅蓝底 / 浅橙底 | `#cdeeff`、`#fae9bd` | 见 3.3 图标色板 |

## 3. 设计规范

### 3.1 令牌（`styles/theme.css`）

```css
--forge-color-primary: #0066ff;
--forge-color-primary-soft: #e8f1ff;
--forge-color-danger: #ff5219;
--forge-text-primary: #171a1d;
--forge-text-secondary: #747677;
--forge-text-tertiary: #a2a3a5;
--forge-border: #f0f1f2;
--forge-page-bg: #f2f1f6;
--forge-surface: #ffffff;
--forge-surface-muted: #ebecf0;
--forge-radius-control: 10px;
--forge-radius-card: 16px;
--forge-radius-popup: 16px;
--forge-radius-icon: 14px;
--forge-control-height: 44px;
--forge-space-page: 12px;
--forge-shadow-float: 0 4px 16px rgba(23, 26, 29, 0.08);
```

- H5 目前没有租户主题色覆盖入口（`appStore.primaryColor` 未应用到 CSS 变量），主色固定为品牌蓝；租户主题色另行立项。
- 卡片默认无阴影，靠白底与页面底色区分；只有悬浮底栏、弹层使用 `--forge-shadow-float`。
- 字号：页面标题 20px 加粗，卡片标题 16px，正文 15px，说明 13px，底栏标签 10px。

### 3.2 通用版式

- **页签页顶栏**（消息、待办、工作台、通讯录、我的）：自绘，内容依次为租户头像（圆角 10px）、页面标题、租户名副标题，右侧放圆角搜索框和一个功能图标。顶部留出状态栏安全区。工作台顶栏下方有浅蓝到页面底色的渐变。
- **二级页面**：继续使用原生导航栏，白底黑字。
- **列表行**：左侧 24px 线性图标（语义色），中间标题和说明，右侧附加文字加箭头；分隔线从文字起始处开始，不贯穿到最左。
- **分组页签**：选中项文字加粗、颜色 `#171A1D`，下方 3px 深色短横线；未选中项为二级文字色。
- **按钮**：主按钮主色实心；次按钮 `#EBECF0` 底、主色文字（参照截图“管理”按钮）；危险操作红色文字。

### 3.3 应用图标

- 48px 方块，圆角 14px，浅色底 + 同色系 24px 图标。
- 色板 6 组（浅底 / 图标）：
  - 蓝 `#DDF0FF / #0066FF`
  - 橙 `#FFF0D6 / #FD8838`
  - 绿 `#DDF6E8 / #12B76A`
  - 紫 `#EEE8FF / #7A5AF8`
  - 青 `#D9F5F7 / #0BA5B5`
  - 红 `#FFE4E0 / #F04438`
- 颜色按语义分配：`mobile-menu.js` 的 `MENU_ICON_RULES` 每条规则带一个色调（审批 → 蓝、财务 → 橙、人员 → 绿、报表 → 紫、文件 → 青、印章 → 红……）；没有命中规则的菜单按菜单 ID 哈希取色，保证同一个应用在不同位置颜色一致。替换现有的 `MENU_ACCENTS` 循环取色。

### 3.4 底部导航

- 悬浮胶囊：左右边距 12px，距底部 8px 加安全区，高 58px，全圆角，白底，`--forge-shadow-float`。
- 五项：消息、待办、工作台、通讯录、我的；每项 22px 图标加 10px 标签。
- 选中项：图标和文字用一级文字色 `#171A1D`，外面加 `#EBECF0` 圆角底块（参照截图里选中“通讯录”的样子）；未选中为二级文字色。
- 角标：红底白字，超过 99 显示 `99+`；为 0 时不显示。
- 页签页内容区底部留出底栏高度，避免被遮挡。

## 4. 导航与状态

### 4.1 页面路由

- `pages.json` 的 `tabBar.list` 改为五项：
  - `pages/message/index`
  - `pages/todo`
  - `pages/index/index`
  - `pages/contacts/index`
  - `pages/mine/index`
- 五个页签页设置 `"navigationStyle": "custom"`，其余页面保留原生导航栏。
- 新增二级页面：`pages/contacts/org`（组织浏览）、`pages/contacts/member`（成员详情）。
- `mobile-menu.js`：`TAB_ROUTES` 改为五项，`STACK_ROUTES` 去掉消息页。
- 所有跳转消息列表的代码改为 `uni.switchTab`。目前已知入口：首页铃铛和“最新提醒”的“全部”；实现时全局搜索 `/pages/message/index` 逐个确认。

### 4.2 角标状态（Pinia）

- 新建 `src/store/modules/badge.js`（`useBadgeStore`），状态包括 `todoCount`、`unreadCount`，方法包括 `refresh()`、`setTodoCount()`、`setUnreadCount()`。
- 待办数通过 `api.getTodoTasks` 以 `pageSize: 1` 查询取 `total`；未读数取自 `api.getUnreadMessageCount`。
- 刷新时机：每个页签页 `onShow`；审批动作成功后；消息标为已读或全部已读后。
- 并发请求合并为一个进行中的 Promise，避免切页签时重复请求。
- `AiTabBar` 直接读 store，不经过 props 传递。

## 5. 页面改版

| 页面 | 改版要点 |
|------|----------|
| 工作台 `index/index` | 页签页顶栏 + 浅蓝渐变；概览卡片（待办、未读、我发起，点击跳到对应页签）；“常用应用”4 列宫格，最多 7 个加“全部”；“分组应用”卡片按菜单分组切换页签，5 列宫格；“全部应用”弹层保留搜索；去掉“最新提醒”（已有消息页签）；保留下拉刷新和骨架屏 |
| 消息 `message/index` | 页签页顶栏；分类页签用下划线样式；列表行为左侧分类图标方块、标题、右上时间、摘要、未读红点；保留筛选弹层、全部已读、点击跳转审批 |
| 消息详情 `message/detail` | 白色卡片，标题、分类、时间、正文分区；正文排版统一 |
| 待办 `todo` | 页签页顶栏；待处理 / 已处理 / 我发起 三个下划线页签带数量；卡片为申请人头像、标题、当前节点、时间、状态标签；保留认领后直接进入详情的逻辑 |
| 待办详情 `todo-detail` | 顶部摘要卡片（申请人头像、标题、橙色“当前节点”提示条；任务数据没有统一的状态字段，不做状态章）；表单卡片；流程时间线改为移动端样式（头像圆点、竖线、状态色文字）；底部固定操作栏：“同意”主按钮、“拒绝”次按钮、“更多”；所有审批逻辑和校验不变 |
| 通讯录（新增） | 见第 6 章 |
| 我的 `mine/index` | 资料卡片（头像、姓名、账号、当前组织）；分组列表：个人资料、切换组织、修改密码、关于；退出登录单独一张卡片；保留头像上传和现有开关 |
| 登录 `login/index` | 白底，品牌 Logo 和产品名；输入框为浅灰底无边框；主按钮通栏；企业微信免登加载态同步新风格；保留背景图契约和单次登录逻辑 |
| 应用承接页 `app-entry` | 居中应用图标方块（3.3 色板）、标题、说明、返回工作台按钮 |
| 低代码运行页 `lowcode-runtime` | 列表卡片、筛选栏、详情表单跟随新令牌；`LowcodeRuntimeList` 同步样式 |
| 组件演示页 `demo/loading` | 用新令牌重绘，作为设计规范的可视化样例 |

公共组件同步：`AiTabBar`、`AiCard`、`AiCell`、`AiCellGroup`、`AiTabs`、`AiSearchBar`、`AiButton`、`AiPopupSheet`、`AiEmpty`、`AiListSkeleton`、`AiTag`、`HomeWorkspaceSkeleton`、`TodoTaskSummary`、`TodoFlowTrace`。

## 6. 通讯录

### 6.1 页面

- **通讯录首页**（页签页）：
  - 顶栏与搜索框，搜索直接查成员；
  - 组织卡片：租户 Logo、租户名、成员总数；
  - 卡片下的入口：组织架构、我的部门（未分配部门时隐藏）；
  - 无关键字时展示“全部成员”分页列表，有关键字时替换为搜索结果。
- **组织浏览** `contacts/org?orgId=`：
  - 顶部面包屑，可点击回到上级；
  - 先列子部门（带人数和箭头），再列本部门成员；
  - 成员列表分页加载。
- **成员详情** `contacts/member?userId=`：
  - 大头像、姓名、部门、岗位；
  - 手机号和邮箱行，可复制；
  - 手机号完整可见时显示“拨打”按钮，调用 `uni.makePhoneCall`。

### 6.2 后端接口

新增 `SysContactsController`，路径前缀 `/system/contacts`，放在 `forge-plugin-system`。H5 通过 app-server 访问（`VITE_HTTP_PROXY_TARGET` 指向 8583）。

| 接口 | 说明 |
|------|------|
| `GET /system/contacts/summary` | `tenantName`、`memberCount`（有效成员总数）、`myOrgId` / `myOrgName`（当前用户主部门）；Logo 沿用 H5 品牌资源 `appStore.brandLogoUrl` |
| `GET /system/contacts/orgs?parentId=` | 子部门列表（`id`、`orgName`、`memberCount`，人数含全部下级部门、按成员去重），不传 `parentId` 返回顶级部门 |
| `GET /system/contacts/members?orgId=&keyword=&pageNum=&pageSize=` | 成员分页；`orgId` 只查直属成员；`keyword` 匹配姓名和账号 |
| `GET /system/contacts/members/{userId}` | 成员详情 |

- 返回 `ContactMemberVO`：`userId`、`realName`、`avatar`（文件 ID）、`orgNames`、`postNames`、`phone`、`email`。不返回账号状态、身份证、密码等管理字段。
- 只查当前租户内 `sys_user_tenant.status = 1`、`sys_user.user_status` 为启用、`del_flag = 0` 的用户。
- SQL 全部写在新的 `SysContactsMapper.xml`，**不走数据权限改写**（通讯录按租户可见，不是管理视图）；租户隔离靠 `TenantLineInnerInterceptor` 和显式 `tenant_id` 条件。
- `pageSize` 上限 50；`keyword` 去空格后为空则不过滤，超过 50 字截断。H5 端关键字为空时不发搜索请求。
- 四个接口都是 GET，H5 不设置 `encrypt: true`（请求加密只作用于请求体），响应由拦截器统一解密。
- `sys_user` 同时受租户拦截器（`u.tenant_id = 当前租户`）和 `sys_user_tenant` 成员关系约束，与管理端用户列表的可见范围一致。
- **可见范围**：登录即可见。接口不登记到 `sys_resource`，同租户有效成员互相可见。
- **手机号和邮箱**：同租户成员之间完整返回，H5 提供拨打和复制。接口不打印手机号和邮箱日志，也不提供导出。

## 7. 设计契约测试改写

在 `console-design-system.test.js` 中**替换**而不是删除冲突断言，新断言覆盖：

1. 令牌：3.1 的色值、圆角、间距逐项断言。
2. 导航：
   - `tabBar.list` 恰好五项，且顺序正确；
   - `AiTabBar` 包含 `key: 'message'`、`'todo'`、`'home'`、`'contacts'`、`'mine'`；
   - 存在 `ai-tabbar__badge`，并读取 `useBadgeStore`。
3. `pages.json`：只有五个页签页使用 `"navigationStyle": "custom"`；`message/detail`、`todo-detail` 等二级页面不使用。
4. 工作台：
   - 有概览卡片、常用应用（`slice(0, 7)` 加“全部”）、分组应用页签；
   - 不再有 `feed-section`；
   - 4 列和 5 列宫格断言。
5. 图标色板：`mobile-menu.js` 不再包含 `MENU_ACCENTS` 循环；同一个菜单 ID 多次计算颜色结果一致。这一条在 `mobile-menu.test.js` 中覆盖。
6. 跳转：源码中不再有 `navigateTo` 指向 `/pages/message/index`。
7. 通讯录：三个页面存在；`api/index.js` 有四个 `/system/contacts/*` 方法；成员详情调用 `uni.makePhoneCall` 时以“手机号完整可见”为前提。

第 2.1 节列出的其余断言保持原样。保留 `#4266f7|#165dff|#1f5fbf` 禁用色断言。

## 8. 待澄清（已确认）

1. 通讯录可见范围：**登录即可见**，同租户有效成员互相可见，本变更不新增迁移脚本。
2. 手机号和邮箱：**同租户成员之间完整显示**，可以拨打和复制。
3. 后续变更 `h5-office-essentials`（发起审批 + 公告）：本变更完成、用户验收后再写 spec。

## 9. 风险与回滚

- **契约测试改写**：只替换第 2.1 节列出的冲突断言，评审时逐条对照。
- **消息页改为页签页**：遗漏的 `navigateTo` 会在运行时报错。用第 7 章第 6 条的源码断言兜底。
- **自绘顶栏在企业微信等第三方 App 内嵌浏览器里**：宿主自带标题栏，会出现双标题。用 `uni.getSystemInfoSync().statusBarHeight` 和宿主环境判断决定是否渲染顶栏标题行；企业微信内只保留搜索和功能区。
- **通讯录信息暴露**：同租户成员能看到彼此完整手机号和邮箱，这是用户确认的产品决策。接口只返回展示字段，不跨租户，不返回停用和已删除用户。
- **回滚**：回滚相关提交即可。后端只新增接口和 Mapper，没有数据库变更，不影响既有功能。

## 10. 第二轮视觉修正（用户验收反馈）

反馈：通讯录出现两个 logo；空状态插画自带底色，与页面底色不一致；审批表单可编辑与只读字段都是灰底，分不清；消息列表单调；工作台部分应用图标空白。

1. **彩色应用图标**：用生图接口（`gpt-image-2-low`）生成一套 3D 风格彩色图标，放 `static/app-icons/*.png`（144px，透明底）。`mobile-menu.js` 的语义规则改为映射到图标键，未命中规则按菜单 ID 哈希取兜底图标；后端配置的线性图标（`i-*`、`ionicons5:*`）在移动端不再使用，避免外链图标加载失败出现空白块。后端配置的图片地址（`/static/`、`http(s)`）仍原样使用。`AiAppIcon` 传入图片时用 `<image>` 渲染，传入线性图标时保持原来的浅底 + 着色图标。
2. **空状态插画**：生成透明底插画（空数据、加载失败、无搜索结果），放 `static/illustrations/`；`AiEmpty` 按 `type`（`empty` / `error` / `search`）选图，默认 `empty`。
3. **通讯录**：`AiTabHeader` 增加 `showOrg`，为 `false` 时只显示页面标题；通讯录页有组织卡片，顶栏不再重复显示 logo 和租户名。成员加载失败时给出“重新加载”按钮。
4. **审批表单**：只读字段改为纯文本（无边框、无底色、一级文字色，空值用三级文字色 `-`）；审批面板内可编辑控件改为白底加描边，与灰色分组底区分。
5. **消息列表**：左侧改为彩色图标（审批、系统、公告、业务各一套）；已读消息标题和摘要降为二级文字色，未读加粗并带红点。

生图密钥只在本地生成时通过环境变量传入，不进入仓库；仓库只提交生成后的 PNG。

## 11. 第三轮：待办表单展示（用户验收反馈）

反馈：待办详情里的业务表单“有点乱”，希望接近主流移动办公应用的审批详情页。

现状问题：

- 白色“表单信息”卡片里再套浅灰分组卡片，卡中卡，层次多。
- 单行只读字段 `min-height: 88rpx` 再加每个字段 12px 间距，一屏只能看到四五个字段。
- 多行文本、单选、多选等只读字段没有走左右布局，标签在上、值在下，与相邻字段排版不一致。
- 子表每条记录的字段按纵向表单堆叠（`inline_grid` 移动端 `gap: 32rpx`），条目之间只有一条细线，看不出边界。
- 布局容器（栅格）在移动端还保留 `16rpx` 间距和 `18rpx` 外边距，字段节奏忽大忽小。

目标样式（只作用于审批表单面板 `FlowBusinessFormPanel`，移动端 ≤1023px）：

1. **分组扁平化**：主表分组去掉浅灰底和内边距，直接铺在“表单信息”白卡里；分组标题 15px 加粗，分组之间用细分隔线隔开。
2. **只读字段统一为描述列表**：除附件、图片、签名、子表等专用渲染器外，所有只读字段都用“左标签 78px、右值”的左右布局；标签二级文字色 14px，值一级文字色 15px，行高 22px，长文本在右侧换行。行内上下各 7px，无最小高度。空值保留三级文字色 `-`，不隐藏字段。
3. **可编辑字段**：单行控件保持左右布局，控件保持 44px 触控高度；多行文本等仍标签在上。白底描边不变，白卡上依旧可区分。
4. **子表条目卡片化**：每条记录是浅灰底圆角小卡片（`surface-subtle`，圆角 10px），卡片间距 8px；顶部“第 N 条”13px 二级文字；条目内字段沿用描述列表，标签列 72px。可编辑子表控件在灰卡上为白底。
5. **布局容器**：审批面板内的栅格、盒子容器在移动端去掉额外间距和外边距，由字段行统一控制节奏。

不改动：字段权限、数据结构、`LowcodeField` 的只读判定逻辑；PC 端（≥1024px）样式。`LowcodeField` 只新增一个只读行的样式类 `lowcode-field--readonly-row`，供审批面板做左右布局。

回滚：回滚本轮提交即可，只涉及样式和一个样式类。

## 12. 工作台轮播图（用户需求）

需求：工作台首页加一个简单轮播，两张图，提升观感。

1. **位置**：工作台顶栏下方、概览卡片之上；PC 端（≥1024px）放在左列顶部，右列分组应用不变。
2. **素材**：用生图接口生成两张 3D 风格横幅底图（右侧主体、左侧留白），裁成 2.2:1，导出 1098×499 JPG 放 `static/banners/`。图内不含文字，标题和说明用页面文字叠加，避免生成文字乱码。
3. **内容**（前端固定配置，不走后端）：
   - 移动审批：“移动审批，随时处理 / 待办、发起、催办一站完成”，按钮“发起审批”进入 `/pages/approval/start`。
   - 通讯录与公告：“找同事、看通知 / 通讯录与公告随手可查”，按钮“查看通讯录”切到通讯录页签。
4. **交互**：`swiper` 自动播放 4 秒、循环；底部左侧短条指示器；整张图可点。
5. **组件**：新增 `components/home/HomeBanner.vue`，工作台页只引用组件。

不做：后台可配置的轮播管理（需要接口和数据表，另起变更）。

回滚：删除组件、素材和工作台引用即可。

## 13. 刷新方式、小程序顶栏避让、工作台概览（用户验收反馈）

反馈：右上角刷新按钮别扭，统一改为下拉刷新；首页这类自绘顶栏的页面在小程序里顶部被挡；工作台“待我处理 / 未读消息 / 我发起的”样式普通。

1. **去掉刷新按钮，统一下拉刷新**
   - 去掉工作台顶栏刷新按钮、审批详情摘要卡片右上角刷新按钮。
   - 审批详情正文在 `scroll-view` 内滚动，页面级下拉不会触发；改用 `scroll-view` 自带下拉（`refresher-enabled`），下拉调用原 `refresh`。
   - 通讯录首页、组织架构页已有 `onPullDownRefresh` 处理，但 `pages.json` 未开启下拉，且正文同样在 `scroll-view` 内滚动，下拉从未生效；改为 `scroll-view` 自带下拉，复用原刷新函数。
   - 工作台、消息、待办、公告的页面级下拉保持不变。
2. **小程序顶栏避让胶囊按钮**
   - 现状：`AiTabHeader` 只加了状态栏高度，小程序右上角胶囊按钮与顶栏同一行，搜索框和右侧图标被胶囊盖住。
   - 方案：仅小程序（`#ifdef MP`）读取 `uni.getMenuButtonBoundingClientRect()`：
     - 顶栏行高 = `(胶囊 top - 状态栏高度) × 2 + 胶囊高度`，标题与胶囊垂直居中对齐；
     - 右内边距 = `窗口宽度 - 胶囊 left + 8px`，右侧工具区不进入胶囊区域。
   - H5 和 App 不受影响；接口异常时保持原有 56px 行高。使用 `AiTabHeader` 的工作台、消息、待办、通讯录、我的五个页签页统一生效。
3. **工作台概览改为三张浅色数据块**
   - 每块浅色底（待我处理蓝、未读消息橙、我发起的绿），左上角彩色小图标（复用 `static/app-icons` 的 approval / notice / file，图标主色与数据块底色一致），下方大号数字和标签，标签后带箭头表示可点。
   - 待我处理、未读消息大于 0 时数字为红色，保持原有提醒语义；超过 99 显示 99+。
   - 点击跳转与数据来源不变。

回滚：回滚本轮提交即可，没有接口和数据变更。

## 14. 内嵌宿主 App 时隐藏页面自带导航栏（用户需求）

需求：H5 挂在第三方移动办公 App 里时，宿主已经提供返回键和标题，页面自己的返回、标题栏全部去掉。

1. **识别规则**：抽出 `utils/embedded-host.js` 的 `isEmbeddedHost()`，沿用 `AiTabHeader` 现有 UA 判断（企业微信与另一款移动办公 App）。`AiTabHeader` 改为引用该函数，不再各写一份。
2. **原生导航栏**：仅 H5（`#ifdef H5`），应用创建时给 `<html>` 加 `forge-embedded-host` 类；全局样式据此隐藏 `uni-page-head`，并把 `--window-top` 置 0、`--forge-page-height` 改为 `100vh`，避免顶部留出 44px 空白或底部被裁。
3. **标题**：宿主读取 `document.title` 显示标题，uni-app 已按 `navigationBarTitleText` 设置，不需额外处理。
4. **自绘顶栏**：`AiTabHeader` 内嵌时已隐藏品牌和标题、只保留搜索和功能区，保持不变。`AiLayoutPage` 自绘导航默认关闭，无需处理。
5. **不影响**：普通浏览器、小程序、App。

验证：浏览器把 UA 改成宿主 App 的 UA，检查审批详情、公告等原生导航栏页面顶部无返回和标题、内容贴顶且底部不被裁；恢复普通 UA 后导航栏正常。

回滚：删除 `<html>` 类名设置和对应全局样式即可。
