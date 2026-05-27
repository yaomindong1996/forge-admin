# forge-starter-crypto 模块深度分析报告

> 分析时间：2026-05-27
> 模块路径：`forge/forge-framework/forge-starter-parent/forge-starter-crypto`
> 前端配套代码：`forge-admin-ui/src/utils/crypto/`

---

## 一、模块概述

`forge-starter-crypto` 是 MDFrame 的接口加解密 Starter 模块，基于 Spring Boot 自动配置机制，提供**请求解密、响应加密、字段级加解密、数据脱敏、防重放攻击、动态密钥协商**六大核心能力，支持 SM4/AES 两种对称加密算法及 RSA 密钥交换。

### 核心依赖

| 依赖 | 用途 |
|------|------|
| `hutool-crypto` | 提供 AES/SM4/RSA 加密工具封装 |
| `bcprov-jdk15to18` | BouncyCastle 国密算法（SM4）支持 |
| `spring-boot-starter-web` | 提供 `@RestControllerAdvice`、`FilterRegistrationBean` 等 Web 基础设施 |
| `forge-starter-core` | 提供 `@ApiEncrypt`、`@ApiDecrypt` 注解定义及 `CryptoProperties` 配置属性 |
| `forge-starter-cache`（可选） | 提供 `ICacheService`，用于会话密钥存储和防重放 Token 缓存 |
| `forge-starter-tenant`（可选） | 多租户支持 |
| `forge-starter-api-config` | 提供 `IApiConfigManager`，支持基于 API 配置的加解密策略 |
| Sa-Token | 提供 `@SaIgnore` 注解，跳过鉴权 |

---

## 二、注解体系

### 2.1 `@ApiEncrypt` — 响应加密注解

**所在包**：`com.mdframe.forge.starter.core.annotation.crypto`

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEncrypt {
    String algorithm() default ""; // 加密算法，空则使用配置文件默认算法
}
```

- **标注位置**：Controller 类或方法上
- **作用**：标注后，该接口的响应体将被加密后返回
- **算法指定**：可通过 `algorithm = "AES"` 或 `algorithm = "SM4"` 覆盖全局配置

### 2.2 `@ApiDecrypt` — 请求解密注解

**所在包**：`com.mdframe.forge.starter.core.annotation.crypto`

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDecrypt {
    String algorithm() default ""; // 解密算法，空则使用配置文件默认算法
}
```

- **标注位置**：Controller 类或方法上
- **作用**：标注后，该接口的请求体将被解密后再进入 Controller

### 2.3 `@CryptoField` — 字段级加解密注解

**所在包**：`com.mdframe.forge.starter.crypto.crypto`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = CryptoFieldSerializer.class)
@JsonDeserialize(using = CryptoFieldDeserializer.class)
public @interface CryptoField {
    String algorithm() default ""; // 加密算法
    boolean encrypt() default true;  // 序列化时是否加密
    boolean decrypt() default true;  // 反序列化时是否解密
}
```

- **标注位置**：实体类字段上
- **作用机制**：通过 Jackson 的 `ContextualSerializer` / `ContextualDeserializer` 接口，在 JSON 序列化/反序列化时自动对字段进行加解密
- **典型场景**：数据库存密文，API 返回明文；或 API 接收明文，存库前加密

### 2.4 `@Desensitize` — 数据脱敏注解

**所在包**：`com.mdframe.forge.starter.crypto.desensitize.annotation`

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DesensitizeSerializer.class)
public @interface Desensitize {
    DesensitizeType type() default DesensitizeType.CUSTOM;
    int prefixKeep() default 0;     // 前置保留长度
    int suffixKeep() default 0;     // 后置保留长度
    char replaceChar() default '*';  // 替换字符
    boolean enabled() default true;  // 是否启用脱敏
}
```

- **内置脱敏类型**（`DesensitizeType` 枚举）：

