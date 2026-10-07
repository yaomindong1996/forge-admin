# 插件化底座 Test Spec

## 1. 改名基线（T0 / T5 / T6）

执行位置：仓库根目录；模板输入必须按 `baseline/README.md` 冻结，不能拿后续变更的工作区直接对比。

```bash
pnpm forge:create -- "$baseline_dir/forge-baseline-full" --base-package com.acme.demo --preset full
pnpm forge:create -- "$baseline_dir/forge-baseline-min" --base-package com.acme.demo --preset minimal-admin
```

- T0：记录两套工程全部文件的相对路径和 SHA-256。
  `baseline_dir` 是通过 `mktemp -d` 新建的专用目录；固定末级目录名决定项目名，禁止 `--force` 覆盖已有工程。
- T5：重构后以相同参数重新生成，与基线逐项比对，差异必须为 0。
  比对时只向冻结输入替换生成器及其依赖，不能混入 T1–T4 的生产源码修改。
- T6：差异只允许出现在以下几处：
  - 新增的 `package.json`、`scripts/`；
  - `forge.config.json` 的 `forgeVersion`、`plugins` 字段；
  - `.gitignore` 去掉的模板区块。

## 2. Node 单测（无需数据库和 Maven）

```bash
node --test scripts/forge-shared scripts/forge-plugin scripts/guards
node --test forge-server/scripts/db/clean-db.test.mjs
```

| 用例 | 预期 |
|------|------|
| rename：pom 先改 groupId，再做文本替换 | groupId 与 basePackage 不同时，pom 与 Java 结果都正确 |
| rename：Java 目录移动 | `com/mdframe/forge/...` 移到新包路径，空目录被清理 |
| plugin add：模板仓库 | 不改名；文件落到 `forge-server/plugins/`、`src/views/plugins/hello/`；标记区块写入 |
| plugin add：生成工程 | 包名、groupId、模块名与工程一致；`forge.config.json.plugins` 写入 |
| plugin add：版本不兼容 | 失败，不写任何文件 |
| plugin add：重复安装 | 未加 `--force` 失败；加了但插件目录有未提交修改仍失败 |
| plugin add：非法描述 | `id` 格式、`edition` 与 `features` 前缀不一致时失败 |
| plugin add `--dev` | 模板仓库创建软链接；生成工程拒绝 |
| plugin list / remove | 列表正确；删除目录、标记区块和配置记录，不触碰数据库 |
| check-edition | `ee` 包名、`ee` 描述、非空标记区块、被跟踪的插件目录各报一次错；干净仓库通过 |
| clean-db | 主库/插件历史表不在 DROP/TRUNCATE/逐行 DELETE 计划中，显式删除在连接数据库前拒绝 |
| forge:create | `.gitignore` 无模板区块；`forge.config.json` 含 `forgeVersion`、`plugins` |

## 3. Java 单测（需 Maven；T1 已用临时工具执行）

```bash
cd forge-server
mvn -pl forge-framework/forge-starter-parent/forge-starter-plugin,forge-framework/forge-plugin-parent/forge-plugin-system -am test -Penable-tests
```

| 用例 | 预期 |
|------|------|
| `ForgeVersion.satisfies` | `>=1.2.0 <2.0.0` 对 1.2.0 / 1.9.9 为真，对 1.1.3 / 2.0.0 为假；非法表达式抛错 |
| `CommunityFeatureGate` | 空编码启用、`ee.*` 未启用、其它编码启用 |
| `FeatureGateInterceptor` | 方法注解优先于类注解；未授权返回 403 与约定文案 |
| `PluginRegistry` | 重复 ID、非法字段、版本不兼容时启动失败，错误信息含插件 ID 与版本范围 |
| `PluginFlywayMigrationStrategy` | 主迁移先执行；插件表名推导正确；插件 `baselineVersion` 为 0 |
| `SysResourceServiceImpl.getUserResources` | 超级管理员和普通用户都隐藏 `ee.*` 资源；空编码资源保留 |
| `UserLoadServiceImpl` | 普通用户的按钮权限、接口权限不含 `ee.*` 资源 |

## 4. 人工验收（真实 MySQL 8 + Maven）

