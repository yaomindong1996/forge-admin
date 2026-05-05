<template>
  <div class="flow-modeler-container" :class="{ dark: darkMode }">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <n-space>
        <!-- 撤销/重做 -->
        <n-button-group>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" :disabled="!canUndo" @click="handleUndo">
                <i class="i-material-symbols:undo" />
              </n-button>
            </template>
            撤销 (Ctrl+Z)
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" :disabled="!canRedo" @click="handleRedo">
                <i class="i-material-symbols:redo" />
              </n-button>
            </template>
            重做 (Ctrl+Y)
          </n-tooltip>
        </n-button-group>

        <n-divider vertical />

        <!-- 缩放控制 -->
        <n-button-group>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" @click="handleZoomOut">
                <i class="i-material-symbols:remove" />
              </n-button>
            </template>
            缩小
          </n-tooltip>
          <n-button size="small" disabled style="min-width: 60px">
            {{ Math.round(zoomLevel * 100) }}%
          </n-button>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" @click="handleZoomIn">
                <i class="i-material-symbols:add" />
              </n-button>
            </template>
            放大
          </n-tooltip>
        </n-button-group>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button size="small" @click="handleZoomReset">
              <i class="i-material-symbols:fit-screen" />
            </n-button>
          </template>
          适应屏幕
        </n-tooltip>

        <n-divider vertical />

        <!-- 导入导出 -->
        <n-button-group>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" @click="handleImport">
                <i class="i-material-symbols:upload" />
              </n-button>
            </template>
            导入BPMN
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" @click="handleDownloadBpmn">
                <i class="i-material-symbols:download" />
              </n-button>
            </template>
            导出BPMN
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" @click="handleDownloadSvg">
                <i class="i-material-symbols:image" />
              </n-button>
            </template>
            导出SVG
          </n-tooltip>
        </n-button-group>

        <n-divider vertical />

        <!-- 预览 -->
        <n-button size="small" @click="handlePreview">
          <template #icon>
            <i class="i-material-symbols:preview" />
          </template>
          预览XML
        </n-button>

        <!-- 验证 -->
        <n-button size="small" @click="handleValidate">
          <template #icon>
            <i class="i-material-symbols:check-circle" />
          </template>
          验证流程
        </n-button>

        <n-divider vertical />

        <!-- 工具 -->
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button size="small" @click="handleAutoLayout">
              <i class="i-material-symbols:auto-awesome" />
            </n-button>
          </template>
          自动布局
        </n-tooltip>

        <n-tooltip trigger="hover">
          <template #trigger>
            <n-button size="small" @click="toggleDarkMode">
              <i :class="darkMode ? 'i-material-symbols:light-mode' : 'i-material-symbols:dark-mode'" />
            </n-button>
          </template>
          {{ darkMode ? '亮色模式' : '暗色模式' }}
        </n-tooltip>
      </n-space>
    </div>

    <!-- 画布区域 -->
    <div class="canvas-wrapper">
      <div ref="canvasRef" class="canvas-container" />

      <!-- 叠加层组件 -->
      <Minimap
        v-if="modeler"
        :modeler="modeler"
        :visible="true"
      />
      <ShortcutsBar :visible="true" />
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".bpmn,.bpmn20.xml,.xml"
      style="display: none"
      @change="handleFileChange"
    >

    <!-- XML预览弹窗 -->
    <n-modal v-model:show="showPreviewModal" preset="card" title="BPMN XML 预览" style="width: 900px; max-height: 80vh">
      <div class="xml-preview-container">
        <n-code :code="previewXml" language="xml" :show-line-numbers="true" :word-wrap="true" />
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="handleCopyXml">
            <template #icon>
              <i class="i-material-symbols:content-copy" />
            </template>
            复制XML
          </n-button>
          <n-button type="primary" @click="handleDownloadXmlFromPreview">
            <template #icon>
              <i class="i-material-symbols:download" />
            </template>
            下载XML
          </n-button>
          <n-button @click="showPreviewModal = false">
            关闭
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import autoLayout from './AutoLayout.js'
import CanvasBackground from './CanvasBackground.js'
import CustomRenderer from './CustomRenderer.js'
import flowableModdle from './flowable-moddle.json'
import Minimap from './Minimap.vue'
import ShortcutsBar from './ShortcutsBar.vue'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

