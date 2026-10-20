# 执行记录

## 2026-10-20 开始

- 当前 main；本轮不发版，版本仍为 1.2.1。
- 已保留现有部署文档、导航图标变更记录、memory 与 .DS_Store 等无关本地修改。
- 按用户选定的第一张低饱和主画布稿实现，不改后端和审批协议。
- 4317 端口无现成前端服务；后续以独立本地组件验证入口做浏览器检查，不依赖或操作真实业务数据。

## 2026-10-20 实现与回归

### 实现

- 采用确认稿第一张的白卡/低饱和图标/主题色交互，未引入新依赖或图片资产。
- 提取 FlowDesignToolbar，真实模型名称/状态及原返回、历史、AI、保存、发布动作保持连接。
- 添加节点使用 NPopover，更多操作使用 NDropdown；高级类型默认折叠但全部保留。
- 条件卡复用真实边和字段目录，数字以字符串分组保留 Decimal/Long 精度；未知表达式不伪造业务摘要。
- 画布几何只作用编辑器，复杂回流继续用紧凑避让标签，不改变 BPMN 节点、出边顺序与序列化协议。
- 节点配置局部挂载到设计器；顶部操作不被遮罩，正文独立滚动、底部按钮固定；保留草稿取消/确认和全局保存前提交。
- 补齐真实内容适配、失焦释放拖拽、输入框空格保护、原生全屏及全屏浮层挂载。
- 设计页 971 行、DingFlowDesigner 882 行；未向超限 SFC 继续堆逻辑。

### 自动化证据

在 `forge-admin-ui` 执行（沿用已有依赖，不安装/升级依赖）：

```sh
source ~/.nvm/nvm.sh && nvm use v20.19.0
node node_modules/vitest/vitest.mjs run src/components/flow-designer src/views/flow/components/__tests__/FlowDesignToolbar.spec.js --maxWorkers=2 --minWorkers=1
```

结果：46 个测试文件、399 项用例通过、0 失败。复用 converter、节点权限/表单、撤销栈、查看器等原有基线；
新增条件卡几何、可读摘要与精度、菜单关闭/高级类型、适应画布/输入空格、工具栏动作及全屏浮层测试。
首轮新摘要测试因字符串数字未分组失败，已修复；没有删除/放宽该断言。
Vitest 中原有 n-alert/n-text stub 缺失与 transfer 重复注册 warning 非阻断，未通过禁用警告或跳过测试掩盖。

```sh
source ~/.nvm/nvm.sh && nvm use v20.19.0
{ git diff --name-only --relative -- .; git ls-files --others --exclude-standard -- .; } | rg '\.(vue|js)$' | xargs node node_modules/eslint/bin/eslint.js
```

结果：本轮所有修改及新建前端 JS/Vue 文件 ESLint 通过（退出码 0）。

```sh
source ~/.nvm/nvm.sh && nvm use v24.21.0
node --max_old_space_size=8192 node_modules/vite/bin/vite.js build
```

结果：完整构建退出码 0，10178 模块处理完成，最终输出 `built in 29.26s`。
本机 pnpm 11.7 在 Node 20 下因 node:sqlite 不支持而退出；Node 24 下的 pnpm build 又触发依赖自动检查/安装而失败。
因此使用已安装的项目本地 Vitest/ESLint/Vite 可执行文件验证；未修改 package.json、锁文件或依赖版本。
原始临时报告：`/private/tmp/forge-flow-soft-ui-tests.json`、`/private/tmp/forge-flow-soft-ui-build.log`。

### 浏览器证据

本地 `verification/preview.mjs` 挂载真实生产组件，仅提供合成示例数据和隔离请求适配；不访问真实业务接口。

- 1440×960：常用 5 种 + 展开高级 4 种节点可见；新增脚本任务打开配置，取消草稿不改名称；确认名称后卡片同步。
- 更多菜单复制后节点数从 7 到 8，撤销后恢复 7；撤销/重做按钮与历史栈联动。
- 点击金额条件卡，只展示分支 1，并识别真实字段“报销金额 ≥ 5,000”。
- 点击保存生成本地 XML（4046 字符），夹具明确提示未提交服务器；真实保存接口仍沿用生产页面原动作。
- 缩放至 100%，点击适应画布恢复 90%；使用说明正常展开；控制台无 error。
- 390×844：展开全部高级节点时 documentWidth=390；弹层 x=52、宽 286、高约 416，内容完整可达。
- 窄屏审批面板 footer 范围 left=0/right=390/bottom=810.5，取消/确定均可见，正文可独立滚动。
- 深色主题及原生全屏进入/退出、全屏下添加浮层可见性已检查。已退出全屏并恢复临时 viewport。
- 调试跨断点溢出期间 Vite 捕获过一次 ResizeObserver loop 通知；未屏蔽该错误，修正浮层锚点后最终全新加载控制台 error 列表为空。

截图：`desktop-canvas.jpg`、`desktop-panel.jpg`、`desktop-dark.jpg`、`narrow-menu.jpg`、`narrow-panel.jpg`。
本轮修复的视觉问题：Drawer 默认滚动包装撑高、浮层跨断点后锚点过期、动态节点图标遗漏 safelist。

