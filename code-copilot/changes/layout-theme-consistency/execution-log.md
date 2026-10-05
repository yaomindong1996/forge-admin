# 执行记录

## 2026-10-05 调查

- 工作分支 `codex/workbench-illustrations`，基线 `9aff6ba9`；既有 `.DS_Store` 改动保留。
- 根因：Header 工具混用正文/菜单颜色；部分布局硬编码白底；SideLogo 与沉浸 Header 强制滤白 Logo；App 排除 `empty` 的设置入口。
- 租户页面 1764 行，配色字段约 20 项；本轮先拆分再实现。
- 已阅读 DESIGN、编码/测试标准、主题/布局代码及既有业务工作台验证记录。

## 实现与修复

- 新增共用导航配色计算、六种预设、三基础色和折叠高级设置；历史 JSON、暗色分组、尺寸及扩展字段保留。
- 拆分租户外观、表单 Schema、用户弹窗与 API 模块；租户页 306 行，外观设置壳 89 行。
- 统一十种布局接入；Logo 不再滤白或额外加蓝底；移除嵌套 Logo 链接。
- 沉浸式抽屉使用授权菜单树，支持搜索、完整子项、独立滚动与叶子导航后关闭。
- 根级 Pinia 外观抽屉修复空白布局无法恢复；路由强制空白和应用门户不显示设置。
- 业务工作台增加真实根菜单图标和克制选中态；便当盒目录入口可展开菜单、任意层级路径正确激活。
- 浏览器发现 Tab 固定项/搜索工具、Nexus 父容器、组织切换器的 scoped 白底覆盖公共样式，逐项修正区域变量。

## 自动化验收

- Node `v20.19.0`；增量 Vitest 六个文件共 38 项通过（新增 20 项，既有工作台 12 项、租户工作区 6 项）。
- 修改/新增 JS、Vue 文件 ESLint 通过；`git diff --check` 通过。
- 最终生产构建：`node --max-old-space-size=8192 node_modules/vite/bin/vite.js build`，退出 0，32.64 秒。
- 构建日志保存在 `/private/tmp/forge-layout-theme-build.log`；保留插件耗时提示，不作为编译错误。
- 修改过的 SFC 均低于 1000 行；原 Tab 编排组件保持 954 行，未继续堆功能。

## 浏览器验收（隔离预览）

- 使用真实 App、布局、Store 和公共组件；仅模拟用户、授权菜单与查询 API，不请求或保存真实租户配置。
- 检查通用、全屏、顶部菜单、混合、简约、沉浸、便当盒、Nexus、业务工作台、空白十种布局。
- 清爽白、品牌蓝、石墨方案，以及暗色模式均检查工具/文字可见性；蓝色顶栏上的折叠、通知、搜索、Tab 文字可辨识。
- 个人外观：切空白后关抽屉，再通过恢复入口切回；抽屉切布局保持打开。
- 沉浸式：搜索父目录保留 30 个授权子项；菜单滚动高度 1252px / 可视 512px，搜索固定；点击最后叶子可导航并关闭。
- 租户外观组件：基础三色、简约两色、空白仅主色；高级默认折叠；草稿预览变色不污染当前系统顶栏。
- 工作台真实根图标、Nexus 收起/展开、便当盒目录入口、多租户/多组织切换器逐一点击检查。
- 1440、1280、1024px 宽度检查；1024px 下右侧工具不越界，Tab 保留溢出滚动。
- 最后一次新浏览器检查无运行时错误；早期模拟组织接口路径错误已修正，仅属于预览桩问题。
- 截图：`/private/tmp/forge-layout-theme-preview.jpg`；临时服务 `127.0.0.1:3032` 已停止，验证标签已关闭。

## 未执行项与交付

- 不启动后端/数据库，不改变线上租户、成员关系、权限或菜单数据。
- 未执行真实租户保存/重登录端到端测试；JSON 保留与编辑器行为由单测验证。
- 既有 `.DS_Store` 改动保留、不纳入提交；当前功能仅本地提交，不自动推送。

