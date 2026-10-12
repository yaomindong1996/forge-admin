# Spring Boot 4 T0 隔离验证

这个 Maven 工程不在正式 `forge-server` reactor 中，不继承生产 POM，不加载业务配置。
使用 Java 17；不是发行版，也不是可以部署的 Forge 应用。

## 版本矩阵

| profile | Boot | Flowable | Spring AI | Alibaba |
|---|---|---|---|---|
| boot3-baseline | 3.5.13 | 7.0.1 | 1.1.8 | 1.1.2.3 |
| boot4-target | 4.1.1 | 8.0.0 | 2.0.1 | 2.0.0-M1.1 |
| boot4-old-ai | 4.1.1 | 8.0.0 | 1.1.8 | 1.1.2.3 |
| boot4-alibaba-upstream | 4.0.0 | 8.0.0 | 2.0.0-M1 | 2.0.0-M1.1 |

Alibaba 的 milestone 只用于验证，不因构建成功自动认定可以生产使用。
默认跳过测试，实际执行必须加 `-Penable-tests`。不要同时选择两个版本 profile。

## 模块边界

- `flow-probe`：直接编译仓库现有采购 BPMN、流程定义、状态枚举和五个 DTO/VO。
  Spring Boot 装配真实 Flowable aggregate Starter，在显式 H2 内存 DataSource 上执行流程。
  覆盖审批、会签、驳回重提、终止、转办、意见、租户限定查询、字段权限属性与动态变量。
  不加载 Forge Mapper、回调、任务授权服务，不替代其集成测试和真实 MySQL 迁移。
- `ai-probe`：直接编译十个真实源文件，包含当前 OpenAI / DashScope 适配器及最小实体依赖。
  只构造模型和验证 ChatClient / 会话内存 API，不调用真实供应商。
  升级导致原始源文件编译失败时，保留原样作为阻塞证据，不修改生产文件绕过。
- `mcp-probe`：直接编译当前协议 guard，对照原始 Streamable transport / Jackson 2 构造 API。
  不开放 MCP 路由，不声称已覆盖 OAuth / capability / 数据权限 / 全部自动配置。
- `ai-runtime-probe`：单独编译包含现有通义适配器在内的九个真实源文件，避免旧 OpenAI 错误遮挡。
  目标组合运行新的 OpenAI SDK 四项 PoC 和真实通义适配器三项验证。
  上游 milestone 组合仅运行相同的三项通义测试；其旧 OpenAI API 不编译新的 SDK PoC。
  HTTP 只绑定 loopback 随机端口并在每项测试后关闭，响应是合成数据，不代表真实供应商能力。
  这个模块不支持 Boot 3 基线，不能拿它替代 `ai-probe`。

## 执行

在仓库根目录执行：

```bash
mvn -f code-copilot/changes/spring-boot4-t0/verification/pom.xml \
  -Pboot3-baseline,enable-tests -pl flow-probe test
mvn -f code-copilot/changes/spring-boot4-t0/verification/pom.xml \
  -Pboot4-target,enable-tests -pl flow-probe test
mvn -f code-copilot/changes/spring-boot4-t0/verification/pom.xml \
  -Pboot4-target,enable-tests -pl ai-probe test
mvn -f code-copilot/changes/spring-boot4-t0/verification/pom.xml \
  -Pboot4-target,enable-tests -pl ai-runtime-probe test
mvn -f code-copilot/changes/spring-boot4-t0/verification/pom.xml \
  -Pboot4-target -pl ai-probe dependency:tree
```

完整矩阵：

```bash
bash code-copilot/changes/spring-boot4-t0/verification/run-matrix.sh
```

需要指定 Maven / Java / settings 时：

```bash
FORGE_T0_MVN=/absolute/path/to/mvn \
FORGE_T0_JAVA_HOME=/absolute/path/to/jdk17 \
FORGE_T0_SETTINGS=/absolute/path/to/settings.xml \
bash code-copilot/changes/spring-boot4-t0/verification/run-matrix.sh
```

脚本逐项运行，不因失败中断其它诊断，最终有任意失败则返回非零。
日志在 `target/logs/matrix.<随机标识>/`，每次复跑保留先前日志。
各模块输出在 `target/<profile>/`，均为生成文件，不提交。
每项测试先清理该模块该 profile 的生成目录，避免旧 class 掩盖编译错误。
无 `install`、`deploy`、`spring-boot:run`、生产数据迁移或业务缓存清理步骤。

## 验收

以 `../execution-log.md` 和 `../compatibility-report.md` 为准：核对真实 Tests run 和错误诊断。
“依赖已下载”“源文件编译通过”“测试通过”“生产就绪”是不同结论，不得混用。
