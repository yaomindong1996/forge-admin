# 执行日志

## 2026-08-20 基线

- 工作树基线：仅存在用户已有 `.DS_Store` 变动，本变更不触碰。
- 前端：`http://localhost:3001` 已有 Vite 服务。
- 后端：`http://localhost:8580` 当前不可用，Vite `/dev-api` 返回 502。
- 验证策略：先执行单元测试、构建和 mock 浏览器回归；真实 Flowable/数据库 E2E 作为环境跳过项明确记录。

## 2026-08-20 实现与收尾验证

- 变更范围：应用增强可视化配置与运行时、页面组父子页创建、业务流程节点配置、表单资产、独立流程状态字段、安全增量 DDL 与流程发布校验。
- 修复测试编译阻断：将 `LowcodeDdlAdditiveColumnTest` 中重载 `resolve` 的 Mockito 匹配器限定为 `any(LowcodeModelSchema.class)`。
- 前端定向测试：使用 Node `v20.19.0` 直接执行 Vitest，12 个测试文件、121 个测试全部通过；覆盖流程 API/工作台、增强作用域 CSS/沙箱/正式运行时、应用运行态、嵌套 CRUD 数据源、页面动作以及页面组保存重载。
- 前端测试非阻断警告：预期的 `WARN` 增强失败隔离日志；测试桩未注册 `n-icon/n-modal` 的 Vue warning；Dart Sass legacy API 提示。
- 后端敏感逻辑首轮：`LowcodeDdlAdditiveColumnTest,BusinessProcessSchemaValidatorTest,BusinessProcessOrchestratorTest`，20 个测试全部通过。
- 后端扩大定向测试：`BusinessApplicationRuntimeServiceTest,BusinessExtensionExecutionServiceTest,BusinessExtensionValidationServiceTest,BusinessFlowStatusFieldServiceTest,BusinessProcessSchemaValidatorTest,BusinessProcessOrchestratorTest,LowcodeDdlAdditiveColumnTest`，49 个测试全部通过。
- 前端生产构建：`NODE_OPTIONS=--max-old-space-size=8192 node ./node_modules/vite/bin/vite.js build`，成功，耗时约 49.45 秒。
- 前端构建非阻断警告：Vite native config loader 未来兼容提示、仓库既有 CSS `//` 注释、无效动态导入拆包提示、构建插件耗时和大 chunk 提示。
- 后端编译：JDK 17 执行 `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`，32 个 Reactor 模块全部成功，耗时约 27.44 秒；仅有既有 deprecated/unchecked/Lombok Builder 提示。
- Flyway 静态检查：`V1.0.126__add_business_flow_status_dict.sql` 版本唯一；无 `tenant_id = 0`；无 `${...}`；字典类型和五条字典数据均显式列名并使用 `NOT EXISTS`，租户固定为 `1`。
- 工作树检查：`git diff --check` 通过；用户已有根目录 `.DS_Store` 修改和 `forge/.DS_Store` 删除未触碰。
- 可达性：目标前端 URL 返回 HTTP 200；`localhost:8580` 连接失败，HTTP 000。
- 浏览器限制：Python/Node Playwright 在当前环境不可用；已有 Headless Chrome 只能到登录页，并因后端离线显示预期 502，不能进入鉴权后的设计器执行交互验收。
- 跳过项：未执行真实登录、MySQL DDL/Flyway、对象发布、表单资产落库、Flowable 发起/通过/驳回/取消及回调 E2E；原因是后端 `8580` 离线。以上项目不得视为已通过。
- 服务清理：本轮未启动新服务，无 PID 需要停止；保留用户原有 `localhost:3001` 前端服务。

### 人工审查敏感项

- 自动 DDL 只允许补一个指定列；表不存在、数据源禁用 DDL、已有列类型/容量/非空/默认值/生成列不兼容时直接停止，不删除、重命名或修改已有列。
- 低代码审批发布时必须绑定 `flowStatus / flow_status`；通用业务 `status` 在前端校验、后端 schema 校验和编排执行三层均被拒绝。
- `flowStatus` 创建为只读字典字段，数据库默认 `DRAFT`；运行态普通写入过滤只读/系统字段，流程服务内部路径负责状态回写。
- 服务端增强只从当前应用不可变发布快照解析已启用版本，并按处理器声明的输入字段投影浏览器数据；未声明字段不进入处理器上下文。

