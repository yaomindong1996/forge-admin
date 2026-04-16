# Web 层（forge-starter-web）

Web 层基础模块，集成 Undertow 服务器、Actuator 监控和验证码生成。

## 快速开始

### 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-web</artifactId>
</dependency>
```

## 核心功能

### Undertow 服务器

默认使用 **Undertow** 作为 Web 服务器（非 Tomcat），在 `pom.xml` 中已排除 Tomcat：

```xml
<exclusions>
    <exclusion>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-tomcat</artifactId>
    </exclusion>
</exclusions>
```

Undertow 相比 Tomcat 具有更高的并发性能和更低的内存占用。

### Actuator 监控

集成 `spring-boot-starter-actuator`，默认暴露以下端点：

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/info` | 应用信息 |
| `/actuator/metrics` | 指标数据 |

> **安全提示**：生产环境建议配置端点访问控制，避免暴露敏感信息。

### 验证码

集成 Hutool 验证码库，支持生成图形验证码：

```java
@Autowired
private CaptchaService captchaService;

// 生成图形验证码
@GetMapping("/captcha")
public RespInfo getCaptcha() {
    LineCaptcha captcha = captchaService.generateCaptcha();
    String code = captcha.getCode();
    BufferedImage image = captcha.getImage();
    // ...
}

// 验证验证码
boolean valid = captchaService.verifyCaptcha(inputCode);
```

支持以下验证码类型：
- **LineCaptcha** — 线段干扰型验证码
- **CircleCaptcha** — 圆圈干扰型验证码
- **ShearCaptcha** — 扭曲型验证码

## 配置项

```yaml
server:
  port: 8080
  undertow:
    threads:
      io: 4           # IO 线程数
      worker: 20      # Worker 线程数
    buffer-size: 1024 # 缓冲区大小

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```
