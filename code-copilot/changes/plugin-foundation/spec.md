# 插件化底座 Spec

> 变更名：`plugin-foundation`
> 状态：`confirmed`（待澄清已全部确认，可进入 /apply）
> 创建日期：2026-10-06
> 涉及：权限过滤（菜单/接口按功能授权隐藏和拦截）、Flyway 执行流程、脚手架改名逻辑，需人工审查

## 1. 背景与目标

### 1.1 背景

Forge 后续采用“开源版 + 企业版插件”的模式：

- 开源版继续在 Gitee 公开全部源码（Apache 2.0），用户自行下载编译，不依赖任何制品库。
- 企业版插件同样以**完整源码包**交付。客户把插件包加载到自己的工程里一起编译，企业版源码放在独立私有仓库（阿里云效 Codeup），不进入本仓库。
- 授权采用永久授权 + 年度维保，License 校验插件本身属于企业版，不在本变更范围内。

现有工程无法直接挂载外部插件，问题见第 2 章。本变更只在**开源仓库**内搭建插件化底座，让任何插件（开源示例插件、将来的企业版插件）都能通过一条命令装进模板仓库或脚手架生成的工程。

### 1.2 目标

1. 新增 `forge-starter-plugin`，提供：
   - 功能授权接口 `FeatureGate`，以及开源版默认实现；
   - 接口级授权注解 `@RequiresFeature`；
   - 框架版本常量 `ForgeVersion`；
   - 插件描述文件的加载与兼容校验。
2. 插件迁移执行器：每个插件有独立的 Flyway 脚本目录和历史表，在主库迁移之后执行。
3. `sys_resource` 新增 `feature_code` 字段，当前用户菜单和权限按功能授权过滤。
4. 把 `forge:create` 的改名规则抽成共用模块，保证 `forge:create` 输出与重构前逐字节一致。
5. 新增 `pnpm forge:plugin` 命令，支持 `add`、`list`、`remove` 和 `--dev` 开发模式。插件源码按工程的改名规则装入工程。
6. 定义插件包规范 `forge-plugin.json`，提供开源示例插件 `forge-plugin-hello`，跑通“安装 → 改名 → 编译 → 迁移 → 菜单 → 接口”全链路。
7. 防误提交检查：模板仓库不得包含企业版代码或已安装的插件；`AGENTS.md` 写明规则。

## 2. 代码现状（Research Findings）

### 2.1 后端装配

- 启动类扫描范围固定为 `com.mdframe.forge`，Mapper 扫描固定为 `com.mdframe.forge.**.mapper`。
  - 出处：`forge-server/forge-admin-server/src/main/java/com/mdframe/forge/admin/ForgeAdminApplication.java`，`@SpringBootApplication(scanBasePackages = {"com.mdframe.forge"})`、`@MapperScan("com.mdframe.forge.**.mapper")`。
  - 推论：只要插件源码的包名与工程包名一致（被脚手架同规则改名），插件会被自动扫描，不需要额外的装配机制。
- 2026-10-07 核对：仓库已有 26 个 `META-INF/spring/...AutoConfiguration.imports`，包括 cache、flow、job 等模块。
  业务 Controller/Mapper 可沿用包扫描；需要 `@ConditionalOnMissingBean` 的默认扩展点应复用现有自动配置方式，
  不能根据“仓库没有自动配置”假设依赖组件扫描先后顺序。
- 模块聚合：`forge-server/pom.xml` 第 191–196 行列出 6 个顶层模块；`forge-admin-server/pom.xml` 逐个声明插件依赖（第 24–141 行）。
- 提案时根版本号为 `1.0.0`。框架子 POM 和独立 BOM 也声明了自己的 `revision`，根 POM 还固定导入 `1.0.0` BOM。
  T0 已将根/BOM 升为 `1.1.0`，框架继承根版本，BOM 导入及框架版本属性引用 `${revision}`，避免混用新旧模块。

### 2.2 Flyway

