# plugin-foundation 执行记录

## 2026-10-06：提案

### 背景

确定开源版 + 企业版插件的整体模式：

- 开源版和企业版都交付完整源码；
- 不使用制品库；
- 企业版放在阿里云效私有仓库；
- 永久授权 + 年度维保。

本变更只做开源仓库内的插件化底座。

### 调研结论

- 插件源码按工程同规则改名后，可直接被 `scanBasePackages` / `@MapperScan` 扫描，不需要 AutoConfiguration。
- 超级管理员权限是 `*` 和 `/**`，企业版接口必须靠 `@RequiresFeature` 拦截。
- `clean-db.sh` 的 `_history$` 规则会清空插件迁移历史表，需要加保留规则。
- 生成工程没有 `scripts/`、根 `package.json`，插件安装工具需要随工程一起生成。
- `create-project.mjs` 没有自动化测试，抽取改名规则前必须先做输出基线。

### 待澄清确认（HARD-GATE）

- `revision` 升为 `1.1.0`，此后每次开源版发版都递增。
- 插件目录：后端 `<后端根目录>/plugins/`，前端 `src/views/plugins/<插件ID>/`，路由前缀 `/plugins/<插件ID>`。
- 模块名：`forge-starter-plugin`。

### 验证

- 仅文档变更：`git diff --check` 通过，未执行代码验证。

## 2026-10-07：同步代码、可行性复核与 T0

### 同步与范围

- `git fetch origin main`、`git merge --ff-only origin/main` 成功，同步到
  `e416f7902834763ef43989c4525738441e49bd4c`。
- 创建 `codex/plugin-foundation` 分支，保留原有 `.DS_Store` 修改，不混入其他布局变更或主工作树 `.ci-tools/`。
- 用户明确要求 `/apply plugin-foundation`，本轮从已 confirmed 提案执行 T0；T1–T12 未实现、未标记完成。
- 按 `forge-project-init` Skill 使用隔离源快照生成测试工程，并复跑模板与生成 full 的数据库脚本桩测试。

### 可行性复核

总体可行，详细证据与后续约束见 `feasibility.md`。本轮纠正/识别：

1. 仓库已有 26 个 `AutoConfiguration.imports`，原“无自动配置”判断不准确。
2. 根版本之外，框架子 POM、独立 BOM 及根 BOM 固定引用还停留在 `1.0.0`；只改根 POM 不够。
3. 新 starter 需要同步装配目录和依赖闭包；这部分尚未实现，需后续 Task 补齐。
4. T1–T4 会变动生成输入，T5 纯改名回归必须冻结输入；不能忽略生产差异来伪造零差异。
5. T8 配置存在性识别模板与安装后写配置矛盾；需补安装失败回滚、路径/zip 安全、可恢复升级策略。
6. T10 的“任意文件包含 EE 字符串”会匹配 Spec/负例/检查脚本本身，需要先明确有效扫描范围。

### T0 产物

- `forge-server/pom.xml`：`revision=1.1.0`，内部 BOM 版本、框架版本属性引用 `${revision}`。
- `forge-framework/pom.xml`：移除旧 `revision` 覆盖，继承根版本。
- `forge-dependencies/pom.xml`：独立 BOM `revision=1.1.0`。
- 生产 `create-project.mjs` 和 `module-catalog.json` 未修改；Flyway baseline、迁移版本、数据库 runner 版本未修改。
- 基线输入：源提交 `e416f790` 的 Git archive + `baseline/version-bump.patch`，没有复制实时工作区。
- 固定包名/groupId `com.acme.demo`，固定末级目录名和预设；其他选项沿用冻结生成器默认值。
- `baseline/full.json`：8324 个文件；`baseline/minimal-admin.json`：4866 个文件。
- `baseline/provenance.json` 留存源提交/树、版本补丁/生成器/目录清单摘要和两份清单摘要。
- `baseline/manifest.mjs` 保留完整文件集合及原始字节摘要，拒绝覆盖已有基线，提供非零差异退出码。

### 执行命令与结果

环境：Node `v20.19.0`，pnpm `11.19.0`。前置命令：

```bash
source /Users/mini32g/.nvm/nvm.sh && nvm use v20.19.0
```

在 `/private/tmp/forge-plugin-t0.rmLIiB/template` 执行：

```bash
git apply /Users/mini32g/Desktop/project/forge-admin/code-copilot/changes/plugin-foundation/baseline/version-bump.patch
pnpm forge:create -- /private/tmp/forge-plugin-t0.rmLIiB/forge-baseline-full --base-package com.acme.demo --preset full
pnpm forge:create -- /private/tmp/forge-plugin-t0.rmLIiB/forge-baseline-min --base-package com.acme.demo --preset minimal-admin
```

两套生成成功；使用同样参数再次生成到 `repeat/forge-baseline-full` / `repeat/forge-baseline-min`，同样成功。
没有使用 `--force`，没有执行生成器输出的数据库初始化建议。

在仓库根目录执行：

```bash
node --test code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- 初轮 22/22 通过：4 个清单工具测试 + 18 个数据库脚本测试。
- 增加存档摘要/计数/文件范围验证后，单独复跑 `manifest.test.mjs`，5/5 通过。
- 在生成 full 的 `forge-baseline-full-server/scripts/db` 执行
  `node --test init-db.test.mjs clean-db.test.mjs`，18/18 通过。
- 最终覆盖 41 项测试（5 + 18 + 18）；MySQL 和迁移 Maven 调用均由测试桩替代，没有真实库操作。

```bash
node code-copilot/changes/plugin-foundation/baseline/manifest.mjs record \
  /private/tmp/forge-plugin-t0.rmLIiB/forge-baseline-full code-copilot/changes/plugin-foundation/baseline/full.json
node code-copilot/changes/plugin-foundation/baseline/manifest.mjs record \
  /private/tmp/forge-plugin-t0.rmLIiB/forge-baseline-min code-copilot/changes/plugin-foundation/baseline/minimal-admin.json
node code-copilot/changes/plugin-foundation/baseline/manifest.mjs verify \
  /private/tmp/forge-plugin-t0.rmLIiB/repeat/forge-baseline-full code-copilot/changes/plugin-foundation/baseline/full.json
node code-copilot/changes/plugin-foundation/baseline/manifest.mjs verify \
  /private/tmp/forge-plugin-t0.rmLIiB/repeat/forge-baseline-min code-copilot/changes/plugin-foundation/baseline/minimal-admin.json
