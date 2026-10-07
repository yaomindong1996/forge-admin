# 插件化底座 Spec

> 变更名：`plugin-foundation`
> 状态：`implementing`（T0–T8 及 T5-F1 已交付；T9–T12 待继续）
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
  T0 已将根/BOM 统一为 `1.2.0`（2026-10-07 用户确认），框架继承根版本，BOM 导入及框架版本属性引用
  `${revision}`，避免混用新旧模块；不修改已存在的发布标签。

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

- 原 `forge-server/scripts/db/clean-db.sh` 把 `_history$` 结尾的表列为清空对象，迁移历史只保护 `forge_schema_history`。
  - 插件迁移历史可能被清空，导致下次启动重跑；T4 已补优先保留及显式删除保护，见第 5.2 节。

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
  - 复制主 Flyway 的数据源和通用配置，不修改主配置；程序化迁移来源按 T2 隔离边界处理；
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
| `add <目录> --dev` | 仅模板：后端宿主接入 POM + 源码链接，前端目录链接；不复制/改名/修改外部业务源码 |
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
  "requiresCore": ">=1.2.0 <2.0.0",
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

由 F2 自动创建，例如 `forge_plugin_hello_history`。`clean-db.sh` 按小写表名匹配
`_plugin_[a-z0-9_]+_history$`，在框架/业务表分类之前保留，兼容脚手架改名后的任意工程前缀。
主 Flyway 历史表与插件历史表都不进入 DROP、TRUNCATE、其它租户或逻辑删除残留清理。
显式 `--drop-table` 指向这些表时，在连接数据库前拒绝，不允许参数覆盖迁移历史保护。
普通业务历史表及历史表的备份/临时副本仍按原规则清理，不把所有 `_history` 表都排除。

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
| 真实 MySQL 全链路未验收 | 桩测试不能证明首次迁移、重复启动和清理历史正确 | T1 已用临时 JDK/Maven 编译和单测；真实迁移留待 T12 |

## 8.5 测试策略

- **改名基线**（最先做）：重构前用固定参数执行 `forge:create`，生成 `full` 和 `minimal-admin` 两套工程，记录全部文件的相对路径和 SHA-256；重构后再生成并逐项比对。
- **Node 单测**（`node --test`）：
  - `forge-shared`：替换顺序、Java 目录移动；
  - `forge-plugin`：`add` 到模板形态和生成工程形态、版本不兼容拒绝、重复安装需要 `--force`、有未提交修改拒绝覆盖、`list`、`remove`、`--dev` 只允许模板仓库；
  - `check-edition`：四类违规各一例，以及干净仓库通过；
  - `forge:create` 生成的 `.gitignore` 不含模板区块，`forge.config.json` 含 `forgeVersion` 与 `plugins`。
- **Java 单测**（需 Maven，执行 `-Penable-tests`；T1 已用临时工具验证）：
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

1. **版本号策略**：2026-10-07 用户确认改为 `1.2.0`，替代提案原定的 `1.1.0`；远端已有 `v1.1.2` 和 `v1.1.3`
   标签，不复用已有版本。此后每次开源版发版都递增；插件的 `requiresCore` 以此为准，先升版再生成 T0 基线。
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
- T0 按用户确认的 `1.2.0` 修正并重建基线；初次 `1.1.0` 记录作为历史留在提交 `369f345b`，不覆盖旧标签。
- 基线不是直接读取后续变动的工作区：冻结输入为 `e416f7902834763ef43989c4525738441e49bd4c` 加版本补丁。
  T5/T6 必须在该输入上替换生成器及其依赖后做输出对比，防止把 T1–T4 的业务源码变更误认成改名回归。
- 重复生成均无新增、缺失或内容差异。复跑方法见 [baseline/README.md](./baseline/README.md)。
- T0 阶段未修改生产生成器、未实现 T1–T12，未启动服务或操作真实数据库；当时缺 Maven/JDK，未执行 Java 编译。
- 用户要求重点复核“源码插件＋永久授权＋年度维保”的可行性，分析见 `feasibility.md` 的商业模式小节。
  商业条款和 License 实现仍是建议/后续工作，不因本次分析视为已经上线或确认具体售价。

