# 消息推送模块

统一的消息推送模块，支持站内信、短信等渠道。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-message</artifactId>
</dependency>
```

## 配置

```yaml
forge:
  message:
    channels:
      - web
      - sms
    templates:
      cache-enabled: true
```

## 使用示例

### 发送消息

```java
@Autowired
private MessageClient messageClient;

public void sendNotification(Long userId, String content) {
    messageClient.send(MessageSendRequest.builder()
        .userId(userId)
        .title("系统通知")
        .content(content)
        .channel("web")
        .build());
}
```

### 批量发送

```java
public void sendToUsers(List<Long> userIds, String content) {
    messageClient.sendBatch(MessageSendRequest.builder()
        .userIds(userIds)
        .title("批量通知")
        .content(content)
        .channel("web")
        .build());
}
```

### 使用模板

```java
public void sendWithTemplate(Long userId, String templateCode, Map<String, Object> params) {
    messageClient.sendByTemplate(userId, templateCode, params);
}
```

## 消息渠道

### 站内信 (Web)

```java
@Component
public class WebMessageChannel implements MessageChannel {
    
    @Override
    public String getChannelType() {
        return "web";
    }
    
    @Override
    public void send(Message message) {
        // 保存到消息表
        messageMapper.insert(message);
    }
}
```

### 短信 (SMS)

```java
@Component
public class SmsMessageChannel implements MessageChannel {
    
    @Override
    public String getChannelType() {
        return "sms";
    }
    
    @Override
    public void send(Message message) {
        // 调用短信服务
        smsService.send(message.getPhone(), message.getContent());
    }
}
```

## 消息模板

### 模板配置

```java
public class SysMessageTemplate {
    private Long templateId;
    private String templateCode;    // 模板编码
    private String templateName;    // 模板名称
    private String title;           // 标题模板
    private String content;         // 内容模板
    private String channel;         // 渠道
}
```

### 模板变量

```
标题：订单状态变更通知
内容：尊敬的${username}，您的订单${orderNo}状态已变更为${status}。
```

```java
Map<String, Object> params = new HashMap<>();
params.put("username", "张三");
params.put("orderNo", "ORD20240115001");
params.put("status", "已发货");

messageClient.sendByTemplate(userId, "ORDER_STATUS_CHANGE", params);
```

## API 参考

### MessageClient

```java
// 发送单条消息
void send(MessageSendRequest request);

// 批量发送
void sendBatch(MessageSendRequest request);

// 使用模板发送
void sendByTemplate(Long userId, String templateCode, Map<String, Object> params);

// 获取未读数量
int getUnreadCount(Long userId);

// 标记已读
void markAsRead(Long messageId);
```