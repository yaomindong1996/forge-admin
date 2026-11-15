# 执行记录：流程管理 / 流程编排全量巡检（2026-11-14）

- 范围：流程中心（模型、BPMN 设计器、版本历史/对比、分类、表达式、监控、用户组、常用意见）、
  我的待办/已办/发起/抄送、应用业务流程（列表、编排设计器、运行记录、复制/停用/删除）。
- 环境：本地 admin-ui dev server（最新代码）→ 演示网关；浏览器 ego-browser，账号 admin。
- 版本：未发版，计入 CHANGELOG Unreleased；问题清单与根因见 `findings.md`。

## 环境处理

- 演示 Flow 服务停在 `08b71ae75`，与最新前端不匹配（发布信息字段缺失）；部署 Flow 至 `ed06cfac5`。
- 部署回归：Admin/App/Flow 新镜像误用无字体库基础镜像，发布流程 500（`libharfbuzz.so.0`）；
  改用 `forge-local-runtime:ubuntu24-java17-fonts-20261019` 重建为 `1.2.1-ed06cfac5-fonts`，与原线上镜像字体库一致（6 个）。

## 自动化验证

| 命令 | 结果 |
|------|------|
| `vitest run src/components/flow-designer src/views/flow src/components/flow` | 72 文件 / 547 条全部通过 |
| `vitest run src/components/business-process-designer` | 57 条全部通过 |
| `vitest run src/views/workspace/__tests__/WorkspaceSummaryMetrics.spec.js` | 3 条通过（去掉修复时 2 条失败） |
| `vitest run`（全量） | 2916 条，69 失败；同环境暂存本次改动基线 73 失败（本次修复 4 条存量失败，未新增） |
| `mvn -Penable-tests -pl forge-plugin-flow,forge-flow-server -am test`（流程包全部） | 17 + 206 + 43 条全部通过 |
| `mvn -Penable-tests ... -Dtest='BusinessProcess*Test,BusinessStartableObject*Test,...'` | 98 条全部通过 |
| `FlowModelPublicationDictionaryMigrationTest` | 修正迁移目录后通过 |

全量前端存量失败集中在 `AiFormItem`、`printRuntimeContext` 等非流程用例，昨日基线为 4 条，今日环境升至 73 条，待单独排查。

## 浏览器验证（修复后）

- 设计器通知模板预览正常显示；版本详情只读图结束圆点居中，「自动布局」后重新保存坐标一致；发布 v1/v2、版本对比正常。
- 「发起测试」→「我发起的」自动按标题筛出 1 条；待办同意 →「审批通过 1 条」。
- 分类新增/子级/编辑/停用/启用；用户组新增/成员/删除；表达式、常用意见新增/删除；业务流程新建/复制/停用/删除、取消运行。
- 删除有历史数据的流程模型：仅提示一次具体原因。
- 后端修复（常用语去重、分类管理含停用、运行详情名称与顺序）需部署后验证，见下节。

## 测试数据

- 保留：流程模型「QA流程测试1114」（有 1 条历史实例，按设计不可删除）。
- 已删除：模型副本、业务流程 QA编排测试1114 及副本、QA 表达式、QA 用户组、QA 常用意见。
- 已取消：单据 1/2 的 `approval_qsxf` 测试运行。
- 已清理：分类「QA分类1114改」及子分类（部署修复后在管理页可见，已删除）。

## 部署与线上验证（f33f7d6a1）

- 提交 `f33f7d6a1` 已推送；独立 worktree 构建 Admin/App/Flow（`-DskipTests`），build-info commit 均为 `f33f7d6a1`。
- `~/forge-upload/20261114-admin-app-flow-f33f7d6a1/deploy.sh`：固定 `FROM forge-local-runtime:ubuntu24-java17-fonts-20261019`，
  构建后校验字体库（libharfbuzz/libfreetype/liblcms2 共 6 个）不足即中止；compose 备份后替换三服务，任一不健康即回滚。
- 结果：三服务 `1.2.1-f33f7d6a1-fonts` healthy，字体库 6/6，Scheduler started 0，Flyway `forge_admin_new` up to date（无迁移）。
- 线上验证：可用常用语「批阅、同意、已阅、情况属实」不再重复；分类管理树含停用分类、选择树仅启用；
  业务流程运行详情显示「报销审批」，节点顺序 手动开始 → 审批流程 → 完成；Admin `/system/version` commit `f33f7d6a1`。

