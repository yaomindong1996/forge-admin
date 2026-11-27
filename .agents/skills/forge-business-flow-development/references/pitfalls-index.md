# Flow Pitfalls Index (read titles only, then open matching section)

Source: `code-copilot/memory/pitfalls/flow.md`. Do not load the whole file unless debugging that title.

## Task form / schema

- 审批表单字段目录不能只信发布态 editSchema
- 审批子表列控件类型不能只依赖发布态 masterDetailConfig
- 审批子表只读列能改却保存报「不允许编辑子表字段」
- 审批/主从子表下拉人员部门开关失效与窄列
- 明细页下拉正常但子表引用后失效：optionSource 没回写字段注册表
- 代码表单 Provider 不能依赖低代码运行配置 configKey
- AiForm 字段权限必须由组件和调用端共同接入

## Start / objectCode / binding

- 动态 CRUD 事件不能把运行配置对象码当作流程标准对象码
- 单据详情运行态也要归一 objectCode/configKey
- 低代码 START_FLOW 不能同时走 custom-action 和内置发起
- Flow 服务 sys_flow_business 也必须做 businessKey 幂等
- Flowable 节点表达式变量必须由低代码映射提供

## Assignees / BPMN

- Flowable 固定审批人不能保存为用户变量表达式
- sys_flow_task.assignee 必须存用户 ID
- 前端禁止把雪花 Long ID 转成 Number
- 示例流程初始化覆盖用户 BPMN 节点配置
- BPMN XML属性值带前导空格导致匹配失败
- 同一流程实例重复待办优先检查 BPMN 重复 sequenceFlow

## Status / events / notify

- 超时 auto_pass/auto_reject 不能只 complete，且 DELETED 会覆盖终态
- 流程完成事件必须携带完整变量快照
- 流程待办消息完成后必须自动置已读
- Flowable 7 流程取消事件不能强转 FlowableEntityEvent
