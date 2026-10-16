# T6 执行记录

## 2026-10-12：开始

- 仍在 codex/spring-boot4-t0-compatibility；工作区 397 项已有脏记录均保留，HEAD 不变。
- 已读 AGENTS、code-copilot 规则/偏好/相关踩坑、自动化标准、发版规则和 T5 四份验证文档/报告。
- 核查实际源码，确认异步 ThreadLocal 尾批次缺刷新、未捕获可信租户/随机 ID、
  工具无结果标成功和旁路确认/背压失败结果被忽略。
- 本轮只后端生命周期纠错，不触发无关 CRUD/项目初始化/流程设计 Skill。
- 版本 1.2.0 / Unreleased；不提交、push、main 合并、发版或真实环境操作。

实际命令、失败与修复、通过结果及清理情况后续追加，不预填通过。

## 定向验证与初次全量

- target1.log：8 范围中的首批 7 类 / 50 项通过，16:30:50 完成，22.252 秒。
- target2.log：补真实 Spring @EnableAsync 装配及并发 SSE 后，8 类 / 52 项通过，
  16:34:40 完成，21.526 秒；0 失败/错误/跳过。
- clean1.log：出站组件的 6 项 MockWebServer 用例被沙箱禁止本地端口绑定，
  `SocketException: Operation not permitted`，Reactor 在 outbound 停止，不能记为全量通过。
  不排除测试、不改客户端安全策略；仅申请隔离测试的 loopback 端口权限后重新运行。
- Java 编译器 AST 检查发现持久化器 4 行旧长行，已仅换行收敛；没有扩大业务变更范围。
- 日志前缀为 `/private/tmp/forge-boot4-t6-`；新增测试故意注入数据库/模型失败的 ERROR 日志
  属于负面断言夹具，不等于测试失败；真实环境仍未调用。

## 最终全量与质量审查

- clean2.log：53 模块 clean package 成功，16:43:24 完成，4:23。
  796 份 Surefire XML 实际汇总：3,493 tests、0 failures、0 errors、0 skipped。
  AI 338 / 系统 302；新增 29 项，定向复跑不重复累计。
- Spring @EnableAsync 装配仍在同一可信线程执行审计；真实有界 loop 池、断连、
  失去租约、线程复用隔离以及 SQL 失败不重放均经过断言。
- 实际 AgentEventMapper XML + H2 验证明确列/tenant/Long/时间/父事件、
  多值 INSERT 的批次原子性与参数绑定；没有新增迁移或改历史 SQL。
- shape2.log：使用 JDK JavacTask 解析 6 个生产 Java 文件，文件 19–490 行，
  方法 ≤80、参数 ≤5、行 ≤120；状态枚举、构造器注入、锁序、缓冲边界与日志安全自查完成。
- 版本检查通过 1.2.0；git diff --check 通过。前端本轮无改动，未重跑 PC/H5/Report。
  复用 T5 PC 2,353 项证据而不宣称本轮新跑；其它端参考 T3 历史结果。
- 既有 Jackson 2、MCP SSE 兼容 API 的弃用/unchecked 编译警告不阻断本次构建，
  未通过关闭警告、删除测试或移除既有兼容层掩盖它们。
- 四个 JAR build-info 实际读取：version=1.2.0；artifact 分别为
  forge-admin-server / forge-app-server / forge-flow-server / forge-report-server；
  time=2026-10-12T08:39:00.775Z，commit=unknown。Admin JAR 不包含新增 H2 测试依赖。
  当前工作区未提交，不将这些验证包称为正式发布产物。
- 所有测试/构建会话已完成。没有启动真实服务或浏览器；测试内线程池、Spring 上下文、H2
  与 loopback 模拟服务在用例结束时关闭，无需停止用户现有进程。
- 分支/HEAD 仍为 codex/spring-boot4-t0-compatibility / 719a2fc0b28d929edef62769dc2adc5e75ef557e；
  main、ForgePro、其它工作树与无关用户文件未操作，未提交/push/打标签/发布/部署。

## 可复跑命令

在仓库根运行，工具路径沿用 T5；只有隔离 MockWebServer/loopback 测试需要本地端口权限：