| 类型 | 策略 | 示例 |
|------|------|------|
| `PHONE` | 保留前3后4 | `138****1234` |
| `ID_CARD` | 保留前6后4 | `110101********1234` |
| `EMAIL` | `@`前保留首字符 | `t**@example.com` |
| `BANK_CARD` | 保留后4位 | `****1234` |
| `NAME` | 保留姓氏 | `张*` |
| `ADDRESS` | 保留前6后4 | `北京市****1234号` |
| `PASSWORD` | 全部替换 | `******` |
| `CAR_LICENSE` | 保留后2位 | `京A****1` |
| `CUSTOM` | 按 `prefixKeep`/`suffixKeep` 自定义 | 灵活配置 |

---

## 三、请求/响应加解密实现机制

### 3.1 整体架构

```
前端请求                     后端处理                        前端响应
─────────                   ─────────                   ─────────
JSON明文  ──→  encryptRequest()  ──→  DecryptRequestBodyAdvice
                                    ↓
                             Controller @ApiDecrypt
                                    ↓
                             业务逻辑处理
                                    ↓
                             EncryptResponseBodyAdvice
                                    ↓
         decryptResponse()  ←──  JSON密文（EncryptedResponse） ←──
```

### 3.2 请求解密：`DecryptRequestBodyAdvice`

**文件**：`advice/DecryptRequestBodyAdvice.java`

实现 `RequestBodyAdvice` 接口，在 `@RequestBody` 参数反序列化**之前**介入：

```java
@Override
public boolean supports(MethodParameter methodParameter, ...) {
    // 1. 检查全局开关 forge.crypto.enabled + forge.crypto.enable-api-crypto
    // 2. 检查请求头 X-Inner-Call=true（内部服务调用跳过）
    // 3. 优先查 IApiConfigManager（API配置表）判断是否需要解密
    // 4. 降级到 @ApiDecrypt 注解判断
}
```

**解密流程**（`beforeBodyRead`）：

1. 读取请求体原始 JSON 字符串
2. 解析为 `EncryptedRequest`（包含 `data` 密文 + `algorithm` 算法标识）
3. 通过 `EncryptorFactory` 获取对应算法的 `Encryptor`
4. **优先使用动态密钥**（`SessionKeyStore` 中获取），降级使用默认密钥
5. 解密后得到明文 JSON，包装为 `DecryptedHttpInputMessage` 供后续反序列化

**关键设计**：
- 内部服务调用通过 `X-Inner-Call: true` 请求头跳过加解密
- 支持通过 `EncryptedRequest.algorithm` 字段动态指定算法（前端可覆盖）

### 3.3 响应加密：`EncryptResponseBodyAdvice`

**文件**：`advice/EncryptResponseBodyAdvice.java`

实现 `ResponseBodyAdvice<Object>` 接口，在响应体**写入输出流之前**介入：

```java
@Override
public boolean supports(MethodParameter returnType, ...) {
    // 1. 检查全局开关
    // 2. 跳过 ResponseEntity<byte[]> 类型（文件/图片下载）
    // 3. 检查 X-Inner-Call 请求头
    // 4. 优先查 IApiConfigManager，降级到 @ApiEncrypt 注解
}
```

**加密流程**（`beforeBodyWrite`）：

1. 将响应体序列化为 JSON 字符串
2. 通过 `EncryptorFactory` 获取对应算法的 `Encryptor`
3. **优先使用动态密钥**，降级使用默认密钥
4. 加密后得到密文，包装为 `EncryptedResponse`（包含 `data` + `algorithm`）
5. 前端收到后通过 `decryptResponse()` 拦截器自动解密

**关键设计**：
- 自动跳过二进制响应（`ResponseEntity<byte[]>`），避免加密文件/图片
- 排除路径可通过 `forge.crypto.exclude-paths` 配置

### 3.4 前端配套：`encrypt-request.js` + `crypto-interceptor.js`

**前端加密流程**（`crypto-interceptor.js`）：