```

- 两轮逐文件比对：两套工程的 `missing / added / changed` 均为空。
- 生成 full 运行桩测试后再次 verify，仍无差异。
- 修改的 3 个 POM、生成 full 全部 53 个 POM、minimal-admin 全部 34 个 POM 经 `xmllint --noout` 解析通过。
- 用 Node assert 校验模板和两套工程的根/BOM 均为 `1.1.0`、框架无旧 revision 覆盖、根 BOM 引用 `${revision}`，通过。
- `git apply --reverse --check <版本补丁>` 在冻结源中通过，补丁可重现相同版本变更。
- `node --check baseline/manifest.mjs`、`node --check scripts/forge-create/create-project.mjs` 通过。
- `git diff --check` 通过。

### 限制与保留环境

- 在 `forge-server` 尝试 `mvn -pl forge-admin-server -am compile -DskipTests`，退出 127：`command not found: mvn`。
- `/usr/libexec/java_home -V` 无 Java Runtime，标准 Homebrew 工具位置也不存在。
  因此没有完成 Maven 编译、Java 单测、Spring 装配或真实 Flyway 验证；静态版本检查不等于编译通过。
- 没有新增依赖、Java/前端业务代码或 SQL，未执行 UI 构建；本轮仅版本链、基线和文档，不扩展到 T1。
- 没有启动服务，没有操作真实数据库，不需要停止用户已有进程。
- 专用临时目录 `/private/tmp/forge-plugin-t0.rmLIiB/` 保留供后续检查；源码快照和完整生成工程不进入提交。
- T0 按项目规则本地提交到 `codex/plugin-foundation`；本轮用户未要求 push，未推送。

## 2026-10-07：T0 版本更正与商业模式复核

### 确认与范围

- 用户指出 Gitee 已发布 `1.1.2`；核对远端发现 `v1.1.2`、`v1.1.3` 均存在，标签中的 POM 仍声明 `1.0.0`。
  初轮仅依赖旧 POM 判断版本不充分；经用户确认改用新次版本 `1.2.0`，不修改或复用旧发布标签。
- 根 POM、独立 BOM 及冻结输入的版本补丁同步更正；框架继承关系、内部 BOM 的 `${revision}` 引用保持不变。
  本轮没有发布 `1.2.0` 标签，没有实现 T1 及之后的插件、License 或计费功能。
- 用户要求重点分析源码插件与收费方式，已补充到 `feasibility.md`，并区分方案建议与已确认合同/售价。
  使用 Apache 官方许可及 ABP 官方定价、FAQ、许可说明核对源码交付和永久授权/年度更新模式。

### 基线再生成与差异

- 按项目初始化 Skill 在 `/private/tmp/forge-plugin-t0-120.rsKZae/` 隔离复跑，不连接真实数据库。
  输入仍为 `e416f7902834763ef43989c4525738441e49bd4c` 的 Git archive 加更正版本补丁。
- Node `v20.19.0`、pnpm `11.19.0`；固定末级目录、包名和预设，full/minimal-admin 各生成两次，四次均成功。
- 新摘要与初轮清单对比：full 8324 个文件、minimal-admin 4866 个文件，分别仅根 POM 和独立 BOM 两项变化；
  没有新增或缺失文件。确认差异范围后更新清单及 provenance，旧清单可从 `369f345b` 恢复。
- 更新后的仓库清单与临时重新捕获清单逐字节一致；两套 repeat 输出经 `manifest.mjs verify` 检查，
  `missing / added / changed` 均为空。版本补丁的 `git apply --reverse --check` 通过。

### 增量验证

- 清单工具测试 5/5、模板数据库初始化/清理桩测试 18/18、生成 full 的同类桩测试 18/18，共 41 项通过。
- 修改版本链涉及的 3 个源 POM、full 的 53 个 POM、minimal-admin 的 34 个 POM 均经 XML 解析通过。
- 仓库、冻结模板和两套生成工程的根/BOM 版本均为 `1.2.0`；框架继承 revision，内部 BOM 引用 revision。
  初次静态检查误用未改名的框架属性名；按生成器实际前缀规则修正检查后通过，未修改生产生成器或降低断言。
- `git diff --check` 通过。没有启动应用，没有真实 MySQL 操作，没有新增依赖或 SQL。
- Maven/JDK 仍不可用，Java 编译、Spring 装配与真实 Flyway 验证未执行，不把静态/桩测试等同于这些验证。
- 仅本地提交到 `codex/plugin-foundation`，不推送；保留用户原有 `.DS_Store` 修改，不纳入提交。

### 商业复核结论

- 源码插件、私有化部署、永久使用权加可选年度更新支持的方向可行，但尚未证明客户需求、售价和续费率。
- 首期以工程装配而非热加载实现；重点补足核心扩展点、兼容矩阵和客户二次开发后的安全升级边界。
- 建议分离永久使用授权与维保期限，既有授权版本不因维保到期停止运行；实施、定制和外部服务费用单独约定。
- 优先少量能力套件，明确企业内部/服务商交付范围、业务应用交付/框架源码转售区别及技术支持边界。
  Codeup 用于内部开发，客户按权益取得固定源码交付包；不把源码授权校验宣称为防破解保证。
- 上述建议未写入商业合同、报价或生产授权逻辑；商业协议及第三方许可边界仍需专业法律审查。

## 2026-10-07：T1 插件运行时底座

### 确认、边界与产物

- 用户确认按此前分析开始实现，本轮交付 T1；编码前已补充 Spec 实现边界及增量测试矩阵。
- 使用项目 `forge-project-init` Skill 核对新增模块的 BOM、目录清单、依赖闭包和生成工程改名。
  不修改生产生成器，不重录 T0 基线，不执行初始化/清理脚本建议中的真实库操作。
- 新增 `forge-starter-plugin`：14 个生产 Java 类、6 个测试类，核心版本资源和两项自动配置导入。
  默认 Community Gate 可被客户配置/先行自动配置覆盖；API 功能拦截不替代登录与角色/接口权限。
- 插件描述按 ID 确定排序且不可变，校验字段/前缀/目录/兼容范围；拒绝重复 JSON 键、未知字段、
  标量转字符串和超过 64 KiB 的文件。全局懒加载下仍立即校验，非法或不兼容插件使启动失败。
- SemVer 支持预发布优先级及构建元信息，范围采用完整校验后的空格分隔比较式。
- 同步 starter parent、独立 BOM、Admin 依赖及 `module-catalog.json`，确保生成工程不会裁掉依赖。
- T2–T12 保持未完成，未添加迁移 SQL、菜单权限字段、安装器、示例插件或 EE License/计费实现。

### 工具环境

- 系统 PATH 中仍无 Maven、未注册 Java Runtime，但发现并复用了已有临时工具，未安装或修改全局环境：
  - JDK：`/private/tmp/lawhub-october-jdk/Contents/Home`，Temurin `17.0.20.1`；
  - Maven：`/private/tmp/apache-maven-3.9.11/bin/mvn`，版本 `3.9.11`。
- Node `v20.19.0`、pnpm `11.19.0`；Maven 使用 `-o` 复用现有缓存，没有依赖网络安装。
- 独立 BOM 必须先安装 `1.2.0`，然后测试/编译通过 `-am` 使用本轮 Reactor 源码，未用旧 starter jar 代替。

### Java 与聚合编译

下面命令均设置 `JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home`，在仓库根目录执行 BOM 安装：

```bash
/private/tmp/apache-maven-3.9.11/bin/mvn -o \
  -f forge-server/forge-framework/forge-dependencies/pom.xml install -DskipTests -B -ntp
```

在 `forge-server` 执行：

```bash
/private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-framework/forge-starter-parent/forge-starter-plugin -am test \
  -Penable-tests -Dmaven.test.redirectTestOutputToFile=true
/private/tmp/apache-maven-3.9.11/bin/mvn -q -o -pl forge-admin-server -am compile -DskipTests
```

- 最终 106 项用例全部通过，失败/错误/跳过均为 0：版本 38、社区 Gate 11、描述校验 27、Registry 15、
  HTTP 拦截 6、自动配置与容器启动 9。
- 首轮 100 项测试中 1 个 Mockito 夹具错误：对先前设置 `thenThrow` 的 resolver 用 `when` 重新 stub，
  在重新 stub 时触发旧异常。改为独立 resolver 夹具后完整复跑通过；未删除用例或放宽断言。
- 后续补充 JSON 强类型和全局懒加载负例，最终以 106 项重新执行。负例异常日志保存在 Surefire 输出中，
  重定向只避免控制台堆栈噪音，不跳过异常测试。直接解析三套工程 Surefire XML 复核统计一致。
- Admin 及依赖共 47 模块首次聚合编译 `BUILD SUCCESS`，最终源码更新后再次编译退出 0。
- MockMvc 覆盖真实 MVC 自动注册、403 状态/统一响应体、方法优先和自定义 Gate 替换；登录顺序为测试夹具，
  未启动 Sa-Token/Redis 或调用实际登录接口，不宣称已完成真实鉴权端到端验收。
- 非阻塞构建提示：资源过滤的 propertiesEncoding 未显式配置（版本资源只有 ASCII）、Mockito CDS 提示，
  现有模块另有 deprecated/unchecked 提示；没有编译错误。

### 生成工程与增量回归

在仓库根目录执行以下命令，输出目录此前不存在，未使用 `--force`：

```bash
pnpm forge:create -- /private/tmp/forge-plugin-t1.RmXjPh/verified/forge-baseline-full \
  --base-package com.acme.demo --preset full
pnpm forge:create -- /private/tmp/forge-plugin-t1.RmXjPh/verified/forge-baseline-min \
  --base-package com.acme.demo --preset minimal-admin
node --test code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs
node --test forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- full/minimal-admin 均生成成功，两套 `forge.config.json.modules` 都包含 `starter-plugin`。
  新模块、Admin 依赖、BOM 和自动配置类名随 artifact/package 改名，无残留旧 Java 包名。
- 在两套生成工程中分别先安装独立 BOM，再按改名后的 `-pl <framework>/<starter-parent>/<starter-plugin>`
  运行相同 `-am test -Penable-tests` 命令，各 106/106 通过。运行时版本资源均实际读取 `1.2.0`；
  `META-INF/forge/forge-version.properties` 保留固定协议路径，不把该文件名当业务包名改掉。
- 清单工具 5/5、模板数据库脚本 18/18、最终生成 full 的 `scripts/db` 同类测试 18/18，共 41 项通过。
  MySQL 及迁移命令均由桩替代，测试中的重建/清理行为没有作用于任何真实数据库。
- 修改的源 POM 与两套生成工程 POM 经 `xmllint --noout` 解析通过：full 54 个，minimal-admin 35 个。
  新增 Java 无超过 120 字符的行、字段注入或禁用测试；`git diff --check` 通过。
- 当前生成工程包含 T1 新增源码，不与 T0 冻结输出做零差异比较；T0 清单、补丁和 provenance 均未修改。
  T5 的纯改名回归仍须使用冻结输入，不能通过重新生成基线掩盖差异。

### 交付与未验收项

- 仅本地提交到 `codex/plugin-foundation`，未推送，未合并 main；用户原有 `.DS_Store` 修改保留且不提交。
- 没有启动业务服务，没有操作真实数据库，没有改动 UI 或其他项目源码；完整 Flyway/插件链路仍留待 T2–T12。
- 隔离生成工程保留在 `/private/tmp/forge-plugin-t1.RmXjPh/verified/`，不进入提交。
- 实现方式核对了 Spring Boot 官方自动配置文档和 SemVer 官方规则；测试结论以上述本地输出为准：
  - https://docs.spring.io/spring-boot/3.5/reference/features/developing-auto-configuration.html
  - https://semver.org/

## 2026-10-07：T2 插件独立迁移执行器

### 范围与实现

- 用户要求继续实现，本轮交付 T2，编码前增量补充 Spec 和测试矩阵；没有扩展到收费、License 或真实数据库。
- 新增迁移计划、Flyway 配置工厂、迁移策略及自动配置四个生产类；生产类均在 100 行以内，方法不超过 80 行。
  自动配置先于 Boot Flyway 配置登记策略，客户自定义策略可覆盖；开关关闭或 Flyway 缺失时不装配。
