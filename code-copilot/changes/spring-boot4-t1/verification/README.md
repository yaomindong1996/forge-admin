# T1 真实源码隔离验证

本工程直接编译 Forge 的真实采购 BPMN、流程表单策略和改造后的 AI 适配器。
不继承生产 POM、不引用预先安装的 Forge JAR，输出目录独立，不读取服务 dev/test 配置。
数据库仅用 H2，HTTP 仅 loopback；结束自动关闭 fixture，不需要真实 API Key。

从仓库根目录运行（Java 17 + Maven）：

```bash
mvn -B -ntp -f code-copilot/changes/spring-boot4-t1/verification/pom.xml -Penable-tests clean test
```

与生产 reactor 的回归是互补关系：这里验证真实 BPMN 引擎行为和 AI HTTP 协议，
生产 reactor 验证自动配置、依赖聚合、业务编译与安全链。两者都不代表存量 MySQL/真实供应商验收。

采购用例复用 T0 现有源码，T0 原始失败日志不变；本工程执行的是 T1 改造后的当前源码。
完整命令与结果见上级 `execution-log.md` 和 `test-spec.md`。