```javascript
// 请求拦截器
export function encryptRequest(config) {
    // 1. 检查 config.encrypt !== false 且路径匹配 shouldEncrypt()
    // 2. 检查 cryptoConfig.secretKey 是否有效
    // 3. 将 config.data（JSON对象）加密为 { data: encryptedData, algorithm: 'SM4' }
}

// 响应拦截器
export function decryptResponse(response) {
    // 1. 检查 response.data 是否为 EncryptedResponse 格式
    // 2. 使用 cryptoConfig.secretKey 解密
    // 3. 将解密后的 JSON 对象替换 response.data
    // 4. 特殊处理：解密失败（padding error）抛出 DECRYPT_ERROR，由上层处理（触发密钥重新协商）
}
```

**前端密钥管理**（`crypto-config.js`）：
- 初始状态 `secretKey` 为空
- 动态密钥模式下，密钥通过 `/crypto/exchange` 接口协商后写入 `cryptoConfig.secretKey`
- 解密失败时自动触发密钥重新协商

---

## 四、加密算法与密钥管理机制

### 4.1 算法支持

| 算法 | 密钥长度 | 实现类 | 说明 |
|------|---------|--------|------|
| **SM4** | 16 字节（128位） | `SM4Encryptor` | 国密对称加密，默认算法，基于 BouncyCastle |
| **AES** | 16/24/32 字节 | `AESEncryptor` | 国际标准，基于 Hutool `SecureUtil.aes()` |

**算法选择优先级**（以响应加密为例）：
1. `@ApiEncrypt(algorithm = "AES")` 注解指定
2. `EncryptedRequest.algorithm` 前端动态指定
3. `forge.crypto.algorithm` 配置文件指定（默认 `SM4`）

### 4.2 密钥管理体系

#### 4.2.1 默认密钥（降级方案）

```
forge.crypto.secret-key=Base64Encoded16ByteKey==
```

- 配置在 `application.yml` 中
- 当动态密钥不可用时的降级方案
- `SM4Encryptor` / `AESEncryptor` 构造时校验密钥长度并初始化默认加密器

#### 4.2.2 动态密钥协商（核心安全机制）

**整体流程**：

```
前端                                        后端
─────                                      ────
1. GET /crypto/public-key  ──────────────→  返回 RSA 公钥（Base64）
2. 前端生成随机 16字节会话密钥（SM4/AES）
   用 RSA 公钥加密会话密钥
3. POST /crypto/exchange  ──────────────→  用 RSA 私钥解密，得到会话密钥
   { encryptedKey: RSA加密后的会话密钥 }       存储到 Redis: crypto:session:{sessionId}
4. 后续请求使用会话密钥加密/解密
   （从 Authorization 或 X-Session-Id 关联）
```

**核心类**：

| 类 | 职责 |
|----|------|
| `RsaKeyPairHolder` | 管理服务端 RSA 密钥对（自动生成或配置文件加载） |
| `KeyExchangeService` | 处理密钥交换逻辑：用 RSA 私钥解密前端发来的加密会话密钥，存储到 `SessionKeyStore` |
| `KeyExchangeController` | 暴露 `/crypto/public-key`（获取公钥）和 `/crypto/exchange`（密钥交换）接口 |
| `SessionKeyStore` | 将会话密钥存储到 Redis（`crypto:session:{sessionId}`），过期时间默认 7200 秒 |

**会话标识获取优先级**（`getSessionIdFromRequest`）：

1. `Authorization: Bearer {token}` → 使用 JWT Token 作为 sessionId
2. `X-Session-Id` 请求头
3. `HttpSession.getId()`（传统 Session 模式）

**配置属性**：

```yaml
forge:
  crypto:
    enable-dynamic-key: true          # 启用动态密钥（默认 true）
    rsa-public-key: ""               # 可选：配置固定 RSA 公钥（Base64）
    rsa-private-key: ""              # 可选：配置固定 RSA 私钥（Base64）
    session-key-expire: 7200         # 会话密钥过期时间（秒）
```

