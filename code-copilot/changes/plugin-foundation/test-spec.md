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
2. 在独立空验收库中按「全量 SQL → required seed → 全部主迁移 → clean」初始化基础库。
   2026-10-07 顺序修正：遵循 T11 技能，禁止先装插件再清理基础库；不对现有业务库使用 --recreate。
3. 在生成工程执行 `pnpm forge:plugin add <模板仓库>/plugins-samples/forge-plugin-hello`，
   然后编译启动：`mvn -pl <admin模块> -am package -DskipTests`。
4. 检查：
   - `SHOW TABLES LIKE '%_plugin_hello_history'` 存在，且记录 `V1.0.0` 成功；
   - admin 登录后菜单出现“示例插件”，页面可打开，`GET /plugin/hello/info` 返回插件版本和框架版本。
5. 授权过滤：
   - 执行 `UPDATE sys_resource SET feature_code = 'ee.test' WHERE ...`，重新登录后菜单消失；
   - 给示例接口加 `@RequiresFeature("ee.test")` 后重启，超级管理员调用返回 403。
6. 再次启动，Flyway 主库和插件都报告 up to date。
7. 执行 `clean-db.sh --execute --yes` 后再启动，插件脚本不重复执行。

### T12 已授权执行环境（2026-10-07）

- 参考 lawhub/deploy 的主机/基础设施配置；192.168.66.158 的 forge-mysql 8.0.46 已健康。
  独立库名 forge_plugin_t12_20261007_0y7usg；只创建该库的临时测试账号，不改原用户权限。
- 本地隔离目录 /private/tmp/forge-plugin-t12.0y7UsG；新生成 plugin-check / minimal-admin。
  复用 T9–T11 已通过的 Java/Node/UI 结果，重点补真实 MySQL、Redis、登录及插件生命周期。
- 本机没有 MySQL CLI；可通过 SSH 调用既有容器内真实 mysql 客户端导入/清理，
  本地 Maven/服务通过回环 SSH 隧道访问同一验收库；必须记录真实 SQL 执行而非数据库桩。
- 普通用户只在验收库建立，先测试未授权，再授权 hello 菜单/按钮/API；重新登录验证快照过滤。
  ee.test 测试覆盖管理员及普通用户，未登录仍由认证拦截器拒绝。
- 不重启/修改原容器、不接触 lawhub/CRM 业务库、不输出密钥；收尾核对原容器身份/健康状态，
  验收账号/库/Redis/隧道/本地服务只按明确创建清单清理。未通过项保留失败证据，不勾选 T12。

### T12 实际执行结果（本轮后端部分）

- 新生成 minimal-admin，独立 BOM 安装及 5 次 hello 测试构件 Reactor package 通过；
  实际主迁移达到 1.0.209，hello 首装历史含 baseline=0、成功 V1.0.0，未给已有角色授权。
- basic/granted/filter/gate 四阶段各 13 个 HTTP 步骤；清理后重启/升级/重装各 7 个，共 73 个通过。
  登录使用 RSA/正常 Sa-Token，普通用户通过真实租户成员和当前组织角色绑定，不使用内部调用替代登录。
  QA 初始化默认验证码关闭，本轮仅在 QA 库加强为图形验证码，并用已有 local-profile 回显完成 API 测试。
- 管理员/普通用户菜单与普通用户按钮/API 权限快照满足矩阵；RequiresFeature 未开通返回 HTTP 403。
  原有 RBAC 拒绝沿用 HTTP 200 + 业务 code=403，匿名请求业务 code=401；未将其混淆为 Gate 的 HTTP 403。
- clean 前后及重启后：主历史 209 条、hello 历史 2 条，包含安装时间的完整摘要一致。
  临时 1.0.1 交付包保留旧 SQL 字节，仅追加标记迁移；升级后 hello 历史 3 条，CLI 卸载/重装后摘要不变。
- 初轮离线 Flyway 插件解析、Maven 缓存写权限、测试夹具和 pnpm 11 ignored-builds 失败均保留日志，
  不作为成功结果；用正确依赖上下文/权限及完整夹具复跑。直接启动已有 Vite 成功，不宣称 pnpm install 成功。
