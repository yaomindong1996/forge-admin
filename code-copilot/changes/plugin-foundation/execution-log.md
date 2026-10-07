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