## 2026-08-21 作用域 CSS 模板编译修复

- 根因：`PortalPageRenderer.vue` 在客户端组件模板中直接渲染 `<style v-for>`，Vue SFC 编译器将 `style/script` 视为副作用标签并忽略。
- 修复：新增渲染函数组件 `RuntimeScopedStyles.js`，把已治理、已按应用/页面重写作用域的 CSS Teleport 到 `document.head`；页面切换、属性变化和组件卸载由 Vue 自动移除旧节点。
- 失败基线：新增测试首次运行因 `RuntimeScopedStyles` 尚不存在而失败，确认用例能够捕获缺失实现。
- 定向测试：Node `v20.19.0` 执行 `RuntimeScopedStyles.spec.js`、`application-extension-runtime.spec.js`、`application-runtime-load.spec.js`，3 个测试文件、11 项测试全部通过。
- 生产构建：`NODE_OPTIONS=--max-old-space-size=8192 node ./node_modules/vite/bin/vite.js build` 成功，耗时约 48.73 秒；原 Vue 副作用标签告警已消失。
- 非阻断警告：仍有仓库既有 Vite native config loader、CSS `//` 注释、动态导入拆包和构建性能提示，与本次修复无关。
- 页面可达性：本轮复核时 `localhost:3001` 已未运行（HTTP 000），因此未执行浏览器/HMR 点击回归；生产构建和 jsdom 生命周期测试作为替代验证。
- 服务清理：本轮未启动新服务，无 PID 需要停止。

## 2026-08-21 流程节点配置布局回归修复

- 根因：节点配置从底部区域调整为右侧浮层后仍固定为 560px；审批配置又使用浏览器视口 `@media` 判断双列，导致 560px 面板内仍强制并排主表单和流程预览。
- 修复：面板扩为最大 720px，并把审批、条件、字段赋值和开始条件改为基于 `node-config` 容器宽度的自适应布局；内容区保持独立滚动，底部操作区保持固定可见。
- 定向测试：Node `v20.19.0` 执行 `node ./node_modules/vitest/vitest.mjs run src/components/business-process-designer/__tests__/business-process-designer.spec.js src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js`，2 个测试文件、41 项测试全部通过。
- ESLint：`BusinessProcessNodeConfigDrawer.vue` 定向检查通过；对四个相关组件一起检查时，`ActionAndApprovalNodeConfig.vue` 仍报告本轮前已存在的 import 排序、Naive 组件大小写、正则和 `no-use-before-define` 规则问题，未因本轮 CSS 布局改动新增。
- 生产构建：`NODE_OPTIONS=--max-old-space-size=8192 node ./node_modules/vite/bin/vite.js build` 成功，最终一次耗时约 50.55 秒。
- 非阻断警告：仍为仓库既有 Vite native config、CSS `//` 注释、动态导入拆包和大 chunk/构建耗时提示。
- 页面可达性：`localhost:3001` 与 `localhost:5173` 均为 HTTP 000，未执行真实浏览器几何尺寸和点击回归；定向组件测试与生产构建作为替代验证。
- 工作树检查：`git diff --check` 通过。
- 服务清理：本轮未启动新服务，无 PID 需要停止。

## 2026-08-21 表单绑定、字段目录和流程检查回归修复