- 文档契约 6/6、check-edition 通过；生产代码未变，复用 T9/T11 相关 Java/Node/UI 证据。
  浏览器真实页面仍未验收，等待本地验证码填写确认；资源最终清理未执行，不勾选整个 T12。

### T12 浏览器与最终收尾结果

- 上述待确认记录为中间状态。后续读取时已处于正常登录后的首页，Agent 未填写/提交验证码。
  真实“示例插件”菜单打开 /plugins/hello；页面显示 hello / 1.0.1 / 1.2.0，刷新有加载态且恢复数据，
  无错误态；plugin-page.jpg 记录实际页面，不是 T9 的模拟接口页面。
- cleanup-final.log 成功：测试库先备份、SHA-256 留存，再删除本轮测试库/账号，验证均为 0；
  专有 Redis 的 ID/owner 必须与创建记录一致才停止/删除，配置目录只允许包含 redis.conf 才清理。
  原 MySQL/Redis 容器身份一致；本机 13316/16316/18590/3132 均无监听，临时浏览器标签页已关闭。
- T12 全部完成；正式提案实现待 Review / 归档。没有残留运行中的本轮验收服务或服务器资源，
  有限的临时工程、私有测试备份、日志/截图留存，不进入提交。

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

## 10. T5 增量验证（2026-10-07）

- P0：groupId 与 basePackage 不同，POM 使用 groupId，Java/Mapper/自动配置使用 basePackage；
  root parent、具体 Docker/H5 前缀、旧/新模块路径、启动类名替换顺序保持一致。
- P0：main/test Java 包目录移动、空父目录清理、目录合并、已有目标文件/目录的原有规则、
  同包名 no-op；PNG 等二进制字节不改，文本/点文件/SQL 仍按同一张表替换。
- P1：full/minimal 的 report/H5 条件、SSO 配置行保留/删除、自定义前端路径/端口/代理、
  不同模块 artifact 前缀、扩展插件模块 map、未知目录/空目录和原样文本处理。
- node --check 生成器与共享模块；node --test scripts/forge-shared，并复跑已有清单工具与 DB 桩测试。
- 按 baseline/README.md 重建冻结模板，不混入当前模块目录和业务文件，带入生成器及全部共享依赖；
  两个预设用 manifest.mjs verify 对照冻结清单，新增/缺失/变化文件均为 0，不归一化或忽略任何输出。
- 当前模板另生成 full/minimal-admin，核对版本/插件模块/依赖/历史保护规则，改名 full 复跑 DB 桩测试；
  根据生成集成范围执行离线 Admin 聚合编译。没有 UI/数据库业务变动，不启动业务服务或初始化真实库。

### T5 执行结果

- 新增共享规则 25 项，模板合计 60/60、实时改名 full DB 桩 30/30；失败/错误/取消/跳过均为 0。
  初轮报表条件用例误认为旧模块别名也不替换，核对原规则后修正精确期望，再完整复跑；未改变生产行为迁就测试。
- 抽取前后 10 组映射与完整有序替换数组一致；冻结 full 8324/minimal-admin 4866 文件的
  missing/added/changed 均为 []，保留全部原始字节，不更新基线或增加忽略路径。
- 两套当前生成工程 54/35 个 POM XML 及版本/新 starter/System 依赖/自动配置/迁移/历史保护检查通过；
  生成器和共享模块 Node 语法、新增行宽 <=120、辅助方法 <=80 行、git diff --check 通过。
- 两套离线 Admin 聚合 compile 均失败：full 最先报 plugin-data 打印类型缺失，minimal-admin 最先报
  plugin-generator 打印类型缺失；原 module-catalog 未包含 plugin-print，冻结输出同样缺模块和依赖。
- 直接 javac -proc:none 对生成 Admin 源文件确认 public 类名/文件名冲突；未提供外部类路径，该调用只用于
  命名错误诊断，不视作聚合编译或其它依赖验证。两个旧问题待单独修复，T5 未改变其冻结输出。
