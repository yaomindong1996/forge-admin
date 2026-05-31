# 执行日志：form-first-business-object-designer
> updated: 2026-05-31

## 本轮完成范围

- 应用中心 `/app-center`：收敛为套件导航、业务对象、应用入口三层结构，卡片高度、操作区和移动端布局已稳定。
- CRM 套件 `/app-center/suite/CRM`：交付验收改为侧栏摘要，阻断项、关键计数、下一步动作更清晰。
- 对象设计器表单页：修复 `fcDesigner` 在 1024px 下被空栅格列压缩的问题，设计器画布保持稳定最小宽度，窄屏由画布内部横向滚动承载完整设计器。
- 表单优先链路：FormDesignerSchema、ViewSchema、LinkageSchema、设计版本快照、发布检查和运行态编译已接入。
- 对象设计器入口：应用中心、套件页、对象详情页和新建对象后的默认设计入口统一切换到 `表单设计`。
- 对象设计器壳层：改为全屏浮层式工作台，左侧导航和主画布独立滚动，提升表单画布可用空间。
- 主子表运行态：子表新增行立即同步到父表单，复杂子表字段复用 `AiFormItem` 真实运行态组件；后端主子配置生成在编辑区缺少子表 fieldRef 时兜底输出可编辑子表字段。
- 业务组件预览：fcDesigner 加载和变更后会为字典、用户、组织、行政区划和引用对象拉取真实选项，不再依赖静态假数据。
- 表单布局一致性：FormDesignerSchema 的 `gridColumns`、组件 `span`、`labelPlacement`、`labelWidth` 进入 form-create 设计态和 AiCrudPage 运行态同一套编译链路。

## 验证命令

### 2026-05-31 动态运行态表单组件与布局回归修复

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint \
  src/components/ai-form/AiFormItem.vue \
  src/components/ai-form/AiForm.vue \
  src/views/app-center/components/designer/BusinessFormDesigner.vue
```

结果：通过。仍存在 `AiForm.vue` 既有 `vue/no-required-prop-with-default` 警告。

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests
```

结果：通过。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过。仍存在既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态 import chunk 和大 chunk 警告。

修复摘要：

- `AiFormItem` 对组织/用户选择组件增加历史别名兼容，避免动态页面退化为普通输入框。
- `AiFormItem` 用户选择回填从单一目标字段扩展为候选名称字段集合，并在选择、清空、编辑回填时保持展示文本一致。
- `AiFormItem` 组织树选择接入真实组织树选项，并为当前值补齐名称兜底选项。
- `BusinessFormDesigner` 保存表单设计时同步生成运行态 `fieldSettings`，覆盖顺序、span、labelWidth、align、组件类型和 label。
- `LowcodeRuntimeConfigBuilder` 运行态编辑字段优先使用设计器覆盖 label/defaultValue，并为组织/用户字段补齐默认回显字段。

### 2026-05-31 设计器交互回归修复

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint \
  src/views/app-center/components/designer/BusinessFormCreateDesigner.vue \
  src/views/app-center/components/designer/BusinessListDesigner.vue \
  src/views/app-center/components/designer/BusinessRelationDesigner.vue \
  src/views/app-center/components/designer/BusinessObjectDesignerShell.vue \
  'src/views/app-center/object-designer.[objectCode].vue'
```

结果：通过。

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests
```

结果：通过。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过。仍存在既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态 import chunk 和大 chunk 警告。

修复摘要：

