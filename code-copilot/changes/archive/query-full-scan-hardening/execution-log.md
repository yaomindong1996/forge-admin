# 全表检索查询收敛 - 执行记录

## 2026-09-02

Java 17。

```bash
cd forge-server
mvn test -P enable-tests -pl :forge-plugin-system,:forge-plugin-flow -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SysUserMapperSqlContractTest,FlowTaskMapperSqlContractTest,FlowTimeoutServiceQueryContractTest
```

- SysUserMapperSqlContractTest：1 passed
- FlowTaskMapperSqlContractTest：3 passed
- FlowTimeoutServiceQueryContractTest：2 passed
- BUILD SUCCESS

跳过：未启动 Admin/Flow 真实服务，未跑 EXPLAIN。