- 只有 `forge-admin-server` 执行迁移，配置在 `forge-server/forge-admin-server/src/main/resources/application.yml` 第 71–78 行：
  - `table: forge_schema_history`；
  - `baseline-on-migrate: true`、`baseline-version: 1.0.0`；
  - `placeholder-replacement: false`；
  - `locations` 为文件系统路径，可由 `FORGE_FLYWAY_LOCATIONS` 覆盖。
- 仓库中没有自定义 `FlywayMigrationStrategy` / `FlywayConfigurationCustomizer`（全局搜索仅 `BusinessApplicationCodegenService` 字符串引用 Flyway，与迁移执行无关）。
- 踩坑记录 `code-copilot/memory/pitfalls/db-flyway.md` #55：`jobAutoRegistrar` 依赖 `flywayInitializer`。插件迁移必须在同一个初始化阶段完成，否则依赖插件表的 Bean 会先于插件建表执行。
- 最新迁移版本：`forge-server/db/migration/V1.0.208__expand_tenant_theme_config.sql`。

### 2.3 菜单与权限

- 前端菜单来自 `GET /auth/current/menu`（`forge-admin-ui/src/api/index.js` `getMenu`）→ `AuthController.getCurrentUserMenuTree` → `SysResourceServiceImpl.selectCurrentUserMenuTree`。
- `SysResourceServiceImpl.getUserResources(LoginUser)`（第 445 行起）是当前用户资源树、菜单树、按钮权限的**唯一来源**：
  - 超级管理员：查询全部资源；
  - 普通用户：按角色查 `sys_role_resource` 再查资源。
- 超级管理员权限被硬编码为通配：
  - `SysResourceServiceImpl.selectCurrentUserPermissions` 返回 `*`、`*:*:*`；
  - `UserLoadServiceImpl.loadApiPermissions`（第 425 行起）返回 `/**`。
  - 推论：**仅靠菜单隐藏和权限标识无法阻止超级管理员调用企业版接口**，必须在接口层按功能授权拦截。
- 普通用户的按钮权限和接口权限在登录时由 `UserLoadServiceImpl.loadUserPermissions` / `loadApiPermissions` 计算并缓存到会话。
- `SysResourceMapper.xml` 中的 `selectUserNavigationResources`、`selectUserApiPermissions`、`selectUserResourcesByRoleIds` 没有任何 Java 调用方，本变更不改。
- 实体：`forge-plugin-system/.../entity/SysResource.java`，`@TableName("sys_resource")`，当前无 `featureCode` 字段。

### 2.4 前端路由

- 路由由 `unplugin-vue-router` 按文件自动生成。`forge-admin-ui/vite.config.js` 第 55–62 行：
  - `routesFolder: [{ src: 'src/views', path: '' }]`；
  - 排除 `**/components/**`、`**/api/**`、`**/workspace/**`。
  - 推论：插件页面只要放进 `src/views/plugins/<插件ID>/`，就会自动生成 `/plugins/<插件ID>/...` 路由，组件和接口文件放在插件目录的 `components/`、`api/` 下不会被当成页面。
- `forge-admin-ui/src/store/modules/permission.js` 第 198–211 行把菜单 `component` 统一规范为 `/src/views/<component>.vue`。

### 2.5 脚手架

- `scripts/forge-create/create-project.mjs`（1498 行，无自动化测试）`main()` 第 100–167 行的改名顺序：
  1. `rewritePomGroupIds`：所有 pom 中 `com.mdframe.forge` → `groupId`（第 682 行）。
  2. `buildTextReplacements` + `rewriteTextFiles`：全部文本按替换表替换，其中 `com.mdframe.forge` → `basePackage`，`forge_schema_history` → `<项目蛇形名>_schema_history`，并包含按 `module-catalog.json` 生成的模块名映射（第 710–834 行）。
  3. `moveJavaPackageDirectories`：Java 目录 `com/mdframe/forge` → 新包路径（第 891 行）。
  4. `renameFilesByBasename`、`renameArtifactDirectories`。