- 根因：`getFormAssets` 先收集到同一 `formKey` 的设计态空字段资产时，去重逻辑直接丢弃后续运行态字段目录；当前真实流程还保存了 `business_object` 伪表单引用，而接口返回空资产，导致流程检查和任务表单均不可用。
- 后端修复：重复资产按身份合并，字段目录、字段数、字段预览、配置键和保存能力从更完整资产补齐；无表单设计/运行配置时从当前业务对象字段注册表生成受治理对象表单资产；表单校验消息包含具体 `formKey`。
- 前端修复：空目录不再合成 `objectCode` 表单；新建/插入审批节点仅绑定服务端真实资产；流程问题卡片拆分定位和复制操作，复制内容包含消息、节点、错误编码、配置路径和处理建议，并隐藏重复的依赖级表单错误。
- 真实浏览器检查：使用 Playwright 启动 Vite 3001、登录后进入目标流程，确认 API 返回的真实对象为 `business_object`，流程节点引用 `business_object`，原接口 `formAssets=[]` 且提示“业务对象尚未配置低代码表单资产”；页面无控制台错误但显示两条原始通用错误。后端随后离线，修复后真实接口复验跳过。
- 前端定向测试：`business-process-designer.spec.js` 与 `business-process-designer-workbench.spec.js` 共 44 项通过。
- 前端生产构建：Node `v20.19.0` 执行 `NODE_OPTIONS=--max-old-space-size=8192 node ./node_modules/vite/bin/vite.js build` 成功（最终一次约 50.80 秒）；保留既有 Vite native config、CSS 注释、动态导入和大 chunk 警告。
- 后端定向测试：JDK 17 执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessFlowServiceFormAssetMergeTest,BusinessProcessSchemaValidatorTest test`，19 项通过。
- 后端首次测试因默认 JDK 22 报“无效的目标发行版: 17”，切换项目要求的 Homebrew JDK 17 后通过；不属于代码失败。
- `git diff --check` 通过；本轮 Playwright 启动的 Vite 进程由 `with_server.py` 自动停止，无遗留 PID。

## 2026-08-21 旧流程页面表单绑定补齐

- 根因确认：应用 `presale_registration_apply`（ID `2089968247981060098`）的实际页面为“测试”（`page_page`），页面块绑定 `form_form`；但业务流程 `2090384244139360257` 的审批节点 `approval_mt2533e6_1` 的 `config.formAsset` 仍为空，`dependencies.formAssets` 也为空，因此待办无法解析页面字段。
- 后端修复：`BusinessProcessService` 在设计器读取时展示唯一可确定的页面 `formAsset`（包括 `applicationId/pageId/pageName/formKey`），并在保存草稿/流程检查时规范化持久化；普通 GET 不隐式写库，多表单场景不猜测，仍由设计器选择。
- 具体绑定结果：页面 `测试` / `page_page`，表单资产 `form_form`，运行表单 Key `app_2089968247981060098_page_page_page_form_form_form`，字段 `fieldSlider、fieldRate、fieldNumber、fieldInput`。
- 新增后端回归测试：旧审批节点空表单时自动绑定唯一应用页面表单，断言节点引用和依赖清单；`BusinessProcessServiceTest` 10 tests、0 failures。
- 后端验证：JDK 17 执行 `mvn -o -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessServiceTest,BusinessProcessPublishServiceTest,BusinessFlowServiceFormAssetMergeTest -Dmaven.test.skip=false test`，25 tests、0 failures；保存草稿和流程检查也会执行同一兼容补齐；此前 Generator reactor compile 32/32 模块通过。
- 未执行项：本轮未重启 Admin/Flow 服务，也未直接修改远程数据库；需要用户部署新后端后重新打开该流程页面，前端会自动保存补齐的节点，随后执行“流程检查 → 保存/发布”。若不经过设计器，调用保存/检查接口也会执行服务端兼容补齐。旧待办和旧 Flowable 流程实例不会自动切换到新页面表单。

## 2026-08-21 流程行锁 SQL 语法修复

- 根因：`sys_flow_task` 的 `selectByTaskIdForUpdate` 原始 SQL 使用 `LIMIT 1 FOR UPDATE`；多租户 JSqlParser 重写后变成 MySQL 不支持的 `FOR UPDATE LIMIT 1`，异常栈因此出现在 `IgnoreTenantAspect` 附近。
- 修复：任务、流程业务、错误日志、填报明细及高风险审批的带锁主键/流程引擎标识查询均移除冗余 `LIMIT 1`，保留 `... WHERE 定位条件 FOR UPDATE`。这些查询按主键或 Flowable 全局标识定位业务行，也避免租户拦截器重新排序锁定子句。
- 回归测试：`mvn -o -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-flow -Dtest=FlowLockMapperSqlContractTest -Dmaven.test.skip=false test`，1 项通过。
- 编译验证：`mvn -o -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-capability-parent/forge-plugin-capability-high-risk-approval -DskipTests compile`，成功。
- 静态扫描：仓库中已无实际 `FOR UPDATE ... LIMIT` 或 `LIMIT ... FOR UPDATE` 锁查询；仅保留说明性注释。未启动服务、未修改数据库，需部署/重启 Flow 或 Admin 服务后重试任务操作。

## 2026-08-21 流程按钮运行态、表单位置与多字段条件修复

- 根因一：列表只消费静态流程动作，未逐记录读取业务流程运行态，所以流程启动后按钮仍保留；旧判断又可能根据单据主流程状态隐藏全部应用级流程按钮。
- 根因二：应用工作台/门户调用 CRUD 渲染接口时未传当前应用入口 ID；对象被应用复用时，流程动作缺少明确的应用页面上下文。
- 根因三：编译器把 `DETAIL/FORM` 合并为详情动作，运行组件又把全部 `runtimeActions` 合回列表，导致“列表、表单都选中”不能稳定投影到两个位置。
- 修复：批量读取记录活动流程编码；应用流程按 `processCode` 精确隐藏；单据主流程继续使用独立 `flowStatus`；`ROW/DETAIL/FORM` 分别进入列表、详情、编辑表单；工作台和门户请求携带入口 `appId`。
- 显示条件：开始节点支持多规则新增/删除和 AND/OR；运行态支持结构化规则及编译字符串的数值比较，避免 `>=` 被旧等号解析器误判。
- 前端定向测试：Node `v20.19.0` 执行 4 个测试文件，47 项全部通过：`business-action-runtime.spec.js`、`business-process-designer-workbench.spec.js`、`runtime-crud-props.spec.js`、`page-design-tabs.spec.js`。
- 后端定向测试：JDK 17 执行 `BusinessProcessRuntimeActionCompilerTest,BusinessApplicationRuntimeConfigOverlayServiceTest`，最终 11 项全部通过；首次使用环境默认 JDK 8 报“无效的目标发行版: 17”，显式切换 Homebrew JDK 17 后成功。
- 前端生产构建：`NODE_OPTIONS=--max-old-space-size=8192 npm run build` 成功，最终一次约 34.78 秒；仅保留既有 Vite native loader、CSS `//` 注释、动态导入和构建性能提示。
- 工作树检查：`git diff --check` 通过。
- 跳过项：本轮未连接真实 MySQL/Redis/Flowable，未执行真实记录“启动流程后按钮隐藏”的浏览器 E2E；部署并重启后端后需对目标应用复验列表、详情和编辑表单三个位置。
- 浏览器只读复核：本机前后端已可访问，Playwright 使用默认账号进入目标应用；列表 3 条记录中前两条“更多”含“审批”，第三条已无审批动作，页面无控制台错误、失败请求或 HTTP 4xx/5xx。该结果证明当前运行态已按记录区分按钮，但本轮没有再次发起/变更真实流程实例，因此不把完整 Flowable 状态流转视为通过。
- 进一步取证：目标应用 `entries=[]`，页面直接绑定应用对象；已将 CRUD 请求修正为携带 `applicationId=2089968247981060098`。当前发布流程版本为 6，开始节点实际保存 `positions=[ROW, DETAIL]` 和 `fieldRate = 1`，没有 `FORM`；运行中的旧后端响应仍只有 `rowActions`，批量运行态也尚无新字段 `activeProcessCodes`，说明必须重启 Admin 服务后才能加载本轮后端类并看到详情动作、精确隐藏逻辑。若还需要“编辑表单”而非只读详情，需在开始节点新增勾选“编辑表单操作”并重新发布。
- 服务清理：本轮未启动长期运行服务，无 PID 需要停止。

