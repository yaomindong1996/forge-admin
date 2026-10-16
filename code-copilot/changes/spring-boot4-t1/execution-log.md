# T1 执行记录

## 2026-10-11：启动

- 当前分支 `codex/spring-boot4-t0-compatibility`，基线 `719a2fc0b28d929edef62769dc2adc5e75ef557e`。
- 保留 .DS_Store、多色导航 Spec/任务/测试/日志、production-runbook 的用户未提交改动。
- T0 验证目录与 AI pitfalls 是上阶段改动，本轮不覆盖原证据。
- 阅读项目编码/自动化测试规范与流程 Skill；按采购审批真实参考进行增量回归。
- 批准范围仅开发与隔离验证；不改 main、不提交、不部署、不连接真实数据服务。

后续命令、测试数量、失败原因与修复在实际执行后追加，不能预填通过。

## 2026-10-11：依赖与生产源码迁移

- 核验官方 Boot、Flowable、Spring AI、MP、Sa-Token、Redisson、springdoc 说明与实际 Maven 制品/API。
- 实施真实 root POM / Forge BOM，Java 17 和产品 revision=1.2.0 不变。
- 迁移 MP Spring Service 导入及两份 VM 模板；补 Spring 与 JSQLParser 4.9 模块，保留既有 SQL 协议。
- WebMVC/Tomcat、Jackson 2 桥、Flyway optional 装配、Sa-Token/Redisson API 随底座统一迁移。
- Flowable JsonNode 明确 Jackson 2 JSON 变量；MCP 新 transport 单独注入 Jackson 3 mapper。
- OpenAI 迁至官方 SDK 构造/协议；通义采用独立原生桥，不混用 Alibaba milestone 或改成兼容模式。
- 使用流程 Skill 的真实采购 BPMN、CRUD Skill 的模板兼容规范、OpenAI 官方文档的重试/异常边界。
- 全程没有更改真实配置/历史 SQL、没有连接 MySQL/Redis 或调用模型，没有启动业务服务。

## 2026-10-11：失败与修复（保留过程，不只记录绿色结果）

| 阶段 | 观察到的失败 | 修复 / 边界 |
| --- | --- | --- |
| 生产 clean package | MP Spring/解析器、Sa-Token、Redisson 与自动配置 API 改动 | 实际新包/类型迁移；四服务初步打包成功不等于后续测试成功 |
| regression-1～4 | Flyway 测试缺 WebMVC 模块、MP 工厂包、旧 SaToken staff、移除的 AI 异常类 | 补对应模块，迁移真实 API；Web 新 JSON 测试显式依赖 core |
| regression-5～6 | 旧 RecordingAdapter 未实现 embedding、ChatModel mock 仍用 getDefaultOptions、ASR 桩返回错误格式 | 修正 fixture；真实 SDK 按 response_format=text 收到纯文本 |
| regression-7～9 | 旧 Task 自动配置误改包；委托身份测试漏装限流拦截器；旧 Spring Sa 上下文导致 ModelBox 空 | 恢复实际 JAR 包名、补真实拦截器与 mock 配置、使用上游新 ThreadLocal 上下文 |
| MCP focused | ASYNC dispatch 的 SaRouter 缺请求上下文 | 注册上游同一个 Context Filter 至 REQUEST/ASYNC/ERROR，保留顺序与鉴权断言 |
| regression-final | Redis setIfAbsent 新增 Expiration 重载，any() 发生 testCompile 歧义 | 指定 any(Duration.class)，生产催办逻辑不变 |
| regression-final2 | 旧守卫断言指向提取前代码；v3 必填样例未携带 required=true | 核对未修改的当前权限协议，补调用链断言、显式必填、schema 回退/节点取消必填测试 |
| services-final-clean | 根 Surefire 使用 forge.tests.skip，-DskipTests 不会覆盖；随后 Mock Mapper 缺列元数据而报错 | 不修改构建规则；补内存元数据，保留组织越权/用户关系断言，7 项 focused 全通过 |
| services-final2-clean | 既有 SecureActionCatalogServiceTest 漏导入 SecureActionDescriptor | 仅补正确导入；该测试加入最终四服务测试集，不跳过 |
| services-final3-clean | 独立流程服务两项结构契约仍在主服务查找已提取至 ViewAssembler 的日志/可选查人代码 | 断言当前完整调用链及稳定错误边界；无生产策略修改，原 28 项边界断言全部保留 |

