# 全表检索查询收敛 - 测试计划

## P0
1. `SysUserMapper.xml` 的 `selectUserPage` / `selectExportList` 不含 `GROUP BY sut.user_id` 这类非相关聚合，且子查询带 `user_id = u.id`。
2. `FlowTaskMapper.xml` 有 `selectCandidateTasks`，含 `tenant_id` 与未签收条件；`FlowTaskServiceImpl` 的 `candidateTasks` 不再 `.list()`。
3. `FlowTimeoutServiceImpl` 使用 `listPage`，源码不含 `.active().list()`。

## 命令

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd forge-server
mvn test -P enable-tests -pl :forge-plugin-system,:forge-plugin-flow -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SysUserMapperSqlContractTest,FlowTaskMapperSqlContractTest,FlowTimeoutServiceQueryContractTest
```

## 跳过
- 不启动 Admin/Flow 真实服务。