- 未执行 Java 新测试/UI 构建/真实数据库迁移：本轮没有这些源码或协议变动；不启动业务服务，暂无服务 PID。

## 11. T5-F1 增量验证（2026-10-07）

- P0：品牌替换与 AdminApplication 不再前缀冲突，普通品牌文本仍改名；main 引用和类文件与映射一致，
  覆盖自定义类前缀、含 ForgeAdmin 的目标前缀、原包名 no-op 及扩展插件类。
- P0：plugin-print 描述、目录和内部直接 POM 依赖齐全；generator/data 的编译闭包包含打印模块，
  generator 的 data/external 直接依赖不会被裁掉；四个 preset 的依赖解析均无未知 ID。
- P0：重新生成实时 full/minimal-admin，解析所有 POM，安装独立 BOM，再 Admin 聚合 package。
  确认启动类文件/类名/main 引用一致、打印模块登记/坐标一致、minimal-admin 保留 AI 降级适配器。
- P0：应用集成的三项依赖全部选中时保留，分别缺失时仅裁剪 integration 主/测试包和精确 Mapper 文件；
  缺失目录可安全重复处理，其它源码/测试/Mapper 不变。生成 full 保留 11 个文件，minimal-admin 不残留。
- 冻结输入单独生成并对照 T0：列出完整差异，核对仅本轮声明的命名/坐标/模块登记/闭包/config 变化；
  SQL 模块按来源和内容逐文件验证，额外移除的应用集成文件必须是上述 11 个精确路径。
  不重录原清单，不以目录全忽略代替内容审核。T6 仍未实现，不允许新增其工具/config 字段。
- node --test 共享规则/新增目录清单测试/基线工具/模板 DB 桩，生成 full 复跑 DB 桩；
  Node 语法、XML、行宽/规模及 git diff --check。无 Java 业务修改，不重跑已有全部 Java 单测或真实 DB/UI 联调。

### T5-F1 执行结果

- 目录清单 9 项、可选接入层裁剪 5 项、共享规则新增 2 项；本轮增量 16 项。
  模板合计 76/76（含已有共享规则/清单工具/DB 桩）、实时 full DB 桩 30/30，失败/取消/跳过均为 0。
- 初次修复后 full package 成功；minimal-admin 失败于未选择能力开放模块但保留了 Admin 应用集成。
  明确追加 Spec 边界并增加裁剪测试后，从最终脚本重新生成，两套 BOM install/Admin package 均退出 0。
- 54/37 个 POM XML、声明/文件名/main/包内 Start-Class、打印 jar 摘要、最小预设 AI 降级及 full 保留
  11 个应用集成文件/minimal-admin 全部裁掉均通过。Node 语法/新增行宽/辅助方法规模/diff 检查通过。
- 冻结 full missing=112/added=112/changed=10；minimal-admin missing=125/added=317/changed=9。
  打印模块移路径文件按唯一坐标替换逐字节验证；新增 data/external 与冻结 full 的同源改名内容一致；
  两套 54/41 个 SQL 按冻结源与明确改名规则逐字节验证，新增只有 9 个 data/external 来源，消息仅重排序号。
- 所有 19 个 changed 文件的完整 diff 人工审核；应用集成删除精确为 6 个主源码、4 个测试、1 个 Mapper。
  未修改 T0 原始清单。首次 SQL 来源审计误漏 forge_admin_new 的先替换规则，修正审计期望后完整通过，
  未改生成器或 SQL 迁就审计。没有真实数据库/UI/服务启动或 Java 全量单测验收。

## 12. T6 增量验证（2026-10-07）

- P0：revision 从根 POM properties 读取，注释不参与；缺失/重复/变量/非法版本拒绝，支持合法预发布/构建信息。
  CLI 在版本错误时不创建目标；生成配置 only forgeVersion/plugins 新增，plugins=[]。
- P0：根 package 只有 forge:plugin，运行入口/共享模块/原始 catalog 完整可导入，工具字节不参与业务改名；
  不复制测试/夹具/本地文件或 forge:create。验证不同项目/artifact/包名前缀时映射仍从原始模块到目标模块。