### 4.3 `EncryptorFactory` — 加密器工厂

```java
public class EncryptorFactory {
    private final Map<CryptoAlgorithm, Encryptor> encryptorMap = new ConcurrentHashMap<>();
    
    public void register(Encryptor encryptor) { ... }
    public Encryptor getEncryptor(String algorithm) { ... }
    public Encryptor getDefaultEncryptor() { ... }
}
```

- 在 `CryptoAutoConfiguration` 中注册 `SM4Encryptor` 和 `AESEncryptor`
- 支持通过算法名称或 `CryptoAlgorithm` 枚举获取加密器
- 扩展新算法只需实现 `Encryptor` 接口并在配置类中注册

---

## 五、字段级加解密与数据脱敏

### 5.1 字段级加解密

**使用方式**：

```java
@Data
public class UserDTO {
    private Long id;
    
    @CryptoField(algorithm = "SM4")  // 序列化时加密，反序列化时解密
    private String idCard;
    
    @CryptoField(algorithm = "AES", encrypt = true, decrypt = false)
    private String sensitiveData;  // 只加密不解密
}
```

**实现原理**：

- `CryptoFieldSerializer`：Jackson 序列化时，通过 `EncryptorFactory` 获取加密器对字段值加密
- `CryptoFieldDeserializer`：Jackson 反序列化时，通过 `EncryptorFactory` 获取加密器对字段值解密
- 两者均实现 `ContextualSerializer` / `ContextualDeserializer`，能在运行时获取字段上的 `@CryptoField` 注解信息
- `JacksonCryptoConfiguration` 通过 `Jackson2ObjectMapperBuilderCustomizer` 将 `EncryptorFactory` 注入到 `ObjectMapper` 的 Attribute 中

**配置开关**：

```yaml
forge:
  crypto:
    enable-field-crypto: true   # 默认 true
```

### 5.2 数据脱敏

**使用方式**：

```java
@Data
public class UserVO {
    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;
    
    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;
    
    @Desensitize(type = DesensitizeType.CUSTOM, prefixKeep = 1, suffixKeep = 2)
    private String customField;  // 自定义：保留前1后2
}
```

**实现原理**：

- `DesensitizeSerializer`：Jackson 序列化时，根据 `@Desensitize` 注解的 `type` 选择对应 `DesensitizeStrategy` 进行脱敏
- `DesensitizeStrategyFactory`：注册所有内置脱敏策略，支持扩展

**配置开关**：

```yaml
forge:
  crypto:
    enable-desensitize: true   # 默认 true
```

### 5.3 `EncryptTypeHandler` — MyBatis Plus 字段加密

**文件**：`handler/EncryptTypeHandler.java`

```java
@MappedTypes({String.class})
public class EncryptTypeHandler extends AbstractJsonTypeHandler<String> {
    private static final String SECRET_KEY = "forge_client_secret_key_16b";
    private static final AES AES = SecureUtil.aes(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
}
```

- 用于数据库字段的透明加解密（存库时加密，读取时解密）
- **注意**：密钥硬编码在代码中（`forge_client_secret_key_16b`），适用于客户端敏感数据存储场景
- 通过 MyBatis Plus 的 `TypeHandler` 机制自动生效，无需修改 SQL

---

## 六、防重放攻击机制

### 6.1 原理

防重放攻击通过验证请求的唯一性和时效性，防止攻击者截获合法请求后重复发送。

### 6.2 实现：`ReplayAttackFilter`

**文件**：`filter/ReplayAttackFilter.java`

**验证流程**：

1. **检查请求头**：
   - `X-Timestamp`：请求时间戳（毫秒）
   - `X-Nonce`：随机字符串（每次请求唯一）

2. **时间戳验证**：
   ```
   |当前时间 - 请求时间| <= forge.crypto.replay-time-window * 1000
   ```
   默认时间窗口 300 秒（5分钟）

