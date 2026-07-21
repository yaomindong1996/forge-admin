# 定时任务出站安全增量测试计划

> status: complete
> version: V8
> created: 2026-07-20
> completed: 2026-07-21
> baseline: V7 Job `137/137`、前端 `463/463`、Admin Reactor `42/42`

## 1. 本轮差异

- 新增共享出站安全 Starter、白名单表和平台管理员管理 API。
- 新增 URL、IPv4/IPv6、DNS 全量结果和本机地址校验。
- 新增真正参与连接的 OkHttp DNS 绑定、逐跳重定向、超时和响应大小治理。
- Flow Webhook 从直连 RestTemplate 迁移到共享客户端并移除 `X-Inner-Call` 外发 Header。

## 2. P0 验证

| 编号 | 场景 | 预期 |
|---|---|---|
| P0-01 | URL 解析 | 只允许 HTTP/HTTPS，拒绝 userinfo、编码 authority、非法端口和非标准数字主机 |
| P0-02 | IP 分类 | 永久阻断环回、链路本地、元数据、保留、组播、本机和 mapped 绕过 |
| P0-03 | 私网例外 | JOB_WEBHOOK 永不允许私网；FLOW_API 仅显式管理员白名单允许 RFC1918/ULA |
| P0-04 | DNS 全量校验 | 任一 A/AAAA 为 PRIVATE/BLOCKED 时按场景失败关闭，不只取首个地址 |
| P0-05 | DNS 重绑定 | 策略预检为公网、连接前解析变为私网时阻断且不发起连接 |
| P0-06 | 重定向 | 默认阻断；启用后每一跳重新执行白名单、DNS 和 IP 校验并限制次数 |
| P0-07 | 资源上限 | 请求、响应、连接、读取、写入和整体超限均失败关闭 |
| P0-08 | Flow 旁路 | FlowWebhookNotifier 不再创建 RestTemplate/HttpClient，不发送 X-Inner-Call |

## 3. P1 验证

| 编号 | 场景 | 预期 |
|---|---|---|
| P1-01 | 白名单管理 | 只有平台管理员且具备细粒度权限可维护，所有写操作有审计 |
| P1-02 | 数据规则 | 标准审计字段、tenant_id=1、逻辑删除唯一键、精确主机和端口范围 |
| P1-03 | Header 安全 | 拒绝 Host/Content-Length/Connection 等危险 Header，跨源跳转裁剪凭据 |
| P1-04 | 日志安全 | 不记录 Authorization、Cookie、Secret、URL query、请求/响应正文 |
| P1-05 | 聚合装配 | Starter、Flow Server 和 Admin Reactor 编译打包通过 |

## 4. 执行命令

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests -pl forge-framework/forge-starter-parent/forge-starter-outbound,forge-framework/forge-plugin-parent/forge-plugin-flow -am test
```

```bash
mvn -pl forge-flow/forge-flow-server -am package -DskipTests
mvn -pl forge-admin-server -am package -DskipTests
```

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration
xmllint --noout forge-server/forge-framework/forge-starter-parent/forge-starter-outbound/src/main/resources/mapper/SysOutboundWhitelistMapper.xml
rg -n 'new RestTemplate|HttpClient\.new|new OkHttpClient|openConnection' forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main
git diff --check
```

## 5. 跳过项

- 不自动启动真实 MySQL、Admin、Flow 或外部 HTTP 服务；Flyway 实跑、真实公网/私网 DNS 和 Webhook E2E 由用户侧环境验收。
- 不迁移 `forge-plugin-external` 等具有独立业务协议的全部出站能力；本轮记录调用点并只治理 Spec 明确要求的 Flow Webhook。
- 不修改或清理工作区中 V1-V7 的未提交成果。

## 6. 执行结果

| 验证项 | 结果 |
|---|---|
| Starter 单测 | `47/47` 通过：迁移 3、白名单服务 6、策略 6、IP 分类 26、受控客户端 6 |
| Flow 插件单测 | `12/12` 通过：其中 Flow Webhook 2 个用例通过，503 首次失败日志为重试场景预期输出 |
| 聚合测试 Reactor | `26/26` 模块成功，`BUILD SUCCESS` |
| Flow Server 装配 | `32/32` Reactor 模块成功，`BUILD SUCCESS` |
| Admin Server 装配 | `43/43` Reactor 模块成功，`BUILD SUCCESS` |
| 静态门禁 | Flyway placeholder、Mapper XML、Flow 直连旁路、敏感日志关键字和 `git diff --check` 全部通过 |

自动化范围已完成；第 5 节所列真实环境项仍由用户侧验收，不计入自动化通过结论。