### 2026-10-07 T1 实现边界

- 用户确认按复核方案开始实现。本轮先交付 F1 运行时底座，收费、License、私有仓库仍按第 10 节排除。
- 默认 Gate 及 Registry 由独立自动配置注册；Servlet 拦截器由 Web 自动配置注册，顺序为 4，
  保持现有登录（1）、限流（2）、API 权限（3）的顺序，不替代 RBAC 或登录校验。
  客户 Gate 自动配置需排在默认配置之前；Registry 显式非懒加载，避免全局懒加载使错误插件绕过启动校验。
- 拒绝功能时 HTTP 和响应体 `code` 均为 403，使用现有 `RespInfo.error(403, message)`，不输出堆栈。
- 版本比较遵循 SemVer 的预发布优先级、忽略构建元信息；范围只接受本 Spec 的空格分隔比较式，
  非法表达式必须完整校验后报错，不能因前一比较不匹配而跳过后面的非法条件。
- 描述加载拒绝重复 JSON 键、未知字段及超出 64 KiB 的文件；插件版本/ID/功能前缀/相对目录均严格校验，
  功能编码限 64 字符小写字母数字与点、下划线、短横线；启动失败附插件 ID、兼容范围和核心版本。
  运行时只校验描述元数据；源码目录及 `pom.xml` 存在性留给 T8 安装器，不在 classpath 中寻找源文件。
  字符串元数据不接受数字/布尔值隐式转换；无法解析的描述只标明来源和核心版本，不能臆造插件 ID。
- 同步 BOM 和模块目录清单，新 starter 随 Admin 的依赖闭包进入各预设；不修改 T0 冻结基线。
- 本机找到可用的临时 JDK/Maven，优先补编译和单测；不启动真实服务，不连接数据库。

### T1 交付与验证结果

- 完成新 starter、版本资源、SemVer 范围校验、插件描述注册表、功能授权接口和 Servlet 拦截自动配置。
- 106 项 Java 用例在模板、隔离生成 full 和 minimal-admin 中各全部通过；最新 Admin 47 模块聚合编译通过。
  MVC 登录顺序验证使用 MockMvc 夹具，不等同于真实 Sa-Token/Redis 登录端到端验收。
- 按项目初始化 Skill 复跑清单工具及模板/生成 full 数据库脚本共 41 项 Node 测试，数据库和迁移调用均为桩。
  新模块在生成工程保留且包名/artifact/自动配置引用一致，版本资源读取 `1.2.0`；T0 冻结基线保持不变。
- T1 验证时 T2–T12 未完成：当时尚无插件迁移、菜单权限过滤、安装升级命令或示例插件；商业 License 和计费不在本期。
  具体命令、失败修正及限制见 `execution-log.md`。

### 2026-10-07 T2 实现边界

- 本轮只接入 F2，保留 T1 扩展点及 T0 冻结基线；不执行真实 MySQL、业务服务或权限变更。
- `flyway-core` 使用现有 `10.20.1` 依赖管理且声明为 optional，避免其他宿主被传递引入 Flyway；
  测试复用项目已有 H2、Spring JDBC（仅 test scope），在随机命名的内存数据库验证真实迁移，不使用本地开发库。
- 自动配置先于 Boot Flyway 自动配置登记策略，后于 Plugin 自动配置；未引入 Flyway或
  `spring.flyway.enabled=false` 时不登记策略，客户已有 `FlywayMigrationStrategy` 时默认实现退出。
- 主迁移必须先调用；随后只扫描已注册插件自己的 `db/plugin/<id>/` 下符合主 SQL 后缀的文件，
  无迁移文件的插件不创建历史表。按 ID 排序执行，并在首个插件运行前检查所有历史表名称。
- 主历史表必须是合法 ASCII 标识符并以 `_schema_history` 结尾；插件历史表最多 64 字符，
  短横线转换成下划线。没有插件迁移时不因客户自定义主表名额外失败。
