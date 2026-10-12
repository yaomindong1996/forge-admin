# T2 第一阶段执行记录

## 2026-10-12：启动

- 分支 `codex/spring-boot4-t0-compatibility`，工作区已有 278 条状态记录；保留 T0/T1 和用户无关改动。
- 根 revision=1.2.0，Boot 4.1.1、Spring AI 2.0.1。本轮不是发版，不自动 commit/push/main 合并。
- 读取 AGENTS、编码/自动化测试/发版规范、相关 pitfalls 与 T1 spec/tasks/test-spec/execution-log。
- 核对真实 ReAct：原生模型未获得工具选项，工具结果是普通文本，HITL 重跑且序列化运行时对象。
- 使用 OpenAI Docs 核对官方工具结果调用 ID，另核对 Spring AI 2 与阿里云原生协议；没有 API Key 工具，
  全程只计划本机合成 HTTP，不调用真实模型或外部业务工具。
- Spec 将绑定白名单、确认作用域和当前授权重验明确为本次完整工具链的一部分；现有授权码/默认决策不修改。

后续命令、失败、修复与测试结果在实际执行后追加；不预填通过，不覆盖 T1 历史证据。

## 2026-10-12 07:00～07:35：环境恢复与增量失败修复

- 旧临时 Maven 的 boot/lib 已缺失，`compile1.log` 报 Classworlds 找不到；
  `compile2.log` 再次确认旧 settings 缺失，两次均未进入 Java 编译，不能算代码失败或通过。
- 从 Apache 官方归档获取 Maven 3.9.11 并通过 SHA-512 校验；新目录
  `/private/tmp/forge-boot4-t2-tools.senzpW/apache-maven-3.9.11`，不覆盖其他工程工具或全局配置。
- Java 17 使用已有 `/private/tmp/forgepro-build-tools.5kNsIl/jdk-17.0.20.1+1/Contents/Home`。
  生产源码 `compile3.log` BUILD SUCCESS。日志完整前缀均为 `/private/tmp/forge-boot4-t2-`。
- `tools-tests1.log`：21 项、2 失败。恢复 fixture 没有像真实 factory 一样装配 toolGroupMode，
  Mockito `anyString` 不匹配 null，导致决策为空被正确拒绝；补完整 fixture 后 `tools-tests2.log` 21 项全通过。
- `tools-tests3.log`：50 项、2 错误。重配 Mockito 答案时 `when` 调用了先前已设置的异常/身份断言；
  改成 `doThrow/doReturn`，没有取消身份断言或忽略错误。
- 一次未加引号的 `-Dtest=Agent*Test,...` 被 zsh glob 拒绝，未进入 Maven；以后显式引用该参数。
- `tools-tests4.log`：57 项、1 失败。非法 ID 的无 Redis 读取断言包含 fixture 初始化调用；
  在 fixture 构造末尾 clearInvocations，只清测试初始化计数，不放宽生产校验。
- `tools-tests5.log` 59 项、`tools-tests6.log` 61 项全通过，0 失败/错误/跳过。
- 新增暂停/确认、错误和长度结束负面用例；依赖库会记录预期的 MessageAggregator 错误、
  取消时 Request cancelled/onErrorDropped，以及既有 BeanPostProcessor 警告，不把这些预期负面日志当成成功隐去。

## 2026-10-12 07:37～07:44：服务回归与最终接线修复

- `services-regression1.log`：四服务 clean package，51 模块 BUILD SUCCESS，143 类/624 项全通过。
- 复查发现初始请求在会话占用前建行，可覆盖另一个活跃请求的持久化 map；
  将建行纳入原子占用后的请求线程回调，补竞争/回调异常清理断言。
  `services-regression2.log`：再次 clean package，51 模块，626 项全通过，耗时 01:09。
- 复查 HTTP 工具发现新增 `agent-http` 不在真实策略 SUPPORTED 中；改用既有
  `OutboundScenes.EXTERNAL_CONNECTOR` 与当前白名单，不创建场景/授权 SQL。
  加入真实 DefaultOutboundPolicyService 接线测试，覆盖授权公网、缺白名单和私网拒绝。
- `services-regression3.log`：最终 package，51 模块、147 类/665 项，失败/错误/跳过均为 0，
  BUILD SUCCESS，耗时 01:00，完成于 07:44:39；AI 143 项、出站 48 项、租户基础 6 项。
- `independent-contracts1.log`：复用 T1 独立真实源码 H2/HTTP 工程，5 类/28 项，全通过，
  完成于 07:39:02。采购流程 8 项、JSON 变量 1 项、AI 协议 19 项；有重叠，不叠加为独立用例数。
