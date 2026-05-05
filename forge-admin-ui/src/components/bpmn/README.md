# BPMN 流程设计器组件

## 组件说明

### FlowModeler.vue

BPMN 流程设计器核心组件，基于 bpmn-js 实现。

## 常见错误及解决方案

### 1. XMLException: 元素 'sequenceFlow' 中必须包含属性 'targetRef'

**错误原因**:

- 用户创建了连接线（SequenceFlow）但没有连接到目标节点
- 连接线只有起点，没有终点
- 或者连接线被意外删除了目标节点

**解决方案**:

1. **删除未完成的连接线**: 在画布上选中悬空的连接线，按 Delete 键删除
2. **完成连接**: 将连接线拖拽到目标节点上完成连接
3. **使用验证功能**: 点击工具栏的"验证流程"按钮，系统会自动检查并提示错误

**预防措施**:

- 系统已添加自动验证功能
- 保存/导出前会自动检查流程图完整性
- 如果发现错误，会提示具体的问题位置

### 2. 流程图必须包含开始节点和结束节点

**错误原因**:

- 流程图中缺少开始事件（StartEvent）
- 流程图中缺少结束事件（EndEvent）

**解决方案**:

1. 从左侧工具面板拖拽"开始事件"到画布
2. 从左侧工具面板拖拽"结束事件"到画布
3. 确保流程有明确的开始和结束

## 验证功能

### 自动验证

系统在以下操作时会自动验证流程图：

- 保存草稿
- 发布部署
- 导出 BPMN
- 导出 SVG

### 手动验证

点击工具栏的"验证流程"按钮，可以手动触发验证。

### 验证规则

1. **连接线完整性**: 所有连接线必须有起点和终点
2. **开始节点**: 流程图必须包含至少一个开始节点
3. **结束节点**: 流程图必须包含至少一个结束节点

## API

### Props

```typescript
{
  xml: string // BPMN XML 字符串
  readOnly: boolean // 是否只读模式
}
```

### Events

```typescript
{
  save: (xml: string) => void      // 保存事件
  change: (xml: string) => void    // 变更事件
  ready: (modeler: BpmnModeler) => void  // 就绪事件
}
```

### Methods

```typescript
{
  getXML(skipValidation?: boolean): Promise<string | null>  // 获取 XML，默认会验证
  setXML(xml: string): Promise<void>                        // 设置 XML
  modeler(): BpmnModeler                                    // 获取 modeler 实例
}
```

## 使用示例

```vue
<template>
  <FlowModeler
    ref="modelerRef"
    :xml="bpmnXml"
    @change="handleBpmnChange"
    @ready="handleModelerReady"
  />
</template>

<script setup>
import { ref } from 'vue'
import FlowModeler from '@/components/bpmn/FlowModeler.vue'

const modelerRef = ref(null)
const bpmnXml = ref('')

async function handleSave() {
  // 获取 XML（会自动验证）
  const xml = await modelerRef.value?.getXML()

  if (!xml) {
    console.error('流程图验证失败')
    return
  }

  // 保存 XML
  console.log('保存成功:', xml)
}

// 如果需要跳过验证（不推荐）
async function handleSaveWithoutValidation() {
  const xml = await modelerRef.value?.getXML(true)
  console.log('保存成功:', xml)
}

function handleBpmnChange(xml) {
  bpmnXml.value = xml
}

function handleModelerReady(modeler) {
  console.log('Modeler 已就绪:', modeler)
}
</script>
```

## 设计系统

组件遵循 Forge Flow Designer 设计系统：

- **风格**: Flat Design
- **主色**: #6366F1 (Indigo 500)
- **字体**: Fira Sans
- **详细规范**: 参见 `/design-system/MASTER.md`

## 快捷键

- `Ctrl + Z`: 撤销
- `Ctrl + Y`: 重做
- `Delete`: 删除选中元素
- `Ctrl + C`: 复制
- `Ctrl + V`: 粘贴
- `Ctrl + A`: 全选

## 工具栏功能

1. **撤销/重做**: 撤销或重做上一步操作
2. **缩放控制**: 放大、缩小、适应屏幕
3. **导入/导出**: 导入 BPMN 文件、导出 BPMN/SVG
4. **预览 XML**: 查看当前流程的 XML 代码
5. **验证流程**: 手动验证流程图完整性
6. **自动布局**: 自动优化节点布局（使用 dagre 算法）
7. **暗色模式**: 切换亮色/暗色主题

### 自动布局功能

自动布局使用 dagre 图布局算法，可以自动排列流程图中的所有节点：

**特性**:

- 从左到右的流程布局
- 自动计算节点间距和层级间距
- 保持连接线的逻辑关系
- 自动适应视口

**使用方法**:

1. 点击工具栏的"自动布局"按钮（魔法棒图标）
2. 系统会自动重新排列所有节点
3. 布局完成后自动适应屏幕

**注意事项**:

- 自动布局会移动所有节点到新位置
- 建议在添加多个节点后使用
- 可以使用撤销功能恢复原布局

### 小地图功能

小地图显示在画布右下角，提供流程图的缩略视图：

**特性**:

- 实时显示整个流程图
- 显示当前视口位置
- 支持点击拖拽快速导航
- 可折叠/展开

**使用方法**:

1. 小地图默认展开显示
2. 点击关闭按钮可折叠小地图
3. 点击地图图标可重新展开
4. 在小地图上点击或拖拽可快速移动视口

**视觉设计**:

- 符合 Flat Design 风格
- 使用 Indigo 主题色
- 渐变标题栏
- 平滑过渡动画

## 注意事项

1. **保存前验证**: 始终在保存前验证流程图，避免保存无效的流程
2. **完整连接**: 确保所有连接线都连接到目标节点
3. **必需节点**: 流程图必须包含开始节点和结束节点
4. **命名规范**: 为节点和连接线添加有意义的名称，便于维护
5. **定期保存**: 定期保存草稿，避免数据丢失

## 故障排查

### 问题：保存时提示验证失败

**解决**:

1. 点击"验证流程"按钮查看具体错误
2. 根据错误提示修复问题
3. 重新保存

### 问题：连接线无法连接到节点

**解决**:

1. 确保目标节点类型支持该连接
2. 尝试重新创建连接线
3. 检查节点是否被锁定

### 问题：导出的 XML 无法导入

**解决**:

1. 检查 XML 格式是否正确
2. 确保包含 BPMNDiagram 图形信息
3. 使用"预览 XML"功能检查 XML 结构

## 技术栈

- **bpmn-js**: BPMN 2.0 建模工具
- **Vue 3**: 前端框架
- **Naive UI**: UI 组件库
- **Flowable**: 流程引擎（后端）