- 继承主数据源、ClassLoader、Schema、编码、事务、占位符、SQL 命名及校验配置；仅替换 locations/table/baselineVersion。
  为防止主脚本重复落入插件历史，清空显式 Java migrations/custom resolvers/resource provider，
  插件限定 SQL，使用空 Java class provider 和默认 SQL resolver；客户特殊解析器可覆盖整个策略。
  全局回调继续继承，会在每个插件迁移时触发，回调实现应按当前 Configuration 区分上下文。
- `baselineOnMigrate`、`target`、`ignoreMigrationPatterns`、`outOfOrder` 原样继承并测试：
  `baselineOnMigrate=false` 时不擅自基线化已有库；有限 target 也限制插件版本，且必须存在于各迁移流，
  否则 Flyway 拒绝初始化。模板未设置有限 target，不新增独立插件 target 的配置管理。
  插件 baselineVersion 固定为 0，确保默认非空业务库首装仍执行插件 V1.0.0。
- 主/插件失败均中断初始化；不自动 clean、repair、降级或回滚已执行 DDL，启动错误带插件 ID 与历史表上下文。
  依赖 `flywayInitializer` 的 Bean 只能在所有插件完成后创建；H2/MockMvc 等测试不替代真实 MySQL 验收。

### T2 交付与验证结果

- 完成策略、SQL 迁移计划/工厂和独立自动配置，复用现有 Flyway 版本；不增加传递的宿主迁移依赖。
- 新增 48 项测试，连同 T1 共 154 项在模板、改名 full/minimal-admin 中各全部通过。
  H2 真实 SQL 验证非空库 V1.0.0 首装、历史独立、重复启动、校验失败、顺序与中断；容器测试验证初始化依赖等待。
- Admin 47 模块聚合 package 成功，包内新 starter 与当前模块 jar 摘要一致；41 项 Node 回归及 XML 检查通过。
- 未连接真实 MySQL、未启动业务服务或变更业务表；H2 版本有 Flyway 非阻塞兼容提示，真实 MySQL 验收仍待 T12。
  T3–T12 尚未完成，尤其 T4 清理保护未接入，当前不要对安装了插件的库运行既有清理脚本。

### 2026-10-07 T3 实现边界

- 用户确认继续按提案实现，本轮只交付 F3 的权限收窄，不增加公开接口、角色授权或 License 实现。
- 新增 V1.0.209，用当前库 information_schema 判断 feature_code 是否已存在；新增可空 VARCHAR(64)，
  不修改旧迁移、全量 SQL 或存量资源编码，不连接真实数据库。回滚方式沿用第 5.1 节。
- System 插件依赖现有 starter-plugin，两个资源加载入口使用同一个可替换 FeatureGate Bean。
  getUserResources 的管理员/普通用户结果均过滤；登录加载权限只在普通用户分支过滤资源，
  API 匹配表达式只查询过滤后保留的资源 ID。角色、租户、用户类型、客户端和资源排序规则不变。
- 过滤按资源自身 feature_code，不隐式继承父资源编码；插件菜单、按钮和 API 应分别声明所需功能。
  空编码/普通社区编码沿用 Gate 约定，不把 Registry 的 features 当资源白名单，避免隐藏存量业务。
- 超管通配符不变，企业接口必须声明 RequiresFeature，门禁失败不能降级为放行；
  已登录会话的权限快照需重新登录/刷新会话才能更新，接口功能门禁仍按请求实时判断。
- 管理员的资源配置树和角色授权配置不做功能过滤；未授权资源仍可维护，避免无法纠正配置。
  全局已配置 API 地址目录也不按功能过滤，否则可能把未开通接口误判为无需 RBAC 的接口。
- 本轮沿用三个被改入口已有 Mapper 查询与范围条件，不扩大查询重构范围；历史 Wrapper 查询
  迁入 XML、旧 API pattern 查询的 visible 条件不在本轮新增或修复，不能借功能过滤改动存量权限语义。
- 验证覆盖菜单/资源树、普通按钮权限与登录 API 权限、租户/角色/客户端范围、空集与 Gate 抛错；
  迁移做防重复和静态契约检查，真实 MySQL DDL 与 Sa-Token/Redis 登录联调留给 T12 人工验收。

### T3 交付与验证结果

- 已增加资源 featureCode、V1.0.209 和 System 的 starter-plugin 依赖，同步模块目录闭包。
  当前资源查询与普通用户登录快照均按可替换 Gate 收窄；超管通配、后台配置树和既有范围条件不变。
