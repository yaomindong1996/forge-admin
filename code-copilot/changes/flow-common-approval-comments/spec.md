# 常用审批意见 Spec

> 变更名：`flow-common-approval-comments`  
> 状态：`implemented-pending-e2e`  
> 创建日期：2026-09-07  
> 实施方式：平台级通用能力，不绑定单一业务流程

## 1. 背景

待办快捷审批已有写死的意见芯片（同意 / 已阅 / 情况属实，驳回 / 请补充材料 / 请修改后重提），详情审批框、请假/采购任务表单和 H5 待办都还要手工输入。意见不能按人维护，也不能由流程管理员下发企业常用语。

## 2. 目标

1. 审批时可以一键点选常用意见，填入当前意见框，仍允许继续编辑。
2. 当前用户可以管理自己的常用意见（新增、删除、存当前意见）。
3. 流程管理员可以在流程管理中维护企业常用意见（启用/停用、场景、排序）。
4. 企业启用意见 + 个人意见在审批框中一起展示；个人优先。

## 3. 非目标

- 不改 Flowable 引擎、任务完成协议或 `sys_flow_comment` 历史意见表。
- 不把意见模板做成流程节点配置，也不按流程模型隔离。
- 不在本变更做意见附件、多语言、按部门共享。
- 不自动启动真实 Admin/Flow 服务做联调（由用户按偏好自行验收）。

## 4. 数据模型

表 `sys_flow_comment_phrase`：

| 字段 | 说明 |
|---|---|
| id | 数值主键，自增 |
| tenant_id | 租户 |
| owner_type | `0` 企业常用，`1` 我的常用 |
| user_id | 企业记录固定 `0`；个人记录为当前用户 ID |
| scene | `APPROVE` / `REJECT` / `ALL` |
| content | 意见正文，1–200 字 |
| sort_order | 同范围内排序，越小越靠前 |
| status | `EnableStatus`，`1` 启用 `0` 停用 |
| del_flag | 逻辑删除，删除后写主键 |
| 审计字段 | create_by/time/dept，update_by/time |

唯一约束：`(tenant_id, owner_type, user_id, scene, content, del_flag)`。删除后允许重建相同内容。

## 5. 接口

基础路径：`/api/flow/comment-phrases`

| 方法 | 路径 | 权限 | 行为 |
|---|---|---|---|
| GET | `/usable?scene=` | 登录即可 | 当前租户启用的企业意见 + 当前用户启用的个人意见；`scene` 为空返回全部，否则返回该场景 + `ALL` |
| GET | `/mine` | 登录即可 | 当前用户全部个人意见（含停用） |
| POST / PUT / DELETE | `/`、`/{id}` | 登录即可写自己的个人意见；企业意见需 `flow:comment-phrase:manage` | 写接口走 DTO |
| GET | `/page`、`/{id}` | `flow:comment-phrase:view` | 管理页分页；查企业意见需管理权限，查 `ownerType=USER` 只返回当前用户 |

服务层强制：

- 个人意见的 `user_id` 只能是当前登录用户，禁止读写他人记录。
- 企业意见 `user_id=0`，无管理权限时按不存在处理。
- 个人最多 30 条，企业最多 100 条。
- 查询 SQL 写在 Mapper XML，显式带 `tenant_id` 和 `del_flag = 0`。

## 6. 前端

- 流程管理页 `/flow/commentPhrase`：AiCrudPage 维护企业/个人常用意见。
- 公共组件 `FlowCommentPhraseInput`：意见输入框 + 芯片 + 存为常用 + 管理个人意见。
- 接入：待办详情、待办快捷同意/驳回、请假审批表单、采购单任务表单、H5 待办详情。
- 字典：`flow_comment_phrase_scene`、`flow_comment_phrase_owner_type`；状态复用 `sys_enable_disable`。
- 芯片样式沿用现有快捷审批芯片，不使用装饰色条或彩色图标块。

## 7. 验收

1. 审批框能点选企业种子意见和个人意见，点选后写入输入框。
2. 用户可把当前意见存为个人常用，并可在弹窗中删除自己的意见。
3. 流程管理员能在管理页新增/停用/删除企业意见，停用后审批框不再出现。
4. 不能看到或改其他用户的个人意见；无权限不能改企业意见。
5. 后端模块编译通过，前端组件单测和 lint 通过。