历史测试缺项不是本轮新增业务逻辑故障；相应生产动态表单/候选人/用户权限实现未改变。
未增加 testExcludes、excludedGroups 或关闭鉴权。失败日志保留在 `/private/tmp/forge-boot4-t1-*.log`。

## 2026-10-11 23:07～23:14：已完成验证

- `mcp-identity-fixed.log`：上下文注册 2 项、真实 MCP 委托身份 3 项，全通过。
- `contracts-final.log`：独立真实源码工程 28 项，0 失败 / 0 错误 / 0 跳过；含真实采购 8 项、JSON 变量 1 项。
- `flow-legacy-contracts.log`：动态数组权限 9 项、候选组调用链 2 项，全通过。
- `regression-final3.log`：37 模块风险相关 reactor，108 个测试类、444 项，0 失败 / 0 错误 / 0 跳过。
  完成时间 23:14 前，耗时 37.880 秒；流程 179 项、AI 75 项、MCP 16 项。
- `mybatis-policy.log`：组织越权及用户关联协调 7 项，全通过。
- 各日志的完整路径为 `/private/tmp/forge-boot4-t1-<上述文件名>`。
- `node scripts/forge-shared/version-cli.mjs --check`：发行版本 1.2.0，检查完成。
- `git diff --check`：无输出；新增 Java 文件均不超过 120 字符/行及 1000 行/类。

以上测试不等于完整项目套件；隔离 AI 用例与 reactor 有重叠，不叠加成“472 个独立测试”。
根 POM 原有 5 个 testExcludes 与 exclude 标签保持原样。

## 复跑命令

在仓库根目录使用 Java 17 / Maven。实际运行使用 Maven 3.9.11 和本机镜像 settings；
以下不依赖临时 JDK/Maven 路径，可在自己的开发环境复跑。

```bash
# 独立源码 + H2 / loopback HTTP，不读取 dev 数据源
mvn -B -ntp -f code-copilot/changes/spring-boot4-t1/verification/pom.xml \
  -Penable-tests clean test -Dstyle.color=never
```

```bash
# 风险相关测试选择；并非通过排除失败测试来伪造全量通过
FORGE_T1_TESTS='JacksonHttpContractTest,RequestBodyLimitFilterTest,SaTokenServletContextConfigurationTest,'
FORGE_T1_TESTS+='SaTokenFlowTokenProviderTest,ManagedCacheCodecTest,RedissonServerInfoCompatibilityTest,'
FORGE_T1_TESTS+='Plugin*Test,RuntimeLicenseAutoConfigurationTest,ReplayAttackFilterTest,MachineJsonProtocolTest,'
FORGE_T1_TESTS+='InternalCallRequestVerifierTest,OpenAiCompatibleProviderAdapterTest,DashScopeNativeProviderAdapterTest,'
FORGE_T1_TESTS+='AiProviderHttpContractTest,OpenAiMultimodalHttpContractTest,AiProvider*Test,AiModelFailureClassifierTest,'
FORGE_T1_TESTS+='AiClientImplTest,AiInvocationResolverTest,BpmnXmlUtilsTest,DynamicFormArrayPermissionValidatorTest,'
FORGE_T1_TESTS+='Flow*Test,Mcp*Test,SystemVersionControllerTest,RuntimeLicenseStatusSecurityTest,RemoteJobFlowExecutorTest,'
FORGE_T1_TESTS+='SysRoleAccessPolicyTest,SysUserRelationCoordinatorTest,FlywayClasspathConfigurationTest,'
FORGE_T1_TESTS+='JobFlowControllerSecurityContractTest,SecureActionCatalogServiceTest'

mvn -B -ntp -f forge-server/pom.xml \
  -pl forge-admin-server,forge-app-server,forge-flow/forge-flow-server,forge-report-server \
  -am -Pdev,enable-tests clean package \
  -Dtest="$FORGE_T1_TESTS" -Dsurefire.failIfNoSpecifiedTests=false -Dstyle.color=never
```

