# 低代码报销审批全流程 E2E 执行记录（2026-11-13）

## 环境

- 后端与数据：演示环境（NAS 网关 `https://zebbon2.myds.me:51890`，Admin / App(H5) / Flow 服务）。
- 前端：本机 `forge-admin-ui`（:3000）与 `forge-h5-ui`（:3009）开发服务，代理到上述网关。
  原因：演示站静态资源 `81.70.22.48:8084` 实测约 2~3 KB/s，无法正常加载页面。
- 浏览器：ego-browser；账号 admin，默认租户。
- 发布状态：本轮修复**仅在本地**，未提交、未部署演示环境。

## 流程结果

| 步骤 | 结果 |
|------|------|
| 创建应用「费用报销E2E」(`hr_e2_e`) | 通过 |
| 表单页「报销申请」5 字段 + 必填 + 下拉选项，落表 `hr_apply_kdcp` | 通过（需临时开启数据源写入/建表） |
| 业务流程「报销审批」+ 审批模型「报销申请审批」(审批人 admin) | 通过，发布 V1 |
| 应用发布 v1 | 通过（2 项提醒） |
| H5 新建报销并提交审批 | 通过（需补对象主流程绑定 + 单据配置，见遗留 1） |
| H5 审批同意 | 通过，两轮记录（id 1、2）均「已通过」 |
| H5 我发起的 / 审批详情查看结果 | 通过 |

## 已修复（本地）

| 问题 | 修改 |
|------|------|
| 应用列表每页约 469KB，频繁超过前端 12s 超时 | `BusinessApplicationMapper.xml` 分页改用摘要查询，不返回 `options/portalConfig/aiAssistantConfig` |
| 创建应用后列表刷新超时会导致进不了新应用 | `app-center/index.vue` 先进入应用，列表后台刷新 |
| `/generator/datasource/enabled` 下发 JDBC 地址、账号、密码字段 | `GenDatasourceController` 出口脱敏（拷贝后清空） |
| 建页弹窗默认选不可写数据源，设计完保存才失败 | `PageTypeSelector.vue` 按后端同一规则标注/禁选并提前拦截 |
| 应用内审批模型设计器按 Esc 直接关闭丢失未保存内容 | 两处宿主弹窗补 `:close-on-esc="false"`（与流程中心一致） |
| 弹窗内发布业务流程后列表仍显示草稿/未发布 | 宿主派发 `forge:business-process-changed`，流程列表面板重载 |
| H5 表单分区引用旧字段编码导致表单空白 | H5 `completePageSectionFields` 补齐未引用字段；后端 `TaskFormUiDocumentCompiler` 同规则补齐待办表单分区 |
| H5 日期选择器空值时停在 1970 年 | `AiDateTimePicker` 默认定位到今天 |
| H5 审批后返回单据页状态不刷新 | `lowcode-runtime.vue` onShow 重载详情 |
| H5 所有任务都显示「紧急」 | 优先级换算与管理端一致（Flowable 默认 50 为普通） |
| H5「我发起的」只显示已结束 | 显示已通过/已驳回/已撤回/已终止，标签改「最后处理」 |
| 应用发布页移动端无地址时仍画空二维码 | `AppPublishAccess.vue` 无地址时给出获取指引 |
| 未读消息角标请求失败弹全局错误框 | `getUnreadCount` 设 `needTip: false`（调用方已兜底） |

## 测试

- 后端（`-Penable-tests -am`）：TaskFormUiDocumentCompilerTest 4、GenDatasourceControllerTest 2、
  BusinessApplicationMapperTest 4、BusinessApplicationServiceTest 23、GenDatasourceServiceImplDeleteGuardTest 3，全部通过。
- 管理端 vitest `src/views/app-center src/api`：499 通过，4 失败；撤下本轮改动后同样 4 失败（存量）。
- H5 node:test：190 通过，2 失败（`mobile-field-contract`，存量，与本轮无关）；新增 8 条全部通过。
- H5 未安装 eslint，lint 跳过；管理端改动行无新增 lint 报错。

## 遗留（需决策 / 需后端排查）

1. **低代码应用缺少 H5 发起入口**：H5 只支持对象主流程 `START_FLOW`，业务流程 `START_PROCESS` 提示「请在 PC 端发起」；
   低代码应用没有可见入口配置对象主流程绑定与单据配置（`BusinessFlowBindingPanel` 仅代码应用可见，`ApplicationFlowBindingBar` 未挂载）。
   本轮经平台接口补配后跑通，需产品确认补哪种入口。
2. 演示数据源 183 已按约定还原为不可写；`费用报销E2E` 应用在演示环境将无法再保存新单据。
3. Flow 历史接口审批人姓名返回用户 ID「1」；节点进度「结束」排在「审批人」前。需查演示 Flow 服务版本与日志。
4. 设计器属性面板改字段编码/新增字段后 `pageSections` 未同步（本轮已在运行侧兜底，根因未改）。
5. 发布检查提示页面「aa」引用失效业务对象，非本轮创建，待确认来源。
6. 字段编码默认 `fieldInput` 等，未随显示名称生成（建议仅对未落库字段联动）。
7. 审批标题模板默认「${starterName}发起的…审批单」，同类单据标题相同，建议默认带业务字段。
