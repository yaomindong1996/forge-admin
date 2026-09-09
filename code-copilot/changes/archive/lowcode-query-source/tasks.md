# 低代码统一参数化只读查询源任务

## 1. SDD 与现状盘点

- [x] 复用上一阶段外部连接器与数据集安全基线。
- [x] 确认统一来源固定为 `EXTERNAL_API`、`DATASET`。
- [x] 明确字段事件、回填和前端竞态治理属于下一阶段。
- [x] 建立 `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md`。

## 2. 外部 API 查询契约

- [x] 增加低代码查询开关和输入/输出 Schema 字段及 Flyway。
- [x] 外部 API 管理接口读写新字段并校验只读方法、权限和 Schema。
- [x] 增加外部查询契约校验器，拒绝未知、缺失、错类型和超长参数。
- [x] 增加按稳定 `systemCode/apiCode` 解析的查询源 Mapper XML。
- [x] 增加外部查询源目录、元数据和执行 Facade。
- [x] 外部接口管理页支持配置查询开关与输入/输出 Schema。

## 3. 数据集运行 Facade

- [x] 新增 `DataDatasetRuntimeService`，封装发布态、状态、ACL、连接和字段校验。
- [x] 原数据集 Runtime Controller 委托新 Service，保持接口兼容。
- [x] 提供按 `datasetCode` 获取元数据和执行查询能力。

## 4. 统一低代码查询网关

- [x] generator 模块声明 external/data 依赖。
- [x] 定义统一来源引用、执行 DTO、目录/字段/结果 VO。
- [x] 实现目录、元数据与执行 Service，仅做来源路由和统一封装。
- [x] 实现 `/ai/lowcode/query-source` Controller。
- [x] 增加前端统一查询源 API 封装。

## 5. 测试与验收

- [x] 增加外部契约、Facade、Mapper/Flyway 契约测试。
- [x] 增加数据集 Runtime Service 测试。
- [x] 增加统一查询源 Service/Controller 契约测试。
- [x] 执行 external/data/generator 定向测试和聚合编译。
- [x] 执行前端 Lint/构建和差异静态检查。
- [x] 回填执行记录并完成 Spec/代码质量自审。