const props = defineProps({
  xml: {
    type: String,
    default: '',
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['save', 'change', 'ready'])

// 使用全局 message 实例
const message = window.$message

// refs
const canvasRef = ref(null)
const fileInputRef = ref(null)

// modeler instance - 用 shallowRef 包裹，让模板可以响应式地检测初始化状态
const modelerRef = shallowRef(null)
// 是否初始化完成（用于模板 v-if 判断）
const isModelerReady = ref(false)
let modeler = null

// state
const canUndo = ref(false)
const canRedo = ref(false)
const zoomLevel = ref(1)
const selectedElement = ref(null)
const showPreviewModal = ref(false)
const previewXml = ref('')
const darkMode = ref(false)

// 默认 BPMN XML - 必须包含 BPMNDiagram 图形信息
const defaultXml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:flowable="http://flowable.org/bpmn"
                  targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="Process_1" name="新流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

// 检查 XML 是否包含图形信息
function hasDiagramInfo(xml) {
  return xml && (xml.includes('BPMNDiagram') || xml.includes('bpmndi:BPMNDiagram'))
}

// 初始化
onMounted(async () => {
  await nextTick()
  await initModeler()
})

onUnmounted(() => {
  if (modeler) {
    modeler.destroy()
  }
})

// 初始化 modeler
async function initModeler() {
  modeler = new BpmnModeler({
    container: canvasRef.value,
    keyboard: {
      bindTo: document,
    },
    moddleExtensions: {
      flowable: flowableModdle,
    },
    additionalModules: [
      CustomRenderer,
      CanvasBackground,
    ],
  })
  modelerRef.value = modeler
  isModelerReady.value = true

  // 监听事件
  const eventBus = modeler.get('eventBus')

  // 元素选择事件
  eventBus.on('selection.changed', (e) => {
    const { newSelection } = e
    if (newSelection.length === 1) {
      if (selectedElement.value && selectedElement.value.id === newSelection[0].id) {
        return
      }
      selectedElement.value = newSelection[0]
      // 不弹出 Modal——由父组件的停靠面板处理
      // 不显示快捷操作环——使用 bpmn-js 自带的 context pad
    }
    else {
      selectedElement.value = null
    }
  })

  // 元素变化事件
  eventBus.on('element.changed', () => {
    updateUndoRedoState()
    emitChange()
  })

  // 监听连接线创建事件，防止不完整的连接线残留
  eventBus.on('commandStack.connection.create.postExecuted', (event) => {
    const { context } = event
    if (context && context.connection) {
      const connection = context.connection
      const bo = connection.businessObject

      // 检查连接线是否完整
      if (!bo.sourceRef || !bo.targetRef) {
        console.warn('检测到不完整的连接线，立即清理:', connection.id)
        const modeling = modeler.get('modeling')
        modeling.removeElements([connection])
      }
    }
  })

  // 监听连接线更新事件
  eventBus.on('commandStack.connection.updateWaypoints.postExecuted', (event) => {
    const { context } = event
    if (context && context.connection) {
      const connection = context.connection
      const bo = connection.businessObject

      // 检查连接线是否完整
      if (!bo.sourceRef || !bo.targetRef) {
        console.warn('检测到不完整的连接线，立即清理:', connection.id)
        const modeling = modeler.get('modeling')
        modeling.removeElements([connection])
      }
    }
  })

  // 监听元素删除事件，删除后清理孤立的连接线
  eventBus.on('commandStack.shape.delete.postExecuted', (_event) => {
    // 延迟执行，确保删除操作完成
    setTimeout(() => {
      const cleanedCount = cleanIncompleteFlows()
      if (cleanedCount > 0) {
        console.warn(`删除节点后自动清理了 ${cleanedCount} 条孤立的连接线`)
      }
    }, 50)
  })

  // 命令栈变化
  eventBus.on('commandStack.changed', () => {
    updateUndoRedoState()

    // 每次命令栈变化后，检查并清理不完整的连接线
    // 使用 setTimeout 确保在当前操作完成后执行
    setTimeout(() => {
      const elementRegistry = modeler.get('elementRegistry')
      const modeling = modeler.get('modeling')
      const elements = elementRegistry.getAll()

      const toRemove = []
      elements.forEach((element) => {
        if (element.type === 'bpmn:SequenceFlow') {
          const bo = element.businessObject
          // 检查连接线是否完整
          if (!bo.sourceRef || !bo.targetRef || !element.source || !element.target) {
            toRemove.push(element)
          }
        }
      })

      if (toRemove.length > 0) {
        console.warn('命令栈变化后检测到不完整的连接线，自动清理:', toRemove.map(e => e.id))
        modeling.removeElements(toRemove)
      }
    }, 100)
  })

  // 缩放变化
  eventBus.on('canvas.viewbox.changed', (e) => {
    zoomLevel.value = e.viewbox.scale
  })

  // 导入 XML - 确保使用有效的 XML
  const xmlToImport = props.xml && props.xml.trim() ? props.xml : defaultXml
  await importXML(xmlToImport)

  emit('ready', modeler)
}

// 导入 XML
async function importXML(xml) {
  // 确保使用有效的 XML
  if (!xml || !xml.trim()) {
    console.warn('XML 为空，使用默认模板')
    xml = defaultXml
  }

  // 检查是否包含图形信息，如果没有则使用默认模板
  if (!hasDiagramInfo(xml)) {
    console.warn('XML 缺少图形信息(BPMNDiagram)，使用默认模板')
    xml = defaultXml
  }

  try {
    await modeler.importXML(xml)
    const canvas = modeler.get('canvas')
    canvas.zoom('fit-viewport')

    // 导入后清理不完整的连接线
    const cleanedCount = cleanIncompleteFlows()
    if (cleanedCount > 0) {
      console.warn(`导入后自动清理了 ${cleanedCount} 条不完整的连接线`)
      message.warning(`已自动清理 ${cleanedCount} 条不完整的连接线`)

      // 清理后触发变更事件，确保 XML 同步
      await nextTick()
      emitChange()
    }
  }
  catch (error) {
    console.error('导入 BPMN 失败:', error)
    message.error(`导入 BPMN 失败: ${error.message}`)
  }
}

// 更新撤销重做状态
function updateUndoRedoState() {
  const commandStack = modeler.get('commandStack')
  canUndo.value = commandStack.canUndo()
  canRedo.value = commandStack.canRedo()
}

// 撤销
function handleUndo() {
  const commandStack = modeler.get('commandStack')
  commandStack.undo()
}

// 重做
function handleRedo() {
  const commandStack = modeler.get('commandStack')
  commandStack.redo()
}

// 放大
function handleZoomIn() {
  const canvas = modeler.get('canvas')
  canvas.zoom(canvas.zoom() * 1.2)
}

// 缩小
function handleZoomOut() {
  const canvas = modeler.get('canvas')
  canvas.zoom(canvas.zoom() * 0.8)
}

// 重置缩放
function handleZoomReset() {
  const canvas = modeler.get('canvas')
  canvas.zoom('fit-viewport')
}

// 导入
function handleImport() {
  fileInputRef.value?.click()
}

// 文件选择
async function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file)
    return

  const reader = new FileReader()
  reader.onload = async (event) => {
    try {
      await importXML(event.target.result)
      message.success('导入成功')
    }
    catch (error) {
      message.error(`导入失败: ${error.message}`)
    }
  }
  reader.readAsText(file)

  // 清空文件输入
  e.target.value = ''
}

