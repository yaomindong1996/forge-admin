# 消息管理模块完善设计文档

**日期**: 2026-04-02
**作者**: AI Assistant
**状态**: 待审核

## 概述

完善现有的消息管理模块，增加消息发送测试功能，增强消息监控能力，方便管理员测试消息发送配置和监控消息发送状态。

## 背景

当前消息管理模块存在以下问题：
1. 消息配置页面只包含短信和邮件配置，缺少测试功能，无法验证配置是否正确
2. 消息列表页面只显示当前用户收到的消息，管理员无法查看所有消息的发送记录和接收人信息
3. 缺少发送失败详情，无法排查问题

## 目标

1. 提供消息发送测试功能，方便管理员测试短信、邮件等配置是否正确
2. 提供管理员消息监控页面，可以查看所有消息的发送记录和接收人信息
3. 显示发送失败详情，方便排查问题

## 需求分析

### 用户需求

**消息发送测试功能**：
- 使用场景：管理员测试短信、邮件等渠道配置是否正确
- 支持渠道：WEB站内信、短信SMS、邮件EMAIL
- 功能：发送少量测试消息，显示发送结果（成功/失败）

**消息监控功能**：
- 使用场景：管理员监控所有消息的发送状态
- 显示信息：
  - 发送记录详情：发送渠道、发送状态、发送时间
  - 接收人列表及状态：接收人用户名、组织、阅读状态
  - 发送失败详情：错误信息

### 访问权限

- 消息发送测试功能：管理员专用
- 消息监控功能：管理员专用
- 现有用户消息页面：保持不变（用户查看自己收到的消息）

## 设计方案

### 整体架构

采用职责分离的设计方案，创建独立的消息管理页面：

**页面划分**：
1. `/system/messageConfig/index` - 消息配置页面（保持不变）
   - 短信配置
   - 邮件配置
   
2. `/message/manage` - 新建消息管理页面（管理员专用）
   - 消息发送测试面板
   - 消息监控列表
   
3. `/message/message-list` - 用户消息页面（保持不变）
   - 显示当前用户收到的消息

**职责划分**：
- 配置页面：配置短信、邮件等渠道参数
- 管理页面：测试配置、监控发送状态、管理所有消息
- 用户页面：普通用户查看和阅读自己的消息

### 前端页面设计

#### 消息管理页面 `/message/manage`

**上半部分：消息发送测试面板**

包含以下字段：
- 发送渠道选择（WEB站内信/短信/邮件）
- 消息标题输入框
- 消息内容输入框（支持富文本编辑器，用于邮件）
- 接收人选择：
  - 用户选择器（支持搜索和选择多个用户）
  - 发送范围选择（全员/指定组织/指定人员）
- 发送按钮
- 发送结果提示（显示成功/失败信息）

**下半部分：消息监控列表**

搜索筛选：
- 消息类型（SYSTEM/SMS/EMAIL/CUSTOM）
- 发送渠道（WEB/SMS/EMAIL/PUSH）
- 发送状态（成功/失败）
- 时间范围（开始时间-结束时间）
- 关键词搜索（标题或内容）

消息列表表格列：
- 消息标题
- 发送渠道
- 发送状态（成功/失败）
- 接收人数量
- 已读数/未读数
- 发送时间
- 操作：查看详情

消息详情弹窗：
- 基础信息：标题、内容、类型、渠道
- 发送记录：发送时间、状态、成功数、失败数、错误信息
- 接收人列表：用户名、组织名、阅读状态、阅读时间

### 后端接口设计

#### 新建控制器

创建 `MessageManageController`，路径：`/api/message/manage`

#### 接口列表

**1. 查询所有消息列表**
```
POST /api/message/manage/page?pageNum=1&pageSize=10
请求参数：MessageManageQueryDTO
{
  "type": "SYSTEM/SMS/EMAIL/CUSTOM",
  "channel": "WEB/SMS/EMAIL/PUSH",
  "status": "1/2",  // 1-成功，2-失败
  "startTime": "2024-01-01",
  "endTime": "2024-12-31",
  "keyword": ""
}
响应：IPage<MessageManageVO>
{
  "records": [...],
  "total": 100
}
```

**2. 查询消息详细信息**
```
GET /api/message/manage/{messageId}/detail
响应：MessageDetailVO
{
  "message": SysMessage,
  "sendRecord": SysMessageSendRecord,
  "receivers": List<ReceiverVO>
}
```

**3. 发送测试消息（复用现有接口）**
```
POST /api/message/send
（已存在，无需新增）
请求参数：MessageSendRequestDTO
响应：SysMessage
```

#### VO 类设计

**MessageManageQueryDTO**
```java
public class MessageManageQueryDTO {
    private String type;          // 消息类型
    private String channel;       // 发送渠道
    private Integer status;       // 发送状态：1-成功，2-失败
    private String startTime;     // 开始时间
    private String endTime;       // 结束时间
    private String keyword;       // 关键词
}
```

**MessageManageVO**
```java
public class MessageManageVO {
    private Long id;
    private String title;
    private String type;
    private String channel;
    private Integer status;           // 发送状态
    private Integer receiverCount;    // 接收人总数
    private Integer readCount;        // 已读数
    private Integer unreadCount;      // 未读数
    private LocalDateTime createTime; // 发送时间
    private String senderName;        // 发送人姓名
}
```