- P0：.gitignore 无区块保持字节，完整区块只移除内部内容，LF/CRLF/无末尾换行和相邻规则保留；
  嵌套/重复/未闭合/反向标记报错，不扩大忽略删除范围。
- P1：--help 与无参可运行；add/list/remove 尚未开放时非零且无文件写入，未知命令明确报错。
  在生成工程任意 cwd 用绝对入口验证，再从工程根用 pnpm forge:plugin --help 验证脚本目标。
- 冻结源先运行 T5-F1，再运行 T6，两套预设完整 SHA-256 比较：只允许显式新增运行文件/package、
  forge.config.json 两字段和模板区块删除；原业务源码/POM/SQL/图片/点文件原始字节不变，不重录 T0。
- Node 语法/新测试/既有共享规则与目录测试/基线工具/模板 DB 桩；实时 full DB 桩；
  POM XML、生成工具路径/导入/原字节及 diff 检查。无 UI/生产 SQL/Java 业务变动，不跑真实 DB 或服务联调。

### T6 执行结果

- 新增 project-tools 25 项、插件入口 8 项；最终模板共 109/109，生成 full DB 桩 30/30。
  合计 139 项，失败/取消/跳过均为 0；数据库脚本和迁移调用全部为桩。
- 版本读取含合法预发布/构建信息、非法及重复配置、CLI 失败前不建目标；精确区块剥离及损坏标记拒绝，
  工具递归复制/本地文件排除/软链接拒绝、配置原字段保持、实际共享模块二次改名均通过。
- 最终冻结输出 full 8329/minimal-admin 5063 文件；相对 T5-F1 两套 missing=0/added=5/changed=1。
  唯一改变的 config 删掉 forgeVersion/plugins 后与旧字节相同；新增工具/catalog 原字节、根 package 精确核对。
  夹具模板区块删除后 .gitignore 与 T5-F1 相同；配置写入抽取前后全文件 manifest 零差异，不更新原 T0 清单。
- 最终实时工程 54/37 个 POM XML、任意 cwd 的工具帮助/拒绝安装及 config 不变检查通过。
  两套 pnpm forge:plugin --help 在前一轮实时工程通过；pnpm 自身产生 node_modules/lock 文件，
  这些只在临时验证目录出现，不属于生成输出，也未混入冻结清单比较。
- 两套最终实时工程先 install 独立 BOM 再 Admin 聚合 package -am -DskipTests，四个命令全部退出 0。
  Start-Class 与生成类名一致，包内打印 jar 与本轮 Reactor jar 摘要一致；Node 语法/规模/行宽/diff 检查通过。
- 无生产 Java/UI/SQL 改动，不重跑全部 Java 单测，不启动服务或真实 MySQL/Redis；安装全链路仍待 T8–T12。

## 13. T7 增量验证（2026-10-07）

- P0：根 modules 与 Admin dependencies 各有唯一、完整、空标记，位置在对应直属区块内部，命名符合 F5。
  注释以外的源 POM 字节不变，不改变模块/依赖次序或引入新依赖。
- P0：生成器重写 modules 保留合法空标记；无标记输出与旧规则精确一致、缺文件 no-op。
  重复/未闭合/反向/区块外标记及非空区块拒绝，不能静默丢掉插件登记。
- P1：实际共享改名模块处理不同 groupId/basePackage/artifact 前缀，标记不改名。
  full/minimal-admin 从当前模板重新生成，验证标记、裁剪闭包、所有 POM XML；Maven 模型验证及 Admin 聚合编译。
- 冻结模板复用 T6 已审计输入，先复现其最终输出，再只添加本轮 POM 注释和模块渲染器。
  全文件 manifest 相对 T6 只允许两个 POM 变化；删除精确新增注释后恢复旧 POM 字节，不忽略其它路径。
- 复跑已有 Node 工具回归、模板 DB 桩和重新生成 full 的 DB 桩；Node 语法/形态/diff 检查。
  本轮不改 Java/SQL/UI，不执行全量 Java 单测、真实数据库迁移或服务级/安装端到端验收。