// 导出 BPMN
async function handleDownloadBpmn() {
  try {
    // 先验证
    const errors = validateDiagram()
    if (errors.length > 0) {
      const errorMsg = errors.join('\n• ')
      message.error(`流程图验证失败，无法导出:\n• ${errorMsg}`, {
        duration: 5000,
      })
      return
    }

    const { xml } = await modeler.saveXML({ format: true })
    downloadFile(xml, 'diagram.bpmn', 'application/xml')
    message.success('导出成功')
  }
  catch (error) {
    message.error(`导出失败: ${error.message}`)
  }
}

// 导出 SVG
async function handleDownloadSvg() {
  try {
    const { svg } = await modeler.saveSVG()
    downloadFile(svg, 'diagram.svg', 'image/svg+xml')
    message.success('导出成功')
  }
  catch (error) {
    message.error(`导出失败: ${error.message}`)
  }
}

// 下载文件
function downloadFile(content, filename, type) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

// 预览
async function handlePreview() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    previewXml.value = xml
    showPreviewModal.value = true
  }
  catch (error) {
    message.error(`获取XML失败: ${error.message}`)
  }
}

// 复制XML到剪贴板
function handleCopyXml() {
  navigator.clipboard.writeText(previewXml.value)
  message.success('XML已复制到剪贴板')
}

