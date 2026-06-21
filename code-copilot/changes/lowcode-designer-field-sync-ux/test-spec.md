# 测试计划：lowcode-designer-field-sync-ux

## 本轮增量验证

### P0 验证

- 聚焦 ESLint：验证本次修改的 Vue/JS 文件语法、未使用变量和基础风格。
- 前端生产构建：验证 Vite 构建链路和路由懒加载编译。
- `git diff --check`：验证补丁无尾随空白等格式错误。
- Vite HTTP 路由检查：验证对象设计器路由能返回前端入口。

### P1 验证

- 字段引用重命名工具冒烟：尝试在 Node 环境直接导入工具函数并验证 `customerLevel -> customerGrade` 替换。

## 跳过项

- 浏览器点击级验证：当前 Codex in-app browser 返回 `Browser is not available: iab`，无法通过内置浏览器操作页面。
- 后端接口验证：本轮未修改后端接口，字段保存最终一致性沿用既有后端 `BusinessFieldDesignService.updateField()` 的引用替换逻辑。

## 2026-06-13 追加验证：默认字段编码与已有字段名称

### P0 验证

- 聚焦 ESLint：验证 `formCreateToForge.js`、`forgeToFormCreate.js` 的语法和未使用变量。
- 前端生产构建：验证真实 Vite 打包链路能解析本轮转换逻辑。
- `git diff --check`：验证本轮补丁无尾随空白等格式错误。

### P1 验证

- 尝试使用 Node / Vite SSR 做转换函数冒烟，覆盖 `rule.field = input` 和已有字段 `customerLevel` 场景。

### 跳过或降级项

- 转换函数直接冒烟受 Vite 省略后缀导入、`tsx` IPC socket 限制以及 SSR 下既有 `sm-crypto` CommonJS 命名导出问题影响，未作为通过依据。

## 2026-06-21 追加验证：新版画布 field_ 临时字段与字段资产入口

### P0 验证

- 聚焦 ESLint：验证 `BusinessFormDesigner.vue`、`BusinessObjectDesignerShell.vue`、`designerLayoutFactory.js`、`object-designer.[objectCode].vue` 的语法、未使用变量和基础风格。
- `git diff --check`：验证本轮补丁无尾随空白等格式错误。
- 前端生产构建：验证新版画布模板字段编码、保存 payload 和对象设计器导航改动能通过 Vite 打包。

### 跳过或降级项

- 浏览器点击级验证：本轮未启动后端服务和登录态浏览器，未执行真实拖拽保存；以源码链路审查、ESLint 和生产构建作为本轮自动化验证依据。
- 后端接口验证：本轮未修改后端接口，保存顺序通过前端 payload 调整规避布局接口旧模型校验问题。

## 2026-06-21 追加验证：关联字段引用污染模型 Schema

### P0 验证

- 聚焦 ESLint：验证 `BusinessFormDesigner.vue` 保存草稿拆分后的语法、未使用变量和基础风格。
- `git diff --check`：验证本轮补丁无尾随空白等格式错误。
- 前端生产构建：验证表单设计器保存链路调整能通过 Vite 打包。

### 跳过或降级项

- 浏览器点击级验证：本轮未启动后端服务、数据库和登录态浏览器，未执行真实关联字段拖拽保存。
- 后端接口验证：本轮未修改后端校验器；通过源码审查确认后端 `validatePage` 已允许 `pageSchema.modelRefs[].fields[].fieldRef`，前端只需避免把关联 `fieldRef` 写入 `modelSchema.fields`。

## 2026-06-21 追加验证：表单设计器未修改 dirty 误报

### P0 验证

- 聚焦 ESLint：验证 `BusinessFormDesigner.vue` 的静默 schema 同步、草稿基线比较和保存链路语法。
- `git diff --check`：验证本轮补丁无尾随空白等格式错误。
- 前端生产构建：验证表单设计器 dirty 修复能通过 Vite 打包。

### 跳过或降级项

- 浏览器点击级验证：本轮未启动后端服务和登录态浏览器，未执行真实页面切换点击；以源码链路审查、ESLint 和生产构建作为自动化验证依据。

## 2026-06-21 追加验证：低代码应用 LIST 入口误进静态预览

### P0 验证

- 聚焦 ESLint：验证 `crud-page.vue` 运行态分支判断、模板引用和计算属性语法。
- `git diff --check`：验证本轮补丁无尾随空白等格式错误。
- 前端生产构建：验证 `/ai/crud-page/:configKey` 动态运行页和懒加载模板能通过 Vite 打包。

### 跳过或降级项

- 浏览器点击级验证：本轮未启动后端服务、数据库和登录态浏览器，未执行真实菜单入口点击；以源码链路审查、ESLint 和生产构建作为自动化验证依据。

## 2026-06-21 追加验证：表单新增字段运行态新增数据未入库

### P0 验证

- `git diff --check`：验证 `DynamicCrudService.java` 和变更记录补丁无尾随空白等格式错误。
- 后端目标模块编译：验证 `forge-plugin-generator` 及其依赖模块在 Java 17 下编译通过。
- 源码链路审查：确认运行态新增/更新写入白名单包含 `editSchema` 字段、已落表的 `modelSchema` 可写字段和 STORED 公式字段，并继续过滤系统不可写字段。

### 跳过或降级项

- 真实数据库新增数据验证：本轮未启动后端服务、MySQL 和登录态浏览器，未执行真实表单新增记录；以目标模块编译和源码链路审查作为自动化验证依据。

## 2026-06-21 追加验证：删除自动字段残留与显示名称清空

### P0 验证

- `git diff --check`：验证表单字段资产清理、显示名称规范化、DDL 默认值策略和变更记录无尾随空白等格式错误。
- 聚焦 ESLint：验证 `autoFieldRegistry.js`、`formDesignerSchema.js` 的语法、未使用变量和基础风格。
- 前端生产构建：验证新版画布 schema 规范化和自动字段资产构建能通过 Vite 打包。
- 后端目标模块编译：验证 `LowcodeDdlService` 必填默认值策略在 Java 17 下编译通过。

### 跳过或降级项

- 浏览器点击级验证：本轮未启动后端服务、数据库和登录态浏览器，未执行真实拖入字段、删除字段、发布检查点击。
- 真实 DDL 验证：本轮未连接 MySQL 执行 DDL 预览/同步接口，以源码链路审查、前端构建和后端编译作为自动化验证依据。