- 主迁移先执行，再规划全部已注册插件的 SQL 迁移，校验表名并按 ID 排序；无 SQL 的插件不建历史表。
  数据源及通用配置继承，locations/table/baselineVersion 独立；清空主程序化迁移来源，不污染主配置。
- `baselineOnMigrate`、target、ignoreMigrationPatterns 等保持继承，有限 target 必须存在于各迁移流。
  插件失败中断初始化，不自动 clean/repair/回滚 DDL；依赖初始化器的 Bean 等待插件迁移完成。
- Flyway 复用主项目 `10.20.1`，新依赖为 optional；H2 `2.3.232` 和 Spring JDBC 仅 test scope。
  测试用独立临时 classpath、随机 H2 内存库，关闭时 SHUTDOWN，不读取本地服务/数据库配置。
- 按 `forge-project-init` Skill 验证改名工程和数据库脚本桩测试；生产生成器、T0 基线和 SQL 均未修改。

### Java 与打包证据

使用已有 JDK `/private/tmp/lawhub-october-jdk/Contents/Home`（Temurin 17.0.20.1）和 Maven 3.9.11，
设置 `JAVA_HOME` 后在 `forge-server` 执行；没有安装工具，Maven 均使用离线缓存：

```bash
/private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-framework/forge-starter-parent/forge-starter-plugin -am test \
  -Penable-tests -Dmaven.test.redirectTestOutputToFile=true
/private/tmp/apache-maven-3.9.11/bin/mvn -q -o -pl forge-admin-server -am package -DskipTests
```

- 最终串行单测 154/154 通过（失败/错误/跳过均为 0），其中 T1 原有 106 项、本轮新增 48 项。
  新增分布：历史计划 23、工厂 3、策略 4、H2 SQL 集成 11、Boot 自动配置 7。
- H2 实跑：非空库首装基线为 0，V1.0.0 执行成功；重启不重复执行；两个插件相同版本历史互不冲突；
  未注册目录不执行；JAR/嵌套 SQL 发现成功；主 Java 迁移只执行一次；checksum 改动导致下次启动失败；
  主/插件失败、SQL 后缀及有限 target、baselineOnMigrate=false、主配置不被修改均覆盖。
- Boot 真实 Flyway 自动配置与初始化器测试：本策略生效，自定义策略只调用一次；依赖 Bean 在 SQL 完成后读取；
  插件失败使容器失败。没有启动实际 Admin/Redis/Sa-Token 服务。
- Admin 47 模块聚合 package 退出 0。检查 `target/forge-admin-server.jar` 包含当前 starter 与
  Flyway core/mysql `10.20.1`。嵌入 starter 和模块 jar 的 SHA-256 均为
  `fecaea7b1c96ea703dd5771973a4294547a7bd3051fd3b3802c0822647342b95`，避免仅验证到了旧缓存构件。

### 失败、修正与警告

- 初次编译：`.resolvers()` 存在 String/MigrationResolver 两个可变参数重载，改为明确的空 MigrationResolver 数组。
- 首次 152 项用例中有两项夹具错误：H2 的历史表还有 `TABLE` 建表元记录，不能把整表行数当 SQL 次数；
  有限 target=1.0.1 需要在插件流中实际存在该版本。分别改为明确统计基线/迁移记录和补目标版本夹具，
  另加 target 缺失时拒绝初始化负例；未降低断言或忽略真实迁移错误。
- 一次同时运行同工作区 `test` 与 `package`，共享 `target/classes` 被另一编译重写，出现 14 个 class 读取错误。
  等 package 完成后串行重跑通过，记录到 backend 踩坑索引；不同隔离生成工程才并行构建。
- Flyway 提示 H2 2.3.232 新于其声明支持的 H2 2.2.224，属于未消除的非阻塞兼容警告；已有用例全部通过，
  仍不能替代 MySQL 8 验收。另有 Mockito CDS 提示，未为消除提示升级全局依赖。
- 改名工程的测试验收主历史表前缀随生成器改名；单独表名算法用例用固定 example/acme 字面量独立断言。
  未新增生成器忽略规则，也未重录基线。

### 隔离生成及 Node 回归

Node v20.19.0、pnpm 11.19.0；本轮专用目录为 `/private/tmp/forge-plugin-t2.KqirLH/`。
在仓库根目录执行，输出目录原先不存在，没有使用 `--force` 或执行生成器打印的数据库初始化建议：

```bash
pnpm forge:create -- /private/tmp/forge-plugin-t2.KqirLH/forge-baseline-full \
  --base-package com.acme.demo --preset full
pnpm forge:create -- /private/tmp/forge-plugin-t2.KqirLH/forge-baseline-min \
  --base-package com.acme.demo --preset minimal-admin
node --test code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs
node --test forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- 两套生成成功，分别先 `mvn -q -o -f <改名 framework>/<改名 dependencies>/pom.xml install -DskipTests`，
  再执行改名后的 starter `-am test -Penable-tests`，各 154/154 通过，错误/失败/跳过均为 0。
- 解析三套 Surefire XML 复核计数；两套新自动配置导入引用 `com.acme.demo`，Java 无旧包名残留；
  模块清单保留 starter-plugin，版本资源均读取 1.2.0；真实 H2 历史表使用改名后的主表前缀。
- 模板数据库桩测试 18/18（约 19.4 秒）、生成 full 的 `scripts/db` 同类测试 18/18（约 13.8 秒）、
  清单工具 5/5，共 41 项通过，所有 MySQL/Maven 迁移执行均为测试桩，没有真实重建/清理。
- 修改的源 POM 和 full 54 个/minimal-admin 35 个 POM XML 解析通过；Java 行宽检查及 `git diff --check` 通过。
  包内模块摘要与当前 jar 一致；T0 清单/补丁/provenance 无差异。

### 状态与限制

- T2 完成，T3–T12 继续待办。T4 历史保护尚未实现，当前不要在已经装了插件的库使用旧清理脚本。
- 未执行真实 MySQL 或前端/UI 构建（本轮无前端变更），未操作生产数据，没有启动业务服务或改其他项目。
  H2 内存库均随夹具关闭，无需停止用户原有进程；专用临时生成工程保留，不纳入提交。
- 本轮仅本地提交至 `codex/plugin-foundation`，不 push、不合并 main；原有 `.DS_Store` 修改保留且排除。
- 迁移初始化扩展与 API 语义核对了本地 Boot/Flyway 字节码及官方文档：
  - https://docs.spring.io/spring-boot/how-to/data-initialization.html
  - https://documentation.red-gate.com/flyway/reference/usage/api-java
  - https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-target-setting

## 2026-10-07：T3 功能授权过滤

### 范围与实现

- 用户要求继续，先复用 T2 的 Spec/Task/测试记录，补充 T3 权限收窄边界后编码；仅实现 F3。
- 新增 V1.0.209，以当前库 information_schema 判断列是否存在，可空 VARCHAR(64)，不改存量资源编码。
  实体加入 featureCode，System 引入 starter-plugin，同步 forge:create 模块依赖闭包，不改生成器或 T0 基线。
- 当前菜单/资源树在 getUserResources 的管理员和普通用户结果均按 Gate 过滤；普通登录的按钮/API 权限
  在保存快照前过滤，API patterns 查询只接收保留的资源 ID。角色/租户/客户端/用户类型范围和排序不变。
- 后台资源维护和管理员授权配置保留完整目录，超管通配保持；接口独立的 RequiresFeature 继续负责实时功能拒绝。
  不过滤全局 configured API 目录，避免未开通 API 被误判为不需 RBAC；不新增角色授权或关闭安全机制。
- 新增测试覆盖默认/自定义 Gate、两服务共用 Bean、公开 loadUserByUserId 路径、隐藏页面保留、全禁用空集、
  异常拒绝和租户上下文恢复；Mapper 全为桩。迁移只做防重复/版本/字段映射静态契约，不模拟 MySQL DDL 成功。

### 模板编译、测试与打包

使用同一临时 JDK/Maven，离线缓存；在 forge-server 中设置
`JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home` 后执行：

```bash
/private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-framework/forge-starter-parent/forge-starter-plugin,forge-framework/forge-plugin-parent/forge-plugin-system \
  -am test -Penable-tests -Dmaven.test.redirectTestOutputToFile=true
