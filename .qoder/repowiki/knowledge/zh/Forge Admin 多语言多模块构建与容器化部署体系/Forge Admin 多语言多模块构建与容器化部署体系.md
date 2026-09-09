---
kind: build_system
name: Forge Admin 多语言多模块构建与容器化部署体系
category: build_system
scope:
    - '**'
source_files:
    - forge-server/pom.xml
    - docker/docker-compose.yml
    - docker-forge-admin/docker-compose.yml
    - docker/Dockerfile.admin
    - docker/Dockerfile.flow
    - docker/Dockerfile.ui
    - forge-admin-ui/vite.config.js
    - forge-admin-ui/package.json
    - forge-admin-ui/deploy/index.js
    - forge-admin-ui/.env.example
    - forge-report-ui/Makefile
    - forge-server/scripts/db/init-db.sh
    - docker-forge-admin/application-datasource.yml
---

## 1. 使用的构建系统与工具

仓库是一个前后端混合、多服务聚合的工程，采用 **Maven + pnpm/Vite** 双构建体系：
- 后端：基于 `forge-server/pom.xml` 的 Maven 聚合工程，包含 `forge-admin-server`、`forge-app-server`、`forge-flow`、`forge-report-server`、`forge-business` 以及 `forge-framework`（starter-parent / plugin-parent / dependencies）等多个子模块。
- 前端：三个独立的前端应用——`forge-admin-ui`（Vue3 + Vite + Naive UI）、`forge-h5-ui`（uni-app + Vue3）、`forge-report-ui`（Vue3 + Vite），均使用 pnpm 管理依赖，通过各自的 `package.json` scripts 驱动构建。
- 容器化：提供两套 Docker Compose 编排（`docker/` 基础版、`docker-forge-admin/` 可挂载数据源版本），分别对应不同的生产部署场景。
- 数据库初始化：通过 `forge-server/scripts/db/init-db.sh` 脚本统一执行全量 SQL、seed 数据和可选模块 SQL。

## 2. 关键文件与位置

| 类别 | 关键文件 | 作用 |
|---|---|---|
| Maven 聚合 | `forge-server/pom.xml` | 定义 Java 17、Spring Boot 3.5.13、Flyway、MyBatis-Plus 等版本；声明 local/dev/prod profile；配置 flatten-maven-plugin 输出扁平 POM；集中声明阿里云/华为云/Maven Central/Spring Milestones 仓库 |
| 后端 Dockerfile | `docker/Dockerfile.admin`、`docker/Dockerfile.flow`、`docker/Dockerfile.ui` | 多阶段构建：builder 用 eclipse-temurin:17-jdk 执行 `mvn clean package -pl forge-admin -am -DskipTests -Pprod`，runtime 用 JRE 镜像运行 jar |
| Compose 编排 | `docker/docker-compose.yml`、`docker-forge-admin/docker-compose.yml` | 编排 MySQL 8.0、Redis 7、forge-flow（先启动）、forge-admin、nginx 前端；通过 `.env` 注入密码、端口、FLOW_CLIENT_URL、加密密钥等 |
| 前端构建 | `forge-admin-ui/vite.config.js`、`forge-admin-ui/package.json` | Vite 插件链（unplugin-vue-router、unocss、auto-import、components、devtools）；开发代理 `/api` 到后端、`/api/flow` 和 `/api/workspace` 到 flow 服务；构建使用 oxc 压缩、esbuild 压缩 CSS |
| 前端部署脚本 | `forge-admin-ui/deploy/index.js` | 基于 ssh2-sftp-client 将本地 `dist` 上传至远端服务器，支持备份、清理、统计 |
| 报表前端 Makefile | `forge-report-ui/Makefile` | 封装 `make dev/dist/view/lint/new` 命令调用 npm scripts |
| 数据库初始化 | `forge-server/scripts/db/init-db.sh` | 创建 `forge_admin` 库（utf8mb4_0900_ai_ci），顺序执行 `全量初始化SQL.sql` → `seed/required` → 可选 `module`/`demo`/`optional` |
| 环境变量 | `forge-admin-ui/.env.example`、`docker/.env.example`、`docker-forge-admin/application-datasource.yml` | 前端 VITE_* 变量、Compose 环境变量、可覆盖的数据源配置 |

## 3. 架构与约定

