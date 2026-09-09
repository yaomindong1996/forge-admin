# 执行日志 — 业务域删除孤立对象优化

## 2026-08-04 需求分析与验证基线

- 变更范围：应用中心业务域删除链路。
- 工作树基线：用户既有 `M .DS_Store`、`D forge/.DS_Store`，本任务保持不动。
- 根因证据：
  - `BusinessApplicationService.delete` 删除应用对象关联但保留业务对象。
  - `BusinessSuiteService.delete` 在应用/入口校验之前调用 `countObjectsBySuite`，孤立对象直接触发“该业务套件已存在业务对象，不能删除”。
  - `BusinessSuiteMapper.xml` 的对象统计已过滤 `del_flag = 0`，不是逻辑删除条件遗漏。
- 方案：新增默认关闭的显式孤立对象清理参数；仍有应用、入口或有效应用对象引用时失败关闭；确认后物理清理关系表、逻辑删除对象和业务域。
- 服务与数据库：未启动、未修改。
- 下一步：新增服务测试并确认 Red。

## 2026-08-04 Red / Green 与实现

- Red 命令：
  - `JAVA_HOME=... mvn -Penable-tests -Dtest=BusinessSuiteServiceTest test`
  - 结果：失败；6 处编译错误均为 `BusinessSuiteService.delete` 仅支持 `Long`，尚不支持预期的 `Long, boolean`。
- Green 实现：
  - Controller 新增默认 `false` 的 `cleanupOrphanObjects` 查询参数。
  - Service 先校验子域、业务应用和访问入口，再处理孤立对象；未确认时返回带数量的可操作提示。
  - Mapper 在清理前检查有效应用对象引用；物理删除 `ai_business_object_relation`，按 `del_flag = id` 逻辑删除 `ai_business_object`。
  - 前端两个删除入口根据 `objectCount` 明确提示影响并传递清理参数。
- Green 命令：同 Red。
- Green 结果：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`，构建成功。

## 2026-08-04 增量验证

- generator 模块全量测试：
  - 命令：`JAVA_HOME=... mvn -Penable-tests test`
  - 结果：共 569 个用例，2 failures、4 errors，其余 563 通过。
  - 非本轮失败：
    - `FormulaExecutionEngineLookupTest.lookupResolverFailureIsTraced`
    - `FormulaValueMaskerTest.masksStandaloneSensitiveValues`
    - `BusinessBindingApplicationTargetTest.applicationTargetValidatesIdAndCode`（测试对象 `baseMapper` 未注入）
    - `BusinessExtensionVersionServiceTest` 2 个用例（`applicationChangeTracker` 未注入）
    - `LowcodeRuntimeConfigBuilderTest.doesNotPublishOneToManyChildRelationAsRelationNameTranslation`（测试模型缺少自增主键约束）
  - 判断：上述类与本轮 Controller、Suite Service/Mapper 及前端删除入口没有调用或文件重叠；不作为本轮阻断，后续应单独修复测试基线。
- 前端生产构建：
  - 命令：`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm build`
  - 结果：通过，8847 个模块，耗时 3m43s。
  - 警告：既有组件命名冲突、动态/静态导入和 CSS `//` 注释警告，无本轮新增错误。
- 目标 ESLint：
  - 命令：`pnpm exec eslint src/api/business-app.js src/views/app-center/index.vue 'src/views/app-center/suite.[suiteCode].vue'`
  - 结果：0 errors、3 warnings；均为 `index.vue` 既有属性顺序警告（95-97 行）。
- Mapper XML：`xmllint --noout .../BusinessSuiteMapper.xml` 通过。
- 差异检查：`git diff --check` 通过。
- 服务与数据库：按用户偏好未启动真实服务、未执行数据库变更；本轮也没有 Flyway 变更。
