# H5 办公基础能力 Tasks

> 依赖：spec.md 第 9 章决策已确认。每个任务完成后执行 test-spec.md 对应检查，并追加到 execution-log.md。

## 阶段一：后端

- [x] T1 发起目录接口
  - 新建 `BusinessStartableObjectVO`、`BusinessStartableObjectMapper` + XML，在 `BusinessFlowController` 增加 `GET /startable-objects`（`@ApiPermissionIgnore`）。
  - Service 方法放在独立的 `BusinessStartableObjectService`，不往 `BusinessFlowService` 里堆。
- [x] T2 后端测试：`BusinessStartableObjectMapperContractTest`、VO 字段白名单。（已编写，本机无 JDK 未执行，见 execution-log）

## 阶段二：H5 基础

- [x] T3 权限：`authStore.permissions` + `hasPermission()`（`utils/permission.js` 纯函数并单测）；沿用现有 `fetchAccessSnapshot` 加载和清空。
- [x] T4 API：`api/index.js` 新增第 5.3、6 节共 10 个方法。
- [x] T5 角标：`badgeStore` 增加 `noticeUnreadCount`；`AiTabBar` 消息角标改为两者之和；补单测。

## 阶段三：公告

- [x] T6 公告列表 `pages/notice/index` 和详情 `pages/notice/detail`；`pages.json` 注册。
- [x] T7 消息页顶部公告入口；工作台最新公告条。

## 阶段四：发起审批

- [x] T8 发起页 `pages/approval/start`：`utils/startable-objects.js`（取交集、分组、搜索）并单测；工作台和待办页入口。
- [x] T9 自选审批人：`utils/initiator-select.js` 并单测；`store/modules/initiatorSelect.js` 并单测；`components/flow/InitiatorSelectSheet.vue`（复用通讯录接口和 `ContactAvatar`）。
- [x] T10 单据审批动作：`utils/document-flow-actions.js`（动作映射）并单测；`store/modules/documentFlow.js` 并单测；`composables/lowcode/useLowcodeDocumentFlow.js`；运行页接入状态卡和底部按钮，`create` 模式"提交审批"先保存再发起。
  - `lowcode-runtime.vue` 576 → 613 行（上限 640）。

## 阶段五：收尾

- [x] T11 契约测试：按 spec 7.2 追加断言，已有断言不改；`feedback-components.test.js` 页面清单补上三个新页面。
- [ ] T12 全量验证：`node --test`、`build:h5`、本地预览截图已完成；**后端编译和测试待有 JDK 的环境执行**，真实接口联调待用户确认。
