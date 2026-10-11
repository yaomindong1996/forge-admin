# 执行记录

## 2026-10-11 开始

- 社区 main 有其他任务的未提交文件，已识别并保留；本轮不会将它们带到生成源。
- 用户已确认新的独立工程身份、full 能力及新建独立数据库；凭据不记录在此文件。
- 已读取项目初始化 Skill、数据库清理 Spec、自动化验证与发版规范。
- 当前阶段为初始化/开发验证，不正式发版，不推送，不部署。

## 2026-10-11 代码初始化与隔离验证

- 生成源固定为社区 `54b03a7fad99dc6341614155bd65b1297477a560`，版本 `1.2.0`。
  未复制社区工作区已有的部署/版本规范未提交修改。
- 新增 `scripts/forge-upstream`，冻结生成映射，产品根 POM 版本独立于社区兼容基线。
  plan/apply 只在干净升级分支工作，使用真实 Git merge，保留冲突和 merge 祖先。
- 实际生成发现旧 `com.forge` 匹配会再次命中新包名前缀，已修复为完整包段一次替换；
  Maven groupId 与 mainClass 分开处理。首次生成物移动到本轮临时目录保留，未覆盖任何原工程。
- 第二次生成的规范化基线为 `c68cde8f8800299d1eedb198e6fdc75650f429aa`；
  冻结生成器摘要为 `0c03acaa354a517fc6a13bd5e918558ac0af35b57857fd2290eb3fd8dfe9d360`。
- 新工程排除历史变更/记忆、部署脚本、演示包、客户端旧 AI 配置和旧文档；
  保留运行字体/图片、许可证、数据库/插件/版本工具和开发规范。
- 新工程配置独立本地端口、Flow 内部地址、浏览器登录/协商密钥前缀、Redis Pub/Sub 频道。
  凭据仅存被 Git 忽略的本地 YAML，示例只含环境变量；未配置 push 远端。

### 已执行验证

- `node --test scripts/forge-upstream/upstream.test.mjs scripts/forge-shared/rename.test.mjs scripts/forge-create/*.test.mjs`：97/97。
- `node --test scripts/forge-plugin/installer.test.mjs scripts/forge-plugin/identity.test.mjs scripts/forge-shared/release-info.test.mjs`：55/55。
- 模板 `init-db.test.mjs clean-db.test.mjs`：31/31；实际 full 生成端同两组测试：31/31。
  上述是脚本契约测试，不是远程数据库验收。
- 生成端 `node --test scripts/project-baseline.test.mjs`：4/4（身份、私有配置排除、浏览器状态、流程频道）。
- 生成端 `forge:upstream verify`、`version:check`：通过；迁移目录无产品本地修改。
- 真实生成工程克隆到独立临时目录，使用相同社区提交执行 plan：changes/blocked/conflicts 均为空；
  apply 返回 unchanged，工作区无变化，证明规范化基线可重复生成。
- Java 17 / Maven 3.9.9 已验证官方 SHA256/SHA512，仅临时使用，未修改系统 Java 配置。
  新坐标下四服务聚合 `mvn -o -pl <admin>,<app>,<report>,<flow> -am compile -DskipTests`：51 模块 BUILD SUCCESS。
  最终增量编译再次通过；未把跳过 Java 单测的编译称为测试通过。
- Admin / Report 执行 Node 20 的 Vite build，H5 执行 uni build：三端最终构建通过。
  版本产物均为 1.2.0 / 正确客户端 / 当时真实 Git HEAD（开发验证，不是正式发布包）。
- 继承警告：报表 CSS `:deep()` 语法与 Monaco 分块循环提示、产物大小提示；H5 混合静态/动态 import 提示。
  未在初始化任务里顺带重写这些既有页面。
- `node scripts/guards/check-edition.mjs`：通过；`git diff --check`：通过。

### 未完成与边界

- MySQL TLS 连接返回 `2026 SSL is required but the server doesn't support it`。
  没有自动改明文，没有执行 SQL；尚未确认目标库存在性，也尚未新建/初始化新库。
  已询问用户启用 TLS 或明确允许测试库临时明文访问，等待选择。
- Redis 已按最新用户配置写为 localhost:6379 / DB 0，当前本机未发现监听。
  DB 0 若与社区共用会冲突，已询问使用独立逻辑库或确认专用实例；未启动/写入 Redis。
- 因此未运行后端服务、登录、上传或真实业务验收。没有修改任何现有库，没有部署或推送。
- 发版判定：日常开发初始化，revision 保持 1.2.0；通用工具变化归入 Unreleased。
  新工程只有本地初始化提交；社区本轮通用工具改动仍未提交，其他脏文件原样保留。

### 交接状态

- 新工程最终本地提交 `7e308b5`，当前分支 main，工作区干净，无推送远端。
- 可提交文件 8651 项扫描未发现本轮 MySQL 密码；本地连接配置保持忽略。
- 最后工具格式整理后同步测试 10/10 再次通过；命名/隔离测试 4/4 再次通过。
- 原工程仅增加通用工具/生成器修复/记录，未替用户提交其它任务文件。

## 2026-10-11 用户确认 Redis 15 与 TLS 优先

- 用户确认 localhost:6379 使用独立 DB 15；四服务本地配置和示例中的 Spring Redis、Redisson、
  Sa-Token 已同步。基础认证配置跟随 `spring.data.redis.database`、默认 15，Docker 示例也改为 15。
- 用户确认先启用 MySQL TLS、暂缓真实建库。保持本地 JDBC 强制 TLS，不降级明文；
  本轮未连接 MySQL/Redis，未修改服务端 TLS、建库、执行 SQL 或清空缓存。
- 新工程 README、AGENTS、初始化记录及 Unreleased 已更新，避免后续误把初始化配置当作运行验收。
- `node --test scripts/project-baseline.test.mjs`：5/5；新增四服务及 Docker 库号回归断言。
- 模板 `node --test forge-server/scripts/db/init-db.test.mjs forge-server/scripts/db/clean-db.test.mjs`：31/31。
- 复用本轮已生成的 full 工程，执行对应 `forgepro-server/scripts/db/` 的两组测试：31/31。
  以上均为脚本契约测试，不是远程数据库实跑。
- 四份忽略的本地连接配置脱敏核对：各三处库号均为 15、强制 TLS 为 true、Git 忽略及 600 权限正确；
  检查只输出判断结果，没有输出数据库凭据。
- `node scripts/forge-shared/version-cli.mjs --check`：发行版本 1.2.0，检查完成。
- `node scripts/forge-upstream/index.mjs verify`：仍为原准确社区提交、规范化祖先及冻结映射摘要，通过。
- 两工程 `git diff --check` 通过。本轮未改 Java/前端源码，复用之前编译构建证据；未启动任何服务。
- T3.2 配置完成；T4/T5.2 按用户选择暂缓。日常开发不升版，本次配置补充未提交、推送或部署。
  新工程 main 保留本次可审查改动；社区其它未提交内容未动。