/private/tmp/apache-maven-3.9.11/bin/mvn -q -o -pl forge-admin-server -am package -DskipTests
```

- 最终完整测试退出 0。解析 Surefire XML：starter-plugin 154 项，System 160 项，均失败/错误/跳过为 0；
  相关依赖模块由 -am 同时复跑通过。新增 32 项：菜单/资源 14、权限快照 12、迁移/实体 3、装配/公开加载 3。
- 首轮 System 157 项中新增 29 项已通过，1 个旧安全用例读
  `forge-admin-server/sql/初始化脚本.sql` 报 NoSuchFileException；该副本在 d2c4f0ee 已移除。
  对齐现用权威 SQL `db/全量初始化SQL.sql`，并断言 init-db.sh 声明与调用该路径，保留敏感接口日志保护断言。
  加入装配用例后完整重跑通过，未跳过测试、放宽安全断言或恢复废弃 SQL 副本。
- Admin 聚合 package 退出 0；包内 forge-plugin-system-1.2.0.jar 的 SHA-256 与本轮模块 jar 一致：
  `eda95f3da8777d173c4bfd8e45e4fc17e92d1629eb740385bfdd48766f6dcb53`。
  包内 starter-plugin 摘要与当前模块一致：
  `fecaea7b1c96ea703dd5771973a4294547a7bd3051fd3b3802c0822647342b95`，没有误用旧缓存构件。
- Maven 同一 checkout 的 test/package 串行执行。Console 有 Mockito CDS 提示；既有 T2 H2 兼容告警仍未消除，
  不影响本轮结果，也不能代替真实 MySQL/Redis 验收。

### 改名生成与回归

按 forge-project-init Skill 使用已确认的基线参数进行隔离工程验证；专用 mktemp 目录
`/private/tmp/forge-plugin-t3.RZaqzp/`，没有使用 --force，未执行生成器输出的真实库初始化建议。
Node v20.19.0、pnpm 11.19.0，在仓库根目录执行：

```bash
pnpm forge:create -- /private/tmp/forge-plugin-t3.RZaqzp/forge-baseline-full \
  --base-package com.acme.demo --preset full
pnpm forge:create -- /private/tmp/forge-plugin-t3.RZaqzp/forge-baseline-min \
  --base-package com.acme.demo --preset minimal-admin
node --test code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- 两套生成后分别在改名 server 中先离线 install 独立 BOM，再 -am test -Penable-tests 改名
  starter-plugin 和 plugin-system；两套都退出 0，XML 计数各 154 + 160 = 314，失败/错误/跳过均为 0。
- full 的 54 个 POM、minimal-admin 的 35 个 POM XML 均解析通过；System 引用改名 starter-plugin，
  两个服务 import 为 com.acme.demo，V1.0.209 随工程保留；版本仍为 1.2.0。
- 基线工具 5 项 + 模板 DB 脚本 18 项共 23/23（约 25.1 秒）；生成 full 的同类 DB 桩测试
  18/18（约 30.5 秒），总共 41 项通过。所有 MySQL/Maven 迁移执行均为测试桩，没有真实重建或清理。
- 源 POM XML、模块清单 JSON、新增 Java 行宽、git diff --check 通过；类/方法不超规模上限。
  新迁移无业务占位符；全目录扫描命中原 V1.0.72 的 4 行，不改已执行脚本，主配置原本关闭占位符替换。

### 状态与限制

- T3 完成，T4–T12 待继续。V1.0.209 需由部署时主 Flyway 执行；未运行真实 MySQL 迁移或
  Sa-Token/Redis 登录、会话刷新与企业插件端到端。权限快照变更需重新登录/刷新会话，接口门禁仍实时判断。
- 本轮无前端变更，不执行 UI 构建；测试 Spring 容器和 H2 夹具自行关闭，没有启动业务服务或改其他项目。
  隔离生成工程保留，不纳入提交；T0 冻结清单不变，用户原有 .DS_Store 修改保留且不提交。
- 本轮本地提交到 codex/plugin-foundation，不 push、不合并 main。T4 清理历史保护未接入，
  当前仍不要对安装了插件的数据库运行旧清理脚本。

## 2026-10-07 10:55 CST：T4 清理脚本保护迁移历史

### 范围与实现

- 从 codex/plugin-foundation 的 ed197302 继续，只修改 clean-db.sh、对应测试和阶段文档，
  用户原有 .DS_Store 修改保留、不提交。未改 Java、UI、迁移 SQL 或 T0 冻结基线。
- 按 forge-project-init Skill 检查既有分类/显式删表/行级清理，并在模板及改名 full 工程复跑桩测试。
  主历史表精确匹配，插件历史按 _plugin_[a-z0-9_]+_history$ 优先保护，兼容任意工程前缀。
- DROP/TRUNCATE 及 tenant_id/del_flag/deleted 通用清理都绕开迁移历史；--drop-table 指向历史表时
  在调用 MySQL 前失败，keep/drop 冲突也不能掩盖非法参数。普通表清理保持原行为。
- 自定义 --extra-sql 仍原样追加，必须人工审核；不声称 SQL 解析防护，也不扩展到 --recreate。

### 模板验证

环境：Node v20.19.0、pnpm 11.19.0、系统 /bin/bash 3.2.57。仓库根目录执行：

```bash
/bin/bash -n forge-server/scripts/db/clean-db.sh
node --check forge-server/scripts/db/clean-db.test.mjs
node --test code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
git diff --check
```

- 最终回归退出 0：35/35，约 27.0 秒；clean-db 18（新增 12）、init-db 12、基线工具 5。
  失败/错误/取消/跳过均为 0。初轮与补充主历史表大小写用例后的回归也通过，未删测试或放宽断言。
- 7 种表名的显式删除各验证有/无 --keep-table 两种情况，调用计数均为 0；默认预览无写入，
  --execute --yes 将计划交给 MySQL 函数桩，计划中没有任何主库/插件迁移历史表引用。
- 模拟历史表含 tenant_id/del_flag/deleted 仍无 DELETE；普通密码历史仍 TRUNCATE，
  备份/临时副本仍 DROP，普通表的显式 drop 与 --keep-business-tables 仍生效。
- bash 与 Node 语法检查退出 0；新增代码行宽 <=120，辅助函数均小于 80 行；git diff --check 通过。

### 改名工程验证

用 mktemp 新建专用目录，无 --force，不覆盖已有工程；最终脚本版本的工程位于
`/private/tmp/forge-plugin-t4.lSjaH7/forge-baseline-full`。使用 T0 已确认的生成参数：

```bash
pnpm forge:create -- /private/tmp/forge-plugin-t4.lSjaH7/forge-baseline-full \
  --base-package com.acme.demo --preset full
```

生成 full 工程根目录执行：

```bash
/bin/bash -n forge-baseline-full-server/scripts/db/clean-db.sh
node --check forge-baseline-full-server/scripts/db/clean-db.test.mjs
node --test forge-baseline-full-server/scripts/db/init-db.test.mjs \
  forge-baseline-full-server/scripts/db/clean-db.test.mjs
```

- 生成成功，最终同类 DB 桩回归 30/30，约 25.3 秒，失败/错误/取消/跳过均为 0。
  前一轮隔离工程 /private/tmp/forge-plugin-t4.CvFEKp 中的同类回归也为 30/30。
- 额外使用 Node 严格比较：生成 clean-db.sh 与当前模板按数据库名、主历史表名及 Admin 模块名
  替换后的完整内容一致；主历史表 forge_baseline_full_schema_history 与生成全量 SQL 的建表名称一致。
- 原前缀、改名前缀、Acme 大小写、数字/下划线插件 ID、tmp_ 工程前缀的插件历史均不被自动清理。
  git diff --name-only -- code-copilot/changes/plugin-foundation/baseline 输出为空，未重新录制基线。

### 状态与限制

- T4 完成，T5–T12 待继续；下一步抽取共享改名规则，在冻结输入上验证与 T0 输出逐字节一致。
- 本轮无 Java/UI/Flyway SQL 改动，复用 T3 聚合构建证据，不重复跑 Maven/UI 构建；本轮 Shell/Node
  语法和行为检查均已执行。真实 MySQL 清理后重启、checksum、业务插件完整链路仍待 T12 人工验收。
- 没有连接真实 MySQL、没有重建或清理真实库、没有启动业务服务，无需停止业务 PID。
  专用隔离工程保留但不纳入提交；生成器输出的真实初始化命令未执行，自定义 SQL 不在自动保护范围。
- 按仓库规则本地单独提交 T4，不 push、不合并 main，.DS_Store 原有修改不纳入提交。

## 2026-10-07 12:02 CST：T5 抽取共享改名规则，冻结输出零差异

### 范围与实现

- 从 codex/plugin-foundation 的 73f49cf5 继续，按 forge-project-init Skill 维护工程生成器。
  抽出 scripts/forge-shared/rename.mjs、files.mjs，生成器复用，不改变其编排阶段。
- 映射/顺序化文本规则、POM、Java 包/类文件/模块目录改名共用；新增 renameSourceTree 隔离源码入口。
  Maven groupId 先改，Docker/H5 具体前缀先改；二进制排除、SSO 行清理及已有目标规则保持一致。
- 仅新增同包名 Java 目录 no-op，避免合并后删除自身；生成器 1498 -> 1084 行。
  共享规则 419 行、文件工具 52 行、测试 278 行；插件包校验/安装/回滚仍由 T8 承担。
- 未实现 T6 配置/工具复制，不改 Java/UI/SQL/目录清单/依赖；原有 .DS_Store 修改保留且不提交。

### 规则与模板回归

环境：Node v20.19.0、pnpm 11.19.0、bash 3.2.57。仓库根目录执行：

```bash
source /Users/mini32g/.nvm/nvm.sh && nvm use v20.19.0
node --check scripts/forge-create/create-project.mjs
node --check scripts/forge-shared/rename.mjs
node --check scripts/forge-shared/files.mjs
node --check scripts/forge-shared/rename.test.mjs
node --test scripts/forge-shared/rename.test.mjs \
  code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
git diff --check
```