### T7 执行结果

- 新增模块渲染/标记 20 项：旧渲染/LF/CRLF、空标记裁剪/幂等、损坏/重复/错位/非空拒绝、
  失败文件不写、缺文件 no-op、两源 POM 空标记及实际共享改名。模板共 129/129、生成 full DB 桩 30/30，
  合计 159 项，失败/取消/跳过均为 0。行宽收敛后工具类 99 项与关键 45 项复跑通过，数据库执行均为桩。
- 模板源 POM 删除精确新增的两行注释后与 T6 提交逐字节一致；两套冻结 T6 输出复现零差异。
  T7 full 8329/minimal-admin 5063 文件，相对 T6 各 missing=0/added=0/changed=2，精确为根/Admin POM。
  删掉注释恢复原输出字节，其它文件 SHA-256 不变；没有重录 T0 或增加忽略/归一化规则。
- 实时 full/minimal-admin 的 54/37 个 POM XML 及声明的子模块目录全部通过；
  xmllint XPath 验证标记位于 project 的直属 modules/dependencies 中，唯一且为空；版本/plugins 配置不变。
- 模板 Admin 聚合 compile、两套生成工程 BOM install/Admin validate/Admin 聚合 compile 均退出 0，全部离线。
  Node 语法、新文件行宽/方法规模/参数/diff 检查通过；新模块/测试分别 44/129 行。
- 仅 POM 注释和生成器渲染变更，无新依赖或 Java/UI/SQL 改动，不执行全量 Java 单测或 package；
  编译与模型验证不能代替真实数据库、服务启动或插件安装全链路；本轮没有启动业务服务。

## 14. T8 增量验证（2026-10-07）

- P0：目录/ZIP 安装到模板及改名工程，连续安装不误判模板；UI-only/server-only、版本与运行描述一致性。
- P0：SemVer 完整范围/预发布、大数字及非法范围；描述字段/重复键/路径/模块名、ZIP CRC/大小/条目安全校验。
- P0：POM 标记唯一、直属位置、配置一致；冲突目录/模块/链接拒绝；源包和宿主越界路径不可访问/覆盖。
- P0：重复安装必须 --force；Git dirty/untracked/ignored 和无 Git 摘要保护，成功升级保留旧源码备份。
- P0：源码复制、POM/config 写入等故障注入恢复旧状态；并发锁和宿主文件并发修改不得静默覆盖。
- P1：--dev 仅模板目录，源包不变，卸载不删链接目标；list/remove 排序与缺少 ID 提示、数据库保留指引。
- 工具单测/既有生成器回归及模板/生成 full DB 桩；两套新工程使用已复制工具安装合成插件并 Maven 验证。
- 冻结 T7 输出差异只放行明确运行工具；工具字节与源相同，其它输出不变。无真实 MySQL/服务/License 验收。

### T8 执行结果

- 模板 Node 247/247（比 T7 新增 118 项）；生成 full DB 桩 30/30，总计 277 项；工具回归 217/217。
  Node v20.19.0，无失败/取消/跳过。测试安装器不执行包内脚本，不运行数据库迁移。
- 覆盖 copy/dev 转换、UI-only/server-only、多插件排序、源码/Git/摘要保护及所有权标记。
  12 组写入故障恢复源码/POM/config；保留被替换的他人锁并返回警告，拒绝覆盖预检后发生的宿主改动。
- 开发模式真实 Maven 验证先失败（整个模块软链接导致父 POM 走外部路径），修复为宿主 POM/源码链接后通过。
  测试覆盖外部源码编辑、接入 POM 改动拒绝和构建输出忽略；真实构建后卸载保留外部包原字节。
- 两套实时生成工程安装合成 ZIP，离线 BOM install/Admin package 均退出 0。
  复用本机现有依赖，Admin UI Vite 生产构建退出 0（40.35s/39.88s，未联网安装依赖）。
  JAR 原字节、META-INF/forge-plugin.json、Start-Class 及编译 view-loader 插件路径断言通过；卸载后登记清空。
