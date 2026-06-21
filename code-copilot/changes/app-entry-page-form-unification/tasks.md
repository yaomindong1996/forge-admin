# 任务清单：app-entry-page-form-unification
> status: apply
> spec: `code-copilot/changes/app-entry-page-form-unification/spec.md`

## 任务

- [x] 梳理应用入口、业务对象、表单设计、列表设计和运行态保存链路。
- [x] 建立变更规格和任务清单。
- [x] 多表单 schema v2 兼容层：旧单表单自动包装为默认表单。
- [x] 后端 `FormDesignerSchemaDTO` 增加 `defaultFormKey/forms`，保持旧字段兼容。
- [x] 表单设计器 UI 文案从“设计对象”调整为“主表单/关联表单”。
- [x] 表单设计器补齐表单用途、默认表单、复制/删除/重命名的统一管理入口。
- [x] 入口配置支持选择业务对象、目标页面、目标表单和默认参数。
- [x] 入口配置显式维护 `permissionCode`，并补齐入口类型：对象列表、新增表单、详情页、审批/待办、报表/看板、外链/API。
- [x] 入口默认参数改为结构化编辑，支持 URL 公共参数和表单默认值。
- [x] 列表按钮、行操作、页面跳转配置支持 `targetFormKey`。
- [x] 运行态入口打开时传递并读取 `pageKey/formKey`。
- [x] 运行态入口参数接入列表公共查询和表单默认值。
- [x] 表单级权限、字段规则、表单事件完成协议和设计器配置。
- [x] 运行态应用表单字段规则：隐藏、必填、只读、默认值。
- [x] 表单事件运行态接入请求型事件：打开表单前、提交前、提交成功后。
- [x] 按钮组件补齐主点击动作配置，写入统一事件协议。
- [x] 按钮配置补齐权限码、二次确认和成功后行为。
- [x] 参数映射产品化：按钮、行操作支持固定值、当前行字段、路由参数、系统变量。
- [x] 运行态解析新参数映射协议，后端运行态构建保留 `sourceType/sourceField`。
- [x] 普通画布按钮运行态支持跳转、请求、确认提示和成功后行为。
- [x] 运行态响应式预览补齐桌面、窄屏、弹窗、抽屉、移动预设。
- [x] 表单草稿/发布版本隔离：运行态渲染读取 `publishedVersion` 对应快照，草稿保存不再污染线上入口。
- [x] 真实预览补齐模拟/真实列表/新增/编辑/详情模式、记录 ID、请求状态和错误展示。
- [x] 真实接口预览错误接入发布检查，失败阻断、未验证警告。
- [x] 按钮配置补齐显示条件，并在运行态联动当前用户权限过滤。
- [x] 表单事件补齐请求结果回填和自定义脚本白名单校验。
- [x] 发布检查补齐页面/表单引用、入口类型/权限、无动作按钮、动作目标、请求地址、敏感参数和表单治理校验。
- [x] 发布检查补齐动作参数映射校验：空参数名、来源字段缺失、字段不存在、来源类型无效。
- [x] 执行定向 eslint、前端构建和后端编译。

## 验证记录

- `pnpm --dir forge-admin-ui exec eslint ...`：通过，覆盖入口抽屉、对象设计器、表单设计器、列表设计器、动作设计器、运行态和 AiCrudPage。
- `pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js`：通过，覆盖本轮运行态补充。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/ai-form/AiCrudPage.vue src/views/ai/crud-page.vue`：通过，覆盖本轮按钮、参数映射和响应式预览补充。
- `pnpm --dir forge-admin-ui exec eslint src/components/lowcode-builder/page/ListPageGridDesigner.vue src/components/lowcode-builder/page/GridBlockRenderer.vue src/components/lowcode-builder/page/page-schema.js src/components/ai-form/AiCrudPage.vue src/views/ai/crud-page.vue src/views/app-center/components/designer/forge-form-designer/ForgePropertyPanel.vue`：通过，覆盖版本隔离后的前端承接、真实预览、按钮权限/显示条件和表单事件补充。
- `pnpm --dir forge-admin-ui build`：通过；存在项目既有 CSS `//` 注释警告和 store 动静态混用导入 chunk 警告。
- `mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`：通过。
- `git diff --check`：通过。

## 后续待办

- [ ] 参数映射继续产品化：入口参数和表单事件结果回填后续可抽成同一套映射编辑器；本轮已打通运行协议，暂未做 UI 组件级复用。
- [ ] 浏览器实机验证：本轮完成构建/编译/静态检查，未启动本地后端和浏览器做真实点击链路验证。
