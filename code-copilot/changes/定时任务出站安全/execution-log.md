# 定时任务出站安全执行记录

> version: V8
> status: complete
> baseline-created: 2026-07-20

## 2026-07-20 实施启动

- 变更范围：V8 平台级出站安全 Starter、白名单管理、Flow Webhook 接入和增量测试。
- 基线：复用 V7 Job `137/137`、前端 `463/463`、Admin Reactor `42/42` 成功记录。
- 门禁：独立 Starter；JOB_WEBHOOK 永不允许私网；FLOW_API 私网例外仅平台管理员显式授权。
- 风险：工作区包含 V1-V7 未提交成果，本阶段只叠加 V8，不回退、不创建 Git 提交。
- 服务：本轮不启动真实 MySQL、Admin、Flow 或外部 HTTP 服务。

## 2026-07-20 目标测试与聚合装配

### Starter 与 Flow 聚合测试

```bash
cd forge-server
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn -Penable-tests \
  -pl forge-framework/forge-starter-parent/forge-starter-outbound,forge-framework/forge-plugin-parent/forge-plugin-flow \
  -am test
```

- 结果：`26/26` Reactor 模块成功，`BUILD SUCCESS`。
- Starter：`47/47` 通过，Failures `0`、Errors `0`、Skipped `0`。
- Flow 插件：`12/12` 通过，Failures `0`、Errors `0`、Skipped `0`。
- 预期日志：Flow Webhook 非 2xx 用例第一次返回 `503` 并输出一次 WARN，第二次重试成功；这是故障注入预期结果，不是测试失败。

### 两条服务装配路径

```bash
cd forge-server
mvn -pl forge-flow/forge-flow-server -am package -DskipTests
```

- 结果：`32/32` Reactor 模块成功，`BUILD SUCCESS`。

```bash
cd forge-server
mvn -pl forge-admin-server -am package -DskipTests
```

- 结果：`43/43` Reactor 模块成功，`BUILD SUCCESS`。

## 2026-07-21 最终静态门禁与安全复核

```bash
rg -n '\$\{[^}]+\}' forge-server/db/migration
xmllint --noout forge-server/forge-framework/forge-starter-parent/forge-starter-outbound/src/main/resources/mapper/SysOutboundWhitelistMapper.xml
rg -n 'new RestTemplate|HttpClient\.new|new OkHttpClient|openConnection' forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main
rg -n 'X-Inner-Call|url=\{\}|Authorization|Cookie|Secret' \
  forge-server/forge-framework/forge-starter-parent/forge-starter-outbound/src/main \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-flow/src/main/java/com/mdframe/forge/starter/flow/event/FlowWebhookNotifier.java \
  forge-server/forge-framework/forge-plugin-parent/forge-plugin-system/src/main/java/com/mdframe/forge/plugin/system/controller/OutboundWhitelistController.java
git diff --check
```

- Flyway placeholder：无匹配，`rg` 按无匹配语义返回 `1`，符合预期。
- Mapper XML：`xmllint` 返回 `0`，无语法错误。
- Flow 直连旁路：无匹配，Flow 生产源码只通过 `SecureOutboundClient` 出站。
- 敏感日志关键字：V8 生产源码无匹配；复核确认日志只记录 `scheme://host:port`、场景和状态，不记录 path、query、Header 或正文。
- 仓库调用点复扫：结果与 `outbound-callsite-audit.md` 一致，V8 范围外的 External Proxy、OAuth2 和 Flow Client 已登记为后续独立治理项。
- `git diff --check`：返回 `0`，无空白错误。
- 安全复核：自动配置、平台管理员双门禁、权限资源不自动授权角色、逻辑删除、精确白名单、DNS 二次校验和逐跳重定向均符合 Spec。

## 跳过项与环境状态

- 未启动真实 MySQL、Admin、Flow 或外部 HTTP 服务；本轮新增服务 PID：无。
- 未执行真实 Flyway、真实公网/私网 DNS 和 Webhook E2E，由用户侧环境验收。
- 未迁移 V8 Spec 范围外的直接 HTTP 客户端，调用点已经审计登记且不属于定时任务/流程模型 URL 旁路。
- 未修改或清理 V1-V7 未提交成果，未创建 Git 提交。
