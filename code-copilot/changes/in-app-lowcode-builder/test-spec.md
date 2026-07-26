# 单测 Spec — 应用内低代码搭建器
> status: apply
> created: 2026-07-21

## 0. 测试原则

- 遵循 `code-copilot/rules/automated-testing-standard.md`：先记录基线，再按任务增量执行。
- 编排 Schema 使用 Vitest 单测先行；组件交互使用 `@vue/test-utils` 定向验证。
- 本变更不新增后端接口或数据库，后端真实 API/Flyway 不在本轮验证范围。

## 1. 测试框架

| 项目 | 值 |
|---|---|
| 前端测试 | Vitest（`pnpm test`） |
| Vue 组件测试 | `@vue/test-utils` |
| 构建 | Vite（`pnpm build`） |
| Lint | ESLint（`pnpm exec eslint`） |

## 2. 覆盖范围

### P0 — 应用编排 Schema

| 函数 | 场景 | 预期 |
|---|---|---|
| `normalizeInAppBuilder` | 空 options / 旧 options | 空应用保持零页面；旧页面数据不改写输入 |
| `createNavigationNode` | 在目录下创建页面/组 | 生成稳定 ID、默认顺序和正确父节点；首个页面成为默认入口 |
| `moveNavigationNode` | 移动页面、循环父子关系 | 正确移动；循环移动被拒绝 |
| `removeNavigationNode` | 删除含子节点目录 | 必须按指定策略处理子节点 |
| `insertPageComponent` | 容器、组件后、页面末尾插入 | 使用既有组件默认属性并返回选中组件 ID |

### P0.1 — 空应用与页面模板

- 新建应用没有导航节点时展示应用介绍空态，不持久化虚拟首页。
- 新建页面先选择模板；CRUD、左树右表、主子表必须绑定已有业务对象。
- 数据模板只保存 `objectRef`、模板类型等轻量引用；不得复制业务对象字段、表单或列表 Schema。

### P1 — 前端状态与组件

- `mergeInAppBuilderOptions` 保留未知 options。
- 草稿保存失败保持脏状态。
- 页面树空目录显示“在本组创建页面”。
- 页面树更多菜单可完成重命名、移动、上下排序；删除有子项页面组时必须选择子项处理方式。
- 组件插入弹窗的搜索、分类和点击插入。
- 页面树下方组件 Popover、图标槽位、组件拖入页面和页面内排序手柄的浏览器交互；确认中间区域不存在画布网格、自由定位或缩放锚点。

### P2 — 路由和浏览器验收

- 应用运行壳打开首页和对象页占位。
- 编辑者可切换编辑态；普通用户不出现编辑入口。
- 创建目录 → 创建页面 → 插入组件 → 保存 → 刷新 → 预览。

## 3. 执行计划

