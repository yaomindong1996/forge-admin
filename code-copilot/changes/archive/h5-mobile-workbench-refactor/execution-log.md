# 执行记录

## 2026-07-28 — 基线

- 范围：创建 H5 移动工作台与待办闭环重构的 Spec、任务与验证计划。
- 发现：工作区已有 `.DS_Store`、`forge-h5-ui/src/manifest.json`、锁文件及输出文档等未提交变更；本变更不会覆盖它们。
- 服务：未启动任何服务。

## 2026-07-28 — 实现与静态验证

- 变更：收敛全局主题、按钮、页面壳、弹层、搜索与底部导航；登录/首页/消息/我的页移除装饰性动画与多色玻璃效果；首页改用真实待办摘要；待办增加状态/分类筛选、签收和独立详情页。
- 待办安全边界：详情按 `TaskFormInfo` 处理同意、驳回、退回、终结、转办、必填意见和表单字段；要求签名、外部表单、仅 PC 的代码表单及不支持的动态字段配置均失败关闭，禁止跳过表单直接提交。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`。
- 警告：当前 shell 的 nvm 将 `v20.19.0` 解析为 `N/A`，但命令仍以当前 Node 工具链完成构建；未安装依赖、未启动服务。
- 静态检查：`git diff --check` 通过，无空白错误。
- 跳过：未启动真实 Flow/Admin 服务，未执行会改变流程运行态的 E2E；需由用户环境按 `test-spec.md` 回填。

## 2026-07-28 — 最终增量验证

- 变更：补齐共享 `AiLayoutPage` 的简约导航/底栏与带文字的三项底部导航；待办补充流程分类筛选，并收紧动态表单字段类型的失败关闭规则。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；同样存在 nvm `N/A` 警告但未阻断实际构建。
- 静态检查：`git diff --check` 通过；新增 `pages/todo-detail.vue` 使用 `git diff --no-index --check` 检查通过。

## 2026-07-28 — 待办交互与可读性修正

- 变更：删除“我的”页重复的快捷卡片；重绘底部导航为简洁图标/文字导航；修正首页、消息页的白底文字与刷新图标对比度。
- 待办：移除旧底部审批弹层，改为紧凑任务列表；候选任务先签收，已签收任务进入详情处理；详情页仅显示当前关键操作，其余操作归入“更多”。
- 路由：消息中心在有 `taskId` 时直达详情页，无 `taskId` 时切换到待办 tab；兼容消息配置中的 `/pages/todo?taskId=...`，不再对 tab 页调用 `navigateTo`。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；nvm `N/A` 警告仍存在但不阻断构建。
- 静态检查：`git diff --check` 通过；重写后的 `pages/todo.vue` 使用 `git diff --no-index --check` 检查通过。

## 2026-07-28 — 待办入口最终核对

- 修正：首页后端菜单若映射到 `/pages/todo`，改用 `switchTab`，避免 tab 页被 `navigateTo` 误判为“页面未注册”。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`git diff --check` 通过。
- 文案扫描：H5 源码中不存在“移动端待办页暂未接入”或“待办页面未注册”。

## 2026-07-28 — 消息可读性与首页优先事项收敛

- 变更：消息详情移除“关联业务”内部标识块，不再展示 `FLOW_TODO`、业务键或 UUID；保留“查看业务”作为唯一的业务跳转入口。消息页移除大号未读统计卡，筛选改为宽触达标签与独立“全部标为已读”操作。
- 首页：删除 `feature-inner` 里的菜单、权限、同步等技术计数，工作台改为“优先处理”两条业务行动项（待处理任务、消息提醒）；底栏取消顶部短横与悬浮卡式表现，改为带安全区的三项图标/文字导航。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`nvm use` 仍提示 `N/A` 未安装，但未阻断当前 Node 工具链构建。
- 静态检查：`git diff --check -- forge-h5-ui/src/components/AiTabBar.vue forge-h5-ui/src/pages/index/index.vue forge-h5-ui/src/pages/message/index.vue` 通过；源码扫描未发现待办页未接入文案，也未发现消息页关联业务标识渲染。
- 跳过：未启动真实 Flow/Admin 服务、未执行会改变流程运行态的 E2E；消息数据与真实任务跳转由用户环境按 `test-spec.md` 回填。

## 2026-07-28 — 共享控件紧凑化与真实资料展示

- 变更：`AiSearchBar` 调整为 68rpx 高度，`AiField` 调整为 80rpx 高度；二者改用实体白底、清晰边框和三态焦点/错误反馈，移除模糊与弱边界效果。
- 页面：消息列表的审批提醒卡片改为 58rpx 图标、单行摘要的紧凑卡片；首页优先处理项压缩为 98rpx 行高。快捷入口不再注入“组件演示”。
- 资料：个人页删除顶部账号/手机/权限统计；安全中心的账号、手机、邮箱仅在后端有实际值时显示，不再以 `-` 作为联系方式占位。
- 导航：底栏选中项升级为单色图标底板与浅蓝边界，保持三项导航和安全区，同时避免普通的纯文本/短横表现。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`nvm use` 仍提示 `N/A` 未安装，但未阻断实际构建。
- 跳过：未启动真实服务与浏览器 E2E；后端联系方式字段和真实消息数据由用户环境按 `test-spec.md` 进行页面验收。

## 2026-07-28 — 快捷入口注册校验与胶囊导航回退

- 变更：首页恢复固定“组件演示”入口，`flattenMenus` 与全部应用菜单仅保留 `pages.json` 已注册的 H5 路由；未注册的“联系方式”等后端菜单不会再显示为可点击卡片，因此不会触发“页面未注册”。
- 视觉：`AiTabBar` 回退为原有浮动胶囊导航，保留三项任务导航；`workbench-panel` 移除辅助说明并缩短刷新按钮、图标、行高和内边距，进一步降低首屏占用。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`nvm use` 仍提示 `N/A` 未安装，但未阻断实际构建。
- 跳过：未启动真实服务与浏览器 E2E；用户环境需确认后端菜单路径与 `pages.json` 注册路由一致。

## 2026-07-28 — 待办列表统一与查询骨架屏

- 变更：新增共享 `AiListSkeleton`，使用低对比度静态占位和仅加载期间的轻微透明度反馈；消息、待办、通用 `AiPagingPage`、转办人员和租户切换列表均接入首次查询骨架屏。
- 待办：改用 `AiSearchBar`，将页面收敛为与消息页一致的标题、搜索、横向筛选和白底边框卡片层级；首页 `priority-item` 保持仅两条、84rpx 行高的紧凑工作入口。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`nvm use` 仍提示 `N/A` 未安装，但未阻断实际构建。
- 跳过：未启动真实服务及浏览器 E2E；骨架屏的网络节流、真实待办和转办人员数据需要由用户环境按 `test-spec.md` 回填。

## 2026-07-28 — 首页处理中心重新设计

- 变更：移除旧 `priority-item` 行项目，首页处理区改为两张明确的行动卡（待办、消息）。卡片只呈现可执行的数量、当前状态和对应入口，不再沿用缩小后的列表行布局。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`git diff --check -- forge-h5-ui/src/pages/index/index.vue` 通过。

## 2026-07-28 — 后端菜单保留与全部应用重构

- 变更：恢复所有后端授权的 H5 菜单展示；例如 `/pages/test` 的“联系方式”保留在首页和全部应用中。若专属 H5 页面未编译，统一跳转到新增的 `pages/app-entry` 承接页，避免“页面未注册”且不隐藏正式菜单。
- 全部应用：移除左侧分类栏与三列小方格，改为按业务模块纵向分组、双列行动卡与单一搜索入口；最新提醒在无后端数据时仅展示空状态，不再注入默认系统消息。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`nvm use` 提示 `N/A` 未安装，但未阻断实际构建。

## 2026-07-28 — 公共空状态图片化

- 变更：公共 `AiEmpty` 改用用户提供的 `src/static/images/no-data.png`，去除灰色图标框；消息空状态移除无意义的刷新按钮，保留标题与筛选结果说明。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`git diff --check -- forge-h5-ui/src/components/AiEmpty.vue forge-h5-ui/src/pages/message/index.vue` 通过。

## 2026-07-28 — Sa-Token Redis 配置统一

- 变更：按部署要求，将 Admin、App、Flow、Report 服务中 `sa-token.alone-redis.database` 的显式值由 `1` 统一为 `0`。
- 静态检查：全仓 YAML/Properties 扫描未发现遗留 `database: 1`；`git diff --check` 通过。
- 跳过：本轮仅修改运行时配置，未启动服务或执行登录/待办接口；生产环境需重启相关服务并重新登录后验证。

## 2026-07-28 — 待办列表与详情信息补齐

- 列表：重做 `/pages/todo` 的待办卡片，展示与 PC 一致的当前节点、申请人、流程分类、提交时间和流程名称；名称、分类、状态三项查询条件保留，流程分类改为公共 `AiSelect`。
- 详情：公共 `AiTabs` 调整为小圆角胶囊样式并接入待办详情；补充发起部门、流程分类等基础信息。节点没有配置字段时，改从真实业务记录补只读字段，并展示已配置的业务明细；移除“该节点没有需要展示的业务字段”提示。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`nvm use` 仍提示 `N/A`，未阻断当前 Node 环境构建。
- 静态检查：待办相关已跟踪文件 `git diff --check` 通过；构建确认新增详情页模板可编译。

## 2026-07-28 — 待办详情紧凑化与分类选择修复

- 详情：顶部导航背景跟随页面底色；加载任务时显示列表骨架屏；底部审批按钮改为 `sm` 尺寸并收紧操作栏安全区内边距；紧急优先级标识由红色改为琥珀色。
- 分类：公共 `AiSelect` 不再依赖未注册的 `uni-popup`，改为复用 `AiPopupSheet`；点击“全部流程”可打开分类列表，选择后立即更新筛选结果。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；`nvm use` 的 `N/A` 提示未影响实际构建。

## 2026-07-28 — H5/Flow Token 活跃状态一致性修复

- 根因：H5 登录按 `sys_client.token_activity_timeout=-1` 修改 App 进程内的 Sa-Token 全局配置；Flow 独立进程仍按自身全局活跃超时校验同一 Token，找不到 `last-active` 记录时返回 `TOKEN_FREEZE`（`token 已被冻结`）。这与账号封禁、Nginx 代理和 Redis database 无关。
- 变更：启用 Sa-Token 的动态活跃超时，将客户端的 token 总超时与活跃超时写入每枚 Token 的登录模型；不再在单次登录时修改进程级全局超时配置。Flow 和 App 随后从共享 Redis 的同一 `last-active` 记录读取配置。
- 命令：`JAVA_HOME=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.19/Contents/Home PATH=/Users/yaominliang/Library/Java/JavaVirtualMachines/ms-17.0.19/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests`
- 结果：通过，Reactor 25 个模块均为 `BUILD SUCCESS`。
- 静态检查：`git diff --check` 通过。
- 跳过：未连接生产 Redis，未启动 App/Flow 服务验证真实登录和待办接口；部署含 `forge-starter-auth` 与 `forge-plugin-system` 的新构件后，需要重启 App 与 Flow 并重新登录验证。

## 2026-07-28 — H5 待办表单与 PC 字段对齐

- 根因：H5 待办详情将 `business-code` 表单无条件标记为“请在 PC 端处理”，而 PC 使用 `FlowBusinessForm` 动态加载 PC Vue 组件。采购申请等代码表单实际已由后端 `BusinessTaskFormContextVO` 返回字段、记录数据和节点级读写权限，因此不应被拦截。
- 变更：H5 直接依据后端字段上下文渲染业务代码表单，执行可见、只读和必填规则；新增选择、单选、日期、只读附件展示。动态表单不再仅因选择/日期字段而被拒绝。无移动端字段描述的 PC 私有外部组件，以及 H5 尚未支持的可编辑字段，仍失败关闭，禁止绕过表单直接流转。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；nvm 仍显示 `N/A`，未阻断当前 Node 工具链构建。
- 跳过：未启动真实 App/Flow 服务，需以采购申请待办验证字段权限、暂存和审批提交流程。

## 2026-07-28 — 待办分类弹层与 PC 表单动作对齐

- 变更：流程分类选择器从横向 `scroll-view` 内移至页面根部弹层，避免 H5 固定定位层被滚动容器裁剪或拦截点击；选择、遮罩和关闭按钮均由页面状态控制。
- 表单：待办详情改为先仅凭 `taskId` 请求 `/ai/business/flow/task-form-context`，与 PC 端一致。业务对象/代码表单直接渲染后端返回的字段、记录值与节点读写权限，不再依赖普通 `TaskFormInfo` 携带业务标识。
- 动作：同意/驳回在业务表单场景使用 `/ai/business/flow/task-action`；转办、退回、终结保留 Flow 接口，转办参数与 PC 端保持 `taskId/userId/targetUserId/comment` 一致。业务表单增加“暂存修改”。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；当前 nvm 仍提示 `N/A`，但未阻断实际构建。
- 静态检查：`git diff --check -- forge-h5-ui/src/pages/todo.vue forge-h5-ui/src/pages/todo-detail.vue` 通过。
- 跳过：未启动真实 App/Flow 服务、未执行会修改任务状态的 E2E；需以采购申请待办验证分类弹层、字段渲染、暂存、同意/驳回、转办、退回和终结。

## 2026-07-28 — 业务表单代理与转办交互修正

- 根因：`/ai/business/flow/task-form-context` 被 Vite 的通用 App 代理转发到 8583；该服务未引入 generator 的 `BusinessFlowController`，所以返回 404。Flow 服务 8081 已引入 generator，才是该接口的实际宿主。
- 变更：开发代理新增 `/dev-api/ai/business/flow/** -> Flow 8081`，并置于通用 App 代理前。待办操作栏把“更多”调整到前位并复用次要按钮样式；转办弹层增加清晰的选择态、已选人员回显、确认按钮禁用态与紧凑成员行。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；nvm 的 `N/A` 提示未阻断构建。
- 静态检查：`git diff --check -- forge-h5-ui/vite.config.js forge-h5-ui/src/pages/todo-detail.vue` 通过。
- 部署注意：生产 Nginx 必须将 `/forge-h5-api/ai/business/flow/` 在通用 `/forge-h5-api/` 前代理到 8081；本轮未改动远端 Nginx。

## 2026-07-29 — 移动端流程闭环补齐

- 变更：待办工作台增加“待处理 / 已处理 / 我发起的”三个 Flow 真实列表入口；已办与我发起详情使用只读业务表单上下文，并展示审批历史和 Flow 返回的节点进度；运行中的我发起流程可调用既有撤回接口。
- 表单：新增 `AiSignaturePad` 和 `AiFileUpload`。要求签名的节点会采集笔迹、上传 `/api/file/upload` 后以文件标识提交；可写附件字段同样上传并只向业务表单提交文件标识。日期时间、选择项无选项和未知可写字段均继续遵循“无法安全渲染即阻断”的原则。
- 转办：转办弹层独立维护转办说明、签名与成员分页检索，选择态可见，并过滤当前用户和当前办理人。
- 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-h5-ui build:h5`
- 结果：通过，uni-app 输出 `DONE Build complete.`；shell 仍把 `v20.19.0` 显示为 `N/A`，但未阻断当前 Node 工具链完成构建。
- 静态检查：`git diff --check -- forge-h5-ui/src/api/index.js forge-h5-ui/src/pages/todo.vue forge-h5-ui/src/pages/todo-detail.vue forge-h5-ui/src/components/AiFileUpload.vue forge-h5-ui/src/components/AiSignaturePad.vue code-copilot/changes/h5-mobile-workbench-refactor` 通过。
- 跳过：未启动真实 App/Flow/文件服务，未执行会变更审批状态的 E2E。需要用户环境按 `test-spec.md` 对签名、附件、转办、已办及撤回进行回填验收。

## 2026-07-29 — 业务代码表单空白兜底

- 根因：实际 `task-form-context` 响应的顶层 `fields` 为空，字段元数据位于 `formRef.fields`；同时返回 `代码表单Provider未注册: samplePurchaseOrder`，因此 `recordData` 与节点过滤后的 `fieldPermissions` 也为空。
- 变更：H5 兼容从 `formRef.fields` / `fieldCatalog` 读取字段，避免“业务内容”整块空白。Provider 未注册时，字段强制只读并明确提示服务端缺少 Provider，禁止依据未过滤的通用字段元数据开放编辑或办理。
- 后续：Flow 服务需加载 `samplePurchaseOrder` 的 `BusinessCodeFormProvider` Bean，才能返回真实单据数据和节点级字段权限；这属于服务装配/部署修复，H5 不应伪造该数据。

## 2026-07-29 — 动态表单字段标签修正

- 现象：页面 DOM 出现 `project Name` 而不是配置中的 `项目名称`。这表示页面退回了变量键名自动拆词，而没有命中动态表单字段架构。
- 变更：字段归一化兼容 `fields`、`fieldCatalog`、`formRef.fields`、`form.fields` 与 `rule`；动态表单优先从 `formJson`、再从任务 `fields/formRef/fieldCatalog` 取架构。因此展示优先使用字段的 `label`，例如 `项目名称`。
- 验证：受当前执行环境命令进程被系统以 `137` 终止影响，未能在本次微调后重跑构建；上一轮 H5 构建已通过，需在可用 Node 环境重跑 `pnpm --dir forge-h5-ui build:h5`。

## 2026-07-29 — 移除原始业务键名兜底

- 现象：`field-list` 将 `recordData/variables` 的原始键名自动拆词后展示，如 `project Name`、`warehouse Id`，并暴露业务主键和以分存储的金额字段。这属于开发排障兜底，不是用户界面。
- 变更：删除原始对象键名的字段回退；页面仅根据已返回的表单架构展示标签和值。架构不可用时显示受控说明，不再显示内部 ID、字段编码或技术值。表单 JSON 同时支持双重 JSON 编码解析。

## 2026-07-29 — 流程动作失败可见化

- 现象：审批、驳回等请求在后端返回 500 时，H5 仅写浏览器控制台，用户看不到服务端的 `message`。
- 变更：待办详情统一提取 `data.message`、`response.data.message`、`error.data.message` 等错误结构，并在审批、签收、转办人员加载、暂存等失败路径显示 toast。
- 说明：`数据访问异常，请联系管理员` 是后端已经归一化后的 500 文案；H5 现在会如实展示，具体 SQL/Mapper 根因仍需要从对应 Flow 服务日志定位。