- 所有计数取自每个测试类的 Surefire 行，未把模块汇总行再次相加。
- 未新增测试排除；未读取/执行真实 dev 数据源，未创建业务服务 PID；测试 fixture 自动关闭。

## 可复跑命令

以下在仓库根目录、Java 17/Maven 环境执行。临时工具目录不作为项目配置或发行依赖。

```bash
FORGE_T2_TESTS='JacksonHttpContractTest,RequestBodyLimitFilterTest,SaTokenServletContextConfigurationTest,'
FORGE_T2_TESTS+='SaTokenFlowTokenProviderTest,ManagedCacheCodecTest,RedissonServerInfoCompatibilityTest,'
FORGE_T2_TESTS+='Plugin*Test,RuntimeLicenseAutoConfigurationTest,ReplayAttackFilterTest,MachineJsonProtocolTest,'
FORGE_T2_TESTS+='InternalCallRequestVerifierTest,OpenAiCompatibleProviderAdapterTest,DashScopeNativeProviderAdapterTest,'
FORGE_T2_TESTS+='AiProviderHttpContractTest,OpenAiMultimodalHttpContractTest,AiProvider*Test,AiModelFailureClassifierTest,'
FORGE_T2_TESTS+='AiClientImplTest,AiInvocationResolverTest,BpmnXmlUtilsTest,DynamicFormArrayPermissionValidatorTest,'
FORGE_T2_TESTS+='Flow*Test,Mcp*Test,SystemVersionControllerTest,RuntimeLicenseStatusSecurityTest,RemoteJobFlowExecutorTest,'
FORGE_T2_TESTS+='SysRoleAccessPolicyTest,SysUserRelationCoordinatorTest,FlywayClasspathConfigurationTest,'
FORGE_T2_TESTS+='JobFlowControllerSecurityContractTest,SecureActionCatalogServiceTest,Agent*Test,'
FORGE_T2_TESTS+='DashScopeAgentToolIntegrationTest,DefaultTenantLineHandlerTest,TenantInterceptorWorkspaceTest,'
FORGE_T2_TESTS+='OkHttpSecureOutboundClientTest,Outbound*Test,DefaultOutboundPolicyServiceTest,'
FORGE_T2_TESTS+='OutboundWhitelistServiceTest,IpAddressClassifierTest'

mvn -B -ntp -f forge-server/pom.xml \
  -pl forge-admin-server,forge-app-server,forge-flow/forge-flow-server,forge-report-server \
  -am -Pdev,enable-tests clean package \
  -Dtest="$FORGE_T2_TESTS" -Dsurefire.failIfNoSpecifiedTests=false -Dstyle.color=never
```

最终扩展测试实际使用 package；此前两次已做 clean package。无指定测试的依赖模块可以继续，
但任何已选测试的失败仍会中止 Maven。根 POM 原有排除保持不变。

```bash
mvn -B -ntp -f code-copilot/changes/spring-boot4-t1/verification/pom.xml \
  -Penable-tests clean test -Dstyle.color=never

node scripts/forge-shared/version-cli.mjs --check
git diff --check
```

版本检查仍为 1.2.0，日常开发不创建发布版本/标签。实际分支仍为
`codex/spring-boot4-t0-compatibility`；main、ForgePro、历史 SQL、前端及无关改动未修改。
未 Git 提交、推送、上线或发布制品；四服务 JAR 仅在原 target 目录生成。

## 最终产物 / 差异核对

- 只读检查四个 JAR 的目录、构建信息与相关依赖，没有读取/输出 application 数据源或密钥配置。
- 初次按旧预期的 `BOOT-INF/classes/META-INF/build-info.properties` 读取未命中，
  按实际 JAR 目录重新定位为 `META-INF/build-info.properties`；四个产物都有该构建信息，
  build.version=1.2.0、build.commit=unknown，不伪造 dirty 工作区提交标识。
- Admin/Flow/Report 包含本轮 forge-plugin-ai 1.2.0；四服务包含更新后的 core 与 outbound。
  AI JAR 包内已确认 Prompt 工厂、执行器、v2 快照、线程池配置与 RuntimeFactory 新类型。
- Boot 4.1.1 / AI 2.0.1 / Flowable 8.0.0 仍保持 T1 基线；没有因工具开发再次改发行版本或 SDK。
- 新增 Java 文件均小于 1000 行、行长不超过 120；存量 persister 仍有历史长行，未增加该类长行数量，
  不宣称做过全量存量风格重构。
- `git diff --check` 无输出；历史 SQL/前端差异为空；HEAD 保持
  `719a2fc0b28d929edef62769dc2adc5e75ef557e`，当前兼容分支没有提交或切换。
- 复查发现的注册桥、图像模型选择与旁路事件订阅授权缺口已明确记入交付报告，
  不用“工具链通过”代替这些尚未实现的业务与安全边界。