## 2026-10-05 第二阶段 T06

### 差异与修正

- 基线 `59cbb752`，继续在 `codex/workbench-illustrations` 开发；不修改用户 `.DS_Store`。
- 通知抽屉不再使用固定绿色按钮/白色表面，所有文字、边框、状态、按钮跟随主题；消息 API 与审批导航不变。
- 七种顶栏复用 `HeaderTools`，工作台补租户/组织原组件；全屏和指引进更多工具；个人入口采用点击和键盘可用按钮。
- 恢复当前租户外观与恢复系统配色分离：后者确认后只恢复主题，保留布局、模式和身份；无后端保存或身份切换。
- 配色编辑器增加独立浅/深预览；顶部菜单、混合、便当盒缩略图修正，顶部菜单描述与真实下拉结构一致。
- 浏览器发现四种内容区仍使用固定浅色灰度，改为语义表面色；Nexus 全局 `:root` 会污染 `--primary-300`，已删除并让示例业务页自行使用通用 Token。
- Nexus 不再硬覆盖自定义深色导航或业务页样式；深色内容区 Tab 文字使用可读正文色，顶栏 Tab 保留导航表面对比色。
- 初次 Lint 发现重复 `aria-pressed` 属性，已修正；最终差异无重复属性或新增超长行。

### 自动化

- Node `v20.19.0`；八个 Vitest 文件 60 项通过（原 38 + 新增 17 + 通知协议回归 5），1.71 秒。
- 测试代码行长格式化后单独再跑会话主题 7 项，通过；变更 JS/Vue ESLint、`git diff --check` 通过。
- 最后一次生产构建退出 0，52.01 秒；日志 `/private/tmp/forge-layout-theme-stage2-build.log`，仅保留插件耗时提示。
- 新增控件/工具区/解析器均小规模；Tab 编排壳 960 行（仅增语义提示及 6 行可读性样式），Nexus 367 行，预览编辑器 333 行。

### 浏览器增量验收与边界

- 复用真实 App/布局/Store 的隔离预览，模拟查询 API、授权菜单、多租户/多组织项和一条审批通知；不连接真实后端。
- 工作台 1280/1024/768px 检查，页面宽度等于视口宽度，工具右边界不越界，窄屏隐藏身份名称但保留可访问提示。
- 通用、全屏、顶部菜单、混合、沉浸式、Nexus、工作台七种顶栏均复用工具，搜索只出现一次；标签查找与全部菜单搜索均能打开，搜索用户找到授权菜单。
- 点击租户/组织入口看到原组件下拉项，未选择其他身份；个人中心通过 Enter 打开。
- 更多工具显示全屏和指引；指引三步均能定位并完成。未执行浏览器全屏切换或真实注销。
- 恢复租户外观找回工作台和松石绿；取消系统恢复不变，确认系统恢复布局仍为工作台；空白布局通过恢复入口可切回。
- 深色样例背景 `#061917`，切换预览不改变实际浅色页面/草稿；Nexus 实际深色顶栏 `rgb(6,25,23)`、侧栏 `rgb(8,32,29)` 保留租户配置。
- 通知浅/深表面、蓝色/松石绿主按钮跟随主题；最终通知按钮与租户顶栏同为 `rgb(15,118,110)`，正文未染成顶栏白字。
- 最终浏览器无 error；预览桩仅有根路径未配置及既有组件注册 warning，不作为真实应用验证结论。
- 截图 `/private/tmp/forge-layout-theme-stage2-preview.png`、`/private/tmp/forge-layout-theme-stage2-notification.png`。
- 临时服务 `127.0.0.1:3032` 已停止、端口无监听；验证标签已关闭，视口覆盖已恢复。
- 不执行真实租户保存/重登录、身份切换、消息已读、审批提交，不修改后端/数据库或权限；本地提交，不自动 push。

