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
