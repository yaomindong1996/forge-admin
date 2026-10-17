# 执行记录

## 2026-10-17 基线与授权

- 读取根 AGENTS、发布维护指南、测试标准、现有版本工具及相关变更证据。
- 使用图片生成技能制作海报；社群稿采用用户当前对话的直接务实表达，未检索到可用写作连接工具。
- `git fetch origin main` / `git merge --ff-only origin/main`：main 从 7b66771c5 快进到 aa9e59984。
- 原有 `.DS_Store`、multicolor-navigation-icons 的四份记录、production-runbook.md 保留不动。
- `git ls-remote --tags origin 'refs/tags/v1.2*' 'refs/tags/1.2*'`：已有 v1.2.0，目标 1.2.1 未使用。
  v1.2.0 解引用提交 56dd50e505731fa97f1bb73d461b83f4bc8c3248（2026-10-10）。
- 本周（2026-10-12 至 10-17，Asia/Shanghai）非合并提交共 36 条；与完整发行差异分开整理。
- 当前版本 1.2.0 → 目标 1.2.1；状态为发布准备，不自动提交、推送、打标签、部署或迁移。

后续结果按实际执行追加，不预填通过。

## 首轮增量结果

- `pnpm version:sync` / `pnpm version:check`：Admin、H5、Report、独立依赖 BOM 同步为 1.2.1，检查通过。
- `node --test scripts/forge-shared/release-info.test.mjs`：8 项通过，包含 BOM 漂移及精确随包说明。
- PC 版本 Store / 关于系统：2 文件、7 项通过。
- 首次从前端目录对仓库外脚本执行 ESLint，3 个文件因 outside base path 被跳过，不计通过；改用根目录显式配置复跑。
- 首次后端 `-Penable-tests` 聚合验证在 core 测试编译失败：BusinessExceptionDefaultCodeTest 引用未声明 AssertJ。
  保留 3 个测试和原全部断言，替换为已有 JUnit 的等价 assertEquals，不修改生产业务代码或关闭测试。
- Java 17.0.20.1 / Maven 3.9.9 使用现有本机临时工具；没有安装系统工具或启动真实服务。

## 最终本地验证

### Node / 前端契约

使用 Node v24.21.0；前端依赖复用现有 node_modules，未安装/更新依赖和锁文件。

```bash
node --test scripts/forge-shared/release-info.test.mjs
# forge-admin-ui
node node_modules/vitest/vitest.mjs run src/stores/system/__tests__/versionStore.spec.js \
  src/components/common/__tests__/SystemAboutModal.spec.js
# forge-h5-ui
node ../forge-admin-ui/node_modules/vitest/vitest.mjs run --config ../forge-admin-ui/vitest.config.js \
  src/store/modules/__tests__/version.spec.js
```

- 版本/BOM 工具 8 项、PC 7 项、H5 2 项通过。
- `node --check` 三个修改的 .mjs 脚本通过；`pnpm version:check` 输出发行版本 1.2.1 检查完成。
- 从根目录强制复用前端 ESLint 配置后有 33 个诊断：包含既有紧凑风格与 Node 原生 node:test 被误要求
  改为 Vitest；该配置并非根 scripts 的既定校验，不记录为通过，不改运行器、不关闭规则掩盖结果。
- `pnpm check:edition` 通过（索引 10354、工作区 10367 个文件）；SQL 迁移版本号查重无输出。

### 后端定向用例与四服务验证包

```bash
# forge-server
JAVA_HOME=/private/tmp/crm-t319c-check.dltTDv/jdk-17.0.20.1+1/Contents/Home \
/private/tmp/forgepro-preview-host.BxNECW/apache-maven-3.9.9/bin/mvn -o \
  -l /private/tmp/forge-v1.2.1-backend-validation-retry.log \
  -pl forge-admin-server,forge-app-server,forge-flow/forge-flow-server,forge-report-server -am package \
  -Penable-tests -Dtest=BusinessExceptionDefaultCodeTest,SystemVersionServiceTest,SystemVersionControllerTest,ForgeVersionTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dstyle.color=never
```

- 51 模块 BUILD SUCCESS，1:41，完成于 18:26:03；未进入测试选择范围的类不算执行通过。
- ForgeVersion 38 项、版本服务/控制器 6 项、默认异常码 3 项，共 47 项，0 失败/错误/跳过。
- 初次失败日志 `/private/tmp/forge-v1.2.1-backend-validation.log` 保留，没有以 package 跳过测试冒充通过。
- 读取四个目标 target JAR 根 META-INF/build-info.properties：version 均为 1.2.1，artifact 各自正确，
  time=2026-10-17T10:24:21.463Z；commit=unknown，未把当前 HEAD 当作未提交改动的发布提交。
