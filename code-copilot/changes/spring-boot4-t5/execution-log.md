# T5 执行记录

## 2026-10-12：开始

- 继续 codex/spring-boot4-t0-compatibility，复用 T4 Spec/Tasks/Test/Log/Report。
- 当前已有 T0–T4 和无关用户改动，保留全部；未切换 main，未提交或 push。
- 核对 AuthImage.src 与错误 file-id 传参、setInterval 生命周期、Agent SSE 终态漂移。
- 已读根 AGENTS、code-copilot 规则、测试标准、相关踩坑与前端 DESIGN；不使用无关 Skill。
- 日常开发版本 1.2.0 / Unreleased，真实环境、生产迁移与部署不执行。

实际命令、失败修复、结果与清理证据在执行后追加，不预填通过。

## 第一批前端增量

- ui-target1.log：4 文件 / 47 项通过（共享查询、协议、SSE 生命周期、消息事件）。
- ui-target2.log：7 文件 / 62 项，59 通过 / 3 失败；确认恢复测试夹具遗漏 role=assistant，
  与生产 createAssistantMessage 的结构不一致，导致合法的回放助手查找为空。
  补完整合成消息结构，不改生产归属检查或放宽恢复断言，复跑结果另记。
- eslint1.log：自动格式化后仍有 7 处单行多语句，新代码改为分行，不关闭规则。
- target1.log：目标 Reactor 在 AI testCompile 失败，新测试误写不存在的 ReactContext.cancel()。
  改用真实 setCancelled(true) 及已有取消信号；未改生产 API 或排除用例，后续复跑另记。
- ui-target3.log：7 文件 / 62 项通过，修正消息夹具后确认恢复断言全部保留。
- target2.log：AI 308 项中 5 个新增终态用例因未安装可信登录快照被执行池拒绝。
  测试补与生产调用一致的 LoginUserContextHolder Scope，不关闭 AgentCallerGuard 或降低权限门槛。

## 实现与本地交互验证

- Pinia 只持有任务显示状态与账号版本；有界串行查询由 ImageJobQueries 独立负责，
  同 ID 多订阅共享请求、最后订阅离开取消，暂停恢复仅 GET。
- 图像表单/历史/结果拆分；AuthImage 使用有效 src；Agent 只识别内置已完成的图片任务元数据，
  不访问返回的任意 endpoint。无现有图片查询权限时只提示，不新增角色权限。
- Agent 页面由 1,212 行收敛到 1,000 行以内；直接流与确认生命周期下沉独立对象，
  外层 composable 不堆长函数。模型查询与提交职责进一步拆分。
- 后端 lease 失效只向本次 sink 发 ERROR，不发布旧全局 AGENT_END；模型空响应/异常、预算终态一致。
  确认恢复/拒绝与工具实际结果不被客户端 complete 覆盖。
- 最后审查发现 PERSIST_META 绕过 Jackson Long 策略；已显式字符串化消息记录 ID，补实际 Service SSE 测试。
- 新字典迁移 V1.0.218 只补生成中，H2 验证可重复、租户 1 与未安装/已删除模块边界；未执行真实迁移。
- 仅合成 Vite 验证页绑定 127.0.0.1:3028，所有接口/图片夹具留在临时目录。
  首次沙箱禁止 listen（EPERM）；仅该本地验证页获升级权限后运行。
  临时入口曾误写 Naive ES 扩展名，核对实际 package.module=es/index.mjs 后修正，不改产品依赖。
- 浏览器实际点击模型选择、显式提交、历史结果与暂停恢复；验证成功图片、失败说明与暗色主题。
  320px 初测 documentWidth=694，修正 Grid 轨道与内部表格横向滚动后 documentWidth=320，卡片宽 304。
  已恢复默认视口；截图 light.jpg / dark.jpg / narrow.jpg 位于临时合成页目录，不含真实业务数据。

## 已有通过记录（最终复跑另记）

- target3.log：新终态 5、租约 8、字典 2，共 15 项定向通过，14:40:49 完成。
- clean1.log：53 模块、3,463 项，0 失败/错误/跳过，14:46:49 完成；四服务 JAR 生成。
- ui-target4.log：8 文件 / 71 项通过。
- ui-target5.log：职责拆分后 8 文件 / 71 项通过，15:04:14 开始。
- admin-ui1.log：324 文件 / 2,350 项通过，14:47:18 开始，60.60 秒。
- admin-ui2.log：拆分/滚动边界自检后 324 文件 / 2,350 项通过，15:08:36 开始，53.79 秒。
- admin-build1.log：PC 构建通过，37.61 秒；不是正式发布制品。
- 最后的消息 ID 修复新增 1 Java / 1 JS 测试；最终命令和数量以随后实际复跑为准，不能提前记通过。

