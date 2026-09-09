# 增量测试说明

> baseline: 复用 `lowcode-app-designer-ia` 与 `application-business-process-orchestrator` 的既有验证产物
> environment: 前端 `localhost:3001` 可访问；后端 `localhost:8580` 当前不可用

## 1. 应用增强

- 单一对象自动选中，字段目录自动加载。
- 唯一主对象自动选中；多对象无主对象时要求显式选择。
- 条件字段、设置字段可按名称/编码搜索，保存值仍为字段编码。
- 只读/系统/禁用字段不出现在设置字段目标中。
- 字典、布尔、数字、日期字段显示对应值编辑器。
- 历史失效字段可见且有警告；修复前不能发布错误规则。
- VISUAL_RULE / CLIENT_JS / SCOPED_CSS 仅在启用且已发布时运行，异常隔离且不阻断页面。

## 2. 页面组

- 创建组后立即创建表单页、列表页、自定义页均成功。
- 子页面 `parentId` 等于新组 ID，不出现悬空节点。
- 保存 schema 后 normalize/reload，页面组和子页面仍存在。
- 选择器打开期间父组被删除时给出明确错误。

## 3. 业务流程与状态

- 768/900px 视口高度下弹层顶部、内容和底部操作均可访问。
- 表单资产以 `formKey/formName` 显示和回填；已发布 CRUD 表单可选。
- 当前对象无目录资产时安全回退可用，不伪造其他对象表单。
- 无 `flowStatus` 时创建受管理字段并执行一次安全加列；重复执行无副作用。
- 存在兼容字段时复用；字段冲突时不执行 DDL 并提示。
- 新建、发起、通过、驳回、取消分别得到五种状态。
- 业务 `status` 字段在全部流程事件中保持原值。

## 4. 自动化命令

根据改动文件选择并记录：

```bash
cd forge-admin-ui
pnpm exec vitest run <相关 spec>
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

```bash
cd forge
mvn -pl <相关模块> -am test
mvn -pl <相关模块> -am compile -DskipTests
```

## 5. 浏览器验证与跳过规则

- 优先使用已有 mock/visual smoke 基线检查布局和核心交互。
- 后端不可用时不得声称真实保存、Flowable 发布或数据库 DDL E2E 通过。
- 跳过真实 E2E 时记录原因、已执行替代验证和建议由用户执行的最小清单。

## 6. 2026-08-20 收尾增量验证

本轮在既有基线上新增并复跑以下风险面：

- 作用域 CSS 页面改为页面树名称选择，并兼容显示历史失效页面编码。
- 低代码审批发布前强制选择 `flowStatus / flow_status`，通用业务 `status` 不允许作为流程状态。
- 已存在 `flow_status` 数据库列时校验类型、容量、非空、默认值和生成列语义，不兼容时停止。
- 扩大前端定向测试到流程 API/工作台、增强 CSS/沙箱/运行时、应用运行态、嵌套区块和页面组。

实际命令使用 Node 20.19.0 的直接 CLI，避免当前环境 `pnpm exec` 异常：

```bash
node ./node_modules/vitest/vitest.mjs run \
  src/api/__tests__/business-process-api.spec.js \
  src/components/business-process-designer/__tests__/business-process-designer.spec.js \
  src/components/business-process-designer/__tests__/business-process-designer-workbench.spec.js \
  src/components/lowcode-extension/css/__tests__/scoped-css.spec.js \
  src/components/lowcode-extension/js/__tests__/extension-sandbox.spec.js \
  src/components/lowcode-extension/runtime/__tests__/application-extension-runtime.spec.js \
  src/components/lowcode-builder/page/__tests__/grid-block-renderer-data-source.spec.js \
  src/views/app-center/application-workspace/__tests__/extension-visual-rule.spec.js \
  src/views/app-center/__tests__/application-runtime-load.spec.js \
  src/views/app-center/__tests__/business-process-workspace.spec.js \
  src/views/app-center/components/__tests__/application-flow-interaction.spec.js \
  src/views/app-center/in-app-builder/__tests__/in-app-builder-schema.spec.js
```

```bash
NODE_OPTIONS=--max-old-space-size=8192 node ./node_modules/vite/bin/vite.js build
```

```bash
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-generator \
  -Dtest=BusinessApplicationRuntimeServiceTest,BusinessExtensionExecutionServiceTest,\