// 从预览弹窗下载XML
function handleDownloadXmlFromPreview() {
  downloadFile(previewXml.value, 'diagram.bpmn', 'application/xml')
  message.success('下载成功')
}

// 验证流程
function handleValidate() {
  const errors = validateDiagram()
  if (errors.length === 0) {
    message.success('流程图验证通过！')
  }
  else {
    const errorMsg = errors.join('\n• ')
    message.error(`流程图验证失败:\n• ${errorMsg}`, {
      duration: 5000,
    })
  }
}

// 自动布局
function handleAutoLayout() {
  if (!modeler)
    return
  try {
    autoLayout(modeler)
    message.success('自动布局完成')
  }
  catch (error) {
    message.error(`自动布局失败: ${error.message}`)
  }
}

// 暗色模式切换
function toggleDarkMode() {
  darkMode.value = !darkMode.value
}

// 触发变更
function emitChange() {
  modeler.saveXML({ format: true }).then(({ xml }) => {
    emit('change', xml)
  }).catch(() => {})
}

// 验证流程图
function validateDiagram() {
  const elementRegistry = modeler.get('elementRegistry')
  const errors = []

  // 获取所有元素
  const elements = elementRegistry.getAll()

  // 检查连接线
  const incompleteFlows = []
  elements.forEach((element) => {
    if (element.type === 'bpmn:SequenceFlow') {
      const bo = element.businessObject
      const elementName = bo.name || element.id

      // 检查 sourceRef
      if (!bo.sourceRef) {
        errors.push(`连接线 "${elementName}" 缺少起点`)
        incompleteFlows.push(element.id)
      }

      // 检查 targetRef
      if (!bo.targetRef) {
        errors.push(`连接线 "${elementName}" 缺少终点`)
        incompleteFlows.push(element.id)
      }

      // 额外检查：确保 source 和 target 元素存在
      if (element.source && !element.target) {
        errors.push(`连接线 "${elementName}" 没有连接到目标节点，请删除或完成连接`)
        incompleteFlows.push(element.id)
      }
      if (!element.source && element.target) {
        errors.push(`连接线 "${elementName}" 没有连接到起始节点，请删除或完成连接`)
        incompleteFlows.push(element.id)
      }
    }
  })

  // 如果有未完成的连接线，提供删除选项
  if (incompleteFlows.length > 0) {
    console.error('发现未完成的连接线:', incompleteFlows)
    errors.push(`\n提示：请在画布上选中并删除这些未完成的连接线`)
  }

  // 检查是否有开始节点
  const hasStartEvent = elements.some(el => el.type === 'bpmn:StartEvent')
  if (!hasStartEvent) {
    errors.push('流程图必须包含至少一个开始节点')
  }

  // 检查是否有结束节点
  const hasEndEvent = elements.some(el => el.type === 'bpmn:EndEvent')
  if (!hasEndEvent) {
    errors.push('流程图必须包含至少一个结束节点')
  }

  return errors
}

// 清理未完成的连接线
function cleanIncompleteFlows() {
  const elementRegistry = modeler.get('elementRegistry')
  const modeling = modeler.get('modeling')
  const elements = elementRegistry.getAll()

  const toRemove = []

  elements.forEach((element) => {
    if (element.type === 'bpmn:SequenceFlow') {
      const bo = element.businessObject

      // 情况1：完全孤立的连接线（既没有 sourceRef 也没有 targetRef）
      if (!bo.sourceRef && !bo.targetRef) {
        console.warn('发现完全孤立的连接线:', element.id)
        toRemove.push(element)
        return
      }

      // 情况2：缺少起点或终点
      if (!bo.sourceRef || !bo.targetRef) {
        console.warn('发现不完整的连接线（缺少引用）:', element.id, {
          sourceRef: bo.sourceRef,
          targetRef: bo.targetRef,
        })
        toRemove.push(element)
        return
      }

      // 情况3：引用的节点不存在
      if (!element.source || !element.target) {
        console.warn('发现不完整的连接线（引用的节点不存在）:', element.id, {
          source: element.source,
          target: element.target,
        })
        toRemove.push(element)
        return
      }

      // 情况4：sourceRef 或 targetRef 指向的元素在图中不存在
      const sourceElement = elementRegistry.get(bo.sourceRef.id)
      const targetElement = elementRegistry.get(bo.targetRef.id)
      if (!sourceElement || !targetElement) {
        console.warn('发现孤立的连接线（引用的元素已被删除）:', element.id, {
          sourceExists: !!sourceElement,
          targetExists: !!targetElement,
        })
        toRemove.push(element)
      }
    }
  })

  if (toRemove.length > 0) {
    console.warn('自动清理未完成的连接线:', toRemove.map(e => e.id))
    modeling.removeElements(toRemove)
    return toRemove.length
  }

  return 0
}