### 3.1 后端构建流程
- 根 POM 通过 `<modules>` 聚合所有子模块，默认激活 `dev` profile，日志级别 info；`prod` profile 将日志级别改为 warn。
- 编译阶段启用 Lombok、MapStruct-Plus、Therapi Runtime Javadoc、Spring Configuration Processor 注解处理器，并强制 `-parameters` 参数保留。
- 单元测试通过 `maven-surefire-plugin` 执行，默认跳过（`forge.tests.skip=true`），需显式传入 `-Penable-tests` 才运行测试，且支持按 `@Tag` 分组执行。
- 版本号统一通过 `${revision}` 属性 + `flatten-maven-plugin` 在 `process-resources` 阶段生成扁平 POM，clean 时清理。
- 资源过滤仅对 `application*`、`bootstrap*`、`banner*` 启用，其他资源不过滤，避免误替换。
- 仓库源优先级：spring-milestones → spring-snapshots → central → aliyun → huawei。

### 3.2 前端构建流程
- `forge-admin-ui`：`pnpm dev` 启动 Vite 开发服务器，`pnpm build` 或 `pnpm build:prod` 构建生产包；构建产物输出到 `dist`（可通过 `VITE_OUT_DIR` 覆盖）。
- 开发模式通过 Vite proxy 将请求转发到后端：`/api` → `VITE_HTTP_PROXY_TARGET`，`/api/flow` 和 `/api/workspace` → `VITE_FLOW_PROXY_TARGET`，WebSocket `/ws` 也代理到同一目标。
- 构建优化：关闭 sourcemap、关闭 gzip 体积上报、使用 oxc 作为 minifier、esbuild 压缩 CSS、预构建大型依赖（bpmn-js、echarts、form-create 等）以降低首次加载时间。
- `forge-h5-ui` 与 `forge-report-ui` 各自维护独立的 vite.config 与 package.json，遵循相同的 pnpm + Vite 模式。
- `forge-report-ui` 额外提供 `Makefile` 统一入口，便于习惯 make 的用户操作。

### 3.3 容器化与部署
- 两套 Compose 方案：
  - `docker/`：将 `forge/forge-admin/sql` 挂载为 `/初始化脚本.sql`，由 MySQL 命令参数直接导入；适合快速体验。
  - `docker-forge-admin/`：将 `init-sql/` 挂载到 `/docker-entrypoint-initdb.d`，并通过 volume 挂载 `application-datasource.yml` 和 `../forge-server/db/migration`，支持 Flyway 增量迁移。
- 服务启动顺序严格约束：mysql → redis → forge-flow → forge-admin → forge-ui（Nginx）。每个服务都定义了 healthcheck。
- 后端通过环境变量注入数据库、Redis、Flow 客户端地址及 Forge 加密密钥持久化相关开关（`FORGE_CRYPTO_*`）。
- Nginx 静态资源路径为 `/forge`，访问入口为 `http://localhost/forge`。

### 3.4 数据库初始化约定
- 字符集统一为 `utf8mb4`，collation 为 `utf8mb4_0900_ai_ci`（MySQL 8.0）。
- 初始化脚本顺序固定：全量 SQL → required seed → （可选 module/demon/optional）。
- 支持 `--skip-admin-init` 跳过主初始化脚本，便于只执行 seed 或模块脚本。

## 4. 约定与约束

- **Java 版本锁定**：所有后端模块统一使用 Java 17（`java.version=17`），通过 Maven compiler plugin 强制 source/target。
- **环境隔离**：后端通过 Maven profile（local/dev/prod）切换配置；前端通过 `.env.development` / `.env.production` / `.env.test` 区分；容器通过 `.env` 注入。
- **测试默认禁用**：除非显式传入 `-Penable-tests`，否则 `forge.compiler.skip` 与 `forge.tests.skip` 均为 true，加速 CI/CD 构建。
- **依赖版本集中管理**：Spring Boot BOM、Spring AI BOM、Alibaba Spring AI Alibaba Extensions BOM、forge-dependencies 均在根 POM 的 `dependencyManagement` 中引入，子模块不指定版本。
- **前端包管理器锁定**：根 `package.json` 通过 `packageManager` 字段锁定 pnpm@11.7.0，各子项目使用 `pnpm-lock.yaml` 锁定依赖树。
- **构建产物输出目录**：前端默认 `dist`，可通过 `VITE_OUT_DIR` 覆盖；后端通过 `mvn package` 输出到 `target/*.jar`。
- **无内置 CI 流水线**：仓库未发现 GitHub Actions / GitLab CI / Jenkinsfile 等 CI 配置文件，但 `forge-report-ui/.workflow/` 下存在分支流水线、master 流水线、PR 流水线 YAML（属于该子模块自有的工作流），说明部分子模块具备独立 CI。
- **发布前校验**：`forge-admin-ui` 提供 `lint:fix` 脚本（eslint --fix）；`forge-report-ui` 提供 `make lint`，用于代码风格检查。
- **数据库变更策略**：结合 Flyway（`flyway.version=10.20.1`）与 `forge-server/db/migration` 目录进行增量迁移，Compose 中将 migration 目录挂载进容器供运行时执行。