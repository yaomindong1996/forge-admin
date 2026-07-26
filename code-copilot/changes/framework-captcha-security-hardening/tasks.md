# 框架验证码安全加固 Implementation Plan

> **For agentic workers:** 本计划按仓库 `code-copilot` SDD 工作流内联执行；每个 Task 均先 Red、再 Green、验证后提交。用户确认 HARD-GATE 前不得修改生产代码。

> status: in_progress
> created: 2026-07-26

**Goal:** 消除验证码响应与日志泄露，接入真实短信通道并修复手机号验证码缓存键不一致。

**Architecture:** Auth Starter 负责验证码生命周期并定义 `SmsCaptchaSender` SPI；System 插件作为组合层使用 `MessageClient` 实现该 SPI。开发回显同时受配置开关和 `dev/local` Profile 限制，默认和生产环境均失败关闭。

**Tech Stack:** Java 17、Spring Boot 3.2、JUnit 5、Mockito、AssertJ、SMS4J、Vue 3、Vite。

---

## 前置条件

- [x] 已切换到 `sdd/framework-hardening-phased`，不在默认分支编码。
- [x] 已读取根 `AGENTS.md`、项目记忆、编码规范和自动化测试标准。
- [x] 已确认工作区存在用户未完成的 `CaptchaProperties` 与 import 改动，实施时保留并补全。
- [x] 用户确认 `spec.md` HARD-GATE，状态由 `propose` 更新为 `apply`。

## Task 1: 固化响应契约与开发回显门禁

**Files:**

- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/config/CaptchaProperties.java`
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/CaptchaResult.java`
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/SmsCaptchaResult.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/sms/SmsCaptchaSender.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/test/java/com/mdframe/forge/starter/auth/domain/CaptchaResponseSerializationTest.java`

- [x] **Step 1: 编写响应序列化 Red 测试**

```java
@Test
void shouldOmitNullCaptchaCodeFromJson() throws Exception {
    CaptchaResult result = CaptchaResult.builder().codeKey("key-1").image("image").build();
    assertThat(objectMapper.writeValueAsString(result)).doesNotContain("\"code\"");
}

@Test
void shouldSerializeCaptchaCodeWhenDevelopmentEchoIsAllowed() throws Exception {
    CaptchaResult result = CaptchaResult.builder().code("ABCD").build();
    assertThat(objectMapper.writeValueAsString(result)).contains("\"code\":\"ABCD\"");
}
```

- [x] **Step 2: 运行测试并确认 Red**

```bash
cd forge-server
mvn -Penable-tests \
  -pl forge-framework/forge-starter-parent/forge-starter-auth \
  -am test -Dtest=CaptchaResponseSerializationTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

预期：测试因空 `code` 仍被序列化而失败。

- [x] **Step 3: 实现最小响应契约和发送 SPI**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
private String code;
```

```java
public interface SmsCaptchaSender {
    boolean sendVerificationCode(String phone, String code, Duration duration);
}
```

`CaptchaProperties` 保留 `devEchoCode=false` 默认值，并修正文档为“只有 dev/local Profile 才允许生效”。

- [x] **Step 4: 运行目标测试和编译，确认 Green**

执行 Step 2 命令并补充：

```bash
mvn -pl forge-framework/forge-starter-parent/forge-starter-auth -am compile -DskipTests
```

预期：目标测试与 Reactor 编译均 `BUILD SUCCESS`。

- [x] **Step 5: 更新 SDD 文档并提交**

```bash
git add code-copilot/changes/framework-captcha-security-hardening \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/config/CaptchaProperties.java \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/CaptchaResult.java \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/domain/SmsCaptchaResult.java \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/sms/SmsCaptchaSender.java \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/test/java/com/mdframe/forge/starter/auth/domain/CaptchaResponseSerializationTest.java
git commit -m "[framework-captcha-security-hardening] 固化验证码响应安全契约"
```

## Task 2: 验证码服务失败关闭与敏感日志清理

**Files:**

- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/service/impl/CaptchaServiceImpl.java`
- Modify: `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/service/ICaptchaService.java`
- Create: `forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/test/java/com/mdframe/forge/starter/auth/service/impl/CaptchaServiceImplTest.java`

- [x] **Step 1: 编写核心行为 Red 测试**

测试至少包含：默认图形验证码无 `code`；`dev + devEchoCode=true` 回显；`prod + devEchoCode=true` 仍不回显；无 Sender 时短信失败且不保留验证码；Sender 失败时删除验证码；Sender 成功时缓存并默认不回显；开发模拟时不调用 Sender；短信校验成功后删除。

