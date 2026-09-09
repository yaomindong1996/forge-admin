# 低代码附件、定时任务重建与用户角色排序 Implementation Plan

> **For agentic workers:** 本计划按任务逐项执行，并在每项完成后运行对应的定向验证。

**Goal:** 修复低代码列表中的图片附件展示，提供可强制重建 Quartz 数据的定时任务操作，并让用户角色授权列表把当前用户角色置顶。

**Architecture:** 低代码列表沿用现有 `AiCrudPage`/运行时预览配置，在文件字段已经提供文件 ID 与文件名翻译时，按文件名扩展名选择 `AuthImage` 或普通文件名。定时任务在现有同步协调器之外增加“删除后重建”的显式路径，保持版本收敛、租户和审计边界不变。用户角色排序只在授权页面的当前分页数据上按已勾选角色稳定排序，不改变后端角色分页协议。

**Tech Stack:** Vue 3、Naive UI、Vitest、Java 17、Spring Boot、Quartz、JUnit 5、Mockito。

---

### Task 1: 低代码附件列表自动预览图片

**Files:**
- Create: `forge-admin-ui/src/components/ai-form/file-render-utils.js`
- Test: `forge-admin-ui/src/components/ai-form/__tests__/file-render-utils.spec.js`
- Modify: `forge-admin-ui/src/components/ai-form/AiCrudPage.vue`
- Modify: `forge-admin-ui/src/components/lowcode-builder/preview/LowcodePreviewPane.vue`

- [ ] 写测试覆盖图片扩展名识别、普通附件回退文件名、逗号分隔值。
- [ ] 在 `AiCrudPage` 和低代码预览使用同一辅助逻辑：`fileUpload` 的目标名称字段以图片扩展名结尾时使用 `AuthImage`，非图片保持文件名显示；值仍作为文件 ID/访问路径传给图片组件。
- [ ] 确认无文件、缺失名称字段和多文件场景不抛异常。

### Task 2: 定时任务增加 Quartz 强制重建

**Files:**
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/scheduler/JobScheduler.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/manager/JobScheduleCoordinator.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/service/ISysJobConfigService.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/service/impl/SysJobConfigServiceImpl.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-job/src/main/java/com/mdframe/forge/plugin/job/controller/JobConfigController.java`
- Modify: `forge-admin-ui/src/views/system/job-config.vue`
- Test: existing job scheduler/coordinator/controller contract tests

- [ ] 先补 Quartz 测试：已有残缺 Job/Trigger 时强制重建后两者都存在，状态与配置一致。
- [ ] 增加 `rebuild` 链路，删除同 key Quartz Job 后重新创建；一次性任务遵循既有“过期且不补偿”规则。
- [ ] 增加受权限保护的 `/job/config/{id}/rebuild` 接口与前端“重建调度”操作，允许已显示 SYNCED 但 Quartz 数据实际丢失的任务执行。

### Task 3: 用户角色授权列表置顶当前角色

**Files:**
- Modify: `forge-admin-ui/src/views/system/user.vue`
- Test: `forge-admin-ui/src/views/system/__tests__/user-role-order.spec.js`

- [ ] 写排序测试，验证当前角色优先、同组保持原顺序、未选角色仍可见。
- [ ] 提供计算后的表格数据，并用于普通授权与批量授权表格；不改变选择 ID 和分页数据协议。

### Task 4: 增量验证

- [ ] 执行附件与用户页面 Vitest 定向测试。
- [ ] 执行 job 插件定向 JUnit 测试与 test-compile。
- [ ] 执行 `git diff --check`，记录未执行的真实服务/数据库验证。