- 最终 60/60，约 24.24 秒，失败/取消/跳过均为 0：共享规则 25 + 基线工具 5 + DB 桩 30。
  初轮 24/25 的失败是报表条件夹具期望遗漏旧模块别名替换；核对原规则后修正精确期望，再完整复跑，
  没有改变生产规则迁就测试。模板 DB 测试没有连接真实 MySQL。
- 另用 node --input-type=module 从 git show 73f49cf5 提取原纯函数，VM 中执行并与当前 exports 比较：
  无/仅 admin/仅 report/仅 H5/全部前端 × 默认/自定义参数共 10 组。模块/启动类映射及完整有序规则
  JSON 均相等；顺序未归一化，Maven groupId 与 Java basePackage 的自定义值不同。
- Node 语法、git diff --check、新文件行宽 <=120 和辅助方法 <=80 行通过。初次方法行数扫描误把
  fixture 后面的全部 test callbacks 算进 fixture，改用函数的顶层闭合行复查后无超限；非源码缺陷。

### 冻结输出验证

专用 mktemp 目录 `/private/tmp/forge-plugin-t5.HLERZE`；按 baseline/README.md 从
e416f7902834763ef43989c4525738441e49bd4c 解出 frozen-template，应用已有 version-bump.patch。
只带入当前 create-project.mjs 与 forge-shared 依赖，不复制实时业务源码、module-catalog 或文档。
在冻结模板中执行以下两条生成命令，无 --force；生成后在当前仓库执行 verify：

```bash
pnpm forge:create -- /private/tmp/forge-plugin-t5.HLERZE/frozen-output/forge-baseline-full \
  --base-package com.acme.demo --preset full
pnpm forge:create -- /private/tmp/forge-plugin-t5.HLERZE/frozen-output/forge-baseline-min \
  --base-package com.acme.demo --preset minimal-admin
node code-copilot/changes/plugin-foundation/baseline/manifest.mjs verify \
  /private/tmp/forge-plugin-t5.HLERZE/frozen-output/forge-baseline-full \
  code-copilot/changes/plugin-foundation/baseline/full.json
node code-copilot/changes/plugin-foundation/baseline/manifest.mjs verify \
  /private/tmp/forge-plugin-t5.HLERZE/frozen-output/forge-baseline-min \
  code-copilot/changes/plugin-foundation/baseline/minimal-admin.json
```

- full fileCount=8324、minimal-admin fileCount=4866，两次 verify 均退出 0，
  missing=[]、added=[]、changed=[]。不忽略/归一化文本、点文件、空文件或二进制。
- git diff --name-only -- code-copilot/changes/plugin-foundation/baseline 输出为空，未重新录制/覆盖基线。

### 实时工程集成与已发现旧问题

当前仓库另用同样参数生成 live-output 下 full/minimal-admin 两套工程；在实时 full 根执行：

```bash
node --test forge-baseline-full-server/scripts/db/init-db.test.mjs \
  forge-baseline-full-server/scripts/db/clean-db.test.mjs
```

- 30/30，约 25.61 秒，失败/取消/跳过均为 0；包含改名前缀的主库和插件迁移历史保护。
  模板及实时 full 总计 90 项 Node 回归通过，全部数据库调用为桩。
- 用 xmllint --noout 检查 full 54/minimal-admin 35 个 POM；Node 严格检查 1.2.0 版本、
  System 的 starter-plugin 依赖、com.acme.demo 自动配置、V1.0.209 和历史保护均通过。
- JDK=/private/tmp/lawhub-october-jdk/Contents/Home，Maven=/private/tmp/apache-maven-3.9.11/bin/mvn。
  在每套实时 server 根先执行离线 BOM install，再执行 Admin 聚合 compile：

```bash
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-full-framework/forge-baseline-full-dependencies install -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-full-admin-server -am compile -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-min-framework/forge-baseline-min-dependencies install -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-min-admin-server -am compile -DskipTests
```

- 两次 BOM install 退出 0，Admin compile 均退出 1：full 最先在 plugin-data 的 DatasetPrintDataProvider
  报 plugin.print 类型不存在；minimal-admin 最先在 plugin-generator 的 PrintApplicationAccessAdapter 等类
  报同类错误。原目录清单没有 plugin-print，裁剪会删掉源 POM 已存在的打印依赖，冻结工程同样如此。
- 另在独立 mktemp 输出目录直接运行 javac -proc:none 对 full 生成 Admin 源文件诊断：退出 1，
  `class ForgeBaselineFullApplication is public, should be declared in a file named ForgeBaselineFullApplication.java`。
  原 ForgeAdmin 品牌替换先命中，完整启动类规则无法命中，但文件名仍被独立改为 AdminApplication。
  没有为该调用提供外部类路径，它只确认命名诊断，不是聚合构建；两套 Maven 在更早的打印依赖处失败。
- 两个问题均在冻结输出中存在，不是 T5 抽取新增。保留失败证据与踩坑，不改目录清单或替换顺序来掩盖，
  建议下一阶段先单独确认修复及允许差异；不以“零差异”或局部模块测试代替生成 Admin 构建成功。

### 状态与限制

- T5 的共享规则抽取和零输出差异门禁完成；T6–T12 尚未完成，生成工程上述编译问题未修复。
  已询问用户是否先单独修复再继续工具开发；未收到选择时不突破 T5 的零差异约束。
- 本轮无 Java 新源码/UI/Flyway 改动，不重跑此前相关 Java 单测或 UI 构建，不启动服务或连接真实数据库。
  没有业务服务 PID；隔离验证工程留存且不提交，不执行生成器打印的真实初始化/清理命令。
- 按仓库规则本地单独提交 T5 的脚本与文档，不 push、不合并 main，原有 .DS_Store 修改保留。

## 2026-10-07：T5-F1 生成工程编译修复

### 授权与范围

- 用户在 T5 的两个旧编译问题与修复建议后回复“继续”，本轮先独立修复生成工程，不提前实现 T6。
- 使用 forge-project-init 技能，增量复用既有 Spec/test-spec、隔离生成及模板/生成工程 DB 桩验证。
  保留 codex/plugin-foundation，不推送或合并 main；原有 .DS_Store 修改不纳入提交。
- module-catalog 登记既有 plugin-print，补齐 generator 的 print/data/external 和 data 的 print 依赖；
  共享品牌规则避开完整 ForgeAdminApplication，其它顺序与文本语义不变。
- 首次实时 full 的 BOM install/package 均成功；minimal-admin 的 BOM install 成功、package 失败，
  最先报 Admin 应用集成引用未选中的 capability.controlplane/flowaction/secureaction 类型不存在。
  在 Spec 中追加明确裁剪边界后，新增 source-glue.mjs，仅在缺依赖时裁剪新生成工程的可选接入层。
  不删除模板 Java 文件，不把能力开放套件强塞入最小预设，不修改生产 POM/SQL/UI。

### 最终生成与打包

- 专用目录 /private/tmp/forge-plugin-t5-fix.0Phyoo；最终两套实时工程位于 final-live/，
  两套冻结工程位于 final-frozen/。首次验证工程保留在 live-output/frozen-output，无 --force 或覆盖。
- 冻结模板仍为 e416f7902834763ef43989c4525738441e49bd4c 的 Git archive + 原版本补丁。
  带入最终 create-project.mjs、source-glue.mjs、forge-shared/，目录清单只应用本节 print/依赖增量。
  不复制实时 forge-server/AGENTS/code-copilot，不混入 T1–T4（冻结目录没有 starter-plugin）。
- 生成参数：末级目录 forge-baseline-full/forge-baseline-min，--base-package com.acme.demo，
  --preset full/minimal-admin，其它沿用 T0 默认；在两类源根执行同一生成命令，例如：

```bash
node scripts/forge-create/create-project.mjs \
  /private/tmp/forge-plugin-t5-fix.0Phyoo/final-live/forge-baseline-full \
  --base-package com.acme.demo --preset full
node scripts/forge-create/create-project.mjs \
  /private/tmp/forge-plugin-t5-fix.0Phyoo/final-live/forge-baseline-min \
  --base-package com.acme.demo --preset minimal-admin
```

- Node v20.19.0；JDK=/private/tmp/lawhub-october-jdk/Contents/Home；
  Maven=/private/tmp/apache-maven-3.9.11/bin/mvn。对每套 final-live 的 server 根执行：

```bash
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-full-framework/forge-baseline-full-dependencies install -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-full-admin-server -am package -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-min-framework/forge-baseline-min-dependencies install -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-min-admin-server -am package -DskipTests
```

- 四个最终 Maven 命令全部退出 0。两套工程路径/坐标独立，没有同 checkout 的并发构建。
  两个包内 Start-Class 均为 com.acme.demo.admin.<JavaName>AdminApplication，与源码/文件名/main 一致。
- 包内打印 jar 与本轮 Reactor target jar 摘要一致：
  full=c3733a092585de10b0854fbdf6789ebe3f3b5619a4dd735b7466c96168f2b137；
  minimal-admin=e75e92084987c55b14e32126aab875b52db9afdec9200d2e99a52263c3d0ffb0。
  未用本机旧打印 jar 冒充改名集成成功。