- 冻结 full 8343/minimal-admin 5077 文件；相对已复现的 T7 输出仅新增 14 个运行模块、改变命令入口，
  其它文件无新增/缺失/字节变化。最终 raw 工具交付复用 copyGeneratedPluginTools，不混入测试文件或改名工具本体。
- git diff --check、Node 语法及方法/参数/行宽/嵌套/复杂度形态审查通过；未改 Java/UI/SQL 生产源码。
  未启动服务/真实 MySQL，真实插件迁移、普通用户菜单及接口授权仍待 T9/T12；不执行无关全量 Java 单测。

## 15. T9 增量验证（2026-10-07）

- P0：示例根/运行描述一致，继承核心 revision；Registry 加载、组件扫描、GET 类型响应、RBAC/功能注解。
- P0：实际插件 SQL 在独占 H2 MySQL 模式执行；首次、重复启动、直接重跑 SQL、逻辑删除历史后重建，
  菜单路径/组件/API 权限/tenant_id/feature_code/is_public 正确，无角色权限扩张，插件历史与主历史隔离。
- P0：实际示例目录与 ZIP 安装到模板/独立包名和模块前缀夹具；源包原字节不变，卸载可恢复且不碰数据库。
- P1：Naive UI 页面显示真实响应、加载态、错误重试、刷新互斥；控件及内容无模拟成功状态。
  使用 Vue 组件单测和无后端浏览器验证亮/暗/窄屏；生产 UI 构建与动态页面路径检查。
- 从当前模板重新生成 full/minimal-admin，安装前 samples/plugins 默认为空；示例安装后相关 Java 单测、
  Admin 聚合 package 与 UI build、JAR 描述/SQL/Controller 产物校验；复跑模板与 full DB 桩。
- 不改 T0 原基线；无主业务源码改动时完整输出与 T8 的差异应为 0。
  不运行真实数据库或业务服务；授权/真实 MySQL 方言、登录与菜单点击全链路留给 T12。

### T9 执行结果

- 模板 Node 254/254（新增 7 项）；生成 full DB 桩 30/30；UI 8/8。失败/取消/跳过均为 0。
  最终行宽整理后单独复跑样例 7/7；全部数据库脚本调用为桩，真实 SQL 实跑仅在随机 H2 中。
- Java 12 项在 dev 模板、改名 full/minimal-admin 各通过，合计 36 次执行；无失败/错误/跳过。
  覆盖真实描述、Controller 包扫描、响应/注解、功能 Gate 拒绝、缺描述失败；迁移真实调用插件执行器。
  RBAC 只核对宿主将执行的 SaCheckPermission 注解，不假装已验证 Sa-Token/Redis 登录与授权会话。
- 实际 SQL 首次、重复启动、直接重跑、逻辑删除重建、停用不恢复、客户路径不覆盖通过；角色关系不变，
  菜单/API 有效关联、租户/客户端/公开标记及独立历史正确。H2 的 Flyway 版本提示非阻断。
- 三套 Admin 聚合 package、两套 UI build 均退出 0；JAR 原字节、Controller/JSON/SQL、Start-Class 和
  动态 /plugins/hello 页面路径正确，生产包不含测试 fixture，UI 不生成 api/__tests__ 页面。
- 冻结输出 full 8343/minimal-admin 5077，与 T8 raw SHA-256 比较零差异；实时两预设默认 plugins=[]、
  不带 samples。安装/构建/卸载后源码指纹不变，宿主恢复备份保留，T0 原始清单未修改。
- ESLint、行宽/规模/diff/SQL 占位符检查通过；隔离 Vite + 模拟接口验证亮/暗/320px、加载、失败、重试，
  两个页面的 scrollWidth=clientWidth（904/320）；截图保存在验证目录，临时页关闭、预览进程已停止。
- 未进行真实 MySQL/Redis、业务服务或普通用户菜单/API E2E；T12 留待人工验收，不扩大本轮授权范围。

## 16. T10 增量验证（2026-10-07）

