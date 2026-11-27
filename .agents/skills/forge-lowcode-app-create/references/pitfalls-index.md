# Lowcode Pitfalls Index (creation / design / publish)

Source: `code-copilot/memory/pitfalls/lowcode.md`. Open only the matching section.

## Catalog / form design

- 表单设计器把子表列写进主对象字段目录会重复堆积
- 子表字段不能同时进主表单和主表字段目录
- 表单字段资产未使用列表不能只从当前画布抽字段
- form-create 默认字段名不能沉淀为低代码业务字段编码
- fcDesigner 布局组件不能当业务字段处理
- 表单发布检查必须展平 row/col 子组件

## optionSource / subtable / AiForm

- 明细页下拉正常但子表引用后失效：optionSource 没回写字段注册表（also in flow pitfalls）
- 业务记录选择器空 DTO 导致“选择器缺少业务对象编码”
- AiForm 树选择必填校验误判已选 ID 为空
- Naive Select/TreeSelect 回显 Long ID 必须统一字符串类型

## Publish / DDL / runtime

- 应用协调发布应优先做状态推进，不要每次重跑对象发布
- 导入已有表新增不自动填充审计字段
- 低代码动作种子只有 designer_options 没有发布版本时运行必然失败
- GET render / designPreview 不能在长事务里写关系表
- Flyway `${}` 消息/编码模板占位（see db-flyway / lowcode seed notes）