## 踩坑

- 演示环境 Java 镜像必须基于 `forge-local-runtime:ubuntu24-java17-fonts-20261019`；无字体库基础镜像会在 Flowable 部署生成流程图时
  `UnsatisfiedLinkError: libharfbuzz.so.0`，表现为「发布流程」500。旧标签 `-fonts1` 后缀即指该基础镜像，不能只比对 Dockerfile 末尾层。
- 质量门禁：本机 `quality-gate.py check` 报 800 条 stale 基线条目（改动前即 799 条，疑与生成基线环境差异有关），未执行 prune，待人工确认。

## 抄送节点发布与审批操作（a62ef91c0 起）

- Flow `a62ef91c0`：plugin-flow 210、flow-server 43、client 17 测试通过；字体基础镜像部署 `1.2.1-a62ef91c0-fonts` healthy，字体库 6/6。
- 线上：「QA审批操作1114」（含抄送人节点）发布返回「部署成功」，「发起测试」成功，待办出现在审批A。
- 动态加签 test33：返回「加签成功」，`/api/flow/task/{taskId}/sign-relations` 返回 `targetUserName=test33`，页面却显示 70 → 修复 started.vue。
- `npx vitest run src/views/flow/utils/__tests__/signRelations.spec.js`：2 passed；eslint 通过；`pnpm check:edition` 通过；
  `pnpm quality:check` 无新增发现（仍为既有 800 条 stale 基线，未 prune）。
- 演示 Admin 会话中途失效被登出（验证码登录需人工），减签/转办/驳回/撤回/抄送的浏览器验证待重新登录后继续。
- 重新登录后继续：加签记录显示 test33 ✓；减签只列 test33，「减签成功」后记录变「已撤回」✓；改派 test33 / 改回 admin「当前任务已改派」✓；
  转办 test33「转办成功」，时间轴显示 test33 + 转办说明 ✓。
- 改派回 admin 后同意报「任务动作幂等冲突」→ 修复 FlowTaskActionAuthorization；plugin-flow 213、flow-server 43 通过；部署 Flow `1.2.1-150bdb5c8-fonts` healthy、字体库 6/6；
  同一任务重试「审批通过」✓。审批B 驳回弹窗仅列「审批A」，「已退回」后回到审批A ✓。
- 发起时间修复：`npx vitest run src/views/flow` 12 文件 53 用例通过；浏览器待办详情显示 2026-11-14 14:29:39 ✓。
- 审批A→B 同意后抄送节点生效：「我的抄送」出现「QA审批操作1114-测试发起」未读 ✓，「已读」后状态变已读 ✓，但顶部「未读抄送」卡仍为 1（刷新后 0）→ 修复。
- 撤回：新实例在审批A「撤回成功」✓。指标卡同步修复后：待办页同意审批A，「本周已办」13→14 无需刷新 ✓。
  `npx vitest run src/views/workspace src/stores ...` 14 文件 122 用例通过。
- 分支：QA分支1114（条件分支，分支1 `${1 == 2}`）发起后进入默认「分支2审批」并审批通过 ✓；QA并行1114 同时生成分支1/2 两条待办，
  审批一条后另一条仍在，两条都通过后流程结束 ✓；工作台 summary「发起中」按实例计数后 35（原 65 左右）✓。
- 包容分支修复后：QA包容1114 分支1 `${1 == 2}` 发布「部署成功」，发起后只生成默认「分支2审批」✓（修复前分支1无条件会必走）。
  `npx vitest run src/components/flow-designer` 57 文件 491 用例通过（6 个 unhandled errors 为既有，stash 后同样 6 个）。
- 流程监控（测试实例 QA审批操作1114）：挂起「流程已挂起」后回退/转派提示先激活 ✓；激活 ✓；节点回退到审批A「流程已回退」待办回到审批A ✓；
  终止「流程已终止」✓；删除流程数据「流程数据已删除」列表与任务行消失 ✓。