- P0：干净社区树通过；点号/目录包前缀、路径、二进制、文档、未跟踪源码均拒绝，无目录豁免。
- P0：四类原始违规分别拒绝，两个宿主标记必须存在；重复/错位/损坏/注释非空拒绝，描述严格 JSON。
- P0：工作区正常而暂存 blob 违规仍拒绝；强制暂存忽略插件文件拒绝，暂存删除可通过。
  未忽略的未跟踪文件检查，忽略的本地安装/备份不读取；索引模式冲突/链接/子模块失败。
- P1：任意 cwd、Git 环境变量重定向、Git 不可用、非仓库、换行/中文路径、无法读取及尺寸限制。
  检查前后文件/索引不变；错误只报路径及规则，不输出文件内容、Git blob 或私有链接目标。
- P0：真实根 .gitignore 区块只在模板生效；重新生成 full/minimal-admin 均剥离，允许提交客户插件。
  客户工程没有 guard、check:edition 或默认安装样例；既有 forge:plugin 命令/改名工具仍可用。
- 复用 T9 冻结输入原字节比对：生成器不改，新增 guard/脚本不复制；.gitignore 剥离后旧字节相同。
  实时生成只放行 AGENTS 的新增条件化规则及本轮规则文档改动，不重录 T0 清单。
- 运行 guard 新单测、既有 Node 工具及模板 DB 桩、生成 full DB 桩、语法/形态/diff 检查，实际命令通过。
  没有 Java/UI/SQL 业务改动，不重复 Maven/UI 构建，不启动真实服务或数据库；T12 保持未完成。

### T10 执行结果

- 新增检查契约 41/41，完整模板 Node 295/295、生成 full DB 桩 30/30；失败/取消/跳过均为 0。
  审查后提取 POM 标记校验再跑 41/41，通过；实际 pnpm check:edition 9519 个索引/工作区文件通过。
- 四类违规、索引与工作区不一致、强制暂存忽略文件、暂存删除、损坏 JSON/POM、二进制/中文换行路径、
  链接/父目录置换/子模块/冲突、Git 不可用/对象缺失/环境重定向/replace refs/并发索引、超限均覆盖。
  多批 blob 二进制边界、无写入/无敏感内容输出通过；文档/测试同样检查，忽略本地文件不跟随读取。
- 冻结 full 8343/minimal-admin 5077 与 T9 逐文件原始 SHA-256 零差异，无忽略或归一化清单内容。
  实时 full 8383/minimal-admin 5116 分别只改变 AGENTS.md；精确删除新 5.18 段后旧字节一致。
  两套生成工程剥离真实模板区块、没有 guard/命令/样例；安装 hello 后两端路径均正常加入索引。
- 五个新模块 node --check 通过；AST 60 个函数最大 26 行/3 参数/3 层嵌套，行宽 <=120，diff 检查通过。
  无 Java/UI/SQL 业务改动，不重复 Maven/UI build，不启动真实服务或连接数据库；T12 仍需人工验收。

## 17. T11 增量验证（2026-10-07）

- P0：技能 frontmatter 校验；入口按需指向操作参考，安装/升级/卸载保留明确授权边界，无新 invocation policy。
- P0：作者指南对应真实 descriptor/POM/RequiresFeature/Flyway/installer 契约，命令不新增 unsupported 参数。
  操作参考不引用客户缺失的 samples/guard/forge-docs；根 README 和作者指南本地链接可达。
- P0：重新生成 full/minimal-admin，技能与参考完整复制且路径改名正确；原始作者指南不复制。
  两预设工具 help/list/add/升级/remove 在隔离工程验证；测试 Git dirty 时 --force 仍拒绝覆盖。
- P1：参考的 shell 示例解析/占位符替换后，在隔离工程复跑 Node 命令；不执行真实迁移/授权/部署建议。
  备份与失败恢复说明据实检查，不自动恢复数据库；描述例子用现有严格校验器验证。
- 根 DB 桩及生成 full DB 桩，已有工具回归、实际 check:edition、diff/新增代码语法与形态检查。
  全文件 manifest 只放行技能/参考的明确文档差异，T0 原证据不改；无业务源码改动不重复 Maven/UI build。

