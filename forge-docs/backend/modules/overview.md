# 后端模块概览

## 模块列表

| 模块 | 说明 |
|------|------|
| [forge-starter-excel](./excel.md) | Excel 导出模块 |
| [forge-starter-file](./file.md) | 文件存储模块 |
| [forge-starter-trans](./trans.md) | 字典翻译模块 |
| [forge-starter-id](./id.md) | 分布式 ID 模块 |
| [forge-starter-datascope](./datascope.md) | 数据权限模块 |
| [forge-starter-tenant](./tenant.md) | 多租户模块 |
| [forge-starter-message](./message.md) | 消息推送模块 |
| [forge-starter-websocket](./websocket.md) | WebSocket 模块 |
| [forge-starter-job](./job.md) | 定时任务模块 |
| [forge-starter-log](./log.md) | 操作日志模块 |
| [forge-starter-cache](./cache.md) | 缓存模块 |
| [forge-starter-auth](./auth.md) | 认证授权模块 |
| [forge-starter-crypto](./crypto.md) | 加解密模块 |

## 使用方式

在 `pom.xml` 中引入需要的模块：

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-excel</artifactId>
</dependency>
```