- 版本：QA包容1114 发布 v2（改名「默认分支审批」）→ 回退到 v1 生成 v3「回退成功」，新发起实例节点为「分支2审批」✓；清理历史（保留 1）返回已清理 0（release 版本受保护）。
- 修复后：首屏加载中输入「QA并行1114」立即点查询 →「匹配 1 个流程模型」✓；版本历史 v3（当前）无回退按钮 ✓；监控节点回退在仅有当前节点的实例上显示「无数据」✓。
  `npx vitest run src/views/flow` 12 文件 55 用例通过。
- 业务流程触发（费用报销E2E）：新建「QA事件触发1114」开始节点选「记录创建后」→ 发布 V1；临时开启应用数据源「写数据」，PC 运行态新增报销单
  「新增成功」，运行记录出现「QA事件触发1114 记录 4 执行成功」✓。「QA定时触发1114」选「定时扫描」日期字段=发生日期，发布 V1 ✓
  （演示节点调度关闭、运行账号未配置，未实际触发）。应用发布：发布检查 0 阻断 2 提醒，「应用 v2 发布成功」✓。
- 应用数据源「写数据」已关回（服务端读回 false）。
- 设计器修复后：开始节点名自动变「事件触发/定时触发」✓，过滤条件文案正确 ✓，日期字段仅「发生日期/创建时间/更新时间」✓；
  `npx vitest run src/components/business-process-designer` 60 用例通过。
- 产品确认后（我发起的按申请、版本清理只保护当前/运行中）：plugin-flow 217、flow-server 43 通过；部署 Flow `1.2.1-21f21fa75-fonts` healthy、字体库 6/6。
  线上：「我发起的」QA审批操作1114 由 5 行变 2 行（每实例一条）、QA并行1114 由 2 行变 1 行，全部发起总数 188 ✓；
  QA包容1114 清理历史（保留 1）「已清理 2 个版本」，仅剩当前 v3 ✓。
- H5（:3009，admin + 固定验证码登录）：「发起审批」显示「暂无可发起的审批」，接口 startable-objects 正常返回，原因是应用 v2 发布后页面菜单 visible=0
  被导航过滤 → 修复后列出「费用报销E2E：报销申请、报销审批」✓（事件/定时触发流程不在目录，符合预期）。`node --test` mobile-menu/startable-objects 12 通过。
- H5「我发起的」每申请一张卡片 ✓；对运行中 QA包容1114 点「催办」提示「已催办」并进入冷却 ✓。卡片时间为当前节点时间 → 补 processStartTime，plugin-flow 217 通过。
- H5 待办（QA审批操作1114 新实例）：更多→加签 test33「加签成功」，加签人员区显示 test33 ✓；减签「减签成功」✓；同意审批A ✓；
  审批B 驳回 → 选择驳回节点仅「审批A」→ 回到审批A ✓（修复前需二次确认「确认退回」，修复后单次确认 ✓）；节点进度顺序修复后为 发起人→审批A→审批B→抄送人→结束 ✓。
  `node --test` utils/store：207 用例 205 通过，2 个失败为既有 mobile-field-contract（与本次无关）；同步更新 console-design-system 对提交时间表达式的断言。
- H5 走完 QA审批操作1114（A 同意→B 同意）后「抄送我的」出现未读 1 条，进入详情自动已读 ✓；详情「流程 -」、无表单仍提示去 PC → 修复（Flow 抄送查询补流程名、流程图名称回退；H5 详情仅有表单受阻时提示）。
  plugin-flow 218、flow-server 43、H5 node --test 209 中 207 通过（2 个既有失败）。
- H5 转办：QA包容1114 转办 test33 成功（我发起的处理人变 test33）✓；修复后再次转办无二次确认 ✓；测试后经改派接口改回 admin。
- H5 撤回：我发起的 QA包容1114 运行中实例「确认撤回」→「撤回成功」，卡片变「已撤回」✓。H5 阶段覆盖：发起目录、我发起的、催办、加签/减签、同意、驳回到节点、转办、抄送查看/已读、撤回。

## 第二轮：审批人多人模式与高级节点

