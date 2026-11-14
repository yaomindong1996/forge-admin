# 本地开发与验证

> 从根 `AGENTS.md` 拆出。首次搭建也可对照根 `README.md`。

## 6. 本地开发及验证流程

### 6.1 首次环境搭建

仅用于新建空库，禁止对已有业务库重跑全量 SQL。推荐按 [README 初始化步骤](../../README.md#2-初始化数据库) 使用 `init-db.sh`，统一导入全量 SQL 与 required seed；以下为手工全量导入示例。

`V1.0.0__baseline.sql` 仅含基线注释，不会建表。后续增量迁移由 Admin 启动时执行。Admin 与 Flow 使用同一数据库（模板默认 `forge_admin`），不要另建 Flow 库。

以下命令从仓库根目录执行；服务启动命令分别在不同终端运行。

```bash
# 1. 数据库
mysql -u root -p -e "CREATE DATABASE forge_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u root -p forge_admin < forge-server/db/mysql/全量初始化SQL.sql

# 2. 后端配置
cp forge-server/forge-admin-server/src/main/resources/application-dev.example.yml \
   forge-server/forge-admin-server/src/main/resources/application-dev.yml
# 编辑 application-dev.yml，填入数据库/Redis 连接信息
cp forge-server/forge-flow/forge-flow-server/src/main/resources/application-dev.example.yml \
   forge-server/forge-flow/forge-flow-server/src/main/resources/application-dev.yml
# Flow 使用与 Admin 相同的数据库及 Redis 连接信息

# 3. 前端配置
cp forge-admin-ui/.env.example forge-admin-ui/.env.local
# 如需覆盖开发端口/代理，编辑 forge-admin-ui/.env.development.local

# 4. 启动（每条命令分别运行）
(cd forge-server/forge-admin-server && mvn spring-boot:run)       # 后端 :8580
(cd forge-server/forge-flow/forge-flow-server && mvn spring-boot:run) # 流程 :8081
(cd forge-admin-ui && pnpm install && pnpm dev)                 # 前端 :3000
```

### 6.2 改 → 构建 → 验证闭环

```bash
# === 后端 ===
# 改代码
vim forge-server/forge-framework/forge-plugin-system/src/main/java/.../XxxController.java

# 构建（确认编译通过）
cd forge-server && mvn clean install -DskipTests

# 重启服务
cd forge-server/forge-admin-server && mvn spring-boot:run

# 验证
curl -s http://localhost:8580/xxx/page?pageNum=1&pageSize=10

# === 前端 ===
# 改代码
vim forge-admin-ui/src/views/system/xxx/index.vue

# Lint 检查
cd forge-admin-ui && pnpm lint:fix

# HMR 自动热更新，浏览器验证 http://localhost:3000（以 Vite 输出为准）
```

### 6.3 Token 获取与 API 验证模板

```bash
# 1. 登录获取 Token
curl -s -X POST http://localhost:8580/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | jq '.data.token'

# 2. 用 Token 调用业务接口
TOKEN="<上面获取的token>"

# 分页查询
curl -s http://localhost:8580/system/user/page?pageNum=1&pageSize=10 \
  -H "Authorization: Bearer $TOKEN" | jq .

# 详情查询
curl -s http://localhost:8580/system/user/1 \
  -H "Authorization: Bearer $TOKEN" | jq .

# 新增
curl -s -X POST http://localhost:8580/system/user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"test","nickname":"测试"}' | jq .

# 修改
curl -s -X PUT http://localhost:8580/system/user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":123,"nickname":"新昵称"}' | jq .

# 删除
curl -s -X DELETE http://localhost:8580/system/user/123 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 6.4 日志路径

| 日志 | 路径 |
|------|------|
| 应用日志 | `forge-server/forge-admin-server/logs/` |
| 前端开发日志 | 终端 `pnpm dev` 输出 |
| SQL 日志 | 配置 `spring.datasource.dynamic.datasource.master.log: true`（开发环境） |

---

