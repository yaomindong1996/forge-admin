# 任务清单：lowcode-designer-field-sync-ux
> status: completed
> created: 2026-06-13
> spec: `code-copilot/changes/lowcode-designer-field-sync-ux/spec.md`

## 任务总览

| Task | 名称 | 状态 | 优先级 |
|------|------|------|--------|
| Task 1 | 允许表单设计器字段 ID 手动输入 | completed | P0 |
| Task 2 | 补齐字段资产与表单设计器双向同步 | completed | P0 |
| Task 3 | 修复无修改时左侧导航切换未保存提示 | completed | P0 |
| Task 4 | 字段配置抽屉改为内嵌属性面板 | completed | P0 |
| Task 5 | 前端校验与执行日志回填 | completed | P0 |
| Task 6 | 修复默认字段编码 input 与已有字段名称回显 | completed | P0 |
| Task 7 | 整合字段资产入口并修复新版画布 field_ 临时字段保存 | completed | P0 |
| Task 8 | 修复关联字段引用污染模型 Schema | completed | P0 |
| Task 9 | 修复表单设计器未修改也提示未保存 | completed | P0 |
| Task 10 | 修复低代码应用 LIST 入口误进静态预览 | completed | P0 |
| Task 11 | 修复表单新增字段运行态新增数据未入库 | completed | P0 |
| Task 12 | 清理表单删除后的自动字段并优化必填默认值 | completed | P0 |

## Task 1：允许表单设计器字段 ID 手动输入

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeBusinessComponents.js`

**执行要点**

- 调整基础属性和业务组件属性中的“组件字段ID”规则。
- 下拉保留已有字段选项，同时允许用户输入新字段 ID。
- 占位提示改为“选择已有字段或输入新字段ID”。

**验收标准**

- fcDesigner 属性面板中的字段 ID 可以输入新值。
- 已有字段仍可搜索选择。

## Task 2：补齐字段资产与表单设计器双向同步

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/fieldReferenceUtils.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/viewSchema.js`
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`

**执行要点**

- 新增前端字段引用重命名工具，支持替换表单设计 Schema 内的 `fieldBinding.fieldCode`、`fieldBinding.columnName`、组件 `props.fieldCode`、`props.fieldBinding.fieldCode`。
- 新增视图 Schema 字段编码替换工具，覆盖查询、列表、详情字段引用。
- 字段资产保存后识别旧字段编码和新字段编码；发生变化时向对象设计器上报同步信息。
- 对象设计器在本地先同步 `draft.formDesignerSchema`、`draft.pageSchema`、`draft.viewSchema`、`draft.displayField`，随后仍按现有逻辑 reload 服务端设计器。

**验收标准**

- 字段资产修改字段编码后，本地表单绑定和视图引用立即使用新字段编码。
- 保存后重新加载设计器不恢复旧字段编码。

## Task 3：修复无修改时左侧导航切换未保存提示

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormCreateDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

**执行要点**

- `flushDesigner()` 比较转换后的 schema 与当前 `modelValue`，相同则不触发 dirty。
- `syncDesignerDraft()` 比较字段资产、页面 Schema 和表单 Schema；没有实际差异时返回 `dirty: false`。
- 保留真实拖拽、删除、排序、属性编辑后的 dirty 上报。

**验收标准**

- 进入设计器后不做任何修改，切换左侧导航不提示未保存。
- 修改表单组件后切换导航仍能产生未保存状态。

## Task 4：字段配置抽屉改为内嵌属性面板

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFieldManager.vue`

**执行要点**

- 删除 `n-drawer` 包裹。
- 增加 `field-asset-workbench` 左右分栏。
- 右侧直接渲染 `BusinessFieldPropertyPanel`，未选中时展示面板内空状态。
- 小屏幕下分栏改为纵向布局。

**验收标准**

- 字段资产页点击字段后不再弹出抽屉。
- 属性编辑区与字段列表同时可见。

## Task 5：前端校验与执行日志回填

**涉及文件**

- `code-copilot/changes/lowcode-designer-field-sync-ux/execution-log.md`
- `code-copilot/rules/automated-testing-standard.md`

**执行要点**

- 按自动化测试标准读取验证规则。
- 执行本次前端相关的 lint/build 或可用的等价校验。
- 记录命令、结果、警告、跳过项和服务清理情况。

**验收标准**

- 执行日志包含本次校验命令和结果。
- 若无法完成全量校验，说明原因和残余风险。

