# Framework Security Gap Hardening Test Spec

## 1. 增量范围

本轮覆盖 Crypto、WebSocket、Auth、Tenant、File、DataScope、Job、System、Message、Idempotent Starter 及 Admin/H5 密码链路。统一能力开放平台模块仅做聚合编译回归，不修改其源码或测试。

## 2. P1 测试

1. 内部调用：外部 IP + Header 拒绝；loopback、显式 IP、CIDR 允许；转发头不能改变判定。
2. WebSocket：匿名/坏 Token CONNECT 拒绝；合法 Token 建立 Principal；客户端不能 SEND 到 broker；用户队列隔离。
3. Tenant：租户表缺上下文拒绝；忽略表、上下文 ignore、正常 tenantId 行为不回归。
4. File：普通/分片上传、元数据下载删除、bucket 的 `..`、绝对路径、符号链接逃逸全部拒绝。

## 3. P2/P3 测试

1. Replay：原子 nonce 登记；重复值拒绝；TTL 精确为窗口两倍。
2. Password：前端 RSA 失败不发登录；后端 RSA 解密失败抛业务异常。
3. DataScope：配置存在时上下文或 SQL 失败拒绝；未配置 WARN 去重；DENY 拒绝。
4. Job：默认关闭；启用且 pepper 为空/过短时失败；合规配置通过。
5. Crypto/Idempotent/Message：算法密钥长度、稳定 JSON、PARTIAL 状态与字典。

## 4. 目标命令

```bash
cd forge-server
mvn -pl forge-framework/forge-starter-parent/forge-starter-crypto -am test
mvn -pl forge-framework/forge-starter-parent/forge-starter-websocket -am test
mvn -pl forge-framework/forge-starter-parent/forge-starter-tenant -am test
mvn -pl forge-framework/forge-starter-parent/forge-starter-file -am test
mvn -pl forge-framework/forge-starter-parent/forge-starter-datascope -am test
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-job -am test
mvn -pl forge-framework/forge-plugin-parent/forge-plugin-system -am test
mvn -pl forge-admin-server -am package -DskipTests
```

```bash
source ~/.nvm/nvm.sh
nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

## 5. 部署门禁

- 不在本地伪造生产内部调用网段和 Origin；部署前必须提供真实值。
- 不提交真实 pepper、Token 或内部网络信息。
- 不执行真实数据库迁移；Flyway 仅做静态检查和应用启动可达时的增量验证。
- 不停止或清理并行 Agent 启动的进程和工作区改动。

## 6. 2026-08-02 增量验证结果

- 后端 15 个目标测试类共 58 条通过；收尾单独复跑幂等键 2 条、消息聚合 2 条均通过。
- `forge-admin-server -am package -DskipTests` 47 模块聚合构建通过，包含并行 capability 模块的只读编译回归。
- Admin UI 在 Node 20.19.0 下生产构建通过；H5 在补齐清单直接依赖后 `pnpm build:h5` 通过。
- `git diff --check`、冲突标记扫描、Flyway 版本唯一性、新迁移占位符扫描通过；`V1.0.80` 为当前最高版本。
- 全量 Flyway 占位符扫描只命中已提交的 `V1.0.72` 业务模板变量；该历史迁移不得回改，本轮新迁移无占位符。
- 未启动后端、数据库、Redis 或前端开发服务，因此未执行真实接口、WebSocket 握手和数据库迁移验证；这些属于部署前环境回归。