```java
private final ICacheService cacheService = mock(ICacheService.class);
private final SmsCaptchaSender successSender = (phone, code, duration) -> true;

@Test
void shouldFailClosedWhenSmsSenderIsUnavailable() {
    CaptchaServiceImpl service = service(false, "prod", Optional.empty());
    SmsCaptchaResult result = service.sendSmsCaptcha("13800138000", Duration.ofMinutes(5));

    assertThat(result.getStatus()).isEqualTo("fail");
    assertThat(result.getCode()).isNull();
    verify(cacheService, never()).set(startsWith("captcha:sms:interval:"), any(), any(Duration.class));
}

@Test
void shouldIgnoreDevelopmentEchoInProductionProfile() {
    CaptchaServiceImpl service = service(true, "prod", Optional.of(successSender));
    SmsCaptchaResult result = service.sendSmsCaptcha("13800138000", Duration.ofMinutes(5));
    assertThat(result.getCode()).isNull();
}

private CaptchaServiceImpl service(boolean echoEnabled, String profile,
                                   Optional<SmsCaptchaSender> sender) {
    CaptchaProperties properties = new CaptchaProperties();
    properties.setDevEchoCode(echoEnabled);
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profile);
    return new CaptchaServiceImpl(cacheService, properties, environment, sender);
}
```

- [x] **Step 2: 运行测试并确认 Red**

```bash
cd forge-server
mvn -Penable-tests \
  -pl forge-framework/forge-starter-parent/forge-starter-auth \
  -am test -Dtest=CaptchaServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

预期：现有模拟发送、无 Profile 门禁和无 Sender 行为导致测试失败。

- [x] **Step 3: 实现失败关闭与双门禁**

```java
private boolean isDevelopmentEchoEnabled() {
    return captchaProperties.isDevEchoCode()
            && environment.acceptsProfiles(Profiles.of("dev", "local"));
}
```

发送规则：开发回显模式直接视为本地发送成功；否则必须找到 Sender 并返回成功。发送失败或抛异常时删除本轮 `captcha:sms:<phone>`，返回通用失败信息，不创建 interval 键。

日志规则：只记录 key、布尔结果、脱敏手机号和发送状态；移除验证码、输入答案、缓存答案、滑块目标坐标和移动坐标。

- [x] **Step 4: 运行 Auth Starter 完整测试**

```bash
cd forge-server
mvn -Penable-tests \
  -pl forge-framework/forge-starter-parent/forge-starter-auth \
  -am test
```

预期：Reactor `BUILD SUCCESS`，新增测试全部通过。

- [x] **Step 5: 静态扫描敏感日志并提交**

```bash
rg -n 'code=|cached=|expectedX|验证码.*\{\}|console\.warn.*验证码' \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main \
  forge-admin-ui/src/views/login/index.vue
```

预期：生产代码中无验证码答案日志；前端命中项将在 Task 4 清零并在执行记录中标记。

```bash
git add code-copilot/changes/framework-captcha-security-hardening \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/service/ICaptchaService.java \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main/java/com/mdframe/forge/starter/auth/service/impl/CaptchaServiceImpl.java \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/test/java/com/mdframe/forge/starter/auth/service/impl/CaptchaServiceImplTest.java
git commit -m "[framework-captcha-security-hardening] 加固验证码生命周期与日志"
```

## Task 3: 接入真实短信通道并统一手机号校验

**Files:**

- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/pom.xml`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/auth/MessageSmsCaptchaSender.java`
- Modify: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/auth/MessageSmsCaptchaSenderTest.java`
- Create: `forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImplCaptchaTest.java`

- [ ] **Step 1: 编写真实通道适配和缓存键 Red 测试**

```java
@Test
void shouldSendVerificationCodeThroughSmsChannel() {
    MessageClient messageClient = mock(MessageClient.class);
    MessageSmsCaptchaSender sender = new MessageSmsCaptchaSender(messageClient);
    when(messageClient.send(any())).thenReturn(MessageChannel.SendResult.ok("sms-1"));
    assertThat(sender.sendVerificationCode("13800138000", "123456", Duration.ofMinutes(5))).isTrue();

    ArgumentCaptor<MessageChannel.SendRequest> request = ArgumentCaptor.forClass(MessageChannel.SendRequest.class);
    verify(messageClient).send(request.capture());
    assertThat(request.getValue().getChannel()).isEqualTo("SMS");
    assertThat(request.getValue().getPhoneList()).containsExactly("13800138000");
}

@Test
void shouldUseSmsCaptchaContractForPhoneLogin() {
    ICaptchaService captchaService = mock(ICaptchaService.class);
    UserLoadServiceImpl service = new UserLoadServiceImpl(
            null, null, null, null, null, null, null, null, null,
            captchaService, null, null);
    when(captchaService.validateAndDeleteSmsCaptcha("13800138000", "123456")).thenReturn(true);
    assertThat(service.validatePhoneCode("13800138000", "123456")).isTrue();
    verify(captchaService).validateAndDeleteSmsCaptcha("13800138000", "123456");
}
```

- [ ] **Step 2: 运行 System 插件测试并确认 Red**

