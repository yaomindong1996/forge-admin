# forge-form-designer

Forge 原生表单设计器模块，负责替代 FormCreate 画布的主编辑体验。

- `ForgeFormDesigner.vue`: 三栏主编排，负责字段库、画布、属性面板之间的数据同步。
- `ForgeFieldShelf.vue`: 左侧字段和布局组件入口，负责搜索、分组、点击添加和拖拽 payload。
- `ForgeFormCanvas.vue`: 中间画布，负责根级 grid、空画布和根落点。
- `ForgeFormCanvasNode.vue`: 递归节点渲染，负责组件选择、容器投放、排序和节点快捷操作。
- `ForgePropertyPanel.vue`: 右侧属性面板，负责表单、字段、布局组件的常用属性编辑。
- `designerLayoutFactory.js`: 设计态布局组件工厂，集中生成 row、card、tabs、collapse 等结构。

Schema 的查找、插入、移动、复制、删除和属性更新等纯函数放在 `../form-first/formDesignerSchema.js`，组件内不要重复实现组件树路径逻辑。
