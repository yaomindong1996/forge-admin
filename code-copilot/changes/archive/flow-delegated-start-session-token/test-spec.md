# 交互登录走 start-delegated 时补委托 token - 测试计划

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd forge-server
mvn test -P enable-tests -pl :forge-starter-auth -am \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SaTokenFlowTokenProviderTest
```

跳过：不启动 Admin/Flow 服务。
