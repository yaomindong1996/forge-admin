# 单测 Spec — 框架验证码安全加固
> status: apply
> created: 2026-07-26

## 0. 测试原则

- **Red/Green TDD**：每个 Task 的新增测试先在旧实现上失败，再实施最小修复并确认通过。
- **First Run the Tests**：进入 `/apply` 后先运行 Auth Starter 现有测试，记录初始基线。
- **增量优先**：只扩展 Auth Starter、System 插件、Admin 装配和登录页构建，不默认执行全仓所有测试。
- **安全证据**：除单测外必须执行验证码敏感日志静态扫描，真实短信通道未配置时不得以模拟成功代替。

## 1. 测试框架

| 项目 | 值 |
|------|-----|
| JUnit 版本 | JUnit Jupiter 5，由 Spring Boot 3.2 BOM 管理 |
| Mock 框架 | Mockito + AssertJ |
| 已有测试数量 | Auth Starter 当前 2 个测试类；执行时记录实际用例数 |
| 已有测试风格 | 直接构造被测对象、Mockito mock、AssertJ 断言，不启动 Spring Context |

## 2. 覆盖范围

### P0 — 核心安全逻辑（必须覆盖）

#### 类名: `CaptchaServiceImpl`

| 方法 | 场景 | 输入 | Mock 行为 | 预期结果 |
|------|------|------|-----------|---------|
| `generateGraphicCaptcha` | 默认配置 | `devEchoCode=false` | 缓存正常 | `code=null`，图片和 key 正常 |
| `generateGraphicCaptcha` | 开发回显 | `dev/local + true` | 缓存正常 | 返回实际验证码 |
| `generateGraphicCaptcha` | 生产误配 | `prod + true` | 缓存正常 | 仍为 `code=null` |
| `sendSmsCaptcha` | 无真实 Sender | `prod` | Sender 列表为空 | `status=fail`，不保留验证码和 interval |
| `sendSmsCaptcha` | Sender 成功 | `prod` | 返回 true | 缓存验证码和 interval，响应不含 code |
| `sendSmsCaptcha` | Sender 失败 | `prod` | 返回 false | 删除验证码，不创建 interval，返回 fail |
| `sendSmsCaptcha` | Sender 异常 | `prod` | 抛异常 | 脱敏日志，删除验证码，返回 fail |
| `sendSmsCaptcha` | 开发模拟 | `dev + true` | 不调用 Sender | 缓存并回显验证码，返回 success |
| `validateAndDeleteSmsCaptcha` | 正确验证码 | 缓存匹配 | 返回缓存值 | 返回 true 并删除短信 key |
| `validateAndDeleteSmsCaptcha` | 错误验证码 | 缓存不匹配 | 返回缓存值 | 返回 false 且不删除正确答案 |

#### 类名: `CaptchaResponseSerializationTest`

| 场景 | 输入 | 预期结果 |
|------|------|----------|
| 图形 code 为 null | `CaptchaResult` | JSON 不含 `code` 属性 |
| 短信 code 为 null | `SmsCaptchaResult` | JSON 不含 `code` 属性 |
| 开发 code 非空 | 两个 DTO | JSON 正常包含 `code` |

### P1 — 通道与认证集成

#### 类名: `MessageSmsCaptchaSender`

| 场景 | Mock 行为 | 预期结果 |
|------|-----------|----------|
| MessageClient 成功 | `SendResult.ok` | SMS 渠道、单手机号、验证码参数和有效期正确，返回 true |
| MessageClient 失败 | `SendResult.fail` | 返回 false，不抛供应商错误 |
| MessageClient 异常 | 抛 RuntimeException | 返回 false，日志手机号脱敏且不含验证码 |

#### 类名: `UserLoadServiceImpl`

| 方法 | 场景 | 预期结果 |
|------|------|----------|
| `validatePhoneCode` | 手机号登录 | 精确调用 `validateAndDeleteSmsCaptcha(phone, code)` |

### P2 — 入口与构建

- `application.yml` 默认 `FORGE_CAPTCHA_DEV_ECHO_CODE=false`。
- `forge-admin-ui` 不读取或记录响应 `data.code`。
- Admin Reactor package 验证 Auth、System、Message 自动配置可共同装配。
- 前端生产构建验证登录页修改无类型或模板错误。

### 不测试（明确原因）

- 不调用真实短信供应商：本地没有可提交的 AK/SK，真实发送由用户侧配置环境验收。
- 不新增浏览器 E2E：本阶段只删除控制台副作用，页面交互和布局不变；生产构建足以覆盖前端差异。
- 不测试 Redis 并发原子性：沿用现有 `ICacheService` 合同，本阶段未改变并发模型。
- 不测试验证码抗暴力次数：不属于本次清单范围，作为后续认证安全专题评估。

## 3. 执行计划

- [x] Step 1: 运行 Auth Starter 现有测试套件，记录基线。
- [x] Step 2: 编写 DTO 与 Captcha Service P0 测试，确认 Red → Green。
- [ ] Step 3: 编写 Message Sender 和手机号校验 P1 测试，确认 Red → Green。
- [ ] Step 4: 运行 Auth + System 聚合测试与 Admin package。
- [ ] Step 5: 运行前端 build、敏感日志扫描和 `git diff --check`。

## 4. 历史验证基线

| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-07-26 | Proposal 文档 | 未执行代码测试 | 待 `/apply` | HARD-GATE 前未修改生产代码 |
| 2026-07-26 15:34 CST | Auth Starter 实施前基线 | `mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-auth -am test` | 通过 | Auth 7 条、上游 Outbound 48 条、Flow Client 11 条，均 0 失败/错误/跳过 |

## 5. 本轮增量验证

| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-07-26 | Task 1-4 | Auth/System 测试、Admin package、UI build、静态扫描 | 已完成实施前 Auth 基线，其余见 `tasks.md` | 进行中 | 真实短信 E2E 依赖外部配置 |

## 6. 执行证据

- `execution-log.md`：实施时逐 Task 追加完整命令和关键输出。
- 关键接口：`GET /auth/captcha`、`POST /auth/captcha/sms`、手机号验证码登录。
- 关键数据库检查：无数据库变更；真实短信配置仅做用户侧验收。
- 服务启动与停止：默认不启动服务；如为接口验证启动 Admin，必须记录 PID 并仅停止本轮进程。
