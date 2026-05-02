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
    <n-modal v-model:show="showPreviewModal" preset="card" title="BPMN XML 预览" style="width: 800px">
      <n-code :code="previewXml" language="xml" :show-line-numbers="true" />
    </n-modal>
  </div>
</template>

<script setup>
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, toRaw, watch } from 'vue'
import flowableModdle from './flowable-moddle.json'
import CustomRenderer from './CustomRenderer.js'
import CanvasBackground from './CanvasBackground.js'
import autoLayout from './AutoLayout.js'
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

// 获取元素标题
function getElementTitle() {
  if (!selectedElement.value)
    return '属性设置'

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
    'bpmn:CallActivity': '调用活动',
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

// 自动布局
function handleAutoLayout() {
  if (!modeler) return
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

// 元素更新
function handleElementUpdate() {
  emitChange()
}

// 获取 XML
async function getXML() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    return xml
  }
  catch (error) {
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
  modeler: () => modeler,
  selectedElement: computed(() => selectedElement.value),
})
</script>

<style scoped>
.flow-modeler-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  transition: background 0.3s, border-color 0.3s;
}

.flow-modeler-container.dark {
  background: #1e293b;
  border-color: #334155;
}

.toolbar {
  padding: 8px 12px;
  border-bottom: 1px solid #e0e0e0;
  background: #fafafa;
  transition: background 0.3s, border-color 0.3s;
}

.dark .toolbar {
  background: #0f172a;
  border-color: #334155;
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

/* ========== bpmn-js 工具面板 —— 专业级视觉设计 ========== */

/* 面板容器：浮动卡片 */
:deep(.djs-palette) {
  --palette-entry-color: #64748b;
  --palette-entry-hover-color: #64748b;
  --palette-separator-color: #f1f5f9;
  --palette-entry-selected-color: #2563eb;

  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 14px;
  box-shadow:
    0 8px 32px rgba(15, 23, 42, 0.06),
    0 2px 6px rgba(15, 23, 42, 0.04);
  padding: 4px !important;
  left: 16px !important;
  top: 16px !important;
  overflow: hidden;
  transition: box-shadow 0.3s, border-color 0.3s;
}

:deep(.djs-palette:hover) {
  box-shadow:
    0 12px 40px rgba(15, 23, 42, 0.1),
    0 4px 12px rgba(15, 23, 42, 0.06);
}

/* 每个工具项：保持 bpmn-js 的 float 布局，只增强视觉效果 */
:deep(.djs-palette .entry) {
  border-radius: 10px;
  margin: 1px;
  cursor: grab;
  transition: background 0.2s cubic-bezier(0.4, 0, 0.2, 1), transform 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform;
}

:deep(.djs-palette .entry:active) {
  cursor: grabbing;
  transform: scale(0.92);
}

/* 图标对齐微调 */
:deep(.djs-palette .entry::before) {
  vertical-align: middle !important;
}

/* hover 背景统一 */
:deep(.djs-palette .entry:hover) {
  background: #f1f5f9;
  transform: translateY(-1px);
}

/* 事件类工具 hover — 绿色 */
:deep(.djs-palette .entry.bpmn-icon-start-event-none:hover),
:deep(.djs-palette .entry.bpmn-icon-end-event-none:hover),
:deep(.djs-palette .entry.bpmn-icon-intermediate-event-none:hover) {
  background: #ecfdf5;
  color: #10b981 !important;
}

/* 任务类工具 hover — 蓝色 */
:deep(.djs-palette .entry.bpmn-icon-user-task:hover),
:deep(.djs-palette .entry.bpmn-icon-service-task:hover),
:deep(.djs-palette .entry.bpmn-icon-script-task:hover),
:deep(.djs-palette .entry.bpmn-icon-business-rule-task:hover),
:deep(.djs-palette .entry.bpmn-icon-manual-task:hover),
:deep(.djs-palette .entry.bpmn-icon-send-task:hover),
:deep(.djs-palette .entry.bpmn-icon-receive-task:hover) {
  background: #eff6ff;
  color: #3b82f6 !important;
}

/* 网关类工具 hover — 琥珀 */
:deep(.djs-palette .entry.bpmn-icon-gateway-xor:hover),
:deep(.djs-palette .entry.bpmn-icon-gateway-parallel:hover),
:deep(.djs-palette .entry.bpmn-icon-gateway-complex:hover),
:deep(.djs-palette .entry.bpmn-icon-gateway-or:hover),
:deep(.djs-palette .entry.bpmn-icon-gateway-eventbased:hover) {
  background: #fffbeb;
  color: #f59e0b !important;
}

/* 子流程/调用活动 hover — 紫色 */
:deep(.djs-palette .entry.bpmn-icon-subprocess-expanded:hover),
:deep(.djs-palette .entry.bpmn-icon-subprocess-collapsed:hover),
:deep(.djs-palette .entry.bpmn-icon-call-activity:hover),
:deep(.djs-palette .entry.bpmn-icon-participant:hover) {
  background: #f5f3ff;
  color: #8b5cf6 !important;
}

/* 数据类工具 hover — 灰色 */
:deep(.djs-palette .entry.bpmn-icon-data-object:hover),
:deep(.djs-palette .entry.bpmn-icon-data-store:hover),
:deep(.djs-palette .entry.bpmn-icon-text-annotation:hover),
:deep(.djs-palette .entry.bpmn-icon-group:hover) {
  background: #f8fafc;
  color: #475569 !important;
}

/* 选中态 */
:deep(.djs-palette .entry.selected),
:deep(.djs-palette .highlighted-entry) {
  background: #dbeafe !important;
  color: #2563eb !important;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2) !important;
}

/* 分隔线 */
:deep(.djs-palette .separator) {
  margin: 3px 4px !important;
  padding: 0 !important;
  border-bottom: 1px solid #f1f5f9 !important;
  clear: both;
}

/* 分组标签 */
:deep(.djs-palette .group) {
  padding: 6px 2px 2px !important;
  font-size: 9px !important;
  font-weight: 700 !important;
  color: #94a3b8 !important;
  text-transform: uppercase !important;
  letter-spacing: 0.8px !important;
  text-align: center !important;
  clear: both;
}

/* ========== 上下文菜单（context pad） ========== */
:deep(.djs-context-pad) {
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.1);
  padding: 4px;
  gap: 2px;
}