- QA会签1114：审批节点「候选人员」先搜 admin 勾选、再搜 test33 勾选，确认后只剩 test33 → 修复 UserSelectModal；浏览器复测「已选 2 人：超级管理员、test33」，字段为「超级管理员、test33」✓。
  `npx vitest run src/components/common/__tests__/UserSelectModal.spec.js` 通过（还原修复后失败）。
- QA会签1114（候选人员 admin+test33，并行会签·全部通过）发起报 nrOfInstances → 修复后发布，发起生成 admin、test33 两条任务 ✓；admin 同意后 test33 仍待办 ✓；
  改派 test33 任务给 admin 并同意后实例完成 ✓。plugin-flow 221 通过、flow-designer 58 文件 495 用例通过。
- QA角色会签1114（候选角色 租户管理员，任一通过）同样报错 → 修复后发起实例卡住无任务（角色无成员）→ 发起即提示「没有可审批人员：ZFMANA」✓；发起测试失败不再弹全局对话框 ✓。
- QA角色会签1114 改用流程用户组「测试」（成员 姚民栋、姚民梁），任一通过：发起生成两条任务 ✓；改派其一给 admin 同意后实例完成、另一条状态 5 ✓；
  「我发起的」原显示已取消 → 修复后显示「审批人 已通过 超级管理员」✓。顺序会签：只先生成 admin 任务，同意后才生成 test33 任务 ✓。
- QA表单条件1114（发起表单=报销申请表单，分支1 `${amount > 1000}`）：填报 2000 发起进入「分支1审批」✓。
- QA调用活动1114：发布被拒「暂不支持的执行类型 [callactivity]」（服务端白名单）→ 设计器禁用 4 类高级节点并提示 ✓；flow-designer 495 用例通过。

## 第三轮：审批人类型与节点权限

- QA审批人类型1114（自选审批·会签 → 上级审批 → 主管审批）：自选两人只生成 assignee="1,70" 一条任务 → 修复（Flow 739590e3a）后生成 admin、test33 两条 ✓；
  会签完成后进入上级审批报 Unknown property ${initiatorLeader} → 修复（Flow f48efeed5）后发起即提示「发起人未设置上级领导，无法确定审批节点「上级审批」的审批人」✓。plugin-flow 226 通过。
- QA节点权限1114（报销申请表单；权限审批：关驳回/关转办/开终结/开退回发起人/审批意见选填；金额只读、说明隐藏）：详情按钮为 同意/退回发起人修改/终结 ✓，金额只读、说明不显示、意见可选 ✓；
  列表快捷驳回原先提交后才被拒 → 修复后点击即提示「当前节点不允许驳回」并关闭 ✓；快捷同意提示「需要填写节点表单」✓；详情内同意后实例完成 ✓。
  `npx vitest run src/views/flow` 58、flow-designer 495、BusinessFlowFormAssetSelect 2 通过。

## 第四轮：审批扩展（要点 / 签名 / 退回重提 / 终结 / 批量）

- 新建「QA审批扩展1115」：指定人员 admin；必审要点「已核对金额与附件齐全」；开退回发起人修改 / 允许终止 / 强制签名。
  设计器权限开关点不亮 → 修复 PermissionConfig（嵌套 computed v-model）后开关可亮，发布「部署成功」✓。
  开启退回发起人后画布自动出现「发起人修改」节点与驳回网关 ✓。
- 发起测试成功；快捷同意预检提示需签名 ✓；详情展示审批要点（必审 0/1）与手写签名 ✓，未勾选/未签名时同意被拦 ✓。
- 退回发起人修改（需签名）成功，待办节点变为「发起人修改」✓；发起人「同意」重提后回到「要点签名审批」✓；勾选要点 + 手写签名后同意，流程完成（待办 20→19）✓。
- 再发起「终结测试」实例，详情点终结 →「流程已终结」✓。
- 批量：FlowTaskCardList 原先无法勾选 → 补卡片勾选框后可选「已选 N 项」并出现批量同意/驳回 ✓；批量同意 2 条需表单任务返回「2 条任务未处理」✓（策略跳过符合预期）。
- `npx vitest run src/components/flow-designer/panel/__tests__/PermissionConfig.spec.js` 通过。
- 演示发起测试偶发 ECONNABORTED（既有观察）；签名需 Pointer/CDP 鼠标事件，页面 mouse API 不一定写入 SignaturePad 墨迹。