// 获取 XML
async function getXML(skipValidation = false) {
  try {
    // 先清理未完成的连接线
    const cleanedCount = cleanIncompleteFlows()
    if (cleanedCount > 0) {
      message.warning(`已自动删除 ${cleanedCount} 条未完成的连接线`)
    }

    // 验证流程图
    if (!skipValidation) {
      const errors = validateDiagram()
      if (errors.length > 0) {
        const errorMsg = errors.join('\n')
        message.error(`流程图验证失败:\n${errorMsg}`)
        console.error('流程图验证错误:', errors)
        return null
      }
    }

    const { xml } = await modeler.saveXML({ format: true })
    return xml
  }
  catch (error) {
    console.error('获取XML失败:', error)
    message.error(`获取XML失败: ${error.message}`)
    return null
  }
}

// 设置 XML
async function setXML(xml) {
  if (xml) {
    await importXML(xml)
  }
}

// 监听 xml prop 变化
watch(() => props.xml, (newXml) => {
  if (modeler) {
    const xmlToImport = newXml && newXml.trim() ? newXml : defaultXml
    importXML(xmlToImport)
  }
})

// 暴露方法
defineExpose({
  getXML,
  setXML,
  modeler: () => modeler,
  selectedElement: computed(() => selectedElement.value),
})
</script>

<style scoped>
/* ========== 设计系统变量 ========== */
:root {
  --primary: #6366f1;
  --primary-hover: #4f46e5;
  --secondary: #818cf8;
  --success: #10b981;
  --background: #f5f3ff;
  --surface: #ffffff;
  --text-primary: #1e1b4b;
  --border: #e2e8f0;
  --transition: 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

.flow-modeler-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 2px solid var(--primary);
  border-radius: 16px;
  overflow: hidden;
  transition:
    background var(--transition),
    border-color var(--transition),
    box-shadow var(--transition);
  box-shadow:
    0 4px 6px -1px rgba(99, 102, 241, 0.1),
    0 2px 4px -1px rgba(99, 102, 241, 0.06);
}

.flow-modeler-container:hover {
  box-shadow:
    0 10px 15px -3px rgba(99, 102, 241, 0.15),
    0 4px 6px -2px rgba(99, 102, 241, 0.1);
}

.flow-modeler-container.dark {
  background: #1e293b;
  border-color: #334155;
}

.toolbar {
  padding: 12px 16px;
  border-bottom: 2px solid var(--primary);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.05) 0%, rgba(129, 140, 248, 0.05) 100%);
  transition:
    background var(--transition),
    border-color var(--transition);
  backdrop-filter: blur(8px);
}

.dark .toolbar {
  background: #0f172a;
  border-color: #334155;
}

.canvas-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: var(--background);
}

.canvas-container {
  width: 100%;
  height: 100%;
}