BusinessExtensionValidationServiceTest,BusinessFlowStatusFieldServiceTest,\
BusinessProcessSchemaValidatorTest,BusinessProcessOrchestratorTest,LowcodeDdlAdditiveColumnTest test
```

```bash
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests
```

真实运行态最小验收清单保留为：创建记录确认 `DRAFT`；发起确认 `IN_PROCESS`；分别验证通过、驳回、取消；检查业务 `status` 原值；重新进入页面组确认父子页；在真实发布版本验证四类增强钩子。

## 7. 2026-08-21 作用域 CSS 模板编译修复

- 复现：`PortalPageRenderer.vue` 模板直接包含原生 `<style v-for>`，Vue 报 `Tags with side effect are ignored in client component templates`。
- 修复：新增 `RuntimeScopedStyles`，通过 Vue 渲染函数创建样式节点并 Teleport 到 `document.head`；响应式更新及组件卸载时清理旧样式。
- 回归：验证有效 CSS 挂载、空 CSS 跳过、属性变化移除旧样式、组件卸载清理样式，并重新执行前端生产构建。

## 8. 2026-08-21 流程节点配置布局回归修复

- 桌面节点配置面板恢复为 720px 宽工作面板，保留内容区独立滚动和底部固定操作区。
- 审批主配置与流程预览按面板自身宽度切换单双列，不再使用浏览器视口宽度判断。
- 字段赋值、流程状态、开始条件和条件分支在窄面板下自动换行或降为单列。
- 复跑业务流程设计器两个组件测试文件，并执行前端生产构建和 `git diff --check`。

## 9. 2026-08-21 表单绑定与流程问题可用性回归

- 空表单目录时，审批节点不再使用 `objectCode` 伪造 `formKey`，并显示明确空态。
- 同一 `formKey` 的设计态空字段资产与运行态有字段资产合并，保留展示名称并补齐字段目录、字段数、预览和保存能力。
- 没有低代码运行配置时，从当前业务对象受治理字段注册表生成真实对象表单资产，供流程绑定和校验使用。
- 依赖级表单错误展示具体表单引用；节点级与依赖级同类错误去重；错误卡片可定位或复制完整消息、编码、路径和建议。
- 前端定向测试：44 项通过；前端生产构建成功。
- 后端定向测试：`BusinessFlowServiceFormAssetMergeTest`、`BusinessProcessSchemaValidatorTest` 共 19 项通过（JDK 17）。
- 浏览器联调曾成功进入设计器并确认真实流程对象为 `business_object`，此前接口返回空资产；修复后的后端真实接口复验因本轮后端随后离线而跳过，不能将数据库/Flowable E2E 视为通过。

## 10. 2026-08-21 流程行锁 SQL 回归

- 目标：防止多租户 SQL 重写把 `LIMIT 1 FOR UPDATE` 变成 MySQL 非法的 `FOR UPDATE LIMIT 1`。
- 覆盖：任务、流程业务、错误日志、填报明细四个 Flow Mapper 的带锁查询；高风险审批 Mapper 同步采用无冗余 `LIMIT` 的锁定写法。
- 命令：

  ```bash
  mvn -o -Penable-tests \
    -pl forge-framework/forge-plugin-parent/forge-plugin-flow \
    -Dtest=FlowLockMapperSqlContractTest \
    -Dmaven.test.skip=false test
  ```

- 结果：1 项通过；静态扫描未发现实际 `FOR UPDATE ... LIMIT` 或 `LIMIT ... FOR UPDATE` 锁查询。
- 未执行：本轮未启动服务、未连接数据库；部署新后端后需重新执行一次审批/任务操作确认线上 SQL 日志不再出现语法错误。

## 11. 2026-08-21 流程启动按钮与多字段显示条件回归

- 列表记录通过批量运行态接口补齐 `_documentRuntime` 和 `activeProcessCodes`；同一 `processCode` 为活动态时隐藏启动动作。
- `START_PROCESS` 按流程编码精确隐藏；单据主流程 `START_FLOW` 继续按 `flowStatus/nextAction` 隐藏，二者不混用。
- `ROW / DETAIL / FORM` 独立编译为 `rowActions / detailActions / formActions`，编辑弹窗、抽屉和内嵌表单均消费 `formActions`。
- 应用工作台和正式门户读取 CRUD 渲染配置时传递当前应用入口 ID，验证对象级配置可叠加当前应用的已发布流程动作。
- 开始节点可添加多条字段规则并选择 AND/OR；运行态覆盖结构化规则和已编译字符串的等于、不等于、IN、空值及数值比较。
- 前端定向验证覆盖动作位置隔离、活动流程精确隐藏、多条件求值、配置透传和应用入口上下文。
- 后端定向验证覆盖三种动作位置编译和运行配置隔离。
- 真实 MySQL/Flowable E2E 仍需在服务部署后执行，不以组件测试代替。

## 12. 2026-08-21 按钮显示条件删除回归

- 两条显示条件删除第一条后，只保留第二条且 AND/OR 配置不丢失。
- 删除最后一条或点击顶部“清除”后，完整移除 `visibleCondition`，未配置条件时按钮恢复为始终显示。
- 清空后提供“添加显示条件”入口，可以重新创建第一条规则。
- 节点抽屉提交完整配置时使用替换语义，确认已删除字段不会被画布旧配置合并回来；其他局部节点更新仍保留合并语义。
- 定向执行业务流程设计器两个测试文件，并执行前端生产构建和 `git diff --check`。

## 13. 2026-08-21 审批完成运行闭环回归

- 审批终态事件按 `tenantId + processInstanceId` 只认领一次等待中的业务流程运行；重复事件不产生第二次后继动作。
- `APPROVED` 出口连接 `UPDATE_RECORD` 时，当前主对象记录按发布态 `configKey` 更新，固定值 `2222` 原样进入字段 Map。
- 页面块字段快照早于 `flowStatus` 创建时，运行列表仍补入平台托管且列表可见的流程状态列；页面显式隐藏时不补入。
- 未启用单据模式但存在业务流程实例关联时，详情运行态返回 `processInstanceId`，前端展示流程时间轴/流程图。
- 定向验证后执行 Generator 编译、前端生产构建和 `git diff --check`；真实 MySQL/Flowable 流转只在部署新后端后验收。

## 14. 2026-08-21 审批动作发布态写入复核

- `UPDATE_RECORD` 业务动作使用 `DynamicCrudService.updateFieldsInternal`，同时执行发布模型字段/真实列/租户数据权限校验，不绕过运行配置。
- 固定值 `fieldMappings`、当前主对象记录 ID 和重复终态回调分别由 `BusinessProcessActionExecutorTest`、`BusinessProcessOrchestratorTest` 覆盖。
- 前端显示条件解析改为无复杂回溯正则的分词/比较解析，并复跑 27 项运行时定向测试、生产构建和 ESLint。
- 真实数据库写入和 Flowable 回调仍属于环境待验收项。

## 15. 2026-08-21 表单提交增强运行闭环

- 工作台接口只关联 `enabled_version`，并返回该不可变版本的 `content / processedContent`；草稿内容不进入预览运行时。
- 页面中的独立 `AiForm` 在发送保存请求前执行 `BEFORE_SUBMIT`，规则修改后的记录进入请求；返回 `false` 或 `BLOCK` 异常时不请求后端。
- 保存成功后执行 `AFTER_SUBMIT`，字段变化执行 `FORM_CHANGE`；嵌套表单继续透传同一套增强钩子。
- 用户场景固定覆盖：评分字段 `fieldRate=2`、条件运算符 `EQ`、动作 `SHOW_MESSAGE`，断言提交前调用 `notify('info', '123')`。
- 编辑器测试通过不等于启用；测试通过后必须显式启用当前版本，工作台刷新后生效，正式运行还需重新发布应用。
- 真实页面提交 E2E 需要部署本轮后端 Mapper 并重启 Admin；本轮不以旧进程的响应作为通过证据。

## 16. 2026-08-21 指定用户审批人协议回归

- 前端解析旧 `flowable:assignee="${user_1001}"` 时迁移到 `assigneeUserId: "1001"`，不保留待求值表达式。
- 新选择的 `2090384244139360257` 全程按字符串保存，XML 写回为字面量 `flowable:assignee="2090384244139360257"` 并带 `assigneeType="custom"`。
- 发起人、部门主管、候选用户和 SPEL 表达式保持原语义；旧固定用户只在完整数字表达式下兼容。
- Flow 插件兼容测试覆盖顶层节点、嵌套子流程、动态表达式隔离以及冲突变量由 BPMN 固定用户覆盖。
- 真实 Flowable 发起与任务分配仍需重启 Admin/Flow 后在目标应用验收。
