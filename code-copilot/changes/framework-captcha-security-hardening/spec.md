# 框架验证码安全加固
> status: apply
> created: 2026-07-26
> complexity: 🟡中等

## 1. 背景与目标

当前认证框架会在图形验证码和短信验证码响应中回显明文验证码，同时在服务端和浏览器控制台记录验证码；短信发送仍由始终成功的模拟方法完成，手机号验证码登录还存在发送与校验缓存键不一致的问题。

本变更是框架问题整改路线的第一阶段，只处理验证码链路，形成以下可验证结果：

1. 默认配置及 `prod` 环境下，所有验证码响应都不包含 `code` 字段。
2. 服务端和管理端浏览器日志均不记录图形、滑块或短信验证码答案。
3. 短信验证码通过现有 `forge-starter-message` 通道发送；通道不可用时失败关闭，不写入可登录的验证码缓存，也不返回成功。
4. 仅在 `dev`/`local` Profile 且显式设置 `FORGE_CAPTCHA_DEV_ECHO_CODE=true` 时，允许本地模拟发送并回显验证码。
5. 手机号验证码登录和“用户名密码 + 短信验证码”统一使用 `captcha:sms:<phone>` 缓存键。

后续阶段按独立 SDD 变更推进，不纳入本 Spec：

- 第二阶段：框架加密密钥生命周期与存量密文轮换。
- 第三阶段：Controller 分层、数据权限、异常、分页和流程清理策略。
- 第四阶段：前端业务字典与设计器常量治理。

## 2. 代码现状（Research Findings）

### 2.1 相关入口与链路

- 匿名发送入口：`forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/controller/AuthController.java#sendSmsCaptcha` 暴露 `POST /auth/captcha/sms`。
- 图形验证码入口：`AuthController#getCaptcha` 调用 `IAuthService#getCaptcha`，最终进入 `CaptchaServiceImpl#generateGraphicCaptcha`。
- 短信发送链路：`SystemAuthServiceImpl#sendSmsCaptcha` → `CaptchaServiceImpl#sendSmsCaptcha`。
- 用户名密码短信校验：`UsernamePasswordCaptchaAuthStrategy#validateCaptcha` → `ICaptchaService#validateAndDeleteSmsCaptcha`。
- 手机号验证码登录：`PhoneCaptchaAuthStrategy#doAuthenticate` → `UserLoadServiceImpl#validatePhoneCode`。
- 真实短信基础设施：`forge-server/forge-framework/forge-starter-parent/forge-starter-message/src/main/java/com/mdframe/forge/starter/message/sdk/MessageClient.java#send` → `forge-server/forge-framework/forge-starter-parent/forge-starter-message/src/main/java/com/mdframe/forge/starter/message/channel/SmsMessageChannel.java#send` → SMS4J。

### 2.2 现有实现

本节涉及的实现文件为：

- `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/service/impl/CaptchaServiceImpl.java`
- `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java`
- `forge-admin-ui/src/views/login/index.vue`

- `CaptchaServiceImpl#generateCaptcha` 在 DEBUG 日志打印 `code`。
- `CaptchaServiceImpl#validate` 在 DEBUG 日志同时打印输入值和缓存值。
- `CaptchaServiceImpl#generateGraphicCaptcha` 无环境判断地设置 `CaptchaResult.code`，并在日志打印答案。
- `CaptchaServiceImpl#validateSliderCaptcha` 在日志打印 `moveX` 与 `expectedX`。
- `CaptchaServiceImpl#sendSmsCaptcha` 先写 `captcha:sms:<phone>`，再调用始终返回 `true` 的 `mockSendSms`，响应始终设置 `SmsCaptchaResult.code`。
- `CaptchaServiceImpl#validateSmsCaptcha` 在 DEBUG 日志打印输入值和缓存值。
- `UserLoadServiceImpl#validatePhoneCode` 调用通用图形验证码接口，最终读取 `captcha:phone_code:<phone>`，与短信发送键不一致。
- `forge-admin-ui/src/views/login/index.vue#refreshCaptcha` 和 `#sendSmsCode` 在开发构建中将响应验证码写入浏览器控制台。
- 工作区已有未提交的 `CaptchaProperties` 草稿及两个未使用 import；当前尚未改变运行行为，本变更在其基础上完成。