- `writeGeneratedConfig`（第 1176 行）写出 `forge.config.json`，包含包名、groupId、模块、前端、部署等字段，**没有框架版本号和已安装插件**。
- `copyOptionalRootFiles`（第 1016 行）只复制 `LICENSE`、`.gitignore`。生成的工程**没有** `scripts/` 和根 `package.json`，即没有任何可在生成工程里运行的工具。

### 2.6 数据库清理脚本

- `forge-server/scripts/db/clean-db.sh` 第 178 行把 `_history$` 结尾的表列为清空对象，第 170 行精确保留表只有 `forge_schema_history`。
  - 推论：插件迁移历史表会被清空，下次启动插件脚本重复执行。必须加入保留规则。

## 3. 功能点

### F1 `forge-starter-plugin`

位置：`forge-server/forge-framework/forge-starter-parent/forge-starter-plugin`，包名 `com.mdframe.forge.starter.plugin`。

| 组件 | 说明 |
|------|------|
| `ForgeVersion` | 从模块内 `META-INF/forge/forge-version.properties` 读取版本（Maven 资源过滤写入 `${revision}`）；提供 `CURRENT` 和 `satisfies(String range)` |
| `FeatureGate` | `boolean isEnabled(String featureCode)`；`String edition()` |
| `CommunityFeatureGate` | 开源默认实现，`@ConditionalOnMissingBean(FeatureGate.class)` |
| `@RequiresFeature` | 标注在 Controller 类或方法上 |
| `FeatureGateInterceptor` | 读取 `@RequiresFeature`，未授权时返回 HTTP 403 和明确的错误文案；通过本模块自己的 `WebMvcConfigurer` 注册 |
| `PluginDescriptor` / `PluginRegistry` | 启动时加载 classpath 下全部 `META-INF/forge-plugin.json`，校验字段与框架版本兼容性，对外提供已加载插件列表 |

### F2 插件迁移执行器

- `PluginFlywayMigrationStrategy implements FlywayMigrationStrategy`：
  - 条件：`@ConditionalOnClass(Flyway.class)`、`@ConditionalOnMissingBean(FlywayMigrationStrategy.class)`；
  - 先执行主库 `flyway.migrate()`；
  - 再按插件 ID 字母序，为每个声明了迁移的插件执行一次独立的 Flyway。
- 插件 Flyway 配置：
  - 继承主 Flyway 的数据源和全部配置；
  - `locations` = `classpath:db/plugin/<插件ID>`；
  - `table` = 由主历史表名推导；
  - `baselineVersion` = `0`。
- 主历史表名推导规则：把主表名的 `_schema_history` 后缀替换为 `_plugin_<插件ID下划线化>_history`。
  - 模板：`forge_schema_history` → `forge_plugin_hello_history`；
  - 生成工程：`acme_schema_history` → `acme_plugin_hello_history`。
- 迁移在 `flywayInitializer` 内完成，依赖 Flyway 的 Bean 会等插件迁移一起结束。

### F3 菜单与权限按功能授权过滤

- 迁移 `V1.0.209__add_resource_feature_code.sql`：`sys_resource` 新增 `feature_code VARCHAR(64) NULL`，带 `information_schema` 防重复。
- `SysResource` 新增 `featureCode` 字段。
- `SysResourceServiceImpl.getUserResources` 在返回前过滤 `featureGate.isEnabled(resource.getFeatureCode())` 为 false 的资源，超级管理员与普通用户同样生效。菜单树、资源树、普通用户按钮权限都经过这里，一处生效。
- `UserLoadServiceImpl.loadUserPermissions` / `loadApiPermissions` 的普通用户分支同样过滤，避免会话里缓存未授权的权限标识。
- 超级管理员的通配权限保持不变，接口层由 F1 的 `@RequiresFeature` 拦截。

### F4 共用改名规则