## 最终复跑与产物核对

| 日志 | 实际结果 |
| --- | --- |
| target4.log | 最后的实际 Service SSE Long 测试通过，15:15:44 完成；只记定向结果 |
| clean2.log | 53 模块 clean package 通过，15:20:43 完成，2:09；3,464 项，0 失败/错误/跳过 |
| admin-ui3.log | SSE ID 修复后 324 文件 / 2,351 项通过；15:15:13 开始，80.16 秒 |
| admin-build2.log | SSE ID 修复后 PC 构建通过，约 1:05；既有分块/插件耗时提示不阻断 |
| eslint6.log | 本轮相关 JS/Vue/测试 ESLint 通过，0 错误/警告，不关闭规则 |
| admin-ui4.log | 加入终态半包负面验证后最终 324 文件 / 2,353 项通过；15:34:34 开始，59.06 秒 |
| admin-build3.log | 最终 PC 构建通过，46.44 秒 |
| eslint7.log | 最后一处 SSE 终态保护及测试 ESLint 通过，0 错误/警告 |

日志前缀 `/private/tmp/forge-boot4-t5-`。最终汇总 792 份 Surefire XML，
AI 309 / 系统 302，较 T4 增加 8 项；其它模块数量保持。前端增加 74 项，未新增跳过或排除。
末轮要求 AGENT_END 的数据本身为有效对象，拒绝被通用尾帧解析器保留为字符串的截断 JSON；
以真实 SSE 管线和 null/数组/半包负面测试验证，不靠调整解析器的全局完成语义影响其它调用方。

- 四个 JAR 的 build-info 均核对为 version=1.2.0、各自正确 artifact，time=2026-10-12T07:18:34.384Z；
  build.commit=unknown，未假造发布提交。
- PC dist/version.json：version=1.2.0、client=admin-ui、commit=719a2fc0b28d929edef62769dc2adc5e75ef557e，
  builtAt=2026-10-12T07:34:34.642Z，已从最终 admin-build3 的 version.json 实际读取。
  当前有未提交改动，提交号只代表 HEAD，产物不是干净正式发布提交。
- 18 个相关生产 JS/Vue AST 尺寸/方法/参数检查无超限；Agent 入口 946 行。
  相关 Java 单类/单行检查与安全/状态边界自检完成。version check 保持 1.2.0、diff --check 通过。
- 新 SQL 的 `${...}` 静态扫描无结果，历史迁移无差异；只新增 V1.0.218，未执行真实 Flyway。
- 初版合成预览缺少主应用自动导入的 NTag 注册，曾产生警告；临时入口补注册后重新验证并重存截图，
  最终预览日志无此警告。没有改生产依赖、全局字典组件或关闭校验。
- 默认视口恢复，本轮创建的两个浏览器标签已关闭；两次有效合成 Vite 服务均 Ctrl-C 退出，
  3028 无监听。临时夹具/截图保留便于核对；没有真实 Admin/Flow/DB/Redis 服务 PID。
- 所有 Maven/Node 测试、构建会话结束；测试内线程池、H2、loopback 生命周期自行关闭。
- 当前分支/HEAD 不变；main、ForgePro 和无关改动保留，没有 Git 提交、push、标签、发版或部署。

## 可复跑命令

使用本机 Java 17 / Maven 3.9.11 / Node 20.19.0；本轮预加载既有 Byte Buddy agent：

```bash
JAVA_TOOL_OPTIONS='-javaagent:<本机 byte-buddy-agent-1.18.11.jar 绝对路径>' \
mvn -B -ntp -f forge-server/pom.xml -Pdev,enable-tests clean package -Dstyle.color=never

node scripts/forge-shared/version-cli.mjs --check
git diff --check
```

PC 从 forge-admin-ui 目录：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
node node_modules/vitest/vitest.mjs run --maxWorkers=4 --minWorkers=1
node --max_old_space_size=8192 node_modules/vite/bin/vite.js build
```

实际工具路径复用 T4：`/private/tmp/forgepro-build-tools.5kNsIl/jdk-17.0.20.1+1/Contents/Home`、
`/private/tmp/forge-boot4-t2-tools.senzpW/apache-maven-3.9.11/bin/mvn`，
agent 为本机 Maven 缓存中 1.18.11 JAR。未新增或上传真实凭据。

发版判定：日常开发；版本 1.2.0 保持；CHANGELOG 归属 Unreleased；提交/发布/部署均未执行。
不能据此宣称真实环境所有功能验收完成，剩余边界见 compatibility-report。