**MessageDetailVO**
```java
public class MessageDetailVO {
    private SysMessage message;           // 消息基础信息
    private SysMessageSendRecord sendRecord; // 发送记录
    private List<ReceiverVO> receivers;    // 接收人列表
}
```

**ReceiverVO**
```java
public class ReceiverVO {
    private Long userId;
    private String userName;    // 用户名
    private String orgName;     // 组织名
    private Integer readFlag;   // 阅读状态：0-未读，1-已读
    private LocalDateTime readTime; // 阅读时间
}
```

### 数据流程设计

#### 发送测试消息流程

1. 用户在前端填写发送表单
2. 前端调用 `/api/message/send` 接口
3. 后端 `MessageService.send()` 方法：
   - 渲染消息内容（如有模板）
   - 创建消息主记录（sys_message）
   - 批量创建接收人记录（sys_message_receiver）
   - 发送到渠道（WEB/SMS/EMAIL）
   - 创建发送记录（sys_message_send_record）
4. 返回发送结果给前端
5. 前端显示发送结果（成功/失败）

#### 查询消息列表流程

1. 管理员进入管理页面
2. 前端调用 `/api/message/manage/page` 接口
3. 后端 `MessageManageService.pageMessages()` 方法：
   - 查询 sys_message 表（按条件筛选）
   - 关联查询 sys_message_receiver 表（统计已读/未读数）
   - 关联查询 sys_message_send_record 表（发送状态）
   - 组装成 MessageManageVO 返回
4. 前端渲染消息列表

#### 查询消息详情流程

1. 管理员点击查看详情按钮
2. 前端调用 `/api/message/manage/{messageId}/detail` 接口
3. 后端 `MessageManageService.getMessageDetail()` 方法：
   - 查询 sys_message 表（消息基础信息）
   - 查询 sys_message_send_record 表（发送记录）
   - 查询 sys_message_receiver 表（接收人列表）
   - 关联查询 sys_user 表（获取用户名）
   - 关联查询 sys_org 表（获取组织名）
   - 组装成 MessageDetailVO 返回
4. 前端在弹窗中显示详情

### 权限控制

- `/api/message/manage/*` 接口需要管理员权限
- `/api/message/send` 接口需要管理员权限（测试功能）
- `/api/message/*` 其他接口保持现有权限（用户可查看自己的消息）

## 数据表说明

现有数据表已满足需求，无需新增表：

**sys_message** - 消息主表
- 包含消息标题、内容、类型、渠道、状态等字段

**sys_message_send_record** - 发送记录表
- 包含发送渠道、接收人数量、成功数、失败数、状态、错误信息等字段

**sys_message_receiver** - 接收人表
- 包含用户ID、组织ID、阅读状态、阅读时间等字段

## 实施计划

### 前端实施任务

1. 创建 `/message/manage` 页面
   - 实现消息发送测试面板
   - 实现消息监控列表
   - 实现消息详情弹窗
   
2. 添加前端路由配置
   
3. 在 `message.js` 中新增 API 调用方法

### 后端实施任务

1. 创建 `MessageManageController`
   
2. 创建 Service 实现：
   - `MessageManageService` 接口
   - `MessageManageServiceImpl` 实现类
   
3. 创建 DTO 和 VO 类：
   - MessageManageQueryDTO
   - MessageManageVO
   - MessageDetailVO
   - ReceiverVO
   
4. 实现查询逻辑：
   - 分页查询所有消息
   - 查询消息详情
   - 统计已读/未读数
   
5. 配置权限控制

## 成功标准

1. 管理员可以在消息管理页面发送测试消息，并能看到发送结果（成功/失败）
2. 管理员可以在消息管理页面查看所有消息的发送记录和接收人信息
3. 发送失败的消息显示错误信息，方便排查问题
4. 现有的消息配置页面和用户消息页面功能保持不变
5. 界面友好，操作流畅

## 风险和限制

### 风险

1. 大量接收人时，查询接收人列表可能性能较慢
   - 解决方案：分页查询接收人列表
   
2. 权限配置需要谨慎，避免普通用户访问管理功能
   - 解决方案：在接口层添加权限验证

### 限制

1. 消息发送测试功能仅支持少量接收人，不适合批量发送
2. 接收人列表的分页查询需要在详情弹窗中实现

## 附录

### 相关文件

**前端文件**：
- `forge-admin-ui/src/views/message/manage.vue` (新建)
- `forge-admin-ui/src/api/message.js` (修改)

**后端文件**：
- `forge-plugin-message/controller/MessageManageController.java` (新建)
- `forge-plugin-message/service/MessageManageService.java` (新建)
- `forge-plugin-message/service/impl/MessageManageServiceImpl.java` (新建)
- `forge-plugin-message/domain/dto/MessageManageQueryDTO.java` (新建)
- `forge-plugin-message/domain/vo/MessageManageVO.java` (新建)
- `forge-plugin-message/domain/vo/MessageDetailVO.java` (新建)
- `forge-plugin-message/domain/vo/ReceiverVO.java` (新建)

### 现有数据表结构

**sys_message**
- id, title, content, type, send_scope, send_channel, status, sender_id, sender_name, template_code, template_params, tenant_id, create_time, update_time

**sys_message_send_record**
- id, tenant_id, message_id, channel, receiver_count, success_count, fail_count, external_id, status, error_msg, send_time, create_time

**sys_message_receiver**
- id, tenant_id, message_id, user_id, org_id, read_flag, read_time, create_time