# 执行记录

## 2026-10-20 开始

- main，版本 1.2.1；保留部署/导航图标记录等无关未提交内容。本轮未获得提交、推送或部署授权。
- 使用 Forge 流程技能核对配置所有权、真实版本快照、租户权限；使用界面审查技能但以 DESIGN.md 为优先，不套营销视觉。
- 原模型状态不能说明草稿是否发布，采用后端快照比较；原每页逐模型绑定请求改为有界批量接口。

## 2026-10-20 23:02 本地开发与隔离验证完成

### 变更与审查

- 发布摘要仅取当前租户分页可见的模型，批量匹配模型/租户/版本/部署/流程定义的非删除快照；比较保存的 BPMN 和 formJson，不依赖更新时间，不写回生命周期/发布版本。比较包括格式与画布布局，不是业务语义 diff。
- 业务绑定批量接口最多 100 个 Key，DTO 校验非空/长度，租户来自会话，沿用原 `ai:businessFlow:config` 权限；所有查询在 XML 中参数化。旧单模型接口保留。
- 拆出列表、排序、绑定和操作菜单领域，以及表单引用纯函数。`useFlowModel.part1.js` 从 1188 降至 963 行，`model.vue` 为 342 行；既有业务表单/发起测试/设计器入口未重新实现。
- 工具栏统一点击查询/回车生效，分页使用上次成功查询条件；失败重试保留原失败请求上下文，过期请求和卸载后结果不落地。排序限制本页，锁定查询/分页和卡片动作。
- 界面沿用 DESIGN.md 紧凑卡片与主题变量，无装饰色条/新依赖。新增字典迁移 226，CHANGELOG 只记 Unreleased，版本仍为 1.2.1。

### 自动化命令与结果

前端目录 `forge-admin-ui`，复用已安装依赖，未重新安装：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
node node_modules/vitest/vitest.mjs run src/components/flow-designer src/views/flow \
  --maxWorkers=2 --minWorkers=1 --reporter=json \
  --outputFile=/private/tmp/forge-model-catalog-all-tests.json
{ git diff --name-only --relative -- .; git ls-files --others --exclude-standard -- src/views/flow; } \
  | rg '\.(vue|js)$' | xargs node node_modules/eslint/bin/eslint.js
source ~/.nvm/nvm.sh && nvm use v24.21.0
node --max_old_space_size=8192 node_modules/vite/bin/vite.js build
```

- Vitest：**59 文件、485 项通过，0 失败/跳过**，其中本轮新增 20 项。
- ESLint：最终退出 0。最后新增的表单引用测试曾有一项对象换行风格错误，已修正并复跑通过。
- Vite：退出 0，`built in 24.42s`。插件耗时诊断提示 overlapping hooks 不可单独计时，不是构建错误。
- 详细日志：`/private/tmp/forge-model-catalog-all-tests.json`、`/private/tmp/forge-model-catalog-all-tests.log`、`/private/tmp/forge-model-catalog-build.log`。

后端目录 `forge-server`，复用机器上现有缓存工具，未修改工程 Java 版本或安装新工具：

```bash
export JAVA_HOME=/private/tmp/crm-t319c-check.dltTDv/jdk-17.0.20.1+1/Contents/Home
export MAVEN_SKIP_RC=true
forge_catalog_mvn=/private/tmp/forgepro-persistent-cache.8TXNzW/apache-maven-3.9.9/bin/mvn
"$forge_catalog_mvn" -B -ntp \
  -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator,forge-flow/forge-flow-server \
  -am compile -DskipTests -Dstyle.color=never
JAVA_TOOL_OPTIONS=-javaagent:/Users/mini32g/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.11/byte-buddy-agent-1.18.11.jar \
"$forge_catalog_mvn" -B -ntp \
  -pl forge-framework/forge-plugin-parent/forge-plugin-flow,forge-framework/forge-plugin-parent/forge-plugin-generator,forge-flow/forge-flow-server \
  -am test -Penable-tests \
  -Dtest=FlowModelPublicationSummaryTest,FlowModelPublicationQueryServiceTest,FlowModelPublicationMapperTest,FlowModelPublicationDictionaryMigrationTest,BusinessFlowModelBindingControllerTest,BusinessFlowModelBindingsMapperTest,BusinessFlowModelBindingQueryServiceTest,FlowModelControllerCatalogTest,FlowModelMapperSqlContractTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dstyle.color=never