## 2026-08-21 按钮显示条件删除修复

- 根因：开始节点组件删除最后一条规则后正确发出了不含 `visibleCondition` 的完整配置，但 `useBusinessProcessDesigner.updateNode` 始终将新旧 `config` 浅合并，旧 `visibleCondition` 随即被合并回来，表现为条件无法删除。
- 修复：节点抽屉保存完整节点配置时显式采用 `replaceConfig`；自动绑定表单、分支等局部更新继续保持合并行为。逐条删除、最后一条删除和顶部“清除”均可用，删除按钮使用明确的危险色和可访问名称。
- 空态：清空条件后显示“未设置条件时，按钮始终显示”，并保留“添加显示条件”入口，避免删除后无法重新配置。
- 定向测试：Node `v20.19.0` 执行 `business-process-designer.spec.js` 与 `business-process-designer-workbench.spec.js`，2 个测试文件、50 项全部通过；覆盖删中间条、删最后一条、清除全部、清空后重建以及抽屉保存后不回填旧条件。
- 前端生产构建：`NODE_OPTIONS=--max-old-space-size=8192 npm run build` 成功，耗时约 62 秒；仅有仓库既有 Vite native loader、CSS `//` 注释、动态导入和构建性能提示。
- 工作树检查：`git diff --check` 通过。本轮未启动或停止用户服务。