- 新增 32 项测试：当前资源 14、登录权限 12、实体/迁移静态契约 3、Spring 装配与公开用户加载 3。
  System 共 160 项、starter-plugin 154 项在模板/full/minimal-admin 各全部通过，失败/错误/跳过均为 0。
- Admin 聚合 package 通过，嵌入的 System/starter jar 与本轮构件摘要一致；模板/生成 full DB 桩与基线工具
  共 41 项 Node 回归通过；改名后的依赖、自动配置、迁移文件和 POM XML 验证通过，T0 基线不变。
- 首次完整回归发现一项旧安全测试读已移除的 SQL 副本；改为验证现用权威 SQL 与初始化脚本入口，
  原敏感路径/日志保护断言保留，未跳过测试或放宽业务断言。
- 未执行真实 MySQL 迁移或 Sa-Token/Redis 登录。存量 V1.0.72 的业务占位符保持原样，主配置原本关闭
  placeholder replacement；新增 V1.0.209 无占位符。T4–T12 待办，旧清理脚本的插件历史保护仍未完成。

### 2026-10-07 T4 实现边界

- 本轮只补迁移历史表的清理保护与脚本回归，不增加迁移脚本、接口、插件安装命令或 License 实现。
- 使用主历史表精确名称和插件历史表后缀规则，匹配发生在通用分类/租户删除/逻辑删除之前。
  保留 T2 的插件 ID 连字符转下划线规则；大小写统一按 information_schema 中的小写表名判断。
- 显式删除迁移历史表的参数直接失败，包含同时指定 keep/drop 的情况，避免忽略非法参数后继续清库。
  既有普通表 keep/drop、业务表保留开关、待执行主迁移检查和 SQL 追加机制不变。
- 保护范围限于 clean-db.sh 自动生成的清理语句；用户自定义 --extra-sql 不做 SQL 解析或拦截，
  必须人工审核。init-db.sh --recreate、外部手工 SQL、卸载清库不在此保护范围。
- 先用 bash 3.2 和 MySQL 桩验证预览/执行计划，再在改名 full 隔离工程复跑。
  不连接真实数据库、不重跑或修改已发布迁移；真实 MySQL 清理后重复启动仍留给 T12。

### T4 交付与验证结果

- 已增加主库/插件迁移历史专用保留规则，覆盖自动分类和逐行清理；显式删除历史表在连接前失败。
  更新帮助说明，不改变普通表清理、待执行迁移检查或自定义 SQL 追加行为。
- 新增 12 项脚本测试；模板 35/35、改名 full 30/30 通过，失败/错误/跳过均为 0。
  覆盖大小写、任意前缀、keep/drop 冲突、模拟扩展列、备份副本和实际交给 MySQL 桩的执行计划。
- 系统 bash 3.2.57 语法与执行、Node 语法、新增代码行宽和 diff 检查通过；改名清理脚本与当前模板
  按预期替换后的内容一致，主历史表名称与生成全量 SQL 一致，T0 冻结基线未修改。
- 没有真实数据库写入或业务服务启动；--extra-sql 需人工审核，--recreate 不受本保护约束。
  T5–T12 仍待完成，真实 MySQL 清理后重复启动验收未执行，不把脚本桩测试视作该验收完成。

### 2026-10-07 T5 实现边界

- 仅抽取共享改名能力与新增 Node 回归：rename.mjs 提供模块/启动类映射、顺序化文本替换、
  POM groupId/根模块改名、Java 包目录移动、类文件和模块目录改名，以及 renameSourceTree 编排入口。
  文件遍历与存在性检查下沉同目录工具，生成器复用，不维护两份替换表或遍历实现。
- 生成器保留原编排位置：POM 改名在前，配置/上下文写入后统一文本替换，Docker 特殊改写之后再移动
  Java 包与启动类，SQL 收集及 Docker SQL 同源复制不变；共享入口只处理调用者提供的隔离源码目录。
- 新入口 context 使用生成器已规范化的 options、catalog、selection，可传入扩展 artifactMap 和
  applicationClassMap；先完整构建替换表，再写文件。插件描述/zip/path 校验、安装回滚仍由 T8 实现。