## Task 6：修复默认字段编码 input 与已有字段名称回显

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/form-first/formCreateToForge.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/forgeToFormCreate.js`

**执行要点**

- 转换 form-create rule 时忽略 `input`、`select`、`dictSelect` 等设计器默认字段名，不再作为业务字段编码或数据库列名。
- 对设计器默认标题“输入框”等生成 `fieldInput1` 形式的稳定字段编码，并避免复用 `input` 作为组件 ID。
- 绑定已有字段资产时，组件标题优先使用字段资产 `fieldName` / `label`。
- 从 Forge Schema 回填到 form-create rule 时，旧组件标题为通用标题或字段编码时使用字段资产名称。

**验收标准**

- 拖入默认输入框后保存，不再生成 `columnName = input` 的新字段。
- 选择已有字段 `customerLevel` 后，表单组件标题同步为字段资产名称。

## Task 7：整合字段资产入口并修复新版画布 field_ 临时字段保存

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
- `forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue`
- `forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js`
- `forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue`

**执行要点**

- 新版画布拖入组件模板时不再生成 `field_` + 时间戳随机字段，改为 `fieldInput`、`fieldSelect` 等可读字段编码并自动去重。
- 保存表单布局前同步提交字段列表、`modelSchema`、`pageSchema` 和 `formDesignerSchema`，避免布局接口用旧字段资产校验新页面区域。
- 左侧主导航默认隐藏独立字段资产入口，表单设计器内保留“字段资产”货架，高级字段维护移入右上角更多菜单。
- 保存前兼容清理已存在的未保存 `field_...` 模板字段，未落库字段会先规整成稳定字段编码再沉淀为字段资产。

**验收标准**

- 新增字段组件后直接保存表单，不再报 `页面区域引用了不存在的字段: field_mqn7a19j`。
- 普通设计流程从“表单设计”完成字段新增和复用，不再需要单独进入字段资产面板。
- 高级字段资产维护仍可从右上角更多菜单进入。

## Task 8：修复关联字段引用污染模型 Schema

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

**执行要点**

- 表单保存时拆分主模型保存 Schema 与页面设计态 Schema。
- 提交给 `saveBusinessObjectDesigner` 的 `modelSchema.fields` 只保留主模型系统字段和业务字段。
- 页面布局同步仍使用包含关联字段引用的设计态 Schema，保证 `crm_contact__contactName` 这类 `fieldRef` 可继续用于页面区域。

**验收标准**

- 表单设计器保存关联对象字段时，不再报 `字段名格式不正确: crm_contact__contactName`。
- 关联字段引用仍保留在 `pageSchema.modelRefs` 和页面区域引用校验范围内。

## Task 9：修复表单设计器未修改也提示未保存

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`

**执行要点**

- 将表单设计器草稿构建改为纯计算，不在切换前检查过程中直接写回 `localSchema`。
- `syncDesignerDraft()` 使用当前设计器结果与保存态基线比较，避免字段属性自动归一化被当成用户修改。
- 对 `props.modelValue`、`effectiveModelSchema` 和表单 Schema 派生出的页面 Schema 同步使用静默写入；真实用户操作仍通过 `replaceZone()` 默认标记 dirty。

**验收标准**

- 进入表单设计器后不做任何修改，切换到其他面板不再弹出“未保存变更”提示。
- 拖拽字段、调整关系字段、修改表单属性后仍会触发未保存状态。

## Task 10：修复低代码应用 LIST 入口误进静态预览

**涉及文件**

- `forge-admin-ui/src/views/ai/crud-page.vue`

**执行要点**

- 标准 `pageKey=list` 入口不再优先进入 `ListPageGridDesigner readonly`。
- 让普通 `/ai/crud-page/:configKey?runtimeOpenMode=LIST` 入口直接渲染业务模板 / `AiCrudPage`。
- 仅保留非标准自定义页面使用网格运行容器。

**验收标准**

- 应用 LIST 入口打开后展示实际可操作的业务列表页，不再展示静态预览画布。
- 目标文件 ESLint、`git diff --check` 和前端生产构建通过。

## Task 11：修复表单新增字段运行态新增数据未入库

**涉及文件**

- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java`

**执行要点**

- 新增/更新写入白名单从单纯依赖运行态 `editSchema` 调整为 `editSchema + modelSchema` 兜底。
- 只补充 `modelSchema` 中启用、非系统、非主键、非自增、非只读、表单可见且数据库真实存在列的字段。
- 保留 STORED 公式字段写入和不可变系统字段过滤。

**验收标准**

- 表单设计器新增字段并同步 DDL 后，即使运行态 `editSchema` 尚未重建，新增/编辑记录也能保存新字段值。
- 请求体中的任意字段不会越过 `modelSchema` 和真实表列校验直接写入。
- `git diff --check` 和 generator 插件目标模块 Maven 编译通过。

## Task 12：清理表单删除后的自动字段并优化必填默认值

**涉及文件**

- `forge-admin-ui/src/views/app-center/components/designer/form-first/autoFieldRegistry.js`
- `forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlService.java`

**执行要点**

- 自动字段资产构建时，只保留当前表单仍绑定的自动创建字段；已从表单删除的 `source=designer/createIfMissing=true` 字段不再回写模型。
- 非自动创建的手工字段资产继续保留，避免高级字段资产被误删。
- 新版画布字段组件显式清空 `label` 时保留空字符串，不再被 schema 规范化立即回填。
- DDL 同步将 `required` 视为运行态表单校验；只有配置了非空默认值时才同步数据库 `NOT NULL DEFAULT ...`。

**验收标准**

- 删除表单中的自动创建字段并保存后，发布检查不再提示追加该字段列，例如 `field_mqn7a19j`。
- 字段显示名称可以一次性清空，输入过程中不自动回填。
- 必填字段没有默认值时，新增列或修改列不生成强制 `NOT NULL`。
- 目标文件 ESLint、前端生产构建、后端 generator 模块编译和 `git diff --check` 通过。