## 第五轮：SPEL / 通知 / 业务动作 / H5 复测 / 模板冒烟（2026-11-15）

- SPEL：新建「QASPEL1115」，审批人=SPEL 模板「发起人」(`${initiator}`) → 发布部署成功；API 发起成功，待办 assignee=1，同意通过 ✓。
  站内信 id=517「您有一个待办任务需要处理：qaspel1115流程」（FLOW_TODO）✓。
  「QASPELLEADER1115」用 initiatorLeader 发起被拒：「发起人未设置上级领导，无法确定审批节点「SPEL审批」的审批人」✓（与既有上级 SPEL 行为一致）。
- 业务流程动作（费用报销E2E / apply_kdcp，临时开写库）：
  - 空动作节点随 V1 发布 → 运行失败「动作节点没有配置有效的字段赋值」（action_mwh9x5dn_1）→ 删空节点后重发。
  - 「发送站内信」(FLOW_TODO_CARD)：记录 5 执行成功，「消息已入队」✓；但站内信正文未渲染 `${taskTitle}` → findings #53。
  - 「更新补充说明」(UPDATE_RECORD remark=常量)：记录 6 执行成功，H5 详情补充说明变为「QA动作节点已执行」✓。
  - 「更新状态」场景模板预填 status 与对象 flowStatus 不匹配 → findings #52。
  - 应用发布至 v5；测完写库已关回 false。
- H5：会签「QA会签1114」发起后待办可开详情并同意，会签待办清零 ✓。
  发起人自选：PC「发起测试」校验「请选择「自选审批」的审批人」并展开选人器 ✓；该模型后续还有「上级审批」，无上级时仍会按既有规则拒绝发起。
- 冒烟：设计器「AI辅助」可打开 AI 流程助手面板 ✓；`/flow/template` 路由 403（接口有 4 条）→ #54；`/print/templates` 403 → #55。
- 本地仍有未提交修复：PermissionConfig.vue、FlowTaskCardList.vue（第四轮）；未部署演示。

## 第六轮：修复 #52/#53/空动作拦截并回归（2026-11-15）

- 修复「更新状态」模板默认字段 `status` → `flowStatus`（`node-templates.js` + `suggestedStatusField`）；默认 ACTION 带空 fieldMappings 行便于提示。
- 前后端拦截空 UPDATE/CREATE 动作：`ACTION_FIELD_MAPPING_REQUIRED`；发布前 `business-process.[processId].vue` 先跑客户端图校验。
- 发送消息补齐模板参数 `taskTitle/processName/processCode/startUserName`（`BusinessProcessMessageExecutor`）。
- 单测：`vitest` business-process-designer 63 + workspace 20 通过；`BusinessProcessSchemaValidatorTest` 15、`BusinessProcessMessageExecutorTest` 2 通过（`-Penable-tests`）。
- 浏览器（localhost:3000 → 演示 API，QA动作节点1115）：
  - 选「更新状态」后 `localConfig.fieldMappings[0].field === 'flowStatus'` ✓
  - 清空字段赋值 →「流程问题 1」`ACTION_FIELD_MAPPING_REQUIRED` ✓；再套模板恢复 issues=0 ✓
  - 演示旧包「检查流程」仍提示通过（#53/#56 服务端待部署）；未点确认发布。
- #54/#55 仍为账号菜单 ACL 观察，未改。
- 待授权：中文提交 + 部署 Admin（含第四轮 PermissionConfig / FlowTaskCardList）。

## 第七轮：检查流程误报 + 发布门禁复测（2026-11-15）

- 修复：`handleValidate` 原先忽略设计器传入的客户端校验结果，旧服务端返回 valid 时仍 toast「流程检查通过」。
  现合并 `Math.max(clientErrorCount, serverErrorCount)`；客户端有错即 warning。
- 单测：`business-process-workspace` 新增「client fail 不报通过」；合计 84 通过。
- 浏览器（QA动作节点1115）：
  - 清空字段赋值 → 流程问题 1 ✓
  - 「检查流程」toast「流程检查发现 1 项错误」（不再误报通过）✓
  - 「发布」不弹确认框，直接「流程检查发现 1 项错误，请修正后再发布」✓
  - 套回「更新状态」模板后 issues=0 ✓
