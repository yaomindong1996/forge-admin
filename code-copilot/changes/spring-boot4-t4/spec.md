# Spring Boot 4 T4：异步图像与分布式原子边界

> status: local-validation-complete-awaiting-runtime-acceptance
> created: 2026-10-12

## 基线与授权

继续 `codex/spring-boot4-t0-compatibility`；main、ForgePro、用户无关改动不动。
复用 T3 的全量测试与构建记录，本轮只开发/本地隔离验证，不连接真实数据库、Redis、模型或对象存储，
不部署、不提交/push。日常开发 revision 保持 1.2.0，CHANGELOG 归入 Unreleased。

## 实现范围与契约

1. 图像生成拆分请求入口、独立有界 Spring 线程池、任务执行、受控下载与文件存储。
   只向工作线程传递记录 ID 与可信用户/租户/数据源上下文，不传 HTTP 请求、Token 或解密密钥。
   工作线程重读记录、启用模型和供应商；请求不能注入用户、租户、状态或结果文件。
   保持 POST 返回生成记录 ID 和 GET 轮询结果的协议；Agent 只能返回已提交任务信息，不能把记录 ID 冒充图片 fileId。
   保持 pending → generating → success/failed；队列饱和持久化安全失败，异常正文/签名 URL/密钥不落日志或 errorMsg。
2. URL 下载复用现有 SecureOutboundClient / EXTERNAL_CONNECTOR 场景，遵守白名单、DNS/IP/重定向校验，
   设置 3 秒 HTTP 连接/读写/调用超时和有界响应；base64 在解码前后限额，验证 MIME、文件头与图片尺寸，
   拒绝 SVG/HTML/伪图片。系统 DNS 解析仍使用平台既有实现，不把配置的 HTTP 超时说成 DNS 严格截止时间。
   不新增默认域名授权或私网例外，不关闭 TLS 校验；需下载 CDN 的维护者自行配置既有出站白名单。
   文件通过现有 FileManager 标记私有并提供已知大小，不把生成图片作为公开访问资产。
3. Agent 初始/恢复会话改为 Redis 单 key Lua 原子占用，唯一 owner nonce、TTL、定时续租和条件释放。
   占用成功前不建会话/读历史/写消息/消费确认；客户端断连不能提前释放。
   续租失败或失去所有权即取消模型流并在后续模型/工具边界失败关闭；旧节点不能释放新节点的占用。
   停止请求按可信用户/租户设置 Redis 取消标记，其他节点可停止实际持有节点的循环。
   Redis 不可用不能降级为单机 Map。旁路 SSE 仍是节点内传输，需要粘性路由；本轮不新增跨节点事件总线。
4. Token 保留现有 hash/status 码；生成和消费用 Lua 原子设置 TTL。
   消费只有 UNUSED 且未过期可转 CONSUMED，缺失/未知/已消费拒绝，不创建幽灵 Token。
   TOKEN_REQUIRED 直接原子领取再调用原 delegate，修复请求头读取；validate 接口仅作提示，不能作执行许可。
   已消费不能在业务失败后重置，维持原至多一次语义；不得记录 Token 正文。
   修正注解默认 idempotent: 与生成接口默认 global 的命名空间不一致，两者统一为 global 别名。
   已有显式 idempotent: Token 为短期运行态，更新后重新申请；自定义 TokenService 需实现新增原子方法。

## 状态 / 安全审查

用户已授权本轮上述安全及状态收敛。没有新增角色授权、客户端委托、数据库结构或默认出站权限。
图像查询结果只接受当前可信所有者；列表保持既有 ai:image:page 管理查看权限、租户隔离与用户筛选。
分布式 lease 防止正常并发占用；Redis 故障/主从切换、进程暂停和已经发出的外部副作用仍需幂等业务动作，
不能宣称仅靠 TTL 达到跨系统 exactly-once。真实环境验收和上线人工审查依然独立。

图像生成查询权限沿用现有 ai:image:generate，不自动给 Agent 用户新增页面/模型管理或 MCP 客户端权限。
执行队列是有界进程内队列，不宣称具有节点崩溃后的持久化重试能力。

## 验证

复用既有 Mockito、Spring 装配、loopback/H2 测试基座；新增两节点共享原子 fixture、租约 TTL/ABA/续租失败/取消、
实际 Token handler 竞争、图像任务线程/上下文/失败/轮询及出站负面测试。Mock Lua 只证明代码边界，
不能称作真实 Redis 验证。串行运行 Maven 相关 Reactor 后全量干净打包，保留失败历史和未验证项。

最终本地证据：53 模块 clean package、3,456 项后端用例及 2,279 项 PC 用例通过，0 失败/错误/跳过。
其中本轮新增 59 项后端用例；原 T3 的 H5/Report/交付工具证据保持独立，不称作 T4 重新执行。
详见 execution-log 与 compatibility-report。
