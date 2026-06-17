<script setup>
/**
 * NodeCard — 节点卡片基类
 *
 * 提供：
 * - 卡片外层（圆角、阴影、悬停高亮、选中高亮）
 * - 顶部色条（按节点类型映射颜色）
 * - 标题行（图标 + 名称 + 操作图标）
 * - 默认槽：节点正文内容
 * - 状态徽章（completed / running / rejected / pending）
 *
 * 子组件（StartNode / ApproverNode 等）只需传入 nodeType / name / icon / color，并填默认槽即可。
 *
 * Props:
 *   - node           flowJson 节点对象
 *   - selected       是否选中（外层高亮）
 *   - status         节点运行状态 completed / running / rejected / pending / skipped / null
 *   - readonly       禁用编辑（只读模式 / 查看器）
 *   - icon           顶部图标（i-mdi-xxx）
 *   - colorVar       UnoCSS 颜色变量名：'primary' / 'success' / 'warning' / 'info' / 'error'
 *   - showActions    顶部右上角是否显示操作图标（默认 true，readonly 自动隐藏）
 *
 * Events:
 *   - click（卡片本体）
 *   - delete（顶部 X 按钮，readonly 时不渲染）
 *   - context-menu（右键）
 *
 * Slots:
 *   - default        卡片正文（必填）
 *   - title-extra    标题行右侧附加（如徽章）
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  status: { type: String, default: null },
  readonly: { type: Boolean, default: false },
  icon: { type: String, default: 'i-mdi-checkbox-blank-circle' },
  colorVar: { type: String, default: 'primary' },
  showActions: { type: Boolean, default: true },
  width: { type: Number, default: 220 },
  height: { type: Number, default: 80 },
})

const emit = defineEmits(['click', 'delete', 'context-menu'])

const COLOR_MAP = {
  primary: { bar: 'bg-primary', text: 'text-primary' },
  success: { bar: 'bg-success', text: 'text-success' },
  warning: { bar: 'bg-warning', text: 'text-warning' },
  info: { bar: 'bg-info', text: 'text-info' },
  error: { bar: 'bg-error', text: 'text-error' },
  gray: { bar: 'bg-gray-400', text: 'text-gray-500' },
}

const colorClasses = computed(() => COLOR_MAP[props.colorVar] || COLOR_MAP.primary)

const STATUS_BADGE = {
  completed: { label: '已完成', class: 'bg-success/10 text-success' },
  running: { label: '审批中', class: 'bg-info/10 text-info' },
  pending: { label: '待办', class: 'bg-gray-100 text-gray-500' },
  rejected: { label: '已驳回', class: 'bg-error/10 text-error' },
  skipped: { label: '已跳过', class: 'bg-gray-100 text-gray-400' },
}

const statusBadge = computed(() => STATUS_BADGE[props.status] || null)

const showDelete = computed(() => props.showActions && !props.readonly)
const isDeletable = computed(() => {
  // start / end 不允许删除
  return !['start', 'end'].includes(props.node?.nodeType)
})

function handleClick() {
  emit('click', props.node)
}

function handleDelete(event) {
  event.stopPropagation()
  if (isDeletable.value)
    emit('delete', props.node)
}

function handleContextMenu(event) {
  event.preventDefault()
  emit('context-menu', { event, node: props.node })
}
</script>

<template>
  <div
    class="flow-node-card relative cursor-pointer border rounded-lg bg-white shadow-sm transition hover:shadow-md"
    :class="[
      selected ? 'border-primary ring-2 ring-primary/30' : 'border-gray-200',
      readonly ? '!cursor-default' : '',
    ]"
    :style="{ width: `${width}px`, minHeight: `${height}px` }"
    :data-node-id="node?.id"
    :data-node-type="node?.nodeType"
    @click="handleClick"
    @contextmenu="handleContextMenu"
  >
    <!-- 顶部色条 -->
    <div :class="[colorClasses.bar]" class="h-1 rounded-t-lg" />

    <!-- 标题行 -->
    <div class="flex items-center gap-2 px-3 pt-2">
      <i :class="[icon, colorClasses.text]" class="text-base" />
      <span class="text-sm flex-1 truncate text-gray-800 font-medium">
        {{ node?.name || '未命名节点' }}
      </span>
      <slot name="title-extra" />
      <span
        v-if="statusBadge"
        class="text-xs rounded px-1.5 py-0.5"
        :class="statusBadge.class"
      >
        {{ statusBadge.label }}
      </span>
      <button
        v-if="showDelete && isDeletable"
        class="text-gray-400 hover:text-error"
        aria-label="删除节点"
        @click.stop="handleDelete"
      >
        <i class="i-mdi-close text-sm" />
      </button>
    </div>

    <!-- 正文 -->
    <div class="text-xs px-3 pb-2 pt-1 text-gray-600">
      <slot />
    </div>
  </div>
</template>