3. **Nonce 验证**：
   - 检查 `crypto:replay:{nonce}` 是否已存在于 Redis
   - 已存在 → 拒绝（重放攻击）
   - 不存在 → 通过，并将 nonce 写入 Redis，过期时间为 `replayTimeWindow`

**跳过条件**：

- 请求头 `X-Inner-Call: true`（内部调用）
- 路径匹配 `replayExcludePaths`（默认排除 `/auth/captcha`、`/crypto/public-key` 等）
- 路径不匹配 `replayIncludePaths`（为空则全量保护）

### 6.3 前端实现

前端在 `interceptors.js`（未在本次分析范围内，但 `crypto-interceptor.js` 中有引用）中统一添加 `X-Timestamp` 和 `X-Nonce` 请求头。

**配置属性**：

```yaml
forge:
  crypto:
    enable-replay-protection: false   # 默认关闭
    replay-time-window: 300           # 时间窗口（秒）
    replay-include-paths: []          # 保护路径（为空则全量保护）
    replay-exclude-paths:             # 排除路径
      - /auth/captcha
      - /auth/captcha/**
      - /auth/loginConfig
      - /crypto/public-key
```

---

## 七、自动配置体系

### 7.1 `CryptoAutoConfiguration`

**文件**：`config/CryptoAutoConfiguration.java`

**条件化 Bean 注册**：

| Bean | 条件 | 说明 |
|------|------|------|
| `RsaKeyPairHolder` | 始终注册 | RSA 密钥对（自动生成或配置文件加载） |
| `SessionKeyStore` | 存在 `ICacheService` Bean | 会话密钥 Redis 存储 |
| `KeyExchangeService` | 始终注册 | 密钥交换服务 |
| `KeyExchangeController` | 始终注册 | 密钥交换 Controller |
| `SM4Encryptor` | 始终注册 | SM4 加密器 |
| `AESEncryptor` | 始终注册 | AES 加密器 |
| `EncryptorFactory` | 始终注册 | 加密器工厂 |
| `DesensitizeStrategyFactory` | 始终注册 | 脱敏策略工厂 |
| `DecryptRequestBodyAdvice` | 始终注册 | 请求解密 Advice |
| `EncryptResponseBodyAdvice` | 始终注册 | 响应加密 Advice |
| `ReplayTokenCache` | 存在 `ICacheService` Bean | 防重放 Token 缓存 |
| `ReplayAttackFilter` | 存在 `ReplayTokenCache` Bean 且 `enableReplayProtection=true` | 防重放过滤器 |

### 7.2 `JacksonCryptoConfiguration`

**文件**：`config/JacksonCryptoConfiguration.java`

- 通过 `Jackson2ObjectMapperBuilderCustomizer` 将 `EncryptorFactory` 和 `DesensitizeStrategyFactory` 注入到 `ObjectMapper` 的 Attribute 中
- 条件：`forge.crypto.enabled=true` 且存在 `EncryptorFactory` Bean
- 字段级加解密和脱敏的开关独立控制（`enable-field-crypto`、`enable-desensitize`）

### 7.3 Spring Boot 自动配置注册

