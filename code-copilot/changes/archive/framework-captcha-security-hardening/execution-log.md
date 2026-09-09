# 框架验证码安全加固执行记录

> status: complete
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

## 2026-07-26 15:50 CST Task 3 真实短信通道与统一校验

- 变更范围：在 System 插件增加 `MessageSmsCaptchaSender`，通过 `MessageClient` 向 SMS 通道发送单手机号验证码；手机号登录统一调用 `validateAndDeleteSmsCaptcha`，复用 `captcha:sms:<phone>` 一次性校验协议。
- Red 命令：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test -Dtest=MessageSmsCaptchaSenderTest,UserLoadServiceImplCaptchaTest -Dsurefire.failIfNoSpecifiedTests=false`。
- Red 结果：Reactor 在进入 System 模块前被既有 `forge-starter-datascope` Surefire 配置截断，错误为 `groups/excludedGroups require TestNG, JUnit48+ or JUnit 5`；未将该上游问题误记为目标用例预期失败，也未越界修改 datascope。
- 依赖准备：`mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am install -DskipTests` 执行成功，25 个 Reactor 模块全部 `BUILD SUCCESS`。
- 目标测试：从 `forge-server` Reactor 根目录执行 `mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system test -Dtest=MessageSmsCaptchaSenderTest,UserLoadServiceImplCaptchaTest`，结果 `BUILD SUCCESS`，4 条通过，0 失败、0 错误、0 跳过。
- System 完整测试：`mvn -Penable-tests -pl forge-framework/forge-plugin-parent/forge-plugin-system test`，结果 `BUILD SUCCESS`，当前模块全部 4 条测试通过，主代码和测试代码编译成功。
- 安全检查：异常路径日志只记录 `SensitiveDataUtil.maskPhone(phone)`、异常类型和固定消息 `SMS_CHANNEL_OPERATION_FAILED` 的安全化堆栈，不输出验证码、完整手机号或供应商原始异常消息；`git diff --check` 返回 0。
- 跳过项：未调用真实短信供应商，本地没有可提交的供应商凭据；适配协议由 `MessageClient` Stub 覆盖，真实通道 E2E 留给用户侧配置环境验收。
- 服务：未启动任何后端、前端、数据库或 Redis；新增 PID：无。

## 2026-07-26 15:56 CST Task 4 阶段收尾与两阶段审查

- 变更范围：Admin 显式映射 `FORGE_CAPTCHA_DEV_ECHO_CODE` 且默认 `false`；登录页删除图形和短信验证码答案的控制台输出；业务提示、图片、`codeKey` 和倒计时逻辑保持不变。
- 聚合测试命令：`mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-auth,forge-framework/forge-plugin-parent/forge-plugin-system -am test`。Auth 20 条、Outbound 48 条、Flow Client 11 条及 Excel/File 相关上游测试通过；随后在第 20/25 个模块 `forge-starter-datascope` 因既有 `groups/excludedGroups require TestNG, JUnit48+ or JUnit 5` 失败，System 在该命令中被跳过。System 本阶段已有独立 4 条完整测试通过证据，未修改无关 datascope。
- Admin 聚合命令：`mvn -pl forge-admin-server -am package -DskipTests`，结果 `BUILD SUCCESS`，43 个 Reactor 模块全部打包成功，包含 Auth、Message、System 和 Admin。
- 前端命令：Node `v20.19.0` 下执行 `NODE_OPTIONS=--max-old-space-size=8192 pnpm build`，结果成功，8725 个模块完成转换，耗时 1 分 44 秒。
- 前端警告：保留既有 `UserSelectModal` 组件命名冲突、动态/静态 import 混用、CSS `//` 注释和分块提示；本次只删除登录页控制台副作用，警告与本轮差异无关且不阻断构建。
- 配置校验：使用 Ruby YAML 解析 `forge-admin-server/target/classes/application.yml`，确认 `forge.captcha.dev-echo-code` 为 `${FORGE_CAPTCHA_DEV_ECHO_CODE:false}`；Ruby 同时提示 `/opt/homebrew/opt` 目录权限警告，不影响解析结果。
- 安全扫描：初版宽泛规则命中验证码 key、已脱敏手机号日志和 `expectedX` 局部变量，未将其误判为泄露；改用 `rg --pcre2` 精确扫描 `res.data.code`（排除 `codeKey`）、验证码 `console.warn` 以及日志中的 `codeStr/cacheCode/correctX/moveX/expectedX`，结果无匹配。
- 静态检查：`git diff --check` 返回 0；打包产物目录未进入版本控制差异。
- 阶段一 Spec Compliance：PASS，9 个功能点全部有代码和测试/静态证据。
- 阶段二 Code Quality：PASS_WITH_COMMENTS，未发现阻塞问题；保留项为真实短信供应商 E2E 未执行，以及全 Reactor 测试受既有 datascope 测试引擎配置阻断。
- 服务：未启动任何后端、前端、数据库、Redis 或浏览器进程；新增 PID：无，无需清理服务。