### T11 执行结果

- 新文档契约 6/6：严格 JSON 样例与真实描述一致、本地链接可达；文档中的全部插件 CLI 示例经解析后
  在模板/独立包名与模块前缀客户夹具执行，覆盖脏文件拒绝、已提交定制覆盖、备份、卸载与开发模式边界。
- 模板 Node 301/301（含 DB 桩）、新生成 full DB 桩 30/30；失败/取消/跳过均为 0。
  收尾后文档契约单独复跑 6/6。真实 PyYAML 复用现有临时依赖，三套技能结构校验均通过。
- 新 full 8384/minimal-admin 5117，相对一致 T10 输入仅新增 references/plugins.md、改变 SKILL.md。
  原字节 SHA-256 比较，不改 T0 或忽略输出；作者指南/samples/guard 均不复制。
- 两套真实生成 CLI 从工程外 cwd 执行 help/list/add/upgrade/remove；dirty 时 --force 拒绝且源码不变，
  提交定制后升级整包覆盖；备份保留、卸载登记为空、原示例源包不变，pnpm help/list 入口通过。
- 新测试 Node 语法及 AST 18 个函数通过，最大 31 行/2 参数/2 层嵌套、行宽 <=120；diff 检查通过。
  不重跑无关 Maven/UI build，不连接真实数据库或服务；测试不证明 T12 实际登录/授权和 MySQL 验收。

## 18. Review 增量复现（2026-10-07）

- 复用 Node 20.19.0、现有 project/plugin/write 夹具及实际 addPlugin，不模拟安装器。
- R1：合法 ID forge-starter-core，后端为 forge-plugin-demo，独立 SQL 目录按 ID 命名；
  生成宿主安装后应保留该目录。实际变成 core-starter-core，运行描述 ID 未变，断言失败。
- R2：无 Git 宿主首装后新增 application-dev.yml；整包升级应拒绝未登记的本地配置。
  实际未拒绝，当前配置未保留，断言失败；旧目录恢复备份仍存在，不宣称永久数据丢失。
- 既有 Node 301/301 通过，增量 0/2 通过，分别报告；复现不是修复完成。
- 本轮不再运行真实 MySQL/Redis，不修改生产实现；修复后需把边界用例纳入正式回归。

## 19. R1/R2 修复增量计划（2026-10-07）

- 保留 §18 的失败历史，不改旧断言；正式回归覆盖 ID 与核心/自身 artifact 冲突、独立功能编码、
  Java/Maven 坐标分离、SQL/前端路径、未知扩展名二进制，模板/改名、目录/ZIP 均验证。
- 已安装本地配置覆盖无 Git/整个安装目录忽略/单配置忽略/已提交配置；升级与卸载均拒绝，
  原文件/POM/config 不变；事务预检后新增配置也必须拒绝并恢复已写入登记，不删除用户新增文件。
- 源目录/ZIP 仍不交付本地配置；正常构建输出不阻断升级/卸载。
- 复跑既有工具/生成器/门禁/冻结基线/DB 桩，新 full 工程执行 DB 桩并实际使用生成的安装 CLI；
  不改 Java/UI/SQL 业务实现，本轮不重跑 T12 真实数据库及浏览器验收。

### 本轮执行结果

- 正式新增 44 项（identity 15、ownership 29）；最终 345/345，失败/取消/跳过 0。
- Focused + 独立原复现共 46/46；原 R1 目录保持 forge-starter-core，R2 rejected/retained 均 true。
- 两次新 full 的 DB 桩分别 30/30；最终生成工程逐字节核对 6 个运行工具，实际 CLI 1/1 通过。
  配置被拒绝时内容没有出现在 stderr；移出本轮合成配置后带构建输出升级/卸载正常，源包不变。
- 文档契约、Node 语法、120 字符行宽、两阶段增量自审和开源门禁通过；不重跑无差异 Java/UI 构建、
  真实 MySQL/Redis 或浏览器，不改已执行 SQL，不把元数据路径验证扩写为运行时迁移实跑。