### 2.3 发现与风险

- 图形验证码默认可从响应直接获得，不能发挥人机校验作用。
- “用户名密码 + 短信验证码”可通过匿名发送接口返回的验证码绕过手机持有校验。
- 纯手机号验证码登录因缓存键不一致而不可用，而不是对外完全放开。
- 模拟发送发生在缓存写入之后，未接真实短信通道也会生成可用验证码。
- 仅依赖一个布尔开关仍可能因生产误配置泄露验证码，因此开发回显必须同时受 Profile 和显式开关约束。
- `forge-starter-auth` 是基础 Starter，直接依赖 `forge-starter-message` 会扩大所有认证使用方的依赖面；应通过小型 SPI 保持依赖方向。

## 3. 功能点

- [ ] 增加验证码开发回显策略：仅 `dev`/`local` 且显式开关为真时生效。
- [ ] `CaptchaResult.code` 与 `SmsCaptchaResult.code` 为 `null` 时不参与 JSON 序列化。
- [ ] 清理所有验证码答案、输入答案、缓存答案、滑块目标位置和完整手机号日志。
- [ ] 定义 `SmsCaptchaSender` SPI，由认证服务负责生成、缓存和校验验证码，由组合层负责真实发送。
- [ ] 在 `forge-plugin-system` 中增加 `MessageClient` 适配器，复用现有 SMS4J 通道。
- [ ] 短信通道发送失败或不可用时删除本轮验证码缓存并返回失败，不设置发送间隔。
- [ ] 开发模拟模式下不调用真实短信通道，返回验证码供本地联调。
- [ ] 统一手机号验证码登录的缓存键和一次性消费语义。
- [ ] 删除管理端登录页的验证码控制台输出。

## 4. 业务规则

1. 生产及默认配置禁止回显验证码；响应 JSON 中必须完全省略 `code`，不能只返回 `null` 或空字符串。
2. `forge.captcha.dev-echo-code=true` 只有在活动 Profile 包含 `dev` 或 `local` 时有效；`prod` 中配置为真也必须忽略。
3. 短信发送前生成六位数字验证码。真实通道成功后保留验证码与 60 秒发送间隔；真实通道失败时删除验证码且不创建间隔键。
4. 开发模拟模式显式视为发送成功，可写入缓存、返回验证码并启动倒计时，但日志仍不得打印验证码。
5. 手机号日志统一使用 `SensitiveDataUtil.maskPhone`；异常日志保留堆栈但不得包含验证码或完整手机号。
6. 短信验证码只在校验成功后删除；错误验证码不能删除正确答案，保持现有兼容语义。
7. 图形、滑块和短信现有接口路径、请求字段、成功状态结构保持兼容。

## 5. 数据变更

| 操作 | 表名 | 字段/索引 | 说明 |
|------|------|-----------|------|
| 无 | — | — | 本阶段不修改数据库结构或初始化数据 |

Redis 键协议调整：手机号登录停止读取 `captcha:phone_code:<phone>`，统一读取现有发送键 `captcha:sms:<phone>`。旧键为不可用路径产生的临时数据，无需迁移。

## 6. 接口变更

| 操作 | 接口 | 方法 | 变更内容 |
|------|------|------|----------|
| 兼容收紧 | `/auth/captcha` | GET | 默认及生产响应省略 `data.code`；图片、`codeKey`、有效期不变 |
| 兼容收紧 | `/auth/captcha/sms` | POST | 默认及生产响应省略 `data.code`；无可用短信通道时返回 `status=fail` |
| 内部修复 | 登录接口手机号验证码模式 | POST | 校验键统一为 `captcha:sms:<phone>` |

前端目前仅在控制台读取 `data.code`，登录业务不依赖该字段，因此协议收紧不会影响正常图形验证码输入和短信倒计时。

## 7. 影响范围