- 所有 POM 用 xmllint --noout 验证：full 54、minimal-admin 37，通过。
  full 的 integration 主/测试/Mapper 共 11 文件保留，minimal-admin 为 0；AI 降级适配器仍含 Flux.empty()。
  forge.config.json 仍没有 T6 的 forgeVersion/plugins 字段。

### 增量测试

```bash
node --test scripts/forge-create/module-catalog.test.mjs scripts/forge-create/source-glue.test.mjs \
  scripts/forge-shared/rename.test.mjs code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- 新增 16 项：目录清单 9、可选接入层 5、品牌边界/真实启动类 2；模板共 76/76，约 83.93 秒。
  生成 final-live/full 根复跑 server/scripts/db/init-db.test.mjs、clean-db.test.mjs，30/30，约 84.17 秒。
  总计 106 项，失败/取消/跳过均为 0，数据库执行及迁移调用都是桩，不接真实库。
- Node --check 生成器/source-glue/rename、git diff --check 通过。新增测试/工具行宽 <=120、辅助方法 <=80；
  source-glue 22 行，其测试 73 行，目录测试 79 行，共享规则测试 300 行。

### 冻结输出完整差异审计（不是重新录制基线）

- 用原始 collectManifest/compareManifests 对 T0 清单读取比较，无路径忽略或内容归一化：
  full 8324 文件，missing=112/added=112/changed=10；minimal-admin 5058 文件，missing=125/added=317/changed=9。
- full 的 112 个 missing/added 一一对应打印模块目录改名，原来是未登记/未映射的 forge-plugin-print。
  对每个文件验证唯一坐标替换为 forge-baseline-full-plugin-print 后的原始内容与新文件逐字节一致。
  minimal-admin 的同类 112 文件也做完全相同的对应验证，不整目录放行。
- minimal-admin 另外新增 plugin-data 120 文件、plugin-external 74 文件，全部与最终冻结 full 的同源文件
  按 full -> min 项目/Java 前缀替换后的字节一致；未从实时工作区混入新业务代码。
- minimal-admin 新增 SQL 文件为 9 个 data/external 来源加 2 个消息 SQL 重排文件，旧的 2 个消息序号文件删除。
  两个 manifest 中所有 54/41 个脚本按冻结 source 路径和完整有序改名规则逐字节核对。
  min manifest 保留旧来源列表/相对次序，仅插入 9 个来源并顺延消息序号，没有业务 SQL 修改。
- 首次 SQL 来源审计期望只替换 forge_admin，遗漏了原规则先替换 forge_admin_new；报表 SQL 审计失败。
  改为显式使用完整原改名规则验证后全部通过，未修改生成器/SQL 迁就审计。
- minimal-admin 另外删除精确 11 个可选集成文件：6 个主 Java（Controller/Service/Mapper/3 DTO）、
  4 个测试（Controller/Service/Source/MessageIntegration）、1 个 ApplicationIntegrationMapper.xml。
  没有删除其它源码、测试、XML 或资源；完整预设保留全部文件。
- 两套所有 19 个 changed 文件完整 git diff --no-index 已人工审核：
  - full：Admin POM/启动类、business-core POM、BOM、data POM、generator POM/PrintCodegenContributor.java/
    PRINTING.md.vm、plugin-parent POM、forge.config.json，共 10 文件。
  - min：db/manifest.json、Admin POM/启动类、BOM、generator POM/上述两个打印坐标提示文件、
    plugin-parent POM、forge.config.json，共 9 文件。
  - 内容只为启动类纠正、打印坐标/模块与依赖登记、min data/external 依赖和相应 SQL/config 选择。
    min Admin 同时恢复源 POM 已有的 spring-boot-starter-test：旧裁剪正则把它随相邻 external 依赖一起删掉，
    保留 external 后自然恢复；本轮不修改裁剪正则，不引入源 POM 之外的新外部依赖。
- git diff --name-only -- code-copilot/changes/plugin-foundation/baseline 输出为空；
  原 full/minimal-admin 清单、provenance、version-bump.patch 均未更新，T5 零差异历史证据保留。

### 状态与限制

- T5-F1 完成；T6–T12 仍待完成。本轮只修生成器兼容性，没有新增插件安装/升级/卸载命令或 License。
- 没有 Java 业务源码/UI/生产 SQL 变更，不重跑既有全部 Java 单测，不启动服务或连接真实 MySQL。
  package 和 DB 桩不能代替真实迁移、应用启动、Sa-Token/Redis 或端到端插件验收；没有业务服务 PID。
- 最终验证目录保留，不提交生成工程或构件。按仓库规则本地单独提交脚本/文档，不 push、不合并 main。

## 2026-10-07 T6：生成工程携带插件工具链与版本配置

### 实现范围与规则

- 只实现 T6；插件 CLI 当前是明确的阶段入口，支持无参/--help/-h，安装相关命令非零退出且不写文件。
  模板根 package 的命令注册仍属 T8；模板实际忽略区块属 T10，本轮不提前实现安装或防误提交门禁。
- 根 POM 唯一明文 revision 作为 forgeVersion；注释不参与读取，缺失/重复/变量/非法 SemVer 在目标创建前失败。
  配置只增加 forgeVersion 和 plugins=[]，既有键值/排列/选择不变；配置写入提取到 project-tools。
- 生成工程根 package 仅包含 forge:plugin 脚本，没有新增 npm 依赖、模板 forge:create 或维护命令。
  runtime 工具目录递归复制，排除测试/夹具/.git/.DS_Store/node_modules/.env*/本地文件，拒绝软链接和特殊文件。
- 运行入口、共享改名/文件模块和原始 catalog 在业务源码/SQL/Docker 改写完成后复制，不参与工程改名。
  保留 canonical Forge 匹配表与路径，避免后续插件二次改名使用已被替换的规则。
- .gitignore 只删除单个完整 forge-template-only:begin/end 区块；无区块原样，保留其它换行/末尾字节。
  嵌套/重复/未闭合/反向标记失败，不静默删除其它规则。真实模板还没有区块，用单测及冻结夹具证明行为。

### 隔离目录与冻结来源

- 验证根目录：/private/tmp/forge-plugin-t6.BJerbU；所有工程与构件仅在临时目录保留，未覆盖用户工程。
- frozen-template 从原 T0 的 e416f7902834763ef43989c4525738441e49bd4c 导出，应用原 version-bump.patch；
  带入 a8adba85 的 T5-F1 生成器、source-glue、共享依赖及精确目录清单增量，不带入 T1–T4 的业务/模块变更。
- 先生成 f1-output，两套全部文件与上一阶段 /private/tmp/forge-plugin-t5-fix.0Phyoo/final-frozen 对比，
  missing/added/changed 全部为 []；full 8324、minimal-admin 5058 文件，验证旧允许差异的来源没有变化。
- 随后只带入 T6 生成器/project-tools/插件入口；给冻结夹具 .gitignore 增加精确模板区块，
  先生成 frozen-output，再配置写入抽取后生成 final-frozen。两轮完整 manifest 差异为 0，未改变 README 字节。
- 实时模板首次输出在 live-output，最终代码重新生成到 final-live。最终生成命令：

```bash
node scripts/forge-create/create-project.mjs \
  /private/tmp/forge-plugin-t6.BJerbU/final-live/forge-baseline-full \
  --base-package com.acme.demo --preset full
node scripts/forge-create/create-project.mjs \
  /private/tmp/forge-plugin-t6.BJerbU/final-live/forge-baseline-min \
  --base-package com.acme.demo --preset minimal-admin
```

### 增量测试与生成工具检查

```bash
node --test scripts/forge-create/module-catalog.test.mjs scripts/forge-create/source-glue.test.mjs \
  scripts/forge-create/project-tools.test.mjs scripts/forge-shared/rename.test.mjs \
  scripts/forge-plugin/index.test.mjs code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- Node v20.19.0；新 project-tools 25 项、插件入口 8 项，共新增 33 项；最终模板 109/109，约 53.85 秒。
  在 final-live/full 根复跑 server/scripts/db/init-db.test.mjs、clean-db.test.mjs，30/30，约 54.33 秒。
  合计 139 项，失败/取消/跳过均为 0；所有数据库/迁移调用为桩，不连接真实 MySQL。
- 实时两套运行工具/catalog 与模板源文件逐字节相同；实际动态导入生成 rename 模块，
  ForgeAdminApplication/com.mdframe.forge/forge-plugin-print 可二次改名到对应项目类/包/模块，均通过。
- 两套绝对 Node 入口从 /private/tmp cwd 执行帮助返回 0；add /missing --force 返回 1，配置字节不变。
  前一轮 live-output 中 pnpm 11.19.0 执行 forge:plugin --help 均退出 0，正确定位运行入口。
  pnpm 自身产生无依赖的 node_modules/pnpm-lock.yaml，这些只是临时运行产物，不是生成器输出。
  冻结工程未运行 pnpm，完整清单未混入上述产物，也未通过忽略规则隐藏差异。
- Node --check 生成器/project-tools/插件入口及 git diff --check 通过；新文件分别 128/181/32/38 行，
  新方法 <=80 行、行宽 <=120、参数 <=5。配置写入提取避免旧方法继续超限，README 保持原样。
  行宽审查发现一条测试标题超限，仅缩短描述，未改断言或生产逻辑；最终审查通过。

### 最终生成工程构建