**文件**：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.mdframe.forge.starter.crypto.config.CryptoAutoConfiguration
com.mdframe.forge.starter.crypto.config.JacksonCryptoConfiguration
```

---

## 八、配置属性详解（`CryptoProperties`）

**文件**：`forge-starter-core/.../context/CryptoProperties.java`

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | `true` | 是否启用加密模块 |
| `algorithm` | String | `"SM4"` | 默认加密算法（SM4/AES） |
| `secret-key` | String | `null` | 默认对称密钥（Base64，降级方案） |
| `enable-dynamic-key` | Boolean | `true` | 是否启用动态密钥协商 |
| `rsa-public-key` | String | `null` | RSA 公钥（Base64，可选） |
| `rsa-private-key` | String | `null` | RSA 私钥（Base64，可选） |
| `session-key-expire` | Long | `7200` | 会话密钥过期时间（秒） |
| `enable-api-crypto` | Boolean | `true` | 是否启用 API 级加解密 |
| `enable-field-crypto` | Boolean | `true` | 是否启用字段级加解密 |
| `enable-desensitize` | Boolean | `true` | 是否启用数据脱敏 |
| `enable-replay-protection` | Boolean | `false` | 是否启用防重放保护 |
| `replay-time-window` | Long | `300` | 防重放时间窗口（秒） |
| `replay-include-paths` | List | `[]` | 防重放保护路径 |
| `replay-exclude-paths` | List | `[/auth/captcha, ...]` | 防重放排除路径 |
| `exclude-paths` | List | `[]` | API加解密排除路径 |

**支持配置热刷新**：`@RefreshScope` 注解支持 Nacos/Apollo 等配置中心动态刷新。

---

## 九、使用场景与最佳实践

### 9.1 适用场景

| 场景 | 使用方式 | 说明 |
|------|---------|------|
| **登录接口** | `@ApiDecrypt` + `@ApiEncrypt` | 用户名/密码加密传输，防止被窃听 |
| **敏感数据 API** | `@ApiEncrypt` | 返回数据加密（如身份证号、手机号） |
| **字段级加密存储** | `@CryptoField` | 数据库存密文，查询时自动解密 |
| **数据脱敏展示** | `@Desensitize` | 列表页展示脱敏数据，详情页返回明文 |
| **防重放攻击** | 全局 Filter | 保护敏感写操作接口（如支付、修改密码） |

### 9.2 性能优化策略

1. **排除不必要加密的路径**：通过 `exclude-paths` 配置（如文件上传/下载、公开 API）
2. **跳过二进制响应**：`EncryptResponseBodyAdvice` 自动跳过 `ResponseEntity<byte[]>`
3. **跳过内部调用**：通过 `X-Inner-Call: true` 请求头，Feign/Ribbon 调用时不加解密
4. **按需使用字段级加密**：`@CryptoField` 只标注敏感字段，避免全量加密
5. **防重放按需启用**：通过 `replay-include-paths` 只保护关键接口，减少 Redis 访问

### 9.3 安全建议

1. **生产环境务必启用动态密钥**（`enable-dynamic-key: true`），避免密钥硬编码
2. **RSA 密钥对建议定期轮换**，可通过配置 `rsa-public-key` / `rsa-private-key` 使用固定密钥对
3. **防重放攻击建议开启**（`enable-replay-protection: true`），保护敏感写操作
4. **会话密钥过期时间**（`session-key-expire`）建议设置为与 JWT Token 过期时间一致
5. **MyBatis Plus 的 `EncryptTypeHandler` 密钥硬编码问题**：建议改为从配置文件读取

---

## 十、前端集成指南

### 10.1 安装依赖

```bash
npm install sm-crypto  # SM4 算法库
```

### 10.2 初始化（动态密钥模式）

```javascript
import { updateCryptoConfig } from '@/utils/crypto/crypto-config'
import { postEncrypt } from '@/utils/encrypt-request'

// 1. 获取服务端 RSA 公钥
const publicKey = await get('/crypto/public-key')

// 2. 生成会话密钥（16字节随机密钥，Base64编码）
const sessionKey = generateRandomKey()

// 3. 用 RSA 公钥加密会话密钥，发送到服务端
await postEncrypt('/crypto/exchange', {
  encryptedKey: rsaEncrypt(sessionKey, publicKey)
})

// 4. 更新前端加密配置
updateCryptoConfig({ secretKey: sessionKey })
```

### 10.3 使用加密请求

```javascript
import { postEncrypt } from '@/utils/encrypt-request'

