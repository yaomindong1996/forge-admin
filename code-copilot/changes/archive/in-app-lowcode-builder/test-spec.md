# 单测 Spec — 应用内低代码搭建器
> status: apply
> created: 2026-07-21

## 0. 测试原则

- 遵循 `code-copilot/rules/automated-testing-standard.md`：先记录基线，再按任务增量执行。
- 编排 Schema 使用 Vitest 单测先行；组件交互使用 `@vue/test-utils` 定向验证。
- Task 22 新增应用级表单数据准备接口与后端事务编排；本轮验证 Java 编译、定向 JUnit 和前端契约，不启动真实服务、数据库或 Flyway。

## 1. 测试框架

| 项目 | 值 |
|---|---|
| 前端测试 | Vitest（`pnpm test`） |
| Vue 组件测试 | `@vue/test-utils` |
| 后端测试 | JUnit 5（Maven Surefire） |
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
- CRUD、左树右表、主子表模板直接创建页面和页面表单，不要求先绑定已有业务对象。
- 保存表单后自动准备内部数据存储并显式回绑 CRUD；使用已有数据保留为高级设置。

### P0.2 — 自动托管表单数据表

- 首次保存 `PAGE_FORM` 托管表单时，元数据事务先提交，随后执行一次安全 `CREATE TABLE` 同步。
- 重复保存同一表单复用对象，并根据最新设计再次同步；新增字段走安全 `ADD COLUMN`。
- 数据源必须同时满足启用、可写、非只读、`LOWCODE_RUNTIME` 和 `allowRuntimeDdl=1`；不满足时不创建新托管对象。
- 自动同步入口拒绝非 `PAGE_FORM` 对象，手工同步继续要求权限、设计版本和二次确认。
- DDL 禁用、非追加式变更或执行异常时不回滚已经提交的表单元数据，返回可重试用户提示。

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

### Task 25 增量计划

1. 先运行 `BusinessApplicationFormDataServiceTest` 与 `BusinessObjectDatabaseSyncServiceTest` 新用例，确认在缺少自动同步实现时失败。
2. 使用 JDK 17 执行生成器 reactor `test-compile`，避免 Surefire 复用旧测试 class。
3. 定向执行 `BusinessApplicationFormDataServiceTest,BusinessObjectDatabaseSyncServiceTest,BusinessObjectTableMappingServiceTest`，要求 `Tests run` 非 0 且无失败。
4. 执行相关 Java 文件 `git diff --check`；本轮无前端协议变化时不重复运行前端构建。
5. 不启动 MySQL、Flyway、后端或 Vite，不实际执行 DDL；真实数据库建表和保存表单链路由用户重启环境后验收。

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

## 11. 2026-07-27 发布与统一体验增量测试

### 11.1 后端必跑

- 页面依赖检查器：纯内容零对象通过；零对象 CRUD 阻断；单对象回退通过；多对象显式绑定通过；嵌套区块失效引用阻断；多个主对象阻断。
- 应用对象编排：单个 `SHARED` / `DETAIL` 关联保存后自动规范为 `PRIMARY`，两个 `PRIMARY` 仍拒绝。
- 工作台轻量就绪度：纯内容应用不再显示主对象阻断；有数据依赖且无法解析对象时显示结构化阻断。
- 已发布运行配置：读取 `lastPublishVersion` 不可变快照；未发布应用拒绝；页面权限过滤、父目录保留和首页回退正确。
- 发布运行单：`PAGE_MENUS` 有可读步骤名，步骤执行后失败状态为 `PARTIAL`。

### 11.2 前端必跑

- 普通运行态请求 published runtime API；`edit=1` / `draft=1` 请求 workspace 草稿；查询模式变化时重新加载，且不存在重复 mounted 请求。
- 应用中心仅保留一个“新建应用”按钮并以 `BLANK` 创建，空态不再暴露“页面应用 / 传统业务对象应用”分类。
- 页面设计器内发布抽屉能触发检查、显示问题、执行发布并刷新当前应用。
- 缺少业务对象的数据模板在当前设计器提供新建、关联已有对象和数据库导入入口。