- [x] Step 1：新增 Schema 红灯用例并执行定向 Vitest。
- [x] Step 2：完成 Schema 实现，确认绿灯。
- [ ] Step 3：完成页面树/组件插入/草稿测试。
- [x] Step 4：执行定向 ESLint 和 `pnpm build`。
- [ ] Step 5：按条件启动 Vite 完成浏览器验收并记录服务清理。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|---|---|---|---|---|
| 2026-07-21 | 实施前基线 | 待执行 | pending | 新变更尚无前端测试 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|---|---|---|---|---|---|
| 2026-07-21 | Task 1-2 | 编排 Schema 与 options 合并定向 Vitest | `pnpm --dir forge-admin-ui test src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js` | passed | 5 tests passed；先记录缺少模块的预期 Red 结果，再实现并通过 Green。 |
| 2026-07-22 | Task 7-8、6、10 | Runtime Popover、页面流、列表设计器同款拖拽状态、空页推荐与草稿预览入口 | `pnpm --dir forge-admin-ui exec eslint …`、定向 Vitest、`pnpm --dir forge-admin-ui build` | passed | ESLint 通过，6 个编排 Schema 用例通过，生产构建完成。未启动 Vite，Popover 定位、拖入、锚点缩放、排序和保存回显待浏览器验收。 |
| 2026-07-22 | Task 5 | 页面树更多操作与删除子项策略 | `pnpm --dir forge-admin-ui exec eslint 'src/views/app-center/application-runtime.[applicationCode].vue'`、定向 Vitest、生产构建 | pending | 当前命令执行环境立即以退出码 137 终止，尚无新的静态或构建通过结论；待终端资源恢复后复跑。 |
| 2026-07-22 | Task 6 | 页面模板选择与空页推荐 | 同上，额外覆盖“选择模板后创建注册区块，不复制对象 Schema” | pending | 与 Task 5 共用的终端资源阻塞仍未解除，尚未生成新的验证结论。 |
| 2026-07-22 | Task 9 | 业务对象页创建、对象引用与四个配置深链 | 同上，额外覆盖 `objectRef`、目标面板和 `returnTo` | pending | 终端资源持续以 137 终止，待恢复后验证。 |
| 2026-07-22 | Task 10 | 保存草稿、草稿预览、退出未保存确认与发布跳转 | 同上，额外覆盖草稿标识和放弃修改后的模型重载 | pending | 终端资源持续以 137 终止，待恢复后验证。 |
| 2026-07-22 | 中间组件镜像拖拽与实时换位 | 镜像预览、悬停目标即时腾位、松手落位与灰色原位占位 | `pnpm --dir forge-admin-ui exec eslint 'src/views/app-center/application-runtime.[applicationCode].vue'`；`git diff --check -- 'forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue'`；`pnpm --dir forge-admin-ui build` | passed | ESLint 与 diff 检查无输出；Vite 构建 8721 modules、3m21s。未启动 Vite，真实指针拖动的跟手与换位观感待浏览器验收。 |
| 2026-07-22 | 动态拖拽阴影 | 阴影与镜像共用实时坐标，影子命中即触发目标腾位 | `pnpm --dir forge-admin-ui exec eslint 'src/views/app-center/application-runtime.[applicationCode].vue'`；`git diff --check -- 'forge-admin-ui/src/views/app-center/application-runtime.[applicationCode].vue'`；`pnpm --dir forge-admin-ui build` | passed | ESLint 与 diff 检查无输出；Vite 构建 8721 modules、3m09s。未启动 Vite，实际指针拖动和交换观感待浏览器验收。 |
| 2026-07-23 | 应用内表单资产主链 | 表单资产归一化/保存、表单设计器复用、表单字段派生到页面组件 | `pnpm --dir forge-admin-ui exec eslint …`；`pnpm --dir forge-admin-ui test src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js`；`pnpm --dir forge-admin-ui build` | passed | ESLint 0 error/0 warning；Schema 定向 Vitest 7 用例通过；生产构建 8721 modules 成功。未启动 Vite 或业务服务，表单拖放与保存回显待浏览器验收。 |
| 2026-07-23 | 标题组件迁移与文字格式工具条 | 标题区块持久化、文字类区块的即时格式渲染 | 定向 ESLint、`in-app-builder-schema.spec.js`、生产构建、`git diff --check` | passed | 固定标题改为 `page-title` 区块；迁移标记会持久化，用户删除标题后不会在下次加载时重复补回。文字工具条的格式直写区块 props。ESLint 0 error/0 warning；Vitest 7 用例通过；构建仅有仓库既有 `UserSelectModal` 提示。未启动 Vite，拖拽和选中视觉待浏览器验收。 |
| 2026-07-23 | 内联标题富文本组件 | 主标题/副标题分别编辑，浮动工具条按当前文本行或选区定位 | 定向 ESLint、`in-app-builder-schema.spec.js`、生产构建、`git diff --check` | passed | 新增可复用 `InlineRichText`；不再在区块内部覆盖工具条。标题每行独立保存文案和格式，空副标题只在编辑状态显示占位。ESLint 0 error/0 warning；Vitest 7 用例通过；构建仅有既有 `UserSelectModal` 提示。未启动 Vite，需浏览器验收实际选区定位。 |
| 2026-07-23 | 内联编辑交互修正 | 原生下拉、选区触发与收起行为 | 定向 ESLint、生产构建、`git diff --check` | passed | 文字直接 contenteditable 输入；工具条只由非空文字选区触发，点击空白或其他区域收起。取消工具条全局 `preventDefault`，原生字号下拉可打开。ESLint 0 error/0 warning；构建仅有既有 `UserSelectModal` 提示。 |
| 2026-07-23 | 标题富文本块模型 | 单一 HTML 编辑区、原生输入与兼容旧标题数据 | 定向 ESLint、`in-app-builder-schema.spec.js`、生产构建、`git diff --check` | passed | 标题组件现在以单个 `contenteditable` HTML 区承载任意多行内容，默认结构为 h1/p；运行页直接编辑，右侧只提供 HTML 源码兜底。无自定义选区拦截。ESLint 0 error/0 warning；Vitest 7 用例通过；构建仅有既有 `UserSelectModal` 提示。 |