:deep(.djs-context-pad .entry) {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.djs-context-pad .entry:hover) {
  background: #eff6ff;
  transform: scale(1.08);
}

:deep(.djs-context-pad .entry::before) {
  font-size: 17px;
  color: #475569;
  transition: color 0.2s;
}

:deep(.djs-context-pad .entry:hover::before) {
  color: #3b82f6;
}

/* ========== 弹出菜单（类型切换等） ========== */
:deep(.djs-popup) {
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.1);
  padding: 6px;
}

:deep(.djs-popup .entry) {
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  color: #334155;
  transition: all 0.15s;
}

:deep(.djs-popup .entry:hover) {
  background: #eff6ff;
  color: #3b82f6;
}

/* 隐藏 bpmn-js 水印 */
:deep(.bjs-powered-by) {
  display: none;
}

/* bpmn-js 标签样式 */
:deep(.djs-label text) {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  fill: #334155 !important;
}

/* 暗色模式 —— 面板 */
.dark :deep(.djs-palette) {
  --palette-entry-color: #94a3b8;
  --palette-entry-hover-color: #94a3b8;
  --palette-separator-color: #334155;

  background: rgba(30, 41, 59, 0.92);
  border-color: rgba(51, 65, 85, 0.6);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    0 2px 6px rgba(0, 0, 0, 0.2);
}

/* 暗色模式 —— hover 默认背景 */
.dark :deep(.djs-palette .entry:hover) {
  background: #334155;
}

/* 暗色模式 —— 事件类 hover */
.dark :deep(.djs-palette .entry.bpmn-icon-start-event-none:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-end-event-none:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-intermediate-event-none:hover) {
  background: #064e3b;
  color: #34d399 !important;
}

/* 暗色模式 —— 任务类 hover */
.dark :deep(.djs-palette .entry.bpmn-icon-user-task:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-service-task:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-script-task:hover) {
  background: #1e3a5f;
  color: #60a5fa !important;
}

/* 暗色模式 —— 网关类 hover */
.dark :deep(.djs-palette .entry.bpmn-icon-gateway-xor:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-gateway-parallel:hover) {
  background: #451a03;
  color: #fbbf24 !important;
}

/* 暗色模式 —— 子流程 hover */
.dark :deep(.djs-palette .entry.bpmn-icon-subprocess-expanded:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-subprocess-collapsed:hover),
.dark :deep(.djs-palette .entry.bpmn-icon-call-activity:hover) {
  background: #2e1065;
  color: #a78bfa !important;
}

/* 暗色模式 —— 选中态 */
.dark :deep(.djs-palette .entry.selected),
.dark :deep(.djs-palette .highlighted-entry) {
  background: #1e40af !important;
  color: #93c5fd !important;
  box-shadow: 0 0 0 2px rgba(147, 197, 253, 0.3) !important;
}

/* 暗色模式 —— 分隔线 */
.dark :deep(.djs-palette .separator) {
  border-color: #334155 !important;
}

/* 暗色模式 —— 分组标签 */
.dark :deep(.djs-palette .group) {
  color: #64748b !important;
}

.dark :deep(.djs-context-pad) {
  background: #1e293b;
  border-color: #334155;
}

.dark :deep(.djs-context-pad .entry:hover) {
  background: #334155;
}

.dark :deep(.djs-popup) {
  background: #1e293b;
  border-color: #334155;
}

.dark :deep(.djs-popup .entry:hover) {
  background: #334155;
}

/* 暗色模式画布 */
.dark :deep(.djs-container) {
  background: #0f172a;
}

.dark :deep(.djs-container svg) {
  background: #0f172a;
}

.dark :deep(.djs-minimap) {
  background: #1e293b;
  border-color: #334155;
}
</style>
