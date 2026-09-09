# 任务拆分 — 业务域删除孤立对象优化
> 拆分顺序：接口协议 → 底层实现 → 上层交互 → 验证收尾
> 每个任务聚焦一个可验证的原子结果

## 前置条件

- [x] 已确认应用删除会保留业务对象，当前报错来自真实孤立对象，不是逻辑删除过滤遗漏。
- [x] 已确认业务应用与业务对象必须属于同一业务域，应用存在关联时禁止直接迁移业务域。
- [x] 已读取自动化测试标准并建立本变更 `test-spec.md`、`execution-log.md`。
- [x] 保留用户已有 `.DS_Store` 工作树变更，不纳入本任务。

## Task 1: 建立删除服务回归测试

- **状态**：已完成
- **目标**：用单元测试固定阻断顺序、显式清理与防误删行为，并先确认 Red。
- **涉及文件**：
  - `forge-server/forge-framework/forge-plugin-parent/forge-plugin-generator/src/test/java/com/mdframe/forge/plugin/generator/service/businessapp/BusinessSuiteServiceTest.java` — 新增服务删除场景测试。
- **关键签名**：
  ```java
  void deleteCleansOrphanObjectsAfterExplicitConfirmation()
  void deleteRejectsCleanupWhenApplicationStillExists()
  void deleteRejectsObjectsWithoutExplicitConfirmation()
  void deleteRejectsObjectsReferencedByActiveApplication()
  ```
- **验收标准**：实现前测试失败；实现后所有新增用例通过。

## Task 2: 实现后端显式孤立对象清理

- **状态**：已完成
- **目标**：扩展删除协议，在事务内安全清理关系和孤立对象。
- **涉及文件**：
  - `BusinessSuiteController.java` — 接收默认关闭的 `cleanupOrphanObjects` 参数。
  - `BusinessSuiteService.java` — 调整校验顺序并编排显式清理。
  - `BusinessSuiteMapper.java` — 新增有效应用引用统计、关系删除和对象逻辑删除方法。
  - `BusinessSuiteMapper.xml` — 实现显式租户/逻辑删除条件 SQL。
- **关键签名**：
  ```java
  public void delete(Long id, boolean cleanupOrphanObjects)
  Long countActiveApplicationObjectReferencesBySuite(Long tenantId, String suiteCode)
  int deleteObjectRelationsBySuite(Long tenantId, String suiteCode)
  int logicDeleteObjectsBySuite(Long tenantId, String suiteCode)
  ```
- **验收标准**：应用、入口或有效引用存在时不发生任何清理；显式确认后按顺序清理并删除业务域。

## Task 3: 更新前端删除确认与请求参数

- **状态**：已完成
- **目标**：让用户在删除前看见孤立对象清理影响，并明确传递清理意图。
- **涉及文件**：
  - `forge-admin-ui/src/api/business-app.js` — 删除 API 支持 `cleanupOrphanObjects`。
  - `forge-admin-ui/src/views/app-center/index.vue` — 根据 `objectCount` 生成确认文案并传参。
  - `forge-admin-ui/src/views/app-center/suite.[suiteCode].vue` — 同步旧详情页行为。
- **关键签名**：
  ```javascript
  export function deleteBusinessSuite(id, cleanupOrphanObjects = false)
  ```
- **验收标准**：有对象时明确提示会清理对象配置但不删除业务数据表；无对象时保持简洁确认；两处入口行为一致。

## Task 4: 增量验证与文档回填

- **状态**：已完成
- **目标**：完成后端单测、模块编译/测试、前端构建、XML 与差异检查并回填证据。
- **涉及文件**：
  - `test-spec.md`
  - `execution-log.md`
  - `spec.md`
  - `tasks.md`
- **验收标准**：必跑项有实际命令和输出；跳过真实服务/数据库联调时说明原因；工作树仅包含本任务文件和用户既有变更。
- **执行结果**：定向测试 6/6、前端构建、目标 ESLint、Mapper XML 和差异检查通过；generator 全量测试的 6 个非本轮失败已记录。
