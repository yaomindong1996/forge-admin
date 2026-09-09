# 流程待办/已办列表性能优化测试规格

## 本轮增量验证（2026-08-30）

本轮针对新增分页 count 解析兼容层验证，避免复杂候选组 SQL 在租户/数据权限拦截器中因
`COUNT(*)` 解析失败而报错。

### P0

- `CountOnePaginationInnerInterceptorTest`：构造包含多层 `EXISTS`、`CAST`、`FIND_IN_SET`
  和 `REPLACE` 的待办查询，确认自动 count 以 `COUNT(1)` 开头且可被 JSqlParser 解析。
- `forge-starter-orm` 编译，确认自定义分页拦截器及 MyBatis-Plus 装配无编译错误。
- `forge-plugin-flow` 聚合编译，确认流程插件可正常引用新的 ORM 分页拦截器。

### 跳过项

- 未启动 Admin/Flow 服务，未连接真实 MySQL；运行态分页 SQL、租户/数据权限上下文和执行
  计划由用户在真实环境重启服务后验证。