| 2026-07-23 | 标题文本光标与根页面防重叠 | 文本区原生输入光标；结束移动或尺寸调整后的根级块碰撞整理 | 定向 ESLint、`in-app-builder-schema.spec.js`、`git diff --check`、生产构建 | passed-static | 富文本编辑区及其内部元素固定为 text 光标；浮动格式条仍只在真实非空文本选区出现，不会在组件选中时常驻。根页面仅对根级块在移动结束/调尺寸结束/尺寸菜单/新增后进行横向碰撞整理，将下方相交块平滑下推；嵌入布局 children 不参与。ESLint 0 error/0 warning；Vitest 7 用例通过；diff 无输出。生产构建已启动，当前工具只返回 Vite transforming 阶段，未将其作为通过结论。 |

| 2026-07-24 | 标题块切换为完整富文本输入 | 复用 WangEditor；编辑态输入、粘贴、换行、工具栏操作与只读预览 | 定向 ESLint、`in-app-builder-schema.spec.js`、`git diff --check`、生产构建 | passed-static | 移除手写 contenteditable/选区浮层；标题块直接使用项目已集成 WangEditor。进入文本后才显示工具栏，点击工具栏不触发提前收起；预览模式不显示工具栏。标题默认高度扩展为 176px，并在旧页面迁移时同步预留足够纵向空间。ESLint 0 error/0 warning；Vitest 7 用例通过；diff 无输出。构建工具仅返回 Vite transforming 阶段，未作为通过结论。 |

| 2026-07-24 | 富文本浮层层级与无边框外观 | 工具栏/字号下拉不被裁切；富文本聚焦时隐藏区块操作层；移除编辑器默认边框 | 定向 ESLint、`git diff --check` | passed | 标题块、富文本容器和编辑器滚动区在聚焦时允许溢出；标题块层级提升。原区块拖动、更多和缩放锚点在富文本聚焦时隐藏，避免压住工具栏。WangEditor 工具栏和正文容器均移除默认边框。ESLint 0 error/0 warning；diff 无输出。 |

| 2026-07-24 | 富文本首次点击直达 | 标题文本首次点击直接获得编辑焦点，不被透明区块操作层拦截 | 定向 ESLint、`git diff --check` | passed | 页块顶部操作层默认不再接收指针事件，只有实际的拖动手柄、更多按钮和颜色浮层恢复 pointer events。标题文本首击可直接落到 WangEditor；不会再发生首击看到旧操作头、第二次才进入输入的情况。ESLint 0 error/0 warning；diff 无输出。 |

| 2026-07-24 | 富文本工具栏显示时机 | 首击进入输入但不显示工具栏；只有拖选非空文字才显示格式项 | 定向 ESLint、`git diff --check` | passed | WangEditor 工具栏的显示条件由焦点改为同一编辑区的非空原生选区。首次点击的折叠光标不会显示头部；选中文字后才挂载工具栏，点击编辑区外会收起。ESLint 0 error/0 warning；diff 无输出。 |

| 2026-07-24 | 富文本编辑时保留尺寸锚点 | 输入或选字时，顶部拖动/更多隐藏而四周缩放锚点继续可用 | 定向 ESLint、`git diff --check` | passed | 富文本聚焦态不再隐藏 `.page-block-resize-anchor`，仅隐藏顶部旧操作层；用户可直接编辑富文本，同时继续通过八向锚点调整当前组件尺寸。ESLint 0 error/0 warning；diff 无输出。 |

| 2026-07-24 | 富文本浮动工具条与尺寸锚点 | 正文无顶部编辑器头部；选区工具条浮于选区附近；锚点可拖动 | 定向 ESLint、`git diff --check` | passed | WangEditor 工具栏由组件内部改为 Teleport 到 body 的选区浮层，不再占据或挤压富文本正文。富文本输入区移除高 z-index，页面块本身维持层级，四周尺寸锚点可见且能获得拖拽事件。ESLint 0 error/0 warning；diff 无输出。 |

| 2026-07-24 | 富文本单工具条与焦点选中 | 选字时只显示一套格式工具；首次进入编辑立即选中组件并显示尺寸锚点 | 定向 ESLint、`git diff --check` | passed | 关闭 WangEditor 默认 text hoverbar，避免与平台选区浮动工具条重复。富文本 focus 事件向页面壳派发组件激活，页面壳立即设置 selectedPageBlockId，因此八向尺寸锚点会在首次输入时渲染。ESLint 0 error/0 warning；diff 无输出。 |