- 新增 `scripts/forge-shared/rename.mjs`，从 `create-project.mjs` 原样迁出：
  - `buildArtifactMap`、`buildApplicationClassMap`、`buildTextReplacements`、`applyTextReplacements`、`rewriteTextFiles`、`moveJavaPackageDirectories`，以及它们依赖的工具函数；
  - 新增 `renameSourceTree(dir, context)`：按 `create-project.mjs` 完全相同的顺序（pom groupId → 文本替换 → Java 目录移动 → 类文件改名）处理一个目录。
- `create-project.mjs` 改为引用共用模块，**生成结果与重构前逐字节一致**（见 8.5 的基线比对）。
- `forge.config.json` 新增：
  - `forgeVersion`：生成时模板的 `revision`；
  - `plugins: []`：已安装插件。
- 生成的工程额外包含：
  - 根 `package.json`，只有 `forge:plugin` 脚本；
  - `scripts/forge-plugin/`、`scripts/forge-shared/`、`scripts/forge-create/module-catalog.json`（改名需要模块映射）。

### F5 `pnpm forge:plugin`

| 命令 | 行为 |
|------|------|
| `add <插件包.zip\|目录> [--force]` | 校验 → 改名 → 复制 → 登记 pom → 写入 `forge.config.json` |
| `add <目录> --dev` | 只允许在模板仓库使用：不复制、不改名，以软链接接入，用于本地联调企业版或社区插件 |
| `list` | 列出已安装插件、版本、来源（复制/软链接） |
| `remove <插件ID>` | 删除插件目录、pom 登记、配置记录；不删数据库表和数据，打印手工清理指引 |

`add` 的处理步骤：

1. 读取插件包根目录 `forge-plugin.json`，校验字段（见 4.1）。
2. 用 `requiresCore` 校验工程框架版本：生成工程读 `forge.config.json.forgeVersion`，模板仓库读根 pom `revision`。不兼容直接失败。
3. 确定改名上下文：
   - 模板仓库（无 `forge.config.json`）：不改名；
   - 生成工程：用 `forge.config.json` + `module-catalog.json` 重建与 `forge:create` 相同的替换表。
4. 复制并改名：
   - 后端模块 → `<后端根目录>/plugins/<模块名>/`；
   - 前端 → `<admin-ui>/src/views/plugins/<插件ID>/`。
5. 在 pom 标记区块内登记：
   - `<后端根目录>/pom.xml` 的 `<!-- forge-plugins:modules:begin/end -->`：`<module>plugins/<模块名></module>`；
   - `forge-admin-server/pom.xml` 的 `<!-- forge-plugins:dependencies:begin/end -->`：插件依赖，`groupId` 用工程 groupId，`version` 用 `${revision}`。
6. 写入 `forge.config.json.plugins`。
7. 已安装同 ID 插件时，必须加 `--force` 才覆盖；覆盖前检查插件目录在 git 中是否有未提交修改，有则拒绝并提示先提交。

### F6 插件包规范与示例插件

插件包结构：

```text
<插件包>/
├── forge-plugin.json
├── server/<模块名>/                       # Maven 模块，parent 指向 forge-server
│   └── src/main/resources/
│       ├── META-INF/forge-plugin.json    # 与根目录同内容，供运行时 PluginRegistry 读取
│       └── db/plugin/<插件ID>/V1.0.0__*.sql
└── ui/                                   # 页面、components/、api/
```

示例插件 `plugins-samples/forge-plugin-hello/`：

- 后端：一个 Controller `GET /plugin/hello/info`，带 `@SaCheckPermission`，返回插件 ID、版本、`ForgeVersion.CURRENT`。
- 迁移：插入一个菜单（`NOT EXISTS`、`tenant_id = 1`），`feature_code` 为空。
- 前端：一个页面 `index.vue`。
- 不默认安装，只用于验收和作为社区插件模板。

### F7 防误提交检查