```bash
cd forge-server
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-system \
  -am test -Dtest=MessageSmsCaptchaSenderTest,UserLoadServiceImplCaptchaTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

预期：发送适配器不存在、手机号校验仍调用通用验证码接口，测试失败。

- [ ] **Step 3: 实现 MessageClient 适配器与键统一**

先在 `forge-plugin-system/pom.xml` 增加测试依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageSmsCaptchaSender implements SmsCaptchaSender {
    private final MessageClient messageClient;

    @Override
    public boolean sendVerificationCode(String phone, String code, Duration duration) {
        MessageChannel.SendRequest request = new MessageChannel.SendRequest();
        request.setChannel(ChannelType.SMS.name());
        request.setType(ChannelType.SMS.name());
        request.setPhoneList(List.of(phone));
        request.setContent("您的验证码为${code}，${expireMinutes}分钟内有效，请勿泄露。");
        request.setParams(Map.of("code", code, "expireMinutes", duration.toMinutes()));
        try {
            MessageChannel.SendResult result = messageClient.send(request);
            return result != null && result.success;
        } catch (RuntimeException exception) {
            log.warn("短信验证码通道调用失败: phone={}, errorType={}",
                    SensitiveDataUtil.maskPhone(phone), exception.getClass().getSimpleName());
            return false;
        }
    }
}
```

`UserLoadServiceImpl#validatePhoneCode` 改为直接调用：

```java
@Override
public boolean validatePhoneCode(String phone, String code) {
    return captchaService.validateAndDeleteSmsCaptcha(phone, code);
}
```

- [ ] **Step 4: 运行 System 插件完整测试与编译**

```bash
cd forge-server
mvn -Penable-tests \
  -pl forge-framework/forge-plugin-parent/forge-plugin-system \
  -am test
```

预期：Reactor `BUILD SUCCESS`，适配和缓存键测试通过。

- [ ] **Step 5: 更新文档并提交**

```bash
git add code-copilot/changes/framework-captcha-security-hardening \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/pom.xml \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/auth/MessageSmsCaptchaSender.java \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImpl.java \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/auth/MessageSmsCaptchaSenderTest.java \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/test/java/com/mdframe/forge/plugin/system/service/impl/UserLoadServiceImplCaptchaTest.java
git commit -m "[framework-captcha-security-hardening] 接入短信通道并统一验证码校验"
```

## Task 4: 清理前端泄露并完成聚合验证

**Files:**

- Modify: `forge-server/forge-admin-server/src/main/resources/application.yml`
- Modify: `forge-admin-ui/src/views/login/index.vue`
- Modify: `code-copilot/changes/framework-captcha-security-hardening/spec.md`
- Modify: `code-copilot/changes/framework-captcha-security-hardening/tasks.md`
- Modify: `code-copilot/changes/framework-captcha-security-hardening/test-spec.md`
- Modify: `code-copilot/changes/framework-captcha-security-hardening/execution-log.md`

- [ ] **Step 1: 显式映射开发回显环境变量**

```yaml
forge:
  captcha:
    # 仅 dev/local Profile 可生效；生产即使误配为 true 也不回显
    dev-echo-code: ${FORGE_CAPTCHA_DEV_ECHO_CODE:false}
```

- [ ] **Step 2: 删除前端验证码控制台输出**

删除 `refreshCaptcha` 和 `sendSmsCode` 中读取 `res.data.code` 并 `console.warn` 的代码，保留图片、`codeKey`、状态提示和倒计时逻辑。

- [ ] **Step 3: 执行后端聚合验证**

```bash
cd forge-server
mvn -Penable-tests \
  -pl forge-framework/forge-starter-parent/forge-starter-auth,forge-framework/forge-plugin-parent/forge-plugin-system \
  -am test
mvn -pl forge-admin-server -am package -DskipTests
```

预期：两条命令均 `BUILD SUCCESS`。

- [ ] **Step 4: 执行前端和静态安全验证**

```bash
cd forge-admin-ui
source ~/.nvm/nvm.sh
nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm build
```

```bash
cd ..
rg -n 'code=|cached=|expectedX|验证码.*\{\}|console\.warn.*验证码' \
  forge-server/forge-framework/forge-starter-parent/forge-starter-auth/src/main \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main \
  forge-admin-ui/src/views/login/index.vue
git diff --check
```

预期：前端构建成功；敏感日志扫描无匹配；`git diff --check` 返回 0。

- [ ] **Step 5: 回填执行证据、更新状态并提交**

将命令、测试数量、跳过的真实短信 E2E、服务 PID 写入 `execution-log.md`；将 Spec 状态更新为 `review`。

```bash
git add code-copilot/changes/framework-captcha-security-hardening \
  forge-server/forge-admin-server/src/main/resources/application.yml \
  forge-admin-ui/src/views/login/index.vue
git commit -m "[framework-captcha-security-hardening] 完成验证码安全验证"
```