1. 生成工程：`pnpm forge:create -- ../plugin-check --base-package com.acme.check --preset minimal-admin`。
2. 在生成工程中执行：`pnpm forge:plugin add <模板仓库>/plugins-samples/forge-plugin-hello`。
3. 用 `init-db.sh --recreate --clean` 初始化库，然后编译启动：`mvn -pl <admin模块> -am package -DskipTests`。
4. 检查：
   - `SHOW TABLES LIKE '%_plugin_hello_history'` 存在，且记录 `V1.0.0` 成功；
   - admin 登录后菜单出现“示例插件”，页面可打开，`GET /plugin/hello/info` 返回插件版本和框架版本。
5. 授权过滤：
   - 执行 `UPDATE sys_resource SET feature_code = 'ee.test' WHERE ...`，重新登录后菜单消失；
   - 给示例接口加 `@RequiresFeature("ee.test")` 后重启，超级管理员调用返回 403。
6. 再次启动，Flyway 主库和插件都报告 up to date。
7. 执行 `clean-db.sh --execute --yes` 后再启动，插件脚本不重复执行。

## 5. T0 增量验证（2026-10-07）

- 清单工具：`node --test code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs`。
  覆盖点文件、空文件、二进制、确定排序、增删改检测、软链接拒绝、失败退出码及基线覆盖拒绝。
- 按初始化 Skill 复跑模板和生成 full 工程的 `init-db.test.mjs` / `clean-db.test.mjs`；MySQL/Maven 全部为桩。
- 用 `manifest.mjs verify` 比较独立输出目录中的两轮生成结果，文件集合和每个文件内容均必须相同。
- 对修改的 3 个 POM，以及两套工程全部 POM 执行 `xmllint --noout`。
- 静态验证模板/生成工程的根版本、框架版本继承、BOM 版本和根 BOM 引用均一致；数据库迁移版本不随核心版本修改。
- 本轮无 Java/前端业务代码变更，不执行 UI 构建或真实数据库初始化。Java 编译受本机工具缺失限制，不能以静态检查代替编译通过。

### 用户确认的 1.2.0 修正

- 在同一源提交上应用修正后的版本补丁，重新生成 full/minimal-admin 并记录摘要，再重复生成验证。
- 与初次 1.1.0 清单对比，允许差异仅为两个预设各自的根 POM、独立 BOM 的版本内容；不允许新增/缺失文件。
- 用户明确确认重新生成，是版本更正，不是后续改名回归中为了通过测试而重录基线。
- 复跑清单工具、模板/生成 full 数据库桩测试及 XML/版本链检查；旧基线可从 `369f345b` 恢复。

## 6. T1 增量验证（2026-10-07）

- Java：版本范围、非法比较式、SemVer 预发布排序、构建元信息；社区 Gate 三类编码及空白处理。
- Registry：空 classpath、多个目录/JAR 描述、确定排序、重复 ID、字段非法、未知字段、重复 JSON 键、
  版本不兼容、不可读/过大文件、标量隐式转字符串；普通/全局懒加载容器均必须启动失败，返回列表不得可变。
- Servlet：方法注解优先于类注解，继承/组合注解、无注解和静态资源正常通过；拒绝返回 403 和统一 JSON。
  MockMvc 验证登录拦截器先执行、实际 MVC 注册生效；自定义 FeatureGate 替换默认实现且只有一个 Bean。
  覆盖用户配置 Bean 和先于默认配置的客户自动配置两种接入方式。
- Maven：先构建/安装独立 `forge-dependencies`，再以 `-am -Penable-tests` 运行新 starter 及核心依赖测试；
  Admin 聚合编译验证主应用依赖装配。不得依赖其他版本的旧 starter jar 代替 Reactor。
- 脚手架：隔离生成 full/minimal-admin，检查新 starter 保留、BOM/依赖改名一致、版本资源正确读取及自动配置类名改名；
  复跑模板和生成 full 的数据库脚本桩测试。不更改 T0 输出比对标准。
- 不执行真实 Flyway、登录接口或企业授权校验，尚未实现的 T2–T12 不标为完成。

### T1 执行结果

