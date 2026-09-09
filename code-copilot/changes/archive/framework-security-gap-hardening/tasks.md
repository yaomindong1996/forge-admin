# Framework Security Gap Hardening Tasks

> **For agentic workers:** 按仓库 SDD 工作流内联执行；每个任务先补测试、再实现、再跑目标模块验证。并行 Agent 正在修改 capability 平台，禁止切分支、提交或改动冻结文件。

## 执行状态

- [x] Proposal：完成源码核验、风险校正和并行工作区边界确认。
- [x] Task 1：加固内部调用来源验证与 nonce 原子登记。
- [x] Task 2：实现 WebSocket CONNECT 鉴权、用户队列和 Origin 限制。
- [x] Task 3：租户缺失失败关闭与本地文件路径约束。
- [x] Task 4：关闭密码明文降级、DataScope 错误放行和 Job 固定密钥默认值。
- [x] Task 5：完成自动密钥、幂等键和消息 PARTIAL 状态修复。
- [x] Task 6：处理 WorkerId 与未实现能力的安全收口。
- [x] Task 7：完成目标测试、聚合构建、并行改动隔离检查和文档回填。

## Task 1：内部调用与防重放

**文件：**

- Create: `forge-starter-crypto/.../config/InternalCallProperties.java`
- Create: `forge-starter-crypto/.../support/InternalCallRequestVerifier.java`
- Modify: Crypto 三处 Advice/Filter、`ReplayTokenCache`、`CryptoAutoConfiguration`
- Test: 可信地址/CIDR、伪造 Header、nonce 原子性和两倍 TTL

**步骤：**

- [x] 写失败测试，证明任意远端地址当前可使用 `X-Inner-Call` 跳过。
- [x] 实现统一来源校验器并接入三处跳过点。
- [x] 写失败测试，证明 `exists`/`cache` 非原子且 TTL 只有单窗口。
- [x] 改为原子登记并将 TTL 调整为两倍窗口。
- [x] 运行 Crypto Starter 单测。

## Task 2：WebSocket 鉴权

**文件：**

- Create: WebSocket authentication SPI、properties、channel interceptor
- Modify: `WebSocketConfig.java`、Auth Starter 配置、`SysOnlineUserServiceImpl.java`、Admin `websocket.js`
- Test: CONNECT/SUBSCRIBE/SEND 鉴权和 destination 边界

**步骤：**

- [x] 写失败测试覆盖匿名 CONNECT、无效 Token 和直发 `/topic`。
- [x] 在 Auth Starter 提供 Sa-Token 认证适配器并绑定 Principal。
- [x] 将认证通知从 `/topic/auth` 改为用户队列。
- [x] Admin CONNECT 携带 Bearer Token并订阅用户队列/合法广播。
- [x] 运行 WebSocket、Auth、System 目标测试和 Admin UI 构建。

## Task 3：租户与文件路径

**文件：**

- Modify: `TenantProperties.java`、`DefaultTenantLineHandler.java`
- Modify: `LocalFileStorage.java`
- Test: Tenant handler 与 LocalFileStorage 路径边界

**步骤：**

- [x] 写租户表无上下文失败、忽略表放行、显式 ignore 放行测试。
- [x] 调整 handler 判断顺序并默认启用 strict mode。
- [x] 写普通/分片上传、下载、删除、bucket 的穿越测试。
- [x] 引入统一规范路径 resolver 和符号链接检查。
- [x] 运行 Tenant/File Starter 单测。

## Task 4：认证、DataScope 与 Job 配置

**文件：**

- Modify: Admin/H5 密码加密调用、`UsernamePasswordAuthStrategy.java`
- Modify: `DataScopeProperties.java`、`DataScopeInterceptor.java`
- Modify: `JobProperties.java`、Job 安全配置、Admin `application.yml`
- Test: 密码失败、DataScope fail-closed、Job 缺密钥启动边界

**步骤：**

- [x] 前后端移除明文降级并补失败测试。
- [x] 已配置 DataScope 的上下文/解析异常改为抛出；未配置 mapper 只告警一次并支持 DENY。
- [x] Job Open API 默认关闭、移除 pepper 默认值、启用时启动校验。
- [x] 运行 System/DataScope/Job 测试和 Admin/H5 前端构建。

## Task 5：低风险一致性修复

**文件：**

- Modify: Crypto bootstrap、Idempotent key generator、Message status
- Create: Flyway 字典迁移
- Test: 算法密钥长度、稳定参数摘要、PARTIAL 聚合状态

**步骤：**

- [x] 自动密钥按 SM4/AES 算法生成和校验。
- [x] 幂等参数改为稳定 JSON 序列化后摘要。
- [x] 消息部分失败写入状态 3，并新增 `sys_message_send_status` 字典项。
- [x] 运行三个目标模块测试和 Flyway 静态检查。

## Task 6：剩余技术债安全收口

**文件：** 根据源码验证后的最小范围确定，不修改 capability 模块。

**步骤：**

- [x] WorkerId 先实现可观测阈值/容量告警，不在没有租约协议时盲目复用 ID。
- [x] 流程超时、统计、IP 库、AI 出码未实现能力不得返回伪成功；已有明确异常的 AI 策略保持不变。
- [x] 对需要产品规则或外部基础设施的能力记录部署门禁和剩余任务。

## Task 7：聚合验证与隔离复核

- [x] 执行 `git diff --check`。
- [x] 执行各目标 Maven 模块测试和 Admin 聚合 package。
- [x] 执行 Admin UI build 和 H5 build。
- [x] 对比开始时 capability 文件清单，确认没有由本变更新增的修改。
- [x] 回填 `test-spec.md`、`execution-log.md`、本文件和 Spec 状态。
