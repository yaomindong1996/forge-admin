# 低代码表单移动扫码与动态字段实施计划

**目标：** 在现有字段事件、协同容器扫码、子表动作和事务命令基础上形成可发布、可运行的通用能力。

**架构：** 继续复用 `AiForm`/`AiFormItem`、`ChildTableEditor`、统一查询源网关和 BusinessAction 发布快照。浏览器只提交结构化值和受控上下文，服务端重新读取权威记录并在主数据源事务内执行。

**执行顺序：** 先修复发布快照中的动态显示，再实现扫码字段和普通 H5 适配，随后统一子表字段事件/选项源，最后补事务门禁并做增量验证。

## 文件边界

- 前端规则：`runtime-rules.js`、`RuntimeRulesEditor.vue`、`AiForm.vue`、`AiFormItem.vue`。
- 设计器：`formDesignerSchema.js`、`ForgeFieldShelf.vue`、`designerLayoutFactory.js`、`ForgePropertyPanel.vue`。
- 子表：`ChildTableEditor.vue`。
- 后端发布：`BusinessObjectDesignerService.java`、`LowcodeRuntimeConfigBuilder.java`。
- 事务：`BusinessActionCommandPolicy.java`、`BusinessActionExecutionService.java`、动作步骤执行器及 Mapper XML。
- 测试与记录：本目录下 `test-spec.md`、`execution-log.md` 以及对应前后端测试文件。