### 11.3 验证命令

- 后端：生成器模块相关 JUnit 定向测试（确认 `Tests run` 非 0）及模块编译；如聚合构建被仓库既有注解处理器问题阻断，记录完整证据。
- 前端：相关 Vue/JS ESLint、编排 Schema Vitest、`NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`。
- 本轮不启动真实数据库、Flyway、后端服务或 Vite；浏览器角色权限与发布 E2E 作为人工验收项明确记录。

### 11.4 执行结果

- 后端 reactor `test-compile`：29 个模块 `BUILD SUCCESS`，生成器编译 91 个测试源文件。
- 后端定向测试：6 个测试类共 26 个用例，`Failures: 0, Errors: 0, Skipped: 0`。
- 前端 ESLint：0 error；`index.vue` 保留 3 条既有 `vue/attributes-order` warning。
- 编排 Schema Vitest：1 个文件、9 个用例全部通过。
- 前端生产构建：Node `v24.14.0`，最终重跑 Vite 转换 8789 个模块，`built in 2m 50s`；仅保留仓库既有组件重名、动态/静态导入和 CSS 注释警告。
- `git diff --check`：通过，无空白错误。
- 未执行：真实 MySQL/Flyway、后端/Vite 服务、浏览器角色权限和发布菜单 E2E；本轮未启动任何需清理的服务。

## 12. 2026-07-28 页面表单升级与字段配置增量测试

### 12.1 自动化范围

- 当稳定页面模型的 `fields` 为空、当前组件字段目录非空时，列表布局同步必须保留已选 `fieldRefs` 与 `searchFieldRefs`。
- 页面表单升级载荷必须包含归一化 `formDesignerSchema` 和可持久化业务字段，保留字段编码、数据库列名、必填、组件类型与列表/表单可见性。
- `AiCrudPage` 未绑定对象时可直接选择应用已有对象；从当前页面表单创建对象后，新增对象加入应用并自动绑定当前区块。
- 对象尚未发布 CRUD 配置时，页面编辑态可从对象设计草稿读取字段目录；正式运行态不得读取设计草稿兜底。

### 12.2 边界与人工项

- 本轮不启动 MySQL、Flyway、后端服务或 Vite，不执行真实 DDL；页面表单升级后的数据库同步和对象发布保留人工确认。
- 浏览器人工验收：字段选择/隐藏/排序、已有对象直绑、从表单新建对象、关闭抽屉后当前 CRUD 回显对象和字段、发布阻断从“未绑定对象”推进到对象自身真实就绪项。

### 12.3 执行结果

- 新增 2 个纯函数测试文件：字段同步 1 例、页面表单升级与对象草稿字段归一化 3 例；连同原编排 Schema 共 3 个文件、13 个用例全部通过。
- 相关 Vue/JS ESLint：0 error、0 warning。
- 生产构建：Node `v24.14.0`，Vite 转换 8790 个模块，`built in 1m 54s`；保留仓库既有 `UserSelectModal` 重名、动态/静态导入和 CSS `//` 注释警告。
- `git diff --check`：通过，无空白错误。
- 未启动 MySQL、Flyway、后端服务或 Vite；未执行真实 DDL、对象发布或浏览器 E2E，无服务需要清理。

## 13. 2026-07-28 表单保存自动准备数据存储增量测试

### 13.1 自动化范围

- 后端：默认可写 `LOWCODE_RUNTIME` 数据源选择、首次创建、重复保存复用、关联丢失恢复、空字段拒绝和无可写数据源失败关闭。
- 前端：单表单多 CRUD 只准备一次、空持久化字段跳过、托管对象继续同步、手工绑定不改写、根/嵌套区块回绑、19 位 ID 字符串保持和页面级手工绑定保护。
- 交互：数据页面模板直接创建页面并进入表单设计；表单草稿、自动准备、对象引用二次保存和失败重试使用单层反馈。