## 2026-10-05 第三阶段 T07 开始

- 基线 `9b64b337`；继续 `codex/workbench-illustrations`，保留用户 `.DS_Store`。
- 增量范围：窄屏菜单可达、无顶栏工具收纳、登录主题解析一致、可选深色高级编辑。
- 复用现有 Spec/测试基线与隔离预览，不运行真实后台服务或写接口。

### T07 实现与自动化结果

- Nexus 删除边缘拖拽与离屏侧栏逻辑，顶栏共用响应式菜单按钮；简约桌面侧栏与窄屏入口互斥挂载。
- 简约/便当盒共用 `CompactLayoutTools`，保留实际个人资料、租户/组织组件，主题/全屏/外观收入面板。
- ≤480px 共用顶栏只直显搜索、通知、账户工具；操作指引保留在面板，不重复挂载菜单搜索或切换器。
- `applyTenantConfig` 复用 `readTenantAppearance`，删除另一套解析；登录和恢复完整主题对比覆盖八种场景。
- 高级区浅/深配置分组明确，深色基础和状态色不写入浅色分组；同步 DESIGN 语义背景 Token。
- 初次高级控件测试仍引用基础色修改后已被卸载的手动项；修正测试为显式回到手动模式，未放宽断言。
- 初次 Lint 发现两个单行 watch 回调多语句，改为块函数；高级字段映射拆为清晰块函数，避免超长行。
- Node `v20.19.0`；最终 Vitest 十一个文件 78 项全部通过，1.92 秒；目标 JS/Vue Lint 通过。
- 生产构建 `node --max-old-space-size=8192 node_modules/vite/bin/vite.js build` 退出 0，52.23 秒。
- 构建日志 `/private/tmp/forge-layout-theme-stage3-build.log`，仅有插件耗时提示，无编译错误。
- 新增工具/菜单组件 110 行以内，外观编辑器 377 行、Nexus 228 行、简约 149 行；未修改超限 SFC。

可复跑增量命令（先进入 `forge-admin-ui`，切换 Node 20.19.0）：

```bash
node node_modules/vitest/vitest.mjs run \
  src/utils/__tests__/navigation-theme.spec.js \
  src/utils/__tests__/app-theme.spec.js \
  src/utils/__tests__/tenant-config-appearance.spec.js \
  src/components/common/appearance/__tests__/appearance-controls.spec.js \
  src/layouts/components/__tests__/responsive-menu-toggle.spec.js \
  src/layouts/components/__tests__/compact-layout-tools.spec.js \
  src/layouts/__tests__/layout-theme-surfaces.spec.js \
  src/layouts/components/__tests__/message-notification-utils.spec.js \
  src/layouts/business-workbench/__tests__/menu-model.spec.js \
  src/layouts/business-workbench/__tests__/settings.spec.js \
  src/views/system/__tests__/tenant-workspace-ux.spec.js
git diff 9b64b337 --relative --name-only -- src | rg '\.(js|vue)$' | \
  xargs node node_modules/eslint/bin/eslint.js
```

### T07 浏览器证据与边界

