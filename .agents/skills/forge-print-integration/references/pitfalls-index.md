# Print Pitfalls Index

Open matching titles in `code-copilot/memory/pitfalls/{frontend,backend,lowcode}.md`.

## Frontend

- 打印客户端 PDF 不能用 html2canvas 重排 flex 表格
- 打印表头不透明底会盖住表格外框上/左边
- 打印表格选中格不能用 !important 盖住表头底色
- 打印示例图不能用 SVG data URL
- 打印字体栈不能只校验第一个名字
- 打印取色器会写出协议不认的颜色格式
- 打印横竖线不能用 SVG viewBox 描边
- 打印毫米字段不要用 NInputNumber
- 打印预览新标签不能 router.back
- 打印模板必须跟页面走，设计器不能回到 /print

## Backend

- 打印关系外键 businessObject0eq3Id 对不上是设计器列名+model_schema 漏字段
- 打印执行事件新增枚举必须同步 Jackson/DTO/Mapper/字典
- 打印 style 不能用封闭白名单拦展示属性

## Lowcode

- 应用发布不能因打印模板字段目录阻断