- `scripts/guards/check-edition.mjs`，通过根 `package.json` 的 `pnpm check:edition` 运行。以下任一情况失败：
  - 任何文件包含 `com.mdframe.forge.ee` 或 `com/mdframe/forge/ee`；
  - 任何 `forge-plugin.json` 的 `edition` 为 `ee`；
  - pom 的 `forge-plugins` 标记区块非空；
  - `forge-server/plugins/`、`forge-admin-ui/src/views/plugins/` 下有被 git 跟踪的文件。
- 模板仓库 `.gitignore` 增加 `forge-server/plugins/`、`forge-admin-ui/src/views/plugins/`，放在带标记的“仅模板仓库”区块内；`forge:create` 复制 `.gitignore` 时去掉该区块，生成工程正常提交已安装插件。
- `AGENTS.md` 新增 5.18“开源仓库禁止企业版代码”。

## 4. 业务规则

### 4.1 `forge-plugin.json`

```json
{
  "id": "hello",
  "name": "示例插件",
  "version": "1.0.0",
  "edition": "community",
  "requiresCore": ">=1.1.0 <2.0.0",
  "features": ["community.hello"],
  "server": { "module": "forge-plugin-hello" },
  "ui": { "dir": "ui" }
}
```

| 字段 | 规则 |
|------|------|
| `id` | `^[a-z][a-z0-9-]{1,31}$`，全局唯一；历史表名中 `-` 转 `_` |
| `version` | 语义化版本 |
| `edition` | `community` 或 `ee` |
| `requiresCore` | 空格分隔的比较式，支持 `>=`、`>`、`<=`、`<`、`=` |
| `features` | 插件使用的功能编码；`ee` 插件必须以 `ee.` 开头，`community` 插件不得以 `ee.` 开头 |
| `server.module` | 可选；后端模块目录名，必须存在 `pom.xml` |
| `ui.dir` | 可选；前端目录 |

运行时 `PluginRegistry` 发现同 ID 重复、字段非法、版本不兼容时，**启动失败**，错误信息写明插件 ID、要求的版本范围和当前版本。

### 4.2 `CommunityFeatureGate`

| 功能编码 | 结果 |
|----------|------|
| 空或 null | 启用（普通菜单、开源功能） |
| `ee.` 开头 | 未启用 |
| 其它 | 启用（社区插件自用编码） |

`edition()` 返回 `community`。企业版 License 插件以 `@Primary` 或同类型 Bean 覆盖该实现，不在本变更范围。

### 4.3 `@RequiresFeature`

- 方法注解优先于类注解。
- 未授权：HTTP 403，`RespInfo.error("当前版本未开通该功能：<功能编码>")`，不打印堆栈。
- 拦截器排在 Sa-Token 登录校验之后，未登录仍返回原有的未登录错误。

### 4.4 插件迁移

- 插件脚本遵守 `AGENTS.md` 5.13：`NOT EXISTS` / `information_schema` 防重复、`tenant_id = 1`、禁止 `${...}`。
- 插件版本号从 `V1.0.0` 起独立递增，与主库版本无关。
- 插件迁移失败时启动失败，与主库迁移行为一致。

## 5. 数据变更

### 5.1 `V1.0.209__add_resource_feature_code.sql`

```sql
-- 通过 information_schema 判断列不存在时执行：
ALTER TABLE sys_resource
  ADD COLUMN feature_code VARCHAR(64) NULL COMMENT '功能授权编码，空表示不受授权控制' AFTER perms;
```

- 存量数据全部为 NULL，行为不变。
- 不建索引：过滤在 Java 内存中进行，不参与 SQL 条件。
- 回滚：`ALTER TABLE sys_resource DROP COLUMN feature_code;` 并回退实体字段。

### 5.2 插件迁移历史表

由 F2 自动创建，例如 `forge_plugin_hello_history`。`clean-db.sh` 第 170 行的精确保留正则增加 `_plugin_[a-z0-9_]+_history$`，保证清理模板库时不被清空。

