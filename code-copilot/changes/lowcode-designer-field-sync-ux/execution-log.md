# 执行日志：lowcode-designer-field-sync-ux

## 2026-06-13

- 创建变更 Spec 与任务清单。

## 2026-06-13 验证记录

### 变更范围

- 前端低代码业务对象设计器：
  - 表单设计器字段 ID 可输入。
  - 字段资产保存后前端本地同步表单、页面、视图字段引用。
  - 表单设计器 dirty 等价判断。
  - 字段资产配置由抽屉改为页面内嵌属性面板。

### 执行命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint ...`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 2m 26s`。
  - 警告：存在既有 CSS `//` 注释格式警告；`src/store/index.js` 同时被动态和静态导入导致 chunk 警告。本轮未修改相关文件。
- `git diff --check -- <本轮相关文件>`
  - 结果：通过。
- `curl -I 'http://127.0.0.1:5173/app-center/object/demo/designer?objectId=1'`
  - 结果：HTTP 200，Vite 能返回前端入口。
- `curl -s 'http://127.0.0.1:5173/app-center/object/demo/designer?objectId=1'`
  - 结果：返回 `index.html`，包含 `/src/main.js` 入口。

### 非阻断失败与跳过项

- Node 工具函数冒烟：
  - 命令：`node --input-type=module -e "<动态导入 fieldReferenceUtils/viewSchema>"`
  - 结果：失败，原因是源码使用 Vite 扩展省略导入，裸 Node ESM 无法解析 `./formCreateToForge`。
  - 判定：非阻断，Vite build 已覆盖真实打包解析。
- 浏览器点击级验证：
  - 内置浏览器初始化返回 `Browser is not available: iab`。
  - 已降级为 Vite HTTP 路由检查，未覆盖登录后字段配置点击流程。

### 服务清理

- 首次启动 Vite dev server 因 `EMFILE: too many open files, watch` 退出。
- 使用 `ulimit -n 65535 && CHOKIDAR_USEPOLLING=true pnpm --dir forge-admin-ui exec vite --host 127.0.0.1 --port 5173` 成功启动本轮临时 Vite 服务。
- 验证后已通过 `Ctrl-C` 停止该 Vite 服务。

## 2026-06-13 追加验证记录：默认字段编码与已有字段名称

### 变更范围

- 修复 form-create 默认 `rule.field = input`、`select` 等设计器内部字段名被当作业务字段编码，导致保存时报“数据库列名重复: input”的问题。
- 修复绑定已有字段资产后，表单组件标题未优先带出字段资产名称的问题。

### 执行命令与结果

- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/form-first/formCreateToForge.js src/views/app-center/components/designer/form-first/forgeToFormCreate.js`
  - 结果：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/form-first/formCreateToForge.js forge-admin-ui/src/views/app-center/components/designer/form-first/forgeToFormCreate.js code-copilot/changes/lowcode-designer-field-sync-ux/spec.md code-copilot/changes/lowcode-designer-field-sync-ux/tasks.md code-copilot/changes/lowcode-designer-field-sync-ux/test-spec.md code-copilot/changes/lowcode-designer-field-sync-ux/execution-log.md`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 47.69s`。
  - 警告：仍存在既有 CSS `//` 注释格式警告，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 警告；本轮未修改相关文件。

### 非阻断失败与跳过项

- `pnpm --dir forge-admin-ui exec esno --help`
  - 结果：失败，原因是 `tsx` 在当前沙箱内创建本地 IPC socket 时返回 `EPERM`。
- `node --experimental-specifier-resolution=node --input-type=module -e "<直接导入 formCreateToForge.js>"`
  - 结果：失败，原因是 Node 原生 ESM 无法解析项目中 Vite 风格的省略后缀导入。
- `node --input-type=module -e "<Vite SSR ssrLoadModule formCreateToForge.js>"`
  - 结果：失败，原因是 Vite SSR 会连带加载 `@/utils` 加密模块，既有 `sm-crypto` CommonJS 命名导出在 SSR 下不兼容；该问题不影响生产构建。

### 服务清理

- 本轮未启动长期服务。
- 前端构建命令已正常退出，无需额外清理进程。

## 2026-06-21 追加验证记录：低代码应用 LIST 入口误进静态预览

### 变更范围

- 修复 `/ai/crud-page/:configKey?runtimeOpenMode=LIST` 标准应用入口优先进入 `ListPageGridDesigner readonly`，导致展示静态预览画布的问题。
- `pageKey=list` 或未显式传 `pageKey` 的入口直接渲染业务模板 / `AiCrudPage`，非标准自定义页面仍保留网格运行容器。
- 表格列宽适配只在实际渲染网格运行容器时启用，避免标准列表页被设计器布局元数据影响。

