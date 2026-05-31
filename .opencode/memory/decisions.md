# Forge项目决策记录

> 记录项目级架构和产品决策，避免后续变更重复讨论。

## 1. 低代码应用与代码生成统一主链路

**记录日期**: 2026-05-25

低代码应用、AI 应用生成和代码生成统一收敛到“应用管理/应用开发”主入口。用户从需求描述生成模型和应用草稿后，必须确认才保存；应用级代码预览和 ZIP 下载默认使用已保存草稿，发布版本作为可选来源。

模型管理继续保留独立入口，因为模型是领域资产，可以不参与应用设计。模型导入直接读取数据源表结构生成 `ai_lowcode_model.model_schema`，不从旧 `GenTable` 选择；旧 `GenTable` 数据保留但新流程忽略。

数据源管理保留为开发者菜单；模板管理菜单去掉。首期代码生成支持单表/单主模型，主子表、左树右表和树形单表作为后续扩展。

## 2. AI Agent 配置来源

**记录日期**: 2026-05-25

Forge 的 AI Agent 角色提示词必须优先配置在 `ai_agent.system_prompt`，可维护上下文、输出协议和规则必须放在 `ai_context_config`，代码只传 `agentCode`、用户输入和运行时变量。低代码业务系统生成使用 `lowcode_system_generator`，禁止把完整业务 Prompt 长期硬编码在 Java Service 中；Java 里只保留规则降级和协议归一化逻辑。

## 3. 表单优先业务对象设计器使用 fcDesigner 作为首期画布

**记录日期**: 2026-05-31

低代码业务对象设计器后续主链路调整为“表单优先”：普通用户默认先设计最终表单，平台再维护字段注册表、视图投影、级联规则和运行态配置。

首期不从零自研完整表单画布，优先复用系统已集成的 `fcDesigner` / form-create 能力。`fcDesigner` 负责拖拽画布、组件排序、基础属性编辑和预览，Forge 负责业务组件适配、字段绑定、FormDesignerSchema、ViewSchema、LinkageSchema、发布检查和运行态编译。

form-create rule/options 只作为设计器可编辑表示，不能成为 Forge 运行时唯一事实来源。保存和发布必须通过 Forge Adapter 转换为 `FormDesignerSchema + FieldRegistry + ViewSchema + LinkageSchema`，发布运行态继续编译到 `AiCrudPage`、`AiForm`、`DynamicCrudController` 和 `LowcodeRuntimeConfigBuilder`。