### 5.3 不改

- `forge-server/db/全量初始化SQL.sql` 与 `docker-forge-admin/init-sql/01-init.sql` 不改。新列由增量迁移补齐，`init-db.sh --migrate` 和 admin 启动都会执行。

## 6. 接口变更

- 新增接口：无（示例插件的 `GET /plugin/hello/info` 只在安装示例插件后存在）。
- 行为变更：`GET /auth/current/menu`、`GET /auth/current/permissions` 等经过 `getUserResources` 的接口，会隐藏 `feature_code` 未授权的资源。开源版存量资源 `feature_code` 均为空，返回结果不变。

## 7. 影响范围

| 范围 | 文件 / 模块 |
|------|-------------|
| 新增模块 | `forge-starter-plugin`；`forge-starter-parent/pom.xml` 增加 module；`forge-admin-server`、`forge-plugin-system` 增加依赖 |
| 权限 | `SysResource`、`SysResourceServiceImpl.getUserResources`、`UserLoadServiceImpl.loadUserPermissions` / `loadApiPermissions` |
| 迁移 | `V1.0.209`；`PluginFlywayMigrationStrategy` 接管主迁移调用 |
| pom | `forge-server/pom.xml`、`forge-admin-server/pom.xml` 增加空的插件标记区块 |
| 脚手架 | `create-project.mjs` 改为引用 `scripts/forge-shared/rename.mjs`；`writeGeneratedConfig`、`copyOptionalRootFiles` 扩展 |
| 新增脚本 | `scripts/forge-shared/`、`scripts/forge-plugin/`、`scripts/guards/`、`plugins-samples/forge-plugin-hello/` |
| 数据库脚本 | `clean-db.sh` 保留规则 |
| 文档 | `AGENTS.md` 5.18；`.agents/skills/forge-project-init/SKILL.md` 增加插件安装说明 |

不影响：Flow 服务、报表服务、App 服务（不执行 Flyway，不加载插件迁移）；前端存量页面与路由。

## 8. 风险与关注点

| 风险 | 影响 | 应对 |
|------|------|------|
| 改名规则迁出后行为漂移 | 生成工程或插件改名不一致，编译失败 | 重构前先生成基线并记录文件哈希，重构后逐文件比对，必须完全一致 |
| `FlywayMigrationStrategy` 接管主迁移 | 写错会导致主库不迁移 | 主迁移调用放在第一行，单测验证调用顺序；保留 `@ConditionalOnMissingBean` 让项目可覆盖 |
| 插件历史表 `baselineOnMigrate` | 若沿用主配置 `1.0.0`，插件 `V1.0.0` 会被当成基线跳过 | 插件 Flyway 显式设置 `baselineVersion = 0`，并有单测断言 |
| 超级管理员通配权限 | 菜单隐藏后仍能直接调接口 | 企业版接口一律加 `@RequiresFeature`，写入插件开发规范 |
| 升级覆盖丢失客户修改 | 客户直接改插件源码后被覆盖 | 覆盖前检查 git 未提交修改；文档建议定制写在扩展点或应用层 |
| `.gitignore` 规则带进生成工程 | 客户已安装插件无法提交 | 仅模板区块带标记，`forge:create` 复制时去除，并有测试覆盖 |
| `--dev` 软链接修改了模板 pom | 联调后误提交 | `check:edition` 检查标记区块必须为空 |
| 本机无 Maven / MySQL | Java 编译、单测、Flyway 实跑无法在本机执行 | 列入人工验收，由用户执行并回填 |

## 8.5 测试策略