- JDK=/private/tmp/lawhub-october-jdk/Contents/Home；Maven=/private/tmp/apache-maven-3.9.11/bin/mvn，3.9.11。
  在每套 final-live 的对应 server 根执行其 BOM install/Admin package，全部离线，跳过 Java 单测：

```bash
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-full-framework/forge-baseline-full-dependencies install -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-full-admin-server -am package -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-min-framework/forge-baseline-min-dependencies install -DskipTests
JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home \
  /private/tmp/apache-maven-3.9.11/bin/mvn -q -o \
  -pl forge-baseline-min-admin-server -am package -DskipTests
```

- 四个最终命令全部退出 0，工程路径/坐标独立。所有 POM xmllint --noout 通过：full 54、minimal-admin 37。
- 包内 Start-Class 正确：com.acme.demo.admin.ForgeBaselineFullAdminApplication / ForgeBaselineMinAdminApplication。
  包内打印 jar 与本次 Reactor jar SHA-256 相同：
  full=4400916c8a20cbd46773fb70bad21adea50b356d1d178b034715d22a13ef6868；
  minimal-admin=a6d6b77c04dfefd93c8edd5c9a8e7858bbc8a33f2e0376e3e6ea63a856b573a0。

### 冻结输出完整差异审计

- 用原 collectManifest/compareManifests 对每个文件路径/SHA-256 比较，无路径忽略或内容归一化。
  相对 f1-output：final-frozen/full 8329、minimal-admin 5063 文件，两套 missing=0/added=5/changed=1。
- 两套 added 精确为 package.json、scripts/forge-plugin/index.mjs、scripts/forge-shared/files.mjs、
  scripts/forge-shared/rename.mjs、scripts/forge-create/module-catalog.json；四个运行文件/catalog 与模板原字节一致。
  根 package 字段与仅有的 forge:plugin 脚本逐项核对，没有模板 forge:create、测试文件或额外依赖。
- 唯一 changed 为 forge.config.json。验证 forgeVersion=1.2.0/plugins=[]，删掉仅这两个新字段后
  以原 JSON 缩进/结尾序列化，恢复原 T5-F1 的全部配置字节。其它业务源码/POM/SQL/图片/点文件不变。
- 冻结夹具的模板忽略区块剥离后，两套 .gitignore 与 T5-F1 原始字节相同；没有其它规则删除或替换。
- 相对原 T0：full missing=112/added=117/changed=10；minimal-admin missing=125/added=322/changed=9。
  旧例外全部沿用上一节逐文件审计结果，新增只多上述 5 文件；config 已在旧 changed 中仅加两字段。
  git diff --name-only -- code-copilot/changes/plugin-foundation/baseline 为空，没有改写原清单/provenance/版本补丁。

### 状态与限制

- T6 完成，T7–T12 待继续；下一阶段增加 POM 插件接入标记，再实现安装器。CLI 帮助不能替代安装全链路。
- 无生产 Java/UI/SQL 改动，不执行全部 Java 单测、真实 MySQL/Redis、业务启动或迁移/权限端到端测试。
  本轮 Maven package 与数据库桩验证不代表真实数据库或安装验收；没有本轮业务服务 PID。
- 按工程初始化 Skill 重生成两预设并复跑模板/生成 full DB 桩；只提交本轮脚本/阶段文档，
  保留既有 .DS_Store 改动，不提交临时工程/构件，不 push、不合并 main。

## 2026-10-07 T7：POM 插件接入标记

### 范围与实现

- 用户确认继续下一阶段。本轮只交付 T7，沿用 T6 的帮助入口，不实现 T8 安装/升级/卸载或 License。
- 后端根 POM modules 末尾新增 forge-plugins:modules:begin/end，Admin dependencies 末尾新增
  forge-plugins:dependencies:begin/end。区块为空，现有模块/依赖/版本及顺序不变。
- 原 replacePomModules 整块重写会丢失注释；提取为 pom-modules.mjs，保留合法空标记。
  无标记旧模板输出不变；重复/缺失/反向/错位/非空区块拒绝，单文件校验失败不写入该文件。
  生成器不新增全工程回滚；失败可能留下部分临时输出，不能把单文件保护表述为安装器事务。
- T6 的 CLI 版本错误夹具同步带入新的模块渲染依赖，保留原“不创建目标”和版本错误断言。
  新测试覆盖原渲染兼容、标记拒绝/幂等、源 POM 契约及真实共享改名，不放宽原断言或删除测试。

### 隔离生成与原字节审计

- 验证根目录：/private/tmp/forge-plugin-t7.H0ou4a，未覆盖用户工程、未使用 --force。
- frozen-template 复制 T6 的已审计冻结输入 /private/tmp/forge-plugin-t6.BJerbU/frozen-template；
  先在 before 生成两套工程，全部文件与 T6 final-frozen 比较，missing/added/changed 均为 []。
  未把实时业务源码、目录清单或 T1–T4 新模块混入冻结输入。
- 随后只带入 T7 生成器/pom-modules 和两处精确 POM 注释，在 after 生成：

```bash
node scripts/forge-create/create-project.mjs \
  /private/tmp/forge-plugin-t7.H0ou4a/after/forge-baseline-full \
  --base-package com.acme.demo --preset full
node scripts/forge-create/create-project.mjs \
  /private/tmp/forge-plugin-t7.H0ou4a/after/forge-baseline-min \
  --base-package com.acme.demo --preset minimal-admin
```

- 实时模板使用相同固定参数，目标改为 live/forge-baseline-full、live/forge-baseline-min，均生成成功。
  before/after 的命令从 frozen-template 执行，live 的命令从仓库根执行，不连接数据库。
- 审计命令：node /private/tmp/forge-plugin-t7.H0ou4a/audit.mjs /private/tmp/forge-plugin-t7.H0ou4a，退出 0。
  使用原 collectManifest/compareManifests，对全部文件路径、字节数、SHA-256 比较，没有任何路径忽略/归一化。
- full 8329、minimal-admin 5063 文件；相对 before 两套 missing=[]/added=[]，changed 精确为：
  <name>-server/pom.xml、<name>-server/<name>-admin-server/pom.xml。
  删除唯一新增的两行空标记注释后，每个文件与 before 原字节一致；配置/业务/POM 其它内容/SQL/图片不变。
- 模板两个源 POM 同样删除精确新增注释后，与 0b93c1b2 中对应文件逐字节一致。
  git diff --name-only -- code-copilot/changes/plugin-foundation/baseline 为空，原 T0 清单/provenance/补丁不变。
- 两套实时工程所有 POM xmllint --noout 通过（54/37），声明的每个子模块目录及 pom.xml 均存在。
  xmllint XPath 验证空标记为 project 直属 modules/dependencies 的子节点；唯一、为空，未落入 dependencyManagement。
  forgeVersion=1.2.0/plugins=[] 未变，实际共享改名测试验证独立 Maven groupId/Java 包/模块前缀不改标记名称。

### 测试与编译

- Node v20.19.0；完整模板回归命令：

```bash
node --test scripts/forge-create/module-catalog.test.mjs scripts/forge-create/source-glue.test.mjs \
  scripts/forge-create/pom-modules.test.mjs scripts/forge-create/project-tools.test.mjs \
  scripts/forge-shared/rename.test.mjs scripts/forge-plugin/index.test.mjs \
  code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- 新增 20 项；模板共 129/129，约 29.95 秒；live/full 根复跑以下命令 30/30，约 27.85 秒。
  合计 159 项，失败/取消/跳过均为 0，所有数据库/迁移调用均为桩：

```bash
node --test forge-baseline-full-server/scripts/db/init-db.test.mjs \
  forge-baseline-full-server/scripts/db/clean-db.test.mjs
