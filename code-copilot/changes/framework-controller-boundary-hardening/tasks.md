# 任务拆分 — Controller 边界与查询规范整改

## 前置条件

- [x] 已读取根目录规范、记忆文件和自动化测试标准。
- [x] 已在当前工作树重新扫描 Controller 基线。
- [x] 已确认用户授权按分阶段方案继续实施。
- [x] 不启动真实服务、不连接真实数据库、不执行流程清理。

## 执行状态

- [x] Task 1：修复分页协议和 Controller 异常边界。
- [x] Task 2：下沉 System Controller 查询。
- [x] Task 3：下沉 AI Controller 查询和事务更新。
- [x] Task 4：下沉 Generator Controller 查询。
- [x] Task 5：下沉 Data/Message Controller 数据操作。
- [x] Task 6：下沉 Flow Controller 查询、统计和清理。
- [x] Task 7：聚合验证并执行两阶段审查。

## Task 1：分页协议和异常边界

- **修改**：`SysCacheController`、`FlowMonitorController`、`FlowErrorLogController`，增加 `pageNum/page` 双读。
- **修改**：`SysUserController`、`SysExcelExportConfigController`、`FlowModelVersionController`、`ExcelEnhancedController`。
- **验收**：前端只传 `pageNum` 可正确翻页，旧 `page` 仍可用；Controller `throw new RuntimeException` 为零；Excel 不回传底层消息。

## Task 2：System Controller 查询下沉

- **修改**：`SysLoginLogController` 对应 Service/Mapper/XML；`LoginTenantAssetController` 改用已有或新增 Service 查询。
- **验收**：两个 Controller 无 Wrapper/直接 Mapper，过滤、排序和租户语义不变。

## Task 3：AI Controller 查询与事务更新下沉

- **修改**：`AiAgentController`、`AiModelController`、`AiProviderController` 及对应 Service/Mapper/XML。
- **验收**：Controller 无 Wrapper；默认项切换在 Service 事务内完成；列表条件不变。

## Task 4：Generator Controller 查询下沉

- **修改**：`GenController`、`GenDatasourceController`、`GenTemplateController`、`GenTableColumnController` 及对应 Service/Mapper/XML。
- **验收**：Controller 无 Wrapper；用途范围、表名、模板类型、字段排序等条件不变。

## Task 5：Data/Message 操作下沉

- **修改**：`DataDatasetController`、`MessageBizTypeController` 及对应 Service/Mapper/XML。
- **验收**：数据集状态更新和启用业务类型查询不再由 Controller 构造。

## Task 6：Flow 查询、统计和清理下沉

- **修改**：`FlowInstanceController`、`FlowErrorLogController`、`FlowMonitorController` 及对应 Service/Manager/Mapper/XML。
- **清理合同**：`FlowFormInstance`/`FlowFillBatchItem` 继续逻辑删除；其余 5 张 Forge 表仅在管理员确认清理端点执行物理删除。
- **验收**：三个 Controller 无 Wrapper/直接 Mapper；确认门禁、统计字段和失败清单保持。

## Task 7：聚合验证与审查

- 执行相关 Maven 模块 `package -DskipTests` 和增量单测。
- 执行 Mapper XML、Wrapper、RuntimeException、分页参数、尾随空白和 EOF 检查。
- 回填 `spec.md`、`test-spec.md`、`execution-log.md`。
- 先 Spec Compliance，通过后再 Code Quality Review。