| 2026-07-24 | 富文本首次短 Hoverbar 回归 | 鼠标经过或首次选字不出现 WangEditor 内置短工具条；仅保留统一选区工具条 | 定向 ESLint、`git diff --check` | passed-static | `hoverbarKeys` 改为空对象，避免 `text` 键仍触发 WangEditor 实例化 hoverbar；并以全局 `.w-e-hover-bar` 强制隐藏动态挂在编辑器外层的原生短条。ESLint 0 error/0 warning；diff 无输出。未启动 Vite，真实浏览器交互待人工复核。 |
| 2026-07-24 | 富文本选区触发时序回归 | 首次普通点击不误弹；每次鼠标拖选或键盘扩选结束后均显示格式工具条 | 定向 ESLint、`git diff --check` | passed-static | 不再用全局 `selectionchange` 在焦点刚进入时判断选区。改为鼠标按下时先收起，文档 `pointerup` 的下一帧读取选区；键盘选择由 `keyup` 读取。ESLint 0 error/0 warning；diff 无输出。未启动 Vite，真实浏览器交互待人工复核。 |
| 2026-07-24 | 富文本工具条重复选区回归 | 连续两次及以上选择文字时，格式工具条均可稳定打开 | 定向 ESLint、`git diff --check` | passed-static | 工具条从条件渲染改为在编辑器创建后常驻实例、仅用 `v-show` 切换可见性，避免第二次选择时重建 `WangToolbar` 丢失与编辑器的绑定。ESLint 0 error/0 warning；diff 无输出。未启动 Vite，真实浏览器交互待人工复核。 |
| 2026-07-24 | 应用运行页 AiCrudPage 列回显 | 右侧配置的列表字段在运行页应渲染为实际列，不能只显示操作列 | 运行页定向 ESLint、编排 Schema Vitest、`git diff --check` | passed-static | 根页面渲染此前固定传入空 `fields`，导致区块 `fieldRefs` 无法解析为列。现按每个区块绑定的表单资产传入字段目录，拖拽镜像也使用同一字段来源。ESLint 0 error/0 warning；Vitest 7 tests passed；diff 无输出。未启动 Vite，真实接口数据待人工复核。 |
| 2026-07-24 | 运行页编辑入口与组件选择器 | 非编辑态不显示顶部设计头；侧栏图标进入编辑；组件选择器按列表/图表/视图/其他分组 | 运行页定向 ESLint、编排 Schema Vitest、`git diff --check` | passed-static | 顶栏仅在编辑态存在；侧栏折叠按钮左侧新增无文字的编辑入口。组件选择器由横向分类+双列描述列表改为可搜索的四组图标网格，复用原组件目录和插入逻辑。ESLint 0 error/0 warning；Vitest 7 tests passed；diff 无输出。未启动 Vite，移动端视觉待人工复核。 |
| 2026-07-24 | 组件选择器搜索区间距 | 搜索框独立清晰，和首个组件分组保留足够间距 | 运行页定向 ESLint、`git diff --check` | passed-static | 搜索区加大底部留白和分组间距，输入框统一为浅灰圆角底、聚焦蓝色内描边，避免控件与“列表”分组视觉粘连。ESLint 0 error/0 warning；diff 无输出。未启动 Vite，视觉待人工复核。 |
| 2026-07-24 | 组件选择器图标资产回归 | 每个现有页面组件优先显示 `assets/images/form` 的既有图标 | 运行页定向 ESLint、`git diff --check` | passed-static | 使用 `import.meta.glob` 预加载表单图标目录，以区块类型映射对应拼音文件名；未映射的未来组件仍回退到统一线性 SVG。ESLint 0 error/0 warning；diff 无输出。未启动 Vite，资源加载视觉待人工复核。 |
| 2026-07-24 | 扩展页面组件图标回归 | 穿梭框、条形码、倒计时等页面组件显示其目录内对应 PNG | 运行页定向 ESLint、`git diff --check` | passed-static | 补齐 `page-widget-schema` 中已注册组件的图标映射：穿梭框、水印、Vue 组件、Markdown、条形码、二维码、日历、代码、倒计时、描述、公示、列表、日志、数值动画、面包屑、菜单和分页。ESLint 0 error/0 warning；diff 无输出。 |
| 2026-07-24 | 侧栏应用标识 | 侧栏标题使用系统 Logo，不显示通用文件夹图标 | 运行页定向 ESLint、`git diff --check` | passed-static | 复用 `AuthImage + tenantStore.systemLogo`，并以项目默认 `logo.png` 作为兜底；图标槽保持 18px，移除虚线边框和灰底。ESLint 0 error/0 warning；diff 无输出。 |
| 2026-07-24 | 页面内添加组件入口与紧凑布局 | 添加组件在 page-surface 内自由拖动；默认圆形加号、悬停显示文字；页面留白收紧 | 运行页定向 ESLint、`git diff --check` | passed-static | 添加组件入口移出 grid host，改为 page-surface 的绝对定位子元素，拖动边界按页面表面实际尺寸限制。默认 46px 圆形主按钮，hover/focus 扩展为“添加组件”。运行区、页面表面、内容流和对象提示卡的间距同步收紧。ESLint 0 error/0 warning；diff 无输出。 |