- 替换顺序和已有 SSO 行清理/二进制排除/目标已存在时的规则保持一致，不增加前缀全局排序或内容归一化。
  只补同包名移动的 no-op 保护：源目标相同不得走合并后删除源目录，正常改名输出不受影响。
- 不做 T6 的工具复制、新配置、gitignore 区块或 T7/T8 安装登记，不新增 Java/UI/SQL 业务改动或依赖。
- 验证分两条：冻结 e416f790 + 版本补丁，只替换生成器入口与共享依赖，full/minimal-admin 对 T0 全文件
  原始 SHA-256 比较必须零差异；实时工作区另外生成两套工程检查 T1–T4 装配并复跑 DB 桩测试。
  禁止复制实时业务文件/目录清单污染冻结输入、覆盖旧工程、重新录制基线或连接真实数据库。

### T5 交付与验证结果

- 共用模块已抽出，生成器复用同一份映射、替换和文件遍历；顺序不变，新增同包名移动的 no-op 保护。
  生成器从 1498 行减少至 1084 行，共享规则/遍历/测试分别为 419/52/278 行。
- 抽取前后 10 组条件与自定义参数的映射及完整有序替换数组一致；冻结 full 8324 文件、minimal-admin
  4866 文件逐字节 SHA-256 比较的 missing/added/changed 均为空，未覆盖或重新录制 T0 清单。
- 新增 25 项 Node 测试；模板 60/60、当前改名 full 的 DB 桩 30/30 通过，失败/跳过均为 0。
  生成工程 54/35 个 POM XML、1.2.0 版本、System/starter 依赖、自动配置和历史保护检查通过。
- 生成工程 Admin 聚合编译未通过：full 在 plugin-data、minimal-admin 在 plugin-generator 因打印类型缺失失败。
  原 module-catalog 未登记 plugin-print，POM 裁剪移除了两个模块原有的打印依赖；冻结输出也有相同缺失。
- 另确认原有 ForgeAdmin 品牌替换先于启动类替换，导致 Admin 文件名带 AdminApplication、类名却为 Application；
  直接 javac 给出类名/文件名不匹配诊断。聚合编译在更早的依赖失败处停止，不能声称已编译到 Admin。
- T5 兼容抽取完成不代表生成工程已可完整编译；上述两项应单独确认修复并列出允许差异，不能改写冻结基线
  或把旧缺陷修复夹入零差异重构。T6–T12 与真实数据库验收未完成，本轮未启动业务服务或连接真实库。

### 2026-10-07 T5-F1 生成工程编译修复边界

- 用户在两项旧问题及修复建议后确认“继续”，本轮先做独立兼容修复，不与 T5 零差异抽取混合。
  T0 原始清单/provenance/版本补丁保持不变，后续 T6 的增量比对必须同时识别本节明确允许的修复差异。
- 修正 ForgeAdmin 品牌替换的匹配边界，避免命中 ForgeAdminApplication；完整启动类仍走同一份映射。
  不调整其它替换阶段或通用字面量语义；声明、main 引用、文件名及文档/构建中的启动类引用一起保持一致。
- 目录清单新增现有 plugin-print 及其真实 POM 依赖，full 显式保留；generator/data 保留打印依赖。
  generator POM 还直接依赖 plugin-data、plugin-external，同一处目录闭包遗漏一并补齐，不裁掉仍被源码引用的能力。
  不修改生产 POM/Java/SQL，不引入外部新依赖，不靠本地仓库旧 jar 补齐生成工程。
- 相对 T0 允许差异只包括：启动类引用纠正；打印模块坐标/目录改名及 Maven 登记；minimal-admin 补回
  data/external 编译依赖模块和 Maven 登记；对应 SQL 模块选择/顺序/manifest 与 forge.config.json 变化。
  生产 SQL 不变，输出的 SQL 逐文件核对来源与改名；其它输出不得任意忽略。
  用冻结源 + 原版本补丁 + 本轮生成器/共享依赖 + 仅本节目录清单增量验证，不能混入 T1–T4 的模块新增。