- 待办批量：卡片勾选出现「已选 2 项 / 驳回 / 同意 / 清空」✓（第四轮 FlowTaskCardList）
- #54/#55 复测：`/flow/template`、`/print/templates` → `/403` 禁止访问（观察不变）；`/flow/model`、`/flow/todo` 可进。

## 第八轮：支撑配置 + CREATE_RECORD + 用户组选人修复（2026-11-15）

- 流程分类：管理树含停用项（9→10→9）；新增/删除「QA分类1115」✓（删除后需点查询才刷新计数，体验可后续优化）。
- 流程表达式：列表可加载「发起人 / 发起人上级」等；首屏异步组件未就绪时可能短暂「共0条」，搜索后正常。
- 流程用户组：列表「测试」2 人；维护成员已选摘要「姚民栋、姚民梁」✓，但打开后表格长时间无数据 → #57 修复；修复后并行加载可出 admin 等用户 ✓。
- 常用审批意见：范围占位「企业常用（默认）」✓；列表 6 条无重复「同意」；新增/删除冒烟（列表偶发未即时刷新）。
- `/flow/conditionRule` → 403 → #58 观察。
- CREATE_RECORD 场景模板：`actionType=CREATE_RECORD`、`objectCode=apply_kdcp`、带空 fieldMappings 行 ✓（需补字段，否则会被 #56 拦截）。
- 运行记录：可见成功/失败历史（含空动作失败记录）；监控页可进、有分布统计，当前筛选实例列表为空。
- 待办：19 项，快捷同意/驳回与勾选框仍在 ✓。
- `vitest` UserSelectModal 2 通过。

## 第九轮：会签显隐 / 发起测试校验文案 / 版本预选 / 页签复用（2026-11-15）

- #59：`ApproverConfig` 单人指定不展示「多人审批方式」，候选人员仍展示；切回单人重置会签。`ApproverConfig.spec` 5 通过。
- #60：`formatStartTestValidationMessage` 提取必填文案；`startTestValidation.spec` 2 通过。
- #61：`versionCompare` 加载后预选最新两版；`versionCompare.preselect.spec` 1 通过。
- #62：`tab-guard` + `App.resolveRouteViewKey` 对流程任务列表按 path 复用；`tab-guard.spec` 1 通过。
  浏览器：`/flow/started?title=A` → `?title=B` 后 sessionStorage 仅 1 个 key=`/flow/started`、path 更新为 B ✓。
- 本轮相关 vitest 合计 9 通过。未部署；设计器画布节点点选冒烟未稳定点开属性面板（单测已覆盖会签显隐）。

## 部署 Admin 0521d689b 与复测（2026-11-15）

- 提交：`0521d689b`（`fix: 业务流程校验与发起测试体验修复`）；构建 `-Dforge.build.commit=0521d689b…`。
- 上传 `~/forge-upload/20261115-admin-0521d689b`，`deploy.sh` 仅替换 admin；镜像 `forge-admin-server:1.2.1-0521d689b-fonts`，字体库 6/6，healthy；Flyway `forge_admin_new` up to date；Scheduler started 0。
- 版本接口：`/system/version` → build.commit=`0521d689b27f8977e000f9f56e4b9fe9f9c27e6d`，time=`2026-11-15T10:01:55.438Z`。
- 包内校验：`forge-plugin-generator` 含 `BusinessProcessMessageExecutor`（`taskTitle`/`processName`）与 `ACTION_FIELD_MAPPING_REQUIRED`。
- #56 复测（QA动作节点1115）：清空/空动作后「检查流程」→ toast「流程检查发现 1 项错误」，流程问题含 `ACTION_FIELD_MAPPING_REQUIRED` ✓。
- #61 复测（QA会签1114 版本对比）：下拉预选 `v3`、`v2` ✓。
- #62 此前浏览器已确认页签复用 ✓。
- #59/#60：单测已覆盖；本轮浏览器设计器属性面板点选 / 带必填表单的发起测试弹窗未稳定打开，保留单测证据。
- #53：部署包已含参数补齐；真实站内信触发正文复验可下次补做。
- 未 push remote；本地 `main` ahead 1。