### 执行命令与结果

- `git diff --check -- forge-admin-ui/src/views/ai/crud-page.vue code-copilot/changes/lowcode-designer-field-sync-ux/test-spec.md code-copilot/memory/pitfalls.md`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/ai/crud-page.vue`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 1m 24s`。
  - 警告：仍存在既有 CSS `//` 注释格式警告，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 警告；本轮未修改相关文件。

### 跳过项

- 未启动后端服务、数据库或登录态浏览器，未做真实菜单入口点击验证；本轮以源码链路审查、ESLint 和生产构建作为自动化验证依据。

### 服务清理

- 本轮未启动长期服务。
- 前端构建命令已正常退出，无需额外清理进程。

## 2026-06-21 追加验证记录：新版画布 field_ 临时字段与字段资产入口

### 变更范围

- 新版 Forge 原生表单画布拖入组件模板时，不再生成 `field_` + 时间戳随机字段编码，改为可读业务字段编码并按当前 schema 去重。
- 表单布局保存前通过设计器保存接口同步提交字段、`modelSchema`、`pageSchema`、`formDesignerSchema`，避免后端布局接口用旧字段集校验新页面区域时报 `页面区域引用了不存在的字段`。
- 对象设计器左侧主导航默认隐藏独立“字段资产”入口，字段资产复用留在表单设计器左侧货架，高级维护移入右上角更多菜单。

### 执行命令与结果

- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue forge-admin-ui/src/views/app-center/components/designer/BusinessObjectDesignerShell.vue forge-admin-ui/src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js 'forge-admin-ui/src/views/app-center/object-designer.[objectCode].vue'`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFormDesigner.vue src/views/app-center/components/designer/BusinessObjectDesignerShell.vue src/views/app-center/components/designer/forge-form-designer/designerLayoutFactory.js 'src/views/app-center/object-designer.[objectCode].vue'`
  - 结果：首次失败，`regexp/prefer-d` 要求正则 `[0-9]` 改为 `\d`。
  - 修复后复跑通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 1m 13s`。
  - 警告：仍存在既有 CSS `//` 注释格式警告，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 警告；本轮未修改相关文件。

### 跳过项

- 未启动后端服务、数据库或浏览器登录态，未做真实拖拽后落库验证；本轮先覆盖前端保存链路和构建链路。

### 服务清理

- 本轮未启动长期服务。
- 前端构建命令已正常退出，无需额外清理进程。

## 2026-06-21 追加验证记录：关联字段引用污染模型 Schema

### 变更范围

- 修复表单设计器保存时把关联字段引用 `crm_contact__contactName` 写入 `modelSchema.fields`，导致后端按真实业务字段校验并报 `字段名格式不正确` 的问题。
- 保存 payload 拆分为主模型 Schema 和页面设计态 Schema：主模型 Schema 只提交系统字段和当前对象业务字段，页面设计态 Schema 继续保留关联字段用于布局同步。

### 执行命令与结果

- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFormDesigner.vue`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 1m 5s`。
  - 警告：仍存在既有 CSS `//` 注释格式警告，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 警告；本轮未修改相关文件。

### 跳过项

- 未启动后端服务、数据库或浏览器登录态，未做真实关联字段拖拽保存落库验证；本轮以源码链路审查、ESLint 和生产构建作为自动化验证依据。

### 服务清理

- 本轮未启动长期服务。
- 前端构建命令已正常退出，无需额外清理进程。

## 2026-06-21 追加验证记录：表单设计器未修改 dirty 误报

### 变更范围

- 修复表单设计器未做修改时切换面板仍弹出“未保存变更”的问题。
- `buildCurrentDesignerDraft()` 改为纯计算，不再在切换前检查过程中写回 `localSchema`。
- `syncDesignerDraft()` 改为与保存态基线比较，避免字段属性自动归一化产生误报。
- `localSchema` 派生同步增加静默写入模式，避免加载、模型同步、表单 Schema 编译等内部同步触发父级 dirty。

### 执行命令与结果

- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/BusinessFormDesigner.vue`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/BusinessFormDesigner.vue`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 1m 4s`。
  - 警告：仍存在既有 CSS `//` 注释格式警告，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 警告；本轮未修改相关文件。

### 跳过项

- 未启动后端服务、数据库或浏览器登录态，未做真实无修改切换面板点击验证；本轮以源码链路审查、ESLint 和生产构建作为自动化验证依据。