- starter-plugin-1.2.1.jar 的 META-INF/forge/forge-version.properties 为 version=1.2.1。

### 三端构建

```bash
# forge-admin-ui
node --max-old-space-size=8192 node_modules/vite/bin/vite.js build \
  --outDir /private/tmp/forge-v1.2.1-admin-validation --logLevel error
# forge-h5-ui
UNI_OUTPUT_DIR=/private/tmp/forge-v1.2.1-h5-validation \
node --max-old-space-size=8192 node_modules/@dcloudio/vite-plugin-uni/bin/uni.js build
# forge-report-ui
node --max-old-space-size=8192 node_modules/vite/bin/vite.js build \
  --outDir /private/tmp/forge-v1.2.1-report-validation --logLevel error
```

- 三端退出码 0，PC 10167 模块、972 chunks；H5 DONE；Report 构建完成。
- Report 的 Monaco 插件在绝对 outDir 验证时另外产生四个 worker JS 到工程内 `private/tmp/...`；
  只将本轮新建的该副产物目录移动到 `/private/tmp/forge-v1.2.1-report-monaco-side-output` 保留，
  不提交构建产物、不修改插件或用户原有文件；因此不能把此次临时 Report 输出当完整发布包。
- H5 有现有动态/静态导入混用提示；Report 有 lottie eval 及 Monaco/图表样式 re-export 循环 chunk 警告，
  未将这些构建警告视为业务回归已通过，也未扩展到无关修复。
- Node 逐个断言临时目录 version.json：version=1.2.1，client 为对应 admin-ui/h5-ui/report-ui，
  commit=aa9e59984b1481e25130398cda712a1c4e23803e，builtAt 合法，notes 只取精确的 1.2.1 发布准备段。
  这些为脏工作区验证产物，HEAD 不包含本轮修改，禁止当正式制品发布。

### 社群资料与图片

- `weekly-commits.md` 保留本周 36 条提交依据；`community-message.txt` 可复制发送，明确发布准备。
- `upgrade-notes.md` 写明底座大版本、八个迁移、旧 Agent 确认/Token 与插件兼容、备份和回退限制。
- 使用内置 image_gen 生成，最终提示词保存在 `poster-prompt.txt`。
- 原图位于本任务 generated_images/exec-36ff2856-b573-4b46-9dc4-32b20c3ab77d.png，
  非破坏复制为本目录 `release-poster.png`，尺寸 1086×1448。
- 检查可见中文、V1.2.1、10.12–10.17、四组实际功能与发布准备提示；没有额外上线宣称或虚假二维码。

## 实现阶段收尾状态（追加提交授权前）

- 发版判定：发布准备；版本 1.2.0 → 1.2.1；CHANGELOG 属于精确 1.2.1 草稿段。
- 64 项定向测试、后端聚合及三端构建/版本核对通过；不等于全量回归或生产升级通过。
- 原有前端全量 25 失败的干净基线对照复用 recent-downloads-redesign 记录，本轮没有宣称已修复。
- 当前 main，发布提交待提交；没有提交、推送、新标签、正式包发布、部署或生产迁移。
- 未启动业务服务，无需停止用户进程；本轮测试/构建进程已自然退出，不影响已有 3038 预览。
- 下一步需授权提交，再从同一干净提交重建正式制品并完成真实环境门禁；不得复用本轮验证包冒充发布包。

## 2026-10-17 提交推送前复核

- 用户明确授权：将本次 V1.2.1 版本升级、更新说明、发版图片及相关验证改动一起提交并推送 main。
  不包含新标签、制品发布、部署及生产迁移；海报和说明继续使用发布准备口径。
- `git fetch origin main` 成功，main 与 origin/main 均为 aa9e59984，分歧计数 0/0。
- 复跑 `pnpm version:check` 与 `node --test scripts/forge-shared/release-info.test.mjs`，
  版本 1.2.1 检查完成，8 项测试通过；不重复累计上轮 64 项定向测试。
- 复用上轮四服务聚合与三端构建证据，未修改已验证的生产实现，不重跑无关全量测试或真实环境验收。
- 以明确清单暂存 21 个任务文件，含 1086×1448 PNG 海报和生成提示词；
  不纳入 .DS_Store、多色导航的四份记录、production-runbook.md，也不纳入临时构建制品。
- 此条为提交前记录，实际提交号与推送结果以 Git 和本次会话回执为准。
- 暂存检查发现 weekly-commits.md 末尾多一个空行，已删除；不改变统计或业务内容。
  `pnpm check:edition` 检查暂存与工作区通过（索引/工作区各 10363 个文件）。