- 模板与 full/minimal-admin 三套工程各 106 项 Java 用例通过，失败/错误/跳过均为 0。
- 最新 Admin 聚合编译通过；模板和生成工程 POM 均可解析，full 54 个、minimal-admin 35 个。
- 清单工具 5 项、模板数据库脚本 18 项、生成 full 数据库脚本 18 项通过；均未连接真实 MySQL。
- 首轮 Mockito 不可读资源负例的重新 stub 触发旧异常，已改为独立 resolver 夹具并完整复跑；未弱化断言。
- T2 迁移、T3 权限过滤和真实插件端到端验收尚未实现/执行，后续按既有矩阵增量补测。

## 7. T2 增量验证（2026-10-07）

- 历史表：模板/改名工程、短横线 ID、最大长度边界、非法字符/后缀、相邻插件 ID 不冲突。
- 策略：主迁移先执行、插件按 ID 排序、无插件/无 SQL 不创建历史、主失败不执行插件、插件失败停止后续。
- 配置：复制不污染主配置，保留数据源/ClassLoader/Schema/编码/占位符/命名及校验策略；
  独立路径、历史表、baselineVersion=0，不重复执行主 Java/custom resolver/resource provider 的脚本。
  有限 target、ignoreMigrationPatterns 和 baselineOnMigrate=false 明确继承；全局回调保留。
- 自动配置：Boot 的真实 Flyway 初始化器选中本策略；开关关闭/类缺失不装配，自定义策略可替换；
  依赖初始化器的 Bean 等待所有插件完成，迁移失败容器启动失败。
- H2：随机内存库，主迁移创建业务表后插件 SQL 写入；非空库首装仍执行 V1.0.0，重复启动不重跑，
  两插件同版本不冲突、未注册插件目录不执行、目录/JAR/嵌套资源可发现；不连接真实 MySQL。
- 回归：复跑 T1 106 项测试、Admin 聚合 package、模板/生成 full 的数据库桩测试；
  隔离生成 full/minimal-admin 验证改名工程的新自动配置及目标模块测试，不重录 T0 基线。

### T2 执行结果

- 48 项新增测试：计划 23、工厂 3、策略 4、H2 集成 11、Boot 自动配置 7；连同 T1 共 154 项。
  模板/full/minimal-admin 各 154/154 通过，失败/错误/跳过均为 0。
- Admin 聚合 package 成功，包内 starter 摘要与本轮 jar 一致；模板/生成 full 数据库脚本各 18 项、清单工具 5 项通过。
- 首轮发现空 resolvers 重载歧义；随后发现 H2 TABLE 元记录及 target 精确版本规则，已修正类型与测试夹具。
  一次同工作区并行 package/test 导致 class 文件读取失败，改为串行完整复跑通过，未跳过测试或降低断言。
- H2 2.3.232 比 Flyway 10.20.1 声明的 H2 支持版本新，有兼容告警；使用已有依赖，不升级全项目版本来消除提示。
  所有 H2 库均随机命名且关闭时 SHUTDOWN；未连接实际开发库，真实 MySQL 与 T3–T12 仍未验收。

## 8. T3 增量验证（2026-10-07）

- 实际调用 SysResourceServiceImpl 的当前菜单/资源树/权限/资源 ID 方法；管理员与普通用户均不返回
  未启用资源，普通隐藏页/空编码/社区编码不被误删；无角色/无资源返回空，未登录保持原行为。
- 用自定义 Gate 验证允许 ee 编码与禁止社区编码，不把企业前缀规则写死在 System；Gate 异常不得放行。
- 调用 UserLoadServiceImpl 权限加载方法，验证普通按钮/API 权限无禁用编码；API Mapper 只收到允许 ID，
  空集不调用 pattern 查询，重复/空白 pattern 仍规范化；管理员通配与用户类型边界保持不变。
- 捕获原有 Wrapper，验证角色 ID、租户 ID、默认/明确客户端和用户类型范围仍存在；不通过 mock 自动筛选
  功能资源，混合启用/禁用资源由生产过滤逻辑真实处理。复跑现有用户加载、资源授权与 API 权限回归。
