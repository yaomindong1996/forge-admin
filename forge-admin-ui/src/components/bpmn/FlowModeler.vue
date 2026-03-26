<template>
  <div class="flow-modeler-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <n-space>
        <!-- 撤销/重做 -->
        <n-button-group>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" @click="handleUndo" :disabled="!canUndo">
                <i class="i-material-symbols:undo" />
              </n-button>
            </template>
            撤销 (Ctrl+Z)
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button size="small" @click="handleRedo" :disabled="!canRedo">
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

      </n-space>
    </div>

    <!-- 画布区域 -->
    <div class="canvas-wrapper">
      <div class="canvas-container" ref="canvasRef"></div>
    </div>

    <!-- 节点属性弹窗 -->
    <n-modal
      v-model:show="showPropertiesModal"
      preset="card"
      :title="getElementTitle()"
      style="width: 600px; max-height: 80vh"
      :mask-closable="false"
    >
      <div class="properties-modal-content">
        <NodePropertiesPanel
          v-if="rawSelectedElement && isModelerReady"
          :element="rawSelectedElement"
          :modeler="modeler"
          @update="handleElementUpdate"
        />
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showPropertiesModal = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 隐藏的文件输入 -->
    <input
      type="file"
      ref="fileInputRef"
      accept=".bpmn,.bpmn20.xml,.xml"
      style="display: none"
      @change="handleFileChange"
    />

    <!-- XML预览弹窗 -->
    <n-modal v-model:show="showPreviewModal" preset="card" title="BPMN XML 预览" style="width: 800px">
      <n-code :code="previewXml" language="xml" :show-line-numbers="true" />
    </n-modal>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, toRaw, computed, shallowRef } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'
import NodePropertiesPanel from './NodePropertiesPanel.vue'
import flowableModdle from './flowable-moddle.json'

const props = defineProps({
  xml: {
    type: String,
    default: ''
  },
  readOnly: {
    type: Boolean,
    default: false
  }
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
const showPropertiesModal = ref(false)
const showPreviewModal = ref(false)
const previewXml = ref('')

// 获取原始元素（避免 Vue 代理与 bpmn-js 冲突）
const rawSelectedElement = computed(() => selectedElement.value ? toRaw(selectedElement.value) : null)

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
      bindTo: document
    },
    moddleExtensions: {
      flowable: flowableModdle
    }
  })
  modelerRef.value = modeler
  isModelerReady.value = true

  // 监听事件
  const eventBus = modeler.get('eventBus')

  // 元素选择事件
  eventBus.on('selection.changed', (e) => {
    const { newSelection } = e
    if (newSelection.length === 1) {
      // 如果选中的是同一个元素，保持弹窗状态不变
      if (selectedElement.value && selectedElement.value.id === newSelection[0].id) {
        return
      }
      selectedElement.value = newSelection[0]
      showPropertiesModal.value = true
    } else {
      // 只有当弹窗没有打开时才清空选中状态
      // 这样可以防止在属性面板操作时意外关闭弹窗
      if (!showPropertiesModal.value) {
        selectedElement.value = null
      }
    }
  })

  // 元素变化事件
  eventBus.on('element.changed', () => {
    updateUndoRedoState()
    emitChange()
  })

  // 命令栈变化
  eventBus.on('commandStack.changed', () => {
    updateUndoRedoState()
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
  } catch (error) {
    console.error('导入 BPMN 失败:', error)
    message.error('导入 BPMN 失败: ' + error.message)
  }
}

// 更新撤销重做状态
function updateUndoRedoState() {
  const commandStack = modeler.get('commandStack')
  canUndo.value = commandStack.canUndo()
  canRedo.value = commandStack.canRedo()
}

// 获取元素标题
function getElementTitle() {
  if (!selectedElement.value) return '属性设置'
  
  const type = selectedElement.value.type
  const name = selectedElement.value.businessObject?.name
  
  const typeNames = {
    'bpmn:StartEvent': '开始节点',
    'bpmn:EndEvent': '结束节点',
    'bpmn:UserTask': '用户任务',
    'bpmn:ServiceTask': '服务任务',
    'bpmn:ScriptTask': '脚本任务',
    'bpmn:BusinessRuleTask': '业务规则任务',
    'bpmn:ManualTask': '手工任务',
    'bpmn:ExclusiveGateway': '排他网关',
    'bpmn:ParallelGateway': '并行网关',
    'bpmn:InclusiveGateway': '包容网关',
    'bpmn:SequenceFlow': '序列流',
    'bpmn:SubProcess': '子流程',
    'bpmn:CallActivity': '调用活动'
  }
  
  return name || typeNames[type] || '属性设置'
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
  if (!file) return
  
  const reader = new FileReader()
  reader.onload = async (event) => {
    try {
      await importXML(event.target.result)
      message.success('导入成功')
    } catch (error) {
      message.error('导入失败: ' + error.message)
    }
  }
  reader.readAsText(file)
  
  // 清空文件输入
  e.target.value = ''
}

// 导出 BPMN
async function handleDownloadBpmn() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    downloadFile(xml, 'diagram.bpmn', 'application/xml')
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败: ' + error.message)
  }
}

// 导出 SVG
async function handleDownloadSvg() {
  try {
    const { svg } = await modeler.saveSVG()
    downloadFile(svg, 'diagram.svg', 'image/svg+xml')
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败: ' + error.message)
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
  } catch (error) {
    message.error('获取XML失败: ' + error.message)
  }
}

// 触发变更
function emitChange() {
  modeler.saveXML({ format: true }).then(({ xml }) => {
    emit('change', xml)
  }).catch(() => {})
}

// 元素更新
function handleElementUpdate() {
  emitChange()
}

// 获取 XML
async function getXML() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    return xml
  } catch (error) {
    console.error('获取XML失败:', error)
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
  modeler: () => modeler
})
</script>

<style scoped>
.flow-modeler-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.toolbar {
  padding: 8px 12px;
  border-bottom: 1px solid #e0e0e0;
  background: #fafafa;
}

.main-area {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.canvas-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.canvas-container {
  width: 100%;
  height: 100%;
}

/* bpmn-js 样式覆盖 */
:deep(.djs-palette) {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.djs-palette-entries) {
  padding: 8px;
}

:deep(.djs-palette-entry) {
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
}

:deep(.djs-palette-entry:hover) {
  background: #e8f4ff;
}

:deep(.bjs-powered-by) {
  display: none;
}

:deep(.djs-context-pad) {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.djs-context-pad .entry) {
  padding: 4px;
  border-radius: 4px;
}

:deep(.djs-context-pad .entry:hover) {
  background: #e8f4ff;
}

.properties-modal-content {
  max-height: 60vh;
  overflow-y: auto;
}
</style>