| 2026-07-24 | 添加入口图标与应用编辑历史 | 添加组件按钮加号双轴居中；应用页面撤销/重做与快捷键 | `source ~/.nvm/nvm.sh && nvm use v24.13.0 && pnpm exec eslint 'src/views/app-center/application-runtime.[applicationCode].vue' && pnpm exec vitest run 'src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js' && git diff --check -- …` | passed-static | `+` 已改为 Flex 双轴居中；复用列表设计器的 `ArrowUndoOutline`/`ArrowRedoOutline`，记录导航、组件、表单资产和布局快照，输入框/富文本保留浏览器原生快捷键。ESLint 0 error、0 warning；Vitest 7 tests passed；diff 无输出。当前 pnpm 11 要求 Node 22+，因此验证使用已安装的 Node 24。 |
| 2026-07-24 | 应用页 CRUD 运行规则与富文本操作层 | 填充容器清除旧尺寸；富文本选中后顶部拖动/更多仍可点；未配置接口的 CRUD 不请求空地址 | `source ~/.nvm/nvm.sh && nvm use v24.13.0 && pnpm exec eslint 'src/views/app-center/application-runtime.[applicationCode].vue' 'src/components/lowcode-builder/page/GridBlockRenderer.vue' && pnpm exec vitest run 'src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js' && git diff --check -- …` | passed-static | 真实预览仅在显式开启并配置接口后发请求；其余情况展示静态结构并禁用新增、导入和导出，避免空地址落到当前页面而 404。ESLint 0 error、0 warning；Vitest 7 tests passed；diff 无输出。 |
| 2026-07-24 | 静态 CRUD 新增交互 | 静态预览保留“新增”入口，但提交不得请求空接口 | `source ~/.nvm/nvm.sh && nvm use v24.13.0 && pnpm exec eslint 'src/components/lowcode-builder/page/GridBlockRenderer.vue' && git diff --check -- …` | passed-static | 点击新增会打开表单；提交显示“需绑定真实接口”的反馈，不隐藏新增按钮，也不发送 HTTP 请求。ESLint 0 error、0 warning；diff 无输出。 |
| 2026-07-24 | 应用页 CRUD 默认真实运行绑定 | 对象页自动解析当前对象、单对象应用自动解析唯一对象；使用对象 `configKey` 读取已有 CRUD 运行配置，并透传真实 `apiConfig`、列、查询和编辑 Schema | 定向 ESLint、编排 Schema Vitest、`git diff --check`、生产构建 | passed-static / build-inconclusive | ESLint 0 error/0 warning；Vitest 1 文件 7 用例通过；diff 无输出。生产构建仅返回 Vite transforming 和既有 `UserSelectModal` 命名冲突提示，未取得完成汇总，未作为通过结论。未启动业务服务，真实接口请求需在已登录的本地环境浏览器验收。 |
| 2026-07-26 | 页面设计与业务对象设计入口收敛 | 工作台入口、页面设计上下文、对象设计上下文与返回链路 | 定向 ESLint、编排 Schema Vitest、`git diff --check`、生产构建 | passed-static / build-inconclusive | “设计页面”固定打开应用 runtime 的 `edit=1`；“管理业务对象”进入对象工作台；对象设计器和页面设计器各自显示职责。ESLint 0 error/0 warning；Vitest 7 tests passed；diff 无输出。生产构建只返回 Vite transforming 和既有 `UserSelectModal` 命名冲突提示，未取得完成汇总。未启动 Vite 或业务服务。 |
| 2026-07-26 | 空页面组件引导视觉升级 | 工作台按钮一致性、空页面横版引导、组件图标与组件插入 | 定向 ESLint、编排 Schema Vitest、`git diff --check`、生产构建 | passed-static / build-inconclusive | “设计页面”改为同级描边操作；移除重复旧入口。空页面引导改成淡蓝页面线框预览及带 Ionicons 的可点击组件模块，仍复用原 `appendPageBlock` 插入逻辑。ESLint 0 error/0 warning；Vitest 7 tests passed；diff 无输出。生产构建只返回 Vite transforming 与既有 `UserSelectModal` 命名冲突提示，未取得完成汇总。 |
| 2026-07-26 | 菜单空图标、CRUD 默认尺寸与右侧属性优先 | 菜单无默认方框；AiCrudPage 默认高度；右侧属性/数据视图切换 | 定向 ESLint、编排 Schema Vitest、`git diff --check` | passed-static | 无图标菜单不再渲染占位方框。新建 AiCrudPage 默认 `heightMode=full`。嵌入式属性设计器默认落在“属性”，外层数据绑定收进“数据”页签，并将表单绑定整理为紧凑卡片。ESLint 0 error/0 warning；Vitest 7 tests passed；diff 无输出。 |
| 2026-07-26 | 新页面标题、CRUD 预览滚动与极简新建 | 空白页初始标题、CRUD 内容可见性与左侧新建交互 | 定向 ESLint、编排 Schema Vitest、`git diff --check` | passed-static | 空白模板不再跳过标题初始化，新页面立即包含 `page-title`。CRUD 预览链路改为弹性高度与内部滚动，避免中间内容裁切。左侧新建改为直接选择页面/页面组；页面自动按当前数量命名，后续通过更多菜单重命名。ESLint 0 error/0 warning；Vitest 7 tests passed；diff 无输出。 |

