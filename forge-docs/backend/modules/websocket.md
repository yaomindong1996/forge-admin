# WebSocket 模块

基于 Spring WebSocket 的实时消息推送模块。

## 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-websocket</artifactId>
</dependency>
```

## 配置

```yaml
forge:
  websocket:
    enabled: true
    path: /ws
    allowed-origins: "*"
```

## 使用示例

### 发送消息

```java
@Autowired
private IMessagePushService messagePushService;

// 发送给指定用户
messagePushService.pushToUser(userId, "通知内容");

// 发送给所有用户
messagePushService.pushToAll("系统公告");

// 发送给指定会话
messagePushService.pushToSession(sessionId, message);
```

### 消息类型

```java
public enum MessageType {
    TEXT,       // 文本消息
    NOTICE,     // 通知
    COMMAND,    // 命令
    DATA        // 数据推送
}
```

### 消息结构

```java
public class WebSocketMessage {
    private String type;        // 消息类型
    private String title;       // 标题
    private String content;     // 内容
    private Object data;        // 数据
    private Long timestamp;     // 时间戳
}
```

## 前端连接

```js
import { useWebSocket } from '@/utils/websocket'

const { connect, disconnect, send, onMessage } = useWebSocket('/ws')

// 连接
connect()

// 监听消息
onMessage((message) => {
  console.log('收到消息:', message)
})

// 发送消息
send({ type: 'text', content: 'Hello' })

// 断开连接
disconnect()
```

## API 参考

### IMessagePushService

```java
// 推送给指定用户
void pushToUser(Long userId, String message);

// 推送给指定用户（对象消息）
void pushToUser(Long userId, WebSocketMessage message);

// 推送给所有用户
void pushToAll(String message);

// 推送给指定会话
void pushToSession(String sessionId, WebSocketMessage message);

// 判断用户是否在线
boolean isOnline(Long userId);

// 获取在线用户数
int getOnlineCount();
```

## 注意事项

1. 需要配置 WebSocket 路径和跨域
2. 前端需要处理重连逻辑
3. 消息建议使用 JSON 格式
4. 生产环境建议使用 STOMP 协议