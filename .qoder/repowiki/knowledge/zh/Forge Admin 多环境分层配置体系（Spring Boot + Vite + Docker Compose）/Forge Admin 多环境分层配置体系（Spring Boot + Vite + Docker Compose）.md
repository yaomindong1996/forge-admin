---
kind: configuration_system
name: Forge Admin 多环境分层配置体系（Spring Boot + Vite + Docker Compose）
category: configuration_system
scope:
    - '**'
source_files:
    - forge-server/forge-admin-server/src/main/resources/application.yml
    - forge-server/forge-app-server/src/main/resources/application.yml
    - forge-server/forge-flow/forge-flow-server/src/main/resources/application.yml
    - forge-server/forge-report-server/src/main/resources/application.yml
    - forge-server/forge-admin-server/src/main/resources/application-dev.example.yml
    - docker-forge-admin/docker-compose.yml
    - docker-forge-admin/application-datasource.yml
    - forge-admin-ui/.env.example
    - forge-admin-ui/vite.config.js
    - forge-h5-ui/vite.config.js
    - forge-admin-ui/src/settings.js
    - forge-h5-ui/src/settings.js
    - forge-server/forge-framework/forge-starter-parent/forge-starter-config/src/main/resources/sql/config_group_table.sql
---

## 1. 整体方案

本仓库采用 **Spring Boot Profile + Maven 资源过滤 + 环境变量注入** 的后端配置体系，配合 **Vite `loadEnv` + `.env.*` 文件** 的前端配置体系，并通过 **Docker Compose 的 `.env` + 挂载 `application-prod.yml`** 实现生产环境的集中化、可覆盖式部署。所有服务共享统一的命名约定与加载顺序。

后端四个 Spring Boot 应用（forge-admin-server / forge-app-server / forge-flow / forge-report-server）均遵循同一模式：
- 根级 `application.yml` 定义默认值与公共结构（server/undertow、logging、spring、mybatis-plus、sa-token、forge.* 业务开关等）；
- 每个模块提供 `application-dev.example.yml` 作为开发环境数据源/Redis/MCP/Crypto 示例；
- 通过 `spring.profiles.active: @profiles.active@` 由 Maven `profiles.active` 在构建期替换为 `dev`/`prod` 等；
- 运行时通过 `application-{profile}.yml` 或外部 `-Dspring.config.location` 覆盖。

前端三个应用（forge-admin-ui / forge-h5-ui / forge-report-ui）统一使用 Vite 的 `loadEnv(mode, process.cwd())` 读取 `.env`、`.env.development`、`.env.production`、`.env.test` 中的 `VITE_*` 变量，并在 `vite.config.js` 中映射到代理目标、端口、路由前缀等。

## 2. 关键文件

- 后端核心配置
  - `forge-server/forge-admin-server/src/main/resources/application.yml`：Admin 服务默认配置，含 Flyway、AI/MCP、Capability、Crypto、Auth、Business 等大量 `FORGE_*` 环境变量开关。
  - `forge-server/forge-app-server/src/main/resources/application.yml`：App 服务默认配置。
  - `forge-server/forge-flow/forge-flow-server/src/main/resources/application.yml`：Flow 服务默认配置，含 Flowable、多租户、事件回调线程池等。
  - `forge-server/forge-report-server/src/main/resources/application.yml`：Report 服务默认配置。
  - `forge-server/forge-admin-server/src/main/resources/application-dev.example.yml`：开发数据源、Redis、MCP/Crypto Pepper 说明注释。
  - `docker-forge-admin/application-datasource.yml`：生产挂载的 `application-prod.yml`，仅暴露 MySQL/Redis/Flowable 连接参数，全部走 `${MYSQL_*}`、`${REDIS_*}` 环境变量。

- Docker Compose 编排
  - `docker-forge-admin/docker-compose.yml`：定义 mysql、redis、forge-flow、forge-admin、forge-ui 五服务，通过 `environment:` 注入数据库、Redis、JAVA_OPTS、`FORGE_CRYPTO_*` 等密钥相关变量，并将 `application-datasource.yml` 挂载为 `/app/application-prod.yml`。
  - `docker/.env.example`（同目录存在 `.env.example`）：提供 `MYSQL_*`、`REDIS_*`、`FLOW_PORT`、`ADMIN_PORT`、`NGINX_PORT`、`JAVA_OPTS` 等默认值。

- 前端配置
  - `forge-admin-ui/.env.example`：`VITE_TITLE`、`VITE_API_BASE_URL`、`VITE_PUBLIC_PATH`、`VITE_USE_HASH`、`VITE_HOME_PATH`、`VITE_H5_BASE_URL`、SSO 桥接配置、`VITE_APP_ID` 等。
  - `forge-admin-ui/vite.config.js`：通过 `loadEnv` 读取 `VITE_HTTP_PORT`、`VITE_REQUEST_PREFIX`、`VITE_PUBLIC_PATH`、`VITE_HTTP_PROXY_TARGET`、`VITE_FLOW_PROXY_TARGET`、`VITE_OUT_DIR`，并配置 dev server 代理与构建输出。
  - `forge-h5-ui/vite.config.js`：同样基于 `VITE_*` 变量配置 uni-app 代理（flow-server、app-server、ws）。
  - `forge-admin-ui/src/settings.js`、`forge-h5-ui/src/settings.js`：前端运行时 UI 布局/主题默认值。

