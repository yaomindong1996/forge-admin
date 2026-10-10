# H5 钉钉风格改版 Test Spec

## 1. 环境

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
cd forge-h5-ui && pnpm install
```

后端编译需要本机 Maven；本机没有时记录“跳过”，由用户在真实环境补测。

## 2. 前端 Node 单测

```bash
cd forge-h5-ui && node --test src/utils/__tests__
```

| 用例 | 文件 | 预期 |
|------|------|------|
| 令牌 | `console-design-system.test.js` | 3.1 色值、圆角、间距全部命中；旧主色 `#3b82f6` 不再出现在 `theme.css` |
| 五页签 | 同上 | `tabBar.list` 顺序为消息、待办、工作台、通讯录、我的；`AiTabBar` 有 `ai-tabbar__badge` 并读取 `useBadgeStore` |
| 自绘顶栏范围 | 同上 | 只有五个页签页使用 `custom`；二级页面不使用 |
| 工作台结构 | 同上 | 常用应用 `slice(0, 7)`；分组应用页签存在；`feed-section` 不存在 |
| 消息跳转 | 同上 | 源码中没有 `navigateTo` 指向 `/pages/message/index` |
| 通讯录 | 同上 | 三个页面和四个 API 方法存在；拨打按钮受手机号可见条件控制 |
| 保留断言 | 同上 | 44px 触控高度、单次登录、审批意见滚动、业务表单字段来源、`AiAuthImage` 重试等断言原样通过 |
| 图标色板 | `mobile-menu.test.js` | 语义规则命中对应色调；同一菜单 ID 多次计算结果一致；不再出现 `MENU_ACCENTS` |
| 角标 store | `store/modules/__tests__/badge.test.js` | 并发 `refresh()` 只请求一次；失败时保留旧值；数量超过 99 时显示 `99+` |

## 3. 构建

```bash
cd forge-h5-ui && pnpm build:h5
```

构建必须通过，不允许新增警告类错误。

## 4. 后端

```bash
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile
cd forge-server && mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests -Dtest='SysContacts*'
```

| 用例 | 预期 |
|------|------|
| 租户隔离 | 只返回当前租户 `sys_user_tenant.status = 1` 的成员 |
| 用户状态 | 停用和已删除用户不返回 |
| 分页 | `pageSize` 大于 50 时按 50 处理；参数名为 `pageNum`、`pageSize` |
| 字段白名单 | VO 不包含身份证、密码、盐值、账号状态 |
| 手机号规则 | 同租户成员返回完整手机号和邮箱 |

## 5. 视觉验收

- 本地 `pnpm dev:h5`，用 375×812 视口逐页截图：登录、工作台、消息、消息详情、待办、待办详情、通讯录三页、我的、应用承接页、低代码运行页、组件演示页。
- 后端未启动时，需要登录的页面无法实际渲染：记录为“待用户联调”，只验收登录页和组件演示页。
- 对照用户提供的钉钉截图检查：底栏胶囊和选中底块、应用图标浅底方块、分组页签下划线、列表行分隔线缩进。