### 13.2 执行结果

- 生成器 reactor `test-compile`：29 个模块 `BUILD SUCCESS`，生成器编译 92 个测试源。
- 后端定向 JUnit：7 个测试类共 32 个用例，0 failure、0 error、0 skipped；新服务测试使用项目 Stub/Proxy 风格，不依赖本机不可用的 Mockito inline attach。
- 前端定向 ESLint：0 error、0 warning。
- 前端 Vitest：4 个文件共 18 个用例全部通过。
- 前端生产构建：Node `v20.19.0`，Vite 转换 8791 个模块，`built in 2m 10s`；仅保留仓库既有组件重名、动态/静态导入和 CSS 注释警告。
- 未启动 MySQL、Flyway、后端服务、Vite 或浏览器；未执行 DDL、对象发布或应用发布，无服务需要清理。真实接口和浏览器点击链路保留人工验收。

## 14. 2026-07-28 自动回绑后的草稿 CRUD 预览增量测试

### 14.1 自动化范围

- `buildRuntimeCrudProps` 在设计态为列表、详情、新增、修改、删除等端点追加 `designPreview=1`，替换 `当前配置` 占位符，并避免重复追加；正式运行端点保持不变。
- 应用编辑/草稿模式调用 `crudConfigRender` 时显式启用设计预览；对象设计器字段兜底同样标记为草稿运行参数。
- 设计画布默认静态预览，不自动加载尚未同步数据库的数据；显式开启真实数据预览时仍通过带授权标记的草稿 CRUD 接口请求。
- 执行相关 JS/Vue ESLint、定向 Vitest、前端生产构建和 `git diff --check`。

### 14.2 边界与人工项

- 不启动后端、Vite、MySQL 或 Flyway，不执行 DDL、对象发布或应用发布。
- 真实浏览器保存表单后的 Network 验收由用户重启现有环境后执行，重点确认默认画布没有自动 `/ai/crud/.../page` 请求，开启真实数据预览后的请求携带 `designPreview=1`。

### 14.3 执行结果

- Node `v20.19.0`；相关 JS/Vue ESLint 0 error、0 warning。
- Vitest：3 个文件、9 个用例全部通过，覆盖设计态全端点标记、正式运行不改写、标记去重、草稿参数识别以及既有表单回绑/字段同步回归。
- Vite 生产构建：8791 个模块，`built in 2m 13s`；保留仓库既有组件重名、动态/静态导入和 CSS 注释警告。
- `git diff --check` 通过。未启动后端、Vite、MySQL、Flyway 或浏览器，未执行 DDL、对象发布或应用发布，无服务需要清理。

## 15. 2026-07-28 模型编码边界与应用编码自动生成增量测试

### 15.1 自动化范围

- 后端命名：`normalizeModelCode` 和 `buildModelCode` 对长输入始终返回不超过 48 位的合法编码；对象编码已包含业务域前缀时不重复拼接。
- 表单数据准备：超长应用编码与表单名称组合创建托管对象时，请求中的 `modelCode` 不超过 48 位，且对象关联/设计同步语义保持不变。
- 应用创建：编码为空时根据业务域和名称自动生成；中文未知词使用稳定摘要；租户内重名按后缀避让；显式重复/非法编码继续拒绝；创建结果同时返回 ID 和最终编码。
- 前端命名：数据库表导入和对象向导使用的 `buildModelCode` 与后端一致限制为 48 位。
- 前端交互：普通新建不把应用编码作为必填项，编码仅在高级设置中可选填写；创建完成、初始化失败保留草稿和成功跳转都使用服务端最终编码。

### 15.2 验证命令与边界

