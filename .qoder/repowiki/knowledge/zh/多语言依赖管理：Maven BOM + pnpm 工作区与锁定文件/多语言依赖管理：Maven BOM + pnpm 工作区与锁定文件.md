---
kind: dependency_management
name: 多语言依赖管理：Maven BOM + pnpm 工作区与锁定文件
category: dependency_management
scope:
    - '**'
source_files:
    - forge-server/pom.xml
    - forge/forge-framework/forge-dependencies/pom.xml
    - package.json
    - forge-admin-ui/package.json
    - forge-admin-ui/pnpm-lock.yaml
    - forge-admin-ui/pnpm-workspace.yaml
    - forge-h5-ui/package.json
    - forge-report-ui/package.json
---

## 1. 使用的系统/方法

本仓库是一个前后端混合工程，依赖管理分为两条主线：

- **后端（Java/Spring Boot）**：采用 Maven 聚合工程 + `dependencyManagement` + BOM 的方式统一管理版本。根 POM `forge-server/pom.xml` 通过 `<dependencyManagement>` 集中声明 Spring Boot、Spring AI Alibaba、Flyway、MyBatis-Plus、Hutool、Redisson、Flowable、MapStruct-Plus 等所有第三方库的版本号，子模块仅声明 groupId/artifactId 而不写 version。
- **前端（Vue3 / uni-app / Vite）**：统一使用 **pnpm** 作为包管理器，每个前端子项目（`forge-admin-ui`、`forge-h5-ui`、`forge-report-ui`）各自维护独立的 `package.json` 与 `pnpm-lock.yaml` 锁定文件；根目录也有一份顶层 `package.json`（仅用于脚手架脚本）和 `pnpm-lock.yaml`。

## 2. 关键文件

| 领域 | 关键文件 | 作用 |
|---|---|---|
| Maven 聚合根 | `forge-server/pom.xml` | 定义 Java 版本、全局属性、`dependencyManagement`、仓库镜像（阿里云/华为云/spring-milestones/spring-snapshots）、插件仓库 |
| 框架依赖聚合 | `forge/forge-framework/forge-dependencies/pom.xml` | 框架内部依赖的集中声明（当前目录为空占位，实际版本由父 POM 管理） |
| Admin 前端 | `forge-admin-ui/package.json`、`forge-admin-ui/pnpm-lock.yaml`、`forge-admin-ui/pnpm-workspace.yaml` | 依赖声明、锁定、构建时跳过特定 native addon |
| H5 移动端 | `forge-h5-ui/package.json`、`forge-h5-ui/pnpm-lock.yaml` | uni-app 多端依赖 |
| 报表前端 | `forge-report-ui/package.json`、`forge-report-ui/pnpm-lock.yaml` | 可视化编辑器依赖 |
| 顶层脚手架 | `package.json` | 声明 `packageManager: "pnpm@11.7.0+sha512..."` 固定 pnpm 版本 |
| Git 忽略 | `forge-admin-ui/.gitignore` | 显式忽略 `node_modules/`、`package-lock.json`、`yarn.lock`，只保留 pnpm 产物 |

## 3. 架构与约定

### 后端 Maven 依赖管理
- **版本集中化**：所有第三方库版本号集中在 `forge-server/pom.xml` 的 `<properties>` 中（如 `spring-boot.version=3.5.13`、`mybatis-plus.version=3.5.7`、`hutool.version=5.8.31`、`redisson.version=3.50.0`、`flowable.version=7.0.1`、`flyway.version=10.20.1`），子模块通过 `<dependencyManagement>` 引入 `com.mdframe.forge:forge-dependencies:1.0.0` 以及 Spring Boot、Spring AI Alibaba 的 BOM 来继承版本。
- **多仓库镜像**：默认启用阿里云 `maven.aliyun.com/repository/public` 和华为云 `mirrors.huaweicloud.com/repository/maven/` 作为主镜像，同时保留 Spring Milestones/Snapshots 仓库以支持 Spring AI Alibaba 1.1.x 正式版。
- **Flatten 插件**：使用 `flatten-maven-plugin` 在 `process-resources` 阶段生成扁平化 POM，便于 CI 消费。
- **Profile 隔离**：通过 `local/dev/prod` profile 控制日志级别与测试开关，`enable-tests` profile 可开启编译与测试。

### 前端 pnpm 依赖管理
- **每应用独立 lockfile**：三个前端子项目各自拥有 `pnpm-lock.yaml`，不共享 workspace 级别的依赖图，避免跨应用依赖冲突。
- **严格锁定 pnpm 版本**：根 `package.json` 通过 `packageManager` 字段锁定 pnpm 版本为 `11.7.0+sha512...`，确保团队与 CI 使用同一版本解析器。
- **构建期 native 模块白名单**：`forge-admin-ui/pnpm-workspace.yaml` 通过 `allowBuilds` 允许 `esbuild`、`vue-demi` 编译，通过 `ignoreBuilds` 跳过 `@carbon/icons`、`@parcel/watcher`、`core-js-pure`、`cpu-features`、`es5-ext`、`ssh2` 等含原生代码的包，加速安装并规避平台差异。
- **Git 追踪策略**：`.gitignore` 显式忽略 `node_modules/`、`package-lock.json`、`yarn.lock`，只提交 `pnpm-lock.yaml` 作为唯一锁定源。

## 4. 约定与约束

- **禁止在子模块直接指定第三方库版本**：后端子模块应通过父 POM 的 `<dependencyManagement>` 继承版本，不得重复声明 `<version>`。
- **新增第三方库需先在根 POM 的 `<properties>` 中声明版本号**，再在对应模块的 `<dependencies>` 中引用。
- **前端新增依赖必须更新对应子项目的 `package.json`**，并由 pnpm 生成新的 `pnpm-lock.yaml`；不允许混用 npm/yarn lockfile（`.gitignore` 已排除 `package-lock.json`、`yarn.lock`）。
- **CI/本地构建必须使用根 `package.json` 中声明的 pnpm 版本**（通过 `packageManager` 字段强制）。
- **私有仓库/镜像**：当前未配置 `.npmrc`，pnpm 直接使用默认 registry；Maven 侧通过 `forge-server/pom.xml` 的 `<repositories>` 指向阿里云/华为云镜像，未配置认证信息（凭据应在 CI 或本地 `~/.m2/settings.xml` 中配置）。
- **依赖升级工具**：Admin 前端提供 `taze major -I` 脚本（`up` 命令）用于批量检查大版本升级，但升级后仍需人工校验兼容性。
- **Docker 构建**：`docker/` 与 `docker-forge-admin/` 下的 Dockerfile 基于官方 Node/Maven 镜像，依赖下载走容器内网络，不受本地 pnpm cache 影响；`forge-admin-ui/.gitignore` 中的 `node_modules/` 保证镜像体积最小化。