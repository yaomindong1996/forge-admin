# 操作日志模块

自动记录操作日志和登录日志。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-log</artifactId>
</dependency>
```

## 使用示例

### 记录操作日志

```java
@OperationLog(module = "用户管理", operation = "新增用户")
@PostMapping
public Result add(@RequestBody User user) {
    // 业务逻辑
}
```

### 注解说明

```java
@OperationLog(
    module = "用户管理",      // 模块名称
    operation = "新增用户",   // 操作描述
    recordParams = true,     // 是否记录参数
    recordResult = false     // 是否记录返回值
)
```

### 登录日志

登录日志通过事件监听自动记录：

```java
@Component
public class LoginLogListener {
    
    @EventListener
    public void onLoginSuccess(LoginSuccessEvent event) {
        // 记录登录成功日志
    }
    
    @EventListener
    public void onLoginFailure(LoginFailureEvent event) {
        // 记录登录失败日志
    }
}
```

## 日志结构

### OperationLogInfo

```java
public class OperationLogInfo {
    private Long logId;           // 日志 ID
    private String module;        // 模块
    private String operation;     // 操作
    private String method;        // 方法名
    private String requestUrl;    // 请求 URL
    private String requestMethod; // 请求方法
    private String requestParams; // 请求参数
    private String responseData;  // 响应数据
    private Long executeTime;     // 执行时间
    private String ip;            // IP 地址
    private String location;      // 地理位置
    private Long userId;          // 用户 ID
    private String userName;      // 用户名
    private Integer status;       // 状态
    private String errorMsg;      // 错误信息
}
```

### LoginLogInfo

```java
public class LoginLogInfo {
    private Long logId;           // 日志 ID
    private String userName;      // 用户名
    private String ip;            // IP 地址
    private String location;      // 地理位置
    private String browser;       // 浏览器
    private String os;            // 操作系统
    private Integer status;       // 状态
    private String message;       // 消息
    private Date loginTime;       // 登录时间
}
```

## SPI 扩展

### ILogService

自定义日志存储：

```java
@Component
public class MyLogService implements ILogService {
    
    @Override
    public void saveOperationLog(OperationLogInfo log) {
        // 保存操作日志
    }
    
    @Override
    public void saveLoginLog(LoginLogInfo log) {
        // 保存登录日志
    }
}
```

## 配置

```yaml
forge:
  log:
    enabled: true
    async: true           # 异步记录
    record-params: true   # 记录参数
    exclude-urls:         # 排除的 URL
      - /actuator/**
```