- 框架 Starter（运行时动态配置能力）
  - `forge-server/forge-framework/forge-starter-parent/forge-starter-config/`：提供 `config_group_table.sql`、`config_properties.sql`，用于将部分配置项持久化到数据库并通过 API 动态刷新（如水印字典、Excel 导出配置等），是“代码即配置”向“运行时配置中心”的延伸。

## 3. 架构与约定

### 3.1 后端配置分层
| 层级 | 来源 | 作用 | 覆盖优先级 |
|---|---|---|---|
| 默认层 | `application.yml` | 各服务的通用默认值（端口、日志、MyBatisPlus、Sa-Token、forge.* 开关） | 最低 |
| Profile 层 | `application-{profile}.yml`（构建期由 `@profiles.active@` 选择） | 按环境切换数据源、Redis、Flyway 位置等 | 中 |
| 环境变量层 | `FORGE_*`、`spring.data.redis.*`、`MYSQL_*`、`REDIS_*` 等 | 敏感信息、实例差异、运行时开关 | 高 |
| 启动参数层 | `-Dspring.config.location` / JVM 参数 | 最高优先级覆盖 | 最高 |

### 3.2 环境变量命名规范
- 所有业务开关统一以 `FORGE_` 前缀（如 `FORGE_MCP_ENABLED`、`FORGE_CAPABILITY_OPEN_GATEWAY_ENABLED`、`FORGE_AUTH_LEGACY_CLIENT_SECRET_READ_ENABLED`、`FORGE_FLOW_JOB_REMOTE_ENABLED`、`FORGE_JOB_OPEN_API_ENABLED`）。
- 安全相关（Pepper、KEK、Token TTL、OIDC issuer/resource）集中在 `forge.crypto.*`、`forge.capability.identity.*`、`forge.capability.open-gateway.*` 下，并通过 `@ConfigurationProperties` 绑定。
- 数据库/缓存连接统一通过 `spring.datasource.*`、`spring.data.redis.*` 暴露，再由 Sa-Token、Dynamic-Datasource、Redisson 引用。

### 3.3 前端配置分层
- `.env`：基础常量（标题、API 前缀、publicPath）。
- `.env.development` / `.env.production` / `.env.test`：按 Vite `mode` 叠加，决定代理目标、端口、是否 hash 路由等。
- `settings.js`：UI 布局、主题色等运行时可热更新的展示配置。
- `vite.config.js`：构建期读取 `VITE_*`，生成 dev server proxy 规则与 build outDir。

### 3.4 容器化部署约定
- 通过 `docker-forge-admin/docker-compose.yml` 一键拉起，MySQL/Redis 健康检查后启动 flow → admin → ui。
- 生产配置文件通过 volume 挂载 `./application-datasource.yml:/app/application-prod.yml`，避免把密码写进镜像。
- 加密密钥（Pepper、KEK）通过 `FORGE_CRYPTO_BOOTSTRAP_FILE` 指向持久卷 `/var/lib/forge/secrets/crypto.properties`，支持多实例共享。

## 4. 约定与约束

- **Profile 激活方式**：所有后端服务通过 `spring.profiles.active: @profiles.active@` 由 Maven profile 注入，禁止在 `application.yml` 中硬编码固定 profile。
- **敏感配置必须走环境变量**：数据库密码、Redis 密码、`FORGE_CRYPTO_SECRET_KEY`、`FORGE_CAPABILITY_*_PEPPER`、`FORGE_CAPABILITY_OIDC_*` 等均通过 `${VAR:default}` 形式从环境变量注入，不得明文写入 `application.yml`。
- **功能开关默认关闭原则**：MCP (`FORGE_MCP_ENABLED:false`)、Open Gateway (`FORGE_CAPABILITY_OPEN_GATEWAY_ENABLED:true` 但受限于 pepper)、Secure Actions、High Risk 等均以 `false` 或受限默认值启动，需显式开启。
- **Flyway 迁移路径**：默认扫描 `filesystem:./db/migration,filesystem:../db/migration,filesystem:forge-server/db/migration`，容器内通过 volume 挂载 `../forge-server/db/migration` 保证迁移脚本一致。
- **前端代理规则**：`/api/flow`、`/ai/business/flow` 必须优先于通用 `/api` 代理，否则会被转发到错误服务（见 H5 vite.config 注释）。
- **运行时动态配置**：通过 `forge-starter-config` 提供的数据库表（`config_group_table`、`config_properties`）和 API 进行运行时修改，替代重启改配置的方式。
- **日志级别**：`com.forge` 包日志级别通过 Maven `@logging.level@` 占位符注入，便于不同 profile 差异化控制。
