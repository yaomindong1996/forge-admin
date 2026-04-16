# 流程引擎客户端（forge-flow-client）

流程引擎 REST 客户端模块，支持通过注解将业务方法与流程实例绑定，自动启动流程和处理流程事件。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.mdframe.forge</groupId>
    <artifactId>forge-flow-client</artifactId>
</dependency>
```

### 2. 配置流程服务地址

```yaml
forge:
  flow:
    client:
      url: http://localhost:8080    # 流程服务地址
      token: your-auth-token        # 服务间认证 Token（可选）
      connect-timeout: 3000         # 连接超时（ms）
      read-timeout: 10000           # 读取超时（ms）
      redis-subscribe: true         # 是否启用 Redis Pub/Sub 订阅流程事件
```

## 核心功能

### 注解驱动的流程集成

#### `@FlowBind` — 绑定流程模型

标注在 Service 类上，声明该类与某个流程模型关联：

```java
@Service
@FlowBind(modelKey = "leave", businessType = "leave_request")
public class LeaveService {
    // ...
}
```

#### `@FlowStart` — 自动启动流程

标注在方法上，方法执行成功后自动启动流程：

```java
@FlowStart(
    businessKeySpEl = "#result.id",
    titleSpEl = "#request.title",
    variablesSpEl = "#request.variables"
)
public LeaveResult submitLeave(LeaveRequest request) {
    // 提交请假申请...
    return result;  // 方法成功后自动启动流程
}
```

| 属性 | 说明 |
|------|------|
| `modelKey` | 流程模型编码（可选，默认取类级别 `@FlowBind` 的值） |
| `businessKeySpEl` | **必填**，业务主键 SpEL 表达式 |
| `titleSpEl` | 流程标题 SpEL |
| `userIdSpEl` | 申请人 ID SpEL |
| `userNameSpEl` | 申请人姓名 SpEL |
| `deptIdSpEl` | 申请部门 ID SpEL |
| `deptNameSpEl` | 申请部门名称 SpEL |
| `variablesSpEl` | 流程变量 SpEL |
| `skipOnError` | 发生异常时是否跳过（默认 true） |

#### `@FlowCallback` — 流程事件回调

标注在方法上，接收流程引擎推送的事件：

```java
@FlowCallback(on = {FlowEventContext.ON_COMPLETED, FlowEventContext.ON_REJECTED})
public void onFlowComplete(FlowEventContext context) {
    if (context.getEventType().equals(FlowEventContext.ON_COMPLETED)) {
        // 流程审批通过
        leaveService.approve(context.getBusinessKey());
    } else {
        // 流程被驳回
        leaveService.reject(context.getBusinessKey());
    }
}
```

| 属性 | 说明 |
|------|------|
| `on` | 监听的事件类型列表 |

**可用事件类型**：

| 事件常量 | 说明 |
|----------|------|
| `ON_COMPLETED` | 流程完成（审批通过） |
| `ON_REJECTED` | 流程被驳回 |
| `ON_CANCELED` | 流程被取消 |
| `ON_TASK_CREATED` | 任务创建 |
| `ON_TASK_COMPLETED` | 任务完成 |
| `ON_TASK_ASSIGNED` | 任务被分配 |

### `FlowEventContext`

流程事件上下文，传递给 `@FlowCallback` 方法：

| 字段 | 说明 |
|------|------|
| `event` / `eventType` | 事件类型 |
| `businessKey` | 业务主键 |
| `processInstanceId` | 流程实例 ID |
| `processDefKey` | 流程定义编码 |
| `title` | 流程标题 |
| `startUserId` / `applyUserId` | 申请人 ID |
| `startUserName` / `applyUserName` | 申请人姓名 |
| `eventTime` | 事件时间 |
| `lastComment` | 最新审批意见 |
| `taskId` | 任务 ID |
| `taskName` | 任务名称 |
| `assigneeId` | 处理人 ID |
| `assigneeName` | 处理人姓名 |
| `comment` | 审批意见 |
| `variables` | 流程变量 |

## REST API 封装

`FlowClient` 封装了以下流程服务接口调用：

### 流程操作

| 方法 | 说明 |
|------|------|
| `startProcess()` | 启动流程 |
| `getProcessStatus()` | 查询流程状态 |
| `terminateProcess()` | 终止流程 |
| `withdrawProcess()` | 撤回流程 |
| `getProcessVariables()` | 获取流程变量 |
| `updateProcessVariables()` | 更新流程变量 |

### 任务操作

| 方法 | 说明 |
|------|------|
| `approve()` | 审批通过 |
| `reject()` | 驳回 |
| `delegate()` | 转交任务 |
| `claimTask()` | 签收任务 |
| `remind()` | 催办 |

### 查询

| 方法 | 说明 |
|------|------|
| `getTodoTasks()` | 待办任务列表 |
| `getDoneTasks()` | 已办任务列表 |
| `getStartedTasks()` | 我发起的流程 |
| `getProcessComments()` | 获取审批意见列表 |
| `getProcessDiagram()` | 获取流程图 |
| `getModelList()` | 获取流程模型列表 |

## 动态 Token

`FlowTokenProvider` 接口支持动态获取认证 Token：

```java
@Component
public class MyFlowTokenProvider implements FlowTokenProvider {
    @Override
    public String getToken() {
        // 从配置中心/缓存中获取动态 Token
        return configService.getFlowToken();
    }
}
```

## 核心组件

| 组件 | 说明 |
|------|------|
| `FlowClient` | 流程服务 HTTP 客户端 |
| `FlowStartAspect` | `@FlowStart` AOP 切面 |
| `FlowEventSubscriber` | Redis Pub/Sub 事件订阅，路由到 `@FlowCallback` 方法 |
| `FlowClientHelperAutoConfiguration` | 辅助组件自动配置 |
| `FlowResult<T>` | 流程服务统一响应体 |
| `FlowClientException` | 流程客户端自定义异常 |