- 复用本机隔离 Vite 预览，真实 App/布局/Store/组件配模拟授权菜单、身份选项和查询数据；无真实后端调用。
- Nexus 桌面可点击/Enter 收起展开；390px 菜单搜索父目录并点击业务报表 30，导航且关闭；放大后遮罩关闭。
- 320px 共用工具区宽 108px，右边界 303px；Nexus 顶栏右边界 312px，页面宽与视口同为 320px。
- 768×480 长菜单滚动容器可视 272px、内容 1252px；搜索区固定，全部 30 个叶子可访问。
- 简约桌面收起后侧栏 64px，账户/展开按钮仍在视口内；缩至窄屏打开菜单，再切回桌面，偏好未改变。
- 简约窄屏账户面板右边界 368px（390px 视口）；便当盒短窗口面板全部工具可见，深色文字跟随主题。
- 点击真实租户和组织组件看到模拟接口选项，未选择身份；账户按钮 Enter 打开，外观按钮关闭面板并打开根级抽屉。
- 深色高级色板改背景，预览立即更新，浅色背景保持白色，实际系统导航未被租户草稿污染。
- 浏览器工具 HEX 填值未触发原生 change，未作为键入成功证据；补真实取色器 DOM input/change 单测通过。
- 最终浏览器无 error；根路由未配置和既有重复组件注册 warning 仅属于隔离预览，不代表线上验证。
- 截图 `/private/tmp/forge-layout-theme-stage3-preview.png`；验证标签已关闭，临时视口已恢复。
- 临时服务 PID `20024`（127.0.0.1:3032）已通过本轮会话 Ctrl+C 停止；不停止用户其他进程。
- 无真实登录、配置保存、身份切换、注销、全屏或审批/消息写入验收；无后端/数据库/依赖改动。
- 保留用户 `.DS_Store`，本阶段仅本地提交，不自动推送。

## 2026-10-05 第四阶段 T08

### 范围与实现

- 基线 `bdac24d7`，继续在 `codex/workbench-illustrations`；保留用户 `.DS_Store`。
- 通用/全屏顶栏接入 `ResponsiveMenuToggle`，窄屏不挂桌面侧栏；顶部/混合窄屏以抽屉替代横向菜单。
- 抽屉外路由变化也会关闭菜单；窄屏状态不写桌面 `collapsed`，删除旧离屏固定侧栏与 width 动画。
- 新增可选 `navigationModeDark`，历史单模式配置仍回退到原值；首次编辑固定另一模式，实际渲染互不覆盖。
- 手动开关按当前模式回显；进入手动从当前渲染颜色生成，避免跳回原始颜色。
- 拆分 `AccountIdentity` 与 `useAccountActions`；紧凑面板直接进入资料/退出确认，保留门户资料路由和指引锚点。
- 头像沿用文件解析器，迟到响应不覆盖新身份，图片失败回退姓名且不循环请求；退出危险色增加 Token 兜底。

### 自动化证据

- Node `v20.19.0`；13 个 Vitest 文件 104 项通过（原 78 + 新增 26），最终耗时 1.91 秒。
- 首次头像错误测试的匿名桩无组件名，导致定位失败；为桩添加名字后通过，未修改/放宽产品断言。
- 初次 Lint 提示 import 顺序、空行和单行多语句，目标格式化及回调拆行后全部通过。
- 修改 JS/Vue 与四个新增文件 ESLint 通过；`git diff --check` 通过。
- 最终生产构建退出 0，37.15 秒；日志 `/private/tmp/forge-layout-theme-stage4-build.log`。
- 仅保留插件耗时提示，不作为编译错误；新增账户组件 64 行、面板 131 行、动作 62 行，外观编辑器 379 行。

复跑：沿用 T07 的 11 文件 Vitest 命令，追加下面两个文件：

```text
src/layouts/components/__tests__/account-actions.spec.js
src/layouts/components/__tests__/account-identity.spec.js
```

ESLint 沿用 T07 差异检查（基线改为 `bdac24d7`）；构建命令仍为：

```bash
node --max-old-space-size=8192 node_modules/vite/bin/vite.js build
```

### 浏览器检查与边界

- 复用真实 App/Store/布局/组件的隔离预览；模拟认证、授权菜单与查询 API，无真实后台连接。
- 四种布局 320/390/768/1024px 验证：窄屏无桌面侧栏，页面宽度等于视口；菜单按钮保持可达。
- 通用桌面收起→390px 打开长菜单→最后叶子导航→1024px，侧栏仍为 64px；未修改桌面偏好。
- 全屏/混合在真实菜单协议的父级页面恢复侧栏；选中、关闭按钮、遮罩与放大窗口均关闭抽屉。
- 个人资料直接导航 `/profile`；退出打开原确认框，点击取消不注销；功能确认由模拟单测覆盖。
- 浅色手动开启后切深色，深色仍关闭，返回浅色仍开启；未执行真实租户保存。
- 浏览器截图发现退出色变量缺少默认别名，已增加现有 `--error-500` 兜底；实测 `rgb(239,68,68)`。
- 初期模拟目录用了 `directory` 且无稳定 ID，混合菜单会空白/递归；改为实际 `module` 与稳定 ID 后，
  新标签复跑四种布局无 error。属于预览桩问题，不修改生产菜单协议。
