# 框架验证码安全加固执行记录

> status: in_progress
> baseline-created: 2026-07-26

## 2026-07-26 Proposal

- 分支：`sdd/framework-hardening-phased`。
- 变更范围：阶段一仅处理验证码响应回显、敏感日志、模拟短信发送和手机号校验键不一致。
- Research：确认 `forge-starter-message` 已提供 `MessageClient` 与 `SmsMessageChannel`，`forge-plugin-system` 同时依赖 Auth 与 Message，适合作为发送 SPI 的组合实现层。
- 现有工作区：保留用户未提交的 `CaptchaServiceImpl` 两个 import 和新增 `CaptchaProperties.java`，未回退、未覆盖。
- 本轮操作：只创建 SDD 文档，未修改生产代码，未执行构建或测试。
- 文档校验：已执行新建文档空白检查与占位符扫描，未发现尾随空格、冲突标记或待实现占位标记。
- 服务：未启动任何后端、前端、数据库或 Redis 进程；新增 PID：无。
- HARD-GATE：等待用户确认完整 Spec 后进入 `/apply framework-captcha-security-hardening`。

## 2026-07-26 HARD-GATE 确认

- 用户于 `2026-07-26 15:29:28 CST` 回复“开始”，明确批准阶段一 Spec。
- Spec 状态已由 `propose` 更新为 `apply`，Tasks/Test Spec 状态同步进入实施阶段。

## 2026-07-26 15:34 CST 实施前测试基线

- 变更范围：进入 Task 1 前，仅验证 Auth Starter 及其 Reactor 上游现有测试，尚未实施验证码行为改动。
- 命令：`JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-auth -am test`。
- 结果：`BUILD SUCCESS`，Auth Starter 现有 2 个测试类共 7 条用例通过，0 失败、0 错误、0 跳过。
- 上游证据：`forge-starter-outbound` 48 条、`forge-flow-client` 11 条用例通过，均为 0 失败、0 错误、0 跳过。
- 警告：`SliderCaptchaResult`、`SmsCaptchaResult` 存在既有 Lombok `@Builder` 默认值警告；不阻断本阶段基线，Task 1 不扩大处理范围。
- 服务：未启动后端、前端、数据库或 Redis；新增 PID：无。

## 2026-07-26 15:36 CST Task 1 响应安全契约

- 变更范围：为图形/短信响应的 `code` 字段增加 `NON_NULL` 序列化规则，补充默认关闭的 `CaptchaProperties.devEchoCode`，新增 Auth Starter 内部 `SmsCaptchaSender` SPI。
- Red 命令：`mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-auth -am test -Dtest=CaptchaResponseSerializationTest -Dsurefire.failIfNoSpecifiedTests=false`。
- Red 结果：目标测试 3 条实际执行，2 条失败；失败 JSON 分别包含 `\"code\":null`，符合预期失败原因。
- Green 命令：同一目标测试命令；结果 `BUILD SUCCESS`，3 条通过，0 失败、0 错误、0 跳过。
- 编译命令：`mvn -pl forge-framework/forge-starter-parent/forge-starter-auth -am compile -DskipTests`；结果 `BUILD SUCCESS`。
- 静态检查：`git diff --check` 返回 0。
- 警告：仍只有基线中已记录的 Lombok Builder 默认值警告，本任务未引入新警告。
- 服务：未启动任何服务；新增 PID：无。

## 2026-07-26 15:43 CST Task 2 验证码生命周期与日志

- 变更范围：增加 Profile + 显式开关双门禁；短信真实发送失败关闭；验证码缓存失败回滚；发送成功后才创建 60 秒间隔键；移除答案、输入/缓存值、滑块坐标和完整手机号日志。
- Red 命令：`mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-auth -am test -Dtest=CaptchaServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`。
- Red 结果：`testCompile` 按预期失败，现有 `CaptchaServiceImpl` 只有单参数构造器，尚无配置、Profile 和 Sender 边界。
- 目标 Green：`CaptchaServiceImplTest` 10 条通过，覆盖默认/dev/prod 回显、Sender 缺失/失败/异常/成功、缓存回滚、开发模拟和短信一次性校验。
- 全量命令：`mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-auth -am test`；结果 `BUILD SUCCESS`，Auth Starter 20 条通过，0 失败、0 错误、0 跳过。
- 上游结果：Outbound 48 条、Flow Client 11 条通过，均 0 失败、0 错误、0 跳过。
- 日志检查：对 Auth Starter 生产 Java 扫描 `codeStr/cacheCode/correctX/moveX/expectedX/code` 的日志参数，结果无匹配；全部 6 处手机号日志均显式调用 `SensitiveDataUtil.maskPhone`。
- 异常日志：保留原始堆栈位置和异常类型，但以固定安全消息替换可能携带验证码/手机号的第三方异常消息。
- 静态检查：`git diff --check` 返回 0。
- 服务：未启动任何服务；新增 PID：无。