## 2026-08-21 审批后置动作与运行态收尾复核

- 复核并收紧审批后置动作：`UPDATE_RECORD` 调用发布态模型字段校验的 `updateFieldsInternal`，固定值 `fieldMappings` 按当前业务记录 ID 写入；动作失败会使外层运行失败，后续重复终态回调不会重复执行已认领的动作节点。
- 后端定向测试（JDK 17）：`mvn -o -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-generator -Dtest=BusinessProcessActionExecutorTest,BusinessProcessOrchestratorTest,BusinessDocumentRuntimeServiceTest -Dmaven.test.skip=false test`，7 tests、0 failures、BUILD SUCCESS。
- 前端定向测试（Node v20.19.0）：`business-action-runtime.spec.js`、`runtime-crud-props.spec.js`，27 tests、0 failures；相关 ESLint 定向检查通过。
- 解析器安全收紧后再次执行前端生产构建，结果为 BUILD SUCCESS；保留仓库既有 Vite loader、CSS 注释、动态导入和大 chunk 性能警告。
- `git diff --check` 通过。未启动新服务，未修改数据库或 Flowable 运行态。
- 未执行：真实审批通过、数据库字段回写、应用工作台发布快照刷新和流程时间轴接口 E2E；部署新后端并重新发布对象/应用后由验收清单执行。

## 2026-08-21 可视化增强空字段与抽屉关闭回归修复

- 根因：历史可视化规则可能引用已删除字段，字段目录查找返回 `null`；`extensionFieldValueKind` 的默认参数只覆盖 `undefined`，模板继续读取 `fieldType` 时触发运行时异常。抽屉关闭期间未完成的字段/动作目录请求也可能继续回写过渡中的组件状态。
- 修复：字段类型解析对 `null/undefined` 安全回退为文本；加载历史条件和动作时过滤空/非对象行并补齐默认结构；抽屉关闭和离场时递增请求序列，使过期目录请求失效并停止加载状态。
- 前端定向测试：Node `v20.19.0` 执行 `node ./node_modules/vitest/vitest.mjs run src/views/app-center/application-workspace/__tests__/extension-visual-rule.spec.js`，7 项通过，覆盖空字段类型和空历史条件行。
- ESLint：对 `extension-visual-rule.js`、`ExtensionEditorDrawer.vue` 和对应测试文件定向检查通过。
- 生产构建：Node `v20.19.0` 执行 `NODE_OPTIONS=--max-old-space-size=8192 node ./node_modules/vite/bin/vite.js build` 成功（约 61 秒）。保留仓库既有 Vite native config、CSS `//` 注释、动态导入和构建性能提示。
- `git diff --check` 通过；本轮未启动后端或浏览器服务。Playwright 只读复核确认 `localhost:3001` 可达且首次登录后页面无控制台错误，但未能稳定进入增强面板完成抽屉点击（后续登录页进入验证码态），因此不将浏览器 E2E 记为通过；需部署后在增强编辑器中确认“选择字段 → 关闭/重新打开”路径。

