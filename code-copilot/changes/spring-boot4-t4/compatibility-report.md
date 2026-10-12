# Spring Boot 4 T4 验证与兼容说明

> 2026-10-12；三项代码补齐与本地回归完成，不代表真实环境上线验收。

## 结果

仍在 `codex/spring-boot4-t0-compatibility`，main 未动；没有提交/push 或部署。
发行版本保持 1.2.0，底座 Boot 4.1.1 / AI 2.0.1 / Flowable 8.0.0，变更属于 Unreleased。

| 项目 | 本地验证结果 |
| --- | --- |
| 后端根 Reactor | 53 模块 clean package，3,456 项，0 失败/错误/跳过 |
| 关键模块 | AI 303、幂等 53、出站 48 项 |
| PC | 316 文件 / 2,279 项通过 |
| 本轮新增 | 图像 41、租约 10、Token 8 项，共 59 项 |
| 产物 | Admin / App / Flow / Report JAR 在各模块 target 生成，仅本地验证 |
| 版本与质量 | 1.2.0 版本检查、diff --check 和相关 Java 单类/单行尺寸通过 |

失败过程和可复跑命令在 [execution-log](execution-log.md)。H5、Report UI 和交付工具本轮未修改/重跑，
历史记录见 T3，不能叠加计入本轮通过数量。

## 图像

- 请求返回任务 ID，结果接口轮询 pending/generating/success/failed，成功后读取 resultFileId。
- 工作池独立、有界；显式传递可信用户/租户/DS/MDC，不保留 HTTP 请求或 Token，不在请求线程生成图片。
- 排队后重验当前租户启用的图像模型和供应商；身份、主键、状态、结果等不接受客户端注入。
- 状态用 Mapper XML 的条件更新；重复调度/终态不重发供应商请求，队列拒绝是 failed。
- URL 复用 SecureOutboundClient / EXTERNAL_CONNECTOR 场景，不自动授权 CDN 域名，不支持私网例外。
  新环境需在既有出站白名单配置实际 CDN 的协议、域名和端口。未授权明确失败，不能回退裸 HttpClient。
- PNG/JPEG 的 MIME、完整解码、字节/像素限制生效，SVG/HTML/WebP/GIF 等拒绝；默认 2 MiB / 16M 像素。
  URL 还受平台 forge.outbound.max-response-bytes 更严格的限额，修改图像限额不自动提高平台上限。
- 设置 HTTP 连接/读写/调用超时 3 秒。DNS 采用既有平台系统解析，不承诺慢系统 DNS 严格在 3 秒内中止。
- FileManager 显式私有与已知大小；供应商/存储异常只记录类型，用户错误不包含密钥、签名链接或正文。
- Agent 工具返回 data 型任务信息，不把记录 ID 当 fileId 或伪造已生成图片；GET 仍须既有 ai:image:generate 权限。
  不自动给普通 Agent 用户增加图像页面/模型维护或 MCP 客户端权限。

## 多节点会话

- 会话 key 全局唯一，单 key Lua 原子占用；独立 nonce、TTL、心跳和比较释放，Redis 不可用不降级 Map。
- 建行、读取历史、消息编辑/重试、确认消费之前取得占用；断连不释放，实际后台终止才释放。
- 续租/所有权失败取消模型流，模型/工具/事件边界失败关闭；旧节点不能删除新节点的占用或发布旧终止事件。
- 同用户/租户的停止请求可以写取消标记，持有节点在心跳或下一执行边界停止；跨用户/租户拒绝。
- 心跳池不暴露为通用 TaskScheduler，避免业务 @Scheduled 扫描挤占续租；Spring 生命周期负责启停。
- 默认 TTL 60 秒，心跳 10 秒；TTL 10–300 秒、心跳 1 秒至 TTL/3，非法配置启动时拒绝。

## Token

- 保留 hash 的 UNUSED/CONSUMED 码；生成/TTL 与领取/状态/TTL 分别在单个 Lua 中完成。
- TOKEN_REQUIRED 使用 tryConsumeToken，不走 validate → consume；只有一个请求进入原 delegate。
- validate 只是展示提示；缺失/过期/未知/已消费不生成 key，不延长可用期，业务失败不返还已消费 Token。
- 修正请求上下文转换和可配置请求头；Token/原始幂等键不写日志。
- global、空前缀、注解默认 idempotent: 统一为 global；更新后旧的显式 idempotent: 短期 Token 重新申请。
- 自定义 TokenService 实现必须实现新增 tryConsumeToken；不提供非原子的默认降级实现。

## 可选配置（无凭据）

```yaml
forge:
  ai:
    image:
      core-pool-size: 2
      max-pool-size: 4
      queue-capacity: 16
      max-image-bytes: 2097152
      max-image-pixels: 16777216
  agent:
    session-lease:
      ttl: 60s
      heartbeat-interval: 10s
```

没有新增表或修改历史迁移；升级前需人工审查上述权限、Token 命名空间、格式/限额和出站配置影响。

## 明确保留的边界

1. 本轮共享内存 fixture 不执行 Lua，仅验证原子语义下的代码边界；真实 Redis TTL/CAS/Cluster/故障切换未验证。
2. 旁路 SSE 是节点内分发，重连需要粘性路由；未实现跨节点事件总线。
3. 租约不能撤回已经发出的外部副作用，网络分区/主从切换仍需业务动作幂等，不宣称跨系统 exactly-once。
4. 图像执行队列为进程内队列，节点崩溃后的持久化恢复/重试不是本轮能力；不伪造完成或静默重新计费。
5. 真实 MySQL/Flyway/Flowable、Redis、供应商、COS 的私有访问和 PC/H5 E2E 按用户要求本轮不执行。

Lua 单 key 方案参照 [Redis 官方脚本文档](https://redis.io/docs/latest/develop/programmability/eval-intro/)，
实际实现与本地用例在仓库中；官方语义说明和 Mock 通过均不能替代上述真实验收。

## 后续 T5

[T5](../spring-boot4-t5/compatibility-report.md) 对齐图片查询/结果组件、Agent 错误与确认终态、
SSE Long ID 精度和窄屏滚动边界；本报告保留 T4 当时的结果，不回写为历史已完成。
T5 同样没有执行真实环境验收或扩展现有 MCP/图片查询权限。