`failIfNoSpecifiedTests=false` 只让无上述测试的依赖模块继续构建，不忽略任何已选测试的失败。
没有业务服务 PID 需要停止；HTTP/H2 fixture 在测试结束关闭。

## 2026-10-11 23:26：最终四服务干净构建

执行上述最终复跑命令，原始日志 `/private/tmp/forge-boot4-t1-services-final4-clean.log`：

- Maven `BUILD SUCCESS`，51 模块均为 SUCCESS，耗时 01:06 min。
- 127 个测试类、543 项，0 失败 / 0 错误 / 0 跳过。
- 按实际各测试类行汇总，不把 Surefire 汇总行再次相加。
- 模块计数：plugin 102、cache 7、flow-client 17、auth 12、crypto 7、system 25、web 7、
  generator 4、AI 75、MCP 16、capability-platform 4、capability-actions 41、Admin 4、
  flow-plugin 179、Flow server 43。
- 开启测试源码编译，保留项目原有 5 项历史排除及 exclude 标签；不称为全部项目测试套件。
- 新增 Java 与本轮新增代码行均符合 120 字符限制；真实生产巨类本轮仅必要 API/导入变化，不声称已完成整体重构。

实际生成：

```text
forge-server/forge-admin-server/target/forge-admin-server.jar
forge-server/forge-app-server/target/forge-app-server.jar
forge-server/forge-flow/forge-flow-server/target/forge-flow-server.jar
forge-server/forge-report-server/target/forge-report-server.jar
```

用 `unzip -Z1 <jar>` 与 `unzip -p <jar> BOOT-INF/classes/META-INF/build-info.properties` 检查：

- 四服务 Boot 4.1.1 / Tomcat 11.0.24 / MP Boot 4 3.5.17 / DD Boot 4 4.5.0 / Sa-Token Boot 4 1.45.0。
- Spring 7.0.9、Jackson 2.21.5 与 3.1.5、Redisson 4.8.0；业务桥使用 spring-boot-jackson2。
- Admin/Flow/Report 相关 AI 模块为 2.0.1 / openai-java-core 4.49.0；独立 Flow 的引擎为 8.0.0。
- 四个 JAR 未发现 Undertow、Alibaba milestone、Boot 3 专用 starter；未读取/输出其数据源配置。
- build.version=1.2.0，build.commit=unknown，未伪造 dirty 工作区提交号；没有发布到制品库。

当前交付仅兼容分支本地验证通过；真实启动、存量 MySQL 升级、Redis、供应商和 PC/H5 E2E 门禁仍未执行。

## 最终收尾

- 对同一最终源码再次 `clean test` 独立 verification：
  `/private/tmp/forge-boot4-t1-contracts-release-check.log`，28 项，0 失败 / 0 错误 / 0 跳过，BUILD SUCCESS。
- `git diff --check` 无输出；版本检查仍为 1.2.0；当前分支仍为 `codex/spring-boot4-t0-compatibility`。
- 未更改 main、历史 SQL、前端、ForgePro 和用户无关未提交文件，未进行 Git 提交/推送或部署。
- 尚未扩展原生通义 ReAct 工具绑定，未完成真实模型长推理超时预算及全部 Agent/RAG E2E；
  这些限制与真实 MySQL/Redis/业务验收共同列入后续门禁，不能凭本轮计数宣称生产闭环。
