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