/* XML预览容器样式 - 浅色主题 */
.xml-preview-container {
  max-height: 60vh;
  overflow-y: auto;
  overflow-x: auto;
  background: #f8f9fa;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

:deep(.xml-preview-container .n-code) {
  background: transparent;
}

:deep(.xml-preview-container .n-code pre) {
  white-space: pre-wrap;
  word-break: break-all;
  color: #1e1e1e;
  background: transparent;
}

:deep(.xml-preview-container .n-code .hljs) {
  background: transparent;
}

/* ========== bpmn-js 工具面板 —— Flat Design + Indigo 主题 ========== */

/* 面板容器：现代化卡片 */
:deep(.djs-palette) {
  --palette-entry-color: #64748b;
  --palette-entry-hover-color: var(--primary);
  --palette-separator-color: var(--border);
  --palette-entry-selected-color: var(--primary);

  background: var(--surface);
  border: 2px solid var(--primary);
  border-radius: 16px;
  box-shadow:
    0 10px 15px -3px rgba(99, 102, 241, 0.1),
    0 4px 6px -2px rgba(99, 102, 241, 0.05);
  padding: 8px !important;
  left: 20px !important;
  top: 20px !important;
  overflow: hidden;
  transition:
    box-shadow var(--transition),
    transform var(--transition);
}

:deep(.djs-palette:hover) {
  box-shadow:
    0 20px 25px -5px rgba(99, 102, 241, 0.15),
    0 10px 10px -5px rgba(99, 102, 241, 0.1);
  transform: translateY(-2px);
}

/* 每个工具项：Flat Design 风格 */
:deep(.djs-palette .entry) {
  border-radius: 12px;
  margin: 2px;
  cursor: pointer;
  transition: all var(--transition);
  will-change: transform, background-color;
}

:deep(.djs-palette .entry:hover) {
  cursor: pointer;
}

:deep(.djs-palette .entry:active) {
  transform: scale(0.95);
}

/* 图标对齐 */
:deep(.djs-palette .entry::before) {
  vertical-align: middle !important;
}

/* hover 背景 - Flat Design 纯色 */
:deep(.djs-palette .entry:hover) {
  background: rgba(99, 102, 241, 0.1);
  transform: translateY(-1px);
}

/* 事件类工具 hover — 成功绿色 */
:deep(.djs-palette .entry.bpmn-icon-start-event-none:hover),
:deep(.djs-palette .entry.bpmn-icon-end-event-none:hover),
:deep(.djs-palette .entry.bpmn-icon-intermediate-event-none:hover) {
  background: rgba(16, 185, 129, 0.1);
  color: var(--success) !important;
}

/* 任务类工具 hover — 主色 */
:deep(.djs-palette .entry.bpmn-icon-user-task:hover),
:deep(.djs-palette .entry.bpmn-icon-service-task:hover),
:deep(.djs-palette .entry.bpmn-icon-script-task:hover),
:deep(.djs-palette .entry.bpmn-icon-business-rule-task:hover),
:deep(.djs-palette .entry.bpmn-icon-manual-task:hover),
:deep(.djs-palette .entry.bpmn-icon-send-task:hover),
:deep(.djs-palette .entry.bpmn-icon-receive-task:hover) {
  background: rgba(99, 102, 241, 0.15);
  color: var(--primary) !important;
}

/* 网关类工具 hover — 琥珀色 */
:deep(.djs-palette .entry.bpmn-icon-gateway-xor:hover),
:deep(.djs-palette .entry.bpmn-icon-gateway-parallel:hover),
:deep(.djs-palette .entry.bpmn-icon-gateway-complex:hover),
:deep(.djs-palette .entry.bpmn-icon-gateway-or:hover),
:deep(.djs-palette .entry.bpmn-icon-gateway-eventbased:hover) {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b !important;
}

/* 子流程/调用活动 hover — 次要色 */
:deep(.djs-palette .entry.bpmn-icon-subprocess-expanded:hover),
:deep(.djs-palette .entry.bpmn-icon-subprocess-collapsed:hover),
:deep(.djs-palette .entry.bpmn-icon-call-activity:hover),
:deep(.djs-palette .entry.bpmn-icon-participant:hover) {
  background: rgba(129, 140, 248, 0.15);
  color: var(--secondary) !important;
}

/* 数据类工具 hover — 灰色 */
:deep(.djs-palette .entry.bpmn-icon-data-object:hover),
:deep(.djs-palette .entry.bpmn-icon-data-store:hover),
:deep(.djs-palette .entry.bpmn-icon-text-annotation:hover),
:deep(.djs-palette .entry.bpmn-icon-group:hover) {
  background: rgba(100, 116, 139, 0.1);
  color: #475569 !important;
}

/* 选中态 - Flat Design 纯色 */
:deep(.djs-palette .entry.selected),
:deep(.djs-palette .highlighted-entry) {
  background: var(--primary) !important;
  color: white !important;
  box-shadow: 0 4px 6px -1px rgba(99, 102, 241, 0.3) !important;
}

/* 分隔线 */
:deep(.djs-palette .separator) {
  margin: 6px 4px !important;
  padding: 0 !important;
  border-bottom: 2px solid var(--border) !important;
  clear: both;
}

/* 分组标签 */
:deep(.djs-palette .group) {
  padding: 8px 4px 4px !important;
  font-size: 10px !important;
  font-weight: 700 !important;
  color: var(--primary) !important;
  text-transform: uppercase !important;
  letter-spacing: 1px !important;
  text-align: center !important;
  clear: both;
}

/* ========== 上下文菜单（context pad） ========== */
:deep(.djs-context-pad) {
  background: var(--surface);
  border: 2px solid var(--primary);
  border-radius: 14px;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.1);
  padding: 6px;
  gap: 4px;
}