- 后端先执行生成器 reactor `test-compile`，再执行 `BusinessNamingServiceTest`、`BusinessApplicationFormDataServiceTest`、`BusinessApplicationServiceTest` 和 `BusinessApplicationControllerTest` 定向 JUnit，确认测试数非 0。
- 前端使用 Node `v20.19.0` 执行相关 JS/Vue ESLint、命名工具 Vitest 和生产构建；最后执行目标文件及全局 `git diff --check`。
- 不启动 MySQL、Flyway、后端、Vite 或浏览器，不执行真实 DDL、对象发布或应用发布。真实接口重试和页面跳转由用户重启当前环境后人工验收。

### 15.3 执行结果

- 生成器 reactor `test-compile`：29 个模块 `BUILD SUCCESS`，生成器编译 93 个测试源。
- 后端定向 JUnit：4 个测试类共 27 个用例，0 failure、0 error、0 skipped。
- 前端定向 ESLint：修正 1 条 import 顺序错误后最终 0 error、0 warning。
- 前端 Vitest：3 个文件共 10 个用例全部通过，包含模型编码、创建响应和既有表单数据准备回归。
- 前端生产构建：Node `v20.19.0`，Vite 转换 8792 个模块，`built in 1m 50s`；仅保留仓库既有组件重名、动态/静态导入和 CSS 注释警告。
- 未启动 MySQL、Flyway、后端、Vite 或浏览器，未执行 DDL、对象发布或应用发布；真实接口重试和页面跳转保留人工验收。

## 16. Task 25 自动托管数据表增量结果

- 生成器 reactor `test-compile`：29 个模块 `BUILD SUCCESS`，生成器编译 93 个测试源。
- 后端定向 JUnit：`BusinessApplicationFormDataServiceTest`、`BusinessObjectDatabaseSyncServiceTest`、`BusinessObjectTableMappingServiceTest` 共 21 个用例，0 failure、0 error、0 skipped。
- 已覆盖元数据事务提交后才执行建表、首次/重复保存同步、DDL 失败保留设计并重试、非托管对象拒绝、非追加式 DDL 拒绝、自动建表开关和历史快照兼容。
- 目标差异 `git diff --check` 通过；本轮没有前端协议变化，因此未重复执行前端构建。
- 未启动 MySQL、Flyway、后端、Vite 或浏览器，未实际执行 DDL；真实建表由用户重启后重新保存原表单验收。

## 17. Task 26 查询与发布状态增量计划

1. 前端先补红灯用例：运行字段只含 ID 时仍合并保留表单字段；`searchFieldRefs` 独立于列表 `fieldRefs` 生成查询 Schema；自动托管绑定首次启用真实预览并能升级旧绑定，手工对象不被覆盖。
2. 后端先补红灯用例：应用级重同步只处理来源应用、表单标识一致的 `PAGE_FORM` 对象；发布检查和最终发布源码契约均在 readiness 前调用重同步。
3. 实现后执行相关 Vitest 和 JUnit；测试源码变化后先执行生成器 reactor `test-compile`，再执行 Surefire 并核对测试数非 0。
4. 使用 Node `v20.19.0` 执行目标 JS/Vue ESLint和生产构建，最后执行 `git diff --check`。
5. 不启动 MySQL、Flyway、后端、Vite 或浏览器，不实际执行 DDL；真实查询、新增与发布由用户重启后验收。

### 17.1 执行结果

- Node `v20.19.0`：目标 ESLint 0 error、0 warning；3 个 Vitest 文件共 15 个用例全部通过，覆盖表单字段合并、查询字段独立配置、显式空查询、旧区块兼容、托管预览初始化与遗留绑定修复。
- JDK 17：生成器 reactor 29 个模块 `test-compile` 成功并编译 93 个测试源；`BusinessApplicationDraftPreviewContractTest`、`BusinessApplicationFormDataServiceTest`、`BusinessObjectDatabaseSyncServiceTest`、`BusinessApplicationPhaseFiveControllerTest`、`BusinessApplicationControllerTest` 共 32 个 JUnit，0 failure、0 error、0 skipped。
- Vite 生产构建成功：8792 个模块，`built in 4m 39s`；仅保留仓库既有的 `UserSelectModal` 重名、动态/静态导入和 CSS `//` 注释警告。
- `git diff --check` 无输出。未启动 MySQL、Flyway、后端、Vite 或浏览器，未实际执行 DDL 或发布；真实安全差异自动同步和高风险差异提示由用户重启环境后验收。

