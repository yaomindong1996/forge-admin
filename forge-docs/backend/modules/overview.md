# 后端模块概览

## Starter 模块

| 模块 | 说明 |
|------|------|
| [forge-starter-core](./core.md) | 核心工具（统一响应、异常处理、会话管理、常用注解） |
| [forge-starter-config](./config.md) | 动态配置中心（@RefreshScope、数据库配置刷新） |
| [forge-starter-idempotent](./idempotent.md) | 分布式幂等（多策略、Redisson 分布式锁、Token API） |
| [forge-starter-orm](./orm.md) | MyBatis-Plus 集成（自动填充、分页、乐观锁） |
| [forge-starter-social](./social.md) | 社交登录（微信、钉钉、GitHub 等 18+ 平台） |
| [forge-starter-web](./web.md) | Web 层（Undertow、Actuator、验证码） |
| [forge-starter-api-config](./api-config.md) | API 配置管理（自动注册、两级缓存、实时刷新） |
| [forge-starter-auth](./auth.md) | 认证授权 |
| [forge-starter-cache](./cache.md) | 缓存 |
| [forge-starter-crypto](./crypto.md) | 加解密 |
| [forge-starter-datascope](./datascope.md) | 数据权限 |
| [forge-starter-excel](./excel.md) | Excel 导出 |
| [forge-starter-file](./file.md) | 文件存储 |
| [forge-starter-id](./id.md) | 分布式 ID |
| [forge-starter-job](./job.md) | 定时任务 |
| [forge-starter-log](./log.md) | 操作日志 |
| [forge-starter-message](./message.md) | 消息推送 |
| [forge-starter-tenant](./tenant.md) | 多租户 |
| [forge-starter-trans](./trans.md) | 字典翻译 |
| [forge-starter-websocket](./websocket.md) | WebSocket |

## 插件模块

| 模块 | 说明 |
|------|------|
| [forge-plugin-ai](./ai.md) | AI 插件（多提供商、Agent 管理、Chat 会话、流式 SSE） |
| forge-plugin-system | 系统管理 |
| forge-plugin-generator | 代码生成 |
| forge-plugin-job | 任务管理 |
| forge-plugin-message | 消息管理 |
| forge-plugin-flow | 流程管理 |

## 流程引擎

| 模块 | 说明 |
|------|------|
| [forge-flow-client](./flow-client.md) | 流程引擎客户端（注解驱动、自动启动、事件回调） |

## 使用方式

在 `pom.xml` 中引入需要的模块：

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-starter-core</artifactId>
</dependency>
```