- `BusinessFormCreateDesigner` 增加销毁态和加载序号保护，避免左侧菜单切换后异步 hydrate 继续调用空 `fcDesigner` 实例。
- `BusinessListDesigner` 移除查询条件、表格列、工具栏、行操作汇总侧栏。
- `BusinessObjectDesignerShell` 降低全屏层级，避免 form-create 预览弹层被遮挡。
- `BusinessRelationDesigner` 停止在 prop watcher 中反向 emit `linkageSchema`，保存级联时不再携带未加载或未修改的空关系数组。
- `object-designer.[objectCode].vue` 的通用草稿保存不再携带 `relations`，避免非关系面板保存误覆盖关系。
- `BusinessObjectDesignerDTO` 将 `fields`、`relations`、`designerOptions` 改为可空，避免部分保存请求被误判为“清空关系”。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFormDesigner.vue src/views/app-center/components/designer/BusinessFormCreateDesigner.vue
```

结果：通过。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint \
  src/views/app-center/index.vue \
  'src/views/app-center/suite.[suiteCode].vue' \
  src/views/app-center/components/BusinessObjectWizardDrawer.vue \
  'src/views/app-center/object.[objectCode].vue' \
  'src/views/app-center/object-designer.[objectCode].vue' \
  src/views/app-center/components/designer/BusinessObjectDesignerShell.vue \
  src/components/page-templates/ChildTableEditor.vue \
  src/views/app-center/components/designer/BusinessFormCreateDesigner.vue \
  src/views/app-center/components/designer/form-first/forgeBusinessComponents.js \
  src/views/app-center/components/designer/form-first/forgeToFormCreate.js \
  src/views/app-center/components/designer/form-first/formCreateToForge.js \
  src/views/ai/crud-page.vue
```

结果：通过。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过。仍存在既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态 import chunk 和大 chunk 警告。

备注：未加 `NODE_OPTIONS` 的首次构建在打包阶段因 Node 默认堆内存不足 OOM 退出；加大堆内存后构建成功。

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests
```

结果：通过。

```bash
python3 /private/tmp/forge_app_center_visual.py
```

结果：通过。mock 数据截图覆盖应用中心、CRM 套件页和对象设计器表单页。

## 接口验证

本地后端健康检查通过：`GET /actuator/health`。

使用已登录令牌验证 CRM 客户对象读取型接口均返回 HTTP 200：

- `GET /ai/business/object/1910000000000000101/designer`
- `GET /ai/business/object/1910000000000000101/layout/form`
- `GET /ai/business/object/1910000000000000101/fields`
- `GET /ai/business/object/1910000000000000101/publish-check`

未直接执行写入型接口：

- `PUT /ai/business/object/{objectId}/designer`
- `POST /ai/business/object/{objectId}/publish`

原因：当前本地后端连接共享数据库，直接保存或发布会改写真实草稿和版本。

## 截图路径

- `/private/tmp/forge_app_center_visual/app-center_desktop.png`
- `/private/tmp/forge_app_center_visual/app-center_tablet.png`
- `/private/tmp/forge_app_center_visual/app-center_mobile.png`
- `/private/tmp/forge_app_center_visual/app-center_suite_CRM_desktop.png`
- `/private/tmp/forge_app_center_visual/app-center_suite_CRM_tablet.png`
- `/private/tmp/forge_app_center_visual/app-center_suite_CRM_mobile.png`
- `/private/tmp/forge_app_center_visual/app-center_object_CRM_CUSTOMER_designer?suiteCode=CRM&panel=form_desktop.png`
- `/private/tmp/forge_app_center_visual/app-center_object_CRM_CUSTOMER_designer?suiteCode=CRM&panel=form_tablet.png`
- `/private/tmp/forge_app_center_visual/app-center_object_CRM_CUSTOMER_designer?suiteCode=CRM&panel=form_mobile.png`

## 剩余风险

- Task 39 CRM 客户对象真实端到端验收未执行，需要独立本地库或测试租户，避免污染共享环境。
- 全量 `eslint .` 仍有项目既有问题，已用本变更触达文件的 targeted lint 和全量 build 控制风险。

### 2026-05-31 组织数据源、运行态样式和字段 DDL 同步修复

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiForm.vue src/components/ai-form/AiFormItem.vue src/components/ai-form/AiCrudPage.vue src/components/ai-form/AiCrudPageProps.js src/views/ai/crud-page.vue src/views/app-center/components/designer/BusinessFormDesigner.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/views/app-center/components/designer/form-first/forgeToFormCreate.js src/views/app-center/components/designer/form-first/formCreateToForge.js
```