```

- 相关 39 模块 reactor 编译：`BUILD SUCCESS`，21.300s。
- generator 6、flow 插件 10、Flow Controller 2，共 **18 项通过，0 失败/跳过**；`BUILD SUCCESS`，30.996s。
- 初次 Mockito 静态 Mock 因本机 JVM 禁止 self-attach 失败，改用本机现有 Byte Buddy Java agent 后复跑通过，没有跳过或删除失败用例。
- Mapper XML 展开后的 BoundSql 与 JSqlParser 语法/参数契约验证通过；新增字典 SQL 在独立 H2 MySQL 模式最小表结构中连续执行两次，结果 1 个类型、4 个 tenant=1 状态，不访问真实库。
- 日志：`/private/tmp/forge-model-catalog-compile.log`、`/private/tmp/forge-model-catalog-backend-tests.log`。

仓库根目录：

```bash
git diff --check
ls forge-server/db/migration/V*.sql | sed 's/__.*//' | sort | uniq -d
rg -n '\$\{' forge-server/db/migration/V1.0.226__add_flow_model_publication_state_dict.sql
source ~/.nvm/nvm.sh && nvm use v24.21.0
node scripts/guards/check-edition.mjs
```

- whitespace、迁移重号、226 占位符扫描均无输出；开源边界检查通过（索引 10416、工作区 10506 个文件）。

### 浏览器隔离验收

- `verification/preview.mjs` 绑定 `127.0.0.1:4321`，装配真实 `model.vue`、卡片、工具栏及列表 composable；只在验收适配器提供合成模型/字典/绑定，POST 明确拒绝。业务表单、选人组件用排除范围提示代替，不能作为业务 E2E。
- 浅/深色桌面 1440×960，390×844、320×840 验证无文档横向溢出；390px 工具栏三行，列表正文实际可滚动（scrollTop 875.5），分页底部约 759.6px 保持可见。
- 正常模型：1 次模型请求、1 次批量绑定；设计中不显示虚构 v1，已发布显示匹配快照版本，历史不匹配显示“发布版本 — / 发布信息待核对”。
- 实际点击验证：无匹配查询与重置、设计中状态查询、进入/取消本页排序、发布更新确认/取消、更多→编辑信息/取消；没有保存或发布业务写入。
- 绑定失败仍展示模型并标“业务绑定暂不可用”与重试；模型失败有独立错误/重试提示，不冒充空列表。最终页面 console error 数为 0。
- 初次隔离页面因装配到范围外 AuthImage/业务表单依赖报错；仅修正验收入口的业务表单排除别名后新建页面，生产组件未替换。最终截图和控制台结论均来自修正后的页面。
- 证据：`artifacts/desktop-catalog.jpg`、`dark-catalog.jpg`、`narrow-catalog.jpg`、`binding-error.jpg`、`error-query.jpg`。截图标明“合成数据，不连接业务服务”。
- 仅启动本轮预览 PID **73650**，已 Ctrl-C 停止；4321 无监听，验收标签已关闭、viewport 覆盖已 reset；未操作用户原标签或其他任务服务。

### 交付边界

- 未连接真实 MySQL、未启动 Admin/Flow、未执行真实 Flyway/Flowable 发布与审批、未测生产加密或 Sa-Token 拦截全链路。DTO/权限声明/租户与 SQL 单测不能替代这些环境验证。
- 用户部署时需同步 Admin（批量绑定和字典迁移）、Flow（发布摘要）与 UI；Admin Flyway 执行 `V1.0.226__add_flow_model_publication_state_dict.sql`。应用回滚可保留无害字典，不删除业务数据。
- 本轮只在 main 开发，未提交/push/部署；保留所有原有部署、导航图标和 memory 等无关未提交文件。

## 2026-10-21 Git 交付检查

- 用户明确授权将本次列表优化、字典迁移及配套测试/规范/截图记录提交并 push 到 main，不包含部署或新版本发布；版本保持 1.2.1，更新归属 Unreleased。
- 远端仍为 `git@gitee.com:ForgeLab/forge-admin.git`；`git fetch origin main` 成功，提交前 `HEAD...origin/main` 为 `0 0`，无需合并/重写历史。
- 仅选择本变更的 55 个文件；其他部署、导航图标、memory、`.DS_Store` 等原有改动保留在工作区，不加入本次提交。
- 复用 10-20 已完成的 485 项前端/18 项后端、编译/构建及浏览器证据；本轮没有业务代码改动，不重复启动服务或真实数据库验证。
- 提交前重新检查 whitespace、Flyway 版本号唯一性和迁移 226 占位符，均无问题；开源模板边界检查通过。提交/推送最终结果以 Git 输出和远端 SHA 核对为准。