- 迁移静态契约：版本唯一、information_schema 当前库/表/列防重复、可空 64 字符、AFTER perms、
  PREPARE/EXECUTE/DEALLOCATE 配对、无 UPDATE/DELETE/旧脚本改写/业务占位符；实体映射与新依赖可编译。
- Maven：System 与 starter-plugin 连同依赖模块执行 -am test -Penable-tests，再串行 Admin package；
  隔离生成 full/minimal-admin，验证新增依赖及改名后的目标用例；按 Skill 复跑模板/生成 full 的 DB 桩测试。
- 不启动真实业务服务或连接开发库；真实 MySQL 迁移、登录会话刷新和企业插件授权端到端仍待 T12。

### T3 执行结果

- 新增 32 项（当前资源 14、权限快照 12、迁移/实体 3、Spring/公开用户加载 3）。System 160 项和
  starter-plugin 154 项在模板、改名 full/minimal-admin 各通过，失败/错误/跳过均为 0；相关上游测试同时通过。
- 首轮 System 157 项中，新增 29 项均通过；1 项旧安全契约因已删初始化副本而报 NoSuchFileException。
  对齐权威全量 SQL 和脚本实际调用路径，并新增 3 项装配测试后完整复跑，未禁用/删除测试或降低断言。
- Admin 聚合 package 退出 0，新 System/starter 内嵌构件摘要一致；POM XML、模块清单和改名引用通过。
- 模板 DB 桩 18 项、生成 full DB 桩 18 项、基线工具 5 项通过。新增迁移无业务占位符；旧 V1.0.72 的
  4 处存量命中未修改，主配置原本关闭 placeholder replacement。未将静态迁移契约表述为 MySQL 执行通过。
- 未改前端、不执行 UI 构建；无真实服务/数据库写入或待清理业务进程，T4 清理历史保护仍未完成。

## 9. T4 增量验证（2026-10-07）

- 复用已通过的 DB 脚本桩测试和 T0 清单工具，不连接真实 MySQL；固定用 /bin/bash 验证 macOS bash 3.2。
- P0：模板主历史表、原前缀/任意改名前缀插件历史表、大小写表名、含数字和下划线的插件 ID 均保留。
  预览 DROP/TRUNCATE 列表、打印 SQL、--execute --yes 交给 MySQL 桩的计划均不得包含这些表。
- P0：给历史表额外提供 tenant_id/del_flag/deleted 列，证明不会被通用逐行清理误删；
  显式 --drop-table（含大小写或同时 --keep-table）指向历史表时，非零退出且没有任何 MySQL 调用/写入。
- P1：普通业务历史和备份/临时副本仍清理，普通表显式删除与 --keep-business-tables 的行为不变；
  缺主迁移、缺管理员仍拒绝，追加自定义 SQL 保持原有顺序，帮助文本说明保护范围与自定义 SQL 责任。
- 命令：/bin/bash -n clean-db.sh；node --check clean-db.test.mjs；模板运行 init-db/clean-db 桩测试与
  baseline/manifest.test.mjs。生成改名 full 工程后复跑同类 DB 测试，核对主历史表名称替换与全量 SQL 一致。
- 本轮只有 Shell/Node 脚本与阶段文档变更，不跑 Java/UI 全量构建；真实 MySQL 清理后重启、插件 checksum
  与二次启动验收仍留给 T12，不把测试桩中的 SQL 计划通过表述为真实数据库验收通过。

### T4 执行结果

- clean-db 新增 12 项，共 18 项；加上 init-db 12 项和基线工具 5 项，模板 35/35 通过。
  改名 full 的两类 DB 桩共 30/30 通过，失败/错误/跳过均为 0，全部数据库执行为桩。
- --drop-table 的 7 个表名变体各验证单独 drop 和 keep/drop 冲突，均在 MySQL 调用前失败；
  其它 5 项新测试覆盖预览/执行保留、备份副本、业务表模式、帮助边界，普通表写入断言仍保留。
- bash 3.2.57、Node 语法、新增行宽 <=120、改名一致性与 diff 检查通过；T0 清单未变。
- 未修改 Java/UI/Flyway SQL，不执行相应全量构建或真实数据库清理；无本轮业务服务或残留服务 PID。