## 2026-08-21 表单提交增强运行闭环修复

- 根因一：应用工作台增强查询只返回名称、状态和版本号等摘要，没有返回已启用版本的规则正文，前端虽然选中了增强，实际执行内容为空。
- 根因二：页面中的独立 `AiForm` 直接发送保存请求，没有接入 `BEFORE_SUBMIT / AFTER_SUBMIT / FORM_CHANGE`；`AiCrudPage` 的异步提交前钩子异常又被吞掉，`BLOCK` 策略无法阻断保存。
- 后端修复：`selectWorkspaceSummaries` 按 `enabled_version` 关联不可变版本，返回 `content / processedContent`，不读取 `draft_version`。
- 前端修复：独立表单在请求前后执行增强钩子并使用增强后的记录；提交前返回 `false` 或抛错时停止请求；嵌套表单透传钩子。编辑器测试通过后新增“启用当前版本”，并提示工作台刷新、正式应用重新发布的生效边界。
- 前端定向测试：Node `v20.19.0` 执行 4 个测试文件，40 项全部通过；覆盖 `fieldRate=2`、`EQ`、提交前显示消息 `123`，以及独立表单前置修改、后置执行和失败阻断。
- 后端定向测试：JDK 17 离线执行 `BusinessExtensionMapperTest,BusinessExtensionExecutionServiceTest,BusinessApplicationWorkspaceServiceTest`，12 项全部通过，`BUILD SUCCESS`。
- 既有收尾验证：相关前端文件 ESLint 通过；Generator 编译成功；前端生产构建成功（约 55.65 秒）。保留 Vite native config、CSS `//` 注释、动态导入拆包和 UnoCSS 构建耗时等非阻断警告。
- 工作树检查：`git diff --check` 通过；未覆盖或清理工作区既有改动。
- 未执行：真实浏览器提交 E2E。当前运行中的 Admin 若未重启仍是旧 Mapper，必须重启后端，再按“保存并测试 → 启用当前版本 → 刷新工作台 → 评分设为 2 → 提交”验收；正式应用需重新发布。

## 2026-08-21 指定用户审批人协议修复

- 根因：两个流程设计器把指定用户编码为 `flowable:assignee="${user_45}"`，Flowable 将其当作流程变量表达式；启动时未提供 `user_45`，因此抛出 `Unknown property used in expression`。
- 前端修复：新增 `assigneeUserId`；指定人员选择器、Ding 流程设计器和兼容 BPMN 属性面板均按字符串用户 ID 保存；旧 `${user_<数字>}` 仅在加载/写回时迁移，动态表达式和 SPEL 不受影响。
- 后端兼容：启动已部署流程前读取 BPMN，递归识别严格的 `${user_<数字>}` 固定节点，按 BPMN 中的固定 ID 注入同名变量；不处理 `${initiator}`、`${deptManager}` 或方法调用表达式。
- 前端定向测试：Node `v20.19.0` 执行 6 个流程设计器测试文件，63 项全部通过；新增选人组件用例覆盖 `2090384244139360257` 字符串精度和表达式清空。
- 前端 ESLint：指定用户相关源码与测试文件通过。
- 后端定向测试：JDK 17 执行 `LegacyFixedAssigneeVariableSupportTest`，1 项通过；Flow 插件编译成功。
- 前端生产构建：Node `v20.19.0` 执行 Vite build 成功（约 61.7 秒）；保留仓库既有 CSS 注释、Vite native loader、动态导入和构建性能警告。
- `git diff --check` 与真实 Flowable 发起 E2E 尚待最终执行；未启动服务、未修改数据库或清理用户运行态数据。