```

- 行宽审查后，非 DB 工具回归 99 项及关键 pom-modules/project-tools 45 项复跑退出 0。
  仅缩短一条测试标题/注释并折行相同错误文案，不改测试断言或生成输出语义。
- JDK=/private/tmp/lawhub-october-jdk/Contents/Home，Maven=/private/tmp/apache-maven-3.9.11/bin/mvn，3.9.11。
  以下均使用 JAVA_HOME 指向上述 JDK、Maven -q -o 离线运行，-DskipTests：
  - 仓库 forge-server：-pl forge-admin-server -am compile，退出 0。
  - live/full server：-pl forge-baseline-full-framework/forge-baseline-full-dependencies install，
    -pl forge-baseline-full-admin-server -am validate，以及 -pl forge-baseline-full-admin-server -am compile，退出 0。
  - live/min server：-pl forge-baseline-min-framework/forge-baseline-min-dependencies install，
    -pl forge-baseline-min-admin-server -am validate，以及 -pl forge-baseline-min-admin-server -am compile，退出 0。
  - 最后两套 BOM install/validate 在 set -e 的会话中再复核，均退出 0。
  三套工程路径/坐标独立，没有同 checkout 的并发构建，也没有引入新依赖或靠新增插件模块凑编译通过。
- Node --check 生成器/pom-modules、git diff --check、新增行宽 <=120/方法 <=80/参数 <=5 检查通过。
  新运行模块/测试分别 44/129 行；生成器由 1076 减至 1061 行，原渲染器提取后不再在入口追加逻辑。

### 状态与限制

- T7 完成；下一阶段为 T8 安装命令，T8–T12 和真实数据库/插件全链路未完成。
- 本轮仅 POM 注释和生成器渲染变更，编译验证不等于 package、应用启动、全部 Java 单测或真实 MySQL 验收。
  无 Java/UI/SQL 改动，按增量标准不重复上述无关全量验证，未启动业务服务或操作真实库。
- 按 forge-project-init Skill 重生成两预设并复跑模板/生成 full DB 桩；临时工程保留，
  既有 .DS_Store 改动不提交；本轮单独本地提交到 codex/plugin-foundation，不 push、不合并 main。

## 2026-10-07 T8：插件安装器

### 范围与实现

- 继续现有 plugin-foundation，分支 codex/plugin-foundation；交付 T8，不提前实现 T9 示例、T10 门禁或 License。
- 根 forge:plugin 注册，add 支持目录/ZIP，--force 明确整包替换，list/remove 及模板 --dev 开放。
  生成工程沿用 forge.config.json 的独立 Maven/Java/模块前缀，未知配置字段保留；模板首次登记 projectType。
  空/null/数组配置不当成“未配置模板”覆盖，forgeVersion 对照直属根 revision。
- 全部路径由合法工程上下文和插件 ID 推导；严格 JSON/重复键/元数据、SemVer/范围、根/运行描述一致性。
  源 POM 校验单模块、Forge 父工程与继承 revision，不允许插件覆盖核心 revision。
  限制 ZIP 文件数/体积/解压大小，校验 CRC/目录/本地头，拒绝加密/ZIP64/穿越/链接/特殊文件/重复路径。
- 安装器不执行包内脚本或 Maven，不连接数据库。隔离副本复用 renameSourceTree，未知二进制原字节保留。
  修复 UI-only 映射不能插入 undefined 键的问题；元数据文件保留 canonical ID/feature，不做品牌替换。
- Git dirty/untracked 拒绝；无 Git/忽略路径按摘要保护。所有权、POM 区块和配置登记不一致拒绝。
  独占锁、每次写入前检查、隔离 staging、逐操作原件移动/回滚；首装/升级/卸载都留恢复备份。
  恢复目录自身 .gitignore='*'，不实现 T10 插件源码版次门禁；他人替换的锁不删除，并显式警告。
  捕获失败恢复宿主源码/POM/config；不承诺进程崩溃/断电的跨文件原子性，遗留锁/恢复目录人工检查。
- remove 只移走已登记源码/接入目录，保留备份；提示重新构建、停用关联菜单和保留迁移历史，数据库未删除。

### 实测问题与修复

- 首轮 193 项工具测试仅“从任意 cwd 执行复制入口”失败。macOS /var 与 /private/var 别名导致
  import.meta.url 与 argv 字面路径不同；按入口 realpath 比较后通过，不能把退出 0/无输出当命令执行成功。
- 整目录开发链接第一次 Maven package 失败：parent.relativePath 沿外部模块真实路径解析，父 POM 不存在。
  后端改为宿主普通接入 POM + 外部源码/资源链接；前端仍整目录链接。不复制/改名/修改外部插件原文件。
  修复后真实 package 退出 0，链接类型、接入 POM 摘要与所有权纳入检查。
- 开发构建后 remove 首次被 .flattened-pom.xml 阻断；排除已知普通 target/.flattened-pom.xml 构建输出，
  仍拒绝构建输出链接或其它未登记文件。普通复制安装摘要同样排除此构件；再实跑构建后卸载通过。
- 形态审查发现 CLI 复杂度 17，按职责提取结果输出后通过；未放宽阈值、跳过用例或降低原生成器断言。
  初次测试命令误把目录当模块入口；后续均使用 Node v20.19.0 和显式 *.test.mjs，保留全部旧回归。

### Node 与安全验证

验证根目录 /private/tmp/forge-plugin-t8.Dxg2B6；Node v20.19.0：

```bash
node --test --test-reporter=spec scripts/forge-create/*.test.mjs scripts/forge-shared/*.test.mjs \
  scripts/forge-plugin/*.test.mjs code-copilot/changes/plugin-foundation/baseline/manifest.test.mjs \
  forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs
```

- 最终 247/247，失败/取消/跳过为 0；较 T7 新增 118 项。日志 final-validation.log。
  单独工具 217 项通过（包含于上述结果）；早期工具 216/216，追加配置空值负例后增加一项。
- live/full 根复跑以下命令，30/30、无失败/取消/跳过，日志 final-generated-db.log：

```bash
node --test --test-reporter=spec forge-baseline-full-server/scripts/db/init-db.test.mjs \
  forge-baseline-full-server/scripts/db/clean-db.test.mjs
```

- 合计 277 项，所有数据库/迁移调用为桩。覆盖严格描述/UTF-8、SemVer、大整数/预发布、ZIP、路径/POM、
  模板连续安装、改名工程、仅前端/仅后端、多插件、copy/dev 互换、Git/摘要、所有权与外部源不变。
  首装/升级/卸载/dev 共 12 组逐写入故障恢复全部宿主文件；并发文件改动不覆盖，被替换的锁保留并警告。
- pnpm forge:plugin --help 从模板根实际执行成功；复制入口从非工程 cwd 执行定位工具所属工程。
  Node 语法、git diff --check、新增方法 <=80/参数 <=5/嵌套 <=3/行宽 <=120/复杂度 <=15 形态检查通过。

### 脚手架原字节回归

- frozen-template 复制已审计 T7 输入，先在 before 重生成 full/minimal-admin，与 T7 after 全文件比较零差异。
  固定末级名称、com.acme.demo、full/minimal-admin 预设，不使用生成器 --force，不引入实时业务模块。
- 只带入 project-tools/插件/共享工具增量，在 after 重生成；实时模板在 live 使用相同固定参数另生成。
  行宽和开发模式修正后复用 copyGeneratedPluginTools 在相同输出交付最终运行文件，未手改业务输出。
- qa.mjs audit 使用原 collectManifest/compareManifests，全部路径/字节数/SHA-256，无忽略/归一化。
  final-audit.log：full 8343、minimal-admin 5077；相对 T7 两套 missing=[]，changed 仅 scripts/forge-plugin/index.mjs。
  added 精确为 scripts/forge-plugin 下 bundle/content/descriptor/installer/json/ownership/paths/pom/project/stage/
  transaction/xml/zip 共 13 模块，以及 scripts/forge-shared/version.mjs；其它输出包括配置/POM/SQL/图片不变。
- 运行文件与模板原字节一致，测试/fixtures 不进入工具交付；模板目录清单保留 canonical 映射。
  冻结 POM XML 53/36、实时安装前 54/37 全通过；根版本 1.2.0，plugins=[]，CLI list 真正执行。
  git diff --name-only -- code-copilot/changes/plugin-foundation/baseline 为空，未改 T0 清单或 provenance。

### 构建与产物证明

- 合成测试包仅存在临时目录 test-package/test-package.zip，不作为 T9 示例交付。
  通过两套工程复制的 CLI 安装 ZIP，源码/UI 路径及原运行描述验证通过。
- JAVA_HOME=/private/tmp/lawhub-october-jdk/Contents/Home，Maven=/private/tmp/apache-maven-3.9.11/bin/mvn。
  两套工程各先离线安装独立 BOM，再 -q -o -pl <name>-admin-server -am package -DskipTests，全部退出 0。
  日志 full-package.log/min-package.log。不同生成目录构建未共享 target。
- 两套 Admin UI 复用本机已有 node_modules（未联网安装），Node v20.19.0/NODE_OPTIONS=--max-old-space-size=8192，
  vite build --mode production，退出 0；Vite 实际依赖版本 8.2.1，耗时 40.35s/39.88s。
  日志 full-ui.log/min-ui.log；有既有构建体积/插件耗时提示，不阻断，没有新增生产 UI 源码修改。
- qa.mjs verify-built：解出 Admin BOOT-INF/lib/<name>-plugin-demo-1.2.0.jar，与 Reactor jar 逐字节一致；
  META-INF/forge-plugin.json 等于包根描述，Start-Class 为对应 ForgeBaselineFull/MinAdminApplication。
  两套编译 JS 的 view-loader 包含 plugins/demo/index.vue；不把静态装配当登录/权限/页面运行验收。
- 临时 dev-template：-q -o -pl plugins/forge-plugin-demo -am package -DskipTests 修复后退出 0，
  dev-package-fixed.log；构建后 remove 成功，备份含源码链接，外部包所有原文件字节不变。
  两套 live 使用最终复制 CLI remove/list，恢复备份保留，plugins 清空；未删除数据库。

### 状态与限制

- T8 完成；T9 示例、T10 防误提交、T11 开发文档及 Skill 扩展、T12 人工运行验收仍待继续。
  当前 Skill 影响本轮：复用生成器改名规则，并重生成 full/minimal-admin、复跑模板/生成 full DB 桩。
- 无生产 Java/SQL/业务 UI 改动，不执行无关全部 Java 单测；未连接真实 MySQL/Redis，不启动 Admin/Flow/Vite 服务。
  没有本轮服务 PID，也未改变浏览器页面。真实迁移/普通用户授权由后续用户验收，不冒充已通过。
- 临时工程/日志/构件保留供复核，不提交。既有 .DS_Store 改动保留；仅本轮文件本地中文提交，
  不 push、不合并或切换 main。