- 新增 Node 用例覆盖前缀重叠、实际源码声明/引用/文件名与 POM/目录闭包；在实时 full/minimal-admin
  重新生成后先 install 独立 BOM，再 Admin 聚合 package -am -DskipTests；复跑模板和生成 full 的 DB 桩。
  不连接真实 MySQL、启动业务服务或执行 UI 构建；这些业务源码均未改变，真实联调仍由用户验收。
- 首次修复验证：full 已打包通过，minimal-admin 在 Admin 的应用集成代码引用未选择的能力开放模块时失败。
  应用集成依赖 generator、capability-platform、capability-actions，缺任一模块时仅裁剪生成工程的
  admin/integration 主/测试源码和 ApplicationIntegrationMapper.xml（共 11 个文件）；full 全部保留。
  不为编译把能力开放套件强塞入最小预设，不改变模板业务代码。回滚方式为还原生成器后重新生成工程。
  该额外缺陷由本轮聚合构建发现，作为同一编译修复的明确输出差异；增加缺依赖和保留其它文件的桩测试。
- minimal-admin 的 Admin POM 同时恢复源文件已有的 spring-boot-starter-test：原裁剪正则把它与后面的
  external 依赖一起删除，保留 external 后该块自然保留。本轮未修改裁剪正则或新增外部依赖声明。

### T5-F1 交付与验证结果

- 三项旧编译问题已修复：打印模块登记/依赖闭包、Admin 启动类命名、最小预设可选接入层裁剪。
  本轮新增 16 项 Node 测试；模板 76/76、实时改名 full DB 桩 30/30 通过，无失败/取消/跳过。
- 从最终脚本重新生成两套实时工程，分别安装 BOM 后离线 Admin 聚合 package 通过；54/37 个 POM XML
  解析通过，包内 Start-Class 正确，嵌入打印 jar 与本轮 Reactor 构件 SHA-256 一致。
- 冻结 full 为 8324 文件：112 个打印模块文件移路径、10 个其它文件内容变化；minimal-admin 为 5058 文件：
  112 个打印文件移路径，新增 data/external 194 文件和 9 个 SQL，2 个消息 SQL 改序号，移除 11 个集成文件，
  9 个其它文件内容变化。全部新增/删除/变化均单列审核，不归一化原清单，详细分类见 execution-log。
- T0 清单/provenance/版本补丁未改，T5 的历史零差异证据保留。T6–T12 尚未完成；不把打包或数据库桩
  当成真实数据库迁移、运行启动或端到端插件安装验收。没有 push/合并 main，无 Java/UI/生产 SQL 改动。

### 2026-10-07 T6 实现边界

- 本轮只扩展 F4 的生成工程工具交付，不提前实现 T7 POM 标记或 T8 安装/升级/卸载行为。
  插件入口提供帮助，尚未开放的命令非零退出且不写项目文件，不能把工具已复制表述为安装链路已完成。
- forgeVersion 从模板根 POM 的唯一明文 revision 读取，缺失/重复/未解析或非法版本在生成目标写入前失败；
  plugins 初始为空数组。其余原配置字段/选择不变，不在生成工程重新硬编码核心版本。
- 根 package.json 仅提供 forge:plugin 脚本，不复制模板的 forge:create 或模板专用维护脚本，零新增 npm 依赖。
  复制 forge-plugin/ 与 forge-shared/ 的运行文件、原始 module-catalog.json，不复制测试/夹具/本地文件。
  配置写入下沉 project-tools，既有 README 模板字节不变，避免继续扩大原有超长 writeGeneratedConfig 方法。
- 工具复制在全部业务改名与 SQL/Docker 写入之后：工具源码和目录清单保留模板的原始匹配字面量与路径，
  后续 T8 才以生成配置重建改名上下文。否则二次安装插件会使用已被改坏的替换表。
- .gitignore 只剥离 # forge-template-only:begin/end 的完整单一区块，保留其它原始字节及换行。
  没有区块时原样；嵌套/重复/未闭合/反向标记拒绝。模板实际防误提交区块仍由 T10 增加，本轮用夹具验证。