- 最终截图 `/private/tmp/forge-layout-theme-stage4-preview.jpg`，390×740，退出按钮 bottom=448px。
- 根路径未配置与既有重复组件注册 warning 属于隔离预览；不能据此宣称真实后端端到端通过。
- 浏览器两个验证标签均关闭，临时视口已恢复；临时服务 PID `84255`（3032）已 Ctrl+C 停止，端口无监听。
- 不执行真实身份切换、登录/注销、租户保存、消息或审批写入；不改后端、数据库、依赖。本地提交，不 push。

## 2026-10-05 第五阶段 T09 / T10 排查

### 实现范围

- 基线 `700276d3`，继续使用 `codex/workbench-illustrations`，保留用户 `.DS_Store`。
- 沉浸式局部 CSS 用正文背景覆盖导航角色，导致白底白字；改为顶栏前景/背景/边框/hover 角色。
- 便当盒通知包装层旧 margin 偏移中心线；统一两入口 36px 热区与 20px 图标。
- 全屏与简约侧栏展开至少 240px，保留更宽配置和收起偏好；全屏顶栏/品牌区 48px。
- 云蓝/墨蓝作为额外预设，色板同时展示顶栏/主色/侧栏，不覆盖租户已有配置。
- 第三方旧指引依赖固定目标并轮询，且随工具浮层卸载；替换为 App 根级 Naive UI 对话框。
  说明按当前布局生成，入口关闭工具浮层，指引可独立继续；路由/布局变化关闭。

### 自动化验证

- Node `v20.19.0`；复用第四阶段 13 文件，追加 `operation-guide.spec.js`，最终 14 文件 125 项通过，2.16 秒。
- 复跑命令沿用 T08，追加 `src/layouts/components/__tests__/operation-guide.spec.js`。
- 初始 Lint 提示导入顺序/格式，针对修改文件修正；目标 tracked JS/Vue 与三个新增文件 ESLint 最终退出 0。
- 指引快速跳转时 opening watch 的异步执行覆盖步骤；改为同步且仅打开时重置，组件与浏览器复跑通过。
- 最终生产构建 `node --max-old-space-size=8192 node_modules/vite/bin/vite.js build` 退出 0，33.94 秒。
  日志 `/private/tmp/forge-layout-theme-stage5-build-complete.log`，仅保留插件耗时提示。
- `git diff --check` 通过；新增指引对话框 135 行、说明 38 行、入口 19 行，未向超限 SFC 堆功能。

### 实际样式与浏览器

- 使用真实 App/Store/布局/组件的隔离预览，授权菜单、认证和查询 API 为桩；不连接真实后台。
- 沉浸式清爽白/云蓝：白色背景 + 深色文字；墨蓝：rgb(30,41,59) + 白色文字；品牌蓝及深色均可见。
- 全屏最初实际高度仍为 60px，发现旧全局 `!important` 使用高度变量；同步局部变量后实测 48px。
- 320px 沉浸式右侧最初越界，发现 scoped display:flex 覆盖公共隐藏规则；同步隐藏中央区并缩边距，
  最终工具右边界 312px，视口 320px；顶栏实际背景 rgb(30,41,59)、文字白色、高度 52px。
