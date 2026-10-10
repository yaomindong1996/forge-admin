# H5 钉钉风格改版 Tasks

> 依赖：spec.md 第 8 章待澄清项确认。每个任务完成后执行 test-spec.md 对应检查，并追加到 execution-log.md。

## 阶段一：基础设施

- [x] T1 设计令牌
  - 改写 `styles/theme.css`、`uni.scss` 中的令牌和 wot 覆盖变量，同步修改 `pages.json` 里的 `globalStyle` 背景色。
  - 契约测试第 7 章第 1 条同步改写。
- [x] T2 图标色板
  - `utils/mobile-menu.js` 新增 6 组色板；`MENU_ICON_RULES` 每条规则带色调，未命中时按 ID 哈希取色，删除 `MENU_ACCENTS`。
  - 补充 `mobile-menu.test.js`。
- [x] T3 角标 store
  - 新建 `store/modules/badge.js`，实现合并请求和 `refresh()`。
  - 单测覆盖：并发调用只发一次请求；接口失败时保留上次的值。
- [x] T4 底部导航
  - 重写 `AiTabBar.vue`：悬浮胶囊、五项、选中底块、红点角标。
  - `pages.json` 的 `tabBar.list` 改为五项，五个页签页设为 `custom`。
  - `TAB_ROUTES`、`STACK_ROUTES` 同步调整。
- [x] T5 页签页顶栏组件
  - 新建 `components/AiTabHeader.vue`：租户头像、标题、副标题、搜索、功能插槽、状态栏安全区，以及企业微信、钉钉内嵌环境下的退化处理。
- [x] T6 公共组件换肤
  - `AiCard`、`AiCell`、`AiCellGroup`、`AiTabs`（下划线样式）、`AiSearchBar`、`AiButton`（次按钮）、`AiPopupSheet`、`AiEmpty`、`AiListSkeleton`、`AiTag`。

## 阶段二：页面改版

- [x] T7 工作台：概览卡片、常用应用 7+1、分组应用页签、去掉最新提醒、浅蓝渐变顶部；`home.scss` 重写。
- [x] T8 消息列表 + 详情：改为页签页；全局把 `navigateTo('/pages/message/index')` 改为 `switchTab`；`message.scss` 重写。
- [x] T9 待办列表：下划线页签带数量；卡片改版；审批动作和已读成功后同步角标。
  - 实现方式：审批后返回待办列表，由 `onShow` 重载并写入 `badgeStore.setTodoCount`；已读后直接 `badgeStore.setUnreadCount`；底部导航挂载时调用 `badgeStore.refresh()`。
- [x] T10 待办详情：摘要卡片、时间线、底部操作栏；只改模板结构、样式和 `TodoTaskSummary` / `TodoFlowTrace`，不改审批逻辑；文件行数不得超过当前的 862 行。
- [x] T11 我的：资料卡片、分组列表、退出卡片；`mine.scss` 重写。
- [x] T12 登录：新视觉，保留背景图和单次登录契约。
- [x] T13 应用承接页、低代码运行页（含 `LowcodeRuntimeList`）、组件演示页。

## 阶段三：通讯录

- [x] T14 后端
  - `SysContactsController` + `ISysContactsService` + `SysContactsMapper.xml` + `ContactMemberVO` / `ContactOrgVO` / `ContactSummaryVO` + `ContactMemberQuery`。
  - 登录即可见，不写迁移脚本；手机号和邮箱完整返回，不打印日志。
- [ ] T15 后端测试：租户隔离、停用和已删除用户不返回、`pageSize` 上限、返回字段白名单（不含身份证、密码、账号状态）。
  - 已写 `SysContactsMapperContractTest`、`SysContactsServiceImplTest`，覆盖 SQL 过滤条件、`pageSize` 上限、关键字处理、字段白名单。
  - 剩余：本机无 JDK，测试未执行；租户隔离和停用用户需连库联调验证。
- [x] T16 前端：`api/index.js` 新增四个方法；`pages/contacts/index`、`org`、`member` 三个页面；拨打按钮按手机号可见规则显示。

## 阶段四：收尾

- [x] T17 契约测试按 spec 第 7 章完成替换；对照第 2.1 节逐条确认没有误删无关断言。
- [ ] T18 全量验证
  - 已完成：`node --test` 105/105 通过，`build:h5` 通过，本地 H5 预览截图已记录。
  - 剩余：后端 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile` 及 T15 测试，需在有 JDK 的环境执行。
