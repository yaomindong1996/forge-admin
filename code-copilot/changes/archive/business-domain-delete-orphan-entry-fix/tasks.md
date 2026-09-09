# 任务拆分 — 业务域删除孤立应用入口修复
> status: completed
> created: 2026-08-05

## 前置条件

- [x] 已确认应用删除会解除停用入口的 `application_id`，但保留入口记录。
- [x] 已确认业务域汇总已返回 `appCount`，无需新增接口或数据库字段。
- [x] 已读取适用 Skill、上一变更 Spec 与测试基线。
- [x] 保留用户已有 `.DS_Store` 变更。

## Task 1：建立后端失败回归

- [x] 修改 `BusinessSuiteServiceTest`：显式确认时孤立入口应按“入口 → 业务域”顺序逻辑删除。
- [x] 增加未确认入口数量提示测试。
- [x] 增加入口仍绑定有效业务应用时失败关闭测试。
- [x] 运行定向测试并记录旧实现 Red。

## Task 2：实现孤立入口安全清理

- [x] `BusinessSuiteMapper` 新增：
  ```java
  Long countActiveApplicationEntryReferencesBySuite(Long tenantId, String suiteCode);
  int logicDeleteEntriesBySuite(Long tenantId, String suiteCode);
  ```
- [x] Mapper XML 使用 `ai_business_app` 与未删除 `ai_business_application` JOIN 校验有效引用。
- [x] Mapper XML 批量更新入口 `status=0, del_flag=id`，显式限定租户、业务域和 `del_flag=0`。
- [x] `BusinessSuiteService#delete` 把入口与对象合并为显式孤立资源清理，所有引用校验完成后才停用入口菜单。

## Task 3：更新前端确认交互

- [x] `app-center/index.vue` 同时读取 `appCount/objectCount`，任一大于零即传清理确认。
- [x] `suite.[suiteCode].vue` 同步相同行为。
- [x] 文案分别展示入口数和对象数，说明入口菜单停用、业务数据表与历史版本不物理删除。

## Task 4：验证与交付

- [x] 读取自动化测试标准和本变更文档后执行定向 JUnit。
- [x] 执行 Generator 聚合编译、Mapper XML、目标 ESLint、前端生产构建和差异检查。
- [x] 回填 Spec、Test Spec、执行日志和可复用踩坑。
- [x] 精确暂存本任务文件并提交，不推送远端。