结果：通过。仍有既有 `AiForm.vue` `vue/no-required-prop-with-default` warning。

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests
```

结果：通过。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过。仍存在既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态 import chunk 和大 chunk 警告。

修复摘要：

- `AiFormItem` 对组织/树选择的远程数据源做有效性判断；空 `optionSource` 不再挡住默认组织树接口。
- `AiFormItem` 远程选项解包支持 `RespInfo.data`、分页 records/list/rows/items 和嵌套 data，并增强 value/label 字段兜底。
- 编辑表单运行态新增 `labelAlign`、`editXGap`、`editYGap`，表单设计器和后端运行配置同步保存/输出 rowGap、columnGap，2 列表单默认弹窗宽度提升到 `1040px`。
- 表单设计器保存时携带 `syncDdl/confirmSyncDdl`，后端通过 `LowcodeDdlService` 预览并执行缺失列同步，继续走 `ai:lowcode:deploy-ddl` 权限。

### 2026-05-31 表单布局组件与保存入口收敛

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/components/ai-form/AiForm.vue src/components/ai-form/AiFormLayoutNodes.vue src/views/ai/crud-page.vue src/views/app-center/components/designer/BusinessFormDesigner.vue src/views/app-center/components/designer/BusinessFormCreateDesigner.vue src/views/app-center/components/designer/form-first/formDesignerSchema.js src/views/app-center/components/designer/form-first/forgeToFormCreate.js src/views/app-center/components/designer/form-first/formCreateToForge.js
```

结果：通过。仍有既有 `AiForm.vue` `vue/no-required-prop-with-default` warning。

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests
```

结果：通过。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过。仍存在既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态 import chunk 和大 chunk 警告。

修复摘要：

- 布局/辅助组件在 `FormDesignerSchema` 中作为虚拟节点保存，保留 form-create 原始类型和布局元数据，避免 `fcRow/col/elTabs` 等回显成普通输入框。
- 表单设计保存同步生成 `formLayout` 树和继承后的字段 span，后端发布运行态下发 `options.editFormLayout`。
- `AiForm` 改为支持递归布局节点，编辑页可以渲染 row/col/card/tabs/collapse/divider，并保留原字段组件、校验和回填逻辑。
- 表单设计内部保存入口已收敛，只保留对象设计器顶部全局“保存”。

### 2026-05-31 应用入口、设计器弹层和多列表单回归修复

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/router/index.js src/router/guards/tab-guard.js src/store/modules/tab.js src/layouts/components/tab/index.vue src/components/ai-form/AiForm.vue src/views/app-center/index.vue 'src/views/app-center/suite.[suiteCode].vue' 'src/views/app-center/object.[objectCode].vue' 'src/views/app-center/object-designer.[objectCode].vue' src/views/app-center/components/designer/BusinessFormDesigner.vue
```

结果：通过。仍有既有 `AiForm.vue` `vue/no-required-prop-with-default` warning。

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-admin-server -am compile -DskipTests
```

结果：通过。

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

结果：通过。仍存在既有 UnoCSS 图标加载、CSS `//` 注释、动态/静态 import chunk 和大 chunk 警告。

修复摘要：

- 应用入口桥接页和业务对象设计器路由标记为 `skipTab`，tab guard 跳过登记并清理遗留桥接 tab。
- 动态 CRUD 页面 tab 增加强制可关闭能力，顶部 tab 支持按单个 tab 判断是否可关闭。
- 应用总览、套件详情、对象详情里的对象设计入口改为直接挂载全屏设计器弹层，不再通过路由新开设计器页签；设计器路由仍保留直达兼容。
- 搜索区操作按钮改为跨整行右对齐，避免按钮落在搜索 grid 第一列。
- 表单设计器新增单列/两列/三列控制，运行态三列表单默认弹窗宽度提升到 `1180px`。