- 冻结输入先复现 T5-F1，再仅带入本轮生成器/工具；T6 相对 T5-F1 仅新增运行工具/根 package、配置两字段，
  .gitignore 按精确区块规则比较。相对 T0 的旧修复差异沿用上节，不重录任何原清单或忽略其它业务输出。
- 重新生成实时 full/minimal-admin 验证工具导入、pnpm 入口及配置；按 Skill 复跑模板/生成 full DB 桩。
  业务源码/POM/SQL 未变，先做原始字节和 XML 门禁，再按风险复验聚合构建；不启动真实业务服务或连接数据库。

### T6 交付与验证结果

- 生成工程新增根 package.json、插件帮助入口、两个共享运行模块和原始目录清单，共 5 个文件；
  forge.config.json 仅增加 forgeVersion/plugins。工具在业务改名完成后原样复制，可继续用于二次插件改名。
- 新增 33 项 Node 测试；模板 109/109、生成 full DB 桩 30/30 通过，失败/取消/跳过均为 0。
  最终重新生成的 full/minimal-admin 两套 BOM install 和 Admin 聚合 package 均通过，54/37 个 POM XML 通过。
- 冻结 T5-F1 输出与上一阶段验证目录逐字节一致；T6 相对 T5-F1 两套均 missing=0/added=5/changed=1。
  移除配置两字段后恢复原始配置字节，运行工具字节与模板一致，夹具模板区块剥离后 .gitignore 恢复原始字节。
  配置写入抽取前后完整输出零差异；T0 清单/provenance/版本补丁均未修改。
- T6 只交付工具与配置；模板命令接入、安装/升级/卸载仍由 T8 实现，真实模板忽略区块由 T10 增加。
  未执行真实数据库迁移、应用启动或端到端插件安装；没有 push/合并 main。

### 2026-10-07 T7 实现边界

- 只给后端根 POM modules 和 Admin POM dependencies 末尾增加一组空的 forge-plugins 标记；
  标记名称严格遵循 F5，不参与项目/artifact/包名替换，不注册插件依赖或修改版本、现有模块次序。
- 模块裁剪原来重写整个 modules 区块，会删除标记；提取其原渲染规则，只额外保留合法空标记。
  无标记时保持原输出字节；损坏、重复、错位或非空区块拒绝，不将已安装插件静默裁掉。
  此保护不等于 T8 安装器，生成失败可能留下临时输出，生成器不新增整体事务/回滚机制。
- 验证模板两组标记唯一且为空、生成 full/minimal-admin 保留标记、自定义 groupId/包名/模块前缀不污染标记；
  冻结 T6 来源只带入精确两处 POM 注释和渲染器改动，全文件差异仅允许两个 POM 加空标记。
  原 T0 基线不改，既有 T5-F1/T6 允许差异不扩大；按 Skill 复跑模板/生成 full DB 桩，不连接真实库或启动服务。

### T7 交付与验证结果

- 两组空标记已加入对应 POM 的直属 modules/dependencies 末尾；模块渲染器保留空标记，
  非法/非空标记拒绝，无标记工程保持旧渲染规则。没有注册外部插件或改变原依赖/模块次序。
- 新增 20 项 Node 测试；模板共 129/129、生成 full DB 桩 30/30 通过，失败/取消/跳过均为 0。
  模板与重新生成 full/minimal-admin 三套 Admin 聚合 compile 通过，两套独立 BOM install/模型 validate 通过。
- 冻结 T6 来源复现结果与上一阶段零差异；T7 两套各仅改变根/Admin POM，删除精确新增注释后字节恢复旧输出。
  full 8329、minimal-admin 5063 文件，missing/added 均为 0；T0 原始清单、provenance 和版本补丁不变。
- 两套实时工程 54/37 个 POM XML、所有子模块目录、直属空标记和配置检查通过；
  独立 Maven/Java/模块前缀改名测试确认标记保留 canonical 名称。T8–T12 未完成，未执行真实库/安装全链路。

### 2026-10-07 T8 实现边界

- 实现目录/ZIP 安装、--force 覆盖、list/remove 和仅模板目录的 --dev；注册模板根 forge:plugin。
  只在隔离夹具/生成工程验证安装，不在工作仓库安装真实插件；T9 示例、T10 门禁和企业 License 仍不实现。