### 交付边界

- 版本源 `forge-server/pom.xml` revision 仍为 1.2.1；只追加带日期的 Unreleased，不发版。
- 未执行真实 Admin/Flow 启动、数据库迁移、审批发起/发布或生产部署。
- 当前 main 工作区保留未提交实现；未 commit/push，不触及已有部署与多色导航记录的业务内容。
- 验收服务仅监听 127.0.0.1:4319，本轮 PID 81523；已通过 Ctrl-C 停止该服务，恢复浏览器尺寸并关闭本轮临时页，未停止其他进程。

## 2026-10-20 用户反馈修正：浮层关联、属性布局与颜色

### 改动及根因

- 添加菜单原桌面间距为 140px，造成与加号脱离；改为 Naive Popover 的 10px 主题间距并开启组件箭头，保留窄屏翻转、resize 关闭和全屏挂载。
- 语义色统一适度提亮，画布节点/图例/添加菜单/条件卡/属性标题共用 node-presentation；未给整卡或连线染色。
- 属性区取消递归匹配 `.space-y-*` 的 padding，普通节点只保留一层容器留白，审批由 Tab pane 维护。
- 条件规则改为容器断点；此前 1440px 浏览器下，480px 侧栏仍用大屏横排，删除按钮被挤出。
- 原条件增删图标名在已安装的图标集合中不可用，替换成同风格 Lucide 图标，实际检查 mask 已生成。
- 双列时限及监听器字段添加 align-items:start，避免末项 margin-bottom=0 导致 Grid 拉伸正文并使输入框低 6px。
- 会签说明段落收紧，表单权限复选框对齐列标题；抄送高级配置上下排布，权限开关/提示/边框改为主题变量。
- 发起节点帮助文案指向现有顶部“更多设置 / 表单配置”。未改变审批规则、表单权限码或 BPMN 协议。

### 自动化增量

沿用前节本地可执行命令（Node 20 测试/lint、Node 24 Vite），最终结果：

- Vitest：47 个文件、404 项通过、0 失败。新增浮层间距/箭头断言及 4 项配置草稿测试，保护取消、确认和 readonly。
- 首次回归仅旧文案断言失败；按当前真实入口修正文案断言，并保留“旧入口文案不得出现”的反向断言。
- 所有触及前端 JS/Vue 文件 ESLint 通过（退出码 0）。清理 FormPermissionConfig 两个原有重复对象键，仍保留展开历史配置后的目录键覆盖，输出值不变。
- 完整 Vite 构建退出码 0：`built in 34.64s`。
- `git diff --check` 通过。设计页 971 行、DingFlowDesigner 886 行、ConditionConfig 986 行。
- `verification/VerificationApp.vue` 位于前端 ESLint 配置覆盖范围之外，显式 lint 仍提示 ignored；不将该忽略视作已 lint。其实际 Vue 编译和交互由隔离 Vite 验证覆盖，未修改全局 lint 范围。
- 临时报告：`/private/tmp/forge-flow-soft-ui-feedback-tests.json`、`/private/tmp/forge-flow-soft-ui-feedback-build.log`。

### 浏览器实测

仍用隔离入口，不连接真实业务接口：

- 1440×960 菜单：trigger.left=708.28、popover.right=698，间距约 10.28px，箭头可见。
- 点验发起、结束、审批、抄送、条件、并行、包容、服务、脚本、子流程、调用活动；审批 4 页签及新增监听器可操作，未用浏览器写入真实数据。
- 条件属性 body.clientWidth=scrollWidth=480，删除按钮 x≈1379/right≈1403，图标可见，未越过右侧 1440 边界。
- 时限两个输入框 top 均为 232.59px（修复前相差 6px）。
- 390×844 展开全部高级菜单：documentWidth=390，浮层 x=52/w=286/h≈416，距触发点约 10px。
- 390px 审批正文 height=539/scrollHeight=969，实际滚动到 scrollTop=430；footer.bottom 仍为 810.5，取消/确定均可见。
- 320×840：documentWidth=320，属性 body.clientWidth=scrollWidth=320，字段表宽 288，页签可横向滚动并访问扩展配置。
- 深色权限面板文字、分割线和开关清晰；最终控制台 error 为 0。

新增截图：`desktop-canvas-feedback.jpg`、`desktop-panel-feedback.jpg`、`desktop-condition-feedback.jpg`、
`desktop-dark-feedback.jpg`、`narrow-menu-feedback.jpg`、`narrow-panel-feedback.jpg`。

本轮隔离验收服务 PID 9546（127.0.0.1:4319）已通过 Ctrl-C 停止；恢复 viewport 并关闭本轮临时页。
当前 main 工作区未提交/push，未升级版本、启动真实后端、访问数据库或部署。

## 2026-10-20 提交交付复核

用户后续授权将样式与节点合并一起提交、推送 main；上文“未提交/push”为实现阶段的历史状态。
已无冲突快进同步远端 3 个提交，并复跑联合基线：50 文件/439 项测试、ESLint、完整前端构建及开源边界检查通过。
本次仍不升版、不创建标签、不部署，不包含其他会话的未提交修改；命令、报告和具体范围见 `../flow-designer-node-merge/execution-log.md`。
