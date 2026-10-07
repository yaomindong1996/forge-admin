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
