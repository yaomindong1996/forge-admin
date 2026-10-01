# 任务拆分 — 独立打印中心与多数据源打印
> 拆分顺序：数据模型 → 接口协议 → 底层实现 → 上层编排 → 入口层

## 前置条件

- [x] 用户确认总体方案和开始实施。
- [x] 创建隔离分支 `codex/standalone-print-center`。
- [x] 阅读 `AGENTS.md`、`forge-admin-ui/DESIGN.md`、编码和自动测试规范。

## Task 1：业务数据源模型与迁移 ✅

- **目标**：建立独立来源持久化模型，并使模板/绑定/执行表兼容无应用来源。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.204__add_standalone_print_sources.sql`
  - `forge-plugin-print/entity/PrintBusinessSource.java`
  - `forge-plugin-print/mapper/PrintBusinessSourceMapper.java`
  - `forge-plugin-print/resources/mapper/PrintBusinessSourceMapper.xml`
- **验证**：迁移静态扫描、Mapper 合同测试、print 插件编译。
- **结果**：模型、Mapper、V1.0.204 迁移和合同测试已完成；XML/占位符/diff 静态检查、打印模块 JUnit 和 Admin 聚合打包均通过。

## Task 2：来源协议、CRUD 与授权 ✅

- **目标**：提供固定 DTO 的来源管理 API，支持 SERVICE/DATASET 配置与修订号控制。
- **涉及文件**：
  - `forge-plugin-print/dto/PrintSource*.java`
  - `forge-plugin-print/vo/PrintBusinessSourceVO.java`
  - `forge-plugin-print/service/PrintBusinessSourceService.java`
  - `forge-plugin-print/controller/PrintBusinessSourceController.java`
- **验证**：DTO 校验、权限拒绝、逻辑删除引用保护测试。
- **结果**：已实现来源类型、固定 DTO/VO、JSON 配置校验、来源 CRUD、修订号/逻辑删除/引用保护和独立权限资源。

## Task 3：双来源身份与模板管理兼容 ✅

- **目标**：`PrintSourceRequest` 同时支持应用来源和独立 `sourceCode`；模板分页、创建和访问双路径工作。
- **涉及文件**：
  - `PrintSourceRequest.java`、`PrintTemplateCreateDTO.java`
  - `PrintTemplateAccess.java`、`PrintTemplateService.java`
  - `PrintTemplateMapper.java/xml`、`PrintTemplateVO.java`
- **验证**：旧 LOWCODE/CODE 请求回归；独立来源模板 CRUD 测试。
- **结果**：模板协议、访问控制、分页、持久化已支持应用/独立来源双路径；独立来源按租户从服务端重新解析规范身份，旧来源摘要算法保持不变。

## Task 4：独立绑定版本解析 ✅

- **目标**：独立来源运行时只使用启用绑定固定的已发布模板版本。
- **涉及文件**：
  - `PrintBinding.java`、`PrintBindingService.java`
  - `PrintBindingMapper.java/xml`
  - `PrintProviderRegistry.java`、`PrintPrepareService.java`
- **验证**：默认模板唯一、未发布模板拒绝、模板 ID 越权拒绝测试。
- **结果**：独立来源绑定强制选择已发布版本，运行时仅解析启用绑定中的固定版本；应用来源继续使用应用发布快照，不允许两种版本所有权混用。

## Task 5：受控参数与数据集 Provider ✅

- **目标**：复用数据集 ACL、行范围和参数 schema，输出统一打印字段与数据。
- **涉及文件**：
  - `PrintRecordRequest.java`、参数校验支持类
  - `forge-plugin-data/printing/DatasetPrintDataProvider.java`
  - `forge-plugin-data/pom.xml`
- **验证**：未知参数、未发布/禁用数据集、无 QUERY 权限、字段投影和记录匹配测试。
- **结果**：运行请求支持受控标量参数；DATASET Provider 复用数据集元数据、ACL、行范围和脱敏；SERVICE 通过服务端 Provider code 分发，采购示例已提供接入实现。

## Task 6：独立打印中心前端 ✅

- **目标**：实现克制的主从工作台，左侧业务来源，右侧模板资产和来源设置。
- **涉及文件**：
  - `forge-admin-ui/src/views/print/index.vue`
  - `components/print/center/*`
  - `stores/print/printCenterStore.js`
  - `api/print.js`
- **验证**：来源切换、空态、分页、窄屏和主题；Vitest + build + 浏览器点击。
- **结果**：已实现主从工作台、来源 CRUD/启停、模板资产、接入信息和响应式滚动边界；来源与模板管理权限分别控制。前端生产构建通过，真实后端数据的浏览器联调留部署环境验收。

## Task 7：统一业务打印调用入口 ✅

- **目标**：新增 `BusinessPrintButton`/`useBusinessPrint` 并复用现有选择、预览和执行事件。
- **涉及文件**：
  - `components/print/runtime/BusinessPrintButton.vue`
  - `components/print/runtime/useBusinessPrint.js`
  - `stores/print/printRuntimeStore.js`
  - `components/print/management/printRouteContext.js`
- **验证**：仅 ID、带受控参数、多模板和无模板场景测试。
- **结果**：新增统一按钮/composable；业务页面可只配置稳定 `sourceCode`，后端按租户解析启用来源；仅传记录 ID 和协议声明参数，复用模板选择、预览和执行事件。

## Task 8：资源、菜单与兼容回归 ✅

- **目标**：增加正式菜单/权限，回归低代码、流程和采购示例打印。
- **涉及文件**：
  - `V1.0.204__add_standalone_print_sources.sql`
  - 低代码/采购 Provider 兼容调整
  - 当前变更文档
- **验证**：后端聚合 package、前端 build、相关打印单测和 `git diff --check`。
- **结果**：打印中心菜单/权限和接入文档已补齐；268 项前端测试、146 项打印后端测试、2 项数据集打印测试和 Admin 46 模块聚合打包全部通过。

## Task 9：修复打印中心菜单层级 ✅

- **目标**：撤销把历史 `/print` 隐藏路由直接显示在“应用总览”下的错误做法，恢复应用总览，并将打印中心拆成独立一级目录。
- **涉及文件**：
  - `forge-server/db/migration/V1.0.205__repair_print_center_menu_hierarchy.sql`
  - `forge-admin-ui/src/router/index.js`
  - `forge-admin-ui/src/views/print/index.vue`
  - `components/print/center/PrintCenterWorkspace.vue`
  - `components/print/management/PrintTemplateList.vue`
- **验证**：菜单迁移合同、前端路由上下文、场景绑定、Store、ESLint、生产构建和 `git diff --check`。
- **结果**：打印中心与应用中心平级，下面拆为“业务数据源 / 打印模板 / 场景绑定”；设计器和预览继续隐藏，旧低代码 `/print?applicationId=...` 入口继续兼容。