```bash
JAVA_HOME=/private/tmp/forgepro-build-tools.5kNsIl/jdk-17.0.20.1+1/Contents/Home \
MAVEN_SKIP_RC=true \
JAVA_TOOL_OPTIONS=-javaagent:/Users/mini32g/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.11/byte-buddy-agent-1.18.11.jar \
/private/tmp/forge-boot4-t2-tools.senzpW/apache-maven-3.9.11/bin/mvn \
  -B -ntp -f forge-server/pom.xml -Pdev,enable-tests clean package -Dstyle.color=never

node scripts/forge-shared/version-cli.mjs --check
git diff --check
```

定向测试在同一 Maven 前缀后改为 `test -pl forge-framework/forge-plugin-parent/forge-plugin-ai -am`，
加 `-Dtest=AgentEventPersistenceTest,AgentEventMapperContractTest,AgentEventPipelineTest,AgentEventPersistenceAssemblyTest,AgentEventWebFluxStreamTest,AgentChatPersisterResumeTest,ReactLoopTerminalTest,AgentSessionLeaseRegistryTest`
与 `-Dsurefire.failIfNoSpecifiedTests=false`；这不关闭其它阶段的完整回归。

发版判定：日常开发；版本保持 1.2.0；CHANGELOG 归属 Unreleased；
提交、推送、正式标签、制品发布、生产迁移、部署均未执行。剩余边界见 compatibility-report。

## 2026-10-12：按用户要求停止扩大升级范围

- 用户明确“保证功能都正常，不用新加功能”；撤销此前将增强项列为必做 T7–T9 的建议。
- Spec/Tasks/Report 区分原功能兼容回归、未实现的可选增强与真实环境门禁。
  已完成代码和测试保留，不新增总线、恢复机制或授权体系，不将排除项标成已完成。
- 本次仅记录范围与偏好，未修改生产代码或重新构建；原 3,493 项后端与 T5 PC 证据仍为历史验证。
- 文档检查执行 git diff --check，真实环境、main、提交/push/部署仍不操作。
- 交付口径：本地覆盖范围内回归通过；真实环境未验证，不承诺生产全部功能已经正常。

## 2026-10-12：准备按用户授权提交当前分支

- 用户要求 push 当前分支供其自行测试；只操作 origin 的 codex/spring-boot4-t0-compatibility。
- 核实 origin 为 git@gitee.com:ForgeLab/forge-admin.git；只读 ls-remote 确认当前分支尚不存在。
  初次沙箱 DNS 失败，在获准连接既有 Gitee 远端后检查成功；不关闭 SSH 主机校验、不强推。
- 提交范围为 T0–T6；.DS_Store、multicolor-navigation-icons 四份文档、production-runbook.md 保留在本地。
  只纳入源文件和回归文档，忽略本地配置、target、dist、日志与临时验证图片。
- 复用最近 clean2 的 53 模块 / 3,493 项后端和 T5 的 PC 2,353 项及构建证据；
  此后只有本轮范围说明/交付记录变化，不为提交重复全量测试。
- pnpm check:edition 通过（工作区 10,237 个可提交文件、原索引 10,072 个）；
  version check 通过 1.2.0，SFC 规模核对未超 1000 行，git diff --check 通过。
- 本次为源码测试交付，不正式发版、不升版、不创建标签、不发布制品或部署。
  提交号、推送结果与最终遗留文件在实际执行后回填，不预先宣称成功。

## 当前分支代码提交与 push 结果

- 提交 7ce8974ecf98a0072e2b03cf3e66f6c03e54275e，说明：
  `[spring-boot4] 完成底座升级及现有功能兼容回归`；491 文件，16,146 行增加 / 2,448 行删除。
- 暂存后开源边界再次通过（索引/工作区均 10,237 个可提交文件）；版本保持 1.2.0。
  暂存内容 diff --check、禁入文件路径及明显密钥格式筛查通过，不代替正式安全审查。
- `git push -u origin HEAD:refs/heads/codex/spring-boot4-t0-compatibility` 成功，
  远端新分支建立，本地开始跟踪同名 origin 分支；没有 force、main 写入或其它项目远端操作。
- 提交后工作区仅剩 6 个原有无关文件：.DS_Store、multicolor-navigation-icons 的
  execution-log/spec/tasks/test-spec、forge-server/scripts/deployment/production-runbook.md。
- 当前回填仅文档，单独提交并推送，不重编译、不将旧验证包标为新提交的正式发行包。
- 最终发布状态：源码已提交并 push 当前测试分支；版本 1.2.0 / Unreleased；
  正式标签、制品发布、真实迁移、生产部署、main 合并均未执行。由用户继续真实业务测试。