- 便当盒消息与账户均 36×36，图标 20px，按钮与图标中心线均 27.5px；768×480 工具面板 bottom=460px。
- 全屏在 1280/1024px 的父级业务路由侧栏 240px，390px 无桌面侧栏，页面宽度等于视口。
- 简约展开 240px，收起 64px；390px 隐藏桌面侧栏，返回桌面仍保留 64px 偏好。
- 桌面更多工具和便当盒账户均能启动指引，原浮层关闭后仍可下一步/上一步/跳转/完成；
  外观按钮打开根级外观面板且关闭指引。320×740 步骤单列，四步与底部按钮完整可见。
- 最终浏览器 error 日志为空；根路由缺失和既有重复组件注册 warning 属于预览桩，不代表真实 E2E。
- 截图 `/private/tmp/forge-layout-theme-stage5-preview.jpg`、`/private/tmp/forge-layout-theme-stage5-guide.jpg`。
- 验证标签 8 已关闭，视口恢复；仅本轮临时 Vite 会话已 Ctrl+C，3032 端口无监听。

### 租户保存未闭环

- 按 Forge CRUD Skill 核对 typed DTO、Service 复制与 Mapper 保存；现有更新不会压缩完整主题 JSON。
- 初始化 SQL 的 `theme_config` 为 varchar(1000)，静态提取默认对象序列化长度为 2108 字符；
  首次带模块导入的测量不适用于 Node 别名环境，改为提取纯配置字面量后得到有效结果。
- 未找到主题列扩容迁移；没有本地服务端日志，未验证实际部署列类型，也未重放 TraceId 对应租户写入。
- 已询问是否提交 varchar→TEXT 的 Flyway 扩容，尚未收到确认；不修改数据库脚本、不执行真实 DDL。
- T10 保持未完成；不能以容量风险替代现场 SQL 异常，也不能宣称租户 500 已修复。
- 不执行真实租户保存、身份切换、登录/注销、消息或审批写入；本轮前端本地提交，不自动推送。

## 2026-10-05 第六阶段 T10

### 根因、授权与实现

- 基线 `abda59fa`，分支 `codex/workbench-illustrations`，保留用户 `.DS_Store`；远端 main 未发现新提交。
- 用户补充服务端 SQL 日志：`theme_config` 超长触发 `MysqlDataTruncation`，并明确同意扩容及移除操作指引。
- 新增 `V1.0.208__expand_tenant_theme_config.sql`，仅将当前库的 sys_tenant/theme_config 窄文本列扩为 TEXT。
- 保留字符集/排序规则，不降级宽文本/JSON；重复执行为空操作，不截断、重写或删除配置。
- 全量和 Docker 初始化 SQL 同步为 TEXT；Java DTO、实体及接口 String 协议不改。
- DDL 可能短时持有表锁，用户安排执行；应用回滚可继续使用 TEXT，不提供会截断数据的缩列回滚。

### 验证与明确边界

- Node `v20.19.0` 执行下面命令：16 项通过（迁移契约 5 + 排序规则回归 11），255.868ms。

```bash
node --test forge-server/scripts/db/tenant-theme-migration.test.mjs \
  forge-server/scripts/db/check-collation-consistency.test.mjs
```

- 测试静态检查版本唯一、作用域/类型守卫、PREPARE 生命周期、无数据 DML/截断、初始化一致及 Java String 定义。
- 大于 1000 字符的扩展配置由前端实际 applyTenantConfig/restore 测试验证，源字符串和未知扩展字段均保留。
- 新脚本无 Flyway `${...}` 业务占位符。全库扫描仅发现历史 V1.0.72 的 4 行模板，原样保留；
  Admin 和脚本 Maven 的 Flyway 配置均禁用 placeholder replacement，不据此宣称全库扫描零命中。
- 本机 `/usr/bin/java` 仅系统占位，`java_home -v17` 未找到运行时且没有 Maven；后端编译/动态测试未执行，
  没有安装工具或修改生产 Java。真实 Flyway、租户保存、后台服务启动明确未执行。
- 本地提交脚本不代表测试环境错误已消除；须先执行 V1.0.208，再由用户重试租户配置保存。