### 服务清理

- 本轮未启动长期服务。
- 前端构建命令已正常退出，无需额外清理进程。

## 2026-06-21 追加验证记录：表单新增字段运行态新增数据未入库

### 变更范围

- 修复动态 CRUD 新增/更新写入白名单只依赖旧 `editSchema`，导致表单设计器新增字段已同步 DDL 但运行态新增数据不入库的问题。
- `DynamicCrudService` 写入白名单调整为 `editSchema` 字段 + 已落表的 `modelSchema` 可写字段 + STORED 公式字段。
- 继续过滤系统字段、主键、自增、只读、隐藏/禁用字段和非真实表列，避免请求体任意字段越权写入。

### 执行命令与结果

- `git diff --check -- code-copilot/changes/lowcode-designer-field-sync-ux/spec.md code-copilot/changes/lowcode-designer-field-sync-ux/tasks.md code-copilot/changes/lowcode-designer-field-sync-ux/test-spec.md code-copilot/changes/lowcode-designer-field-sync-ux/execution-log.md code-copilot/memory/pitfalls.md forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/DynamicCrudService.java`
  - 结果：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 执行目录：`forge-server/`
  - 结果：通过，`BUILD SUCCESS`，总耗时 `12.235 s`。
  - 警告：`GenTableServiceImpl` 存在既有 deprecated API 提示，`BusinessObjectDesignerService` 存在既有 unchecked/unsafe operation 提示；本轮未修改相关代码。

### 跳过项

- 未启动后端服务、MySQL 或登录态浏览器，未执行真实低代码表单新增记录落库验证；本轮以源码链路审查、`git diff --check` 和 generator 目标模块编译作为自动化验证依据。

### 服务清理

- 本轮未启动长期服务。
- Maven 编译命令已正常退出，无需额外清理进程。

## 2026-06-21 追加验证记录：删除自动字段残留与显示名称清空

### 变更范围

- 修复表单组件删除后，自动创建字段仍残留在字段资产和 `modelSchema.fields` 中，导致发布检查继续提示追加 `field_mqn7a19j` 这类已不用字段列的问题。
- 修复新版画布字段“显示名称”清空时被 schema 规范化立即回填的问题。
- 调整 DDL 必填策略：必填字段未配置非空默认值时不再同步数据库 `NOT NULL`，由运行态表单校验必填；配置默认值后才生成 `NOT NULL DEFAULT ...`。

### 执行命令与结果

- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/form-first/autoFieldRegistry.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlService.java`
  - 结果：通过。
- `git diff --check -- forge-admin-ui/src/views/app-center/components/designer/form-first/autoFieldRegistry.js forge-admin-ui/src/views/app-center/components/designer/form-first/formDesignerSchema.js forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/main/java/com/mdframe/forge/plugin/generator/service/lowcode/LowcodeDdlService.java code-copilot/changes/lowcode-designer-field-sync-ux/spec.md code-copilot/changes/lowcode-designer-field-sync-ux/tasks.md code-copilot/changes/lowcode-designer-field-sync-ux/test-spec.md code-copilot/changes/lowcode-designer-field-sync-ux/execution-log.md code-copilot/memory/pitfalls.md`
  - 结果：通过。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && pnpm --dir forge-admin-ui exec eslint src/views/app-center/components/designer/form-first/autoFieldRegistry.js src/views/app-center/components/designer/form-first/formDesignerSchema.js`
  - 结果：通过。
- `JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -pl forge-framework/forge-plugin-parent/forge-plugin-generator -am compile -DskipTests`
  - 执行目录：`forge-server/`
  - 结果：通过，`BUILD SUCCESS`，总耗时 `10.779 s`。
  - 警告：`GenTableServiceImpl` 存在既有 deprecated API 提示，`BusinessObjectDesignerService` 存在既有 unchecked/unsafe operation 提示；本轮未修改相关代码。
- `source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build`
  - 结果：通过，`✓ built in 1m 6s`。
  - 警告：仍存在既有 CSS `//` 注释格式警告，以及 `src/store/index.js` 同时动态/静态导入导致的 chunk 警告；本轮未修改相关文件。

### 跳过项

- 未启动后端服务、MySQL 或登录态浏览器，未执行真实拖入字段、删除字段、保存表单、发布检查和 DDL 预览接口验证；本轮以源码链路审查、ESLint、前端构建和后端目标模块编译作为自动化验证依据。

### 服务清理

- 本轮未启动长期服务。
- 前端构建和 Maven 编译命令已正常退出，无需额外清理进程。