- `forge-starter-auth`：配置、DTO 序列化、验证码服务、短信发送 SPI、单元测试。
- `forge-plugin-system`：短信发送适配器、手机号验证码校验、单元测试及测试依赖。
- `forge-starter-message`：只复用公开 `MessageClient`/`MessageChannel` API，不修改其实现。
- `forge-admin-server`：增加显式环境变量映射，默认关闭开发回显。
- `forge-admin-ui`：删除两处验证码控制台输出。
- 不涉及数据库、Flow 服务、Report 服务和 H5 登录协议改造。

## 8. 风险与关注点

- **安全变更**：这是匿名认证入口和登录第二因子变更，必须验证默认失败关闭、一次性消费和日志脱敏。
- **外部依赖**：真实短信 E2E 依赖已启用的 `sys_sms_config` 和第三方供应商，本地自动化只能通过 Stub 验证适配协议。
- **兼容性**：依赖响应 `code` 自动填充验证码的非正式客户端将失效；这是预期安全收紧，不保留生产兼容开关。
- **发送一致性**：真实供应商已接收但本地缓存写入异常时可能导致用户收到不可用验证码；实现应先暂存验证码，发送失败时回滚缓存，并在异常时返回失败。
- **并发**：沿用现有按手机号 60 秒间隔控制；本阶段不扩展为 IP 级限流或验证码尝试次数限制。

## 8.5 测试策略

- **测试范围**：`CaptchaServiceImpl` 默认/开发/生产回显策略、真实通道成功失败、缓存回滚、一次性校验；响应 DTO JSON；消息适配器；手机号登录键统一；前端生产构建和敏感日志静态扫描。
- **覆盖率目标**：新增与修改的验证码安全分支 100% 场景覆盖，不设仓库总体行覆盖率阈值。
- **独立 Test Spec**：是，见 `test-spec.md`。
- **增量构建**：Auth Starter 测试、System 插件测试、Admin 聚合 package、前端 build。
- **真实短信 E2E**：没有供应商凭据时明确跳过，不把模拟成功当成真实通道通过。

## 9. 待澄清

- 无。采用失败关闭方案：默认必须存在真实短信通道；仅 `dev`/`local` 且显式开关开启时允许模拟回显。

## 10. 技术决策

1. 在 `forge-starter-auth` 定义 `SmsCaptchaSender` SPI，`forge-plugin-system` 提供 `MessageClient` 适配器，避免 Auth Starter 直接依赖消息实现。
2. 使用字段级 `@JsonInclude(NON_NULL)` 省略 `code`，不改变其他响应字段的全局 Jackson 行为。
3. 使用 `Environment.acceptsProfiles(Profiles.of("dev", "local"))` 与 `CaptchaProperties.devEchoCode` 双门禁控制开发回显。
4. 保持现有 `SmsCaptchaResult.status` 协议，通道不可用返回业务失败结果，不在匿名入口暴露供应商错误细节。
5. 先为安全行为建立 Red 测试，再实现最小 Green 代码；每个 Task 通过目标测试和编译后独立提交。

## 11. 执行日志

| Task | 状态 | 实际改动文件 | 备注 |
|------|------|--------------|------|
| Proposal | 完成 | `spec.md`、`tasks.md`、`test-spec.md`、`execution-log.md` | HARD-GATE 已确认，进入 apply |
| Task 1 | 完成 | `CaptchaProperties`、响应 DTO、`SmsCaptchaSender`、序列化测试 | Red 2 条失败；Green 3 条通过，Auth Reactor 编译通过 |
| Task 2 | 完成 | `CaptchaServiceImpl`、`ICaptchaService`、核心行为测试 | 10 条验证码服务测试通过；Auth Starter 共 20 条通过；敏感日志扫描通过 |
| Task 3 | 待执行 | — | 真实短信适配与缓存键统一 |
| Task 4 | 待执行 | — | 前端清理、配置与聚合验证 |

## 12. 审查结论

待 `/apply` 完成后执行 Spec 合规审查和代码质量审查。

## 13. 确认记录（HARD-GATE）

- **确认时间**：2026-07-26 15:29:28 CST
- **确认人**：用户（回复“开始”）