// 自动加密请求体，自动解密响应体
const result = await postEncrypt('/api/user/login', {
  username: 'admin',
  password: '123456'
})
```

---

## 十一、模块文件清单

```
forge-starter-crypto/
├── pom.xml
├── src/main/java/com/mdframe/forge/starter/crypto/
│   ├── advice/
│   │   ├── DecryptRequestBodyAdvice.java      # 请求解密 Advice
│   │   └── EncryptResponseBodyAdvice.java    # 响应加密 Advice
│   ├── cache/
│   │   └── ReplayTokenCache.java             # 防重放 Token 缓存
│   ├── config/
│   │   ├── CryptoAutoConfiguration.java      # 自动配置类
│   │   └── JacksonCryptoConfiguration.java  # Jackson 加解密配置
│   ├── crypto/
│   │   ├── CryptoAlgorithm.java              # 算法枚举（SM4/AES）
│   │   ├── CryptoField.java                  # 字段级加解密注解
│   │   ├── Encryptor.java                   # 加密器接口
│   │   ├── EncryptorFactory.java             # 加密器工厂
│   │   └── impl/
│   │       ├── AESEncryptor.java             # AES 实现
│   │       └── SM4Encryptor.java            # SM4 实现
│   ├── desensitize/
│   │   ├── annotation/
│   │   │   └── Desensitize.java             # 脱敏注解
│   │   ├── serializer/
│   │   │   └── DesensitizeSerializer.java    # 脱敏序列化器
│   │   └── strategy/
│   │       ├── DesensitizeStrategy.java      # 脱敏策略接口
│   │       ├── DesensitizeStrategyFactory.java # 脱敏策略工厂
│   │       ├── DesensitizeType.java          # 脱敏类型枚举
│   │       ├── PhoneDesensitizeStrategy.java
│   │       ├── IdCardDesensitizeStrategy.java
│   │       ├── EmailDesensitizeStrategy.java
│   │       ├── BankCardDesensitizeStrategy.java
│   │       ├── NameDesensitizeStrategy.java
│   │       ├── AddressDesensitizeStrategy.java
│   │       ├── PasswordDesensitizeStrategy.java
│   │       └── CarLicenseDesensitizeStrategy.java
│   ├── domain/
│   │   ├── EncryptedRequest.java            # 加密请求体 DTO
│   │   └── EncryptedResponse.java           # 加密响应体 DTO
│   ├── filter/
│   │   └── ReplayAttackFilter.java          # 防重放过滤器
│   ├── handler/
│   │   └── EncryptTypeHandler.java          # MyBatis Plus 字段加密
│   ├── keyexchange/
│   │   ├── KeyExchangeController.java        # 密钥交换 Controller
│   │   ├── KeyExchangeService.java          # 密钥交换服务
│   │   ├── KeyExchangeRequest.java          # 密钥交换请求 DTO
│   │   ├── PublicKeyResponse.java           # 公钥响应 DTO
│   │   ├── RsaKeyPairHolder.java            # RSA 密钥对持有者
│   │   └── SessionKeyStore.java             # 会话密钥存储（Redis）
│   └── serializer/
│       ├── CryptoFieldSerializer.java        # 字段加密序列化器
│       └── CryptoFieldDeserializer.java     # 字段解密反序列化器
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 十二、总结

`forge-starter-crypto` 是一个设计完善的接口加解密 Starter，具备以下特点：

1. **双层加密体系**：API 级（整个请求/响应） + 字段级（单个字段），灵活适配不同场景
2. **动态密钥协商**：基于 RSA + 会话密钥的双层加密体系，安全性高
3. **国密支持**：原生支持 SM4 算法，满足等保/国密合规要求
4. **数据脱敏**：内置 8 种常用脱敏策略，注解化使用，对前端透明
5. **防重放攻击**：基于 Timestamp + Nonce 的方案，有效防止请求重放
6. **条件化配置**：所有功能均可通过配置文件独立开关，支持热刷新
7. **前端配套完善**：提供完整的 JS/TS 加密拦截器，开箱即用