## 18. Task 27 预览循环与发布性能增量计划

1. 前端纯函数测试覆盖：预览成功/失败状态和文案变化不改变请求签名；真实预览开关、模式、记录或列表 API 变化会改变签名。
2. 静态审查 `GridBlockRenderer` 的监听源，确认不再用返回新数组的单 getter 监听区块对象；检查发布抽屉入口不再自动调用 `runPublishCheck()`。
3. 后端 JUnit 在一个应用中同时放入 `IN_SYNC`、待同步、手工和其它应用托管对象，断言只有待同步的当前应用托管对象调用额外表映射同步。
4. 测试源码变化后先执行生成器 reactor `test-compile`，再跑定向 Surefire 并核对测试数；前端使用 Node `v20.19.0` 执行目标 ESLint、Vitest 和生产构建，最后执行 `git diff --check`。
5. 不启动 MySQL、Flyway、后端、Vite 或浏览器，不执行真实 DDL/发布；真实 Network 请求次数和生产数据库耗时由用户重启环境后验收。

### 18.1 执行结果

- 前端稳定请求身份测试通过：预览结果状态/文案变化不改变 reload key，真实预览开关、记录和列表 API 变化会改变 key；3 个定向 Vitest 文件共 17 个用例全部通过。
- `GridBlockRenderer.vue`、共享请求参数和应用运行页目标 ESLint 0 error、0 warning；静态审查确认发布抽屉入口不再隐式调用 `runPublishCheck()`。
- 后端生成器 reactor 29 个模块 `test-compile` 成功并编译 93 个测试源；`BusinessApplicationDraftPreviewContractTest`、`BusinessApplicationFormDataServiceTest`、`BusinessObjectDatabaseSyncServiceTest` 共 23 个 JUnit，0 failure、0 error、0 skipped。
- Vite 生产构建成功：8792 个模块，`built in 3m 29s`；只有仓库既有组件重名、动态/静态导入和 CSS `//` 注释警告。`git diff --check` 无输出。
- 未启动 MySQL、Flyway、后端、Vite 或浏览器，未执行真实 DDL/发布；一次真实预览的 Network 次数和真实发布耗时由用户重启环境后验收。

## 19. Task 28 动态页面查询条件增量计划

1. 前端纯函数测试覆盖：`searchFieldSettings` 的查询方式、查询组件和映射字段进入运行 Schema；映射目标不存在时安全回退；查询方式元数据只包含受支持操作符。
2. 后端 Controller 测试覆盖：GET 平铺业务字段继续装入 `searchParams`；查询方式控制参数单独解析且不混入业务字段；损坏元数据安全忽略。Service 测试覆盖原查询 Schema 之外、但已在列表或编辑 Schema 公开的字段进入查询白名单，以及非法查询方式不能覆盖默认协议。
3. 实现后使用 Node `v20.19.0` 执行目标 Vitest 和 ESLint；测试源码变化后使用 JDK 17 执行生成器 reactor `test-compile` 和定向 Surefire，核对测试数非 0。
4. 执行前端生产构建和 `git diff --check`，将实际命令、测试数、构建结果和警告追加到 `execution-log.md`。
5. 不启动 MySQL、Flyway、后端、Vite 或浏览器，不执行真实查询或 DDL；真实 Network 参数和数据库结果由用户重启当前环境后验收。

### 19.1 执行结果