- **改名基线**（最先做）：重构前用固定参数执行 `forge:create`，生成 `full` 和 `minimal-admin` 两套工程，记录全部文件的相对路径和 SHA-256；重构后再生成并逐项比对。
- **Node 单测**（`node --test`）：
  - `forge-shared`：替换顺序、Java 目录移动；
  - `forge-plugin`：`add` 到模板形态和生成工程形态、版本不兼容拒绝、重复安装需要 `--force`、有未提交修改拒绝覆盖、`list`、`remove`、`--dev` 只允许模板仓库；
  - `check-edition`：四类违规各一例，以及干净仓库通过；
  - `forge:create` 生成的 `.gitignore` 不含模板区块，`forge.config.json` 含 `forgeVersion` 与 `plugins`。
- **Java 单测**（需 Maven，用户执行 `-Penable-tests`）：
  - `ForgeVersion` 区间解析；
  - `CommunityFeatureGate` 三类编码；
  - `FeatureGateInterceptor` 方法优先于类、403 文案；
  - `PluginRegistry` 重复 ID、非法字段、版本不兼容；
  - 插件历史表名推导、`baselineVersion = 0`、主迁移先执行；
  - `getUserResources` 过滤未授权资源（超级管理员与普通用户）。
- **人工验收**：用 `forge:create` 生成工程 → `forge:plugin add plugins-samples/forge-plugin-hello` → `mvn package` → 启动 admin，检查以下几点：
  - 插件历史表已创建；
  - 菜单可见，接口返回正常；
  - 把示例菜单的 `feature_code` 改为 `ee.test` 后菜单消失；
  - 给示例接口加 `@RequiresFeature("ee.test")` 后，超级管理员调用返回 403。

## 9. 待澄清

已于 2026-10-06 全部确认：

1. **版本号策略**：根 pom `revision` 升为 `1.1.0`，此后每次开源版发版都递增；插件的 `requiresCore` 以此为准。升版放在 T0 生成基线之前，避免基线比对出现版本号差异。
   需同步独立 BOM 的 `revision`、移除框架子 POM 的旧版本覆盖，并统一根 POM 的内部 BOM 版本引用。
2. **目录与路由前缀**：插件后端放 `<后端根目录>/plugins/`，前端页面放 `src/views/plugins/<插件ID>/`，路由前缀 `/plugins/<插件ID>`。
3. **模块命名**：使用 `forge-starter-plugin`，与现有 `forge-starter-*` 一致。

## 10. 不做

- 企业版 License 校验插件、签发工具、私有仓库：属于企业版仓库。
- `@ConditionalOnForgeFeature` 启动期条件装配：条件评估早于 Bean 创建，需要 License 在容器启动前加载，与 License 插件一起设计；本期由接口层 `@RequiresFeature` 拦截兜底。
- 各企业版功能的扩展点（文件预览、配额、导出审批等）：每个企业版插件立项时，先在开源仓库补对应扩展点和默认实现，不预先设计。
- 插件之间的依赖关系、插件卸载时删表、插件热加载。
- 制品库分发、二进制插件、`plugins/` 目录直接放 jar。

## 11. 2026-10-07 可行性复核与执行进度

- 结论：源码插件、独立迁移历史和功能授权接口的总体方向可行；不是运行时热加载或源码防破解机制。
- 复核细节和后续实现约束见 [feasibility.md](./feasibility.md)。其中 T8 模板配置识别、安装回滚和路径防护，
  T10 防误提交检查的自匹配问题，需要在对应 Task 落地前收敛并验证，不属于 T0 已实现内容。
- T0 完成：先统一 `1.1.0` 版本链，再生成两套工程，分别记录 8324 / 4866 个文件的路径、字节数和 SHA-256。
- 基线不是直接读取后续变动的工作区：冻结输入为 `e416f7902834763ef43989c4525738441e49bd4c` 加版本补丁。
  T5/T6 必须在该输入上替换生成器及其依赖后做输出对比，防止把 T1–T4 的业务源码变更误认成改名回归。
- 重复生成均无新增、缺失或内容差异。复跑方法见 [baseline/README.md](./baseline/README.md)。
- 本轮未修改生产生成器、未实现 T1–T12，未启动服务或操作真实数据库；Java 编译因本机缺 Maven/JDK 未执行。