- 模板首次安装写 projectType=template、forgeVersion/plugins；后续明确识别模板，生成配置既有字段保留。
  generated 沿用原配置上下文，核心版本与根 POM 对照；目标只限 Admin 宿主，路径全由合法上下文和插件 ID 推导。
- ZIP 只接受普通文件/目录及 store/deflate，限制文件数和解压体积，拒绝加密/ZIP64/路径穿越/符号链接/
  重复或大小写冲突路径。目录同样拒绝链接与特殊文件，排除 .git/node_modules/target/logs 等构件。
  描述元数据与 T1 一致，拒绝重复 JSON 键/未知字段，后端运行描述须与包根描述一致。
- 插件源 POM 必须是声明的单模块、继承 forge-server，安装后 parent.relativePath 统一为 ../../pom.xml；
  Maven 模块版本继承核心 revision，插件发布版本独立由描述表达。不执行包内脚本或 Maven 生命周期。
- 重复 ID 必须 --force；模块重名、目标目录占用、POM 标记/配置不一致均拒绝。
  Git 工程拒绝覆盖未提交修改；无 Git 或忽略路径以安装摘要检测改动。成功覆盖/卸载保留可恢复备份，
  --force 明确是整包替换，不是三方合并；已提交的定制可通过备份/Git 恢复。
- 写入前完成描述/兼容/路径/POM 预检；项目独占锁、隔离 staging、原文件检查和回滚覆盖源码/POM/config。
  首装同样保留旧 POM/config，恢复目录自身忽略，不靠成功后的删除清理实现事务提交。
  捕获失败恢复旧状态；进程崩溃/断电不承诺跨文件原子性，遗留锁或恢复目录明确提示人工检查，不能自动删除他人锁。
  --dev 后端在宿主生成接入 POM，顶层源码/资源软链接到外部；前端整目录链接，外部文件不复制/改名/修改。
  Maven 沿链接解析真实路径，不能直接链接整个后端模块后假定 ../../pom.xml 仍指向宿主。
  接入 POM/所有权与源码链接纳入校验和备份；remove 只移走宿主接入目录/解除链接，不删除外部文件。
  卸载不删除数据库表/数据，并提示重新构建部署、停用关联菜单和保留迁移历史。
- Node 增量验证覆盖命令、负例、ZIP、Git 覆盖、故障注入和回滚，生成 full/minimal-admin 验证同一工具交付。
  复跑模板/生成 full DB 桩；合成测试插件仅用于 Maven/UI 静态装配验证，不把其当 T9 已交付或真实运行验收。
  冻结 T7 输入只带入工具增量，全文件允许差异精确限制为插件/共享运行工具，不改写 T0 基线。

### 2026-10-07 T8 结果

- add/--force/--dev/list/remove 已实现；根 forge:plugin 脚本注册，无第三方安装器运行依赖。
  复用 forge:create 的改名规则，Maven/Java/模块前缀独立；UI-only 的 undefined 文本及未知二进制不误改。
- 新增 118 项；模板 247/247、生成 full DB 桩 30/30，合计 277 项；工具回归 217/217。
  覆盖严格描述、版本、ZIP、POM、Git/摘要、12 组写入故障及并发锁替换/文件改动保护。
- 两套实时工程安装合成 ZIP 后 Admin 聚合 package 和 Admin UI 生产构建通过。
  包内插件 JAR 与 Reactor 输出逐字节一致，运行描述/Start-Class/前端 view-loader 的插件页面路径验证通过。
- 开发模式直接链接模块的 Maven parent 解析实测失败；宿主接入 POM + 源码链接修复后 package 通过。
  构建产物 target/.flattened-pom.xml 不误报源码改动；构建后卸载通过，外部插件所有原文件字节不变。
- 冻结 full 8343/minimal-admin 5077 文件；相对 T7 仅新增 14 个运行工具、修改插件命令入口。
  其它文件路径/原字节不变；原 T0 清单不动。校验目录 /private/tmp/forge-plugin-t8.Dxg2B6。
- 无真实 MySQL/Redis、服务启动或真实菜单/权限端到端验收；T9 示例、T10 门禁、T11 文档及 T12 人工验收未完成。