- 前端红灯用例先证明页面查询设置未进入运行 Schema、查询方式请求构造器缺失；实现后目标 ESLint 0 error、0 warning，`runtime-crud-props`、页面 Schema 与表单数据准备 3 个 Vitest 文件共 19 个用例全部通过。
- 后端生成器 reactor 29 个模块 `test-compile` 成功并编译 94 个测试源；`DynamicCrudControllerTest`、`DynamicCrudServiceAutoGenerationTest` 共 6 个 JUnit，0 failure、0 error、0 skipped。Controller 覆盖控制参数隔离和损坏 JSON 回退，Service 覆盖公开字段白名单及非法操作符拒绝。
- Vite 生产构建成功：8792 个模块，`built in 2m 19s`；仅保留仓库既有的组件重名、动态/静态导入和 CSS `//` 注释警告。`git diff --check` 无输出。
- 未启动 MySQL、Flyway、后端、Vite 或浏览器，未执行真实查询、DDL 或发布；真实请求中的业务字段、`_searchTypes` 及数据库筛选结果由用户重启当前环境后验收。

## 20. 2026-08-01 应用发布数据库同步误判增量验证

### 20.1 自动化范围

- 对象草稿版本因页面或表单变化递增时，不再直接伪造 `OUT_OF_SYNC`，对象摘要降为 `UNKNOWN` 等待实时检查。
- 未在模型字段中重复声明的 Forge 标准系统列继续展示，但不计入未同步业务变更；自定义额外列、缺失业务列和类型不一致继续阻断。
- 应用发布门禁必须调用实时表映射检查覆盖旧摘要，并在阻断消息中列出具体列、设计/数据库类型或待执行 DDL 数量。

### 20.2 执行结果

- 生成器 reactor `test-compile`：30 个模块 `BUILD SUCCESS`，生成器编译 95 个测试源。
- 定向 JUnit：`BusinessApplicationObjectServiceTest`、`BusinessObjectTableMappingServiceTest`、`BusinessApplicationReadinessServiceTest`、`BusinessObjectDatabaseSyncServiceTest`、`BusinessApplicationFormDataServiceTest` 共 33 个用例，0 failure、0 error、0 skipped。
- Admin 聚合 `package -DskipTests`：47 个模块全部 `BUILD SUCCESS`，包含当前未提交的 Capability 插件装配。
- 首次带 `-am` 定向 `test` 在上游 `forge-starter-datascope` 因缺少对应 JUnit engine 失败，生成器未执行；随后在已完成 reactor `test-compile` 的生成器模块直接运行定向测试并通过，未将首次失败记为测试通过。
- `git diff --check` 无输出。未启动或重启 MySQL、Flyway、Admin、Flow、Vite 或浏览器，未执行真实 DDL/发布；用户现有服务保持不变，真实“离职申请”发布由用户重启 Admin 后验收。

## 21. 2026-08-01 数据库保留列与逻辑删除类型兼容增量验证

### 21.1 自动化范围

- `del_flag` 为系统逻辑删除字段时，设计 `char(1)` 与数据库 `tinyint/int/bigint/varchar` 的 `0/1` 存储语义兼容，不制造发布阻断。
- 未映射数据库列继续显示；可空、有默认值、`auto_increment` 或生成列不阻断，`NOT NULL` 且无默认值、不能自动生成的列继续阻断。
- 发布提示只列出真正阻断的差异，并给出“添加字段映射或调整数据库默认值”的处理建议。
- “按数据库校准字段”只针对已有映射的非系统字段，不能因系统字段类型差异显示无效按钮。

### 21.2 执行结果

- 先补测试并得到预期编译红灯：`BusinessObjectTableFieldMappingVO` 尚无 `blockingDifference` 访问器；首次实现后旧用例仍要求可空额外列阻断，得到 1 个预期行为冲突并更新旧断言。
- 生成器模块 `test-compile` 成功，编译 558 个主源码和 95 个测试源码；定向执行发布链路 5 个测试类共 37 个 JUnit，0 failure、0 error、0 skipped。
- 前端目标组件 ESLint 最终 0 error、0 warning；Admin 聚合 `package -DskipTests` 47 个模块全部 `BUILD SUCCESS`。
- Node `v20.19.0` 前端生产构建退出码 0，Vite 转换 8818 个模块，`built in 5m 39s`；仅保留仓库既有组件重名、动态/静态导入和 CSS 注释警告。
- 未启动或重启 MySQL、Flyway、Admin、Flow、Vite 或浏览器，未执行真实 DDL/发布；真实列可空性和最终发布结果由用户重启 Admin 后验收。