:deep(.djs-context-pad .entry) {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  transition: all var(--transition);
  cursor: pointer;
}

:deep(.djs-context-pad .entry:hover) {
  background: rgba(99, 102, 241, 0.1);
  transform: scale(1.1);
  cursor: pointer;
}

:deep(.djs-context-pad .entry::before) {
  font-size: 18px;
  color: #475569;
  transition: color var(--transition);
}

:deep(.djs-context-pad .entry:hover::before) {
  color: var(--primary);
}

/* ========== 弹出菜单（类型切换等） ========== */
:deep(.djs-popup) {
  background: var(--surface);
  border: 2px solid var(--primary);
  border-radius: 14px;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.1);
  padding: 8px;
}

:deep(.djs-popup .entry) {
  border-radius: 10px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  transition: all var(--transition);
  cursor: pointer;
}

:deep(.djs-popup .entry:hover) {
  background: rgba(99, 102, 241, 0.1);
  color: var(--primary);
  cursor: pointer;
}

/* 隐藏 bpmn-js 水印 */
:deep(.bjs-powered-by) {
  display: none;
}

/* bpmn-js 标签样式 */
:deep(.djs-label text) {
  font-family:
    'Fira Sans',
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    sans-serif !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  fill: var(--text-primary) !important;
}

/* Focus 状态 */
:deep(.djs-palette .entry:focus-visible),
:deep(.djs-context-pad .entry:focus-visible),
:deep(.djs-popup .entry:focus-visible) {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

/* ========== 暗色模式 ========== */
.dark :deep(.djs-palette) {
  --palette-entry-color: #94a3b8;
  --palette-entry-hover-color: var(--secondary);
  --palette-separator-color: #334155;

  background: #1e293b;
  border-color: #475569;
  box-shadow:
    0 10px 15px -3px rgba(0, 0, 0, 0.3),
    0 4px 6px -2px rgba(0, 0, 0, 0.2);
}

.dark :deep(.djs-palette:hover) {
  box-shadow:
    0 20px 25px -5px rgba(0, 0, 0, 0.4),
    0 10px 10px -5px rgba(0, 0, 0, 0.3);
}

.dark :deep(.djs-palette .entry:hover) {
  background: rgba(129, 140, 248, 0.2);
}

.dark :deep(.djs-palette .entry.bpmn-icon-start-event-none:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-end-event-none:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-intermediate-event-none:hover) {
  background: rgba(16, 185, 129, 0.2);
  color: #34d399 !important;
}

.dark :deep(.djs-palette .entry.bpmn-icon-user-task:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-service-task:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-script-task:hover) {
  background: rgba(129, 140, 248, 0.25);
  color: #a5b4fc !important;
}

.dark :deep(.djs-palette .entry.bpmn-icon-gateway-xor:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-gateway-parallel:hover) {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24 !important;
}

.dark :deep(.djs-palette .entry.bpmn-icon-subprocess-expanded:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-subprocess-collapsed:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-call-activity:hover) {
  background: rgba(129, 140, 248, 0.25);
  color: #c4b5fd !important;
}

.dark :deep(.djs-palette .entry.selected),
.dark :deep(.djs-palette .highlighted-entry) {
  background: var(--secondary) !important;
  color: white !important;
  box-shadow: 0 4px 6px -1px rgba(129, 140, 248, 0.4) !important;
}

.dark :deep(.djs-palette .separator) {
  border-color: #475569 !important;
}

.dark :deep(.djs-palette .group) {
  color: var(--secondary) !important;
}

.dark :deep(.djs-context-pad) {
  background: #1e293b;
  border-color: #475569;
}

.dark :deep(.djs-context-pad .entry:hover) {
  background: rgba(129, 140, 248, 0.2);
}

.dark :deep(.djs-context-pad .entry:hover::before) {
  color: var(--secondary);
}

.dark :deep(.djs-popup) {
  background: #1e293b;
  border-color: #475569;
}

.dark :deep(.djs-popup .entry:hover) {
  background: rgba(129, 140, 248, 0.2);
  color: var(--secondary);
}

.dark :deep(.djs-container) {
  background: #0f172a;
}

.dark :deep(.djs-container svg) {
  background: #0f172a;
}

.dark :deep(.djs-minimap) {
  background: #1e293b;
  border-color: #475569;
}

/* ========== 无障碍优化 ========== */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