## 6. 执行证据

- `execution-log.md`：记录每次命令和关键结果。
- 服务启动与停止：本轮尚未启动服务。

## 6. 2026-07-26 CRUD 预览滚动增量验证

- 范围：CRUD 预览容器改为纵向分区，恢复表格 body 单一滚动区；移除组件壳的固定最小高度，防止越框覆盖相邻组件。
- 已通过：`GridBlockRenderer.vue` 定向 ESLint、编排 Schema Vitest（7/7）、`git diff --check`、前端生产构建（8779 modules）。
- 未执行：Vite 浏览器交互验收和真实业务接口验收；本轮未启动服务。

## 7. 2026-07-26 页面创建与布局移入增量验证

- 范围：新页面默认介绍页且不自动插入标题；根组件可移入已有布局组合/标签页；小屏默认可见添加组件入口；顶栏低频操作收进更多菜单。
- 已通过：运行页定向 ESLint、编排 Schema Vitest（7/7）、`git diff --check`、前端生产构建（8779 modules，2 分 33 秒）。
- 未执行：浏览器点击“移入布局”、窄屏定位和下拉菜单的人工验收；本轮未启动服务。

## 8. 2026-07-26 组件更多菜单图标增量验证

- 范围：区分“复制到当前页面”“复制到其他页面”“移入布局”的菜单图标语义。
- 已通过：运行页定向 ESLint、`git diff --check`。
- 未执行：未启动 Vite；本轮为既有 Ionicons 图标替换，不涉及运行时数据或接口。

## 9. 第二阶段发布菜单与权限同步验证

- P0：发布时根据已发布快照同步页面目录、页面菜单、角色资源关系，并在回滚时重新同步。
- P0：草稿保存不得写入 `sys_resource`；页面隐藏/删除只能停用已管理资源。
- P1：已发布运行配置必须按页面权限过滤；发布检查覆盖首页和指定角色页面。
- 命令：生成器模块 Maven 编译、Admin 模块 Maven 编译、后端定向单测、相关前端 ESLint/Vitest/生产构建、`git diff --check`。

## 10. 新建应用直达页面设计验证

- 新建应用默认使用 `BLANK` 起点，只显示基础信息并直接创建。
- 创建成功后新开应用运行壳编辑态，地址携带 `edit=1&fresh=1`；不得默认初始化旧式 CRUD 模板。
- “从已有对象整理应用”仍保留为迁移/高级入口。