## 22. Task 29 应用创建实测回归增量计划

1. 前端 Vitest 覆盖输入、人员、组织、区划字段名称变化后的默认占位，字段资产面板同步入口以及手工占位保护；覆盖人员和组织错误展示字段与主字段同名时不回写主值。
2. CRUD 预览协议测试覆盖区块 `api` 与完整 `apiConfig` 均追加且不重复追加 `designPreview=1`；后端权限测试覆盖仅具备 `ai:businessApplication:edit` 的用户可访问设计草稿，正式运行规则不变。
3. 后端 JUnit 覆盖设计版本变化后最近一次真实 `IN_SYNC` 摘要仍保留；发布实时检查相关既有用例继续通过。
4. 运行配置测试分别覆盖人员、组织字段的 `bigint + userSelect/orgTreeSelect`，错误的同字段 `labelValueField/targetField` 回退到 `${field}Name`；动态 CRUD 测试覆盖姓名等非数字主值被明确拒绝、数字 ID 可通过。
5. 菜单发布测试覆盖无显式 `systemMenuVisible=true` 时只同步空列表、不创建应用根菜单；显式开启页面仍生成菜单。应用协调对象发布显式关闭旧低代码菜单同步，且不会解析/创建通用业务域目录；旧低代码直接发布默认行为保持兼容。
6. 使用 Node `v20.19.0` 执行目标 ESLint、Vitest 和生产构建；使用 JDK 17 执行生成器 reactor `test-compile`、非零定向 Surefire 和 Admin 聚合 `package -DskipTests`，最后执行 `git diff --check`。
7. 不启动 MySQL、Flyway、Admin、Flow、Vite 或浏览器，不执行真实 DDL、发布或菜单写入；真实页面交互与数据库保存结果由用户重启环境后验收。

### 执行结果（2026-08-06）

- Node `v20.19.0`：目标 ESLint 0 error/0 warning；Vitest 5 个文件 33 tests 全通过；生产构建转换 8864 modules，`built in 2m 44s`，退出码 0。仅保留仓库既有组件重名、动态/静态导入和 CSS `//` 注释警告。
- JDK 17：生成器模块主代码和 105 个测试源 `test-compile` 通过；第一组定向 Surefire 16 tests、第二组应用发布链路 10 tests 全部通过，均为 0 failure、0 error、0 skipped；Admin 聚合 `mvn -pl forge-admin-server -am package -DskipTests` 共 47 个模块 `BUILD SUCCESS`。
- 基线说明：首次带 `-am` 的 reactor `test-compile` 在无关的 `forge-plugin-message` 既有测试中断，原因是 `MessageServiceImplTest` 构造器缺少 `ApplicationEventPublisher` 参数；随后直接在生成器模块完成测试编译。首次整类执行 `LowcodeRuntimeConfigBuilderTest` 时，本轮新增用例通过，但既有 `doesNotPublishOneToManyChildRelationAsRelationNameTranslation` 因当前“低代码业务表必须使用 id 自增主键”规则失败；随后方法级执行本轮新增 `selectionLabelsNeverTargetPrimaryIdentifierFields` 通过。上述基线问题未计入本轮通过项。
- `git diff --check` 无输出。未启动或重启 MySQL、Flyway、Admin、Flow、Vite 或浏览器，未执行 DDL、真实发布或菜单写入，无服务 PID 需要清理；真实页面点击、数据库保存和菜单结果留待用户重启环境后验收。
